<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>工单详情</h1><button class="link-btn danger" @click="removeTicket">删除</button></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="ticket" class="page-body">
      <div class="card">
        <div class="row-top"><strong class="mono">{{ ticket.ticketNo }}</strong><span :class="['pill', statusPill(ticket.status)]">{{ label(ticket.status) }}</span></div>
        <div class="row-mid">{{ ticket.customerName }} · {{ ticket.equipmentNo || '无设备' }} · {{ label(ticket.type) }}</div>
        <div class="info-grid">
          <div><span>优先级</span><strong>{{ ticket.priority }}</strong></div>
          <div><span>技师</span><strong>{{ ticket.technicianName || '未派单' }}</strong></div>
          <div><span>联系人</span><strong>{{ ticket.contactName || '—' }}</strong></div>
          <div><span>联系电话</span><strong>{{ ticket.contactPhoneMask || '—' }}</strong></div>
          <div><span>满意度</span><strong>{{ ticket.satisfactionScore || '—' }}</strong></div>
          <div><span>来源</span><strong>{{ ticket.source || '—' }}</strong></div>
          <div class="full"><span>故障描述</span><strong>{{ ticket.description || '—' }}</strong></div>
          <div class="full"><span>处理结果</span><strong>{{ ticket.solution || '—' }}</strong></div>
          <div class="full"><span>客户评价</span><strong>{{ ticket.review || '—' }}</strong></div>
        </div>
        <div class="action-grid">
          <button v-if="ticket.status === 'ACCEPTED'" class="btn btn-primary btn-sm" @click="dispatchVisible=true">派单</button>
          <button v-if="['ACCEPTED','DISPATCHED'].includes(ticket.status)" class="btn btn-ghost btn-sm" @click="accept">接单</button>
          <button v-if="ticket.status === 'PROCESSING'" class="btn btn-ghost btn-sm" @click="openRecord">现场记录</button>
          <button v-if="ticket.status === 'PROCESSING'" class="btn btn-primary btn-sm" @click="completeVisible=true">完工</button>
          <button v-if="['WAIT_REVIEW','COMPLETED'].includes(ticket.status)" class="btn btn-ghost btn-sm" @click="reviewVisible=true">回访评价</button>
        </div>
      </div>

      <div class="section-label">现场记录</div>
      <div class="card">
        <div v-for="r in records" :key="r.id" class="timeline-row">
          <span>{{ dateTime(r.createdAt) }} · {{ r.operator }}</span>
          <p><strong>{{ r.action }}</strong> {{ r.content }}</p>
        </div>
        <div v-if="!records.length" class="muted" style="font-size:11px">暂无处理记录</div>
      </div>
    </div>

    <div v-if="dispatchVisible" class="modal-mask" @click.self="dispatchVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>派单</h3><button class="modal-close" @click="dispatchVisible=false">×</button></div>
        <div class="field"><label>技师</label><select v-model.number="dispatchTech"><option value="">请选择技师</option><option v-for="u in technicians" :key="u.id" :value="u.id">{{ u.name }}（{{ u.roleCode }}）</option></select></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dispatchVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="dispatch">确认派单</button></div>
      </div>
    </div>

    <div v-if="recordVisible" class="modal-mask" @click.self="recordVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>现场记录</h3><button class="modal-close" @click="recordVisible=false">×</button></div>
        <textarea v-model="recordContent" rows="6" style="width:100%;border:1px solid var(--line);border-radius:10px;padding:10px;font-size:13px" placeholder="现场检查、处理措施、耗材使用等情况" />
        <div class="form-actions"><button class="btn btn-ghost" @click="recordVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveRecord">保存记录</button></div>
      </div>
    </div>

    <div v-if="completeVisible" class="modal-mask" @click.self="completeVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>工单完工</h3><button class="modal-close" @click="completeVisible=false">×</button></div>
        <textarea v-model="completeContent" rows="5" style="width:100%;border:1px solid var(--line);border-radius:10px;padding:10px;font-size:13px" placeholder="处理结果与更换备件说明" />
        <div class="form-actions"><button class="btn btn-ghost" @click="completeVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="complete">确认完工</button></div>
      </div>
    </div>

    <div v-if="reviewVisible" class="modal-mask" @click.self="reviewVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>回访与满意度评价</h3><button class="modal-close" @click="reviewVisible=false">×</button></div>
        <div class="field"><label>满意度评分（1-5）</label><input v-model.number="reviewScore" type="number" min="1" max="5" /></div>
        <div class="field" style="margin-top:10px"><label>客户评价</label><textarea v-model="reviewComment" rows="3" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="reviewVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="review">保存评价</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { dateTime, label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter(); const id = Number(route.params.id)
const detail = ref<any>(null); const loading = ref(false)
const ticket = computed(() => detail.value?.ticket); const records = computed(() => detail.value?.records || [])
const technicians = ref<any[]>([])
const dispatchVisible = ref(false); const dispatchTech = ref<number>()
const recordVisible = ref(false); const recordContent = ref('')
const completeVisible = ref(false); const completeContent = ref('')
const reviewVisible = ref(false); const reviewScore = ref(5); const reviewComment = ref('')
async function load() {
  loading.value = true
  try {
    detail.value = await http.get(`/admin/tickets/${id}`)
    technicians.value = detail.value.technicians || []
    if (!technicians.value.length) {
      try { const u: any = await http.get('/admin/users', { params: { size: 200 } }); technicians.value = (u.records || []).filter((x: any) => x.roleCode !== 'CUSTOMER') } catch {}
    }
    reviewScore.value = detail.value.ticket.satisfactionScore || 5
    reviewComment.value = detail.value.ticket.review || ''
  } finally { loading.value = false }
}
async function dispatch() {
  if (!dispatchTech.value) return toast('请选择技师', 'error')
  await http.post(`/admin/tickets/${id}/dispatch`, { technicianId: dispatchTech.value }); toast('已派单', 'success'); dispatchVisible.value = false; load()
}
async function accept() { await http.post(`/admin/tickets/${id}/accept`); toast('已接单', 'success'); load() }
function openRecord() { recordContent.value = ''; recordVisible.value = true }
async function saveRecord() { await http.post(`/admin/tickets/${id}/records`, { action: '现场记录', content: recordContent.value }); toast('记录已保存', 'success'); recordVisible.value = false; load() }
async function complete() { await http.post(`/admin/tickets/${id}/complete`, { solution: completeContent.value }); toast('工单已完工，待回访', 'success'); completeVisible.value = false; load() }
async function review() { await http.post(`/admin/tickets/${id}/review`, { score: reviewScore.value, comment: reviewComment.value }); toast('满意度已保存', 'success'); reviewVisible.value = false; load() }
async function removeTicket() { if (!confirm(`确认删除工单「${ticket.value?.ticketNo}」？`)) return; try { await http.delete(`/admin/tickets/${id}`); toast('已删除', 'success'); router.replace('/admin/tickets') } catch {} }
onMounted(load)
</script>

<style scoped>
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-mid { font-size: 11px; color: var(--steel); margin-top: 5px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; margin-top: 10px; }
.info-grid div { background: #fff; padding: 9px 10px; }
.info-grid .full { grid-column: 1 / -1; }
.info-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.info-grid strong { font-size: 11px; }
.action-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.timeline-row { display: grid; grid-template-columns: 120px 1fr; gap: 8px; border-bottom: 1px dashed var(--line); padding: 7px 0; font-size: 11px; }
.timeline-row span { color: var(--steel-2); }
.timeline-row p { margin: 0; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
</style>
