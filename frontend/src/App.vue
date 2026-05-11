<script setup>
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
auth.restore()
const route = useRoute()
const router = useRouter()

function switchAccount() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout" :class="{ 'layout--merchant': route.path === '/merchant' }">
    <header>
      <RouterLink class="logo" to="/">BCommerce 演示</RouterLink>
      <nav>
        <RouterLink to="/">商品</RouterLink>
        <RouterLink to="/seckill">秒杀</RouterLink>
        <RouterLink v-if="auth.token && auth.role === 'CUSTOMER'" to="/cart">购物车</RouterLink>
        <RouterLink to="/orders">我的订单</RouterLink>
        <RouterLink v-if="auth.role === 'MERCHANT'" to="/merchant">商家后台</RouterLink>
        <template v-if="auth.token">
          <span class="meta">{{ auth.role === 'CUSTOMER' ? '买家' : auth.role === 'MERCHANT' ? '商家' : auth.role }}</span>
          <button type="button" class="link" @click="switchAccount">切换账号</button>
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
:root {
  --page-bg: #eef1f5;
  --panel-solid: #ffffff;
  --text: #0f172a;
  --muted: #64748b;
  --border: rgba(15, 23, 42, 0.1);
  --shadow: 0 18px 45px rgba(2, 6, 23, 0.08);
  --shadow-sm: 0 8px 24px rgba(15, 23, 42, 0.06);
  --brand: #e11d48;
  --brand-2: #fb7185;
  --blue: #2563eb;
  --radius: 14px;
  --ok: #16a34a;
  --warn: #b45309;
}
body {
  margin: 0;
  font-family:
    ui-sans-serif,
    system-ui,
    -apple-system,
    Segoe UI,
    Roboto,
    Helvetica,
    Arial,
    "Apple Color Emoji",
    "Segoe UI Emoji";
  background: var(--page-bg);
  color: var(--text);
  min-height: 100vh;
}
* {
  box-sizing: border-box;
}
a {
  color: inherit;
}
::selection {
  background: rgba(251, 113, 133, 0.35);
}
.layout {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px 32px;
}
.layout.layout--merchant {
  max-width: 1180px;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  background: transparent;
}
.logo {
  font-weight: 800;
  color: var(--brand);
  text-decoration: none;
  letter-spacing: 0.2px;
}
.logo:hover {
  color: #be123c;
}
nav {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
nav a {
  color: #334155;
  text-decoration: none;
  padding: 7px 10px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: #fff;
}
nav a.router-link-active {
  color: #be123c;
  border-color: rgba(225, 29, 72, 0.35);
  background: rgba(254, 226, 232, 0.5);
}
.meta {
  font-size: 12px;
  color: var(--muted);
}
.link {
  background: none;
  border: none;
  color: var(--blue);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
  font-size: 13px;
}
.link:hover {
  color: #1d4ed8;
}
.card {
  background: var(--panel-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: var(--shadow-sm);
}
.err {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.65);
  border: 1px solid rgba(248, 113, 113, 0.45);
  padding: 10px 12px;
  border-radius: 12px;
}
button.primary {
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(225, 29, 72, 0.22);
}
input {
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(203, 213, 225, 0.9);
  background: #fff;
}
main {
  padding-top: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.page-head h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 0.2px;
}
.subtle {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.7);
  padding: 8px 12px;
}
.muted {
  color: var(--muted);
}
.ok {
  color: var(--ok);
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.18);
  padding: 10px 12px;
  border-radius: 12px;
}
.warn {
  color: var(--warn);
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.22);
  padding: 10px 12px;
  border-radius: 12px;
}
.ghost {
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: #fff;
  color: #334155;
  border-radius: 12px;
  padding: 8px 12px;
  cursor: pointer;
}
.ghost:hover {
  border-color: rgba(37, 99, 235, 0.25);
  color: #1d4ed8;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
}
.field > label {
  font-size: 13px;
  color: #475569;
}
.empty {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 560px) {
  nav a {
    padding: 6px 9px;
  }
  .layout {
    padding: 0 12px 24px;
  }
}
</style>
