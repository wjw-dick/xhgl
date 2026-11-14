<template>
  <header class="portal-header">
    <div class="utility">
      <div class="container utility-inner">
        <span class="utility-item">工业锅炉 · 特种设备合规服务</span>
        <div class="utility-right">
          <span class="utility-item">服务热线 <a href="tel:4008006688">400-800-6688</a></span>
          <span class="utility-item">资质公开核验</span>
          <button class="lang-btn" @click="toggleLang">{{ lang === 'zh-CN' ? 'EN' : '中文' }}</button>
        </div>
      </div>
    </div>
    <div class="nav-wrap">
      <div class="container nav-inner">
        <router-link to="/" class="brand" aria-label="鑫火锅炉首页">
          <div class="brand-mark">
            <span class="boiler-body"><i /><i /><i /></span>
            <span class="flame" />
          </div>
          <div class="brand-text">
            <strong>鑫火锅炉</strong>
            <em>XINHUO BOILER</em>
          </div>
        </router-link>
        <nav class="desktop-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >{{ item.title }}</router-link>
        </nav>
        <div class="nav-actions">
          <router-link to="/consult" class="consult-link">{{ t('contact') }}</router-link>
          <router-link :to="accountTarget" class="account-btn">
            <span class="avatar">{{ (store.customer?.name || store.user?.name || '客').slice(0, 1) }}</span>
            <span>{{ store.isLoggedIn ? (store.customer?.name || '客户中心') : t('login') }}</span>
          </router-link>
          <button class="mobile-toggle" @click="mobileOpen = !mobileOpen" aria-label="菜单">
            <span /><span /><span />
          </button>
        </div>
      </div>
      <div v-if="mobileOpen" class="mobile-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="mobile-link"
          @click="mobileOpen = false"
        >{{ item.title }}</router-link>
        <router-link :to="accountTarget" class="mobile-link" @click="mobileOpen = false">{{ t('navAccount') }}</router-link>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { useI18n } from '@/i18n'

const route = useRoute()
const store = sessionStore()
const { t, lang } = useI18n()
const mobileOpen = ref(false)
const remoteMenus = ref<any[]>([])
const fallbackMenus = computed(() => [
  { path: '/', title: t('navHome') },
  { path: '/products', title: t('navProducts') },
  { path: '/demand', title: t('navDemand') },
  { path: '/verify', title: t('navVerify') },
  { path: '/announcements', title: t('navNews') },
  { path: '/account', title: t('navAccount') }
])
const navItems = computed(() => {
  if (!remoteMenus.value.length) return fallbackMenus.value
  return remoteMenus.value
    .filter((m) => m.visible !== 0)
    .filter((m) => !['/consult', '/share'].includes(m.path))
    .map((m) => ({ path: m.path || '/', title: m.title }))
})
const accountTarget = computed(() => store.isLoggedIn ? '/account' : '/login')
function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
function toggleLang() {
  lang.value = lang.value === 'zh-CN' ? 'en' : 'zh-CN'
}
onMounted(async () => {
  try {
    const menus: any[] = await http.get('/public/menus', { params: { target: 'H5' } })
    remoteMenus.value = menus
  } catch {
    remoteMenus.value = []
  }
})
</script>

<style scoped>
.portal-header { position: relative; z-index: 100; background: #fff; }
.utility { background: #0b1220; color: #9aa8bc; font-size: 11px; letter-spacing: .4px; }
.utility-inner { height: 34px; display: flex; align-items: center; justify-content: space-between; }
.utility-right { display: flex; align-items: center; gap: 22px; }
.utility-item a { color: #e7edf5; font-weight: 600; }
.lang-btn { border: 1px solid rgba(255,255,255,.22); color: #d8e0eb; background: transparent; padding: 2px 8px; border-radius: 3px; font-size: 10px; }
.lang-btn:hover { border-color: #fff; color: #fff; }
.nav-wrap { border-bottom: 1px solid var(--line); position: sticky; top: 0; background: rgba(255,255,255,.96); backdrop-filter: blur(12px); z-index: 100; }
.nav-inner { height: 76px; display: flex; align-items: center; justify-content: space-between; gap: 28px; }
.brand { display: flex; align-items: center; gap: 11px; flex: 0 0 auto; }
.brand-mark { width: 38px; height: 38px; border: 2px solid var(--ink); border-radius: 3px; position: relative; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 6px; background: var(--ink); }
.boiler-body { display: flex; gap: 3px; align-items: flex-end; }
.boiler-body i { display: block; width: 5px; background: #fff; border-radius: 1px 1px 0 0; }
.boiler-body i:nth-child(1) { height: 14px; }
.boiler-body i:nth-child(2) { height: 19px; }
.boiler-body i:nth-child(3) { height: 11px; }
.flame { position: absolute; top: -7px; right: -8px; width: 10px; height: 10px; background: var(--accent); clip-path: polygon(50% 0, 100% 65%, 50% 100%, 0 65%); }
.brand-text { display: flex; flex-direction: column; line-height: 1.15; }
.brand-text strong { font-size: 19px; letter-spacing: 2px; font-weight: 800; }
.brand-text em { font-style: normal; font-size: 8px; letter-spacing: 2.2px; color: var(--steel-2); margin-top: 4px; }
.desktop-nav { display: flex; align-items: center; gap: 4px; flex: 1; justify-content: center; }
.nav-link { padding: 10px 15px; font-size: 13px; color: #3a4352; border-radius: 3px; position: relative; white-space: nowrap; }
.nav-link:hover { color: var(--accent); }
.nav-link.active { color: var(--ink); font-weight: 700; }
.nav-link.active::after { content: ''; position: absolute; left: 15px; right: 15px; bottom: 2px; height: 2px; background: var(--accent); }
.nav-actions { display: flex; align-items: center; gap: 16px; flex: 0 0 auto; }
.consult-link { font-size: 12px; color: var(--steel); border-bottom: 1px solid #c9c3b7; padding-bottom: 2px; }
.account-btn { display: flex; align-items: center; gap: 7px; border: 1px solid var(--line); padding: 7px 12px; border-radius: 3px; font-size: 12px; background: var(--paper); max-width: 170px; }
.account-btn:hover { border-color: var(--ink); background: #fff; }
.avatar { width: 22px; height: 22px; border-radius: 50%; background: var(--accent); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 10px; flex: 0 0 22px; }
.account-btn span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mobile-toggle { display: none; border: none; background: transparent; flex-direction: column; gap: 4px; padding: 8px; }
.mobile-toggle span { display: block; width: 20px; height: 2px; background: var(--ink); }
.mobile-nav { display: none; }
@media (max-width: 1020px) {
  .desktop-nav, .consult-link { display: none; }
  .mobile-toggle { display: flex; }
  .mobile-nav { display: flex; flex-direction: column; border-top: 1px solid var(--line); background: #fff; padding: 10px 16px 16px; }
  .mobile-link { padding: 12px 8px; border-bottom: 1px solid #f0ede6; font-size: 14px; }
  .utility-item:first-child { display: none; }
}
@media (max-width: 640px) {
  .utility { display: none; }
  .nav-inner { height: 66px; gap: 10px; }
  .account-btn span:last-child { display: none; }
  .account-btn { padding: 7px 9px; }
  .brand-text strong { font-size: 17px; }
}
</style>
