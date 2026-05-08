<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const acts = ref([])
const err = ref('')
const busy = ref(false)
const qty = ref(1)

onMounted(async () => {
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
    err.value = e.response?.data?.error || '下单失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div>
    <h1>秒杀</h1>
    <p class="sub">Redis 预减 + 数据库事务；支付为 Resilience4j 熔断 mock。</p>
    <label>数量 <input v-model.number="qty" type="number" min="1" max="5" /></label>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-for="a in acts" :key="a.id" class="card">
      <div class="t">{{ a.name }}</div>
      <div>秒杀价 {{ (a.seckillPriceCent / 100).toFixed(2) }} 元，剩余 {{ a.remaining }}</div>
      <button class="primary" type="button" :disabled="busy" @click="buy(a.id)">抢购</button>
    </div>
  </div>
</template>

<style scoped>
.sub {
  color: #64748b;
  margin-bottom: 12px;
}
.t {
  font-weight: 700;
  margin-bottom: 6px;
}
</style>
