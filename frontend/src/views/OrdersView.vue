<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const list = ref([])
const err = ref('')
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await http.get('/c/orders')
    list.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>我的订单</h1>
        <p class="subtle">展示买家侧订单与明细快照。</p>
      </div>
      <button class="ghost" type="button" @click="() => location.reload()">刷新</button>
    </div>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-if="loading" class="card">加载中…</div>
    <div v-else-if="!list.length && !err" class="card empty">
      <span>你还没有订单。</span>
      <a class="ghost" href="/">去逛商品</a>
    </div>
    <div v-else class="grid">
      <div v-for="o in list" :key="o.id" class="card order">
        <div class="top">
          <span class="mono">{{ o.orderNo }}</span>
          <span class="price">{{ (o.totalCent / 100).toFixed(2) }} 元</span>
        </div>
        <div class="badges">
          <span class="badge">{{ o.status }}</span>
          <span class="badge ghosty">{{ o.orderType }}</span>
        </div>
        <div class="meta">下单时间：{{ o.createdAt?.replace('T', ' ') }}</div>
        <ul v-if="o.items?.length" class="items">
          <li v-for="(it, i) in o.items" :key="i">
            <span class="t">{{ it.titleSnapshot }}</span>
            <span class="q">x{{ it.quantity }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mono {
  font-family: monospace;
  font-size: 13px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.order {
  padding: 14px;
}
.top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
}
.price {
  font-weight: 800;
  color: #0f172a;
}
.badges {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.65);
  color: #334155;
}
.badge.ghosty {
  background: rgba(15, 23, 42, 0.04);
  color: #475569;
}
.meta {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}
.items {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}
.items li {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.55);
}
.t {
  color: #334155;
}
.q {
  font-family: monospace;
  color: #475569;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
