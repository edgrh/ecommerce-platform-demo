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
import java.time.Month;
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

        ProductSpu iphone17Pro = new ProductSpu();
        iphone17Pro.setCategoryId(1L);
        iphone17Pro.setTitle("Apple iPhone 17 Pro");
        iphone17Pro.setSubtitle("A19 Pro · 6.3 英寸超视网膜 XDR · 多存储与配色可选 · Ceramic Shield 2");
        iphone17Pro.setDetail(
                """
                主要参数概览：
                · 芯片：A19 Pro，6 核 CPU + 6 核 GPU
                · 显示屏：6.3 英寸超视网膜 XDR OLED，2622×1206，460 ppi，支持 ProMotion 120Hz 与灵动岛
                · 机身：铝金属一体成型，正反面 Ceramic Shield 2，防水防尘等级 IP68
                · 尺寸重量：约 150×71.9×8.8 mm，约 206 g
                · 影像：多摄组合，支持高帧率 4K 视频与夜景拍摄
                · 下单前请在详情页选择存储容量与颜色（对应不同 SKU）""");
        iphone17Pro.setMerchantId(merchant.getId());
        iphone17Pro.setStatus("ON_SHELF");
        productSpuMapper.insert(iphone17Pro);

        ProductSku iphone17ProSku256Slv = new ProductSku();
        iphone17ProSku256Slv.setSpuId(iphone17Pro.getId());
        iphone17ProSku256Slv.setSkuCode("SKU-IPHONE17-PRO-256-SLV");
        iphone17ProSku256Slv.setSpecJson(
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"256GB\",\"color\":\"silver\",\"colorName\":\"银色\"}");
        iphone17ProSku256Slv.setPriceCent(899900);
        iphone17ProSku256Slv.setStock(600);
        productSkuMapper.insert(iphone17ProSku256Slv);

        ProductSku iphone17ProSku256Gld = new ProductSku();
        iphone17ProSku256Gld.setSpuId(iphone17Pro.getId());
        iphone17ProSku256Gld.setSkuCode("SKU-IPHONE17-PRO-256-GOLD");
        iphone17ProSku256Gld.setSpecJson(
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"256GB\",\"color\":\"gold\",\"colorName\":\"金色\"}");
        iphone17ProSku256Gld.setPriceCent(929900);
        iphone17ProSku256Gld.setStock(500);
        productSkuMapper.insert(iphone17ProSku256Gld);

        ProductSku iphone17ProSku512Slv = new ProductSku();
        iphone17ProSku512Slv.setSpuId(iphone17Pro.getId());
        iphone17ProSku512Slv.setSkuCode("SKU-IPHONE17-PRO-512-SLV");
        iphone17ProSku512Slv.setSpecJson(
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"512GB\",\"color\":\"silver\",\"colorName\":\"银色\"}");
        iphone17ProSku512Slv.setPriceCent(999900);
        iphone17ProSku512Slv.setStock(400);
        productSkuMapper.insert(iphone17ProSku512Slv);

        ProductSku iphone17ProSku512Blk = new ProductSku();
        iphone17ProSku512Blk.setSpuId(iphone17Pro.getId());
        iphone17ProSku512Blk.setSkuCode("SKU-IPHONE17-PRO-512-BLK");
        iphone17ProSku512Blk.setSpecJson(
                "{\"model\":\"iPhone 17 Pro\",\"storage\":\"512GB\",\"color\":\"space_black\",\"colorName\":\"深空黑色\"}");
        iphone17ProSku512Blk.setPriceCent(1029900);
        iphone17ProSku512Blk.setStock(350);
        productSkuMapper.insert(iphone17ProSku512Blk);

        ProductSpu iphone17Pm = new ProductSpu();
        iphone17Pm.setCategoryId(1L);
        iphone17Pm.setTitle("Apple iPhone 17 Pro Max 256GB 深蓝色");
        iphone17Pm.setSubtitle("A19 Pro · 6.9 英寸超视网膜 XDR · 更大电池与机身");
        iphone17Pm.setDetail(
                """
                主要参数概览：
                · 芯片：A19 Pro，6 核 CPU + 6 核 GPU
                · 显示屏：6.9 英寸超视网膜 XDR OLED，2868×1320，460 ppi，支持 ProMotion 120Hz 与灵动岛
                · 机身：铝金属一体成型，Ceramic Shield 2，防水防尘等级 IP68，本款为深蓝色
                · 尺寸重量：约 163.4×78×8.75 mm，约 233 g
                · 影像：多摄组合，支持长焦变焦与高倍数字变焦
                · 存储：256GB（本 SKU）""");
        iphone17Pm.setMerchantId(merchant.getId());
        iphone17Pm.setStatus("ON_SHELF");
        productSpuMapper.insert(iphone17Pm);

        ProductSku iphone17PmSku = new ProductSku();
        iphone17PmSku.setSpuId(iphone17Pm.getId());
        iphone17PmSku.setSkuCode("SKU-IPHONE17-PM-256-DBL");
        iphone17PmSku.setSpecJson("{\"model\":\"iPhone 17 Pro Max\",\"storage\":\"256GB\",\"color\":\"deep_blue\"}");
        iphone17PmSku.setPriceCent(999900);
        iphone17PmSku.setStock(400);
        productSkuMapper.insert(iphone17PmSku);

        ProductSpu airpodsPro3 = new ProductSpu();
        airpodsPro3.setCategoryId(1L);
        airpodsPro3.setTitle("Apple AirPods Pro 3");
        airpodsPro3.setSubtitle("主动降噪 · 自适应音频 · USB-C / MagSafe 充电盒");
        airpodsPro3.setDetail(
                """
                主要卖点：
                · 入耳式设计，支持主动降噪、通透模式与自适应音频
                · 充电盒为 USB-C 接口，兼容主流无线充电方式
                · 抗汗抗水，适合通勤与日常运动佩戴
                · 与 iPhone、iPad、Mac 等设备间支持自动切换""");
        airpodsPro3.setMerchantId(merchant.getId());
        airpodsPro3.setStatus("ON_SHELF");
        productSpuMapper.insert(airpodsPro3);

        ProductSku airpodsPro3Sku = new ProductSku();
        airpodsPro3Sku.setSpuId(airpodsPro3.getId());
        airpodsPro3Sku.setSkuCode("SKU-AIRPODS-PRO3-USBC");
        airpodsPro3Sku.setSpecJson("{\"series\":\"AirPods Pro\",\"generation\":3,\"connector\":\"USB-C\"}");
        airpodsPro3Sku.setPriceCent(199900);
        airpodsPro3Sku.setStock(3500);
        productSkuMapper.insert(airpodsPro3Sku);

        ProductSpu airpods4 = new ProductSpu();
        airpods4.setCategoryId(1L);
        airpods4.setTitle("Apple AirPods 4");
        airpods4.setSubtitle("开放式佩戴 · 可选主动降噪款 · 空间音频");
        airpods4.setDetail(
                """
                主要卖点：
                · 半入耳结构，长时间佩戴更为轻松
                · 提供支持主动降噪与通透模式的版本可选
                · 支持空间音频与个性化调音体验
                · 充电盒采用 USB-C 接口，可搭配无线充电配件使用""");
        airpods4.setMerchantId(merchant.getId());
        airpods4.setStatus("ON_SHELF");
        productSpuMapper.insert(airpods4);

        ProductSku airpods4Sku = new ProductSku();
        airpods4Sku.setSpuId(airpods4.getId());
        airpods4Sku.setSkuCode("SKU-AIRPODS-4-ANC");
        airpods4Sku.setSpecJson("{\"series\":\"AirPods 4\",\"feature\":\"active_noise_cancellation\"}");
        airpods4Sku.setPriceCent(139900);
        airpods4Sku.setStock(4200);
        productSkuMapper.insert(airpods4Sku);

        ProductSpu airpodsMax = new ProductSpu();
        airpodsMax.setCategoryId(1L);
        airpodsMax.setTitle("Apple AirPods Max 午夜色 USB-C");
        airpodsMax.setSubtitle("头戴式 · 计算音频 · 通透与主动降噪");
        airpodsMax.setDetail(
                """
                主要卖点：
                · 头戴式耳罩，结合计算音频实现主动降噪与通透模式
                · 配备数码旋钮调节音量与播放，耳垫采用记忆海绵材质
                · 搭配耳机套可进入低功耗状态，充电接口为 USB-C
                · 适合长时间办公、学习与观影场景""");
        airpodsMax.setMerchantId(merchant.getId());
        airpodsMax.setStatus("ON_SHELF");
        productSpuMapper.insert(airpodsMax);

        ProductSku airpodsMaxSku = new ProductSku();
        airpodsMaxSku.setSpuId(airpodsMax.getId());
        airpodsMaxSku.setSkuCode("SKU-AIRPODS-MAX-MIDN-USBC");
        airpodsMaxSku.setSpecJson("{\"series\":\"AirPods Max\",\"color\":\"midnight\",\"connector\":\"USB-C\"}");
        airpodsMaxSku.setPriceCent(399900);
        airpodsMaxSku.setStock(800);
        productSkuMapper.insert(airpodsMaxSku);

        ProductSpu earpods = new ProductSpu();
        earpods.setCategoryId(1L);
        earpods.setTitle("Apple EarPods USB-C");
        earpods.setSubtitle("有线入耳 · USB-C 接口 · 线控与麦克风");
        earpods.setDetail(
                """
                主要卖点：
                · 经典有线入耳造型，USB-C 接口，可直接连接支持该接口的手机和平板
                · 线控按键可完成调节音量、接听挂断、播放暂停等操作
                · 无需充电，适合作为随身备用耳机或长时间语音通话使用""");
        earpods.setMerchantId(merchant.getId());
        earpods.setStatus("ON_SHELF");
        productSpuMapper.insert(earpods);

        ProductSku earpodsSku = new ProductSku();
        earpodsSku.setSpuId(earpods.getId());
        earpodsSku.setSkuCode("SKU-EARPODS-USBC");
        earpodsSku.setSpecJson("{\"series\":\"EarPods\",\"connector\":\"USB-C\"}");
        earpodsSku.setPriceCent(14900);
        earpodsSku.setStock(8000);
        productSkuMapper.insert(earpodsSku);

        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.of(2026, Month.JUNE, 18, 23, 59);

        SeckillActivity act1 = new SeckillActivity();
        act1.setSkuId(iphone17ProSku256Slv.getId());
        act1.setName("iPhone 17 Pro 限时秒杀");
        act1.setSeckillPriceCent(799900);
        act1.setTotalStock(200);
        act1.setSoldStock(0);
        act1.setLimitPerUser(2);
        act1.setStartTime(start);
        act1.setEndTime(end);
        act1.setStatus("ONLINE");
        seckillActivityMapper.insert(act1);

        SeckillActivity act2 = new SeckillActivity();
        act2.setSkuId(iphone17PmSku.getId());
        act2.setName("iPhone 17 Pro Max 周末闪购");
        act2.setSeckillPriceCent(899900);
        act2.setTotalStock(120);
        act2.setSoldStock(0);
        act2.setLimitPerUser(1);
        act2.setStartTime(start);
        act2.setEndTime(end);
        act2.setStatus("ONLINE");
        seckillActivityMapper.insert(act2);

        SeckillActivity act3 = new SeckillActivity();
        act3.setSkuId(airpodsPro3Sku.getId());
        act3.setName("AirPods Pro 3 专场秒杀");
        act3.setSeckillPriceCent(149900);
        act3.setTotalStock(500);
        act3.setSoldStock(0);
        act3.setLimitPerUser(2);
        act3.setStartTime(start);
        act3.setEndTime(end);
        act3.setStatus("ONLINE");
        seckillActivityMapper.insert(act3);

        seckillStockRedisService.reloadAllActivities();
        log.info(
                "demo seed done: merchant/buyer=demo123, seckill activities={}, {}, {}",
                act1.getId(),
                act2.getId(),
                act3.getId());
    }
}
