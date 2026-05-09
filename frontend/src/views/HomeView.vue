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
    <h1>商品</h1>
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
    <div class="row">
      <input v-model="q" placeholder="关键词" @keyup.enter="load" />
      <button class="primary" type="button" @click="load">搜索</button>
    </div>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-for="p in filteredList()" :key="p.id" class="card">
      <strong><RouterLink :to="'/products/' + p.id">{{ p.title }}</RouterLink></strong>
      <div class="muted">{{ p.subtitle }}</div>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.muted {
  color: #64748b;
  font-size: 14px;
  margin-top: 4px;
}
.cat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.chip {
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  padding: 4px 10px;
  background: #f8fafc;
  font-size: 12px;
  cursor: pointer;
}
.chip.active {
  background: #e11d48;
  border-color: #e11d48;
  color: #fff;
}
.card :deep(a) {
  color: #0f172a;
  text-decoration: underline;
}
.card :deep(a:hover) {
  color: #2563eb;
}
</style>
