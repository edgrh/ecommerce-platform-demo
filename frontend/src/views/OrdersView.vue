<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'

const list = ref([])
const err = ref('')
const loading = ref(true)
const page = ref(1)
const PAGE_SIZE = 10

const totalPages = computed(() => Math.max(1, Math.ceil(list.value.length / PAGE_SIZE)))

const pagedList = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return list.value.slice(start, start + PAGE_SIZE)
})

function statusBadge(status) {
  const map = {
    PAID: { text: '已付款', abbr: 'P', cls: 'st-paid' },
    CREATED: { text: '待支付', abbr: '?', cls: 'st-wait' },
    SHIPPED: { text: '已发货', abbr: '发', cls: 'st-ship' },
    COMPLETED: { text: '已完成', abbr: '完', cls: 'st-done' },
    CANCELLED: { text: '已取消', abbr: '×', cls: 'st-cancel' },
  }
  const m = map[status] || { text: status, abbr: status?.slice(0, 1) || '?', cls: 'st-other' }
  return m
}

function typeBadge(orderType) {
  const map = {
    SECKILL: { text: '秒杀', abbr: '秒', cls: 'tp-seckill' },
    NORMAL: { text: '普通订单', abbr: '普', cls: 'tp-normal' },
  }
  return map[orderType] || { text: orderType, abbr: orderType?.slice(0, 1) || '?', cls: 'tp-other' }
}

async function load() {
  loading.value = true
  err.value = ''
  try {
    const { data } = await http.get('/c/orders')
    list.value = Array.isArray(data) ? data : []
    page.value = 1
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  page.value = Math.min(Math.max(1, p), totalPages.value)
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>我的订单</h1>
        <p class="subtle">买家订单与明细；订单状态、类型均带标签。</p>
      </div>
      <button class="ghost" type="button" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新' }}</button>
    </div>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-if="loading" class="card">加载中…</div>
    <div v-else-if="!list.length && !err" class="card empty">
      <span>你还没有订单。</span>
      <RouterLink class="ghost" to="/" style="text-decoration: none; display: inline-block">去逛商品</RouterLink>
    </div>
    <template v-else>
      <div class="grid">
        <div v-for="o in pagedList" :key="o.id" class="card order">
          <div class="top">
            <span class="mono">{{ o.orderNo }}</span>
            <span class="price">¥{{ (o.totalCent / 100).toFixed(2) }}</span>
          </div>
          <div class="badge-row">
            <span class="tag-label">状态</span>
            <span class="tag" :class="statusBadge(o.status).cls" :title="o.status">
              <span class="tag-abbr">{{ statusBadge(o.status).abbr }}</span>
              {{ statusBadge(o.status).text }}
            </span>
            <span class="tag-label">类型</span>
            <span class="tag" :class="typeBadge(o.orderType).cls" :title="o.orderType">
              <span class="tag-abbr">{{ typeBadge(o.orderType).abbr }}</span>
              {{ typeBadge(o.orderType).text }}
            </span>
          </div>
          <div class="meta">下单时间：{{ o.createdAt?.replace('T', ' ')?.slice(0, 19) }}</div>
          <ul v-if="o.items?.length" class="items">
            <li v-for="(it, i) in o.items" :key="i">
              <span class="t">{{ it.titleSnapshot }}</span>
              <span class="q">×{{ it.quantity }} · ¥{{ (it.unitPriceCent / 100).toFixed(2) }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div v-if="list.length > PAGE_SIZE" class="pager">
        <button type="button" class="ghost" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="pi">{{ page }} / {{ totalPages }}（共 {{ list.length }} 笔）</span>
        <button type="button" class="ghost" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: #334155;
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
  color: #e11d48;
  font-size: 18px;
}
.badge-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.tag-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.tag-abbr {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.75);
}
.st-paid {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.35);
  color: #166534;
}
.st-wait {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.35);
  color: #92400e;
}
.st-ship {
  background: rgba(37, 99, 235, 0.1);
  border-color: rgba(37, 99, 235, 0.3);
  color: #1e40af;
}
.st-done {
  background: rgba(100, 116, 139, 0.12);
  border-color: rgba(100, 116, 139, 0.28);
  color: #334155;
}
.st-cancel {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #991b1b;
}
.st-other {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
}
.tp-seckill {
  background: rgba(225, 29, 72, 0.1);
  border-color: rgba(225, 29, 72, 0.35);
  color: #9f1239;
}
.tp-normal {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.32);
  color: #0369a1;
}
.tp-other {
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #64748b;
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
  background: #f8fafc;
}
.t {
  color: #334155;
}
.q {
  font-family: monospace;
  color: #475569;
  white-space: nowrap;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 16px;
  flex-wrap: wrap;
}
.pi {
  font-size: 14px;
  color: #64748b;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
