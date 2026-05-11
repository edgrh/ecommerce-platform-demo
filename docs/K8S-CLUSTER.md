# Kubernetes 集群化说明（托管云 / 多副本）

本仓库的 **「集群」** 主要指两类能力，答辩或报告里可以分开写：

1. **编排层**：用 Kubernetes 调度多容器；`k8s/06-commerce.yaml`、`k8s/07-gateway.yaml` 里 **commerce / gateway 默认各 3 个副本**，Service 做负载均衡，网关仍经 `bcommerce-gateway` 统一入口。  
2. **弹性层（可选）**：`k8s/08-hpa-commerce.yaml` 为 **commerce** 提供 **HPA**，按 CPU 利用率在 2～8 副本间伸缩（需集群安装 **metrics-server**）。

单机 **Docker Compose + 双 JAR** 见 [`CLOUD-DEPLOY.md`](CLOUD-DEPLOY.md)；与 K8s 的差异：**Compose 不负责 Pod 副本与滚动发布**，K8s 负责。

---

## 一、与本地 kind / Docker Desktop 的关系

完整步骤仍以 [`K8S.md`](K8S.md) 为准：`mvn package` → `docker build` → `kubectl apply -f k8s/`。

- 网关镜像在清单中为 **`bcommerce/gateway:local`**，与 `scripts/k8s-kind-up.ps1`、`k8s-redeploy.ps1` 构建标签一致。  
- 业务使用 **`SPRING_PROFILES_ACTIVE=k8s`**，走集群 DNS（`mysql`、`redis` 等），见各模块 `application-k8s.yml`。

---

## 二、云上托管 Kubernetes（通用步骤）

任意云厂商的 **托管 K8s**（如 ACK、TKE、京东云云原生等）思路相同：

1. **镜像仓库**：把 `bcommerce/commerce:local`、`bcommerce/gateway:local`、`bcommerce/elasticsearch:local` 推到该云 **私有镜像仓库**（或能拉取的公开仓库）。  
2. **改清单**：把 `k8s/06-commerce.yaml`、`k8s/07-gateway.yaml`、`k8s/05-elasticsearch.yaml` 里的 `image:` 换成 **带仓库前缀的完整地址**（含 tag），必要时加 `imagePullSecrets`。  
3. **中间件**：云上常见做法是 **托管 RDS / 托管 Redis**，此时应删掉或缩小 `k8s/02-mysql.yaml`、`k8s/03-redis.yaml`，并把 `application-k8s.yml` 中的 JDBC、Redis 地址改为云产品内网地址（可用 **ConfigMap / Secret** 注入，本演示为简化写死在 yml 里）。  
4. **对外暴露**：`NodePort 30080` 适合演示；生产多用 **Ingress + TLS** 或云 **SLB**，需另写 Ingress 资源或改 Service 类型。  
5. **执行**：`kubectl apply -f k8s/01-namespace.yaml` … 按依赖顺序 apply，或使用 `kubectl apply -f k8s/`（注意 StatefulSet/Deployment 就绪顺序与 `K8S.md` 中 `rollout status` 一致）。

---

## 三、启用 commerce 的 HPA

前提：集群已安装 [metrics-server](https://github.com/kubernetes-sigs/metrics-server)（托管集群多数已预装）。

```bash
kubectl apply -f k8s/08-hpa-commerce.yaml
kubectl -n bcommerce get hpa
kubectl -n bcommerce describe hpa bcommerce-commerce
```

若 `kubectl top pods -n bcommerce` 报错，多半是 metrics-server 未就绪或未装。演示环境可不启用 HPA，仅靠固定 **3 副本** 已能说明「多实例 + Service 负载均衡」。

---

## 四、和「高可用集群」一词的边界

- **多副本**：本仓库已具备，用于抗并发与滚动升级。  
- **跨可用区 / 多节点反亲和**：需在 `Deployment` 的 `spec.template.spec.affinity` 中配置，属于进阶，可按云文档模板扩展。  
- **有状态组件**：MySQL、Redis 在演示清单中为单实例 Deployment；生产应使用 **Operator / 云托管**，不在本演示范围内。

更多排障与端口说明见 [`K8S.md`](K8S.md)。
