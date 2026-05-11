package com.bcommerce.web;

import com.bcommerce.cart.CartService;
import com.bcommerce.security.SecuritySupport;
import com.bcommerce.trade.CartOrderService;
import com.bcommerce.web.dto.CartAddRequest;
import com.bcommerce.web.dto.CartItemResponse;
import com.bcommerce.web.dto.CartSetQtyRequest;
import com.bcommerce.web.dto.OrderDetailResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/c/cart")
@RequiredArgsConstructor
public class CCartController {

    private final CartService cartService;
    private final CartOrderService cartOrderService;

    @GetMapping
    public List<CartItemResponse> list() {
        var u = SecuritySupport.requireUser();
        SecuritySupport.requireRole(u, "CUSTOMER");
        return cartService.list(u.id());
    }

    @PostMapping("/add")
    public void add(@Valid @RequestBody CartAddRequest req) {
        var u = SecuritySupport.requireUser();
        SecuritySupport.requireRole(u, "CUSTOMER");
        cartService.add(u.id(), req.skuId(), req.quantity());
    }

    @PostMapping("/set")
    public void set(@Valid @RequestBody CartSetQtyRequest req) {
        var u = SecuritySupport.requireUser();
        SecuritySupport.requireRole(u, "CUSTOMER");
        cartService.setQty(u.id(), req.skuId(), req.quantity());
    }

    @PostMapping("/remove")
    public void remove(@Valid @RequestBody CartSetQtyRequest req) {
        var u = SecuritySupport.requireUser();
        SecuritySupport.requireRole(u, "CUSTOMER");
        cartService.remove(u.id(), req.skuId());
    }

    @DeleteMapping
    public void clear() {
        var u = SecuritySupport.requireUser();
        SecuritySupport.requireRole(u, "CUSTOMER");
        cartService.clear(u.id());
    }

    /** Mock payment (same Resilience4j path as秒杀) then create NORMAL order, clear cart. */
    @PostMapping("/checkout")
    public OrderDetailResponse checkout(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idemKey) {
        var u = SecuritySupport.requireUser();
        SecuritySupport.requireRole(u, "CUSTOMER");
        return cartOrderService.checkoutFromCart(u.id(), idemKey);
    }
}

