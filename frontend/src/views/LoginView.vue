<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const username = ref('buyer')
const password = ref('demo123')
const err = ref('')
const mode = ref('login')

async function submit() {
  err.value = ''
  try {
    if (mode.value === 'login') await auth.login(username.value, password.value)
    else await auth.register(username.value, password.value)
    await router.replace(route.query.redirect || '/')
  } catch (e) {
    err.value = e.response?.data?.error || '操作失败'
  }
}
</script>

<template>
  <div class="card" style="max-width: 400px">
    <h2>{{ mode === 'login' ? '登录' : '注册' }}</h2>
    <p class="hint">演示账号：商家 merchant / demo123；买家 buyer / demo123</p>
    <input v-model="username" placeholder="用户名" />
    <input v-model="password" type="password" placeholder="密码" />
    <p v-if="err" class="err">{{ err }}</p>
    <button class="primary" type="button" @click="submit">{{ mode === 'login' ? '登录' : '注册' }}</button>
    <button type="button" class="link" @click="mode = mode === 'login' ? 'register' : 'login'">
      {{ mode === 'login' ? '去注册' : '去登录' }}
    </button>
  </div>
</template>

<style scoped>
input {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  box-sizing: border-box;
}
.hint {
  font-size: 13px;
  color: #64748b;
}
.link {
  margin-left: 12px;
  background: none;
  border: none;
  color: #e11d48;
  cursor: pointer;
}
</style>
