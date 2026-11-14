<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>微信配置</h1><button class="link-btn" @click="saveConfig">保存</button></div>
    <div class="page-body">
      <div class="tabs">
        <button v-for="t in tabs" :key="t.value" :class="{ active: tab === t.value }" @click="tab = t.value">{{ t.label }}</button>
      </div>
      <div v-if="tab === 'config'" class="card">
        <div class="section-label" style="margin-top:0">小程序</div>
        <div class="form-grid">
          <div class="field full"><label>小程序 AppID</label><input v-model="configs.mpAppid" /></div>
          <div class="field full"><label>AppSecret（留空不修改）</label><input v-model="configs.mpSecret" type="password" /></div>
          <div class="field"><label>消息 Token</label><input v-model="configs.mpToken" /></div>
          <div class="field"><label>EncodingAESKey</label><input v-model="configs.mpAesKey" type="password" /></div>
          <div class="field"><label>request 合法域名</label><input v-model="configs.mpRequestDomain" /></div>
          <div class="field"><label>upload 合法域名</label><input v-model="configs.mpUploadDomain" /></div>
          <div class="field full"><label>download 合法域名</label><input v-model="configs.mpDownloadDomain" /></div>
        </div>
        <div class="section-label">公众号</div>
        <div class="form-grid">
          <div class="field full"><label>公众号 AppID</label><input v-model="configs.oaAppid" /></div>
          <div class="field full"><label>AppSecret（留空不修改）</label><input v-model="configs.oaSecret" type="password" /></div>
          <div class="field full"><label>服务器 URL</label><input v-model="configs.oaServerUrl" /></div>
          <div class="field"><label>服务器 Token</label><input v-model="configs.oaToken" /></div>
          <div class="field"><label>EncodingAESKey</label><input v-model="configs.oaAesKey" type="password" /></div>
          <div class="field"><label>网页授权域名</label><input v-model="configs.oauthDomain" /></div>
          <div class="field"><label>JS 安全域名</label><input v-model="configs.jsDomain" /></div>
          <div class="field full"><label>开放平台 AppID</label><input v-model="configs.openPlatformAppid" /></div>
        </div>
        <div class="section-label">回复配置</div>
        <div class="field"><label>关注欢迎语</label><textarea v-model="configs.welcomeText" rows="2" /></div>
        <div class="field" style="margin-top:10px"><label>关键词回复规则</label><textarea v-model="configs.keywordReply" rows="3" /></div>
      </div>

      <div v-else-if="tab === 'menu'" class="card">
        <div class="field"><label>公众号菜单 JSON</label><textarea v-model="menuJson" rows="12" class="mono" /></div>
        <div class="action-grid">
          <button class="btn btn-primary btn-sm" @click="saveMenu">保存菜单</button>
          <button class="btn btn-ghost btn-sm" @click="syncMenu">同步微信</button>
        </div>
      </div>

      <div v-else class="card">
        <div class="card-title"><h2>图文素材</h2><button class="link-btn" @click="materialVisible=true">＋ 新增</button></div>
        <div v-for="m in materials" :key="m.id" class="list-row">
          <div class="main"><strong>{{ m.title }}</strong><span>{{ m.mediaType }} · {{ dateTime(m.createdAt) }}</span></div>
          <button class="link-btn danger" @click="removeMaterial(m)">删除</button>
        </div>
        <div v-if="!materials.length" class="muted" style="font-size:11px">暂无素材</div>
        <div class="card-title" style="margin-top:16px"><h2>用户标签</h2><button class="link-btn" @click="tagVisible=true">＋ 新增</button></div>
        <div v-for="t in tags" :key="t.id" class="list-row">
          <div class="main"><strong>{{ t.name }}</strong><span>粉丝 {{ t.fanCount || 0 }}</span></div>
          <button class="link-btn danger" @click="removeTag(t)">删除</button>
        </div>
        <div v-if="!tags.length" class="muted" style="font-size:11px">暂无标签</div>
        <div class="card-title" style="margin-top:16px"><h2>粉丝列表</h2></div>
        <div v-for="f in fans" :key="f.id" class="list-row">
          <div class="main"><strong>{{ f.nickname || f.openid }}</strong><span>{{ f.openid }} · 关注 {{ f.subscribed ? '是' : '否' }} · 客户 {{ f.bindCustomerId || '—' }}</span></div>
        </div>
        <div v-if="!fans.length" class="muted" style="font-size:11px">暂无粉丝</div>
        <div class="card-title" style="margin-top:16px"><h2>定向群发</h2><button class="link-btn" @click="massVisible=true">＋ 创建</button></div>
        <div v-for="m in massLogs" :key="m.id" class="list-row">
          <div class="main"><strong>{{ m.targetType }} · {{ m.content }}</strong><span>{{ dateTime(m.createdAt) }} · {{ m.result || '—' }}</span></div>
          <span :class="['pill', m.status === 'SENT' ? 'green' : m.status === 'FAILED' ? 'red' : 'yellow']">{{ m.status }}</span>
        </div>
        <div v-if="!massLogs.length" class="muted" style="font-size:11px">暂无群发记录</div>
      </div>
    </div>

    <div v-if="materialVisible" class="modal-mask" @click.self="materialVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增图文素材</h3><button class="modal-close" @click="materialVisible=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>标题</label><input v-model="materialForm.title" /></div>
          <div class="field"><label>类型</label><select v-model="materialForm.mediaType"><option value="NEWS">图文</option><option value="IMAGE">图片</option><option value="VIDEO">视频</option></select></div>
          <div class="field"><label>素材 URL</label><input v-model="materialForm.url" /></div>
          <div class="field full"><label>正文/摘要</label><textarea v-model="materialForm.content" rows="4" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="materialVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveMaterial">保存</button></div>
      </div>
    </div>
    <div v-if="tagVisible" class="modal-mask" @click.self="tagVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增标签</h3><button class="modal-close" @click="tagVisible=false">×</button></div>
        <div class="field"><label>标签名称</label><input v-model="tagName" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="tagVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveTag">保存</button></div>
      </div>
    </div>
    <div v-if="massVisible" class="modal-mask" @click.self="massVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>定向群发</h3><button class="modal-close" @click="massVisible=false">×</button></div>
        <div class="field"><label>发送对象</label><select v-model="massForm.targetType"><option value="ALL">全部粉丝</option><option value="TAG">按标签</option><option value="OPENIDS">指定 OpenID</option></select></div>
        <div v-if="massForm.targetType === 'TAG'" class="field" style="margin-top:10px"><label>标签 ID</label><input v-model="massForm.tagId" /></div>
        <div v-if="massForm.targetType === 'OPENIDS'" class="field" style="margin-top:10px"><label>OpenID（逗号分隔）</label><textarea v-model="massForm.openids" rows="2" /></div>
        <div class="field" style="margin-top:10px"><label>文本内容</label><textarea v-model="massForm.content" rows="4" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="massVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="sendMass">提交群发</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { dateTime } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const tabs = [{ value: 'config', label: '账号配置' }, { value: 'menu', label: '公众号菜单' }, { value: 'content', label: '内容运营' }]
const tab = ref('config'); const configs = reactive<Record<string, any>>({})
const menuJson = ref(''); const materials = ref<any[]>([]); const tags = ref<any[]>([])
const materialVisible = ref(false); const materialForm = reactive<any>({ mediaType: 'NEWS' })
const tagVisible = ref(false); const tagName = ref('')
const fans = ref<any[]>([]); const fanTotal = ref(0); const fanPage = ref(1); const massLogs = ref<any[]>([])
const massVisible = ref(false); const massForm = reactive<any>({ targetType: 'ALL', contentType: 'text', content: '' })
async function loadConfig() { Object.assign(configs, await http.get('/admin/wechat/config')); const menu: any = await http.get('/admin/wechat/menu'); menuJson.value = JSON.stringify(menu || {}, null, 2) }
async function saveConfig() { await http.put('/admin/wechat/config', configs); toast('微信配置已保存', 'success'); loadConfig() }
async function saveMenu() { try { await http.put('/admin/wechat/menu', JSON.parse(menuJson.value)); toast('菜单已保存', 'success') } catch { toast('菜单 JSON 格式错误', 'error') } }
async function syncMenu() { try { await http.post('/admin/wechat/menu/sync'); toast('菜单已同步微信', 'success') } catch {} }
async function loadContent() {
  try { const m: any = await http.get('/admin/wechat/materials', { params: { page: 1, size: 50 } }); materials.value = m.records || [] } catch {}
  try { tags.value = await http.get('/admin/wechat/tags') } catch {}
  try { const f: any = await http.get('/admin/wechat/fans', { params: { page: fanPage.value, size: 20 } }); fans.value = f.records || []; fanTotal.value = f.total || 0 } catch {}
  try { const l: any = await http.get('/admin/wechat/mass-logs', { params: { page: 1, size: 20 } }); massLogs.value = l.records || [] } catch {}
}
async function saveMaterial() { await http.post('/admin/wechat/materials', materialForm); toast('素材已保存', 'success'); materialVisible.value = false; loadContent() }
async function removeMaterial(m: any) { if (!confirm('确认删除该素材？')) return; await http.delete(`/admin/wechat/materials/${m.id}`); toast('已删除', 'success'); loadContent() }
async function saveTag() { if (!tagName.value) return; await http.post('/admin/wechat/tags', { name: tagName.value }); tagVisible.value = false; tagName.value = ''; toast('标签已新增', 'success'); loadContent() }
async function removeTag(t: any) { if (!confirm('确认删除该标签？')) return; await http.delete(`/admin/wechat/tags/${t.id}`); toast('已删除', 'success'); loadContent() }
async function sendMass() { await http.post('/admin/wechat/mass-send', massForm); toast('群发已提交', 'success'); massVisible.value = false; massForm.content = ''; loadContent() }
onMounted(() => { loadConfig(); loadContent() })
</script>

<style scoped>
.tabs { display: flex; gap: 6px; padding: 2px 0 12px; }
.tabs button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 11px; color: var(--steel); }
.tabs button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.action-grid { display: flex; gap: 8px; margin-top: 10px; }
</style>
