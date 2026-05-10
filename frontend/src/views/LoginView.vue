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
  } catch (e) {
    err.value =
      e.response?.data?.error ||
      (typeof e.message === 'string' ? e.message : '') ||
      '登录/注册请求失败（请用 http:// 访问本站，并在开发者工具 Network 里查看 /api/auth/login）'
    return
  }
  const redirectRaw = route.query.redirect
  const redirectPath = Array.isArray(redirectRaw) ? redirectRaw[0] : redirectRaw || '/'
  try {
    await router.replace(redirectPath)
  } catch (e) {
    // 登录已成功；跳转异常不应掩盖成功（例如重复导航）
    console.warn('login redirect:', e)
    window.location.href = redirectPath
  }
}
</script>

<template>
  <div class="wrap">
    <div class="card panel">
      <div class="head">
        <div>
          <h1>{{ mode === 'login' ? '登录' : '注册' }}</h1>
          <p class="subtle">演示账号：商家 merchant / demo123；买家 buyer / demo123</p>
        </div>
      </div>

      <div class="quick">
        <button type="button" class="ghost" @click="(username = 'buyer'), (password = 'demo123')">填入买家</button>
        <button type="button" class="ghost" @click="(username = 'merchant'), (password = 'demo123')">填入商家</button>
      </div>

      <div class="field">
        <label>用户名</label>
        <input v-model="username" placeholder="buyer / merchant" autocomplete="username" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="password" type="password" placeholder="demo123" autocomplete="current-password" />
      </div>

      <p v-if="err" class="err">{{ err }}</p>

      <div class="actions">
        <button class="primary" type="button" @click="submit">{{ mode === 'login' ? '登录' : '注册' }}</button>
        <button type="button" class="ghost" @click="mode = mode === 'login' ? 'register' : 'login'">
          {{ mode === 'login' ? '去注册' : '去登录' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  min-height: calc(100vh - 160px);
  display: grid;
  place-items: center;
  padding: 10px 0 26px;
}
.panel {
  width: min(520px, 100%);
  padding: 18px;
}
.head h1 {
  margin: 0;
  font-size: 26px;
}
.quick {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 12px 0 14px;
}
.actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 10px;
}
</style>
