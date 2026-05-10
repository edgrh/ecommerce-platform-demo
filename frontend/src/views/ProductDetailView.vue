<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const p = ref(null)
const err = ref('')
const msg = ref('')
const busy = ref(false)
const auth = useAuthStore()
const skuId = ref(null)

async function load() {
  err.value = ''
  msg.value = ''
  p.value = null
  skuId.value = null
  const id = route.params.id
  if (!id) {
    err.value = '无效商品'
    return
  }
  try {
    const { data } = await http.get(`/c/products/${id}`)
    p.value = data
    skuId.value = Array.isArray(data.skus) && data.skus.length ? data.skus[0].id : null
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
}

async function addToCart() {
  msg.value = ''
  err.value = ''
  if (!auth.token) {
    await router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
    return
  }
  if (auth.role !== 'CUSTOMER') {
    err.value = '当前为商家账号，无法加入购物车'
    return
  }
  if (!skuId.value) {
    err.value = '该商品暂无可用 SKU'
    return
  }
  busy.value = true
  try {
    await http.post('/c/cart/add', { skuId: skuId.value, quantity: 1 })
    msg.value = '已加入购物车'
  } catch (e) {
    err.value = e.response?.data?.error || '操作失败'
  } finally {
    busy.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div>
    <p class="back">
      <button type="button" class="linkish" @click="router.back()">← 返回</button>
      <RouterLink to="/">首页商品</RouterLink>
    </p>
    <h1>商品详情</h1>
    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="msg" class="ok">{{ msg }}</p>
    <div v-if="p" class="card">
      <h2>{{ p.title }}</h2>
      <p class="muted">{{ p.subtitle }}</p>
      <div class="detail">{{ p.detail }}</div>
      <div class="actions">
        <button class="primary" type="button" :disabled="busy" @click="addToCart">加入购物车</button>
        <RouterLink v-if="auth.token && auth.role === 'CUSTOMER'" to="/cart">去购物车</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.back {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}
.linkish {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  padding: 0;
  font: inherit;
  text-decoration: underline;
}
.muted {
  color: #64748b;
  margin: 8px 0 16px;
}
.detail {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #334155;
}
.actions {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  align-items: center;
}
.ok {
  color: #16a34a;
}
</style>
