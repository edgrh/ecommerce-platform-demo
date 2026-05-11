<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const step = ref('role')
const username = ref('buyer')
const password = ref('demo123')
const err = ref('')
const mode = ref('login')

watch(
  () => route.fullPath,
  () => {
    err.value = ''
  },
)

function formatLoginErr(e) {
  const raw =
    e.response?.data?.error ||
    (typeof e.message === 'string' ? e.message : '') ||
    ''
  if (raw === 'commerce_unavailable_or_open_circuit') {
    return '网关熔断或 commerce 未就绪：检查 Redis:6380、commerce:8081、gateway:8080，稍后重试或重启服务。'
  }
  return raw || '登录失败，请检查网络或在浏览器 Network 中查看 /api/auth/login'
}

function chooseBuyer() {
  step.value = 'account'
  username.value = 'buyer'
  password.value = 'demo123'
  err.value = ''
}

function chooseMerchant() {
  step.value = 'account'
  username.value = 'merchant'
  password.value = 'demo123'
  err.value = ''
}

function backToRole() {
  step.value = 'role'
  err.value = ''
}

async function submit() {
  err.value = ''
  try {
    if (mode.value === 'login') await auth.login(username.value, password.value)
    else await auth.register(username.value, password.value)
  } catch (e) {
    err.value = formatLoginErr(e)
    return
  }
  const redirectRaw = route.query.redirect
  const redirectPath = Array.isArray(redirectRaw) ? redirectRaw[0] : redirectRaw || '/'
  try {
    await router.replace(redirectPath)
  } catch (e) {
    console.warn('login redirect:', e)
    window.location.href = redirectPath
  }
}
</script>

<template>
  <div class="wrap">
    <div class="card panel">
      <template v-if="step === 'role'">
        <div class="head">
          <h1>选择身份</h1>
          <p class="subtle">选身份后登录；默认密码均为 demo123，用户名可改。</p>
        </div>
        <div class="role-grid">
          <button type="button" class="role-card" @click="chooseBuyer">
            <span class="role-title">我是买家</span>
            <span class="role-hint">浏览商品、购物车、下单与查看订单</span>
            <span class="role-cred">默认：<strong>buyer</strong> / <strong>demo123</strong></span>
          </button>
          <button type="button" class="role-card merchant" @click="chooseMerchant">
            <span class="role-title">我是商家</span>
            <span class="role-hint">快速上架、查看关联订单</span>
            <span class="role-cred">默认：<strong>merchant</strong> / <strong>demo123</strong></span>
          </button>
        </div>
      </template>

      <template v-else>
        <div class="head">
          <div>
            <h1>{{ mode === 'login' ? '登录' : '注册' }}</h1>
            <p class="subtle">可改用户名；默认密码 demo123。</p>
          </div>
        </div>

        <button type="button" class="back ghost" @click="backToRole">← 重新选择身份</button>

        <div class="quick">
          <button type="button" class="ghost" @click="((username = 'buyer'), (password = 'demo123'))">填入买家账号</button>
          <button type="button" class="ghost" @click="((username = 'merchant'), (password = 'demo123'))">
            填入商家账号
          </button>
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
      </template>
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
  width: min(560px, 100%);
  padding: 18px;
}
.head h1 {
  margin: 0;
  font-size: 26px;
}
.role-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}
.role-card {
  text-align: left;
  padding: 16px 14px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: #fff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.role-card:hover {
  border-color: rgba(225, 29, 72, 0.35);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}
.role-card.merchant:hover {
  border-color: rgba(37, 99, 235, 0.35);
}
.role-title {
  font-weight: 800;
  font-size: 17px;
  color: #0f172a;
}
.role-hint {
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}
.role-cred {
  font-size: 12px;
  color: #475569;
  margin-top: 4px;
}
.back {
  margin: 12px 0 6px;
  width: fit-content;
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
@media (max-width: 560px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
