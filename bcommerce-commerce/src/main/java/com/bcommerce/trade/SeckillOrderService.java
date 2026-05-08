package com.bcommerce.trade;

import com.bcommerce.governance.RedisDistributedLock;
import com.bcommerce.governance.SeckillRateLimiter;
import com.bcommerce.mapper.FulfillmentTaskMapper;
import com.bcommerce.mapper.IdempotencyMapper;
import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.mapper.SeckillActivityMapper;
import com.bcommerce.mapper.SettlementEntryMapper;
import com.bcommerce.mapper.TradeOrderItemMapper;
import com.bcommerce.mapper.TradeOrderMapper;
import com.bcommerce.marketing.SeckillStockRedisService;
import com.bcommerce.model.FulfillmentTask;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.model.SeckillActivity;
import com.bcommerce.model.SettlementEntry;
import com.bcommerce.model.TradeOrder;
import com.bcommerce.model.TradeOrderItem;
import com.bcommerce.sharding.OrderSharding;
import com.bcommerce.support.BizIds;
import com.bcommerce.web.dto.OrderDetailResponse;
import com.bcommerce.web.dto.OrderItemResponse;
import com.bcommerce.web.dto.SeckillOrderRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SeckillOrderService {

    private final SeckillActivityMapper activityMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final FulfillmentTaskMapper fulfillmentTaskMapper;
    private final SettlementEntryMapper settlementEntryMapper;
    private final IdempotencyMapper idempotencyMapper;
    private final SeckillStockRedisService stockRedis;
    private final SeckillRateLimiter rateLimiter;
    private final RedisDistributedLock distributedLock;
    private final PaymentMockClient paymentMockClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        TradeOrder o = orderMapper.findByUserAndOrder(OrderSharding.slot(userId), userId, orderId);
        if (o == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return toDetail(o);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> listUserOrders(Long userId) {
        return orderMapper.listByUser(OrderSharding.slot(userId), userId, 50).stream()
                .map(this::toDetail)
                .toList();
    }

    private OrderDetailResponse toDetail(TradeOrder o) {
        int sh = OrderSharding.slot(o.getUserId());
        List<TradeOrderItem> items = orderItemMapper.listByUserAndOrder(sh, o.getUserId(), o.getId());
        List<OrderItemResponse> ir =
                items.stream()
                        .map(
                                i ->
                                        new OrderItemResponse(
                                                i.getSkuId(), i.getQuantity(), i.getUnitPriceCent(), i.getTitleSnapshot()))
                        .toList();
        return new OrderDetailResponse(
                o.getId(), o.getOrderNo(), o.getTotalCent(), o.getStatus(), o.getOrderType(), o.getCreatedAt(), ir);
    }

    @Transactional
    public OrderDetailResponse placeSeckill(Long userId, SeckillOrderRequest req, String idemKey) {
        if (idemKey != null && !idemKey.isBlank()) {
            Long existing = idempotencyMapper.findOrderIdByKey(idemKey.trim());
            if (existing != null) {
                TradeOrder o = orderMapper.findByUserAndOrder(OrderSharding.slot(userId), userId, existing);
                if (o == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key conflict");
                }
                return toDetail(o);
            }
        }

        if (!rateLimiter.allow(userId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many seckill requests");
        }

        String lockName = "seckill:place:" + userId;
        if (!distributedLock.tryLock(lockName, Duration.ofSeconds(12))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not acquire order lock");
        }

        try {
            return doPlace(userId, req, idemKey);
        } finally {
            distributedLock.unlock(lockName);
        }
    }

    private OrderDetailResponse doPlace(Long userId, SeckillOrderRequest req, String idemKey) {
        SeckillActivity act = activityMapper.findById(req.activityId());
        if (act == null || !"ONLINE".equals(act.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid activity");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(act.getStartTime()) || now.isAfter(act.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outside seckill time window");
        }
        if (req.quantity() > act.getLimitPerUser()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exceeds per-user purchase limit");
        }

        Long redisOutcome = stockRedis.reserve(act.getId(), req.quantity());
        if (redisOutcome == null || redisOutcome == -1L) {
            stockRedis.refreshActivity(act.getId());
            redisOutcome = stockRedis.reserve(act.getId(), req.quantity());
        }
        if (redisOutcome == null || redisOutcome == -1L) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Seckill stock temporarily unavailable");
        }
        if (redisOutcome == -2L) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }

        try {
            int rows = activityMapper.tryIncreaseSold(act.getId(), req.quantity());
            if (rows == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
            }
            ProductSku sku = skuMapper.findById(act.getSkuId());
            if (sku == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU not found");
            }
            int skuUpd = skuMapper.increaseSoldDecreaseStock(sku.getId(), req.quantity());
            if (skuUpd == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient product stock");
            }
            ProductSpu spu = spuMapper.findById(sku.getSpuId());
            try {
                paymentMockClient.mockConfirm(act.getId());
            } catch (IllegalStateException e) {
                String m = e.getMessage() != null ? e.getMessage() : "payment_failed";
                String zh =
                        "payment_circuit_open".equals(m)
                                ? "支付熔断，请稍后再试"
                                : ("channel_timeout".equals(m) ? "支付超时，请重试" : m);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, zh);
            }

            TradeOrder order = new TradeOrder();
            order.setId(BizIds.next());
            order.setOrderNo(newOrderNo());
            order.setUserId(userId);
            order.setOrderType("SECKILL");
            order.setTotalCent(act.getSeckillPriceCent() * req.quantity());
            order.setStatus("PAID");
            order.setSeckillActivityId(act.getId());
            int sh = OrderSharding.slot(userId);
            orderMapper.insert(sh, order);

            TradeOrderItem item = new TradeOrderItem();
            item.setId(BizIds.next());
            item.setOrderId(order.getId());
            item.setUserId(userId);
            item.setSkuId(sku.getId());
            item.setQuantity(req.quantity());
            item.setUnitPriceCent(act.getSeckillPriceCent());
            item.setTitleSnapshot(spu != null ? spu.getTitle() : sku.getSkuCode());
            orderItemMapper.insert(sh, item);

            FulfillmentTask task = new FulfillmentTask();
            task.setOrderId(order.getId());
            task.setStatus("PENDING");
            fulfillmentTaskMapper.insert(task);

            if (spu != null) {
                int fee = (int) (order.getTotalCent() * 0.01);
                SettlementEntry st = new SettlementEntry();
                st.setOrderId(order.getId());
                st.setMerchantId(spu.getMerchantId());
                st.setAmountCent(order.getTotalCent() - fee);
                st.setFeeCent(fee);
                st.setStatus("INIT");
                settlementEntryMapper.insert(st);
            }

            if (idemKey != null && !idemKey.isBlank()) {
                idempotencyMapper.insert(idemKey.trim(), order.getId());
            }

            rabbitTemplate.convertAndSend(
                    com.bcommerce.config.RabbitConfig.EXCHANGE,
                    com.bcommerce.config.RabbitConfig.ROUTING_KEY,
                    order.getId().toString());
            rabbitTemplate.convertAndSend(
                    com.bcommerce.config.RabbitConfig.EXCHANGE,
                    com.bcommerce.config.RabbitConfig.RISK_ROUTING_KEY,
                    order.getId() + "," + userId);

            stockRedis.refreshActivity(act.getId());
            TradeOrder loaded = orderMapper.findByUserAndOrder(sh, userId, order.getId());
            return toDetail(loaded);
        } catch (RuntimeException ex) {
            stockRedis.release(act.getId(), req.quantity());
            throw ex;
        }
    }

    private static String newOrderNo() {
        long t = System.currentTimeMillis();
        int s = ThreadLocalRandom.current().nextInt(1_000_000);
        return "BC" + t + String.format("%06d", s);
    }
}
