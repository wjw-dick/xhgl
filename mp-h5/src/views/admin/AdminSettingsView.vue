<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>系统设置</h1><button class="link-btn" :disabled="saving" @click="saveConfigs">{{ saving ? '保存中…' : '保存配置' }}</button></div>
    <div class="page-body">
      <div class="tabs">
        <button v-for="t in tabs" :key="t.value" :class="{ active: tab === t.value }" @click="tab = t.value">{{ t.label }}</button>
      </div>

      <div v-if="tab === 'profile'" class="card">
        <div class="form-grid">
          <div class="field full"><label>企业名称</label><input v-model="profile.name" /></div>
          <div class="field"><label>所属行业</label><input v-model="profile.industry" /></div>
          <div class="field"><label>联系人</label><input v-model="profile.contactName" /></div>
          <div class="field full"><label>客服电话</label><input v-model="profile.contactPhone" /></div>
        </div>
        <button class="btn btn-primary btn-sm" style="margin-top:12px" @click="saveProfile">保存企业资料</button>
      </div>

      <div v-else-if="tab === 'basic'" class="card">
        <div class="form-grid">
          <div class="field full"><label>企业名称</label><input v-model="configs.company_name" /></div>
          <div class="field"><label>客服电话</label><input v-model="configs.service_phone" /></div>
          <div class="field"><label>公众号</label><input v-model="configs.wechat_oa" /></div>
          <div class="field"><label>小程序 AppID</label><input v-model="configs.mp_appid" /></div>
          <div class="field"><label>默认语言</label><input v-model="configs.default_language" /></div>
          <div class="field full"><label>Logo URL</label><input v-model="configs.logo" /></div>
        </div>
      </div>

      <div v-else-if="tab === 'notify'" class="card">
        <div v-for="c in switches" :key="c.key" class="list-row">
          <div class="main"><strong>{{ c.label }}</strong><span>{{ c.desc }}</span></div>
          <select v-model="configs[c.key]" class="switch-select" @change="saveConfigs">
            <option value="true">开启</option><option value="false">关闭</option>
          </select>
        </div>
      </div>

      <div v-else-if="tab === 'seo'" class="card">
        <div class="field"><label>SEO 关键词</label><textarea v-model="configs.seo_keywords" rows="3" /></div>
        <div class="field" style="margin-top:10px"><label>SEO 描述</label><textarea v-model="configs.seo_description" rows="4" /></div>
        <div class="field" style="margin-top:10px"><label>ICP 备案号</label><input v-model="configs.icp" /></div>
      </div>

      <div v-else class="card">
        <router-link to="/admin/storage" class="menu-row"><span>对象存储</span><em>配置 ›</em></router-link>
        <router-link to="/admin/wechat" class="menu-row"><span>微信配置</span><em>配置 ›</em></router-link>
        <router-link to="/admin/menus" class="menu-row"><span>菜单可视化</span><em>编辑 ›</em></router-link>
        <router-link to="/admin/i18n" class="menu-row"><span>多语言</span><em>管理 ›</em></router-link>
        <router-link to="/admin/dicts" class="menu-row"><span>数据字典</span><em>管理 ›</em></router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const router = useRouter(); const store = sessionStore()
const tab = ref('profile'); const saving = ref(false)
const tabs = [{ value: 'profile', label: '企业资料' }, { value: 'basic', label: '基础配置' }, { value: 'notify', label: '通知' }, { value: 'seo', label: 'SEO' }, { value: 'integration', label: '集成入口' }]
const switches = [
  { key: 'payment_enabled', label: '微信支付', desc: '在线收款/定金支付' },
  { key: 'notify_wechat', label: '公众号模板消息', desc: '需求/方案/报价/成交通知' },
  { key: 'notify_sms', label: '短信通道', desc: '预留短信接口' },
  { key: 'notify_station', label: '站内消息', desc: '模板未配置时的兜底通道' }
]
const configs = reactive<Record<string, any>>({ company_name: '', service_phone: '', wechat_oa: '', mp_appid: '', default_language: 'zh-CN', logo: '', payment_enabled: 'false', notify_wechat: 'true', notify_sms: 'false', notify_station: 'true', seo_keywords: '', seo_description: '', icp: '' })
const profile = reactive<any>({ name: '', industry: '', contactName: '', contactPhone: '' })
async function load() {
  const list: any[] = await http.get('/admin/settings/configs')
  list.forEach((c) => { if (c.configValue !== '********') configs[c.configKey] = c.configValue })
  let company: any = null
  try { const me: any = await http.get('/admin/auth/me'); company = me.company } catch {}
  Object.assign(profile, {
    name: company?.name || configs.company_name,
    industry: company?.industry || '',
    contactName: company?.contactName || store.user?.name || '',
    contactPhone: company?.contactPhone || configs.service_phone
  })
}
async function saveConfigs() {
  saving.value = true
  try { await http.put('/admin/settings/configs', configs); toast('系统配置已保存', 'success') } finally { saving.value = false }
}
async function saveProfile() {
  if (!profile.name) return toast('企业名称必填', 'error')
  await http.put('/admin/settings/profile', profile); configs.company_name = profile.name; toast('企业资料已保存', 'success')
}
onMounted(load)
</script>

<style scoped>
.tabs { display: flex; gap: 6px; overflow-x: auto; padding: 2px 0 12px; }
.tabs button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.tabs button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.switch-select { border: 1px solid var(--line); border-radius: 8px; padding: 6px 8px; font-size: 11px; }
.menu-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f2f4f7; font-size: 13px; }
.menu-row:last-child { border-bottom: none; }
.menu-row em { font-style: normal; color: var(--steel-2); font-size: 11px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
</style>
