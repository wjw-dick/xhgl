<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">系统设置</h1><p class="page-sub">企业资料、客服、通知开关、SEO 与对象存储/微信入口；保存实时生效。</p></div>
      <el-button type="primary" :loading="saving" @click="saveConfigs">保存配置</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-tabs v-model="tab">
        <el-tab-pane label="企业资料" name="profile">
          <div style="padding:0 16px 16px">
            <el-form :model="profile" label-position="top" class="form-two" style="max-width:900px">
              <el-form-item label="企业名称"><el-input v-model="profile.name" /></el-form-item>
              <el-form-item label="所属行业"><el-input v-model="profile.industry" /></el-form-item>
              <el-form-item label="联系人"><el-input v-model="profile.contactName" /></el-form-item>
              <el-form-item label="客服电话"><el-input v-model="profile.contactPhone" /></el-form-item>
            </el-form>
            <el-button type="primary" @click="saveProfile">保存企业资料</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="基础配置" name="basic">
          <div style="padding:0 16px 16px">
            <el-form :model="configs" label-position="top" class="form-two">
              <el-form-item label="企业名称"><el-input v-model="configs.company_name" /></el-form-item>
              <el-form-item label="客服电话"><el-input v-model="configs.service_phone" /></el-form-item>
              <el-form-item label="公众号"><el-input v-model="configs.wechat_oa" /></el-form-item>
              <el-form-item label="小程序 AppID"><el-input v-model="configs.mp_appid" /></el-form-item>
              <el-form-item label="默认语言"><el-input v-model="configs.default_language" /></el-form-item>
              <el-form-item label="Logo URL"><el-input v-model="configs.logo" /></el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="通知与支付" name="notify">
          <div style="padding:0 16px 16px">
            <div class="switch-list">
              <div class="switch-row"><div><strong>微信支付</strong><span>在线收款/定金支付（需微信支付商户号）</span></div><el-switch v-model="configs.payment_enabled" active-value="true" inactive-value="false" /></div>
              <div class="switch-row"><div><strong>公众号模板消息</strong><span>需求确认/方案/报价/成交通知推送</span></div><el-switch v-model="configs.notify_wechat" active-value="true" inactive-value="false" /></div>
              <div class="switch-row"><div><strong>短信通道</strong><span>预留短信接口，开启后按配置发送</span></div><el-switch v-model="configs.notify_sms" active-value="true" inactive-value="false" /></div>
              <div class="switch-row"><div><strong>站内消息</strong><span>公众号未配置模板时的兜底通道</span></div><el-switch v-model="configs.notify_station" active-value="true" inactive-value="false" /></div>
            </div>
            <div class="notice" style="margin-top:16px">模板消息 ID 请在「微信配置 → 消息配置」中按业务节点逐项绑定；密钥与配置修改均写入审计日志。</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="门户 SEO" name="seo">
          <div style="padding:0 16px 16px">
            <el-form :model="configs" label-position="top">
              <el-form-item label="SEO 关键词（英文逗号分隔）"><el-input v-model="configs.seo_keywords" type="textarea" :rows="3" /></el-form-item>
              <el-form-item label="SEO 描述"><el-input v-model="configs.seo_description" type="textarea" :rows="4" /></el-form-item>
              <el-form-item label="ICP 备案号"><el-input v-model="configs.icp" /></el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="集成入口" name="integration">
          <div style="padding:0 16px 16px">
            <div class="integration-grid">
              <div class="integration-card" @click="router.push('/storage')"><el-icon><Cloudy /></el-icon><strong>对象存储</strong><span>本地 / S3 / OSS / COS / MinIO 驱动切换</span><el-button link type="primary">配置 →</el-button></div>
              <div class="integration-card" @click="router.push('/wechat')"><el-icon><ChatLineRound /></el-icon><strong>微信配置</strong><span>小程序/公众号/UnionID/模板消息/菜单/素材</span><el-button link type="primary">配置 →</el-button></div>
              <div class="integration-card" @click="router.push('/menus')"><el-icon><Menu /></el-icon><strong>菜单可视化</strong><span>后台导航 / 小程序 tabBar / H5 导航</span><el-button link type="primary">编辑 →</el-button></div>
              <div class="integration-card" @click="router.push('/i18n')"><el-icon><Switch /></el-icon><strong>多语言</strong><span>语言启停 / 内容双语录入</span><el-button link type="primary">管理 →</el-button></div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'

const router = useRouter(); const store = authStore()
const tab = ref('profile'); const saving = ref(false)
const configs = reactive<Record<string, any>>({
  company_name: '', service_phone: '', wechat_oa: '', mp_appid: '', default_language: 'zh-CN', logo: '',
  payment_enabled: 'false', notify_wechat: 'true', notify_sms: 'false', notify_station: 'true',
  seo_keywords: '', seo_description: '', icp: ''
})
const profile = reactive<any>({ name: '', industry: '', contactName: '', contactPhone: '' })
async function load() {
  const list: any[] = await http.get('/admin/settings/configs')
  list.forEach((c) => { if (c.configValue !== '********') configs[c.configKey] = c.configValue })
  Object.assign(profile, {
    name: store.company?.name || configs.company_name,
    industry: store.company?.industry || '',
    contactName: store.company?.contactName || '',
    contactPhone: store.company?.contactPhone || ''
  })
}
async function saveConfigs() {
  saving.value = true
  try {
    await http.put('/admin/settings/configs', configs)
    const me: any = await http.get('/admin/auth/me')
    if (me.company) store.company = me.company
    store.persist()
    ElMessage.success('系统配置已保存并实时生效')
  } finally { saving.value = false }
}
async function saveProfile() {
  if (!profile.name) return ElMessage.warning('企业名称必填')
  const company: any = await http.put('/admin/settings/profile', profile)
  if (store.company) Object.assign(store.company, company)
  else store.company = company
  store.persist()
  configs.company_name = company.name
  ElMessage.success('企业资料已保存')
}
onMounted(load)
</script>
<style scoped>
.switch-list{max-width:760px}
.switch-row{display:flex;justify-content:space-between;align-items:center;padding:14px 0;border-bottom:1px dashed var(--xh-line);gap:20px}
.switch-row strong{display:block;font-size:14px}
.switch-row span{display:block;color:var(--xh-muted);font-size:12px;margin-top:4px}
.integration-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:12px}
.integration-card{border:1px solid var(--xh-line);border-radius:8px;padding:18px;background:#fbfdff;cursor:pointer}
.integration-card .el-icon{font-size:26px;color:var(--xh-primary)}
.integration-card strong{display:block;margin-top:10px}
.integration-card span{display:block;color:var(--xh-muted);font-size:12px;margin:6px 0 8px}
</style>
