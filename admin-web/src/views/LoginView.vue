<template>
  <div class="login-page">
    <div class="login-visual">
      <div class="visual-inner">
        <div class="logo-line">
          <div class="brand-mark">鑫</div>
          <div>
            <div class="brand-name">鑫火锅炉</div>
            <div class="brand-sub">锅炉业务数字化管理平台</div>
          </div>
        </div>
        <h1>从采购需求到售后维保<br />一条链路跑通锅炉业务</h1>
        <p class="lead">
          客户档案、产品选型、方案报价、合同回款、安装验收、售后工单与年检定检，
          全过程线上化、可追溯，资质到期自动预警。
        </p>
        <div class="flow">
          <span>需求</span><i>→</i><span>咨询</span><i>→</i><span>方案</span><i>→</i>
          <span>谈价</span><i>→</i><span>成交履约</span><i>→</i><span>售后</span>
        </div>
        <div class="compliance">
          <div><strong>统一数据管理</strong><span>客户、设备与业务数据集中维护</span></div>
          <div><strong>特种设备合规</strong><span>制造许可 / 监检 / 能效台账</span></div>
          <div><strong>全链路审计</strong><span>关键操作可追溯</span></div>
        </div>
      </div>
    </div>
    <div class="login-panel">
      <div class="login-card">
        <div class="mobile-brand">
          <div class="brand-mark">鑫</div>
          <span>鑫火锅炉</span>
        </div>
        <h2>登录管理平台</h2>
        <p class="panel-sub">系统管理员账号登录</p>

        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" @keyup.enter="submit">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <div v-if="error" class="login-error">{{ error }}</div>
          <el-button type="primary" class="login-btn" :loading="loading" @click="submit">
            登录平台
          </el-button>
        </el-form>

        <p class="foot-note">登录即表示同意《用户协议》与《隐私政策》。敏感字段加密存储，全部操作留审计。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authStore } from '@/stores/auth'

const store = authStore()
const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!form.username || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await store.login({ username: form.username, password: form.password })
    const redirect = route.query.redirect as string
    if (redirect && redirect !== '/login') {
      router.replace(redirect)
    } else {
      router.replace('/dashboard')
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: 1.05fr .95fr; background: #f3f6fb; }
.login-visual {
  position: relative; overflow: hidden; color: #fff; padding: 64px 7vw;
  background: radial-gradient(circle at 15% 20%, rgba(88,166,255,.28), transparent 42%),
              radial-gradient(circle at 85% 80%, rgba(31,111,235,.3), transparent 46%),
              linear-gradient(135deg, #0b1425 0%, #13213a 55%, #0d192b 100%);
  display: flex; align-items: center;
}
.login-visual::after { content: ''; position: absolute; inset: 0; background-image: linear-gradient(rgba(255,255,255,.035) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.035) 1px, transparent 1px); background-size: 38px 38px; pointer-events: none; }
.visual-inner { max-width: 620px; position: relative; z-index: 1; }
.logo-line { display: flex; align-items: center; gap: 14px; margin-bottom: 56px; }
.brand-mark { width: 46px; height: 46px; border-radius: 10px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1f6feb, #58a6ff); font-size: 23px; font-weight: 800; box-shadow: 0 8px 26px rgba(31,111,235,.45); }
.brand-name { font-size: 20px; font-weight: 700; letter-spacing: 1px; }
.brand-sub { font-size: 11px; color: #8fa7c7; letter-spacing: 2px; margin-top: 2px; }
h1 { font-size: 38px; line-height: 1.35; margin: 0 0 20px; letter-spacing: -.5px; }
.lead { color: #a9bbd4; line-height: 1.9; font-size: 15px; margin: 0 0 36px; }
.flow { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 46px; }
.flow span { border: 1px solid rgba(126,170,232,.3); background: rgba(31,111,235,.12); color: #cfe2ff; border-radius: 999px; padding: 6px 14px; font-size: 13px; }
.flow i { font-style: normal; color: #5d7699; }
.compliance { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.compliance div { border: 1px solid rgba(255,255,255,.09); background: rgba(255,255,255,.04); border-radius: 8px; padding: 14px; }
.compliance strong { display: block; font-size: 13px; color: #eaf2ff; margin-bottom: 6px; }
.compliance span { font-size: 11px; color: #8fa7c7; line-height: 1.5; }
.login-panel { display: flex; align-items: center; justify-content: center; padding: 40px; }
.login-card { width: 100%; max-width: 400px; background: #fff; border-radius: 12px; padding: 36px; box-shadow: 0 20px 60px rgba(19,33,58,.12); border: 1px solid #e6ebf2; }
.mobile-brand { display: none; align-items: center; gap: 10px; font-weight: 700; margin-bottom: 20px; }
h2 { font-size: 24px; margin: 0 0 6px; color: #172033; }
.panel-sub { font-size: 13px; color: #697386; margin: 0 0 24px; }
.login-btn { width: 100%; height: 42px; font-weight: 600; margin-top: 4px; }
.login-error { color: #e5484d; font-size: 12px; margin: -4px 0 12px; }
.foot-note { font-size: 11px; color: #9aa4b2; line-height: 1.7; margin: 16px 0 0; }
@media (max-width: 960px) {
  .login-page { grid-template-columns: 1fr; }
  .login-visual { display: none; }
  .login-panel { padding: 24px; align-items: flex-start; padding-top: 70px; }
  .mobile-brand { display: flex; }
}
</style>
