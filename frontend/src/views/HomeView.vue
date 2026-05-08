<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const q = ref('')
const list = ref([])
const err = ref('')

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
    <div class="row">
      <input v-model="q" placeholder="关键词" @keyup.enter="load" />
      <button class="primary" type="button" @click="load">搜索</button>
    </div>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-for="p in list" :key="p.id" class="card">
      <strong>{{ p.title }}</strong>
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
</style>
