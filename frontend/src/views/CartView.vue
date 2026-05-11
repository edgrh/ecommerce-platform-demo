<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
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

async function checkout() {
  busy.value = true
  err.value = ''
  try {
    const { data } = await http.post(
      '/c/cart/checkout',
      {},
      {
        headers: {
          'X-Idempotency-Key': 'cart-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10),
        },
      },
    )
    await load()
    alert('支付成功（模拟），订单号 ' + data.orderNo + '。可在「我的订单」查看，商家后台可看到关联订单。')
    await router.push('/orders')
  } catch (e) {
    err.value = e.response?.data?.error || e.message || '结算失败'
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>购物车</h1>
        <p class="subtle">数量变更会实时写入 Redis。</p>
      </div>
      <div class="toolbar">
        <button v-if="list.length" type="button" class="ghost danger" :disabled="busy" @click="clearAll">
          清空
        </button>
      </div>
    </div>

    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="!list.length" class="card empty">
      <span>购物车是空的。</span>
      <a class="ghost" href="/">去逛商品</a>
    </div>

    <div v-if="list.length" class="grid">
      <div v-for="it in list" :key="it.skuId" class="card row">
        <div class="left">
          <div class="title">{{ it.title || '商品' }}</div>
          <div class="meta">
            <span class="badge">SKU {{ it.skuId }}</span>
            <span class="badge ghosty">单价 {{ (it.unitPriceCent / 100).toFixed(2) }} 元</span>
          </div>
        </div>
        <div class="right">
          <div class="stepper">
            <button
              type="button"
              :disabled="busy"
              class="sbtn"
              @click="setQty(it.skuId, Math.max(1, it.quantity - 1))"
            >
              −
            </button>
            <span class="qty">{{ it.quantity }}</span>
            <button
              type="button"
              :disabled="busy"
              class="sbtn"
              @click="setQty(it.skuId, Math.min(99, it.quantity + 1))"
            >
              +
            </button>
          </div>
          <button type="button" class="ghost danger" :disabled="busy" @click="remove(it.skuId)">删除</button>
        </div>
      </div>

      <div class="card total">
        <div class="sum">
          <div class="muted">合计</div>
          <div class="money">{{ (totalCent() / 100).toFixed(2) }} 元</div>
        </div>
        <div class="pay-row">
          <button type="button" class="primary pay" :disabled="busy" @click="checkout">
            {{ busy ? '处理中…' : '去支付（模拟）' }}
          </button>
          <span class="hint">走与秒杀相同的 mock 支付链路，生成普通订单并出现在「我的订单」与商家后台。</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
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
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.right {
  display: flex;
  gap: 8px;
  align-items: center;
  white-space: nowrap;
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
.danger {
  color: #b91c1c;
  border-color: rgba(248, 113, 113, 0.35);
}
.total {
  grid-column: 1 / -1;
  display: grid;
  gap: 8px;
}
.sum {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.money {
  font-size: 20px;
  font-weight: 900;
}
.hint {
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
  flex: 1;
  min-width: 200px;
}
.pay-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}
.pay {
  padding: 10px 22px;
  font-size: 15px;
}
.stepper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.7);
  padding: 6px 8px;
}
.sbtn {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  line-height: 30px;
  font-size: 18px;
}
.qty {
  display: inline-block;
  min-width: 28px;
  text-align: center;
  font-family: monospace;
  font-size: 14px;
  color: #0f172a;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .right {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}
</style>

