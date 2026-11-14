<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">售后服务</h1><p class="page-sub">报修受理、派单接单、现场记录、完工回访、满意度评价与售后分析。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新建工单</el-button>
        <el-button @click="loadAnalysis"><el-icon><DataAnalysis /></el-icon> 刷新分析</el-button>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-label">工单总量</div><div class="stat-value">{{ analysis.total || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">待处理 / 处理中</div><div class="stat-value">{{ (analysis.byStatus?.ACCEPTED || 0) + (analysis.byStatus?.DISPATCHED || 0) + (analysis.byStatus?.PROCESSING || 0) }}</div></div>
      <div class="stat-card"><div class="stat-label">平均响应（小时）</div><div class="stat-value">{{ analysis.avgResponseHours || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">一次解决率</div><div class="stat-value">{{ analysis.firstResolutionRate || 0 }}%</div><div class="stat-note">满意度 {{ analysis.avgSatisfaction || 0 }} 分</div></div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="工单号 / 客户 / 设备 / 描述" @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="工单状态" @change="search"><el-option label="已受理" value="ACCEPTED" /><el-option label="已派单" value="DISPATCHED" /><el-option label="处理中" value="PROCESSING" /><el-option label="待回访" value="WAIT_REVIEW" /><el-option label="已完成" value="COMPLETED" /></el-select>
        <el-select v-model="type" clearable placeholder="工单类型" @change="search"><el-option label="报修" value="REPAIR" /><el-option label="安装" value="INSTALL" /><el-option label="保养" value="MAINTAIN" /><el-option label="年检" value="INSPECT" /></el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="ticketNo" label="工单号" width="190" />
        <el-table-column prop="customerName" label="客户" min-width="180" show-overflow-tooltip />
        <el-table-column prop="equipmentNo" label="设备" width="170" />
        <el-table-column label="类型" width="80"><template #default="{ row }">{{ statusLabel(row.type) }}</template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="230" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="technicianName" label="技师" width="90" />
        <el-table-column label="状态" width="96"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">处理</el-button><el-button v-if="!readOnly" link type="danger" @click="removeTicket(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-drawer v-model="drawerVisible" title="工单处理" size="680px">
      <template v-if="detail">
        <div class="page-head" style="margin-bottom:10px">
          <div>
            <h2 style="margin:0">{{ detail.ticket.ticketNo }}</h2>
            <p class="page-sub">{{ detail.ticket.customerName }} · {{ detail.ticket.equipmentNo }} · <span :class="['pill', statusPill(detail.ticket.status)]">{{ statusLabel(detail.ticket.status) }}</span></p>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-button v-if="!readOnly && detail.ticket.status === 'ACCEPTED'" type="primary" size="small" @click="openDispatch">派单</el-button>
            <el-button v-if="!readOnly && ['ACCEPTED','DISPATCHED'].includes(detail.ticket.status)" size="small" @click="accept">接单</el-button>
            <el-button v-if="!readOnly && detail.ticket.status === 'PROCESSING'" size="small" @click="openRecord('现场记录')">现场记录</el-button>
            <el-button v-if="!readOnly && detail.ticket.status === 'PROCESSING'" type="success" size="small" @click="openComplete">完工</el-button>
            <el-button v-if="!readOnly && ['WAIT_REVIEW','COMPLETED'].includes(detail.ticket.status)" type="warning" size="small" @click="openReview">回访评价</el-button>
          </div>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报修类型">{{ statusLabel(detail.ticket.type) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ detail.ticket.priority }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.ticket.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.ticket.contactPhoneMask }}</el-descriptions-item>
          <el-descriptions-item label="技师">{{ detail.ticket.technicianName || '未派单' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detail.ticket.source }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ detail.ticket.description }}</el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2">{{ detail.ticket.solution || '—' }}</el-descriptions-item>
          <el-descriptions-item label="满意度">{{ detail.ticket.satisfactionScore || '—' }}</el-descriptions-item>
          <el-descriptions-item label="客户评价">{{ detail.ticket.review || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="card-title" style="margin-top:20px">现场记录</h3>
        <el-timeline style="padding-left:8px">
          <el-timeline-item v-for="r in detail.records" :key="r.id" :timestamp="`${fmtDateTime(r.createdAt)} · ${r.operator}`" placement="top">
            <div class="kanban-item"><div class="row"><strong>{{ r.action }}</strong></div><div style="margin-top:6px">{{ r.content }}</div></div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>

    <el-dialog v-model="createVisible" title="新建售后工单" width="760px" destroy-on-close>
      <el-form :model="createForm" label-position="top" class="form-two">
        <el-form-item label="客户" required><el-select v-model="createForm.customerId" filterable style="width:100%" @change="loadEquipment"><el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="设备"><el-select v-model="createForm.equipmentId" filterable clearable style="width:100%"><el-option v-for="e in equipment" :key="e.id" :label="`${e.equipmentNo} · ${e.model}`" :value="e.id" /></el-select></el-form-item>
        <el-form-item label="工单类型"><el-select v-model="createForm.type" style="width:100%"><el-option label="报修" value="REPAIR" /><el-option label="安装" value="INSTALL" /><el-option label="保养" value="MAINTAIN" /><el-option label="年检" value="INSPECT" /></el-select></el-form-item>
        <el-form-item label="优先级"><el-select v-model="createForm.priority" style="width:100%"><el-option label="普通" value="NORMAL" /><el-option label="紧急" value="HIGH" /><el-option label="低" value="LOW" /></el-select></el-form-item>
        <el-form-item label="联系人"><el-input v-model="createForm.contactName" /></el-form-item>
        <el-form-item label="联系电话（脱敏）"><el-input v-model="createForm.contactPhoneMask" /></el-form-item>
        <el-form-item label="故障/服务描述" class="full"><el-input v-model="createForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="现场照片" class="full"><ImageUpload v-model="createForm.photos" category="ticket" :limit="6" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" @click="saveCreate">创建工单</el-button></template>
    </el-dialog>

    <el-dialog v-model="dispatchVisible" title="派单" width="480px">
      <el-form label-position="top">
        <el-form-item label="技师">
          <el-select v-model="dispatchTech" filterable style="width:100%">
            <el-option v-for="u in detail?.technicians || []" :key="u.id" :label="`${u.name}（${u.roleCode}）`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dispatchVisible=false">取消</el-button><el-button type="primary" @click="dispatch">确认派单</el-button></template>
    </el-dialog>

    <el-dialog v-model="recordVisible" :title="recordAction" width="560px">
      <el-input v-model="recordContent" type="textarea" :rows="5" placeholder="记录现场检查、处理措施、耗材使用等情况" />
      <template #footer><el-button @click="recordVisible=false">取消</el-button><el-button type="primary" @click="saveRecord">保存记录</el-button></template>
    </el-dialog>

    <el-dialog v-model="completeVisible" title="工单完工" width="560px">
      <el-input v-model="completeContent" type="textarea" :rows="5" placeholder="处理结果与更换备件说明" />
      <template #footer><el-button @click="completeVisible=false">取消</el-button><el-button type="success" @click="complete">确认完工</el-button></template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="回访与满意度评价" width="520px">
      <el-form label-position="top">
        <el-form-item label="满意度评分"><el-rate v-model="reviewScore" /></el-form-item>
        <el-form-item label="客户评价"><el-input v-model="reviewComment" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="reviewVisible=false">取消</el-button><el-button type="primary" @click="review">保存评价</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import ImageUpload from '@/components/ImageUpload.vue'
import { fmtDateTime, statusLabel, statusPill } from '@/utils/format'

const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const type = ref(''); const loading = ref(false)
const analysis = ref<any>({})
const drawerVisible = ref(false); const detail = ref<any>(null)
const createVisible = ref(false); const createForm = reactive<any>({ type: 'REPAIR', priority: 'NORMAL' })
const customers = ref<any[]>([]); const equipment = ref<any[]>([])
const dispatchVisible = ref(false); const dispatchTech = ref<number>()
const recordVisible = ref(false); const recordAction = ref('现场记录'); const recordContent = ref('')
const completeVisible = ref(false); const completeContent = ref('')
const reviewVisible = ref(false); const reviewScore = ref(5); const reviewComment = ref('')
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/tickets', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value, type: type.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
async function loadAnalysis() { try { analysis.value = await http.get('/admin/tickets/analysis') } catch { analysis.value = {} } }
async function loadOptions() { try { customers.value = await http.get('/admin/customers/options') } catch {} }
async function loadEquipment(customerId: number) {
  try { const d: any = await http.get('/admin/equipment', { params: { customerId, size: 100 } }); equipment.value = d.records } catch { equipment.value = [] }
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; status.value = ''; type.value = ''; search() }
function openCreate() { Object.keys(createForm).forEach((k) => delete createForm[k]); Object.assign(createForm, { type: 'REPAIR', priority: 'NORMAL' }); equipment.value = []; createVisible.value = true }
async function saveCreate() {
  if (!createForm.customerId) return ElMessage.warning('请选择客户')
  await http.post('/admin/tickets', createForm)
  ElMessage.success('工单已创建'); createVisible.value = false; load(); loadAnalysis()
}
async function openDetail(row: any) { detail.value = await http.get(`/admin/tickets/${row.id}`); drawerVisible.value = true }
async function removeTicket(row: any) {
  await ElMessageBox.confirm(
    `确认删除工单「${row.ticketNo}」？将同时物理删除处理记录、满意度与备件记录，删除后不可恢复。`,
    '删除工单',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/tickets/${row.id}`)
  ElMessage.success('工单已删除')
  drawerVisible.value = false
  if (rows.value.length === 1 && page.value > 1) page.value--
  load(); loadAnalysis()
}
function openDispatch() { dispatchVisible.value = true }
async function dispatch() {
  if (!dispatchTech.value) return ElMessage.warning('请选择技师')
  await http.post(`/admin/tickets/${detail.value.ticket.id}/dispatch`, { technicianId: dispatchTech.value })
  ElMessage.success('已派单'); dispatchVisible.value = false; openDetail(detail.value.ticket); load()
}
async function accept() { await http.post(`/admin/tickets/${detail.value.ticket.id}/accept`); ElMessage.success('已接单'); openDetail(detail.value.ticket); load() }
function openRecord(action: string) { recordAction.value = action; recordContent.value = ''; recordVisible.value = true }
async function saveRecord() { await http.post(`/admin/tickets/${detail.value.ticket.id}/records`, { action: recordAction.value, content: recordContent.value }); ElMessage.success('记录已保存'); recordVisible.value = false; openDetail(detail.value.ticket) }
function openComplete() { completeContent.value = ''; completeVisible.value = true }
async function complete() { await http.post(`/admin/tickets/${detail.value.ticket.id}/complete`, { solution: completeContent.value }); ElMessage.success('工单已完工，待回访'); completeVisible.value = false; openDetail(detail.value.ticket); load(); loadAnalysis() }
function openReview() { reviewScore.value = detail.value.ticket.satisfactionScore || 5; reviewComment.value = detail.value.ticket.review || ''; reviewVisible.value = true }
async function review() { await http.post(`/admin/tickets/${detail.value.ticket.id}/review`, { score: reviewScore.value, comment: reviewComment.value }); ElMessage.success('满意度已保存'); reviewVisible.value = false; openDetail(detail.value.ticket); load(); loadAnalysis() }
onMounted(() => { load(); loadAnalysis(); loadOptions() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
