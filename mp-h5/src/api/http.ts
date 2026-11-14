import axios from 'axios'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const http = axios.create({ baseURL: '/api', timeout: 30000 })
http.interceptors.request.use((config) => {
  const store = sessionStore()
  if (store.token) config.headers.Authorization = `Bearer ${store.token}`
  return config
})
http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 0) {
        toast(payload.message || '请求失败', 'error')
        return Promise.reject(new Error(payload.message || '请求失败'))
      }
      return payload.data
    }
    return payload
  },
  (error) => {
    if (error?.response?.status === 401) sessionStore().logout()
    toast(error?.response?.data?.message || error.message || '网络异常', 'error')
    return Promise.reject(error)
  }
)
export default http
