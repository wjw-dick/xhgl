<template>
  <div class="page">
    <div class="topbar"><h1>售后服务</h1><button class="btn btn-primary btn-sm" @click="openReport">＋ 在线报修</button></div>
    <div class="page-body">
      <div class="grid-2">
        <div class="stat-card"><span>售后工单</span><strong>{{ tickets.length }}</strong><em>{{ pending }} 个处理中</em></div>
        <div class="stat-card"><span>维保/年检</span><strong>{{ maintenance.length }}</strong><em>{{ dueMaintenance }} 个 30 天内到期</em></div>
      </div>
      <div class="section-label">我的工单</div>
      <div v-if="tickets.length" class="card" style="padding:4px 16px">
        <button v-for="t in tickets" :key="t.id" class="ticket-row" @click="openTicket(t)">
          <div class="row-top"><strong class="mono">{{ t.ticketNo }}</strong><span :class="['pill', statusPill(t.status)]">{{ label(t.status) }}</span></div>
          <div class="row-mid">{{ label(t.type) }} · {{ t.equipmentNo || '未关联设备' }}</div>
          <div class="row-bottom"><span>{{ t.description }}</span><span>{{ t.technicianName || '待派单' }}</span></div>
        </button>
      </div>
      <div v-else class="empty"><div class="big">✚</div><div>暂无工单</div><div class="empty-hint">如需报修，请点击右上角「在线报修」</div></div>
      <div class="section-label">维保与年检</div>
      <div class="card">
        <div v-for="m in maintenance" :key="m.id" class="list-row">
          <div class="main"><strong>{{ m.equipmentNo }} · {{ label(m.project) }}</strong><span>下次到期 {{ date(m.nextDate) }} · 周期 {{ m.cycleMonths }} 个月</span></div>
          <span :class="['pill', daysLeft(m.nextDate) < 0 ? 'red' : daysLeft(m.nextDate) <= 30 ? 'yellow' : 'green']">{{ daysLeft(m.nextDate) < 0 ? '已逾期' : '剩余' + daysLeft(m.nextDate) + '天' }}</span>
        </div>
        <div v-if="!maintenance.length" class="empty">设备验收后自动生成维保计划</div>
      </div>
      <div class="notice blue" style="margin-top:14px">按 TSG 11 要求完成年度外部检验与定期内部检验；维保/定检到期平台自动提醒。</div>
    </div>

    <div v-if="reportVisible" class="modal-mask" @click.self="reportVisible = false">
      <div class="modal-card">
        <div class="modal-head"><h3>在线报修</h3><button class="modal-close" @click="reportVisible = false">×</button></div>
        <div class="field"><label>设备</label><select v-model="form.equipmentId"><option :value="undefined">未选择设备</option><option v-for="e in equipment" :key="e.id" :value="e.id">{{ e.equipmentNo }} · {{ e.model }}</option></select></div>
        <div class="form-grid" style="margin-top:10px">
          <div class="field"><label>类型</label><select v-model="form.type"><option value="REPAIR">报修</option><option value="MAINTAIN">保养</option><option value="INSPECT">年检</option></select></div>
          <div class="field"><label>优先级</label><select v-model="form.priority"><option value="NORMAL">普通</option><option value="HIGH">紧急</option></select></div>
          <div class="field full"><label>故障描述 *</label><textarea v-model="form.description" placeholder="现象、报警代码、停机情况" /></div>
          <div class="field full"><label>联系人</label><input v-model="form.contactName" /></div>
          <div class="field full">
            <label>现场照片</label>
            <label class="photo-pick">
              <input type="file" accept="image/*" multiple @change="uploadPhotos" />
              <span>{{ uploading ? '上传中…' : '＋ 添加照片' }}</span>
            </label>
            <div v-if="photos.length" class="photo-list">
              <div v-for="(p, i) in photos" :key="p" class="photo-item">
                <img :src="p" alt="报修照片" />
                <button type="button" aria-label="移除照片" @click="removePhoto(i)">×</button>
              </div>
            </div>
          </div>
        </div>
        <button class="btn btn-primary btn-block" style="margin-top:14px" :disabled="submitting || uploading" @click="submitReport">{{ submitting ? '提交中…' : '提交报修' }}</button>
      </div>
    </div>

    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ detail.ticket.ticketNo }}</h3><button class="modal-close" @click="detail = null">×</button></div>
        <div class="detail-grid">
          <div><span>状态</span><strong>{{ label(detail.ticket.status) }}</strong></div>
          <div><span>技师</span><strong>{{ detail.ticket.technicianName || '待派单' }}</strong></div>
          <div><span>类型</span><strong>{{ label(detail.ticket.type) }}</strong></div>
          <div><span>设备</span><strong>{{ detail.ticket.equipmentNo || '—' }}</strong></div>
        </div>
        <p style="font-size:12px;margin:12px 0">{{ detail.ticket.description }}</p>
        <div class="section-label">处理记录</div>
        <div v-for="r in detail.records" :key="r.id" class="timeline-row"><span>{{ dateTime(r.createdAt) }}</span><p>{{ r.action }}：{{ r.content }}</p></div>
        <div v-if="['WAIT_REVIEW','COMPLETED'].includes(detail.ticket.status)" style="margin-top:12px">
          <div class="section-label">服务满意度</div>
          <div class="stars"><button v-for="i in 5" :key="i" :class="{ active: score >= i }" @click="score = i">★</button></div>
          <textarea v-model="reviewComment" rows="2" placeholder="对本次服务的评价" style="width:100%;border:1px solid var(--line);border-radius:8px;padding:8px;margin:8px 0" />
          <button class="btn btn-primary btn-sm" @click="review">提交评价</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, statusPill, date, dateTime, daysLeft } from '@/utils'

const tickets = ref<any[]>([]); const maintenance = ref<any[]>([]); const equipment = ref<any[]>([])
const reportVisible = ref(false); const submitting = ref(false); const detail = ref<any>(null)
const form = reactive<any>({ type: 'REPAIR', priority: 'NORMAL', description: '', contactName: '', equipmentId: undefined, photos: '' })
const photos = ref<string[]>([]); const uploading = ref(false)
const score = ref(5); const reviewComment = ref('')
const pending = computed(() => tickets.value.filter((t) => ['ACCEPTED', 'DISPATCHED', 'PROCESSING'].includes(t.status)).length)
const dueMaintenance = computed(() => maintenance.value.filter((m) => daysLeft(m.nextDate) <= 30).length)
async function load() {
  tickets.value = await http.get('/customer/tickets')
  maintenance.value = await http.get('/customer/maintenance')
  equipment.value = await http.get('/customer/equipment')
}
function openReport() { Object.assign(form, { type: 'REPAIR', priority: 'NORMAL', description: '', contactName: '', equipmentId: undefined, photos: '' }); photos.value = []; reportVisible.value = true }
async function uploadPhotos(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  uploading.value = true
  for (const file of files) {
    const fd = new FormData(); fd.append('file', file); fd.append('category', 'ticket')
    try {
      const record: any = await http.post('/customer/files/upload', fd)
      if (record?.url) photos.value.push(record.url)
    } catch {}
  }
  form.photos = photos.value.join(',')
  uploading.value = false
  input.value = ''
}
function removePhoto(index: number) { photos.value.splice(index, 1); form.photos = photos.value.join(',') }
async function submitReport() {
  if (!form.description) return toast('请填写故障描述', 'error')
  submitting.value = true
  try {
    await http.post('/customer/tickets', form); toast('报修已提交', 'success'); reportVisible.value = false; load()
  } finally { submitting.value = false }
}
async function openTicket(t: any) { detail.value = await http.get(`/customer/tickets/${t.id}`); score.value = t.satisfactionScore || 5; reviewComment.value = t.review || '' }
async function review() {
  await http.post(`/customer/tickets/${detail.value.ticket.id}/review`, { score: score.value, comment: reviewComment.value })
  toast('评价已提交', 'success'); detail.value = null; load()
}
onMounted(load)
</script>
<style scoped>
.ticket-row { display: block; width: 100%; border: none; background: transparent; text-align: left; padding: 12px 0; border-bottom: 1px solid #f2f4f7; }
.empty-hint { margin-top: 6px; font-size: 10px; color: var(--steel-2); }
.ticket-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; font-size: 10px; }
.row-mid { font-size: 12px; margin: 5px 0 3px; }
.row-bottom { display: flex; justify-content: space-between; gap: 10px; font-size: 10px; color: var(--steel-2); }
.row-bottom span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 75%; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
.detail-grid div { background: #fff; padding: 9px 10px; }
.detail-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.detail-grid strong { font-size: 12px; }
.timeline-row { display: grid; grid-template-columns: 90px 1fr; gap: 8px; border-bottom: 1px dashed var(--line); padding: 7px 0; font-size: 11px; }
.timeline-row span { color: var(--steel-2); }
.timeline-row p { margin: 0; }
.stars button { border: none; background: transparent; font-size: 22px; color: #d8d2c8; padding: 0 2px; }
.stars button.active { color: var(--accent); }
.photo-pick { display: inline-flex; align-items: center; justify-content: center; margin-top: 4px; padding: 7px 14px; border: 1px dashed var(--line); border-radius: 8px; font-size: 12px; color: var(--steel); cursor: pointer; }
.photo-pick input { display: none; }
.photo-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.photo-item { position: relative; width: 62px; height: 62px; }
.photo-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; border: 1px solid var(--line); }
.photo-item button { position: absolute; top: -6px; right: -6px; width: 18px; height: 18px; border: none; border-radius: 50%; background: rgba(0, 0, 0, .65); color: #fff; font-size: 12px; line-height: 18px; padding: 0; }
</style>
