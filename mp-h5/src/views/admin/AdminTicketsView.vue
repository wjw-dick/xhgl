<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>售后服务</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新建</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="工单号 / 客户 / 设备 / 描述" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; search()">{{ s.label }}</button>
        <button class="link-btn" style="margin-left:auto" @click="refreshAnalysis">分析 ›</button>
      </div>
      <div v-if="analysis.total" class="grid-4" style="margin-top:10px">
        <div class="stat-card"><span>工单</span><strong>{{ analysis.total }}</strong><em>待处理 {{ pending }}</em></div>
        <div class="stat-card"><span>响应</span><strong>{{ analysis.avgResponseHours || 0 }}</strong><em>小时</em></div>
        <div class="stat-card"><span>一次解决</span><strong>{{ analysis.firstResolutionRate || 0 }}%</strong><em>售后效率</em></div>
        <div class="stat-card"><span>满意度</span><strong>{{ analysis.avgSatisfaction || 0 }}</strong><em>5 分制</em></div>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="t in rows" :key="t.id" class="ticket-row">
          <button class="ticket-main" @click="router.push(`/admin/tickets/${t.id}`)">
            <div class="row-top"><strong class="mono">{{ t.ticketNo }}</strong><span :class="['pill', statusPill(t.status)]">{{ label(t.status) }}</span></div>
            <div class="row-mid">{{ t.customerName }} · {{ t.equipmentNo || '无设备' }} · {{ label(t.type) }}</div>
            <div class="row-bottom"><span>{{ t.description }}</span><span>{{ t.technicianName || '未派单' }}</span></div>
          </button>
          <div class="row-actions"><button class="link-btn" @click="router.push(`/admin/tickets/${t.id}`)">处理</button><button class="link-btn danger" @click="remove(t)">删除</button></div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">修</div>暂无工单</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新建售后工单</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>客户 *</label><select v-model.number="form.customerId" @change="loadEquipment"><option value="">请选择客户</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option></select></div>
          <div class="field full"><label>设备</label><select v-model.number="form.equipmentId"><option value="">不关联设备</option><option v-for="e in equipment" :key="e.id" :value="e.id">{{ e.equipmentNo }} · {{ e.model }}</option></select></div>
          <div class="field"><label>工单类型</label><select v-model="form.type"><option value="REPAIR">报修</option><option value="INSTALL">安装</option><option value="MAINTAIN">保养</option><option value="INSPECT">年检</option></select></div>
          <div class="field"><label>优先级</label><select v-model="form.priority"><option value="NORMAL">普通</option><option value="HIGH">紧急</option><option value="LOW">低</option></select></div>
          <div class="field"><label>联系人</label><input v-model="form.contactName" /></div>
          <div class="field"><label>联系电话（脱敏）</label><input v-model="form.contactPhoneMask" /></div>
          <div class="field full"><label>故障/服务描述</label><textarea v-model="form.description" rows="3" /></div>
          <div class="field full"><label>现场照片</label><AdminUploader v-model="form.photos" image category="ticket" :limit="6" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">创建工单</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import AdminUploader from '@/components/AdminUploader.vue'
import { label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false); const analysis = ref<any>({})
const dialog = ref(false); const customers = ref<any[]>([]); const equipment = ref<any[]>([])
const form = reactive<any>({ type: 'REPAIR', priority: 'NORMAL' })
const statuses = [{ label: '全部', value: '' }, { label: '已受理', value: 'ACCEPTED' }, { label: '已派单', value: 'DISPATCHED' }, { label: '处理中', value: 'PROCESSING' }, { label: '待回访', value: 'WAIT_REVIEW' }, { label: '已完成', value: 'COMPLETED' }]
const pending = computed(() => (analysis.value.byStatus?.ACCEPTED || 0) + (analysis.value.byStatus?.DISPATCHED || 0) + (analysis.value.byStatus?.PROCESSING || 0))
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/tickets', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function refreshAnalysis() { try { analysis.value = await http.get('/admin/tickets/analysis') } catch { analysis.value = {} } }
async function loadCustomers() { try { customers.value = await http.get('/admin/customers/options') } catch {} }
async function loadEquipment(event: Event) {
  const customerId = Number((event.target as HTMLSelectElement).value)
  try { const d: any = await http.get('/admin/equipment', { params: { customerId, size: 100 } }); equipment.value = d.records || [] } catch { equipment.value = [] }
}
function search() { page.value = 1; load() }
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { type: 'REPAIR', priority: 'NORMAL' })
  equipment.value = []
  dialog.value = true
}
async function save() {
  if (!form.customerId) return toast('请选择客户', 'error')
  await http.post('/admin/tickets', form); toast('工单已创建', 'success'); dialog.value = false; load(); refreshAnalysis()
}
async function remove(row: any) { if (!confirm(`确认删除工单「${row.ticketNo}」？`)) return; try { await http.delete(`/admin/tickets/${row.id}`); toast('已删除', 'success'); load(); refreshAnalysis() } catch {} }
onMounted(() => { load(); refreshAnalysis(); loadCustomers() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; align-items: center; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.ticket-row { border-bottom: 1px solid #f2f4f7; padding: 6px 0 8px; }
.ticket-row:last-child { border-bottom: none; }
.ticket-main { display: block; width: 100%; text-align: left; border: none; background: transparent; padding: 6px 0; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 11px; color: #344054; margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; gap: 8px; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-bottom span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 3px; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
