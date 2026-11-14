import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authStore } from '@/stores/auth'

declare module 'axios' {
  export interface AxiosRequestConfig {
    silent?: boolean
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const store = authStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 0) {
        const message = payload.message || '请求失败'
        if (!(response.config as any).silent) ElMessage.error(message)
        return Promise.reject(new Error(message))
      }
      return payload.data
    }
    return payload
  },
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || error.message || '网络异常'
    if (status === 401) {
      authStore().logout(false)
      if (location.pathname !== '/login') {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
      }
    }
    if (!(error.config as any)?.silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export async function downloadFile(url: string, filename: string) {
  const store = authStore()
  const response = await axios.get(`/api${url}`, {
    responseType: 'blob',
    headers: { Authorization: `Bearer ${store.token}` }
  })
  const blob = new Blob([response.data])
  const href = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  a.click()
  URL.revokeObjectURL(href)
}

export default http
