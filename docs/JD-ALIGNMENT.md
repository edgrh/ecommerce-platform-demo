# 与电商交易 / 中台 / 秒杀 / 治理岗位描述的对应说明

本项目为**可运行的演示工程**，用于简历与面试中**结构化讲述**，不宣称已达到生产环境上万 QPS；可在文档与答辩中说明**目标架构、扩展方式与压测计划**（见 `docs/ARCHITECTURE.md`）。

## 职责 1：业务中台（商品 / 交易 / 营销 / 履约 / 结算 / 风控）

| 中台域 | 本仓库体现 |
|--------|------------|
| 商品 | `product`：SPU/SKU、货架列表 Redis 缓存与失效 |
| 交易 | `trade`：秒杀下单、分表订单、幂等、支付 mock |
| 营销 | `marketing`：活动库存 Redis + Lua、限购 |
| 履约 | `fulfillment`：MQ `order.created`、运单回写 |
| 结算 | `settlement`：入账与平台费流水 |
| 风控 | `risk`：MQ `order.risk`、审核任务表；`GET /api/c/orders/{id}/risk` |

## 职责 2：高并发秒杀

- Redis **Lua** 预减活动库存；DB **条件更新**活动与 SKU 库存，同事务回滚路径释放 Redis。
- **网关限流** + 业务侧 **分钟窗口** + **分布式锁**（同用户互斥）+ **幂等**。
- **订单分表**：`bc_trade_order_{0,1}` / `bc_trade_order_item_{0,1}`，`user_id % 2` 路由（`OrderSharding`），可替换为 ShardingSphere。
- 压测：`scripts/stress_seckill.py`（经 **8080 网关**）。

## 职责 3：分布式治理

- **Trace**：`TraceIdFilter` + MDC；响应头 `X-Trace-Id`。
- **限流**：Gateway **RequestRateLimiter**（Redis）；业务秒杀计数。
- **熔断**：Gateway 到 commerce；`PaymentMockClient` 支付抖动。
- **MQ 削峰**：履约与风控异步消费，缩短同步链路。
- **微服务**：`bcommerce-gateway` + `bcommerce-commerce` 双进程。

## 职责 4：垂直电商 B / C

- **C**：`/api/c/**`；**B**：`/api/b/**`（`MERCHANT` JWT）。

## 任职要求映射

- **Spring Boot / Cloud**：Gateway 模块 + Boot 业务模块。
- **MyBatis**：注解 + XML（分表 `${shard}` 仅允许 0/1）。
- **MySQL / Redis / MQ**：docker-compose。

## 诚信表述建议

- 对 10k+ QPS 表述建议写为：**水平扩展、缓存、异步化、分片后的容量规划**，附本地压测与监控口径。
- 区分 Demo 与线上真实峰值数据。
