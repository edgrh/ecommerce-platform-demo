<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const p = ref(null)
const err = ref('')

async function load() {
  err.value = ''
  p.value = null
  const id = route.params.id
  if (!id) {
    err.value = '无效商品'
    return
  }
  try {
    const { data } = await http.get(`/c/products/${id}`)
    p.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
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
    <div v-if="p" class="card">
      <h2>{{ p.title }}</h2>
      <p class="muted">{{ p.subtitle }}</p>
      <div class="detail">{{ p.detail }}</div>
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
}
</style>
