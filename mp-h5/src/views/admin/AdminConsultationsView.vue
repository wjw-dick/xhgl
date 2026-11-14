<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>售前咨询</h1><button class="btn btn-primary btn-sm" @click="createVisible=true">＋ 录入</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="问题 / 答复 / 客户" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; search()">{{ s.label }}</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="c in rows" :key="c.id" class="consult-row">
          <div class="row-top"><strong>{{ c.customerName || '客户' }}</strong><span :class="['pill', statusPill(c.status)]">{{ label(c.status) }}</span></div>
          <p class="question">{{ c.question }}</p>
          <p v-if="c.answer" class="answer">答复：{{ c.answer }}</p>
          <div class="row-bottom"><span>{{ c.channel }} · {{ dateTime(c.createdAt) }}</span><span>{{ c.consultantName || '—' }}</span></div>
          <div class="row-actions">
            <button class="link-btn" @click="openReply(c)">{{ c.status === 'WAITING' ? '回复' : '修改答复' }}</button>
            <button v-if="c.status !== 'CONVERTED'" class="link-btn" @click="openConvert(c)">转需求</button>
            <button class="link-btn danger" @click="remove(c)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">询</div>暂无咨询</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="replyVisible" class="modal-mask" @click.self="replyVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>回复咨询</h3><button class="modal-close" @click="replyVisible=false">×</button></div>
        <div class="notice">{{ current?.question }}</div>
        <textarea v-model="answer" rows="6" style="width:100%;margin-top:12px;border:1px solid var(--line);border-radius:10px;padding:10px;font-size:13px" placeholder="请输入答复内容" />
        <div class="form-actions"><button class="btn btn-ghost" @click="replyVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="reply">发送答复</button></div>
      </div>
    </div>

    <div v-if="createVisible" class="modal-mask" @click.self="createVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>录入售前咨询</h3><button class="modal-close" @click="createVisible=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>客户</label><select v-model.number="createForm.customerId"><option value="">请选择客户</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option></select></div>
          <div class="field"><label>渠道</label><select v-model="createForm.channel"><option value="ONLINE">在线</option><option value="PHONE">电话</option><option value="STORE">到店</option></select></div>
          <div class="field"><label>状态</label><select v-model="createForm.status"><option value="WAITING">待回复</option><option value="DONE">已完成</option></select></div>
          <div class="field full"><label>咨询问题</label><textarea v-model="createForm.question" rows="3" /></div>
          <div class="field full"><label>答复</label><textarea v-model="createForm.answer" rows="3" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="createVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveCreate">保存</button></div>
      </div>
    </div>

    <div v-if="convertVisible" class="modal-mask" @click.self="convertVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>咨询转采购需求</h3><button class="modal-close" @click="convertVisible=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>锅炉类型</label><select v-model="demandForm.boilerType"><option value="steam">蒸汽锅炉</option><option value="hotwater">热水锅炉</option><option value="thermal">导热油炉</option></select></div>
          <div class="field"><label>燃料类型</label><select v-model="demandForm.fuelType"><option value="gas">燃气</option><option value="oil">燃油</option><option value="electric">电</option><option value="biomass">生物质</option></select></div>
          <div class="field"><label>容量</label><input v-model="demandForm.capacity" placeholder="如 6 t/h" /></div>
          <div class="field"><label>压力等级</label><select v-model="demandForm.pressureLevel"><option value="low">低压</option><option value="medium">中压</option><option value="high">高压</option></select></div>
          <div class="field"><label>预算下限</label><input v-model.number="demandForm.budgetMin" type="number" /></div>
          <div class="field"><label>预算上限</label><input v-model.number="demandForm.budgetMax" type="number" /></div>
          <div class="field"><label>地区</label><input v-model="demandForm.region" /></div>
          <div class="field full"><label>用途说明</label><input v-model="demandForm.usageDesc" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="convertVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="convert">转需求</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { dateTime, label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false); const customers = ref<any[]>([])
const replyVisible = ref(false); const current = ref<any>(null); const answer = ref('')
const createVisible = ref(false); const createForm = reactive<any>({ channel: 'ONLINE', status: 'WAITING' })
const convertVisible = ref(false); const demandForm = reactive<any>({ boilerType: 'steam', fuelType: 'gas', pressureLevel: 'low' })
const statuses = [{ label: '全部', value: '' }, { label: '待回复', value: 'WAITING' }, { label: '已回复', value: 'DONE' }, { label: '已转需求', value: 'CONVERTED' }]
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/consultations', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function loadCustomers() { try { customers.value = await http.get('/admin/customers/options') } catch {} }
function search() { page.value = 1; load() }
function openReply(row: any) { current.value = row; answer.value = row.answer || ''; replyVisible.value = true }
async function reply() {
  if (!answer.value) return toast('请输入答复内容', 'error')
  await http.post(`/admin/consultations/${current.value.id}/reply`, { answer: answer.value })
  toast('答复已发送', 'success'); replyVisible.value = false; load()
}
function openConvert(row: any) {
  current.value = row
  Object.keys(demandForm).forEach((k) => delete demandForm[k])
  Object.assign(demandForm, { boilerType: 'steam', fuelType: 'gas', pressureLevel: 'low' })
  convertVisible.value = true
}
async function convert() { await http.post(`/admin/consultations/${current.value.id}/to-demand`, demandForm); toast('已转为需求', 'success'); convertVisible.value = false; load() }
async function saveCreate() {
  if (!createForm.question) return toast('请输入咨询问题', 'error')
  await http.post('/admin/consultations', createForm); toast('咨询已录入', 'success'); createVisible.value = false; load()
}
async function remove(row: any) {
  if (!confirm(`确认删除咨询「${row.question}」？`)) return
  await http.delete(`/admin/consultations/${row.id}`); toast('已删除', 'success'); load()
}
onMounted(() => { load(); loadCustomers() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.consult-row { border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.consult-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 13px; }
.question { font-size: 12px; margin: 6px 0 0; }
.answer { font-size: 11px; color: var(--steel); margin: 4px 0 0; line-height: 1.6; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 4px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 4px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
