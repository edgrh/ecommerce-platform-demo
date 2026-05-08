# 项目文件与代码用途说明

本文档说明 `ecommerce-platform-demo` 仓库中主要目录与文件的用途（**文档与前端界面为中文**；**Java 接口错误信息、SQL 种子文案、压测脚本输出为英文**）。`**/target/`** 为 Maven 编译产物，可删除，不必纳入作业说明。

---

## 根目录

| 文件 | 用途 |
|------|------|
| `pom.xml` | 父工程：聚合 `bcommerce-gateway`、`bcommerce-commerce`，统一 Spring Boot / Cloud 版本与编码；**Enforcer** 校验 JDK 17 / Maven 3.6.3+。 |
| `README.md` | 启动顺序、端口、账号、脚本与文档入口。 |
| `.gitignore` | 忽略 `target/`、`node_modules/`、IDE 文件、日志、`.env` 等。 |
| `.gitattributes` | 换行策略（如 `*.sh` 使用 LF），减少跨 OS 克隆后脚本损坏。 |
| `docker-compose.yml` | 一键启动 MySQL（本机端口见 compose，默认 3308）、Redis（6380）、RabbitMQ，供本地联调。 |

---

## `docs/`

| 文件 | 用途 |
|------|------|
| `ARCHITECTURE.md` | 系统架构、模块划分、分表与高并发设计要点。 |
| `JD-ALIGNMENT.md` | 岗位关键词与仓库能力的对应说明。 |
| `COURSE-PROJECT-MANUAL.md` | 课程大作业结构、答辩题参考（可选）。 |
| `SETUP-OTHER-MACHINE.md` | 新电脑/新环境：版本、端口、启动顺序、跨平台说明。 |
| `K8S.md` | 本地 Kubernetes（kind 等）部署与故障排查。 |
| `ES-NOTE.md` | Elasticsearch 商品检索与 IK 分词说明。 |
| `PROJECT-FILES.md` | 本文件：全项目文件用途索引。 |

---

## `k8s/`

| 文件 | 用途 |
|------|------|
| `01-namespace.yaml` ~ `07-gateway.yaml` | 命名空间、MySQL/Redis/RabbitMQ/ES、commerce、gateway（NodePort 30080）。 |

## `scripts/`

| 文件 | 用途 |
|------|------|
| `k8s-kind-up.ps1` | 打包、构建镜像、kind 加载镜像、`kubectl apply -f k8s/`（Windows）。 |
| `stop-local-bcommerce.ps1` | Windows：结束占用 8080/8081 的进程，避免 Maven repackage 无法改名 jar。 |
| `stop-local-bcommerce.sh` | Linux/macOS：同上（需 `lsof` 或 `fuser`）。 |
| `debug-api.ps1` | Windows：经网关冒烟（健康检查、登录、商品、秒杀活动、订单）。 |
| `debug-api.sh` | Linux/macOS：同上，curl 版。 |
| `stress_seckill.py` | 多账号并发秒杀压测（默认请求网关）。 |
| `requirements.txt` | 压测脚本 Python 依赖（如 `requests`）。 |

---

## `bcommerce-gateway/`（API 网关，默认端口 8080）

| 文件 | 用途 |
|------|------|
| `pom.xml` | 网关模块依赖：Spring Cloud Gateway、熔断、Redis 响应式客户端等。 |
| `src/main/java/.../GatewayApplication.java` | 网关启动类。 |
| `src/main/java/.../GatewayBeans.java` | 注册 Bean，例如按客户端 IP 的限流 `KeyResolver`。 |
| `src/main/java/.../FallbackController.java` | 熔断降级时转发到的回退接口。 |
| `src/main/resources/application.yml` | 端口、Redis、路由到业务服务、全局限流、熔断器、CORS、Actuator。 |

---

## `bcommerce-commerce/`（业务服务，默认端口 8081）

### 启动与配置

| 文件 | 用途 |
|------|------|
| `pom.xml` | Web、Security、MyBatis、MySQL、Redis、AMQP、JWT、Resilience4j、Actuator 等依赖。 |
| `src/main/java/.../CommerceApplication.java` | 业务服务启动入口。 |
| `src/main/resources/application.yml` | 数据源、Redis、RabbitMQ、Elasticsearch、MyBatis、JWT、秒杀限流、缓存 TTL、日志 MDC、指标与熔断配置。 |
| `src/main/resources/schema.sql` | 建表与部分种子数据；与 `spring.sql.init` 配合初始化数据库。 |

### `bootstrap/`

| 文件 | 用途 |
|------|------|
| `DemoSeedRunner.java` | 空库时插入演示商家/买家、SPU/SKU、秒杀活动，并刷新 Redis 活动库存。 |
| `ProductEsIndexRunner.java` | 启动时从 MySQL 重建商品 ES 索引（失败仅告警）。 |
| `product/es/*` | `ProductSpuDocument`、ES Repository、`ProductSpuEsService` 索引与检索。 |

### `config/`

| 文件 | 用途 |
|------|------|
| `RabbitConfig.java` | 声明交换机、队列及 `order.created`、`order.risk` 路由绑定。 |
| `BcommerceProperties.java` | 绑定 `bcommerce.*` 自定义配置（JWT、秒杀限流、缓存 TTL）。 |
| `RedisConfig.java` | Redis 序列化、Template 等与 Redis 相关的 Bean。 |

### `security/`

| 文件 | 用途 |
|------|------|
| `SecurityConfig.java` | 无状态会话、CSRF 关闭、CORS、URL 级权限、过滤器顺序。 |
| `JwtAuthenticationFilter.java` | 解析 `Authorization` 中的 JWT，写入 SecurityContext 与 MDC。 |
| `JwtService.java` | JWT 签发与解析。 |
| `AuthenticatedUser.java` | 认证主体（用户 id、用户名、角色）。 |
| `SecuritySupport.java` | Controller 中获取当前登录用户的工具方法。 |

### `governance/`

| 文件 | 用途 |
|------|------|
| `TraceIdFilter.java` | 生成或透传 `traceId`，用于日志与响应头。 |
| `RedisDistributedLock.java` | 基于 Redis 的分布式锁（如同用户并发下单互斥）。 |
| `SeckillRateLimiter.java` | 秒杀接口按用户维度的限流窗口。 |

### `sharding/`

| 文件 | 用途 |
|------|------|
| `OrderSharding.java` | 根据 `userId` 计算订单分表槽位（`userId % 2` 得到 0 或 1）。 |

### `support/`

| 文件 | 用途 |
|------|------|
| `BizIds.java` | 生成订单等业务用的长整型 id。 |

### `web/`（REST 控制器）

| 文件 | 用途 |
|------|------|
| `AuthController.java` | 注册、登录，返回 JWT。 |
| `CProductController.java` | C 端商品列表与搜索。 |
| `CSeckillController.java` | C 端秒杀活动列表、秒杀下单。 |
| `COrderController.java` | C 端订单列表、详情、风控记录 `GET .../orders/{id}/risk`。 |
| `BProductController.java` | B 端商家发布 SPU/SKU。 |
| `BOrderController.java` | B 端商家查询与本店相关的订单。 |
| `RestExceptionHandler.java` | 全局异常转为统一 JSON 错误响应。 |

### `web/dto/`

| 文件 | 用途 |
|------|------|
| `AuthLoginRequest.java` / `AuthRegisterRequest.java` | 登录、注册请求体。 |
| `TokenResponse.java` | 登录成功返回 token 与角色等。 |
| `ProductSpuResponse.java` | 商品列表/搜索返回结构。 |
| `SeckillActivityResponse.java` / `SeckillOrderRequest.java` | 活动展示、秒杀下单请求体。 |
| `OrderDetailResponse.java` / `OrderItemResponse.java` | 订单详情及行项目。 |
| `CreateSpuRequest.java` / `CreateSkuRequest.java` | 商家创建商品请求体。 |
| `RiskAuditTaskResponse.java` | 风控审核记录 API 出参。 |

### `auth/`

| 文件 | 用途 |
|------|------|
| `AuthService.java` | 注册、登录业务逻辑（密码加密、查库、签发 JWT）。 |

### `product/`

| 文件 | 用途 |
|------|------|
| `ProductCatalogService.java` | 上架商品列表、关键词搜索（ES，失败回退 MySQL）；与缓存协作。 |
| `ProductListCache.java` | Redis 缓存商品列表及失效策略。 |
| `MerchantProductService.java` | 商家创建 SPU/SKU、写 ES 索引、权限校验。 |

### `marketing/`

| 文件 | 用途 |
|------|------|
| `SeckillCatalogService.java` | 查询可展示的秒杀活动。 |
| `SeckillStockRedisService.java` | Redis/Lua 维护活动库存镜像、扣减、全量重载等。 |

### `trade/`

| 文件 | 用途 |
|------|------|
| `SeckillOrderService.java` | 秒杀下单主流程：幂等、限流、锁、Redis+DB 库存、写分表订单、发 MQ、结算入账等。 |
| `PaymentMockClient.java` | 模拟支付调用，配合 Resilience4j 演示熔断。 |

### `fulfillment/`

| 文件 | 用途 |
|------|------|
| `FulfillmentMessageListener.java` | 消费 `order.created`，写入或更新履约任务、物流信息等。 |

### `risk/`

| 文件 | 用途 |
|------|------|
| `RiskReviewListener.java` | 消费 `order.risk`，异步写入风控审核任务。 |

### `mapper/`（MyBatis 接口）

| 文件 | 用途 |
|------|------|
| `UserAccountMapper.java` | 用户表读写。 |
| `ProductSpuMapper.java` / `ProductSkuMapper.java` | SPU/SKU 持久化与查询。 |
| `SeckillActivityMapper.java` | 秒杀活动表读写与条件更新库存。 |
| `TradeOrderMapper.java` / `TradeOrderItemMapper.java` | 分表订单与明细的插入、按用户查询、商家跨分片查询。 |
| `IdempotencyMapper.java` | 幂等键与订单关联。 |
| `FulfillmentTaskMapper.java` | 履约任务表。 |
| `SettlementEntryMapper.java` | 结算流水表。 |
| `RiskAuditTaskMapper.java` | 风控审核任务表。 |

### `resources/mapper/*.xml`

| 文件 | 用途 |
|------|------|
| `TradeOrderMapper.xml` / `TradeOrderItemMapper.xml` | 动态表名 `bc_trade_order_${shard}` 等 SQL。 |
| `ProductSpuMapper.xml` | SPU 相关复杂或复用 SQL（若存在）。 |

### `model/`（实体，与表对应）

| 文件 | 用途 |
|------|------|
| `UserAccount.java` | 用户账号实体。 |
| `ProductSpu.java` / `ProductSku.java` | 商品 SPU/SKU 实体。 |
| `SeckillActivity.java` | 秒杀活动实体。 |
| `TradeOrder.java` / `TradeOrderItem.java` | 订单与明细实体。 |
| `FulfillmentTask.java` | 履约任务实体。 |
| `SettlementEntry.java` | 结算流水实体。 |
| `RiskAuditTask.java` | 风控审核任务实体。 |

---

## `frontend/`（Vue 3 + Vite，开发端口 5173）

| 文件 | 用途 |
|------|------|
| `package.json` | 前端依赖与脚本。 |
| `index.html` | 单页应用 HTML 入口。 |
| `vite.config.js` | 开发服务器与 `/api` 代理到网关 8080。 |
| `src/main.js` | 创建应用实例，挂载路由与 Pinia。 |
| `src/App.vue` | 根布局与导航。 |
| `src/router/index.js` | 五个页面路由及登录、商家角色守卫。 |
| `src/stores/auth.js` | 登录态、token、角色。 |
| `src/api/http.js` | HTTP 客户端、基路径、请求头附加 JWT。 |
| `src/views/HomeView.vue` | 商品目录页。 |
| `src/views/SeckillView.vue` | 秒杀页。 |
| `src/views/OrdersView.vue` | 订单列表页。 |
| `src/views/LoginView.vue` | 登录/注册页。 |
| `src/views/MerchantView.vue` | 商家后台页。 |

---

## 与常见作业要求的对应关系

| 要求 | 主要落点 |
|------|----------|
| 数据库表（不少于 8） | `schema.sql` |
| 微服务 + 网关限流/熔断 | `bcommerce-gateway` 模块及 `application.yml` |
| Redis 缓存与锁 | `ProductListCache`、`RedisDistributedLock`、`SeckillStockRedisService` 等 |
| 消息队列削峰 | `RabbitConfig`、`FulfillmentMessageListener`、`RiskReviewListener` |
| 分表 | `OrderSharding`、订单 Mapper 与 XML |
| 前端主页面（不少于 5） | `src/router/index.js` 与五个 `views/*.vue` |

---

## 推荐阅读顺序

1. 根目录 `README.md`：如何启动。  
2. `docs/ARCHITECTURE.md`：架构与模块。  
3. 本文件：按文件定位代码。  
