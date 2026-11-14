import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '@/api/http'

export const useSessionStore = defineStore('mp-session', () => {
  const companyName = ref(localStorage.getItem('xh_mp_company_name') || '鑫火锅炉')
  const token = ref(localStorage.getItem('xh_mp_token') || '')
  const user = ref<any>(JSON.parse(localStorage.getItem('xh_mp_user') || 'null'))
  const customer = ref<any>(JSON.parse(localStorage.getItem('xh_mp_customer') || 'null'))
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.userType === 'ADMIN' || user.value?.roleCode === 'ADMIN')
  function persist() {
    localStorage.setItem('xh_mp_company_name', companyName.value)
    localStorage.setItem('xh_mp_token', token.value || '')
    localStorage.setItem('xh_mp_user', JSON.stringify(user.value))
    localStorage.setItem('xh_mp_customer', JSON.stringify(customer.value))
  }
  async function login(payload: { username: string; password: string }) {
    const data: any = await http.post('/public/auth/user-login', payload)
    token.value = data.token
    user.value = data.user
    customer.value = data.customer || null
    companyName.value = data.company?.name || companyName.value
    persist()
    return data
  }
  function setLoginData(data: any) {
    token.value = data.token; user.value = data.user; customer.value = data.customer || null
    companyName.value = data.company?.name || companyName.value
    persist()
  }
  function updateCustomerFields(fields: Record<string, any>) {
    customer.value = { ...(customer.value || {}), ...fields }
    persist()
  }
  function logout() {
    token.value = ''; user.value = null; customer.value = null; persist()
  }
  return { companyName, token, user, customer, isLoggedIn, isAdmin, login, setLoginData, updateCustomerFields, logout }
})

export function sessionStore() { return useSessionStore() }
