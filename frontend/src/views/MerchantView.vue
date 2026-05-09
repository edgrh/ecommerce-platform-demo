<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const orders = ref([])
const err = ref('')
const title = ref('新商品')
/** 商家自编货号/规格编码（SKU），用于区分不同规格，无固定单位 */
const sku = ref('SKU-DEMO-1')
/** 售价（元），提交时换算为分 */
const priceYuan = ref(199)
/** 可售库存，按「件」计 */
const stock = ref(100)

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    err.value = ''
    const { data } = await http.get('/b/orders')
    orders.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
}

async function publish() {
  err.value = ''
  try {
    const { data } = await http.post('/b/spus', {
      categoryId: 1,
      title: title.value,
      subtitle: '商家快速上架',
      detail: ''
    })
    const cent = Math.round(Number(priceYuan.value) * 100)
    if (!Number.isFinite(cent) || cent < 0) {
      err.value = '请输入有效价格（元）'
      return
    }
    await http.post(`/b/spus/${data.id}/skus`, {
      skuCode: sku.value,
      specJson: '{}',
      priceCent: cent,
      stock: stock.value
    })
    await loadOrders()
    alert('已上架')
  } catch (e) {
    err.value = e.response?.data?.error || '操作失败'
  }
}
</script>

<template>
  <div>
    <h1>商家后台</h1>
    <div class="card">
      <h3>快速上架</h3>
      <label class="field">
        <span>商品名称</span>
        <input v-model="title" placeholder="例如：电脑配件" />
      </label>
      <label class="field">
        <span>SKU 编码</span>
        <input v-model="sku" placeholder="例如：SKU-DEMO-1" />
        <small class="hint">货号/规格编码，便于仓库与订单对应；可自定义，无「个/件」含义，单位在库存里体现。</small>
      </label>
      <label class="field">
        <span>售价（元）</span>
        <input v-model.number="priceYuan" type="number" min="0" step="0.01" placeholder="199" />
      </label>
      <label class="field">
        <span>库存（件）</span>
        <input v-model.number="stock" type="number" min="0" step="1" placeholder="100" />
      </label>
      <button class="primary" type="button" @click="publish">提交</button>
    </div>
    <h2>关联订单</h2>
    <button class="ghost" type="button" @click="loadOrders">刷新订单</button>
    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="!orders.length" class="hint">当前暂无关联订单，买家完成下单后会显示在这里。</p>
    <div v-for="o in orders" :key="o.id" class="card">
      <div class="order-top">
        <span class="mono">{{ o.orderNo }}</span>
        <span>{{ (o.totalCent / 100).toFixed(2) }} 元</span>
      </div>
      <div class="meta">类型：{{ o.orderType }} · 状态：{{ o.status }}</div>
      <div class="meta">下单时间：{{ o.createdAt?.replace('T', ' ') }}</div>
      <ul v-if="o.items?.length">
        <li v-for="(it, i) in o.items" :key="i">
          {{ it.titleSnapshot }} x{{ it.quantity }}，单价 {{ (it.unitPriceCent / 100).toFixed(2) }} 元
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.field {
  display: block;
  margin-bottom: 12px;
}
.field > span {
  display: block;
  font-size: 0.9rem;
  margin-bottom: 4px;
  color: #444;
}
.field input {
  display: block;
  width: 100%;
  box-sizing: border-box;
}
.hint {
  display: block;
  margin-top: 4px;
  font-size: 0.8rem;
  color: #666;
  line-height: 1.4;
}
.ghost {
  margin-bottom: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
}
.order-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}
.meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}
ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
  font-size: 14px;
}
</style>
