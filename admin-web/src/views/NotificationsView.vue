<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">消息通知</h1><p class="page-sub">站内消息兜底、公众号模板消息映射、发送限流/重试/日志。</p></div>
      <el-button @click="markAll">全部标记已读</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-tabs v-model="tab">
        <el-tab-pane label="站内消息" name="messages">
          <div style="padding:0 16px 16px">
            <div class="toolbar">
              <el-select v-model="readFlag" clearable placeholder="阅读状态" @change="loadMessages">
                <el-option label="未读" :value="0" /><el-option label="已读" :value="1" />
              </el-select>
            </div>
            <div v-for="n in messages" :key="n.id" class="message-item" :class="{ unread: n.readFlag === 0 }">
              <div class="message-main">
                <div class="row" style="display:flex;justify-content:space-between">
                  <strong>{{ n.title }}</strong>
                  <span class="muted">{{ fmtDateTime(n.createdAt) }}</span>
                </div>
                <div class="message-content">{{ n.content }}</div>
              </div>
              <el-button v-if="n.readFlag === 0" link type="primary" @click="markRead(n)">标记已读</el-button>
            </div>
            <el-empty v-if="!messages.length" description="暂无消息" />
            <div class="pager"><el-pagination v-model:current-page="msgPage" :page-size="msgSize" :total="msgTotal" layout="total, prev, pager, next" @current-change="loadMessages" /></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="模板消息配置" name="configs">
          <div style="padding:0 16px 16px">
            <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
              title="模板 ID 需在微信公众平台申请后填入；未配置模板时自动降级为站内消息，且发送失败会重试并记录日志。" />
            <el-table :data="configs" stripe size="small">
              <el-table-column prop="bizName" label="业务节点" width="180" />
              <el-table-column prop="bizType" label="模板键" width="170" />
              <el-table-column prop="templateId" label="模板 ID" min-width="200" show-overflow-tooltip />
              <el-table-column prop="title" label="标题" min-width="150" />
              <el-table-column label="启用" width="70"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
              <el-table-column prop="minIntervalSeconds" label="限流(秒)" width="90" />
              <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openConfig(row)">配置</el-button></template></el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="发送日志" name="logs">
          <div style="padding:0 16px 16px">
            <el-table :data="logs" stripe size="small">
              <el-table-column prop="createdAt" label="时间" width="170"><template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template></el-table-column>
              <el-table-column prop="channel" label="通道" width="90" />
              <el-table-column prop="bizType" label="节点" width="140" />
              <el-table-column prop="templateId" label="模板 ID" width="150" />
              <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', row.status === 'SUCCESS' ? 'green' : row.status === 'FAILED' ? 'red' : 'gray']">{{ row.status }}</span></template></el-table-column>
              <el-table-column prop="result" label="结果" min-width="260" show-overflow-tooltip />
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="configVisible" title="模板消息配置" width="620px">
      <el-form :model="configForm" label-position="top">
        <el-form-item label="业务节点"><el-input v-model="configForm.bizName" disabled /></el-form-item>
        <el-form-item label="模板 ID"><el-input v-model="configForm.templateId" placeholder="微信公众平台模板消息 ID" /></el-form-item>
        <el-form-item label="消息标题"><el-input v-model="configForm.title" /></el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="configForm.jumpUrl" /></el-form-item>
        <el-form-item label="发送最小间隔（秒）"><el-input-number v-model="configForm.minIntervalSeconds" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="configForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="configVisible=false">取消</el-button><el-button type="primary" @click="saveConfig">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { fmtDateTime } from '@/utils/format'

const tab = ref('messages')
const messages = ref<any[]>([]); const msgTotal = ref(0); const msgPage = ref(1); const msgSize = ref(10); const readFlag = ref<number | undefined>()
const configs = ref<any[]>([]); const logs = ref<any[]>([])
const configVisible = ref(false); const configForm = reactive<any>({})
async function loadMessages() {
  const data: any = await http.get('/admin/notifications', { params: { page: msgPage.value, size: msgSize.value, readFlag: readFlag.value } })
  messages.value = data.records; msgTotal.value = data.total
}
async function loadConfigs() { configs.value = await http.get('/admin/notifications/configs') }
async function loadLogs() { const data: any = await http.get('/admin/notifications/logs', { params: { page: 1, size: 50 } }); logs.value = data.records }
async function markRead(n: any) { await http.post(`/admin/notifications/${n.id}/read`); loadMessages() }
async function markAll() { await http.post('/admin/notifications/read-all'); ElMessage.success('已全部标记已读'); loadMessages() }
function openConfig(row: any) { Object.assign(configForm, row); configVisible.value = true }
async function saveConfig() { await http.put(`/admin/notifications/configs/${configForm.id}`, configForm); ElMessage.success('模板配置已保存'); configVisible.value = false; loadConfigs() }
onMounted(() => { loadMessages(); loadConfigs(); loadLogs() })
</script>
<style scoped>
.message-item { display: flex; align-items: center; gap: 12px; border: 1px solid var(--xh-line); border-radius: 6px; padding: 12px 14px; margin-bottom: 8px; }
.message-item.unread { background: #f5f9ff; border-color: #cfe0fb; }
.message-main { flex: 1; }
.message-content { color: var(--xh-muted); font-size: 13px; margin-top: 4px; }
.pager{display:flex;justify-content:flex-end;padding-top:10px}
</style>
