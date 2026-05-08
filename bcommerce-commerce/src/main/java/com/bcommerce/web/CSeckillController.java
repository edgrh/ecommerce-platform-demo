package com.bcommerce.web;

import com.bcommerce.marketing.SeckillCatalogService;
import com.bcommerce.security.AuthenticatedUser;
import com.bcommerce.security.SecuritySupport;
import com.bcommerce.trade.SeckillOrderService;
import com.bcommerce.web.dto.OrderDetailResponse;
import com.bcommerce.web.dto.SeckillActivityResponse;
import com.bcommerce.web.dto.SeckillOrderRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/c/seckill")
@RequiredArgsConstructor
public class CSeckillController {

    private final SeckillCatalogService seckillCatalogService;
    private final SeckillOrderService seckillOrderService;

    @GetMapping("/activities")
    public List<SeckillActivityResponse> activities() {
        return seckillCatalogService.listOnline();
    }

    @PostMapping("/orders")
    public OrderDetailResponse order(
            @Valid @RequestBody SeckillOrderRequest req,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idem) {
        AuthenticatedUser u = SecuritySupport.requireUser();
        if (!"CUSTOMER".equalsIgnoreCase(u.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customer accounts may place orders");
        }
        return seckillOrderService.placeSeckill(u.id(), req, idem);
    }
}
