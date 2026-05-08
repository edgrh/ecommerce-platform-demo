<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
auth.restore()
</script>

<template>
  <div class="layout">
    <header>
      <RouterLink class="logo" to="/">BCommerce 演示</RouterLink>
      <nav>
        <RouterLink to="/">商品</RouterLink>
        <RouterLink to="/seckill">秒杀</RouterLink>
        <RouterLink to="/orders">我的订单</RouterLink>
        <RouterLink v-if="auth.role === 'MERCHANT'" to="/merchant">商家后台</RouterLink>
        <template v-if="auth.token">
          <span class="meta">{{ auth.role }}</span>
          <button type="button" class="link" @click="auth.logout">退出</button>
        </template>
        <RouterLink v-else to="/login">登录</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<style>
body {
  margin: 0;
  font-family: system-ui, sans-serif;
  background: #f8fafc;
  color: #0f172a;
}
.layout {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px 32px;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #e2e8f0;
}
.logo {
  font-weight: 800;
  color: #e11d48;
  text-decoration: none;
}
nav {
  display: flex;
  gap: 16px;
  align-items: center;
}
nav a {
  color: #334155;
  text-decoration: none;
}
nav a.router-link-active {
  color: #e11d48;
}
.meta {
  font-size: 12px;
  color: #64748b;
}
.link {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
}
.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}
.err {
  color: #b91c1c;
}
button.primary {
  background: #e11d48;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
}
input {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
}
</style>
