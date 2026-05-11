package com.bcommerce.bootstrap;

import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.product.ProductListCache;
import com.bcommerce.product.es.ProductSpuEsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Older demo DBs shipped a single SKU titled "Apple iPhone 17 Pro 256GB 银色". Migrate to one SPU
 * with multiple SKUs (storage × color) so the storefront can offer configuration choices.
 */
@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class Iphone17ProSkusMigrationRunner implements ApplicationRunner {

    private static final String ANCHOR_SKU = "SKU-IPHONE17-PRO-256-SLV";

    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;
    private final ProductListCache productListCache;
    private final ProductSpuEsService productSpuEsService;

    @Override
    public void run(ApplicationArguments args) {
        ProductSku anchor = skuMapper.findBySkuCode(ANCHOR_SKU);
        if (anchor == null || anchor.getSpuId() == null) {
            return;
        }
        long spuId = anchor.getSpuId();
        List<ProductSku> existing = skuMapper.findBySpuId(spuId);
        if (existing.size() >= 4) {
            return;
        }
        ProductSpu spu = spuMapper.findById(spuId);
        if (spu == null) {
            return;
        }
        spuMapper.updateTitleAndSubtitle(
                spuId,
                "Apple iPhone 17 Pro",
                "A19 Pro · 6.3 英寸超视网膜 XDR · 多存储与配色可选 · Ceramic Shield 2");
        insertIfMissing(
                spuId,
                "SKU-IPHONE17-PRO-256-GOLD",
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"256GB\",\"color\":\"gold\",\"colorName\":\"金色\"}",
                929900,
                500);
        insertIfMissing(
                spuId,
                "SKU-IPHONE17-PRO-512-SLV",
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"512GB\",\"color\":\"silver\",\"colorName\":\"银色\"}",
                999900,
                400);
        insertIfMissing(
                spuId,
                "SKU-IPHONE17-PRO-512-BLK",
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"512GB\",\"color\":\"space_black\",\"colorName\":\"深空黑色\"}",
                1029900,
                350);
        productListCache.evictShelf();
        ProductSpu refreshed = spuMapper.findById(spuId);
        try {
            productSpuEsService.saveSpu(refreshed);
        } catch (Exception e) {
            log.warn("Elasticsearch saveSpu after iPhone SKU migration: {}", e.toString());
        }
        log.info("iPhone 17 Pro SKU migration applied for spuId={}", spuId);
    }

    private void insertIfMissing(long spuId, String code, String specJson, int priceCent, int stock) {
        if (skuMapper.findBySkuCode(code) != null) {
            return;
        }
        ProductSku s = new ProductSku();
        s.setSpuId(spuId);
        s.setSkuCode(code);
        s.setSpecJson(specJson);
        s.setPriceCent(priceCent);
        s.setStock(stock);
        skuMapper.insert(s);
    }
}
