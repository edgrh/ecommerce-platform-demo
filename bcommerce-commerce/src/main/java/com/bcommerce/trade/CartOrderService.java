package com.bcommerce.trade;

import com.bcommerce.cart.CartService;
import com.bcommerce.governance.RedisDistributedLock;
import com.bcommerce.mapper.FulfillmentTaskMapper;
import com.bcommerce.mapper.IdempotencyMapper;
import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.mapper.SettlementEntryMapper;
import com.bcommerce.mapper.TradeOrderItemMapper;
import com.bcommerce.mapper.TradeOrderMapper;
import com.bcommerce.model.FulfillmentTask;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.model.SettlementEntry;
import com.bcommerce.model.TradeOrder;
import com.bcommerce.model.TradeOrderItem;
import com.bcommerce.product.ProductListCache;
import com.bcommerce.sharding.OrderSharding;
import com.bcommerce.support.BizIds;
import com.bcommerce.support.SkuSpecSummary;
import com.bcommerce.web.dto.CartItemResponse;
import com.bcommerce.web.dto.OrderDetailResponse;
import com.bcommerce.web.dto.OrderItemResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CartOrderService {

    private final CartService cartService;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final FulfillmentTaskMapper fulfillmentTaskMapper;
    private final SettlementEntryMapper settlementEntryMapper;
    private final IdempotencyMapper idempotencyMapper;
    private final RedisDistributedLock distributedLock;
    private final PaymentMockClient paymentMockClient;
    private final RabbitTemplate rabbitTemplate;
    private final ProductListCache productListCache;

    private OrderDetailResponse toDetail(TradeOrder o) {
        int sh = OrderSharding.slot(o.getUserId());
        var items = orderItemMapper.listByUserAndOrder(sh, o.getUserId(), o.getId());
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
    public OrderDetailResponse checkoutFromCart(Long userId, String idemKey) {
        if (idemKey != null && !idemKey.isBlank()) {
            Long existing = idempotencyMapper.findOrderIdByKey(idemKey.trim());
            if (existing != null) {
                TradeOrder o =
                        orderMapper.findByUserAndOrder(OrderSharding.slot(userId), userId, existing);
                if (o == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key conflict");
                }
                return toDetail(o);
            }
        }

        String lockName = "cart:checkout:" + userId;
        if (!distributedLock.tryLock(lockName, Duration.ofSeconds(30))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not acquire checkout lock");
        }
        try {
            List<CartItemResponse> lines = cartService.list(userId);
            if (lines.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
            }

            record Resolved(ProductSku sku, ProductSpu spu, int qty, int unitPriceCent, String titleSnapshot) {}

            List<Resolved> resolved = new ArrayList<>();
            int totalCent = 0;
            for (CartItemResponse line : lines) {
                ProductSku sku = skuMapper.findById(line.skuId());
                if (sku == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU not found: " + line.skuId());
                }
                ProductSpu spu = spuMapper.findById(sku.getSpuId());
                if (spu == null || !"ON_SHELF".equals(spu.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not available");
                }
                int unit = sku.getPriceCent() != null ? sku.getPriceCent() : 0;
                if (unit <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid price");
                }
                int qty = line.quantity();
                if (sku.getStock() == null || sku.getStock() < qty) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + spu.getTitle());
                }
                int lineCent = unit * qty;
                totalCent += lineCent;
                String snap = buildTitleSnapshot(spu, sku);
                resolved.add(new Resolved(sku, spu, qty, unit, snap));
            }

            long orderId = BizIds.next();
            try {
                paymentMockClient.mockConfirm(orderId);
            } catch (IllegalStateException e) {
                String m = e.getMessage() != null ? e.getMessage() : "payment_failed";
                String zh =
                        "payment_circuit_open".equals(m)
                                ? "支付熔断，请稍后再试"
                                : ("channel_timeout".equals(m) ? "支付超时，请重试" : m);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, zh);
            }

            TradeOrder order = new TradeOrder();
            order.setId(orderId);
            order.setOrderNo(newOrderNo());
            order.setUserId(userId);
            order.setOrderType("NORMAL");
            order.setTotalCent(totalCent);
            order.setStatus("PAID");
            order.setSeckillActivityId(null);
            int sh = OrderSharding.slot(userId);
            orderMapper.insert(sh, order);

            for (Resolved r : resolved) {
                int u = skuMapper.increaseSoldDecreaseStock(r.sku().getId(), r.qty());
                if (u == 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock (concurrent)");
                }
                TradeOrderItem item = new TradeOrderItem();
                item.setId(BizIds.next());
                item.setOrderId(order.getId());
                item.setUserId(userId);
                item.setSkuId(r.sku().getId());
                item.setQuantity(r.qty());
                item.setUnitPriceCent(r.unitPriceCent());
                item.setTitleSnapshot(r.titleSnapshot());
                orderItemMapper.insert(sh, item);
            }

            Map<Long, Integer> byMerchant = new LinkedHashMap<>();
            for (Resolved r : resolved) {
                Long mid = r.spu().getMerchantId();
                if (mid == null) {
                    continue;
                }
                int lineCent = r.unitPriceCent() * r.qty();
                byMerchant.merge(mid, lineCent, Integer::sum);
            }
            for (Map.Entry<Long, Integer> e : byMerchant.entrySet()) {
                int gross = e.getValue();
                int fee = (int) (gross * 0.01);
                SettlementEntry st = new SettlementEntry();
                st.setOrderId(order.getId());
                st.setMerchantId(e.getKey());
                st.setAmountCent(gross - fee);
                st.setFeeCent(fee);
                st.setStatus("INIT");
                settlementEntryMapper.insert(st);
            }

            FulfillmentTask task = new FulfillmentTask();
            task.setOrderId(order.getId());
            task.setStatus("PENDING");
            fulfillmentTaskMapper.insert(task);

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

            cartService.clear(userId);
            productListCache.evictShelf();

            TradeOrder loaded = orderMapper.findByUserAndOrder(sh, userId, order.getId());
            return toDetail(loaded);
        } finally {
            distributedLock.unlock(lockName);
        }
    }

    private static String buildTitleSnapshot(ProductSpu spu, ProductSku sku) {
        String base = spu.getTitle() != null ? spu.getTitle() : "";
        String spec = SkuSpecSummary.of(sku.getSpecJson());
        if (spec.isEmpty()) {
            return base;
        }
        return base + " · " + spec;
    }

    private static String newOrderNo() {
        long t = System.currentTimeMillis();
        int s = ThreadLocalRandom.current().nextInt(1_000_000);
        return "BC" + t + String.format("%06d", s);
    }
}
