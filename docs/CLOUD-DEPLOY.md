# 云上单机部署（轻量应用服务器 / ECS）

面向：**一台 Linux 云主机**跑完整演示（Docker Compose 起中间件 + 网关/业务 + 前端静态资源）。  
本方式**不需要**本机 Docker Desktop Kubernetes，适合笔记本性能有限时使用。

## 1. 规格与安全组

- **建议**：2 核 **8GB** 内存更稳（4GB 仅勉强跑齐 MySQL/Redis/RabbitMQ/ES/Java ×2）；系统盘 ≥ 40GB。
- **安全组入方向**（按需放行）：
  - **22**：SSH（来源限制为你的办公网 / VPN IP，勿对 `0.0.0.0/0` 长期放开若可避免）
  - **80**：HTTP（走 Nginx 同域访问前端 + `/api`）
  - 调试期可临时开 **8080**（直连网关）；对外演示推荐只留 **80/443**，网关仅本机 `127.0.0.1` 访问。

## 2. 主机环境（以 Ubuntu 22.04 为例）

```bash
sudo apt update && sudo apt install -y git openjdk-17-jdk maven nginx
```

安装 Docker（官方文档为准；以下为常见脚本路径示例）：

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
# 重新登录 SSH 使 docker 组生效
docker compose version
```

## 3. 获取代码与启动中间件

```bash
git clone <你的仓库 URL> ecommerce-platform-demo
cd ecommerce-platform-demo

docker compose build elasticsearch   # 首次或改过 ES Dockerfile 时
docker compose up -d
docker compose ps
```

确认 MySQL（映射宿主机 **3308**）、Redis（**6380**）、RabbitMQ、Elasticsearch（**9200**）均为 Up。

## 4. 构建并启动后端（推荐：本机 JAR + 默认配置）

默认 `application.yml` 已按 Compose 端口写死 **localhost:3308 / 6380 / 9200** 等，与「中间件容器映射到宿主机」一致，**无需改配置文件**。

```bash
mvn -q -DskipTests package

# 两个终端分别后台运行，或用 systemd / screen / tmux
nohup java -jar bcommerce-commerce/target/bcommerce-commerce-1.0.0-SNAPSHOT.jar > /tmp/commerce.log 2>&1 &
nohup java -jar bcommerce-gateway/target/bcommerce-gateway-1.0.0-SNAPSHOT.jar > /tmp/gateway.log 2>&1 &

curl -s http://127.0.0.1:8080/actuator/health
```

### 备选：用 Docker 镜像 + host 网络（可不装 JDK/Maven）

在仓库根目录构建镜像后：

```bash
mvn -q -DskipTests package   # 仍需 jar 供 COPY；或在 CI 构建好镜像后 scp 导入
docker build -f docker/commerce/Dockerfile -t bcommerce/commerce:local .
docker build -f docker/gateway/Dockerfile -t bcommerce/gateway:local .

docker run -d --name bcommerce-commerce --restart unless-stopped --network host \
  -e SPRING_PROFILES_ACTIVE=default \
  bcommerce/commerce:local

docker run -d --name bcommerce-gateway --restart unless-stopped --network host \
  -e SPRING_PROFILES_ACTIVE=default \
  bcommerce/gateway:local
```

**说明**：`commerce` 的镜像默认 `SPRING_PROFILES_ACTIVE=k8s`，若用该 Dockerfile **勿带 k8s**，需覆盖为 `default`（如上），否则会连集群内的 `mysql` 主机名而非本机映射端口。

## 5. 前端构建与 Nginx（推荐：同域避免 CORS）

开发环境 `vite` 把 `/api` 代理到 `localhost:8080`；云上浏览器访问公网 IP 时，应通过 **Nginx 同一站点** 提供静态页并反向代理 API，避免网关 `application.yml` 里仅放行 `localhost` 的 CORS 限制。

```bash
cd frontend
npm ci
npm run build
sudo mkdir -p /var/www/bcommerce
sudo cp -r dist/* /var/www/bcommerce/
```

示例 `/etc/nginx/sites-available/bcommerce`：

```nginx
server {
    listen 80;
    server_name _;

    root /var/www/bcommerce;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
}
```

启用并重载：

```bash
sudo ln -sf /etc/nginx/sites-available/bcommerce /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

浏览器访问：`http://<公网IP>/`（页面请求 `/api/...` 与页面同域）。

## 6. 验收与排错

```bash
curl -s http://127.0.0.1/api/c/products -H "Host: <公网IP或域名>"
docker compose logs --tail=50 mysql
tail -n 80 /tmp/gateway.log /tmp/commerce.log
```

- **502 / 空白页**：先看网关是否监听 `8080`、`nginx -t` 是否通过。
- **数据库连不上**：确认 `docker compose ps` 中 mysql healthy，`jdbc` URL 仍为 `localhost:3308`。
- **升级代码**：`git pull` → `mvn package` → 重启两个 jar（或重建并重启容器）。

## 7. 与云上托管 Kubernetes 的关系

本文是 **单机 Compose + 进程/容器** 路径。若改用 **ACK / TKE**，需把 `bcommerce/*` 镜像推到镜像仓库并改写 `k8s/` 中的 `image:`，与本地 kind/Docker Desktop 流程不同；可作为后续进阶。
