<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>方案与案例</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 创建</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="方案编号 / 名称 / 产品" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; search()">{{ s.label }}</button>
        <button class="link-btn" style="margin-left:auto" @click="router.push('/admin/resource/cases')">案例库 ›</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="s in rows" :key="s.id" class="solution-row">
          <div class="row-top"><strong>{{ s.name }}</strong><span :class="['pill', statusPill(s.status)]">{{ label(s.status) }}</span></div>
          <div class="row-mid"><span class="mono">{{ s.solutionNo }}</span> · 版本 {{ s.version }}</div>
          <div class="row-bottom"><span>{{ s.productName || '未选型' }}</span><span>查看 {{ s.viewCount || 0 }}</span></div>
          <div class="row-actions">
            <button class="link-btn" @click="openView(s)">查看</button>
            <button class="link-btn" @click="openEdit(s)">编辑</button>
            <button v-if="s.status === 'DRAFT'" class="link-btn" @click="send(s)">推送客户</button>
            <button class="link-btn" @click="newVersion(s)">新版本</button>
            <button class="link-btn danger" @click="remove(s)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">案</div>暂无方案</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ editing ? '编辑方案' : '创建方案' }}</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>关联需求 *</label><select v-model.number="form.demandId" :disabled="!!editing"><option value="">请选择需求</option><option v-for="d in demands" :key="d.id" :value="d.id">{{ d.demandNo }} · {{ d.contactName }} · {{ d.capacity }}</option></select></div>
          <div class="field full"><label>选型产品</label><select v-model.number="form.productId" @change="onProduct"><option value="">请选择产品</option><option v-for="p in products" :key="p.id" :value="p.id">{{ p.model }} · {{ p.capacity }}</option></select></div>
          <div class="field full"><label>方案名称 *</label><input v-model="form.name" /></div>
          <div class="field full"><label>方案正文</label><textarea v-model="form.content" rows="6" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存</button></div>
      </div>
    </div>

    <div v-if="view" class="modal-mask" @click.self="view=null">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ view.solution?.name }}</h3><button class="modal-close" @click="view=null">×</button></div>
        <div class="muted mono" style="font-size:10px">{{ view.solution?.solutionNo }} · 版本 {{ view.solution?.version }}</div>
        <div class="rich-content" v-html="sanitizeHtml(view.solution?.content)"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { label, statusPill, sanitizeHtml } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false)
const dialog = ref(false); const editing = ref<any>(null); const view = ref<any>(null)
const products = ref<any[]>([]); const demands = ref<any[]>([]); const demandId = ref<number | null>(route.query.demandId ? Number(route.query.demandId) : null)
const form = reactive<any>({ content: '<h2>项目概述</h2><p></p><h2>选型依据</h2><p></p>' })
const statuses = [{ label: '全部', value: '' }, { label: '草稿', value: 'DRAFT' }, { label: '已推送', value: 'PUSHED' }, { label: '已确认', value: 'CONFIRMED' }]
async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value, keyword: keyword.value, status: status.value }
    if (demandId.value) params.demandId = demandId.value
    const d: any = await http.get('/admin/solutions', { params })
    rows.value = d.records || []; total.value = d.total || 0
  } finally { loading.value = false }
}
async function loadOptions() {
  try { const d: any = await http.get('/admin/products', { params: { size: 200 } }); products.value = d.records || [] } catch {}
  try { const d: any = await http.get('/admin/demands', { params: { size: 200 } }); demands.value = d.records || [] } catch {}
}
function search() { page.value = 1; load() }
function openCreate() {
  editing.value = null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { demandId: demandId.value || undefined, content: '<h2>项目概述</h2><p></p><h2>选型依据</h2><p></p>' })
  dialog.value = true
}
function openEdit(row: any) { Object.assign(form, row); editing.value = row; dialog.value = true }
function onProduct(event: Event) {
  const id = Number((event.target as HTMLSelectElement).value)
  const p = products.value.find((x) => x.id === id)
  if (p && !form.name) form.name = `${p.model} 锅炉方案`
}
async function save() {
  if (!form.demandId || !form.name) return toast('关联需求与方案名称必填', 'error')
  if (editing.value) await http.put(`/admin/solutions/${editing.value.id}`, form)
  else await http.post('/admin/solutions', form)
  toast('方案已保存', 'success'); dialog.value = false; load()
}
async function openView(row: any) { view.value = await http.get(`/admin/solutions/${row.id}`) }
async function send(row: any) { if (!confirm('推送后客户可在门户查看，确认推送？')) return; await http.post(`/admin/solutions/${row.id}/send`); toast('方案已推送', 'success'); load() }
async function newVersion(row: any) { const d: any = await http.post(`/admin/solutions/${row.id}/new-version`); toast(`已创建版本 ${d.version}`, 'success'); load() }
async function remove(row: any) { if (!confirm(`确认删除方案「${row.name}」？`)) return; try { await http.delete(`/admin/solutions/${row.id}`); toast('已删除', 'success'); load() } catch {} }
onMounted(() => {
  loadOptions().then(() => { if (demandId.value) openCreate() })
  load()
})
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; align-items: center; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.solution-row { border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.solution-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 13px; }
.row-mid { font-size: 10px; color: var(--steel); margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 4px; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
.rich-content { font-size: 12px; line-height: 1.8; margin-top: 12px; }
</style>
