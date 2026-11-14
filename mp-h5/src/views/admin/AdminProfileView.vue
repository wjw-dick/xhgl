<template>
  <div class="page">
    <section class="hero profile-hero">
      <div class="profile-head">
        <div class="avatar">{{ (store.user?.name || 'A').slice(0, 1) }}</div>
        <div>
          <h1>{{ store.user?.name || '管理员' }}</h1>
          <p>管理员 · {{ store.companyName }}</p>
        </div>
      </div>
    </section>
    <div class="page-body">
      <div class="card">
        <div class="card-title"><h2>账号信息</h2><span class="pill green">ADMIN</span></div>
        <div class="list-row"><div class="main"><strong>登录账号</strong><span>{{ store.user?.username }}</span></div></div>
        <div class="list-row"><div class="main"><strong>企业</strong><span>{{ company?.name || store.companyName }}</span></div></div>
        <div class="list-row"><div class="main"><strong>角色</strong><span>管理员</span></div></div>
      </div>
      <div class="card">
        <div class="card-title"><h2>管理入口</h2></div>
        <router-link to="/admin/business" class="menu-row"><span>全部业务模块</span><em>›</em></router-link>
        <router-link to="/admin/customers" class="menu-row"><span>客户档案</span><em>›</em></router-link>
        <router-link to="/admin/reports" class="menu-row"><span>报表中心</span><em>›</em></router-link>
        <router-link to="/admin/settings" class="menu-row"><span>系统设置</span><em>›</em></router-link>
        <router-link to="/admin/audits" class="menu-row"><span>审计日志</span><em>›</em></router-link>
      </div>
      <div class="card">
        <div class="card-title"><h2>说明</h2></div>
        <p class="muted" style="font-size:11px;line-height:1.8;margin:0">管理员与 PC 后台共用账号与权限，移动端操作会实时写入同一业务数据库，并保留审计记录。</p>
      </div>
      <button class="btn btn-ghost btn-block" style="margin-top:14px" @click="logout">退出登录</button>
      <div class="footer-tip">鑫火锅炉 · 管理员移动工作台</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'

const store = sessionStore(); const router = useRouter(); const company = ref<any>(null)
function logout() { store.logout(); router.replace('/login') }
onMounted(async () => {
  try { const me: any = await http.get('/admin/auth/me'); company.value = me.company } catch {}
})
</script>

<style scoped>
.profile-hero { padding-bottom: 24px; }
.profile-head { display: flex; align-items: center; gap: 12px; }
.avatar { width: 52px; height: 52px; border-radius: 14px; background: rgba(255,255,255,.14); border: 1px solid rgba(255,255,255,.2); display: flex; align-items: center; justify-content: center; font-size: 20px; }
.profile-head h1 { font-size: 21px; margin: 0 0 3px; }
.profile-head p { color: #9fadc0; font-size: 11px; margin: 0; }
.menu-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f2f4f7; font-size: 13px; }
.menu-row:last-child { border-bottom: none; }
.menu-row em { font-style: normal; color: var(--steel-2); font-size: 11px; }
</style>
