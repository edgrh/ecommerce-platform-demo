<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'

const q = ref('')
const list = ref([])
const err = ref('')
const activeCategory = ref('ALL')

const categories = [
  { id: 'ALL', label: '全部' },
  { id: 'DIGITAL', label: '数码影音' },
  { id: 'PHONE_PC', label: '手机电脑' },
  { id: 'CLOTHES', label: '服饰配件' }
]

function classify(p) {
  const title = (p.title || '').toLowerCase()
  if (title.includes('iphone') || title.includes('phone') || title.includes('手机') || title.includes('笔记本')) {
    return 'PHONE_PC'
  }
  if (title.includes('airpods') || title.includes('earpods') || title.includes('耳机') || title.includes('耳机')) {
    return 'DIGITAL'
  }
  if (title.includes('t恤') || title.includes('外套') || title.includes('裤') || title.includes('鞋')) {
    return 'CLOTHES'
  }
  return 'DIGITAL'
}

function filteredList() {
  if (activeCategory.value === 'ALL') return list.value
  return list.value.filter((p) => classify(p) === activeCategory.value)
}

async function load() {
  err.value = ''
  try {
    const url = q.value.trim() ? '/c/products/search' : '/c/products'
    const { data } = await http.get(url, { params: q.value.trim() ? { q: q.value } : {} })
    list.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="hero">
      <div class="hero-main">
        <h1>商品</h1>
        <p class="hero-sub">支持关键词搜索与分类浏览。商品详情页可加入购物车。</p>
      </div>
      <div class="hero-actions">
        <div class="cat-row">
          <button
            v-for="c in categories"
            :key="c.id"
            type="button"
            class="chip"
            :class="{ active: activeCategory === c.id }"
            @click="activeCategory = c.id"
          >
            {{ c.label }}
          </button>
        </div>
        <div class="search">
          <input v-model="q" placeholder="搜索商品（例如 iPhone / AirPods）" @keyup.enter="load" />
          <button class="primary" type="button" @click="load">搜索</button>
        </div>
      </div>
    </div>

    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="!filteredList().length && !err" class="card empty">
      暂无商品数据，请稍后重试。
      <button class="primary" type="button" style="margin-left: 10px" @click="load">重新加载</button>
    </div>

    <div class="grid">
      <div v-for="p in filteredList()" :key="p.id" class="p-card">
        <div class="p-top">
          <span class="tag">{{ categories.find((x) => x.id === classify(p))?.label || '商品' }}</span>
          <span class="tag ghost">{{ p.status }}</span>
        </div>
        <div class="p-title">
          <RouterLink :to="'/products/' + p.id">{{ p.title }}</RouterLink>
        </div>
        <div class="p-sub">{{ p.subtitle }}</div>
        <div v-if="p.minPriceCent != null" class="p-price">¥{{ (p.minPriceCent / 100).toFixed(2) }}<span class="from"> 起</span></div>
        <div v-else class="p-price muted">价格见详情</div>
        <div class="p-actions">
          <RouterLink class="btn" :to="'/products/' + p.id">查看详情</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 14px 35px rgba(2, 6, 23, 0.12);
  margin-bottom: 14px;
}
.hero-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.hero-main h1 {
  margin: 0;
  font-size: 28px;
}
.hero-sub {
  color: #64748b;
  margin: 0;
}
.hero-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
.search {
  display: flex;
  gap: 8px;
  align-items: center;
}
.search input {
  min-width: 260px;
}
.search .primary {
  white-space: nowrap;
}

.cat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.45);
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.65);
  font-size: 12px;
  cursor: pointer;
  transition: transform 0.08s ease, border-color 0.12s ease;
}
.chip.active {
  background: #e11d48;
  border-color: #e11d48;
  color: #fff;
}
.chip:active {
  transform: scale(0.98);
}
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.p-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 16px;
  padding: 14px;
  box-shadow: 0 14px 28px rgba(2, 6, 23, 0.1);
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;
}
.p-card:hover {
  transform: translateY(-2px);
  border-color: rgba(251, 113, 133, 0.45);
  box-shadow: 0 18px 40px rgba(2, 6, 23, 0.14);
}
.p-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}
.tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(225, 29, 72, 0.1);
  border: 1px solid rgba(225, 29, 72, 0.22);
  color: #9f1239;
}
.tag.ghost {
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(148, 163, 184, 0.35);
  color: #475569;
}
.p-title {
  font-weight: 800;
  line-height: 1.25;
  margin-bottom: 6px;
}
.p-title :deep(a) {
  text-decoration: none;
}
.p-title :deep(a:hover) {
  color: #2563eb;
}
.p-sub {
  color: #64748b;
  font-size: 14px;
  line-height: 1.45;
  min-height: 40px;
}
.p-price {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 800;
  color: #e11d48;
}
.p-price.muted {
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
}
.p-price .from {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}
.p-actions {
  margin-top: 12px;
}
.btn {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.7);
  text-decoration: none;
}
.btn:hover {
  border-color: rgba(37, 99, 235, 0.35);
  color: #1d4ed8;
}
.empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .search input {
    min-width: 200px;
    flex: 1;
  }
}
</style>
