<template>
  <div class="page">
    <div class="topbar"><h1>客户档案</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新增</button></div>
    <div class="page-body">
      <div class="search-row">
        <input v-model="keyword" class="search-input" placeholder="企业名称 / 客户编号 / 地址" @keyup.enter="search" />
        <button class="btn btn-ghost btn-sm" @click="search">搜索</button>
      </div>
      <div class="filter-chips">
        <button :class="{ active: status === '' }" @click="status = ''; search()">全部</button>
        <button :class="{ active: status === 'NORMAL' }" @click="status = 'NORMAL'; search()">正常</button>
        <button :class="{ active: status === 'DISABLED' }" @click="status = 'DISABLED'; search()">停用</button>
        <button class="link-btn" style="margin-left:auto" @click="exportRows">导出</button>
        <button class="link-btn" @click="pickImport">导入</button>
        <input ref="importInput" type="file" accept=".xlsx,.xls" hidden @change="importRows" />
      </div>

      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="row in rows" :key="row.id" class="customer-row">
          <button class="customer-main" @click="router.push(`/admin/customers/${row.id}`)">
            <div class="row-top"><strong>{{ row.name }}</strong><span :class="['pill', statusPill(row.status)]">{{ label(row.status) }}</span></div>
            <div class="row-mid">{{ row.industry || '未填行业' }} · {{ row.region || '未填地区' }} · 级别 {{ row.level || '—' }}</div>
            <div class="row-bottom"><span class="mono">{{ row.customerNo }}</span><span>{{ row.ownerName || '待认领' }}</span></div>
          </button>
          <div class="row-actions">
            <button class="link-btn" @click="router.push(`/admin/customers/${row.id}`)">关联视图</button>
            <button class="link-btn" @click="openEdit(row)">编辑</button>
            <button v-if="row.status !== 'DISABLED'" class="link-btn" @click="disable(row)">禁用</button>
            <button class="link-btn danger" @click="remove(row)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">▤</div>暂无客户</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog = false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ editing ? '编辑客户' : '新增客户' }}</h3><button class="modal-close" @click="dialog = false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>企业名称 *</label><input v-model="form.name" placeholder="请输入企业/单位名称" /></div>
          <div class="field"><label>所属行业</label><input v-model="form.industry" /></div>
          <div class="field"><label>企业规模</label><select v-model="form.scale"><option value="">请选择</option><option>大型</option><option>中型</option><option>小型</option></select></div>
          <div class="field"><label>地区</label><input v-model="form.region" /></div>
          <div class="field"><label>客户来源</label><select v-model="form.source"><option value="H5">H5门户</option><option value="MP">小程序</option><option value="OA">公众号</option><option value="PHONE">电话</option><option value="MANUAL">人工录入</option></select></div>
          <div class="field"><label>客户分类</label><input v-model="form.category" placeholder="重点客户 / 潜力客户" /></div>
          <div class="field"><label>客户级别</label><select v-model="form.level"><option value="A">A</option><option value="B">B</option><option value="C">C</option></select></div>
          <div class="field"><label>负责人 ID</label><input v-model.number="form.ownerId" type="number" /></div>
          <div class="field full"><label>详细地址</label><input v-model="form.address" /></div>
          <div class="field full"><label>备注</label><textarea v-model="form.remark" rows="3" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import { label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false)
const dialog = ref(false); const editing = ref<any>(null); const saving = ref(false)
const form = reactive<any>({ source: 'MANUAL', level: 'C', status: 'NORMAL' })
const importInput = ref<HTMLInputElement>()
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/customers', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } })
    rows.value = data.records || []; total.value = data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function openCreate() {
  editing.value = null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { source: 'MANUAL', level: 'C', status: 'NORMAL' })
  dialog.value = true
}
function openEdit(row: any) {
  editing.value = row
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialog.value = true
}
async function save() {
  if (!form.name) return toast('请输入企业名称', 'error')
  saving.value = true
  try {
    if (editing.value) await http.put(`/admin/customers/${editing.value.id}`, form)
    else await http.post('/admin/customers', form)
    toast('客户档案已保存', 'success'); dialog.value = false; load()
  } finally { saving.value = false }
}
async function disable(row: any) {
  if (!confirm(`确认禁用客户「${row.name}」？`)) return
  await http.post(`/admin/customers/${row.id}/disable`)
  toast('已禁用', 'success'); load()
}
async function remove(row: any) {
  if (!confirm(`确认删除客户「${row.name}」？删除后不可恢复。已有业务数据的客户不可删除。`)) return
  try { await http.delete(`/admin/customers/${row.id}`); toast('已删除', 'success'); load() } catch {}
}
function exportRows() { downloadFile('/admin/customers/export', '客户档案.xlsx') }
function pickImport() { importInput.value?.click() }
async function importRows(event: Event) {
  const target = event.target as HTMLInputElement; const file = target.files?.[0]
  if (!file) return
  const fd = new FormData(); fd.append('file', file)
  try {
    const result: any = await http.post('/admin/customers/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    toast(`导入完成：成功 ${result.success ?? 0} 条`, 'success'); load()
  } finally { target.value = '' }
}
onMounted(load)
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; align-items: center; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.customer-row { border-bottom: 1px solid #f2f4f7; padding: 6px 0 8px; }
.customer-row:last-child { border-bottom: none; }
.customer-main { display: block; width: 100%; text-align: left; border: none; background: transparent; padding: 6px 0; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 13px; }
.row-mid { font-size: 11px; color: #344054; margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 3px; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
