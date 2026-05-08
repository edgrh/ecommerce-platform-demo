<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const orders = ref([])
const err = ref('')
const title = ref('新商品')
const sku = ref('SKU-DEMO-1')
const price = ref(19900)
const stock = ref(100)

onMounted(async () => {
  try {
    const { data } = await http.get('/b/orders')
    orders.value = data
  } catch (e) {
    err.value = e.response?.data?.error || '加载失败'
  }
})

async function publish() {
  err.value = ''
  try {
    const { data } = await http.post('/b/spus', {
      categoryId: 1,
      title: title.value,
      subtitle: '商家快速上架',
      detail: ''
    })
    await http.post(`/b/spus/${data.id}/skus`, {
      skuCode: sku.value,
      specJson: '{}',
      priceCent: price.value,
      stock: stock.value
    })
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
      <input v-model="title" />
      <input v-model="sku" />
      <input v-model.number="price" type="number" placeholder="价格(分)" />
      <input v-model.number="stock" type="number" placeholder="库存" />
      <button class="primary" type="button" @click="publish">提交</button>
    </div>
    <h2>关联订单</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-for="o in orders" :key="o.id" class="card">
      {{ o.orderNo }}，{{ (o.totalCent / 100).toFixed(2) }} 元
    </div>
  </div>
</template>

<style scoped>
input {
  display: block;
  width: 100%;
  margin-bottom: 8px;
  box-sizing: border-box;
}
</style>
