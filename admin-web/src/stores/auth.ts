import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '@/api/http'

export interface AuthUser {
  id: number
  username?: string
  name: string
  userType: 'ADMIN' | 'CUSTOMER'
  roleCode?: string
  customerId?: number
  permissions?: string
}

export interface CompanyInfo {
  id: number
  name: string
  industry?: string
  contactName?: string
  contactPhone?: string
  maxUsers?: number
  maxCustomers?: number
}

function loadJson<T>(key: string): T | null {
  const raw = localStorage.getItem(key)
  if (!raw || raw === 'undefined' || raw === 'null') return null
  try { return JSON.parse(raw) as T } catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('xh_admin_token') || '')
  const user = ref<AuthUser | null>(loadJson<AuthUser>('xh_admin_user'))
  const company = ref<CompanyInfo | null>(loadJson<CompanyInfo>('xh_admin_company'))
  const roleName = ref<string>(localStorage.getItem('xh_admin_role_name') || '')

  const isAdmin = computed(() => user.value?.roleCode === 'ADMIN')
  const isCustomer = computed(() => user.value?.userType === 'CUSTOMER')

  function persist() {
    localStorage.setItem('xh_admin_token', token.value || '')
    localStorage.setItem('xh_admin_user', JSON.stringify(user.value ?? null))
    localStorage.setItem('xh_admin_company', JSON.stringify(company.value ?? null))
    localStorage.setItem('xh_admin_role_name', roleName.value || '')
  }

  function applySession(data: any) {
    token.value = data.token
    user.value = data.user
    company.value = data.company || null
    roleName.value = data.roleName || '管理员'
    persist()
  }

  async function login(payload: { username: string; password: string }) {
    const data: any = await http.post('/admin/auth/login', payload, { silent: true } as any)
    applySession(data)
    return data
  }

  function setSession(data: any) {
    applySession(data)
  }

  async function loadMe() {
    if (!token.value) return null
    const data: any = await http.get('/admin/auth/me')
    user.value = data.user
    if (data.company) company.value = data.company
    persist()
    return data
  }

  function logout(callApi = true) {
    if (callApi && token.value) http.post('/admin/auth/logout').catch(() => {})
    token.value = ''
    user.value = null
    company.value = null
    roleName.value = ''
    persist()
  }

  return {
    token, user, company, roleName, isAdmin, isCustomer,
    login, setSession, loadMe, logout, persist
  }
})

export function authStore() {
  return useAuthStore()
}
