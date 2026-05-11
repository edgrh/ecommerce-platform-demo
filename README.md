# 电商秒杀演示项目

本仓库提供一套**可本地复现**的电商秒杀链路：买家浏览商品、选择秒杀活动下单、查看订单；商家上架商品并查看关联订单。  
默认配置面向 **16GB 内存笔记本**，优先保证“能一键跑通、方便演示与写报告”。

## 一、运行环境

- **JDK**：17  
- **Maven**：≥ 3.6.3  
- **Docker**：Docker Desktop（Compose v2）  
- **Node.js**：≥ 18（仅前端需要）

换机/新环境常见问题见：`docs/SETUP-OTHER-MACHINE.md`。

## 二、最快启动（推荐，面向答辩演示）

把仓库克隆到任意目录后，在仓库根目录双击或执行：

- 双击 `run-one-click.bat`（Windows）
- 或命令行：

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File .\scripts\one-click.ps1
```

它会按顺序完成：

1. **构建后端 jar**（`mvn -DskipTests package`）
2. **构建镜像**（gateway/commerce/elasticsearch）
3. **部署到 Docker Desktop Kubernetes**（`kubectl apply -f k8s/`）
4. **后台启动网关 port-forward**：`http://127.0.0.1:8080`
5. **前台启动前端 dev server**：`http://localhost:5173`

仅启动后端（不启动前端）：

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File .\scripts\one-click.ps1 -SkipFrontend
```

## 三、手动部署（便于写报告：每一步可截图）

下面按“依赖 -> 后端 -> 前端 -> 验证”的顺序写，适合在报告里描述实现与部署过程。

### 3.1 启动基础设施（Docker Compose）

在仓库根目录执行：

```powershell
docker compose up -d
```

默认会启动：

- MySQL（宿主机端口 **3308**）
- Redis（宿主机端口 **6380**）
- RabbitMQ（5672 / 管理台 15672）
- Elasticsearch（9200，包含 IK）

首次或修改 `docker/elasticsearch/Dockerfile` 后需要先构建 ES：

```powershell
docker compose build elasticsearch
docker compose up -d elasticsearch
```

### 3.2 构建并启动后端（本机运行）

```powershell
mvn -q -DskipTests package
java -jar bcommerce-commerce\\target\\bcommerce-commerce-1.0.0-SNAPSHOT.jar
java -jar bcommerce-gateway\\target\\bcommerce-gateway-1.0.0-SNAPSHOT.jar
```

说明：

- commerce 默认 8081，gateway 默认 8080。
- 业务配置默认对齐 compose 的端口映射（MySQL 3308、Redis 6380）。

### 3.3 构建并启动前端

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173/`。

### 3.4 验证接口（可写入报告的“结果验证”）

```powershell
curl.exe http://127.0.0.1:8080/actuator/health
curl.exe http://127.0.0.1:8080/api/c/products
curl.exe http://127.0.0.1:8080/api/c/seckill/activities
```

**云上单机（轻量 / ECS）**：不在本机跑 Kubernetes 时，可在买的 Linux 云主机上用 Compose + JAR（或容器）+ Nginx 对外提供服务；规格、安全组、构建步骤与同域代理示例见 [`docs/CLOUD-DEPLOY.md`](docs/CLOUD-DEPLOY.md)。

## 四、Kubernetes 部署（可选：更像线上）

K8s 方式适合演示“容器化+编排”的流程。文档详见：`docs/K8S.md`。  
云上托管集群、多副本与可选 **HPA** 说明见：`docs/K8S-CLUSTER.md`。

典型流程（Docker Desktop Kubernetes）：

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File .\\scripts\\k8s-kind-up.ps1 -DockerDesktop
kubectl -n bcommerce port-forward svc/bcommerce-gateway 8080:8080
```

## 五、账号与功能入口

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

页面入口（前端）：

- `/`：商品列表（含分类与搜索）
- `/products/:id`：商品详情
- `/seckill`：秒杀活动列表与下单
- `/orders`：我的订单
- `/merchant`：商家后台（上架商品、查看关联订单）

## 六、调试与压测（写报告常用）

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

## 七、写报告 / 答辩建议（直接可用）

建议准备以下截图（每张图配 1～2 句说明即可）：

1. **首页商品**：展示分类、搜索与商品卡片（证明商品模块可用）
2. **秒杀页**：展示活动时间、库存、限购与下单按钮（证明秒杀链路可用）
3. **我的订单**：展示订单号、金额、类型、下单时间与明细（证明订单落库与查询可用）
4. **商家后台**：展示上架表单与关联订单列表（证明 B 端能力）
5. （可选）`kubectl get pods -n bcommerce` 或 `docker compose ps`：证明容器化与编排部署

报告里“表结构说明”可引用：`bcommerce-commerce/src/main/resources/schema.sql`（本项目 12 张物理表，含订单分表）。

## 八、文档索引

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
