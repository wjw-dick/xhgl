<template>
  <div class="login-page">
    <div class="login-left">
      <router-link to="/" class="back">← 返回门户首页</router-link>
      <div class="login-copy">
        <span class="kicker">CUSTOMER CENTER</span>
        <h1>登录客户中心<br />查看方案、报价与进度</h1>
        <p>需求、方案、报价、合同、售后工单与维保提醒与小程序/公众号同源。客户仅可查看本人/本企业数据。</p>
        <div class="login-points">
          <div><strong>报价在线确认</strong><span>确认后自动生成合同草稿</span></div>
          <div><strong>售后报修</strong><span>工单进度与年检提醒</span></div>
          <div><strong>资料归档</strong><span>合同、验收、检测报告统一查看</span></div>
        </div>
      </div>
      <div class="login-foot">鑫火锅炉 · 用户服务 · 操作留审计</div>
    </div>
    <div class="login-right">
      <div class="login-card">
        <h2>用户登录</h2>
        <p class="sub">注册用户账号，查看需求、方案、报价与售后进度</p>
        <div class="field"><label>账号</label><input v-model="form.username" placeholder="请输入账号" @keyup.enter="login" /></div>
        <div class="field" style="margin-top:14px"><label>密码</label><input v-model="form.password" type="password" placeholder="密码" @keyup.enter="login" /></div>
        <button class="btn btn-primary btn-block" style="margin-top:22px" :disabled="loading" @click="login">{{ loading ? '登录中…' : '登录' }}</button>
        <div class="register-link"><span>还没有账号？</span><router-link to="/register">立即注册</router-link></div>
        <div class="divider"><span>或</span></div>
        <button class="btn btn-ghost btn-block wechat-btn" @click="wechatLogin"><span class="wechat-dot">微</span> 公众号网页授权登录</button>
        <p class="agreement">登录即表示同意<router-link to="/agreement">《用户协议》与《隐私政策》</router-link>。敏感信息加密存储。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const store = sessionStore(); const route = useRoute(); const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
async function login() {
  if (!form.username || !form.password) return toast('请输入账号与密码', 'error')
  loading.value = true
  try {
    await store.login(form)
    toast('登录成功', 'success')
    router.replace((route.query.redirect as string) || '/account')
  } catch {} finally { loading.value = false }
}
async function wechatLogin() {
  try {
    const redirect = `${location.origin}/oauth`
    const data: any = await http.get('/public/oauth/wechat/url', { params: { redirect } })
    location.href = data.url
  } catch {
    toast('公众号网页授权未配置，请联系管理员或使用账号登录', 'error')
  }
}
</script>

<style scoped>
.login-page { min-height: calc(100vh - 190px); display: grid; grid-template-columns: 1.1fr .9fr; }
.login-left { background: radial-gradient(circle at 20% 20%, rgba(217,99,59,.2), transparent 35%), linear-gradient(135deg, #0f172a, #1b2a41); color: #fff; padding: 54px 7vw; display: flex; flex-direction: column; }
.back { font-size: 11px; color: #96a6bb; }
.login-copy { margin: auto 0; max-width: 540px; }
.kicker { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
.login-copy h1 { font-size: 38px; line-height: 1.35; margin: 14px 0 16px; letter-spacing: -1px; }
.login-copy p { color: #a6b4c7; font-size: 13px; line-height: 1.9; }
.login-points { display: flex; flex-direction: column; gap: 12px; margin-top: 26px; }
.login-points div { display: flex; gap: 12px; align-items: baseline; border-left: 1px solid rgba(255,255,255,.15); padding-left: 14px; }
.login-points strong { font-size: 13px; color: #edf2f8; min-width: 110px; }
.login-points span { font-size: 11px; color: #8697ae; }
.login-foot { font-size: 10px; color: #697b92; letter-spacing: .4px; }
.login-right { display: flex; align-items: center; justify-content: center; background: var(--paper); padding: 40px; }
.login-card { width: 100%; max-width: 390px; background: #fff; border: 1px solid var(--line); padding: 34px; box-shadow: var(--shadow); }
.login-card h2 { font-size: 24px; margin: 0 0 4px; }
.login-card .sub { font-size: 12px; color: var(--steel); margin: 0 0 22px; }
.divider { display: flex; align-items: center; gap: 12px; color: var(--steel-2); font-size: 10px; margin: 18px 0; }
.divider::before, .divider::after { content: ''; height: 1px; background: var(--line); flex: 1; }
.wechat-btn { border-color: #d5e2d9; color: #17663e; }
.wechat-dot { width: 20px; height: 20px; border-radius: 50%; background: #3b9e68; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 10px; }
.register-link { display: flex; justify-content: center; gap: 6px; margin-top: 16px; font-size: 12px; color: var(--steel); }
.register-link a { color: var(--accent); font-weight: 600; }
.agreement { font-size: 10px; color: var(--steel-2); line-height: 1.8; margin-top: 16px; }
.agreement a { color: var(--accent); }
@media (max-width: 900px) { .login-page { grid-template-columns: 1fr; } .login-left { display: none; } .login-right { padding: 40px 18px; } }
</style>
