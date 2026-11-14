import axios from 'axios'
import { sessionStore } from '@/stores/session'

export async function downloadFile(url: string, filename: string) {
  const store = sessionStore()
  const response = await axios.get(`/api${url}`, {
    responseType: 'blob',
    headers: store.token ? { Authorization: `Bearer ${store.token}` } : {}
  })
  const href = URL.createObjectURL(new Blob([response.data]))
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  a.click()
  URL.revokeObjectURL(href)
}
