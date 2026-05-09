# 在新电脑 / 新环境运行本仓库

按本文检查一遍，可避免「换机器就跑不起来」的常见坑。

## 1. 版本与工具（必须）

| 组件 | 要求 | 说明 |
|------|------|------|
| **JDK** | **17**（推荐 Temurin / Oracle） | 根 `pom.xml` 已用 `maven-enforcer-plugin` 校验；低于 17 会直接构建失败并提示 |
| **Maven** | **≥ 3.6.3** | 与 Spring Boot 3.2 常见用法兼容 |
| **Docker** | Docker Desktop（Windows/Mac）或 Docker Engine + Compose v2 | 用于 MySQL / Redis / RabbitMQ / ES |
| **Node.js** | **≥ 18**（推荐 20 LTS） | 前端 Vite 5；`frontend/package.json` 中 `engines` 会提示版本不符 |
| **npm** | 随 Node 即可 | 在 `frontend` 目录执行 `npm ci` 或 `npm install` |

可选：Python 3.10+（仅压测脚本 `scripts/stress_seckill.py`）。

## 2. 端口占用（本机开发）

以下端口需空闲或与 `docker-compose.yml` / `application.yml` 一致：

| 端口 | 用途 |
|------|------|
| 3308 | MySQL（映射到容器 3306） |
| 6380 | Redis（映射到容器 6379） |
| 5672、15672 | RabbitMQ |
| 9200 | Elasticsearch |
| 8080、8081 | 网关、业务 JAR |
| 5173 | Vite 开发服务器（默认） |

若你改了 compose 映射或 `application.yml` 里的地址，请**整仓库一致修改**（见主 `README.md`）。

## 3. 推荐启动顺序（本机 + Docker 依赖）

1. 克隆仓库到**仅 ASCII 路径**（避免极少数工具对中文路径不友好）。
2. 在项目根：`docker compose up -d`  
   - 首次或改过 `docker/elasticsearch` 时：先 `docker compose build elasticsearch`，再 `up`（见主 README）。
3. 等待 MySQL 就绪约 **30～60 秒**（再启动 Java，避免连库失败）。
4. 在项目根：`mvn -DskipTests package`（或 `mvn clean package -DskipTests`）。
5. 两个终端分别启动 `bcommerce-commerce`、`bcommerce-gateway` 的 fat jar（或 `mvn -pl ... spring-boot:run`）。
6. 前端：`cd frontend && npm ci && npm run dev`。

**Windows**：若 Maven 报 **`Unable to rename ... jar.original`**，先执行 `scripts\stop-local-bcommerce.ps1`，并关闭 IDE 里正在运行的 Boot。

**Linux / macOS**：可先执行 `chmod +x scripts/stop-local-bcommerce.sh && ./scripts/stop-local-bcommerce.sh`（需本机有 `lsof` 或 `fuser`），再打包。

## 4. 编码与换行

- 源码与文档为 **UTF-8**；请勿用系统默认 GBK 打开 Java 源文件。
- Shell 脚本在仓库中为 **LF** 换行（见 `.gitattributes`）；从网盘拷贝到 Windows 时若脚本损坏，可用 Git 重新检出 `scripts/*.sh`。

## 5. Maven 与 Docker 镜像

- 若公司内网**无法访问 Maven Central**，需配置私服或 `settings.xml` 镜像（与任何 Spring Boot 项目相同）。
- `docker-compose.yml` 与 `k8s/` 使用 **Docker Hub 官方镜像名**；国内可在 Docker Desktop 配置 `registry-mirrors` 加速。若配置的镜像返回 **401**，请更换或删除该 mirror。

## 6. 从局域网其他设备访问开发机

- 前端开发态使用 **Vite 代理**（`baseURL: '/api'`），浏览器与 Vite 同源，一般不触发浏览器对网关的 CORS。
- 若你改为**浏览器直连网关**（例如自定义端口或 `preview`），网关已配置 **`http://localhost:*` / `http://127.0.0.1:*`** 的 CORS 模式；从**另一台电脑**的浏览器访问时，需在 `bcommerce-gateway` 的 `application.yml` 中增加对应来源或独立 profile，否则会被 CORS 拦截。

## 7. Kubernetes

- Windows 可用 `scripts/k8s-kind-up.ps1`；Linux/macOS 请按 `docs/K8S.md` 手动执行其中的 `mvn`、`docker build`、`kind load`、`kubectl apply` 等步骤（脚本逻辑与文档一致）。

## 8. 自检清单（给别人机子前可打勾）

- [ ] `java -version` 显示 17  
- [ ] `mvn -v` 正常且 JDK 指向 17  
- [ ] `docker compose version` 可用  
- [ ] `docker compose up -d` 后四个依赖容器无异常退出  
- [ ] `mvn -DskipTests package` 成功  
- [ ] 浏览器访问 `http://localhost:8080/actuator/health`（网关）与业务健康检查符合预期  
- [ ] `frontend`：`npm run dev` 后能登录演示账号（见主 README）
