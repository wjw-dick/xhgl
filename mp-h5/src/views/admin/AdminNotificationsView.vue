<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>消息通知</h1><button v-if="tab === 'messages'" class="link-btn" @click="markAll">全部已读</button></div>
    <div class="page-body">
      <div class="tabs">
        <button v-for="t in tabs" :key="t.value" :class="{ active: tab === t.value }" @click="tab = t.value">{{ t.label }}</button>
      </div>
      <div v-if="tab === 'messages'">
        <div v-if="rows.length" class="card" style="padding:4px 16px">
          <div v-for="n in rows" :key="n.id" class="list-row">
            <div class="main"><strong>{{ n.title }}</strong><span>{{ n.content }}</span><span style="font-size:9px">{{ dateTime(n.createdAt) }}</span></div>
            <button v-if="!n.readFlag" class="link-btn" @click="markRead(n)">已读</button>
          </div>
        </div>
        <div v-else class="empty"><div class="big">通</div>暂无消息</div>
      </div>
      <div v-else-if="tab === 'configs'">
        <div v-if="configs.length" class="card" style="padding:4px 16px">
          <div v-for="c in configs" :key="c.id" class="list-row">
            <div class="main"><strong>{{ c.bizName || c.bizType }}</strong><span>模板 {{ c.templateId || '未配置' }} · 间隔 {{ c.minIntervalSeconds || 0 }}s</span></div>
            <div style="text-align:right"><span :class="['pill', c.enabled ? 'green' : 'gray']">{{ c.enabled ? '启用' : '停用' }}</span><button class="link-btn" style="display:block;margin-top:4px" @click="openConfig(c)">编辑</button></div>
          </div>
        </div>
        <div v-else class="empty"><div class="big">模</div>暂无模板配置</div>
      </div>
      <div v-else>
        <div v-if="logs.length" class="card" style="padding:4px 16px">
          <div v-for="l in logs" :key="l.id" class="list-row">
            <div class="main"><strong>{{ l.bizType }} · {{ l.channel }}</strong><span>{{ l.result || '—' }}</span></div>
            <span :class="['pill', l.status === 'SKIPPED' ? 'yellow' : 'green']">{{ l.status }}</span>
          </div>
        </div>
        <div v-else class="empty"><div class="big">日</div>暂无发送日志</div>
      </div>
    </div>

    <div v-if="configDialog" class="modal-mask" @click.self="configDialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>模板配置</h3><button class="modal-close" @click="configDialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>业务名称</label><input v-model="configForm.bizName" /></div>
          <div class="field full"><label>模板 ID</label><input v-model="configForm.templateId" /></div>
          <div class="field full"><label>标题</label><input v-model="configForm.title" /></div>
          <div class="field"><label>最小间隔（秒）</label><input v-model.number="configForm.minIntervalSeconds" type="number" /></div>
          <div class="field"><label>启用</label><select v-model.number="configForm.enabled"><option :value="1">启用</option><option :value="0">停用</option></select></div>
          <div class="field full"><label>跳转链接</label><input v-model="configForm.jumpUrl" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="configDialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveConfig">保存</button></div>
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

const router = useRouter(); const tab = ref('messages')
const tabs = [{ value: 'messages', label: '消息' }, { value: 'configs', label: '模板' }, { value: 'logs', label: '日志' }]
const rows = ref<any[]>([]); const configs = ref<any[]>([]); const logs = ref<any[]>([])
const configDialog = ref(false); const configForm = reactive<any>({})
async function loadMessages() { const d: any = await http.get('/admin/notifications', { params: { page: 1, size: 50 } }); rows.value = d.records || [] }
async function loadConfigs() { configs.value = await http.get('/admin/notifications/configs') }
async function loadLogs() { const d: any = await http.get('/admin/notifications/logs', { params: { page: 1, size: 50 } }); logs.value = d.records || [] }
async function markRead(n: any) { await http.post(`/admin/notifications/${n.id}/read`); loadMessages() }
async function markAll() { await http.post('/admin/notifications/read-all'); toast('已全部标记已读', 'success'); loadMessages() }
function openConfig(c: any) { Object.assign(configForm, c); configDialog.value = true }
async function saveConfig() { await http.put(`/admin/notifications/configs/${configForm.id}`, configForm); toast('模板配置已保存', 'success'); configDialog.value = false; loadConfigs() }
onMounted(() => { loadMessages(); loadConfigs(); loadLogs() })
</script>

<style scoped>
.tabs { display: flex; gap: 6px; padding: 2px 0 12px; }
.tabs button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 11px; color: var(--steel); }
.tabs button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
</style>
