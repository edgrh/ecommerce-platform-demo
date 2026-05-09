package com.bcommerce.marketing;

import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.mapper.SeckillActivityMapper;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.model.SeckillActivity;
import com.bcommerce.web.dto.SeckillActivityResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeckillCatalogService {

    private final SeckillActivityMapper activityMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;

    public List<SeckillActivityResponse> listOnline() {
        return activityMapper.listOnline().stream().map(this::toResponse).toList();
    }

    private SeckillActivityResponse toResponse(SeckillActivity a) {
        String title = "";
        long spuId = 0L;
        Long skuId = a.getSkuId();
        if (skuId != null) {
            ProductSku sku = skuMapper.findById(skuId);
            if (sku != null) {
                ProductSpu spu = spuMapper.findById(sku.getSpuId());
                if (spu != null) {
                    title = spu.getTitle();
                    spuId = spu.getId();
                } else {
                    title = sku.getSkuCode();
                }
            }
        }
        return SeckillActivityResponse.from(a, title, spuId);
    }
}
