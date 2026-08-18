import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    if (res.config.responseType === 'blob') {
      return res
    }
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '网络错误'
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!location.hash.includes('/login') && !location.pathname.includes('/login')) {
        location.href = '/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default http
