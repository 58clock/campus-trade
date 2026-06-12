import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器 — 自动带 JWT
api.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器 — 统一错误处理
api.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    }
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

// ==================== Auth API ====================
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  sendResetCode: (email) => api.post('/auth/forgot-password/send-code', null, { params: { email } }),
  resetPassword: (data) => api.post('/auth/forgot-password/reset', data),
}

// ==================== User API ====================
export const userApi = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  uploadAvatar: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/user/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  getUserById: (id) => api.get(`/user/${id}`),
}

// ==================== Product API ====================
export const productApi = {
  list: (params) => api.get('/products', { params }),
  getById: (id) => api.get(`/products/${id}`),
  create: (fd) => api.post('/products', fd, { headers: { 'Content-Type': 'multipart/form-data' } }),
  update: (id, data) => api.put(`/products/${id}`, data),
  offShelf: (id) => api.put(`/products/${id}/off-shelf`),
  delete: (id) => api.delete(`/products/${id}`),
  myProducts: (params) => api.get('/products/my', { params }),
}

// ==================== Order API ====================
export const orderApi = {
  create: (productId) => api.post('/orders', null, { params: { productId } }),
  getById: (id) => api.get(`/orders/${id}`),
  pay: (id) => api.put(`/orders/${id}/pay`),
  ship: (id) => api.put(`/orders/${id}/ship`),
  receive: (id) => api.put(`/orders/${id}/receive`),
  cancel: (id, reason) => api.put(`/orders/${id}/cancel`, null, { params: { reason } }),
  bought: (params) => api.get('/orders/bought', { params }),
  sold: (params) => api.get('/orders/sold', { params }),
}

// ==================== Message API ====================
export const messageApi = {
  getProductMessages: (productId) => api.get(`/products/${productId}/messages`),
  sendMessage: (productId, content) => api.post(`/products/${productId}/messages`, null, { params: { content } }),
  reply: (id, content) => api.post(`/messages/${id}/reply`, null, { params: { content } }),
  myMessages: () => api.get('/messages'),
}

// ==================== Review API ====================
export const reviewApi = {
  create: (data) => api.post('/reviews', data),
  getUserReviews: (userId, params) => api.get(`/reviews/user/${userId}`, { params }),
  getUserReputation: (userId) => api.get(`/reviews/user/${userId}/reputation`),
}

// ==================== Admin API ====================
export const adminApi = {
  // users
  listUsers: (params) => api.get('/admin/users', { params }),
  banUser: (id) => api.put(`/admin/users/${id}/ban`),
  unbanUser: (id) => api.put(`/admin/users/${id}/unban`),
  resetPassword: (id) => api.put(`/admin/users/${id}/reset-password`),
  // products
  listProducts: (params) => api.get('/admin/products', { params }),
  forceOffShelf: (id) => api.put(`/admin/products/${id}/off-shelf`),
  deleteProduct: (id) => api.delete(`/admin/products/${id}`),
  // reports
  listReports: (params) => api.get('/admin/reports', { params }),
  handleReport: (id, action, note) => api.put(`/admin/reports/${id}/handle`, null, { params: { action, note } }),
  // statistics
  getStatistics: () => api.get('/admin/statistics'),
}

// ==================== Report API ====================
export const reportApi = {
  submit: (targetType, targetId, reason) => api.post('/reports', null, { params: { targetType, targetId, reason } }),
}

// ==================== Skill API ====================
export const skillApi = {
  recommend: (limit) => api.post('/skills/recommend', null, { params: { limit } }),
  suggestPrice: (category, conditionLevel) => api.post('/skills/pricing', null, { params: { category, conditionLevel } }),
}

export default api
