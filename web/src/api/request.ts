import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 原系统后端约定：Authorization 携带 Sa-Token JWT，clientid 标识客户端。
const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'

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
    if (data.code !== 200) {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  (error) => {
    ElMessage.error(error?.message || '网络错误')
    return Promise.reject(error)
  },
)

export default service
