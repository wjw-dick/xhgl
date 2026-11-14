<template>
  <div class="login-page">
    <header class="login-head">
      <div class="brand-row">
        <div class="brand"><span class="mark">鑫</span><div><strong>鑫火锅炉</strong><em>XINHUO BOILER</em></div></div>
        <span class="edition">WORKS · 01</span>
      </div>
      <div class="headline">
        <span class="eyebrow">INDUSTRIAL HEAT SOLUTIONS</span>
        <h1>把每一台锅炉的<br />业务，装进口袋。</h1>
        <p>客户与管理员使用同一账号入口，登录后自动进入对应工作台。</p>
      </div>
      <div class="head-rule"><span>需求</span><span>方案</span><span>报价</span><span>履约</span><span>售后</span></div>
    </header>

    <div class="login-card">
      <div class="login-title">
        <div><strong>用户登录</strong><span>请输入账号与密码</span></div>
        <span class="login-index">01</span>
      </div>
      <div class="field"><label>账号</label><input v-model="form.username" placeholder="请输入账号" autocomplete="username" /></div>
      <div class="field"><label>密码</label><input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" @keyup.enter="login" /></div>
      <button class="btn btn-primary btn-block login-btn" :disabled="loading" @click="login">{{ loading ? '登录中…' : '登录' }}</button>
      <button class="register-btn" @click="goRegister">首次使用？创建客户账号</button>
      <div class="login-tip">登录即同意<router-link to="/agreement">《用户协议》与《隐私政策》</router-link><br />敏感字段加密存储，关键操作全程留痕。</div>
    </div>
    <div class="login-foot">鑫火锅炉（黑龙江）有限公司 · 数字化业务平台</div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const store = sessionStore(); const route = useRoute(); const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
function goRegister() { router.push('/register') }
async function login() {
  if (!form.username || !form.password) return toast('请输入账号与密码', 'error')
  loading.value = true
  try {
    const data = await store.login({ ...form })
    const admin = data?.user?.userType === 'ADMIN' || data?.user?.roleCode === 'ADMIN'
    toast('登录成功', 'success')
    const redirect = route.query.redirect as string
    if (admin) router.replace(redirect && redirect.startsWith('/admin') ? redirect : '/admin/dashboard')
    else router.replace(redirect && !redirect.startsWith('/admin') ? redirect : '/home')
  } catch {} finally { loading.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; background: var(--paper); padding-bottom: 24px; }
.login-head { padding: 26px 22px 22px; }
.brand-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.brand { display: flex; align-items: center; gap: 9px; }
.mark { width: 34px; height: 34px; background: var(--ink); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 15px; border-radius: 8px; }
.brand strong { display: block; font-size: 14px; letter-spacing: .6px; }
.brand em { display: block; font-style: normal; font-size: 8px; color: var(--steel-2); letter-spacing: 1.6px; margin-top: 1px; }
.edition { font-size: 8px; color: var(--steel-2); letter-spacing: 1.6px; padding-top: 5px; }
.headline { padding: 38px 0 24px; border-bottom: 1px solid var(--line); }
.eyebrow { font-size: 9px; color: var(--accent); letter-spacing: 2.3px; font-weight: 650; }
.headline h1 { font-size: 31px; line-height: 1.3; letter-spacing: -1.4px; font-weight: 650; margin: 13px 0 12px; }
.headline p { font-size: 11px; color: var(--steel); line-height: 1.8; margin: 0; max-width: 31ch; }
.head-rule { display: flex; justify-content: space-between; gap: 6px; margin-top: 22px; font-size: 9px; color: var(--steel-2); letter-spacing: .8px; }
.head-rule span { flex: 1; border-top: 1px solid var(--line-strong); padding-top: 7px; }
.login-card { margin: 18px 18px 0; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; padding: 20px 18px; box-shadow: var(--shadow); }
.login-title { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.login-title strong { display: block; font-size: 18px; font-weight: 650; letter-spacing: -.5px; }
.login-title span { display: block; color: var(--steel-2); font-size: 9px; margin-top: 2px; letter-spacing: .3px; }
.login-index { font-size: 10px; color: var(--steel-2); letter-spacing: 1.2px; padding-top: 3px; }
.field { margin-bottom: 13px; }
.login-btn { margin-top: 8px; height: 44px; }
.register-btn { width: 100%; border: none; background: transparent; color: var(--accent); font-size: 10px; padding: 14px 0 4px; letter-spacing: .2px; }
.login-tip { color: var(--steel-2); font-size: 8px; line-height: 1.8; margin-top: 12px; text-align: center; }
.login-tip a { color: var(--steel); }
.login-foot { text-align: center; color: var(--steel-2); font-size: 8px; letter-spacing: .4px; padding: 26px 20px 0; }
</style>
