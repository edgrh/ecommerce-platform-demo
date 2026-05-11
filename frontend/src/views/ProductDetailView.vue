<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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
const selectedStorage = ref('')
const selectedColorKey = ref('')

function parseSpec(json) {
  if (!json || typeof json !== 'string') {
    return {}
  }
  try {
    return JSON.parse(json)
  } catch {
    return {}
  }
}

/** 英文 color / colorName / 常见拼写错误 → 中文（与后端 SkuSpecSummary 一致） */
function colorKeyToCn(raw) {
  if (!raw || typeof raw !== 'string') {
    return ''
  }
  const k = raw
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_')
  const map = {
    silver: '银色',
    sliver: '银色',
    gold: '金色',
    deep_blue: '深蓝色',
    deepblue: '深蓝色',
    black: '深空黑色',
    space_black: '深空黑色',
    spaceblack: '深空黑色',
    midnight: '午夜色',
  }
  return map[k] || ''
}

function hasHan(s) {
  return typeof s === 'string' && /[\u4e00-\u9fff]/.test(s)
}

/** 颜色芯片文案：优先已有中文 colorName，否则由英文 color / colorName 映射 */
function colorChipLabel(j) {
  if (j.colorName && hasHan(j.colorName)) {
    return j.colorName
  }
  const fromColor = colorKeyToCn(j.color)
  if (fromColor) {
    return fromColor
  }
  const fromName = colorKeyToCn(j.colorName)
  if (fromName) {
    return fromName
  }
  return j.colorName || j.color || '默认'
}

const storages = computed(() => {
  const skus = p.value?.skus || []
  const set = new Map()
  for (const s of skus) {
    const j = parseSpec(s.specJson)
    if (j.storage) {
      set.set(j.storage, j.storage)
    }
  }
  return Array.from(set.keys())
})

const colorsForStorage = computed(() => {
  const skus = p.value?.skus || []
  const st = selectedStorage.value
  const seen = new Set()
  const out = []
  for (const s of skus) {
    const j = parseSpec(s.specJson)
    if (j.storage !== st) {
      continue
    }
    const key = j.color || j.colorName || ''
    if (seen.has(key)) {
      continue
    }
    seen.add(key)
    out.push({
      key: j.color || j.colorName || '',
      label: colorChipLabel(j),
    })
  }
  return out
})

const selectedSku = computed(() => {
  const skus = p.value?.skus || []
  const st = selectedStorage.value
  const ck = selectedColorKey.value
  return (
    skus.find((s) => {
      const j = parseSpec(s.specJson)
      return j.storage === st && (j.color === ck || j.colorName === ck || (!j.color && !ck))
    }) || null
  )
})

watch(selectedSku, (s) => {
  skuId.value = s?.id ?? null
})

async function load() {
  err.value = ''
  msg.value = ''
  p.value = null
  skuId.value = null
  selectedStorage.value = ''
  selectedColorKey.value = ''
  const id = route.params.id
  if (!id) {
    err.value = '无效商品'
    return
  }
  try {
    const { data } = await http.get(`/c/products/${id}`)
    p.value = data
    const skus = Array.isArray(data.skus) ? data.skus : []
    if (skus.length === 1) {
      skuId.value = skus[0].id
      const j = parseSpec(skus[0].specJson)
      selectedStorage.value = j.storage || ''
      selectedColorKey.value = j.color || j.colorName || ''
    } else if (skus.length > 1) {
      const j0 = parseSpec(skus[0].specJson)
      selectedStorage.value = j0.storage || ''
      const sameSt = skus.filter((s) => parseSpec(s.specJson).storage === selectedStorage.value)
      const jPick = parseSpec((sameSt[0] || skus[0]).specJson)
      selectedColorKey.value = jPick.color || jPick.colorName || ''
      skuId.value =
        skus.find((s) => {
          const j = parseSpec(s.specJson)
          return (
            j.storage === selectedStorage.value &&
            (j.color === selectedColorKey.value || j.colorName === selectedColorKey.value)
          )
        })?.id ?? skus[0].id
    }
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
}

watch([() => selectedStorage.value, () => p.value?.skus], () => {
  if (!p.value?.skus?.length) {
    return
  }
  const cols = colorsForStorage.value
  if (!cols.find((c) => c.key === selectedColorKey.value)) {
    selectedColorKey.value = cols.length ? cols[0].key : ''
  }
})

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
    err.value = '请选择完整规格（存储与颜色）'
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

      <div v-if="p.skus?.length > 1" class="spec-block">
        <div class="spec-row" v-if="storages.length > 1">
          <span class="spec-label">存储容量</span>
          <div class="chips">
            <button
              v-for="st in storages"
              :key="st"
              type="button"
              class="chip"
              :class="{ on: selectedStorage === st }"
              @click="selectedStorage = st"
            >
              {{ st }}
            </button>
          </div>
        </div>
        <div class="spec-row" v-if="colorsForStorage.length > 1">
          <span class="spec-label">颜色</span>
          <div class="chips">
            <button
              v-for="c in colorsForStorage"
              :key="c.key"
              type="button"
              class="chip"
              :class="{ on: selectedColorKey === c.key }"
              @click="selectedColorKey = c.key"
            >
              {{ c.label }}
            </button>
          </div>
        </div>
        <div v-if="selectedSku" class="price-line">
          已选：<strong>{{ selectedSku.specSummary || '规格' }}</strong>
          <span class="price">¥{{ (selectedSku.priceCent / 100).toFixed(2) }}</span>
          <span class="stock">库存 {{ selectedSku.stock }} 件</span>
        </div>
      </div>
      <div v-else-if="p.skus?.length === 1" class="price-line">
        <span class="price">¥{{ (p.skus[0].priceCent / 100).toFixed(2) }}</span>
        <span class="stock">库存 {{ p.skus[0].stock }} 件</span>
      </div>

      <div class="detail">{{ p.detail }}</div>
      <div class="actions">
        <button class="primary" type="button" :disabled="busy || !skuId" @click="addToCart">加入购物车</button>
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
  margin-top: 14px;
}
.spec-block {
  margin-top: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #f8fafc;
}
.spec-row {
  margin-bottom: 12px;
}
.spec-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 8px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: #334155;
}
.chip.on {
  border-color: #e11d48;
  background: rgba(254, 226, 232, 0.5);
  color: #9f1239;
  font-weight: 700;
}
.price-line {
  margin-top: 8px;
  font-size: 15px;
  color: #334155;
}
.price-line .price {
  margin-left: 10px;
  font-size: 22px;
  font-weight: 900;
  color: #e11d48;
}
.stock {
  margin-left: 10px;
  font-size: 13px;
  color: #64748b;
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
