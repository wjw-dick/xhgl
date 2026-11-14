<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>需求详情</h1><button class="link-btn danger" @click="removeDemand">删除</button></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="demand" class="page-body">
      <div class="card">
        <div class="row-top"><strong class="mono">{{ demand.demandNo }}</strong><span :class="['pill', statusPill(demand.status)]">{{ label(demand.status) }}</span></div>
        <div class="section-label" style="margin:10px 0 4px">{{ customer?.name || '未关联客户' }} · {{ demand.contactName }}</div>
        <div class="info-grid">
          <div><span>锅炉类型</span><strong>{{ label(demand.boilerType) }}</strong></div>
          <div><span>容量</span><strong>{{ demand.capacity || '—' }}</strong></div>
          <div><span>燃料</span><strong>{{ label(demand.fuelType) }}</strong></div>
          <div><span>压力</span><strong>{{ label(demand.pressureLevel) }}</strong></div>
          <div><span>地区</span><strong>{{ demand.region || '—' }}</strong></div>
          <div><span>负责人</span><strong>{{ demand.ownerName || '待认领' }}</strong></div>
          <div><span>预算</span><strong>{{ money(demand.budgetMin) }} ~ {{ money(demand.budgetMax) }}</strong></div>
          <div><span>期望时间</span><strong>{{ date(demand.expectTime) }}</strong></div>
        </div>
        <div class="detail-text">{{ demand.usageDesc }}<br />{{ demand.description }}</div>
        <div class="row-actions" style="justify-content:flex-start;flex-wrap:wrap;gap:8px;margin-top:12px">
          <button v-if="!demand.ownerId" class="btn btn-primary btn-sm" @click="claim">认领</button>
          <button class="btn btn-ghost btn-sm" @click="openEdit">编辑</button>
          <button class="btn btn-ghost btn-sm" @click="assignVisible=true">分配/转派</button>
          <router-link :to="`/admin/solutions?demandId=${demand.id}`" class="btn btn-ghost btn-sm">创建方案</router-link>
          <router-link :to="`/admin/quotes?demandId=${demand.id}`" class="btn btn-ghost btn-sm">创建报价</router-link>
        </div>
      </div>

      <div class="section-label">阶段推进</div>
      <div class="card">
        <div class="status-buttons">
          <button v-for="s in nextStatuses" :key="s.value" class="btn btn-ghost btn-sm" @click="changeStatus(s.value)">{{ s.label }}</button>
          <span v-if="!nextStatuses.length" class="muted" style="font-size:11px">当前阶段暂不可推进</span>
        </div>
      </div>

      <div class="section-label">跟进记录</div>
      <div class="card">
        <div v-for="f in followRecords" :key="f.id" class="timeline-row"><span>{{ dateTime(f.followTime) }}</span><p>{{ f.method ? f.method + ' · ' : '' }}{{ f.content }}</p></div>
        <div v-if="!followRecords.length" class="muted" style="font-size:11px">暂无跟进记录</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field"><label>跟进方式</label><select v-model="followForm.method"><option value="PHONE">电话</option><option value="WECHAT">微信</option><option value="VISIT">拜访</option><option value="EMAIL">邮件</option></select></div>
          <div class="field"><label>下次跟进</label><input v-model="followForm.nextTime" type="datetime-local" /></div>
          <div class="field full"><label>跟进内容</label><textarea v-model="followForm.content" rows="2" placeholder="本次沟通内容与客户反馈" /></div>
        </div>
        <button class="btn btn-primary btn-sm" style="margin-top:8px" @click="addFollow">保存跟进</button>
      </div>

      <div class="section-label">关联方案 / 报价</div>
      <div class="card" style="padding:4px 16px">
        <div v-for="s in solutions" :key="'s'+s.id" class="list-row"><div class="main"><strong>{{ s.name }}</strong><span>版本 {{ s.version }} · {{ label(s.status) }}</span></div></div>
        <router-link v-for="q in quotes" :key="'q'+q.id" :to="`/admin/quotes/${q.id}`" class="list-row"><div class="main"><strong class="mono">{{ q.quoteNo }}</strong><span>{{ money(q.totalAmount) }} · {{ label(q.status) }}</span></div><em>›</em></router-link>
        <div v-if="!solutions.length && !quotes.length" class="empty" style="padding:18px">暂无关联方案或报价</div>
      </div>
    </div>

    <div v-if="assignVisible" class="modal-mask" @click.self="assignVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>分配 / 转派需求</h3><button class="modal-close" @click="assignVisible=false">×</button></div>
        <div class="field"><label>负责人</label><select v-model.number="assignForm.ownerId"><option value="">请选择负责人</option><option v-for="u in owners" :key="u.id" :value="u.id">{{ u.name }}（{{ u.roleCode }}）</option></select></div>
        <div class="field" style="margin-top:10px"><label>分配说明</label><textarea v-model="assignForm.remark" rows="2" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="assignVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="assign">确认</button></div>
      </div>
    </div>

    <div v-if="editVisible" class="modal-mask" @click.self="editVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>编辑需求</h3><button class="modal-close" @click="editVisible=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>锅炉类型</label><select v-model="editForm.boilerType"><option value="steam">蒸汽锅炉</option><option value="hotwater">热水锅炉</option><option value="thermal">导热油炉</option><option value="wasteheat">余热锅炉</option></select></div>
          <div class="field"><label>燃料类型</label><select v-model="editForm.fuelType"><option value="gas">燃气</option><option value="oil">燃油</option><option value="electric">电</option><option value="biomass">生物质</option><option value="coal">燃煤</option></select></div>
          <div class="field"><label>容量</label><input v-model="editForm.capacity" /></div>
          <div class="field"><label>压力等级</label><select v-model="editForm.pressureLevel"><option value="atmospheric">常压</option><option value="low">低压</option><option value="medium">中压</option><option value="high">高压</option></select></div>
          <div class="field"><label>预算下限</label><input v-model.number="editForm.budgetMin" type="number" /></div>
          <div class="field"><label>预算上限</label><input v-model.number="editForm.budgetMax" type="number" /></div>
          <div class="field"><label>地区</label><input v-model="editForm.region" /></div>
          <div class="field"><label>期望时间</label><input v-model="editForm.expectTime" type="date" /></div>
          <div class="field full"><label>用途说明</label><input v-model="editForm.usageDesc" /></div>
          <div class="field full"><label>补充说明</label><textarea v-model="editForm.description" rows="3" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="editVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveEdit">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { date, dateTime, label, money, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter(); const id = Number(route.params.id)
const data = ref<any>(null); const loading = ref(false)
const demand = computed(() => data.value?.demand); const customer = computed(() => data.value?.customer)
const followRecords = computed(() => data.value?.followRecords || []); const solutions = computed(() => data.value?.solutions || []); const quotes = computed(() => data.value?.quotes || [])
const followForm = reactive<any>({ method: 'PHONE', content: '', nextTime: '' })
const editVisible = ref(false); const editForm = reactive<any>({})
const assignVisible = ref(false); const assignForm = reactive<any>({ ownerId: '', remark: '' }); const owners = ref<any[]>([])
const transitions: Record<string, { label: string; value: string }[]> = {
  NEW: [{ label: '推进入咨询', value: 'CONSULTING' }, { label: '标记流失', value: 'LOST' }],
  CONSULTING: [{ label: '推进入介绍', value: 'INTRODUCING' }, { label: '标记流失', value: 'LOST' }],
  INTRODUCING: [{ label: '推进入谈价', value: 'TALKING' }, { label: '标记流失', value: 'LOST' }],
  TALKING: [{ label: '确认成交', value: 'DEAL' }, { label: '标记流失', value: 'LOST' }],
  DEAL: [{ label: '退回答价', value: 'TALKING' }],
  LOST: [{ label: '重新激活', value: 'NEW' }]
}
const nextStatuses = computed(() => transitions[demand.value?.status] || [])
async function load() {
  loading.value = true
  try { data.value = await http.get(`/admin/demands/${id}`) } finally { loading.value = false }
  try {
    const users: any = await http.get('/admin/users', { params: { size: 200 } })
    owners.value = (users.records || []).filter((u: any) => u.roleCode !== 'CUSTOMER')
  } catch {}
}
async function claim() { await http.post(`/admin/demands/${id}/claim`); toast('认领成功', 'success'); load() }
async function addFollow() {
  if (!followForm.content) return toast('请填写跟进内容', 'error')
  await http.post(`/admin/demands/${id}/follow`, followForm)
  followForm.content = ''; toast('跟进已保存', 'success'); load()
}
async function changeStatus(status: string) {
  const remark = status === 'DEAL' || status === 'LOST' ? (prompt(status === 'DEAL' ? '请输入成交说明' : '请输入流失原因') || '') : ''
  await http.post(`/admin/demands/${id}/status`, { status, remark })
  toast('阶段已更新', 'success'); load()
}
function openEdit() {
  Object.keys(editForm).forEach((k) => delete editForm[k])
  Object.assign(editForm, demand.value, { status: undefined, ownerId: undefined, ownerName: undefined, followNextTime: undefined })
  editVisible.value = true
}
async function saveEdit() { await http.put(`/admin/demands/${id}`, editForm); toast('需求已更新', 'success'); editVisible.value = false; load() }
async function assign() {
  if (!assignForm.ownerId) return toast('请选择负责人', 'error')
  await http.post(`/admin/demands/${id}/assign`, assignForm); toast('已分配', 'success'); assignVisible.value = false; load()
}
async function removeDemand() {
  if (!confirm(`确认删除需求「${demand.value?.demandNo}」？`)) return
  try { await http.delete(`/admin/demands/${id}`); toast('已删除', 'success'); router.replace('/admin/demands') } catch {}
}
onMounted(load)
</script>

<style scoped>
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; margin-top: 10px; }
.info-grid div { background: #fff; padding: 9px 10px; }
.info-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.info-grid strong { font-size: 11px; }
.detail-text { font-size: 11px; color: var(--steel); line-height: 1.7; margin-top: 10px; }
.status-buttons { display: flex; flex-wrap: wrap; gap: 8px; }
.timeline-row { display: grid; grid-template-columns: 92px 1fr; gap: 8px; border-bottom: 1px dashed var(--line); padding: 7px 0; font-size: 11px; }
.timeline-row span { color: var(--steel-2); }
.timeline-row p { margin: 0; }
.list-row em { font-style: normal; color: var(--steel-2); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
</style>
