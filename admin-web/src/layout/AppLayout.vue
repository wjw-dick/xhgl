<template>
  <div class="layout" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="brand" @click="goHome">
        <div class="brand-mark">鑫</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>鑫火锅炉</strong>
          <span>业务管理平台</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <div class="menu-group-label" v-show="!collapsed">业务管理</div>
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#c9d4e7"
          active-text-color="#ffffff"
          router
          class="side-menu"
        >
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
            <el-icon><component :is="m.icon || 'Menu'" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-foot" v-show="!collapsed">
        <span>管理员模式</span>
        <span class="dot" />
        <span>审计开启</span>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="top-left">
          <button class="icon-btn" @click="collapsed = !collapsed" title="折叠菜单">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '工作台' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-right">
          <span class="company-chip">{{ store.company?.name || '鑫火锅炉' }}</span>
          <span class="role-chip">{{ store.roleName || '管理员' }}</span>
          <el-badge :value="unread" :hidden="unread === 0" class="notice-badge">
            <button class="icon-btn" @click="router.push('/notifications')" title="消息通知">
              <el-icon><Bell /></el-icon>
            </button>
          </el-badge>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <div class="avatar">{{ (store.user?.name || 'U').slice(0, 1) }}</div>
              <span class="user-name">{{ store.user?.name }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">当前账号：{{ store.user?.username || store.user?.name }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="tagsbar">
        <div class="tags">
          <router-link
            v-for="tag in tags"
            :key="tag.path"
            :to="tag.path"
            class="tag"
            :class="{ active: route.path === tag.path }"
          >
            {{ tag.title }}
            <span v-if="tag.path !== '/dashboard'" class="tag-close" @click.prevent="closeTag(tag.path)">×</span>
          </router-link>
        </div>
      </div>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authStore } from '@/stores/auth'
import http from '@/api/http'

const store = authStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const menus = ref<any[]>([])
const unread = ref(0)
const tags = ref<{ path: string; title: string }[]>([])

const activeMenu = computed(() => route.path)

async function loadMenus() {
  try {
    const data: any[] = await http.get('/admin/menus', { params: { target: 'ADMIN' } })
    menus.value = data.filter((m) => m.visible !== 0)
  } catch {
    menus.value = []
  }
}

async function loadUnread() {
  try {
    const data: any = await http.get('/admin/notifications/unread-count', { silent: true })
    unread.value = data?.count || 0
  } catch {
    unread.value = 0
  }
}

function addTag() {
  const title = (route.meta.title as string) || '页面'
  if (route.path === '/login' || route.path === '/') return
  if (!tags.value.find((t) => t.path === route.path)) {
    tags.value.push({ path: route.path, title })
  }
}

function closeTag(path: string) {
  tags.value = tags.value.filter((t) => t.path !== path)
  if (route.path === path) router.push('/dashboard')
}

function goHome() {
  router.push('/dashboard')
}

function onCommand(command: string) {
  if (command === 'logout') {
    store.logout()
    router.push('/login')
  }
}

onMounted(() => {
  addTag()
  loadMenus()
  loadUnread()
})
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; background: var(--xh-bg); }
.sidebar {
  width: 224px; flex: 0 0 224px; height: 100%; background: linear-gradient(180deg, #101b31 0%, #14213b 55%, #101a2d 100%);
  display: flex; flex-direction: column; transition: width .2s ease, flex-basis .2s ease; color: #c9d4e7;
}
.collapsed .sidebar { width: 64px; flex-basis: 64px; }
.brand { display: flex; align-items: center; gap: 10px; height: 64px; padding: 0 14px; cursor: pointer; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand-mark {
  width: 34px; height: 34px; border-radius: 8px; flex: 0 0 34px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f6feb, #58a6ff); color: #fff; font-weight: 800; font-size: 17px;
  box-shadow: 0 4px 12px rgba(31,111,235,.4);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.25; overflow: hidden; white-space: nowrap; }
.brand-text strong { color: #fff; font-size: 15px; letter-spacing: .5px; }
.brand-text span { font-size: 10px; color: #8296b5; letter-spacing: 1.4px; }
.menu-scroll { flex: 1; padding: 8px; }
.menu-group-label { padding: 12px 12px 6px; color: #6f85a6; font-size: 11px; letter-spacing: 1px; }
.side-menu { border-right: 0; }
.side-menu :deep(.el-menu-item) { height: 42px; border-radius: 6px; margin: 2px 0; color: #b9c6dc; }
.side-menu :deep(.el-menu-item:hover) { background: rgba(255,255,255,.06); color: #fff; }
.side-menu :deep(.el-menu-item.is-active) { background: linear-gradient(90deg, #1f6feb, #2f80ed); color: #fff; box-shadow: 0 4px 12px rgba(31,111,235,.25); }
.sidebar-foot { padding: 12px 16px; font-size: 10px; color: #6f85a6; display: flex; align-items: center; gap: 6px; border-top: 1px solid rgba(255,255,255,.06); }
.dot { width: 4px; height: 4px; border-radius: 50%; background: #16a34a; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; height: 100%; }
.topbar { height: 64px; flex: 0 0 64px; background: #fff; border-bottom: 1px solid var(--xh-line); display: flex; align-items: center; justify-content: space-between; padding: 0 18px; gap: 12px; }
.top-left, .top-right { display: flex; align-items: center; gap: 12px; }
.icon-btn { width: 32px; height: 32px; border: 1px solid var(--xh-line); background: #fff; border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; color: #5b6475; font-size: 16px; }
.icon-btn:hover { color: var(--xh-primary); border-color: #bcd3f7; background: #f5f9ff; }
.company-chip { font-size: 13px; color: #354052; background: #f5f8fc; border: 1px solid var(--xh-line); border-radius: 6px; padding: 5px 10px; max-width: 320px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.role-chip { font-size: 12px; color: #5b6475; }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: linear-gradient(135deg, #1f6feb, #8ab4f8); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 600; }
.user-name { font-size: 13px; color: #354052; max-width: 110px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.tagsbar { height: 38px; flex: 0 0 38px; background: #f7f9fc; border-bottom: 1px solid var(--xh-line); display: flex; align-items: center; padding: 0 18px; gap: 10px; }
.tags { display: flex; gap: 6px; overflow-x: auto; }
.tag { font-size: 12px; color: #5b6475; background: #fff; border: 1px solid var(--xh-line); border-radius: 4px; padding: 3px 9px; display: inline-flex; align-items: center; gap: 6px; }
.tag.active { color: var(--xh-primary); border-color: #bcd3f7; background: #f3f8ff; }
.tag-close { font-size: 13px; color: #9aa4b2; }
.tag-close:hover { color: var(--xh-danger); }
.content { flex: 1; overflow-y: auto; }
.page-fade-enter-active, .page-fade-leave-active { transition: opacity .16s ease, transform .16s ease; }
.page-fade-enter-from { opacity: 0; transform: translateY(4px); }
.page-fade-leave-to { opacity: 0; }
@media (max-width: 900px) {
  .sidebar { width: 64px; flex-basis: 64px; }
  .brand-text, .sidebar-foot, .menu-group-label { display: none; }
  .company-chip, .role-chip { display: none; }
}
</style>
