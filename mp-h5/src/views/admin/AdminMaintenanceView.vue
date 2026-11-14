<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>维保计划</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新增</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="设备编号 / 客户" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button :class="{ active: project === '' }" @click="project = ''; search()">全部</button>
        <button :class="{ active: project === 'MAINTAIN' }" @click="project = 'MAINTAIN'; search()">定期保养</button>
        <button :class="{ active: project === 'ANNUAL' }" @click="project = 'ANNUAL'; search()">年检</button>
        <button :class="{ active: project === 'PERIODIC' }" @click="project = 'PERIODIC'; search()">定检</button>
      </div>
      <div v-if="warnings.length" class="notice" style="margin-top:10px">有 {{ warnings.length }} 项维保即将到期，请及时安排。</div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="m in rows" :key="m.id" class="maintenance-row">
          <div class="row-top"><strong>{{ m.equipmentNo || '未关联设备' }}</strong><span :class="['pill', statusPill(m.status)]">{{ label(m.status) }}</span></div>
          <div class="row-mid">{{ m.customerName || '—' }} · {{ label(m.project) }} · 周期 {{ m.cycleMonths || 12 }} 个月</div>
          <div class="row-bottom"><span>下次到期 {{ date(m.nextDate) }}</span><span>剩余 {{ daysLeft(m.nextDate) }} 天</span></div>
          <div class="row-actions">
            <button class="link-btn" @click="complete(m)">登记完成</button>
            <button class="link-btn" @click="openEdit(m)">编辑</button>
            <button class="link-btn" @click="pdf(m)">报告</button>
            <button class="link-btn danger" @click="remove(m)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">保</div>暂无维保计划</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ editing ? '编辑维保计划' : '新增维保计划' }}</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>设备 ID</label><input v-model.number="form.equipmentId" type="number" /></div>
          <div class="field"><label>设备编号</label><input v-model="form.equipmentNo" /></div>
          <div class="field"><label>客户 ID</label><input v-model.number="form.customerId" type="number" /></div>
          <div class="field"><label>客户名称</label><input v-model="form.customerName" /></div>
          <div class="field"><label>维保项目</label><select v-model="form.project"><option value="MAINTAIN">定期保养</option><option value="ANNUAL">年检</option><option value="PERIODIC">定检</option></select></div>
          <div class="field"><label>周期（月）</label><input v-model.number="form.cycleMonths" type="number" /></div>
          <div class="field"><label>下次到期日</label><input v-model="form.nextDate" type="date" /></div>
          <div class="field"><label>提醒天数</label><input v-model.number="form.remindDays" type="number" /></div>
          <div class="field full"><label>备注</label><textarea v-model="form.remark" rows="2" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import { date, daysLeft, label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const project = ref(''); const loading = ref(false); const warnings = ref<any[]>([])
const dialog = ref(false); const editing = ref<any>(null)
const form = reactive<any>({ project: 'MAINTAIN', cycleMonths: 6, remindDays: 30, status: 'ACTIVE' })
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/maintenance', { params: { page: page.value, size: size.value, keyword: keyword.value, project: project.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function loadWarnings() { try { warnings.value = await http.get('/admin/maintenance/warnings') } catch { warnings.value = [] } }
function search() { page.value = 1; load() }
function openCreate() { editing.value = null; Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { project: 'MAINTAIN', cycleMonths: 6, remindDays: 30, status: 'ACTIVE' }); dialog.value = true }
function openEdit(row: any) { editing.value = row; Object.assign(form, row); dialog.value = true }
async function save() {
  if (editing.value) await http.put(`/admin/maintenance/${form.id}`, form)
  else await http.post('/admin/maintenance', form)
  toast('维保计划已保存', 'success'); dialog.value = false; load(); loadWarnings()
}
async function complete(row: any) { if (!confirm(`确认登记 ${row.equipmentNo} ${label(row.project)} 已完成？`)) return; await http.post(`/admin/maintenance/${row.id}/complete`, {}); toast('已登记完成', 'success'); load(); loadWarnings() }
async function remove(row: any) { if (!confirm('确认删除该维保计划？')) return; await http.delete(`/admin/maintenance/${row.id}`); toast('已删除', 'success'); load(); loadWarnings() }
function pdf(row: any) { downloadFile(`/admin/maintenance/${row.id}/pdf`, `${row.equipmentNo || '维保'}-维保报告.pdf`) }
onMounted(() => { load(); loadWarnings() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.maintenance-row { border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.maintenance-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 11px; color: #344054; margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 4px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
