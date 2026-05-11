package com.bcommerce.product;

import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.product.es.ProductSpuEsService;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.web.dto.CreateSkuRequest;
import com.bcommerce.web.dto.CreateSpuRequest;
import com.bcommerce.web.dto.ProductSpuResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantProductService {

    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductListCache productListCache;
    private final ProductSpuEsService productSpuEsService;

    @Transactional
    public ProductSpuResponse createSpu(Long merchantUserId, CreateSpuRequest req) {
        ProductSpu spu = new ProductSpu();
        spu.setCategoryId(req.categoryId());
        spu.setTitle(req.title());
        spu.setSubtitle(req.subtitle());
        spu.setDetail(req.detail());
        spu.setMerchantId(merchantUserId);
        spu.setStatus("ON_SHELF");
        spuMapper.insert(spu);
        productListCache.evictShelf();
        ProductSpu saved = spuMapper.findById(spu.getId());
        try {
            productSpuEsService.saveSpu(saved);
        } catch (Exception e) {
            log.warn("ES 写入 SPU 失败 id={}，搜索可能暂不落索引: {}", saved.getId(), e.toString());
        }
        return ProductSpuResponse.from(saved);
    }

    @Transactional
    public void createSku(Long merchantUserId, Long spuId, CreateSkuRequest req) {
        ProductSpu spu = spuMapper.findById(spuId);
        if (spu == null || !spu.getMerchantId().equals(merchantUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to modify this SPU");
        }
        ProductSku sku = new ProductSku();
        sku.setSpuId(spuId);
        sku.setSkuCode(req.skuCode());
        sku.setSpecJson(req.specJson());
        sku.setPriceCent(req.priceCent());
        sku.setStock(req.stock());
        skuMapper.insert(sku);
        productListCache.evictShelf();
    }
}
