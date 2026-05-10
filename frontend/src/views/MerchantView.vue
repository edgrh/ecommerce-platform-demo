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
    <div class="page-head">
      <div>
        <h1>商家后台</h1>
        <p class="subtle">快速上架商品，并查看与本商家关联的订单。</p>
      </div>
      <div class="toolbar">
        <button class="ghost" type="button" @click="loadOrders">刷新订单</button>
      </div>
    </div>

    <p v-if="err" class="err">{{ err }}</p>

    <div class="grid">
      <div class="card">
        <div class="section-title">快速上架</div>
        <div class="field">
          <label>商品名称</label>
          <input v-model="title" placeholder="例如：电脑配件" />
        </div>
        <div class="field">
          <label>SKU 编码</label>
          <input v-model="sku" placeholder="例如：SKU-DEMO-1" />
          <small class="hint">货号/规格编码，便于仓库与订单对应；可自定义；库存单位在「库存（件）」里体现。</small>
        </div>
        <div class="two">
          <div class="field">
            <label>售价（元）</label>
            <input v-model.number="priceYuan" type="number" min="0" step="0.01" placeholder="199" />
          </div>
          <div class="field">
            <label>库存（件）</label>
            <input v-model.number="stock" type="number" min="0" step="1" placeholder="100" />
          </div>
        </div>
        <div class="actions">
          <button class="primary" type="button" @click="publish">提交上架</button>
        </div>
      </div>

      <div class="card">
        <div class="section-title">关联订单</div>
        <p v-if="!orders.length" class="hint">当前暂无关联订单，买家完成下单后会显示在这里。</p>
        <div v-for="o in orders" :key="o.id" class="order">
          <div class="order-top">
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
              <span class="q"
                >x{{ it.quantity }} · {{ (it.unitPriceCent / 100).toFixed(2) }} 元</span
              >
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 12px;
  align-items: start;
}
.section-title {
  font-weight: 900;
  margin-bottom: 12px;
}
.hint {
  display: block;
  margin-top: 4px;
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.4;
}
.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.actions {
  margin-top: 8px;
}
.order-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}
.price {
  font-weight: 900;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}
.meta {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}
.order {
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
  padding-top: 12px;
  margin-top: 12px;
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
@media (max-width: 880px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .two {
    grid-template-columns: 1fr;
  }
}
</style>
