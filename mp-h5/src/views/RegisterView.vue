<template>
  <div class="page">
    <div class="topbar">
      <button class="back-btn" @click="router.back()">‹</button>
      <h1>注册用户</h1>
    </div>
    <div class="page-body">
      <div class="notice blue">注册成功后自动登录，可查看需求、方案、报价、合同与售后进度。</div>
      <div class="card">
        <div class="field"><label>企业 / 单位名称（选填）</label><input v-model="form.companyName" maxlength="100" placeholder="选填，如：哈尔滨某某食品有限公司" /></div>
        <div class="form-grid">
          <div class="field"><label>所属行业</label><input v-model="form.industry" maxlength="64" placeholder="如：食品加工" /></div>
          <div class="field"><label>地区</label><input v-model="form.region" maxlength="64" placeholder="如：黑龙江哈尔滨" /></div>
          <div class="field"><label>联系人</label><input v-model="form.contactName" maxlength="32" placeholder="联系人姓名" /></div>
          <div class="field"><label>手机号</label><input v-model="form.phone" maxlength="11" placeholder="11 位手机号" /></div>
          <div class="field"><label>登录账号</label><input v-model="form.username" maxlength="32" placeholder="字母/数字/下划线" /></div>
          <div class="field"><label>邮箱（选填）</label><input v-model="form.email" maxlength="120" placeholder="用于接收通知" /></div>
          <div class="field"><label>密码</label><input v-model="form.password" type="password" placeholder="至少 8 位，含字母和数字" /></div>
          <div class="field"><label>确认密码</label><input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" @keyup.enter="submit" /></div>
        </div>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="btn btn-primary btn-block" style="margin-top:10px" :disabled="loading" @click="submit">{{ loading ? '注册中…' : '注册并登录' }}</button>
        <button class="login-link" @click="router.replace('/login')">已有账号，返回登录</button>
        <p class="agreement">注册即表示同意<router-link to="/agreement">《用户协议》与《隐私政策》</router-link>。敏感字段加密存储。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const store = sessionStore(); const router = useRouter()
const loading = ref(false); const error = ref('')
const form = reactive({
  companyName: '', industry: '', region: '', contactName: '', phone: '', email: '', username: '', password: '', confirmPassword: ''
})
function validate() {
  if (!form.contactName.trim()) return '请输入联系人姓名'
  if (!/^1[3-9][0-9]{9}$/.test(form.phone)) return '请输入正确的 11 位手机号'
  if (!/^[A-Za-z0-9_]{4,32}$/.test(form.username)) return '登录账号须为 4-32 位字母、数字或下划线'
  if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/[0-9]/.test(form.password)) {
    return '密码至少 8 位，且同时包含字母和数字'
  }
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致'
  return ''
}
async function submit() {
  const message = validate()
  if (message) { error.value = message; return }
  loading.value = true
  error.value = ''
  try {
    const data: any = await http.post('/public/auth/register', {
      companyName: form.companyName.trim(),
      industry: form.industry.trim(),
      region: form.region.trim(),
      contactName: form.contactName.trim(),
      phone: form.phone,
      email: form.email.trim(),
      username: form.username.trim(),
      password: form.password
    })
    store.setLoginData(data)
    toast('注册成功，已自动登录', 'success')
    router.replace('/home')
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.topbar { display: flex; align-items: center; gap: 10px; }
.topbar h1 { margin: 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-grid .field { min-width: 0; }
.field select { width: 100%; border: 1px solid var(--line); border-radius: 8px; padding: 10px; font-size: 13px; background: #fff; color: var(--ink); }
.error-text { color: #b33426; font-size: 11px; margin: 2px 0 6px; }
.login-link { display: block; width: 100%; border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 12px 0 2px; }
.agreement { font-size: 9px; color: var(--steel-2); line-height: 1.7; text-align: center; margin: 6px 0 0; }
.agreement a { color: var(--accent); }
</style>
