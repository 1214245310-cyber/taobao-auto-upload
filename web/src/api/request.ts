import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 原系统后端约定：Authorization 携带 Sa-Token JWT，clientid 标识客户端。
const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'

/** 登录失效处理中的标记：并发请求同时 401 时只跳一次 */
let handlingAuthExpired = false

/** 登录失效：清本地 token，回到登录页并重新加载应用（顺带清掉 pinia 里的用户信息） */
function handleAuthExpired(msg?: string) {
  if (handlingAuthExpired) return
  handlingAuthExpired = true

  localStorage.removeItem('token')
  try {
    sessionStorage.setItem('loa_auth_expired', '1')
  } catch {
    // 隐私模式下 sessionStorage 可能不可用，忽略
  }

  const current = window.location.hash.replace(/^#/, '') || '/'
  if (current.startsWith('/auth/login')) {
    // 已经在登录页，只提示，不再跳转
    ElMessage.warning(msg || '登录已过期，请重新登录')
    handlingAuthExpired = false
    return
  }
  window.location.hash = `#/auth/login?redirect=${encodeURIComponent(current)}`
  window.location.reload()
}

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers.clientid = CLIENT_ID
  return config
})

service.interceptors.response.use(
  (resp) => {
    const data = resp.data
    // 原系统统一响应包装：{ code, msg, data }
    // 401（或旧版返回的「未登录」文案）：清 token 并回登录页
    if (data && (data.code === 401 || (typeof data.msg === 'string' && data.msg.includes('未登录')))) {
      handleAuthExpired(data.msg)
      return Promise.reject(data)
    }
    if (data.code !== 200) {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      handleAuthExpired()
      return Promise.reject(error)
    }
    ElMessage.error(error?.message || '网络错误')
    return Promise.reject(error)
  },
)

export default service
