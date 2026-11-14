<template>
  <div class="register-page">
    <div class="register-left">
      <router-link to="/" class="back">← 返回门户首页</router-link>
      <div class="register-copy">
        <span class="kicker">CUSTOMER SIGN UP</span>
        <h1>注册用户<br />在线查看方案与报价</h1>
        <p>填写联系信息（企业/单位名称选填），注册成功后自动登录客户中心，可查看需求进度、方案、报价、合同与售后工单。</p>
        <div class="points">
          <div><strong>独立客户档案</strong><span>注册即建立本企业客户档案</span></div>
          <div><strong>在线确认</strong><span>方案与报价在线确认留痕</span></div>
          <div><strong>售后透明</strong><span>报修、维保与年检进度随时查看</span></div>
        </div>
      </div>
      <div class="register-foot">鑫火锅炉 · 用户注册 · 操作留审计</div>
    </div>
    <div class="register-right">
      <div class="register-card">
        <h2>注册用户</h2>
        <p class="sub">注册成功后自动登录，无需等待企业分配账号</p>
        <div class="field"><label>企业 / 单位名称（选填）</label><input v-model="form.companyName" maxlength="100" placeholder="选填，如：哈尔滨某某食品有限公司" /></div>
        <div class="form-grid">
          <div class="field"><label>所属行业</label><input v-model="form.industry" maxlength="64" placeholder="如：食品加工" /></div>
          <div class="field"><label>地区</label><input v-model="form.region" maxlength="64" placeholder="如：黑龙江哈尔滨" /></div>
          <div class="field"><label>联系人</label><input v-model="form.contactName" maxlength="32" placeholder="联系人姓名" /></div>
          <div class="field"><label>手机号</label><input v-model="form.phone" maxlength="11" placeholder="11 位手机号" /></div>
          <div class="field"><label>登录账号</label><input v-model="form.username" maxlength="32" placeholder="4-32 位字母/数字/下划线" /></div>
          <div class="field"><label>邮箱（选填）</label><input v-model="form.email" maxlength="120" placeholder="用于接收通知" /></div>
          <div class="field"><label>密码</label><input v-model="form.password" type="password" placeholder="至少 8 位，含字母和数字" /></div>
          <div class="field"><label>确认密码</label><input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" @keyup.enter="submit" /></div>
        </div>
        <div v-if="error" class="register-error">{{ error }}</div>
        <button class="btn btn-primary btn-block" style="margin-top:18px" :disabled="loading" @click="submit">{{ loading ? '注册中…' : '注册并进入客户中心' }}</button>
        <div class="login-link"><span>已有账号？</span><router-link to="/login">返回登录</router-link></div>
        <p class="agreement">注册即表示同意<router-link to="/agreement">《用户协议》与《隐私政策》</router-link>。敏感信息加密存储。</p>
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
    router.replace('/account')
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page { min-height: calc(100vh - 190px); display: grid; grid-template-columns: 1.05fr .95fr; }
.register-left { background: radial-gradient(circle at 20% 20%, rgba(217,99,59,.2), transparent 35%), linear-gradient(135deg, #0f172a, #1b2a41); color: #fff; padding: 54px 7vw; display: flex; flex-direction: column; }
.back { font-size: 11px; color: #96a6bb; }
.register-copy { margin: auto 0; max-width: 540px; }
.kicker { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
.register-copy h1 { font-size: 36px; line-height: 1.35; margin: 14px 0 16px; letter-spacing: -1px; }
.register-copy p { color: #a6b4c7; font-size: 13px; line-height: 1.9; }
.points { display: flex; flex-direction: column; gap: 12px; margin-top: 26px; }
.points div { display: flex; gap: 12px; align-items: baseline; border-left: 1px solid rgba(255,255,255,.15); padding-left: 14px; }
.points strong { font-size: 13px; color: #edf2f8; min-width: 110px; }
.points span { font-size: 11px; color: #8697ae; }
.register-foot { font-size: 10px; color: #697b92; letter-spacing: .4px; }
.register-right { display: flex; align-items: center; justify-content: center; background: var(--paper); padding: 34px; }
.register-card { width: 100%; max-width: 480px; background: #fff; border: 1px solid var(--line); padding: 30px 32px; box-shadow: var(--shadow); }
.register-card h2 { font-size: 24px; margin: 0 0 4px; }
.register-card .sub { font-size: 12px; color: var(--steel); margin: 0 0 20px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; margin-top: 12px; }
.register-error { color: #b33426; font-size: 12px; margin: 10px 0 0; }
.login-link { display: flex; justify-content: center; gap: 6px; margin-top: 16px; font-size: 12px; color: var(--steel); }
.login-link a { color: var(--accent); font-weight: 600; }
.agreement { font-size: 10px; color: var(--steel-2); line-height: 1.8; margin-top: 14px; }
.agreement a { color: var(--accent); }
@media (max-width: 960px) { .register-page { grid-template-columns: 1fr; } .register-left { display: none; } .register-right { padding: 30px 18px; } .register-card { padding: 26px 20px; } }
@media (max-width: 520px) { .form-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
