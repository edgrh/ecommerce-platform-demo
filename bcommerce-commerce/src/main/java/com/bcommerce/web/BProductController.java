package com.bcommerce.web;

import com.bcommerce.product.MerchantProductService;
import com.bcommerce.security.SecuritySupport;
import com.bcommerce.web.dto.CreateSkuRequest;
import com.bcommerce.web.dto.CreateSpuRequest;
import com.bcommerce.web.dto.ProductSpuResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/b")
@RequiredArgsConstructor
public class BProductController {

    private final MerchantProductService merchantProductService;

    @PostMapping("/spus")
    public ProductSpuResponse createSpu(@Valid @RequestBody CreateSpuRequest req) {
        var u = SecuritySupport.requireUser();
        return merchantProductService.createSpu(u.id(), req);
    }

    @PostMapping("/spus/{spuId}/skus")
    public void createSku(@PathVariable long spuId, @Valid @RequestBody CreateSkuRequest req) {
        var u = SecuritySupport.requireUser();
        merchantProductService.createSku(u.id(), spuId, req);
    }
}
