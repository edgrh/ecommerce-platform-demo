<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const list = ref([])
const err = ref('')

onMounted(async () => {
  try {
    const { data } = await http.get('/c/orders')
    list.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
})
</script>

<template>
  <div>
    <h1>我的订单</h1>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-for="o in list" :key="o.id" class="card">
      <div class="mono">{{ o.orderNo }}</div>
      <div>{{ (o.totalCent / 100).toFixed(2) }} 元，{{ o.status }}</div>
      <div class="meta">类型：{{ o.orderType }} · 下单时间：{{ o.createdAt?.replace('T', ' ') }}</div>
      <ul>
        <li v-for="(it, i) in o.items" :key="i">{{ it.titleSnapshot }} x{{ it.quantity }}</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.mono {
  font-family: monospace;
  font-size: 13px;
}
ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
  font-size: 14px;
}
.meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}
</style>
