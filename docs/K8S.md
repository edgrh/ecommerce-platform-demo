# Kubernetes 本地部署说明

将 **MySQL / Redis / RabbitMQ / Elasticsearch（含 IK）/ commerce / gateway** 部署到命名空间 `bcommerce`。应用通过 **`SPRING_PROFILES_ACTIVE=k8s`** 使用集群内 DNS（`mysql:3306`、`redis:6379` 等），见各模块 `application-k8s.yml`。

## 前置条件

- 已安装 [kubectl](https://kubernetes.io/docs/tasks/tools/)
- 任选一种集群：
  - **[kind](https://kind.sigs.k8s.io/)**（推荐，跨平台）
  - **Docker Desktop 自带 Kubernetes**（Windows/Mac 开启后，本地 `docker build` 的镜像一般可直接被集群使用，`imagePullPolicy: IfNotPresent`）

## 1. 构建业务 JAR

在**仓库根目录**执行（路径按你的克隆位置替换）：

```bash
mvn -q -DskipTests package
```

**若报错 `Unable to rename ... jar.original`**：说明 **`target/*.jar` 被占用**。请按顺序尝试：

1. 在项目根执行：`powershell -ExecutionPolicy Bypass -File scripts\stop-local-bcommerce.ps1`（Windows），或 `./scripts/stop-local-bcommerce.sh`（Linux/macOS，需 `lsof` 或 `fuser`），结束占用 **8080/8081** 的进程。  
2. **关闭 Cursor / IDEA** 里正在 Run 的 `GatewayApplication` / `CommerceApplication`（IDE 也会锁 jar）。  
3. 任务管理器里结束多余 **java.exe**。  
4. 仍失败：杀毒/Defender 可能对 `target` 目录实时扫描导致锁文件，可对该目录加排除后再打包。

脚本 `k8s-kind-up.ps1` 在 `mvn` 前也会检测 8080/8081；端口空闲后仍失败多半是 **IDE 或杀毒**。

## 2. 构建镜像

在**仓库根目录**执行：

```powershell
docker build -f docker/elasticsearch/Dockerfile -t bcommerce/elasticsearch:local docker/elasticsearch
docker build -f docker/commerce/Dockerfile -t bcommerce/commerce:local .
docker build -f docker/gateway/Dockerfile -t bcommerce/gateway:local .
```

## 3. 将镜像载入 kind（仅 kind 需要）

```powershell
kind create cluster --name bcommerce
kubectl config use-context kind-bcommerce

kind load docker-image bcommerce/elasticsearch:local --name bcommerce
kind load docker-image bcommerce/commerce:local --name bcommerce
kind load docker-image bcommerce/gateway:local --name bcommerce
```

Docker Desktop K8s 通常**不需要** `kind load`，保证镜像已 `docker build` 即可。

## 4. 部署清单

```powershell
kubectl apply -f k8s/
```

等待就绪：

```powershell
kubectl -n bcommerce rollout status deployment/mysql --timeout=300s
kubectl -n bcommerce rollout status deployment/elasticsearch --timeout=300s
kubectl -n bcommerce rollout status deployment/bcommerce-commerce --timeout=600s
kubectl -n bcommerce rollout status deployment/bcommerce-gateway --timeout=300s
```

## 5. 访问网关

- **NodePort**：清单中为网关暴露 **`30080`**（`http://<节点IP>:30080`）。本机 kind 可：
  ```powershell
  kubectl -n bcommerce get svc bcommerce-gateway
  ```
  单节点时节点 IP 多为 `127.0.0.1` 或 `localhost`。
- **端口转发**（不依赖 NodePort 是否可达）：
  ```powershell
  kubectl -n bcommerce port-forward svc/bcommerce-gateway 8080:8080
  ```
  浏览器 / 前端仍指向 `http://localhost:8080/api/...`。

前端 `vite` 代理可把 `target` 设为 `http://127.0.0.1:8080`（与网关 CORS `application-k8s` 中 `localhost` 一致）。

## 6. 一键脚本（kind）

```powershell
powershell -ExecutionPolicy Bypass -File scripts\k8s-kind-up.ps1
```

脚本会：`mvn package`、构建镜像、创建 kind 集群（若不存在）、`kind load`、`kubectl apply`。

## 常见问题

1. **Elasticsearch 一直 Pending / OOM**  
   调大本机 Docker 内存，或降低 `k8s/05-elasticsearch.yaml` 中 `ES_JAVA_OPTS` 与 `limits.memory`。

2. **ImagePullBackOff**  
   kind：确认已执行 `kind load docker-image`。Docker Desktop：确认镜像名与 YAML 一致且已本地构建。

3. **MySQL 首次较慢**  
   `bcommerce-commerce` 的 initContainer 最多等待约 4 分钟；仍失败可 `kubectl -n bcommerce logs deploy/bcommerce-commerce -c wait-deps`。

4. **生产环境**  
   勿使用清单中的明文密码；应使用 Secret、外部托管数据库、Ingress TLS、资源配额与 HPA 等。
