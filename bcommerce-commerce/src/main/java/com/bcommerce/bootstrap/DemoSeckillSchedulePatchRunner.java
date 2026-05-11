package com.bcommerce.bootstrap;

import com.bcommerce.mapper.SeckillActivityMapper;
import com.bcommerce.marketing.SeckillStockRedisService;
import java.time.LocalDateTime;
import java.time.Month;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 将仍为 ONLINE 的秒杀活动结束时间统一到固定日期，并刷新 Redis 库存键。 */
@Component
@Order(11)
@RequiredArgsConstructor
@Slf4j
public class DemoSeckillSchedulePatchRunner implements ApplicationRunner {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillStockRedisService seckillStockRedisService;

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime end = LocalDateTime.of(2026, Month.JUNE, 18, 23, 59, 0);
        int n = seckillActivityMapper.extendOnlineEndTime(end);
        if (n > 0) {
            log.info("已更新 {} 条 ONLINE 秒杀活动结束时间为 {}", n, end);
        }
        seckillStockRedisService.reloadAllActivities();
    }
}
