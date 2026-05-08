package com.bcommerce.bootstrap;

import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.mapper.SeckillActivityMapper;
import com.bcommerce.mapper.UserAccountMapper;
import com.bcommerce.marketing.SeckillStockRedisService;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.model.SeckillActivity;
import com.bcommerce.model.UserAccount;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DemoSeedRunner implements ApplicationRunner {

    private final UserAccountMapper userAccountMapper;
    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillStockRedisService seckillStockRedisService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountMapper.countAll() > 0) {
            seckillStockRedisService.reloadAllActivities();
            return;
        }

        UserAccount merchant = new UserAccount();
        merchant.setUsername("merchant");
        merchant.setPasswordHash(passwordEncoder.encode("demo123"));
        merchant.setRole("MERCHANT");
        merchant.setDisplayName("Merchant");
        userAccountMapper.insert(merchant);

        UserAccount buyer = new UserAccount();
        buyer.setUsername("buyer");
        buyer.setPasswordHash(passwordEncoder.encode("demo123"));
        buyer.setRole("CUSTOMER");
        buyer.setDisplayName("Buyer");
        userAccountMapper.insert(buyer);

        ProductSpu spu = new ProductSpu();
        spu.setCategoryId(1L);
        spu.setTitle("ANC Wireless Earbuds Pro");
        spu.setSubtitle("48 小时续航，主动降噪");
        spu.setDetail("Hybrid drivers, ANC, wireless charging case.");
        spu.setMerchantId(merchant.getId());
        spu.setStatus("ON_SHELF");
        productSpuMapper.insert(spu);

        ProductSku sku = new ProductSku();
        sku.setSpuId(spu.getId());
        sku.setSkuCode("SKU-EAR-BLK");
        sku.setSpecJson("{\"color\":\"black\"}");
        sku.setPriceCent(129900);
        sku.setStock(5000);
        productSkuMapper.insert(sku);

        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusDays(7);
        SeckillActivity act = new SeckillActivity();
        act.setSkuId(sku.getId());
        act.setName("Back-to-school flash sale");
        act.setSeckillPriceCent(9900);
        act.setTotalStock(2000);
        act.setSoldStock(0);
        act.setLimitPerUser(2);
        act.setStartTime(start);
        act.setEndTime(end);
        act.setStatus("ONLINE");
        seckillActivityMapper.insert(act);

        seckillStockRedisService.reloadAllActivities();
        log.info("demo seed done: merchant/buyer=demo123, seckill activity id={}", act.getId());
    }
}
