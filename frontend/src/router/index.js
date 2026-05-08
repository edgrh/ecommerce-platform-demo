import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('../views/HomeView.vue') },
    { path: '/seckill', component: () => import('../views/SeckillView.vue') },
    { path: '/orders', component: () => import('../views/OrdersView.vue'), meta: { auth: true } },
    { path: '/login', component: () => import('../views/LoginView.vue') },
    {
      path: '/merchant',
      component: () => import('../views/MerchantView.vue'),
      meta: { auth: true, role: 'MERCHANT' }
    }
  ]
})

router.beforeEach((to) => {
  const a = useAuthStore()
  if (to.meta.auth && !a.token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.role && a.role !== to.meta.role) return { path: '/' }
  return true
})

export default router
