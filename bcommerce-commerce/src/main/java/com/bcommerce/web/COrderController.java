package com.bcommerce.web;

import com.bcommerce.mapper.RiskAuditTaskMapper;
import com.bcommerce.security.SecuritySupport;
import com.bcommerce.trade.SeckillOrderService;
import com.bcommerce.web.dto.OrderDetailResponse;
import com.bcommerce.web.dto.RiskAuditTaskResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/c/orders")
@RequiredArgsConstructor
public class COrderController {

    private final SeckillOrderService seckillOrderService;
    private final RiskAuditTaskMapper riskAuditTaskMapper;

    @GetMapping
    public List<OrderDetailResponse> list() {
        var u = SecuritySupport.requireUser();
        return seckillOrderService.listUserOrders(u.id());
    }

    @GetMapping("/{id}")
    public OrderDetailResponse detail(@PathVariable long id) {
        var u = SecuritySupport.requireUser();
        return seckillOrderService.getOrder(u.id(), id);
    }

    @GetMapping("/{orderId}/risk")
    public List<RiskAuditTaskResponse> riskTrail(@PathVariable long orderId) {
        var u = SecuritySupport.requireUser();
        return riskAuditTaskMapper.listByOrderAndUser(orderId, u.id()).stream()
                .map(RiskAuditTaskResponse::from)
                .toList();
    }
}
