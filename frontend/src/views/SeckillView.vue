<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const acts = ref([])
const err = ref('')
const busy = ref(false)
const qty = ref(1)
function formatTimeRange(a) {
  if (!a.startTime || !a.endTime) return ''
  const s = new Date(a.startTime)
  const e = new Date(a.endTime)
  const pad = (n) => n.toString().padStart(2, '0')
  const fmt = (d) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
      d.getMinutes()
    )}`
  return `${fmt(s)} ~ ${fmt(e)}`
}

const isBuyer = computed(() => (auth.role || '').toUpperCase() === 'CUSTOMER')
const roleHint = computed(() => {
  if (!auth.token) return '登录后即可抢购（请使用买家账号 buyer / demo123）。'
  if (!isBuyer.value) return '当前为商家账号，无法秒杀；请退出后使用买家账号 buyer / demo123 登录。'
  return ''
})

onMounted(async () => {
  err.value = ''
  try {
    const { data } = await http.get('/c/seckill/activities')
    acts.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
})

async function buy(id) {
  if (!auth.token) {
    await router.push('/login')
    return
  }
  if (!isBuyer.value) {
    err.value = '请使用买家账号 buyer / demo123 登录后再抢购'
    return
  }
  busy.value = true
  err.value = ''
  try {
    const { data } = await http.post(
      '/c/seckill/orders',
      { activityId: id, quantity: qty.value },
      { headers: { 'X-Idempotency-Key': `${id}-${Date.now()}` } }
    )
    await router.push('/orders')
    alert('下单成功，订单号 ' + data.orderNo)
  } catch (e) {
    const msg = e.response?.data?.error
    err.value = msg || e.message || '下单失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>秒杀</h1>
        <p class="subtle">Redis 预减 + 数据库事务；支付为 Resilience4j 熔断 mock。</p>
      </div>
    </div>
    <p class="hint">
      每个秒杀活动<strong>固定绑定一款商品</strong>（由商家在后台配置），本页无需再选商品；下方卡片即活动与对应商品。
    </p>
    <p v-if="roleHint" class="warn">{{ roleHint }}</p>
    <div class="toolbar">
      <label class="pill">
        数量
        <input v-model.number="qty" type="number" min="1" max="5" style="width: 84px" />
      </label>
      <span class="muted">建议 1~2 件，避免触发限购或限流。</span>
    </div>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-for="a in acts" :key="a.id" class="card">
      <div class="t">{{ a.name }}</div>
      <div v-if="a.productTitle" class="product">
        商品：
        <RouterLink v-if="a.spuId" :to="'/products/' + a.spuId" class="plink">{{ a.productTitle }}</RouterLink>
        <template v-else>{{ a.productTitle }}</template>
      </div>
      <div class="time" v-if="a.startTime && a.endTime">活动时间：{{ formatTimeRange(a) }}</div>
      <div>秒杀价 {{ (a.seckillPriceCent / 100).toFixed(2) }} 元，剩余 {{ a.remaining }} 件</div>
      <div v-if="a.limitPerUser" class="limit">每人限购 {{ a.limitPerUser }} 件</div>
      <button
        class="primary"
        type="button"
        :disabled="busy || !isBuyer"
        @click="buy(a.id)"
      >
        抢购
      </button>
    </div>
  </div>
</template>

<style scoped>
.hint {
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.5;
  margin-bottom: 12px;
}
.t {
  font-weight: 700;
  margin-bottom: 6px;
}
.product {
  font-size: 0.95rem;
  margin-bottom: 6px;
  color: #334155;
}
.plink {
  color: #2563eb;
  text-decoration: underline;
}
.plink:hover {
  color: #1d4ed8;
}
.limit {
  font-size: 0.85rem;
  color: #64748b;
  margin: 4px 0 8px;
}
</style>
