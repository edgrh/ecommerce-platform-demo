import http from 'k6/http'
import { check, sleep } from 'k6'

// Run with:
//   docker run --rm -i docker.m.daocloud.io/grafana/k6:0.51.0 run - < perf/k6-realistic-10k.js
//
// Env:
//   BASE_URL=http://127.0.0.1:8080   (gateway, recommended on cloud host)
//   RATE=10000                       (target RPS, close to QPS)
//   DURATION=2m
//   PRE_VUS=3000
//   MAX_VUS=30000
//
// Weights (sum doesn't need to be 100):
//   W_PRODUCTS=55 W_DETAIL=25 W_SECKILL_LIST=10 W_CART=8 W_SECKILL_ORDER=2

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080'
const RATE = Number(__ENV.RATE || 10000)
const DURATION = __ENV.DURATION || '2m'
const PRE_VUS = Number(__ENV.PRE_VUS || 2000)
const MAX_VUS = Number(__ENV.MAX_VUS || 20000)

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

function pick() {
  let r = Math.random() * WEIGHT_SUM
  for (const [k, w] of WEIGHTS) {
    r -= w
    if (r <= 0) return k
  }
  return 'products'
}

function jsonHeaders(token) {
  const h = { 'Content-Type': 'application/json' }
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

function loginBuyer() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: 'buyer', password: 'demo123' }),
    { headers: jsonHeaders() }
  )
  check(res, { 'login 200': (r) => r.status === 200 })
  try {
    return res.json('token')
  } catch {
    return null
  }
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
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500']
  },
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
      const res = http.get(`${BASE_URL}/api/c/products/search?q=${encodeURIComponent(q)}&limit=50`)
      check(res, { 'search 200': (r) => r.status === 200 })
    } else {
      const res = http.get(`${BASE_URL}/api/c/products?limit=50`)
      check(res, { 'products 200': (r) => r.status === 200 })
    }
    sleep(Math.random() * 0.05)
    return
  }

  if (flow === 'detail') {
    const id = 1 + Math.floor(Math.random() * 6)
    const res = http.get(`${BASE_URL}/api/c/products/${id}`)
    check(res, { 'detail 200': (r) => r.status === 200 })
    sleep(Math.random() * 0.05)
    return
  }

  if (flow === 'seckill_list') {
    const res = http.get(`${BASE_URL}/api/c/seckill/activities`)
    check(res, { 'seckill activities 200': (r) => r.status === 200 })
    sleep(Math.random() * 0.03)
    return
  }

  if (flow === 'cart') {
    if (!token) return
    const listRes = http.get(`${BASE_URL}/api/c/cart`, { headers: jsonHeaders(token) })
    check(listRes, { 'cart list 200': (r) => r.status === 200 })

    if (Math.random() < 0.35) {
      const skuId = 1 + Math.floor(Math.random() * 6)
      const addRes = http.post(
        `${BASE_URL}/api/c/cart/add`,
        JSON.stringify({ skuId, quantity: 1 }),
        { headers: jsonHeaders(token) }
      )
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
      { headers: { ...jsonHeaders(token), 'X-Idempotency-Key': key } }
    )
    check(res, { 'seckill order responded': (r) => r.status >= 200 && r.status < 500 })
    sleep(Math.random() * 0.02)
  }
}

