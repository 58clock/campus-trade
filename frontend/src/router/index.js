import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/Home.vue') },
  { path: '/product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetail.vue') },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { guest: true } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/ForgotPassword.vue'), meta: { guest: true } },
  { path: '/publish', name: 'Publish', component: () => import('@/views/Publish.vue'), meta: { auth: true } },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { auth: true } },
  { path: '/my-products', name: 'MyProducts', component: () => import('@/views/MyProducts.vue'), meta: { auth: true } },
  { path: '/my-orders', name: 'MyOrders', component: () => import('@/views/MyOrders.vue'), meta: { auth: true } },
  { path: '/messages', name: 'Messages', component: () => import('@/views/Messages.vue'), meta: { auth: true } },
  { path: '/my-reports', name: 'MyReports', component: () => import('@/views/MyReports.vue'), meta: { auth: true } },
  { path: '/skills/recommend', name: 'SkillRecommend', component: () => import('@/views/SkillRecommend.vue'), meta: { auth: true } },
  { path: '/skills/pricing', name: 'SkillPricing', component: () => import('@/views/SkillPricing.vue'), meta: { auth: true } },

  // 后台管理（嵌套路由 + 侧边栏布局）
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrderManage.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/ProductAudit.vue') },
      { path: 'reports', name: 'AdminReports', component: () => import('@/views/admin/ReportManage.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.auth && !userStore.isLoggedIn) {
    return next('/login')
  }
  if (to.meta.admin && !userStore.isAdmin) {
    return next('/')
  }
  if (to.meta.guest && userStore.isLoggedIn) {
    return next('/')
  }
  next()
})

export default router
