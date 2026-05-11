#!/usr/bin/env bash
# 同一台机器用多个 Docker 容器并行跑 k6，使网关看到多个不同源 IP，从而拆 Redis 限流桶。
#
# 用法（在仓库根目录）：
#   chmod +x scripts/k6-docker-fanout.sh
#   ./scripts/k6-docker-fanout.sh
#
# 常用环境变量：
#   NUM=4                 容器数量（默认 4）
#   TOTAL_RATE=8000     所有容器「迭代/秒」之和；会均分到每个容器（默认 2000）
#   DURATION=1m         每容器 k6 持续时间
#   SCRIPT=k6-mixed-10k-stress.js   perf/ 下脚本名（默认混合压测 stress 版）
#   HOST_IP=            留空则自动探测 docker0 网桥 IP（容器访问宿主机网关用）
#   K6_IMAGE=           默认 docker.m.daocloud.io/grafana/k6:0.51.0
#   PRE_VUS_PER=800     每容器预分配 VU
#   MAX_VUS_PER=8000    每容器最大 VU
#   STRICT=             传给 k6（如 STRICT=1）
#   SHARD_MOD=64        传给 k6：需网关 stress profile 才按 X-Stress-Shard 拆桶（见 docs/PERF-STRESS.md）
#
# 示例：4 容器 × 2500 iter/s ≈ 10000 迭代/秒 总到达率（网关侧约 4 个客户端 IP）：
#   NUM=4 TOTAL_RATE=10000 DURATION=1m ./scripts/k6-docker-fanout.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

NUM="${NUM:-4}"
TOTAL_RATE="${TOTAL_RATE:-2000}"
DURATION="${DURATION:-1m}"
SCRIPT="${SCRIPT:-k6-mixed-10k-stress.js}"
K6_IMAGE="${K6_IMAGE:-docker.m.daocloud.io/grafana/k6:0.51.0}"
PRE_VUS_PER="${PRE_VUS_PER:-800}"
MAX_VUS_PER="${MAX_VUS_PER:-8000}"

if ! [[ "$NUM" =~ ^[1-9][0-9]*$ ]]; then
  echo "NUM must be a positive integer" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker 不可用或未启动" >&2
  exit 1
fi

if [[ ! -f "perf/$SCRIPT" ]]; then
  echo "找不到 perf/$SCRIPT" >&2
  exit 1
fi

HOST_IP="${HOST_IP:-}"
if [[ -z "$HOST_IP" ]]; then
  if ip -4 addr show docker0 >/dev/null 2>&1; then
    HOST_IP="$(ip -4 addr show docker0 | awk '/inet / {print $2}' | cut -d/ -f1 | head -1)"
  fi
fi
if [[ -z "$HOST_IP" ]]; then
  HOST_IP="172.17.0.1"
fi

RATE_PER=$((TOTAL_RATE / NUM))
if [[ "$RATE_PER" -lt 1 ]]; then
  echo "TOTAL_RATE=$TOTAL_RATE 太小，无法均分到 NUM=$NUM" >&2
  exit 1
fi

SUFFIX="$(date +%s)-$$"
CIDS=()
cleanup() {
  local c
  for c in "${CIDS[@]:-}"; do
    docker rm -f "$c" >/dev/null 2>&1 || true
  done
}
trap cleanup EXIT INT TERM

echo "======== k6 Docker fan-out ========"
echo "ROOT=$ROOT"
echo "HOST_IP=$HOST_IP  (容器内访问宿主机网关: http://${HOST_IP}:8080)"
echo "NUM=$NUM  TOTAL_RATE=$TOTAL_RATE  -> 每容器 RATE=$RATE_PER  DURATION=$DURATION"
echo "SCRIPT=$SCRIPT  PRE_VUS_PER=$PRE_VUS_PER  MAX_VUS_PER=$MAX_VUS_PER"
echo "K6_IMAGE=$K6_IMAGE"
echo

# 可选：从容器网络探测宿主机 8080（需本机已拉取 curlimages/curl）
if docker image inspect curlimages/curl >/dev/null 2>&1; then
  if docker run --rm --network bridge curlimages/curl -sS -m 3 -o /dev/null -w "probe HTTP %{http_code}\n" "http://${HOST_IP}:8080/actuator/health"; then
    :
  fi
else
  echo "(跳过探测: 无 curlimages/curl 镜像，可 docker pull curlimages/curl)"
fi
echo

EXTRA_ENV=()
if [[ -n "${STRICT:-}" ]]; then
  EXTRA_ENV+=(-e "STRICT=$STRICT")
fi
if [[ -n "${SHARD_MOD:-}" ]]; then
  EXTRA_ENV+=(-e "SHARD_MOD=$SHARD_MOD")
fi

i=1
while [[ "$i" -le "$NUM" ]]; do
  name="k6fan-${SUFFIX}-${i}"
  # 不用 --rm：容器退出后仍可用 docker logs 取 k6 汇总，再由 cleanup 统一删除
  cid="$(
    docker run -d \
      --name "$name" \
      -v "$ROOT/perf:/perf:ro" \
      -e BASE_URL="http://${HOST_IP}:8080" \
      -e RATE="$RATE_PER" \
      -e DURATION="$DURATION" \
      -e PRE_VUS="$PRE_VUS_PER" \
      -e MAX_VUS="$MAX_VUS_PER" \
      "${EXTRA_ENV[@]}" \
      "$K6_IMAGE" run "/perf/$SCRIPT"
  )" || {
    echo "启动容器 $name 失败" >&2
    exit 1
  }
  CIDS+=("$cid")
  echo "started $name -> $cid"
  i=$((i + 1))
done

echo
echo "等待全部容器结束..."
FAIL=0
for c in "${CIDS[@]}"; do
  code="$(docker wait "$c" || echo 1)"
  if [[ "$code" != "0" ]]; then
    FAIL=1
  fi
  echo "---- docker logs $c (exit $code) ----"
  docker logs "$c" 2>&1 | tail -80
  echo
done

if [[ "$FAIL" -ne 0 ]]; then
  echo "存在非 0 退出（多为 k6 STRICT 阈值或压测失败），请查看上方日志。" >&2
  exit 1
fi

echo "全部容器已结束且退出码为 0。"
