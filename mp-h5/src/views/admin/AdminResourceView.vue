<template>
  <div class="page">
    <div class="topbar">
      <button class="back-btn" @click="router.back()">‹</button>
      <h1>{{ config?.title || '资料管理' }}</h1>
      <button v-if="!readOnly" class="btn btn-primary btn-sm" @click="openCreate">＋ 新增</button>
    </div>
    <div class="page-body">
      <div v-if="!config" class="empty"><div class="big">?</div>模块不存在</div>
      <template v-else>
        <p class="muted" style="font-size:11px;line-height:1.7;margin:0 0 12px">{{ config.subtitle }}</p>
        <div class="search-row">
          <input v-model="keyword" class="search-input" :placeholder="config.keywordPlaceholder || '搜索'" @keyup.enter="search" />
          <button class="btn btn-ghost btn-sm" @click="search">搜索</button>
        </div>
        <div v-if="config.filters.length" class="filter-chips">
          <select v-for="f in config.filters" :key="f.prop" v-model="filterValues[f.prop]" class="chip-select" @change="search">
            <option value="">{{ f.label }}</option>
            <option v-for="o in f.options || []" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <div v-if="config.exportable || config.importable" class="toolbar-row">
          <button v-if="config.exportable" class="link-btn" @click="exportRows">导出 Excel</button>
          <button v-if="config.importable" class="link-btn" @click="pickImport">导入 Excel</button>
          <input ref="importInput" type="file" accept=".xlsx,.xls" hidden @change="importRows" />
        </div>

        <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
        <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
          <button v-for="row in rows" :key="row.id" class="resource-row" @click="openEdit(row)">
            <div class="resource-head">
              <strong>{{ displayValue(row, config.columns[0]) }}</strong>
              <span v-if="hasStatus" :class="['pill', statusPill(row.status)]">{{ label(row.status) }}</span>
            </div>
            <div class="resource-meta">
              <span v-for="c in config.columns.slice(1, 4)" :key="c.prop">{{ c.label }}：{{ displayValue(row, c) }}</span>
            </div>
          </button>
        </div>
        <div v-else class="empty"><div class="big">▤</div>暂无数据</div>

        <div v-if="total > size" class="pager-row">
          <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
          <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
          <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
        </div>
      </template>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog = false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ editing ? '编辑' : '新增' }}{{ config?.title }}</h3><button class="modal-close" @click="dialog = false">×</button></div>
        <div class="form-grid">
          <div v-for="f in config?.fields || []" :key="f.prop" class="field" :class="{ full: f.full }">
            <label>{{ f.label }}<span v-if="f.required" style="color:#b33426"> *</span></label>
            <select v-if="f.type === 'select'" v-model="form[f.prop]">
              <option value="">请选择</option>
              <option v-for="o in f.options || []" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
            <textarea v-else-if="f.type === 'textarea' || f.type === 'richtext'" v-model="form[f.prop]" :rows="f.rows || 4" :placeholder="f.placeholder" />
            <AdminUploader v-else-if="f.type === 'images' || f.type === 'file'" v-model="form[f.prop]" :image="f.type === 'images'" :category="f.category" />
            <input v-else-if="f.type === 'number'" v-model.number="form[f.prop]" type="number" :placeholder="f.placeholder" />
            <input v-else-if="f.type === 'date'" v-model="form[f.prop]" type="date" />
            <input v-else-if="f.type === 'datetime'" v-model="form[f.prop]" type="datetime-local" />
            <input v-else v-model="form[f.prop]" :placeholder="f.placeholder" />
          </div>
        </div>
        <div class="form-actions">
          <button v-if="editing && !readOnly" class="btn btn-ghost" style="color:#b33426" @click="remove">删除</button>
          <button class="btn btn-primary" style="flex:1" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import AdminUploader from '@/components/AdminUploader.vue'
import { adminResourceConfigs, type AdminResourceColumn } from '@/admin/resourceConfigs'
import { date, dateTime, label, money, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter()
const config = computed(() => adminResourceConfigs[route.params.key as string])
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const filterValues = reactive<Record<string, any>>({})
const loading = ref(false); const saving = ref(false); const dialog = ref(false); const editing = ref<any>(null)
const form = reactive<Record<string, any>>({}); const importInput = ref<HTMLInputElement>()
const readOnly = computed(() => false)
const hasStatus = computed(() => (config.value?.columns || []).some((c) => c.prop === 'status'))
function requestParams() {
  const params: Record<string, any> = { page: page.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  for (const [k, v] of Object.entries(filterValues)) if (v !== '' && v !== undefined && v !== null) params[k] = v
  return params
}
async function load() {
  if (!config.value) return
  loading.value = true
  try {
    const data: any = await http.get(`/admin/${config.value.api}`, { params: requestParams() })
    rows.value = data.records || data || []
    total.value = Number(data.total ?? rows.value.length)
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function displayValue(row: any, column: AdminResourceColumn) {
  const value = row[column.prop]
  if (value === null || value === undefined || value === '') return '—'
  if (column.format === 'money') return money(value)
  if (column.format === 'date') return date(value)
  if (column.format === 'datetime') return dateTime(value)
  if (column.format === 'status') return label(String(value))
  return String(value)
}
function openCreate() {
  editing.value = null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, config.value?.defaults || {})
  dialog.value = true
}
function openEdit(row: any) {
  editing.value = row
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialog.value = true
}
async function save() {
  if (!config.value) return
  for (const f of config.value.fields) {
    if (f.required && (form[f.prop] === undefined || form[f.prop] === null || form[f.prop] === '')) {
      return toast(`请填写${f.label}`, 'error')
    }
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (editing.value) await http.put(`/admin/${config.value.api}/${editing.value.id}`, payload)
    else await http.post(`/admin/${config.value.api}`, payload)
    toast(editing.value ? '已保存' : '已新增', 'success')
    dialog.value = false
    load()
  } finally { saving.value = false }
}
async function remove() {
  if (!config.value || !editing.value) return
  if (!confirm(`确认删除该${config.value.title}记录？`)) return
  await http.delete(`/admin/${config.value.api}/${editing.value.id}`)
  toast('已删除', 'success'); dialog.value = false; load()
}
function exportRows() {
  if (!config.value) return
  downloadFile(`/admin/${config.value.api}/export?${new URLSearchParams(requestParams() as any).toString()}`, `${config.value.title}.xlsx`)
}
function pickImport() { importInput.value?.click() }
async function importRows(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !config.value) return
  const data = new FormData(); data.append('file', file)
  try {
    const result: any = await http.post(`/admin/${config.value.api}/import`, data, { headers: { 'Content-Type': 'multipart/form-data' } })
    toast(`导入完成：成功 ${result.success ?? 0} 条`, 'success'); load()
  } finally { target.value = '' }
}
onMounted(load)
watch(() => route.params.key, () => {
  page.value = 1
  keyword.value = ''
  Object.keys(filterValues).forEach((k) => delete filterValues[k])
  dialog.value = false
  load()
})
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.chip-select { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 9px; font-size: 11px; color: var(--steel); }
.toolbar-row { display: flex; gap: 14px; justify-content: flex-end; padding: 8px 0 0; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.resource-row { display: block; width: 100%; text-align: left; border: none; border-bottom: 1px solid #f2f4f7; background: transparent; padding: 12px 0; }
.resource-row:last-child { border-bottom: none; }
.resource-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.resource-head strong { font-size: 13px; }
.resource-meta { display: flex; flex-wrap: wrap; gap: 4px 12px; color: var(--steel); font-size: 10px; margin-top: 5px; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
