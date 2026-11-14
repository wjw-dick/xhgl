<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>合同履约</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新建</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="合同编号 / 客户 / 名称" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; search()">{{ s.label }}</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="c in rows" :key="c.id" class="contract-row">
          <button class="contract-main" @click="router.push(`/admin/contracts/${c.id}`)">
            <div class="row-top"><strong class="mono">{{ c.contractNo }}</strong><span :class="['pill', statusPill(c.status)]">{{ label(c.status) }}</span></div>
            <div class="row-mid">{{ c.customerName }} · {{ c.title }}</div>
            <div class="row-bottom"><span class="money">{{ money(c.amount) }}</span><span>审批 {{ label(c.auditStatus) }} · 签署 {{ label(c.signStatus) }}</span></div>
          </button>
          <div class="row-actions">
            <button class="link-btn" @click="router.push(`/admin/contracts/${c.id}`)">履约详情</button>
            <button class="link-btn" @click="pdf(c)">PDF</button>
            <button class="link-btn danger" @click="remove(c)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">合</div>暂无合同</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新建合同</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>客户 *</label><select v-model.number="form.customerId" @change="onCustomer"><option value="">请选择客户</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option></select></div>
          <div class="field full"><label>合同名称 *</label><input v-model="form.title" /></div>
          <div class="field"><label>合同金额</label><input v-model.number="form.amount" type="number" /></div>
          <div class="field"><label>付款条件</label><input v-model="form.paymentTerms" /></div>
          <div class="field"><label>合同模板</label><input v-model="form.templateName" /></div>
          <div class="field full"><label>备注/正文</label><textarea v-model="form.content" rows="4" /></div>
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
import { label, money, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false); const dialog = ref(false)
const customers = ref<any[]>([]); const form = reactive<any>({ paymentTerms: '预付30%，发货前40%，验收后30%' })
const statuses = [{ label: '全部', value: '' }, { label: '草稿', value: 'DRAFT' }, { label: '已审批', value: 'APPROVED' }, { label: '履约中', value: 'PERFORMING' }, { label: '已完成', value: 'DONE' }]
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/contracts', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function loadCustomers() { try { customers.value = await http.get('/admin/customers/options') } catch {} }
function search() { page.value = 1; load() }
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { paymentTerms: '预付30%，发货前40%，验收后30%' })
}
function onCustomer(event: Event) {
  const id = Number((event.target as HTMLSelectElement).value)
  const c = customers.value.find((x) => x.id === id)
  if (c) form.title = `${c.name} 锅炉采购合同`
}
async function save() {
  if (!form.customerId || !form.title) return toast('客户与合同名称必填', 'error')
  await http.post('/admin/contracts', form); toast('合同草稿已创建', 'success'); dialog.value = false; load()
}
function pdf(row: any) { downloadFile(`/admin/contracts/${row.id}/pdf`, `${row.contractNo}.pdf`) }
async function remove(row: any) { if (!confirm(`确认删除合同「${row.contractNo}」？将同时删除关联订单、回款、发票、安装、设备与维保计划。`)) return; try { await http.delete(`/admin/contracts/${row.id}`); toast('已删除', 'success'); load() } catch {} }
onMounted(() => { load(); loadCustomers() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.contract-row { border-bottom: 1px solid #f2f4f7; padding: 6px 0 8px; }
.contract-row:last-child { border-bottom: none; }
.contract-main { display: block; width: 100%; text-align: left; border: none; background: transparent; padding: 6px 0; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 11px; color: #344054; margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 3px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
