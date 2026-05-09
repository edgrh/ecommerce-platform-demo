# 电商秒杀演示项目

**换机 / 新环境**：先看 [`docs/SETUP-OTHER-MACHINE.md`](docs/SETUP-OTHER-MACHINE.md)。

**环境版本**：JDK **17**、Maven **≥ 3.6.3**、Docker（Compose v2）、前端 **Node ≥ 18**。构建时 Enforcer 会校验 JDK/Maven。

Maven 多模块：

- **`bcommerce-gateway`**：Spring Cloud Gateway（:8080），Redis 限流 + 熔断回退。
- **`bcommerce-commerce`**：业务服务（:8081），MyBatis + MySQL + Redis + RabbitMQ。

业务域按职责拆分为：**商品、交易、营销（秒杀）、履约、结算、风控（异步）**。  
主交易链路（登录 -> 浏览商品 -> 秒杀下单 -> 订单查询）在本仓库可直接跑通，异步链路用于履约与风控任务落库，便于演示同步/异步边界。

表结构：**12 张物理表**（订单与明细按 `user_id%2` 分表，见 `docs/ARCHITECTURE.md`）。

## 一键启动（推荐）

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File .\scripts\one-click.ps1
```

脚本会自动完成：

1. K8s 部署/更新（Docker Desktop Kubernetes）
2. 后台启动网关 `port-forward`（`localhost:8080`）
3. 前台启动前端 `npm run dev`（`http://localhost:5173`）

仅启动后端（不启动前端）：

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File .\scripts\one-click.ps1 -SkipFrontend
```

## 启动

在**仓库根目录**（克隆后的 `ecommerce-platform-demo` 路径）执行：

```bash
docker compose up -d
mvn -q -DskipTests package
```

Windows PowerShell 同样适用；若 `docker` / `mvn` 不在 PATH，请先安装并重启终端。

Compose 包含 **MySQL、Redis、RabbitMQ、Elasticsearch（9200）**。ES 镜像内置 **IK 中文分词**（见 `docker/elasticsearch/Dockerfile`）；**首次或改 Dockerfile 后**需执行 `docker compose build elasticsearch` 再 `up`。商品 **关键词检索** 走 ES；未启动 ES 时会回退 MySQL `LIKE`（见日志告警）。

**Kubernetes**：首次部署用 `scripts\k8s-kind-up.ps1`（详见 [`docs/K8S.md`](docs/K8S.md)）。后续改 Java/配置用 `scripts\k8s-redeploy.ps1` 做滚动更新。

不要在同一轮命令里紧跟 `docker compose down`，否则会立刻删掉刚启动的容器。需要停服务时再执行 `docker compose down`。

若拉镜像慢或失败：`docker-compose.yml` 与 `k8s/` 使用 **Docker Hub** 官方镜像名。可在 Docker Desktop → **Settings → Docker Engine** 配置 `registry-mirrors`（使用你账号下的阿里云等镜像加速）；若某镜像站返回 **401**，请换镜像源或暂时移除该 mirror。

若构建时报 **`auth.docker.io` 连接超时 / `connectex ... failed`**：说明本机到 Docker Hub 认证服务不通（常见于国内网络）。请任选：**全局代理 / VPN**；或在阿里云控制台开通 **容器镜像服务 ACR 的镜像加速器**，把控制台给你的专属 `https://xxxx.mirror.aliyuncs.com` 写入 Docker Engine 的 `registry-mirrors` 后 **Apply & restart**，再执行 `docker build` / `docker compose pull`。

MySQL 在 compose 里映射为 **本机 3308**（若 3307 被占用）。`bcommerce-commerce` 的 `spring.datasource.url` 已对应 `localhost:3308`。

Redis 在 compose 里映射为 **本机 6380**（避免与本机已有 Redis 占用的 6379 冲突）。若你停掉本机 Redis 并改回 `6379:6379`，请同时把两个模块 `application.yml` 里的 `spring.data.redis.port` 改回 `6379`。

终端 1（业务）：

```bash
java -jar bcommerce-commerce/target/bcommerce-commerce-1.0.0-SNAPSHOT.jar
```

终端 2（网关，需 Redis）：

```bash
java -jar bcommerce-gateway/target/bcommerce-gateway-1.0.0-SNAPSHOT.jar
```

Windows 下路径分隔符也可用反斜杠 `\`。

开发期也可用：

```bash
mvn -pl bcommerce-commerce spring-boot:run
mvn -pl bcommerce-gateway spring-boot:run
```

前端 `frontend`：先 `npm ci` 或 `npm install`，再 `npm run dev`，请求经 **8080 网关** 转发到业务服务。

## 账号

- 商家 `merchant` / `demo123`
- 买家 `buyer` / `demo123`

## 调试与压测脚本

```powershell
powershell -ExecutionPolicy Bypass -File scripts\debug-api.ps1
```

Linux / macOS：

```bash
chmod +x scripts/debug-api.sh
./scripts/debug-api.sh http://127.0.0.1:8080
```

压测（本地脚本）：

```powershell
pip install -r scripts\requirements.txt
python scripts\stress_seckill.py
```

压测（K8s 集群内 k6，默认不随部署自动创建）：

```powershell
kubectl -n bcommerce apply -f perf/k6-job.yaml
kubectl -n bcommerce wait --for=condition=complete job/k6-activities --timeout=240s
kubectl -n bcommerce logs job/k6-activities
```

## 文档

- **语言约定**：`README.md` 与 `docs/*.md` 为**中文**；前端界面为**中文**；**Java 接口错误信息、SQL 种子文案、压测脚本输出**仍为**英文**。
- `docs/ARCHITECTURE.md`：架构、核心链路、模块说明。
- `docs/PROJECT-FILES.md`：全仓库主要文件与代码用途索引。
- `docs/K8S.md`：本地 Kubernetes（kind / Docker Desktop）部署步骤；脚本 `scripts/k8s-kind-up.ps1`（`-DockerDesktop` 可走 Docker Desktop 内置 K8s，无需 kind）。
- `docs/SETUP-OTHER-MACHINE.md`：新电脑运行检查清单与端口说明。
- `docs/COURSE-PROJECT-MANUAL.md`：答辩可用的报告模板与讲解提纲（可按个人风格改写）。

## 注意

- Maven 报 **`Unable to rename ... jar.original`**（多见于 Windows）：先运行 `scripts\stop-local-bcommerce.ps1`，或 Linux/macOS 下 `./scripts/stop-local-bcommerce.sh`；并关闭 IDE 内正在运行的 Boot；详见 `docs/K8S.md`。
- 若本机 Maven 使用阿里云镜像且缺少部分依赖，可在 `settings.xml` 增加中央仓库或直连 Maven Central。
- 源码与 Markdown 均为 **UTF-8**；请用支持 UTF-8 的编辑器打开，勿用 GBK 解码，否则易出方框乱码。
- Windows 下若编译报编码错，请确认 IDE / Maven 使用 UTF-8。
- 若曾拉过旧版 schema，升级分表后请 **`docker compose down -v`** 或手动删库再执行 `schema.sql`，避免表名不一致。
