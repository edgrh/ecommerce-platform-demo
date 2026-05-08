package com.bcommerce.product;

import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.product.es.ProductSpuEsService;
import com.bcommerce.web.dto.ProductSpuResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductCatalogService {

    private final ProductSpuMapper spuMapper;
    private final ProductListCache productListCache;
    private final ProductSpuEsService productSpuEsService;

    public List<ProductSpuResponse> listShelf(int limit) {
        int cap = Math.min(limit, 100);
        List<ProductSpuResponse> cached = productListCache.getShelf();
        if (cached != null) {
            return cached.size() > cap ? cached.subList(0, cap) : cached;
        }
        List<ProductSpuResponse> list = spuMapper.listOnShelf(cap).stream().map(ProductSpuResponse::from).toList();
        try {
            productListCache.putShelf(list);
        } catch (Exception e) {
            log.warn("product cache write failed: {}", e.toString());
        }
        return list;
    }

    public List<ProductSpuResponse> search(String q, int limit) {
        if (q == null || q.isBlank()) {
            return listShelf(limit);
        }
        int cap = Math.min(limit, 100);
        String term = q.trim();
        try {
            return productSpuEsService.search(term, cap);
        } catch (Exception e) {
            log.warn("elasticsearch search failed, fallback to mysql like: {}", e.toString());
            return spuMapper.searchByKeyword(term, cap).stream().map(ProductSpuResponse::from).toList();
        }
    }
}
