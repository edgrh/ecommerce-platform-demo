package com.bcommerce.web;

import com.bcommerce.mapper.TradeOrderItemMapper;
import com.bcommerce.mapper.TradeOrderMapper;
import com.bcommerce.sharding.OrderSharding;
import com.bcommerce.model.TradeOrder;
import com.bcommerce.model.TradeOrderItem;
import com.bcommerce.security.SecuritySupport;
import com.bcommerce.web.dto.OrderDetailResponse;
import com.bcommerce.web.dto.OrderItemResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/b/orders")
@RequiredArgsConstructor
public class BOrderController {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;

    @GetMapping
    public List<OrderDetailResponse> list() {
        var u = SecuritySupport.requireUser();
        return orderMapper.listByMerchant(u.id(), 100).stream().map(this::toDetail).toList();
    }

    private OrderDetailResponse toDetail(TradeOrder o) {
        List<TradeOrderItem> items =
                orderItemMapper.listByUserAndOrder(OrderSharding.slot(o.getUserId()), o.getUserId(), o.getId());
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
}
