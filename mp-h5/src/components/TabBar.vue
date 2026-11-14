<template>
  <nav class="tabbar">
    <router-link v-for="(tab, i) in tabs" :key="i" :to="tab.path" :class="{ active: isActive(tab.path) }">
      <span class="tab-icon"><AppIcon :name="tab.icon" :size="19" /></span>
      <span>{{ tab.title }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute(); const store = sessionStore()
const remoteTitles = ref<string[]>([])
const adminTabs = [
  { path: '/admin/dashboard', icon: 'grid', title: '工作台' },
  { path: '/admin/customers', icon: 'users', title: '客户' },
  { path: '/admin/business', icon: 'layers', title: '业务' },
  { path: '/admin/profile', icon: 'user', title: '我的' }
]
const customerTabs = computed(() => [
  { path: '/home', icon: 'home', title: remoteTitles.value[0] || '首页' },
  { path: '/demands', icon: 'doc', title: remoteTitles.value[1] || '需求' },
  { path: '/service', icon: 'tool', title: remoteTitles.value[2] || '售后' },
  { path: '/profile', icon: 'user', title: remoteTitles.value[3] || '我的' }
])
const tabs = computed(() => store.isAdmin ? adminTabs : customerTabs.value)
function isActive(path: string) { return route.path === path || (path !== '/admin/dashboard' && route.path.startsWith(path + '/')) }
onMounted(async () => {
  if (store.isAdmin) return
  try {
    const menus: any[] = await http.get('/public/menus', { params: { target: 'MP' } })
    remoteTitles.value = menus.map((m) => m.title)
  } catch {}
})
</script>
