import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '@/api/http'

export const useSessionStore = defineStore('session', () => {
  const companyName = ref(localStorage.getItem('xh_portal_company_name') || '鑫火锅炉')
  const token = ref(localStorage.getItem('xh_portal_token') || '')
  const user = ref<any>(JSON.parse(localStorage.getItem('xh_portal_user') || 'null'))
  const customer = ref<any>(JSON.parse(localStorage.getItem('xh_portal_customer') || 'null'))
  const lang = ref(localStorage.getItem('xh_portal_lang') || 'zh-CN')
  const isLoggedIn = computed(() => !!token.value)

  function persist() {
    localStorage.setItem('xh_portal_company_name', companyName.value)
    localStorage.setItem('xh_portal_token', token.value || '')
    localStorage.setItem('xh_portal_user', JSON.stringify(user.value))
    localStorage.setItem('xh_portal_customer', JSON.stringify(customer.value))
    localStorage.setItem('xh_portal_lang', lang.value)
  }
  function setCompany(name?: string) {
    if (name) companyName.value = name
    persist()
  }
  async function login(payload: { username: string; password: string }) {
    const data: any = await http.post('/public/auth/login', { username: payload.username, password: payload.password })
    token.value = data.token
    user.value = data.user
    customer.value = data.customer
    companyName.value = data.company?.name || companyName.value
    persist()
    return data
  }
  function setLoginData(data: any) {
    token.value = data.token
    user.value = data.user
    customer.value = data.customer
    companyName.value = data.company?.name || companyName.value
    persist()
  }
  function updateCustomerFields(fields: Record<string, any>) {
    customer.value = { ...(customer.value || {}), ...fields }
    persist()
  }
  function logout() {
    token.value = ''
    user.value = null
    customer.value = null
    persist()
  }
  function setLang(value: string) {
    lang.value = value
    persist()
  }
  return { companyName, token, user, customer, lang, isLoggedIn, setCompany, login, setLoginData, updateCustomerFields, logout, setLang, persist }
})

export function sessionStore() {
  return useSessionStore()
}
