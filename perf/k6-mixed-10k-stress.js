import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter } from 'k6/metrics'

/**
 * 混合业务压测：默认目标约 10k 迭代/秒（与「HTTP QPS」接近；购物车分支可能 2 次 HTTP/迭代）。
 *
 * 与 k6-realistic-10k.js 流量模型一致；本脚本默认放宽 thresholds，便于在「网关限流 / 429 较多」
 * 时仍作为压测发生器跑满到达率；要 SLA 门禁请加 STRICT=1。
 *
 * 运行示例：
 *   cd ~/ecommerce-platform-demo
 *   BASE_URL=http://127.0.0.1:8080 k6 run perf/k6-mixed-10k-stress.js
 *
 * Docker（打宿主机网关，勿用容器内 127.0.0.1）：
 *   docker run --rm --network host -v "$PWD/perf:/perf:ro" \
 *     -e BASE_URL=http://127.0.0.1:8080 -e RATE=10000 -e DURATION=1m \
 *     docker.m.daocloud.io/grafana/k6:0.51.0 run /perf/k6-mixed-10k-stress.js
 *
 * 多容器拆限流桶（同一台机、多个源 IP 并行 k6）：仓库根执行
 *   ./scripts/k6-docker-fanout.sh
 *
 * 环境变量：
 *   BASE_URL          网关根地址（不要带 /api）
 *   RATE              目标迭代/秒，默认 10000
 *   DURATION          默认 2m
 *   PRE_VUS / MAX_VUS 预分配与最大 VU，默认 3000 / 30000
 *   STRICT=1          启用与 realistic 相同的延迟/失败率阈值（限流下易红）
 *   SHARD_MOD=16      为每个请求加 X-Stress-Shard: (__VU % SHARD_MOD)；需网关 KeyResolver 配合才拆限流桶
 *   W_PRODUCTS 等     与 realistic 相同，调整各分支权重
 */

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080'
const RATE = Number(__ENV.RATE || 10000)
const DURATION = __ENV.DURATION || '2m'
const PRE_VUS = Number(__ENV.PRE_VUS || 3000)
const MAX_VUS = Number(__ENV.MAX_VUS || 30000)
const STRICT = __ENV.STRICT === '1'
const SHARD_MOD = __ENV.SHARD_MOD ? Number(__ENV.SHARD_MOD) : 0

const W_PRODUCTS = Number(__ENV.W_PRODUCTS || 55)
const W_DETAIL = Number(__ENV.W_DETAIL || 25)
const W_SECKILL_LIST = Number(__ENV.W_SECKILL_LIST || 10)
const W_CART = Number(__ENV.W_CART || 8)
const W_SECKILL_ORDER = Number(__ENV.W_SECKILL_ORDER || 2)

const WEIGHTS = [
  ['products', W_PRODUCTS],
  ['detail', W_DETAIL],
  ['seckill_list', W_SECKILL_LIST],
  ['cart', W_CART],
  ['seckill_order', W_SECKILL_ORDER]
]
const WEIGHT_SUM = WEIGHTS.reduce((s, [, w]) => s + w, 0)

const cnt200 = new Counter('biz_status_200')
const cnt429 = new Counter('biz_status_429')
const cnt5xx = new Counter('biz_status_5xx')
const cntOther = new Counter('biz_status_other')

function pick() {
  let r = Math.random() * WEIGHT_SUM
  for (const [k, w] of WEIGHTS) {
    r -= w
    if (r <= 0) return k
  }
  return 'products'
}

function shardHeaders() {
  if (!SHARD_MOD || SHARD_MOD < 2) return {}
  return { 'X-Stress-Shard': String(__VU % SHARD_MOD) }
}

function jsonHeaders(token) {
  // k6 运行时不用对象展开，用 Object.assign
  const h = Object.assign({ 'Content-Type': 'application/json' }, shardHeaders())
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

function bumpStatus(res) {
  const s = res.status
  if (s === 200) cnt200.add(1)
  else if (s === 429) cnt429.add(1)
  else if (s >= 500) cnt5xx.add(1)
  else cntOther.add(1)
}

function loginBuyer() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: 'buyer', password: 'demo123' }),
    { headers: Object.assign({ 'Content-Type': 'application/json' }, shardHeaders()) }
  )
  bumpStatus(res)
  check(res, { 'login 200': (r) => r.status === 200 })
  try {
    return res.json('token')
  } catch (e) {
    return null
  }
}

const thresholds = STRICT
  ? {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<800', 'p(99)<1500']
    }
  : {
      // 压测发生器模式：不因 429/限流导致 k6 非 0 退出；仍可看 summary 与 biz_status_* 计数
      http_req_duration: ['p(99)<120000']
    }

export const options = {
  scenarios: {
    mixed_traffic: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_VUS,
      maxVUs: MAX_VUS
    }
  },
  thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max']
}

export function setup() {
  return { buyerToken: loginBuyer() }
}

export default function (data) {
  const token = data && data.buyerToken ? data.buyerToken : null
  const flow = pick()

  if (flow === 'products') {
    if (Math.random() < 0.2) {
      const q = ['iphone', 'airpods', '耳机', 'max'][Math.floor(Math.random() * 4)]
      const res = http.get(`${BASE_URL}/api/c/products/search?q=${encodeURIComponent(q)}&limit=50`, {
        tags: { flow: 'search' },
        headers: shardHeaders()
      })
      bumpStatus(res)
      check(res, { 'search 200': (r) => r.status === 200 })
    } else {
      const res = http.get(`${BASE_URL}/api/c/products?limit=50`, {
        tags: { flow: 'products' },
        headers: shardHeaders()
      })
      bumpStatus(res)
      check(res, { 'products 200': (r) => r.status === 200 })
    }
    sleep(Math.random() * 0.05)
    return
  }

  if (flow === 'detail') {
    const id = 1 + Math.floor(Math.random() * 6)
    const res = http.get(`${BASE_URL}/api/c/products/${id}`, {
      tags: { flow: 'detail' },
      headers: shardHeaders()
    })
    bumpStatus(res)
    check(res, { 'detail 200': (r) => r.status === 200 })
    sleep(Math.random() * 0.05)
    return
  }

  if (flow === 'seckill_list') {
    const res = http.get(`${BASE_URL}/api/c/seckill/activities`, {
      tags: { flow: 'seckill_list' },
      headers: shardHeaders()
    })
    bumpStatus(res)
    check(res, { 'seckill activities 200': (r) => r.status === 200 })
    sleep(Math.random() * 0.03)
    return
  }

  if (flow === 'cart') {
    if (!token) return
    const listRes = http.get(`${BASE_URL}/api/c/cart`, { headers: jsonHeaders(token), tags: { flow: 'cart_list' } })
    bumpStatus(listRes)
    check(listRes, { 'cart list 200': (r) => r.status === 200 })

    if (Math.random() < 0.35) {
      const skuId = 1 + Math.floor(Math.random() * 6)
      const addRes = http.post(
        `${BASE_URL}/api/c/cart/add`,
        JSON.stringify({ skuId, quantity: 1 }),
        { headers: jsonHeaders(token), tags: { flow: 'cart_add' } }
      )
      bumpStatus(addRes)
      check(addRes, { 'cart add ok': (r) => r.status === 200 || r.status === 204 })
    }
    sleep(Math.random() * 0.05)
    return
  }

  if (flow === 'seckill_order') {
    if (!token) return
    const actId = 1 + Math.floor(Math.random() * 3)
    const key = `${actId}-${Date.now()}-${Math.random()}`
    const res = http.post(
      `${BASE_URL}/api/c/seckill/orders`,
      JSON.stringify({ activityId: actId, quantity: 1 }),
      {
        headers: Object.assign({}, jsonHeaders(token), { 'X-Idempotency-Key': key }),
        tags: { flow: 'seckill_order' }
      }
    )
    bumpStatus(res)
    check(res, { 'seckill order responded': (r) => r.status >= 200 && r.status < 500 })
    sleep(Math.random() * 0.02)
  }
}

export function handleSummary(data) {
  const m = data.metrics || {}
  const val = (name, key) =>
    m[name] && m[name].values && m[name].values[key] != null ? m[name].values[key] : null

  const summary = {
    script: 'k6-mixed-10k-stress.js',
    base_url: BASE_URL,
    rate_target_iters_per_s: RATE,
    duration: DURATION,
    strict_thresholds: STRICT,
    shard_mod: SHARD_MOD || null,
    http_reqs: val('http_reqs', 'count'),
    http_reqs_per_s: val('http_reqs', 'rate'),
    iterations: val('iterations', 'count'),
    iterations_per_s: val('iterations', 'rate'),
    dropped_iterations: val('dropped_iterations', 'count'),
    http_req_failed_rate: val('http_req_failed', 'rate'),
    http_req_duration_p95_ms: val('http_req_duration', 'p(95)'),
    http_req_duration_p99_ms: val('http_req_duration', 'p(99)'),
    biz_status_200: val('biz_status_200', 'count'),
    biz_status_429: val('biz_status_429', 'count'),
    biz_status_5xx: val('biz_status_5xx', 'count'),
    biz_status_other: val('biz_status_other', 'count')
  }
  return { stdout: JSON.stringify(summary, null, 2) + '\n' }
}
