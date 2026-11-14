<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">微信配置</h1><p class="page-sub">小程序 / 公众号 / 开放平台 UnionID 底座；密钥 AES 加密、回掩码、配置修改留审计。</p></div>
      <el-button type="primary" :loading="saving" @click="saveConfig">保存微信配置</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-tabs v-model="tab">
        <el-tab-pane label="账号与服务器" name="config">
          <div style="padding:0 16px 16px">
            <el-alert type="warning" :closable="false" show-icon style="margin-bottom:14px"
              title="AppSecret / EncodingAESKey 页面仅显示掩码；真实微信联调需服务号认证、开放平台 UnionID 主体、合法域名与服务器回调 URL。" />
            <div class="grid-2">
              <div>
                <h3 class="card-title">小程序</h3>
                <el-form :model="configs" label-position="top">
                  <el-form-item label="小程序 AppID"><el-input v-model="configs.mpAppid" /></el-form-item>
                  <el-form-item label="小程序 AppSecret"><el-input v-model="configs.mpSecret" type="password" show-password placeholder="已保存则显示掩码，留空不修改" /></el-form-item>
                  <el-form-item label="消息推送 Token"><el-input v-model="configs.mpToken" /></el-form-item>
                  <el-form-item label="EncodingAESKey"><el-input v-model="configs.mpAesKey" type="password" show-password placeholder="留空不修改" /></el-form-item>
                  <el-form-item label="request 合法域名"><el-input v-model="configs.mpRequestDomain" /></el-form-item>
                  <el-form-item label="upload 合法域名"><el-input v-model="configs.mpUploadDomain" /></el-form-item>
                  <el-form-item label="download 合法域名"><el-input v-model="configs.mpDownloadDomain" /></el-form-item>
                </el-form>
              </div>
              <div>
                <h3 class="card-title">公众号（服务号）</h3>
                <el-form :model="configs" label-position="top">
                  <el-form-item label="公众号 AppID"><el-input v-model="configs.oaAppid" /></el-form-item>
                  <el-form-item label="公众号 AppSecret"><el-input v-model="configs.oaSecret" type="password" show-password placeholder="留空不修改" /></el-form-item>
                  <el-form-item label="服务器 URL"><el-input v-model="configs.oaServerUrl" placeholder="https://api.example.com/api/public/wechat/callback" /></el-form-item>
                  <el-form-item label="服务器 Token"><el-input v-model="configs.oaToken" /></el-form-item>
                  <el-form-item label="EncodingAESKey"><el-input v-model="configs.oaAesKey" type="password" show-password placeholder="安全模式使用，留空不修改" /></el-form-item>
                  <el-form-item label="网页授权域名"><el-input v-model="configs.oauthDomain" /></el-form-item>
                  <el-form-item label="JS 安全域名"><el-input v-model="configs.jsDomain" /></el-form-item>
                  <el-form-item label="开放平台 AppID"><el-input v-model="configs.openPlatformAppid" /></el-form-item>
                </el-form>
              </div>
            </div>
            <h3 class="card-title" style="margin-top:10px">关注与关键词回复</h3>
            <el-form :model="configs" label-position="top">
              <el-form-item label="关注欢迎语"><el-input v-model="configs.welcomeText" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="关键词回复规则"><el-input v-model="configs.keywordReply" type="textarea" :rows="3" placeholder="需求:提交采购需求|报价:查看报价|报修:售后报修" /></el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="公众号菜单" name="menu">
          <div style="padding:0 16px 16px">
            <div class="grid-2">
              <div>
                <el-form label-position="top">
                  <el-form-item label="菜单 JSON（view / miniprogram / click）">
                    <el-input v-model="menuJson" type="textarea" :rows="18" class="mono" />
                  </el-form-item>
                </el-form>
                <div style="display:flex;gap:8px">
                  <el-button type="primary" @click="saveMenu">保存菜单</el-button>
                  <el-button @click="syncMenu">一键同步微信</el-button>
                </div>
                <div class="notice" style="margin-top:12px">同步需要真实服务号 AppID/Secret 与 IP 白名单；本地无资质时将返回明确错误并记录审计。</div>
              </div>
              <div class="wx-menu-preview">
                <div class="wx-input">鑫火锅炉 · 公众号会话</div>
                <div class="wx-message">您好，欢迎关注鑫火锅炉。回复“需求”提交采购需求。</div>
                <div class="wx-buttons">
                  <div v-for="(b, i) in menuButtons" :key="i" class="wx-btn">{{ b.name }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="内容运营" name="content">
          <div style="padding:0 16px 16px">
            <el-tabs v-model="contentTab" type="card">
              <el-tab-pane label="图文素材" name="material">
                <div class="toolbar"><el-button type="primary" size="small" @click="materialVisible=true">新增素材</el-button></div>
                <el-table :data="materials" stripe size="small">
                  <el-table-column prop="title" label="标题" min-width="220" />
                  <el-table-column prop="mediaType" label="类型" width="100" />
                  <el-table-column prop="mediaId" label="微信 media_id" width="200" />
                  <el-table-column prop="status" label="状态" width="90" />
                  <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="danger" @click="removeMaterial(row)">删除</el-button></template></el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="粉丝标签" name="tag">
                <div class="toolbar"><el-button type="primary" size="small" @click="tagVisible=true">新增标签</el-button></div>
                <el-table :data="tags" stripe size="small">
                  <el-table-column prop="name" label="标签名称" />
                  <el-table-column prop="wxTagId" label="微信 tag_id" width="140" />
                  <el-table-column prop="fanCount" label="粉丝数" width="100" />
                  <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="danger" @click="removeTag(row)">删除</el-button></template></el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="粉丝列表" name="fan">
                <el-table :data="fans" stripe size="small">
                  <el-table-column prop="openid" label="OpenID" min-width="220" />
                  <el-table-column prop="nickname" label="昵称" width="150" />
                  <el-table-column prop="tags" label="标签" width="150" />
                  <el-table-column prop="subscribed" label="关注" width="80" />
                  <el-table-column prop="bindCustomerId" label="绑定客户" width="100" />
                </el-table>
                <div class="pager"><el-pagination v-model:current-page="fanPage" :page-size="10" :total="fanTotal" layout="total, prev, pager, next" @current-change="loadFans" /></div>
              </el-tab-pane>
              <el-tab-pane label="定向群发" name="mass">
                <div class="toolbar">
                  <el-button type="primary" size="small" @click="massVisible=true">创建群发</el-button>
                  <span class="muted">全部粉丝 / 标签 / 指定 OpenID，文本消息</span>
                </div>
                <el-table :data="massLogs" stripe size="small">
                  <el-table-column prop="createdAt" label="时间" width="170"><template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template></el-table-column>
                  <el-table-column prop="targetType" label="目标" width="100" />
                  <el-table-column prop="contentType" label="类型" width="90" />
                  <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
                  <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><span :class="['pill', row.status === 'SENT' ? 'green' : row.status === 'FAILED' ? 'red' : 'yellow']">{{ row.status }}</span></template></el-table-column>
                  <el-table-column prop="result" label="结果" min-width="220" show-overflow-tooltip />
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="materialVisible" title="新增图文素材" width="600px">
      <el-form :model="materialForm" label-position="top">
        <el-form-item label="标题"><el-input v-model="materialForm.title" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="materialForm.mediaType" style="width:100%"><el-option label="图文" value="NEWS" /><el-option label="图片" value="IMAGE" /></el-select></el-form-item>
        <el-form-item label="正文"><el-input v-model="materialForm.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="图片 URL"><el-input v-model="materialForm.url" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="materialVisible=false">取消</el-button><el-button type="primary" @click="saveMaterial">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="tagVisible" title="新增粉丝标签" width="420px">
      <el-input v-model="tagName" placeholder="标签名称" />
      <template #footer><el-button @click="tagVisible=false">取消</el-button><el-button type="primary" @click="saveTag">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="massVisible" title="定向群发" width="560px">
      <el-form :model="massForm" label-position="top">
        <el-form-item label="发送对象"><el-select v-model="massForm.targetType" style="width:100%"><el-option label="全部粉丝" value="ALL" /><el-option label="按标签" value="TAG" /><el-option label="指定 OpenID" value="OPENIDS" /></el-select></el-form-item>
        <el-form-item v-if="massForm.targetType === 'TAG'" label="标签 ID"><el-input v-model="massForm.tagId" /></el-form-item>
        <el-form-item v-if="massForm.targetType === 'OPENIDS'" label="OpenID（逗号分隔）"><el-input v-model="massForm.openids" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="文本内容"><el-input v-model="massForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="massVisible=false">取消</el-button><el-button type="primary" @click="sendMass">提交群发</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { fmtDateTime } from '@/utils/format'

const tab = ref('config'); const contentTab = ref('material'); const saving = ref(false)
const configs = reactive<any>({})
const menuJson = ref('')
const materials = ref<any[]>([]); const tags = ref<any[]>([]); const fans = ref<any[]>([]); const fanPage = ref(1); const fanTotal = ref(0); const massLogs = ref<any[]>([])
const materialVisible = ref(false); const tagVisible = ref(false); const massVisible = ref(false); const tagName = ref('')
const materialForm = reactive<any>({ mediaType: 'NEWS' }); const massForm = reactive<any>({ targetType: 'ALL', contentType: 'text' })
const menuButtons = computed(() => {
  try { return (JSON.parse(menuJson.value || '{}').button || []).slice(0, 3) } catch { return [] }
})
async function loadConfig() {
  Object.assign(configs, await http.get('/admin/wechat/config'))
  const menu: any = await http.get('/admin/wechat/menu')
  menuJson.value = JSON.stringify(menu, null, 2)
}
async function saveConfig() {
  saving.value = true
  try { await http.put('/admin/wechat/config', configs); ElMessage.success('微信配置已保存并实时生效'); await loadConfig() } finally { saving.value = false }
}
async function saveMenu() {
  try { const menu = JSON.parse(menuJson.value) } catch { return ElMessage.error('菜单 JSON 格式错误') }
  await http.put('/admin/wechat/menu', JSON.parse(menuJson.value)); ElMessage.success('菜单已保存')
}
async function syncMenu() { try { await http.post('/admin/wechat/menu/sync'); ElMessage.success('菜单已同步微信') } catch {} }
async function loadContent() {
  const m: any = await http.get('/admin/wechat/materials', { params: { page: 1, size: 20 } }); materials.value = m.records
  tags.value = await http.get('/admin/wechat/tags')
  const l: any = await http.get('/admin/wechat/mass-logs', { params: { page: 1, size: 20 } }); massLogs.value = l.records
  loadFans()
}
async function loadFans() { const d: any = await http.get('/admin/wechat/fans', { params: { page: fanPage.value, size: 10 } }); fans.value = d.records; fanTotal.value = d.total }
async function saveMaterial() { await http.post('/admin/wechat/materials', materialForm); ElMessage.success('素材已保存'); materialVisible.value = false; loadContent() }
async function removeMaterial(row: any) { await http.delete(`/admin/wechat/materials/${row.id}`); loadContent() }
async function saveTag() { if (!tagName.value) return; await http.post('/admin/wechat/tags', { name: tagName.value }); tagVisible.value = false; tagName.value = ''; loadContent() }
async function removeTag(row: any) { await http.delete(`/admin/wechat/tags/${row.id}`); loadContent() }
async function sendMass() {
  await ElMessageBox.confirm('群发受微信频率限制，确认提交？', '群发确认', { type: 'warning' })
  await http.post('/admin/wechat/mass-send', massForm); massVisible.value = false; loadContent()
}
onMounted(() => { loadConfig(); loadContent() })
</script>
<style scoped>
.mono :deep(textarea) { font-family: Consolas, monospace; font-size: 12px; }
.wx-menu-preview { background: #f2f2f2; border-radius: 8px; padding: 14px; max-width: 360px; margin: 0 auto; }
.wx-input { background: #fff; border-radius: 4px; padding: 10px; font-size: 12px; color: #57606a; }
.wx-message { background: #fff; border-radius: 6px; padding: 12px; font-size: 12px; color: #354052; margin: 10px 0; line-height: 1.7; }
.wx-buttons { background: #fff; border-radius: 6px; overflow: hidden; display: grid; grid-template-columns: repeat(3, 1fr); }
.wx-btn { padding: 10px; text-align: center; font-size: 11px; border-right: 1px solid #eee; }
.pager{display:flex;justify-content:flex-end;padding-top:10px}
</style>
