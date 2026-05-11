# 压测与网关 stress 配置

## 问题根因（简述）

- 默认网关 **按 IP 令牌桶限流**（约 120/s），单源 IP 高并发时大量 **429**。
- 单机压 **10k 迭代/秒** 时，下游与队列易饱和，出现 **5xx、秒级延迟、k6 dropped_iterations**。

## 方案 A：stress  profile（推荐做实验）

1. **重启网关**并启用 profile（**不要**在生产开）：

```bash
export SPRING_PROFILES_ACTIVE=stress
# 先 commerce 再 gateway，与平时一致
java -jar bcommerce-commerce/target/bcommerce-commerce-1.0.0-SNAPSHOT.jar &
java -jar bcommerce-gateway/target/bcommerce-gateway-1.0.0-SNAPSHOT.jar &
```

`application-stress.yml` 会把限流提高到 **50000/s** 量级（仍走 Redis，仅数值放大）。

2. 再跑 k6 / `scripts/k6-docker-fanout.sh`。

## 方案 B：stress + `X-Stress-Shard`（单 IP 下再拆多桶）

仅在 **`stress` profile** 下，网关会读请求头 **`X-Stress-Shard`**；若存在，限流 key 为 `gw:stress:<值>`，与 IP 无关。

k6 脚本已支持环境变量 **`SHARD_MOD`**（每个 VU 自动带 `X-Stress-Shard: __VU % SHARD_MOD`）。

示例：

```bash
SHARD_MOD=128 BASE_URL=http://127.0.0.1:8080 RATE=2000 DURATION=1m k6 run perf/k6-mixed-10k-stress.js
```

Docker fan-out 传入：

```bash
SHARD_MOD=64 NUM=4 TOTAL_RATE=8000 DURATION=1m ./scripts/k6-docker-fanout.sh
```

**注意**：未启用 `stress` 时，网关**忽略**该头，避免被恶意伪造绕过限流。

## 云主机把项目推到 GitHub（示例）

在 **`~/ecommerce-platform-demo`**（已是 clone）：

```bash
cd ~/ecommerce-platform-demo
git status
git remote -v
# 勿提交含密码的私有文件；若 application.yml 含敏感信息请先改或 .gitignore
git add -A
git commit -m "sync from cloud host"
git pull --rebase origin main   # 若远程已有新提交
git push origin main
```

若远程拒绝：先 `git fetch origin` 再 `git rebase origin/main` 或按提示处理冲突。

## 本机拉取

```bash
cd G:\program\java\ecommerce-platform-demo
git fetch origin
git pull origin main
```

## 恢复默认限流

去掉 `SPRING_PROFILES_ACTIVE=stress`，重启网关即可。
