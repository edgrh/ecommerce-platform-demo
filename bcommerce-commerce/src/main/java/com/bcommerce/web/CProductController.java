package com.bcommerce.web;

import com.bcommerce.product.ProductCatalogService;
import com.bcommerce.web.dto.ProductSpuDetailResponse;
import com.bcommerce.web.dto.ProductSpuResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/c/products")
@RequiredArgsConstructor
public class CProductController {

    private final ProductCatalogService productCatalogService;

    @GetMapping
    public List<ProductSpuResponse> list(@RequestParam(defaultValue = "50") int limit) {
        return productCatalogService.listShelf(Math.min(limit, 100));
    }

    @GetMapping("/search")
    public List<ProductSpuResponse> search(
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "50") int limit) {
        return productCatalogService.search(q, Math.min(limit, 100));
    }

    @GetMapping("/{id}")
    public ProductSpuDetailResponse detail(@PathVariable long id) {
        return productCatalogService.getShelfDetail(id);
    }
}
