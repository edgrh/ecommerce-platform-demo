<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const list = ref([])
const err = ref('')
const busy = ref(false)

function totalCent() {
  return list.value.reduce((s, it) => s + (it.unitPriceCent || 0) * (it.quantity || 0), 0)
}

async function load() {
  err.value = ''
  try {
    const { data } = await http.get('/c/cart')
    list.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
}

async function setQty(skuId, qty) {
  busy.value = true
  err.value = ''
  try {
    await http.post('/c/cart/set', { skuId, quantity: qty })
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || '操作失败'
  } finally {
    busy.value = false
  }
}

async function remove(skuId) {
  busy.value = true
  err.value = ''
  try {
    await http.post('/c/cart/remove', { skuId, quantity: 0 })
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || '操作失败'
  } finally {
    busy.value = false
  }
}

async function clearAll() {
  busy.value = true
  err.value = ''
  try {
    await http.delete('/c/cart')
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || '操作失败'
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>购物车</h1>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="!list.length" class="card">购物车是空的。</div>

    <div v-for="it in list" :key="it.skuId" class="card row">
      <div class="left">
        <div class="title">{{ it.title || '商品' }}</div>
        <div class="meta">SKU：{{ it.skuId }} · 单价：{{ (it.unitPriceCent / 100).toFixed(2) }} 元</div>
      </div>
      <div class="right">
        <button type="button" :disabled="busy" @click="setQty(it.skuId, Math.max(1, it.quantity - 1))">-</button>
        <span class="qty">{{ it.quantity }}</span>
        <button type="button" :disabled="busy" @click="setQty(it.skuId, Math.min(99, it.quantity + 1))">+</button>
        <button type="button" class="danger" :disabled="busy" @click="remove(it.skuId)">删除</button>
      </div>
    </div>

    <div v-if="list.length" class="card total">
      <div>合计：<strong>{{ (totalCent() / 100).toFixed(2) }}</strong> 元</div>
      <button type="button" class="danger" :disabled="busy" @click="clearAll">清空购物车</button>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.title {
  font-weight: 700;
}
.meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}
.right {
  display: flex;
  gap: 8px;
  align-items: center;
  white-space: nowrap;
}
.qty {
  display: inline-block;
  min-width: 24px;
  text-align: center;
  font-family: monospace;
}
button {
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.danger {
  border-color: #fecaca;
  color: #b91c1c;
}
.total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
</style>

