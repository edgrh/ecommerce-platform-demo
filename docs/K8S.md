# Kubernetes 本地部署说明

将 **MySQL / Redis / RabbitMQ / Elasticsearch（含 IK）/ commerce / gateway** 部署到命名空间 `bcommerce`。应用通过 **`SPRING_PROFILES_ACTIVE=k8s`** 使用集群内 DNS（`mysql:3306`、`redis:6379` 等），见各模块 `application-k8s.yml`。

## 前置条件

- 已安装 [kubectl](https://kubernetes.io/docs/tasks/tools/)
- 任选一种集群：
  - **[kind](https://kind.sigs.k8s.io/)**（推荐，跨平台）
  - **Docker Desktop 自带 Kubernetes**（Windows/Mac 开启后，本地 `docker build` 的镜像一般可直接被集群使用，`imagePullPolicy: IfNotPresent`）

### 资源建议（16GB 笔记本）

- 这套清单默认按 **16GB 内存笔记本**做了保守配置：`gateway/commerce` 各 3 副本、Elasticsearch 堆内存 256m。
- 如果机器内存更大、想提吞吐，可以再提高副本数与资源上限；如果仍遇到 ES OOM，优先继续下调 `k8s/05-elasticsearch.yaml` 的 `ES_JAVA_OPTS` 和 `limits.memory`，或在演示时暂时关闭 ES（搜索将回退到 MySQL `LIKE`）。

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

说明：`k8s/` 目录只包含业务运行所需资源（MySQL/Redis/RabbitMQ/ES/commerce/gateway），不再默认创建压测 Job。

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

## 6. 一键脚本

**方式 A：已安装 [kind](https://kind.sigs.k8s.io/)（且在 PATH 中）**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\k8s-kind-up.ps1
```

脚本会：`mvn package`、构建镜像、创建 kind 集群 `bcommerce`（若不存在）、`kind load`、`kubectl apply`。

**方式 B：只用 Docker Desktop 自带 Kubernetes（无需安装 kind）**

先在 Docker Desktop → **Settings → Kubernetes** 勾选 **Enable Kubernetes** 并等待就绪，然后：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\k8s-kind-up.ps1 -DockerDesktop
```

本地 `docker build` 的镜像可被该集群直接使用，**不需要** `kind load`。

若提示找不到 `docker-desktop` 上下文，执行 `kubectl config get-contexts`，再用 `kubectl config use-context <名称>` 切到你的集群后，可手动从「步骤 2」构建镜像开始执行 `kubectl apply -f k8s/`。

**方式 C：日常只改 Java / 种子数据——重建镜像并滚动重启（不必再手抄一长串命令）**

集群和 `k8s/` 已部署过一次后，在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\k8s-redeploy.ps1
```

脚本会**自动判断**：若存在 kind 集群 `bcommerce` 则 `kind load`；否则若 kubectl 有 `docker-desktop` 上下文则直接用 Docker Desktop 内置 K8s（**不必安装 kind，也不必写 `-DockerDesktop`**）。仅当你想**强制**切到 `docker-desktop` 时再传 `-DockerDesktop`。

若使用纯 kind、且本机没有启用 Docker Desktop K8s，保持上述一条命令即可（会先走 kind 分支）。

脚本会：`mvn package` → 构建 `bcommerce/commerce:local` 与 `bcommerce/gateway:local` →（仅 kind 时 `kind load`）→ `kubectl rollout restart` 两个 Deployment 并等待就绪。若改了 ES/IK Dockerfile，可加 `-Elasticsearch` 一并重建 `elasticsearch` 镜像并重启对应 Deployment。

**是否需要重启？** 改代码或资源文件后，运行中的 Pod / 本机 `java -jar` **必须**用新构建替换才会生效；本脚本把这件事收敛成一条命令。**仅改前端**时一般只需刷新或重启 `npm run dev`，不必动 K8s。

**演示种子（DemoSeedRunner）**：只在**空库**（无用户）时插入数据；K8s 里 MySQL 若有 PVC，反复重启 commerce **不会**自动清空重灌。要重新跑种子需删库/删 PVC 或 `kubectl delete pvc ...` 后让 MySQL 重新初始化（生产勿用）。

## 7. 集群内 k6 压测（可选）

默认部署不会自动创建压测任务。需要压测时手动执行：

```powershell
kubectl -n bcommerce apply -f perf/k6-job.yaml
kubectl -n bcommerce wait --for=condition=complete job/k6-activities --timeout=240s
kubectl -n bcommerce logs job/k6-activities
```

如需重复压测，先删除旧 Job：

```powershell
kubectl -n bcommerce delete job k6-activities --ignore-not-found=true
```

## 常见问题

1. **Elasticsearch 一直 Pending / OOM**  
   调大本机 Docker 内存，或降低 `k8s/05-elasticsearch.yaml` 中 `ES_JAVA_OPTS` 与 `limits.memory`。

2. **ImagePullBackOff**  
   kind：确认已执行 `kind load docker-image`。Docker Desktop：确认镜像名与 YAML 一致且已本地构建。

3. **MySQL 首次较慢**  
   `bcommerce-commerce` 的 initContainer 最多等待约 4 分钟；仍失败可 `kubectl -n bcommerce logs deploy/bcommerce-commerce -c wait-deps`。

4. **生产环境**  
   勿使用清单中的明文密码；应使用 Secret、外部托管数据库、Ingress TLS、资源配额与 HPA 等。
