<script setup>
import { computed, ref, watch } from 'vue'
import http from '../api/http'

const orders = ref([])
const err = ref('')
const loadingOrders = ref(false)
const title = ref('新商品')
const sku = ref('SKU-DEMO-1')
const priceYuan = ref(199)
const stock = ref(100)

const publishSubmitting = ref(false)
const showConfirmModal = ref(false)
/** seconds until 「确定上架」 becomes clickable */
const confirmCooldown = ref(0)
let cooldownTick = null

const PAGE_SIZE = 5
const orderPage = ref(1)

const totalOrderPages = computed(() =>
  Math.max(1, Math.ceil(orders.value.length / PAGE_SIZE)),
)

const pagedOrders = computed(() => {
  const start = (orderPage.value - 1) * PAGE_SIZE
  return orders.value.slice(start, start + PAGE_SIZE)
})

watch(
  () => orders.value.length,
  () => {
    if (orderPage.value > totalOrderPages.value) {
      orderPage.value = totalOrderPages.value
    }
  },
)

watch(showConfirmModal, (open) => {
  if (cooldownTick) {
    clearInterval(cooldownTick)
    cooldownTick = null
  }
  if (open) {
    confirmCooldown.value = 3
    cooldownTick = setInterval(() => {
      confirmCooldown.value = Math.max(0, confirmCooldown.value - 1)
      if (confirmCooldown.value <= 0 && cooldownTick) {
        clearInterval(cooldownTick)
        cooldownTick = null
      }
    }, 1000)
  } else {
    confirmCooldown.value = 0
  }
})

function formatHttpError(e) {
  const st = e.response?.status
  const d = e.response?.data
  let msg =
    (typeof d === 'string' && d) ||
    d?.error ||
    d?.message ||
    e.message ||
    '请求失败'
  if (st) {
    msg += `（HTTP ${st}）`
  }
  return msg
}

function openPublishConfirm() {
  err.value = ''
  if (!title.value || !String(title.value).trim()) {
    err.value = '请填写商品名称'
    return
  }
  if (!sku.value || !String(sku.value).trim()) {
    err.value = '请填写 SKU 编码'
    return
  }
  const cent = Math.round(Number(priceYuan.value) * 100)
  if (!Number.isFinite(cent) || cent < 1) {
    err.value = '售价需大于 0（后端以分为单位，最低 1 分）'
    return
  }
  const st = Number(stock.value)
  if (!Number.isFinite(st) || st < 0 || !Number.isInteger(st)) {
    err.value = '库存需为不小于 0 的整数'
    return
  }
  showConfirmModal.value = true
}

function closeConfirmModal() {
  if (publishSubmitting.value) {
    return
  }
  showConfirmModal.value = false
}

async function loadOrders() {
  loadingOrders.value = true
  try {
    err.value = ''
    const { data } = await http.get('/b/orders')
    orders.value = Array.isArray(data) ? data : []
    orderPage.value = 1
  } catch (e) {
    err.value = formatHttpError(e)
    console.error('[merchant] load orders failed', e)
  } finally {
    loadingOrders.value = false
  }
}

async function confirmPublish() {
  if (confirmCooldown.value > 0 || publishSubmitting.value) {
    return
  }
  publishSubmitting.value = true
  err.value = ''
  try {
    const { data } = await http.post('/b/spus', {
      categoryId: 1,
      title: title.value.trim(),
      subtitle: '商家快速上架',
      detail: '',
    })
    const cent = Math.round(Number(priceYuan.value) * 100)
    await http.post(`/b/spus/${data.id}/skus`, {
      skuCode: sku.value.trim(),
      specJson: '{}',
      priceCent: cent,
      stock: Number(stock.value),
    })
    showConfirmModal.value = false
    await loadOrders()
    alert('商品已成功上架')
  } catch (e) {
    err.value = formatHttpError(e)
    console.error('[merchant] publish failed', e)
  } finally {
    publishSubmitting.value = false
  }
}

function goOrderPage(p) {
  const next = Math.min(Math.max(1, p), totalOrderPages.value)
  orderPage.value = next
}

function statusLabel(s) {
  const map = {
    PAID: '已付款',
    CREATED: '待支付',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return map[s] || s
}

function typeLabel(t) {
  const map = {
    SECKILL: '秒杀',
    NORMAL: '普通',
  }
  return map[t] || t
}

function formatMoney(cent) {
  return (Number(cent) / 100).toFixed(2)
}

function orderSummary(o) {
  const items = o.items || []
  if (!items.length) {
    return '—'
  }
  const first = items[0]
  const extra = items.length > 1 ? ` 等 ${items.length} 种商品` : ''
  return `${first.titleSnapshot} ×${first.quantity}${extra}`
}

loadOrders()
</script>

<template>
  <div class="merchant-shell">
    <header class="sell-head">
      <div class="sell-brand">
        <span class="sell-logo">商家工作台</span>
        <span class="sell-tag">演示 · 快速上架 / 订单管理</span>
      </div>
      <div class="sell-actions">
        <button class="btn-outline" type="button" :disabled="loadingOrders" @click="loadOrders">
          {{ loadingOrders ? '刷新中…' : '刷新订单' }}
        </button>
      </div>
    </header>

    <p v-if="err" class="sell-alert">{{ err }}</p>

    <div class="sell-grid">
      <!-- 左侧：上架（类京东 / 淘宝卖家中心卡片） -->
      <section class="sell-panel publish-panel">
        <h2 class="panel-title">发布宝贝</h2>
        <p class="panel-desc">填写基本信息与价格库存，确认后上架到卖场。</p>

        <div class="form-grid">
          <label class="fg-label">
            <span class="req">*</span> 商品名称
            <input v-model="title" class="fg-input" placeholder="例如：无线蓝牙耳机" maxlength="120" />
          </label>
          <label class="fg-label">
            <span class="req">*</span> 商家 SKU 编码
            <input v-model="sku" class="fg-input" placeholder="例如：SKU-DEMO-1" maxlength="64" />
            <small class="fg-hint">用于仓库与订单对应；买家看到的是商品标题与规格。</small>
          </label>
          <div class="fg-row">
            <label class="fg-label half">
              <span class="req">*</span> 售价（元）
              <input
                v-model.number="priceYuan"
                class="fg-input"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="199"
              />
            </label>
            <label class="fg-label half">
              <span class="req">*</span> 库存（件）
              <input v-model.number="stock" class="fg-input" type="number" min="0" step="1" placeholder="100" />
            </label>
          </div>
        </div>

        <div class="publish-footer">
          <button type="button" class="btn-primary-lg" @click="openPublishConfirm">提交上架</button>
          <RouterLink class="link-back" to="/">返回卖场</RouterLink>
        </div>
      </section>

      <!-- 右侧：订单列表（表格化 + 分页） -->
      <section class="sell-panel orders-panel">
        <div class="orders-toolbar">
          <h2 class="panel-title tight">卖出订单</h2>
          <span class="muted-count">共 {{ orders.length }} 笔</span>
        </div>

        <div v-if="!orders.length && !loadingOrders" class="empty-orders">
          <p>暂无关联订单</p>
          <span class="sub">买家下单包含您店铺商品后，会出现在这里。</span>
        </div>

        <div v-else class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th class="col-no">订单编号</th>
                <th class="col-sum">实收款</th>
                <th class="col-st">状态</th>
                <th class="col-time">下单时间</th>
                <th class="col-goods">商品摘要</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in pagedOrders" :key="o.id">
                <td class="mono">{{ o.orderNo }}</td>
                <td class="money">¥{{ formatMoney(o.totalCent) }}</td>
                <td>
                  <span class="pill-status">{{ statusLabel(o.status) }}</span>
                  <span class="pill-type">{{ typeLabel(o.orderType) }}</span>
                </td>
                <td class="time">{{ o.createdAt?.replace('T', ' ')?.slice(0, 19) }}</td>
                <td class="goods">{{ orderSummary(o) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="orders.length > PAGE_SIZE" class="pager">
          <button type="button" class="btn-page" :disabled="orderPage <= 1" @click="goOrderPage(orderPage - 1)">
            上一页
          </button>
          <span class="page-info">{{ orderPage }} / {{ totalOrderPages }}</span>
          <button
            type="button"
            class="btn-page"
            :disabled="orderPage >= totalOrderPages"
            @click="goOrderPage(orderPage + 1)"
          >
            下一页
          </button>
        </div>
      </section>
    </div>

    <!-- 二次确认：倒计时后才能点「确定上架」 -->
    <Teleport to="body">
      <div v-if="showConfirmModal" class="modal-mask" @click.self="closeConfirmModal">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
          <h3 id="confirm-title" class="modal-title">确认上架商品？</h3>
          <p class="modal-body">
            商品：<strong>{{ title }}</strong><br />
            SKU：<strong>{{ sku }}</strong> · 售价 ¥{{ Number(priceYuan).toFixed(2) }} · 库存 {{ stock }} 件
          </p>
          <p v-if="confirmCooldown > 0" class="modal-countdown">请核对信息，{{ confirmCooldown }} 秒后可确认上架</p>
          <div class="modal-actions">
            <button type="button" class="btn-outline" :disabled="publishSubmitting" @click="closeConfirmModal">
              取消
            </button>
            <button
              type="button"
              class="btn-danger"
              :disabled="confirmCooldown > 0 || publishSubmitting"
              @click="confirmPublish"
            >
              {{ publishSubmitting ? '提交中…' : confirmCooldown > 0 ? `确定上架 (${confirmCooldown}s)` : '确定上架' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.merchant-shell {
  --sell-bg: #e9ecef;
  --sell-card: #ffffff;
  --sell-text: #222;
  --sell-muted: #666;
  --sell-border: #d9d9d9;
  --sell-orange: #ff5000;
  --sell-orange-dark: #e64500;
  --sell-blue: #1677ff;
  min-height: calc(100vh - 120px);
  margin: 0 -8px;
  padding: 16px 12px 28px;
  border-radius: 8px;
  background: var(--sell-bg);
  color: var(--sell-text);
}

.sell-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  background: linear-gradient(90deg, #ffede1 0%, #fff6f0 40%, #ffffff 100%);
  border: 1px solid #ffc8a8;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.sell-brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sell-logo {
  font-size: 20px;
  font-weight: 800;
  color: #c2410c;
  letter-spacing: 0.5px;
}

.sell-tag {
  font-size: 12px;
  color: var(--sell-muted);
}

.sell-alert {
  margin: 0 0 12px;
  padding: 10px 14px;
  border-radius: 6px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #a8071a;
  font-size: 14px;
}

.sell-grid {
  display: grid;
  grid-template-columns: minmax(300px, 400px) 1fr;
  gap: 14px;
  align-items: start;
}

.sell-panel {
  background: var(--sell-card);
  border: 1px solid var(--sell-border);
  border-radius: 8px;
  padding: 18px 18px 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.panel-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--sell-text);
}

.panel-title.tight {
  margin-bottom: 0;
}

.panel-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--sell-muted);
  line-height: 1.5;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fg-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.req {
  color: var(--sell-orange);
  margin-right: 2px;
}

.fg-input {
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
  color: #222;
}

.fg-input:focus {
  outline: none;
  border-color: var(--sell-blue);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15);
}

.fg-hint {
  font-weight: 400;
  color: #888;
  font-size: 12px;
  line-height: 1.45;
}

.fg-row {
  display: flex;
  gap: 12px;
}

.fg-label.half {
  flex: 1;
}

.publish-footer {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-primary-lg {
  padding: 11px 28px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(180deg, var(--sell-orange) 0%, var(--sell-orange-dark) 100%);
  border: 1px solid #d4380d;
  border-radius: 4px;
  cursor: pointer;
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.04);
}

.btn-primary-lg:hover {
  filter: brightness(1.03);
}

.btn-outline {
  padding: 8px 16px;
  font-size: 13px;
  color: #333;
  background: #fff;
  border: 1px solid #bbb;
  border-radius: 4px;
  cursor: pointer;
}

.btn-outline:hover:not(:disabled) {
  border-color: var(--sell-blue);
  color: var(--sell-blue);
}

.btn-outline:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.link-back {
  font-size: 13px;
  color: var(--sell-blue);
  text-decoration: none;
}

.link-back:hover {
  text-decoration: underline;
}

.orders-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 2px solid #ffefe6;
}

.muted-count {
  font-size: 13px;
  color: var(--sell-muted);
}

.empty-orders {
  padding: 36px 12px;
  text-align: center;
  color: var(--sell-muted);
}

.empty-orders p {
  margin: 0 0 6px;
  font-size: 15px;
  color: #444;
}

.empty-orders .sub {
  font-size: 13px;
}

.table-wrap {
  overflow-x: auto;
}

.order-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.order-table th {
  text-align: left;
  padding: 10px 8px;
  background: #fafafa;
  border-bottom: 1px solid var(--sell-border);
  color: #333;
  font-weight: 700;
  white-space: nowrap;
}

.order-table td {
  padding: 12px 8px;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: top;
  color: #222;
}

.order-table tbody tr:hover {
  background: #fffbf7;
}

.col-no {
  min-width: 140px;
}
.col-sum {
  min-width: 88px;
}
.col-st {
  min-width: 120px;
}
.col-time {
  min-width: 150px;
}
.col-goods {
  min-width: 200px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  color: #333;
}

.money {
  font-weight: 800;
  color: var(--sell-orange);
  white-space: nowrap;
}

.time {
  color: #555;
  font-size: 12px;
  white-space: nowrap;
}

.goods {
  color: #333;
  line-height: 1.45;
}

.pill-status,
.pill-type {
  display: inline-block;
  margin: 2px 4px 2px 0;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  border: 1px solid #d9d9d9;
  background: #fafafa;
}

.pill-type {
  background: #e6f4ff;
  border-color: #91caff;
  color: #0958d9;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.btn-page {
  padding: 6px 14px;
  font-size: 13px;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
}

.btn-page:hover:not(:disabled) {
  border-color: var(--sell-orange);
  color: var(--sell-orange);
}

.btn-page:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--sell-muted);
}

/* Modal */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 8px;
  padding: 22px 22px 18px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
  border: 1px solid #e8e8e8;
}

.modal-title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #222;
}

.modal-body {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #444;
}

.modal-countdown {
  margin: 0 0 16px;
  font-size: 13px;
  color: #d4380d;
  font-weight: 600;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-danger {
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: var(--sell-orange);
  border: 1px solid #d4380d;
  border-radius: 4px;
  cursor: pointer;
}

.btn-danger:hover:not(:disabled) {
  filter: brightness(1.05);
}

.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 920px) {
  .sell-grid {
    grid-template-columns: 1fr;
  }
  .fg-row {
    flex-direction: column;
  }
}
</style>
