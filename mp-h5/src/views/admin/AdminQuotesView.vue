<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>报价议价</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新建</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="报价单号 / 客户 / 产品" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; search()">{{ s.label }}</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="q in rows" :key="q.id" class="quote-row">
          <button class="quote-main" @click="router.push(`/admin/quotes/${q.id}`)">
            <div class="row-top"><strong class="mono">{{ q.quoteNo }}</strong><span :class="['pill', statusPill(q.status)]">{{ label(q.status) }}</span></div>
            <div class="row-mid">{{ q.customerName }} · {{ q.productName || '未选产品' }} · 版本 {{ q.version }}</div>
            <div class="row-bottom"><span class="money">{{ money(q.totalAmount) }}</span><span>有效期 {{ date(q.validUntil) }}</span></div>
          </button>
          <div class="row-actions">
            <button class="link-btn" @click="router.push(`/admin/quotes/${q.id}`)">详情</button>
            <button class="link-btn" @click="pdf(q)">PDF</button>
            <button class="link-btn danger" @click="remove(q)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">价</div>暂无报价</div>
      <div v-if="totalCount > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(totalCount / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(totalCount / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新建报价单</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>关联需求 *</label><select v-model.number="form.demandId" @change="onDemand"><option value="">请选择需求</option><option v-for="d in demands" :key="d.id" :value="d.id">{{ d.demandNo }} · {{ d.contactName }} · {{ d.capacity }}</option></select></div>
          <div class="field full"><label>产品型号 *</label><select v-model.number="form.productId" @change="onProduct"><option value="">请选择产品</option><option v-for="p in products" :key="p.id" :value="p.id">{{ p.model }} · {{ p.capacity }}</option></select></div>
          <div class="field"><label>单价</label><input v-model.number="form.unitPrice" type="number" /></div>
          <div class="field"><label>数量</label><input v-model.number="form.quantity" type="number" min="1" /></div>
          <div class="field"><label>税率</label><input v-model.number="form.taxRate" type="number" step="0.01" /></div>
          <div class="field"><label>运费</label><input v-model.number="form.freight" type="number" /></div>
          <div class="field"><label>安装调试费</label><input v-model.number="form.installFee" type="number" /></div>
          <div class="field"><label>底价红线</label><input v-model.number="form.floorPrice" type="number" /></div>
          <div class="field"><label>有效期至</label><input v-model="form.validUntil" type="date" /></div>
          <div class="field"><label>质保期</label><input v-model="form.warranty" /></div>
          <div class="field full"><label>付款条件</label><input v-model="form.paymentTerms" /></div>
          <div class="field full"><label>报价合计</label><strong class="money" style="font-size:18px">{{ money(total) }}</strong></div>
        </div>
        <div v-if="total < form.floorPrice" class="notice" style="margin-top:10px">低于底价红线，保存后自动进入审批流。</div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存报价</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import { date, label, money, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter()
const rows = ref<any[]>([]); const totalCount = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false)
const dialog = ref(false); const products = ref<any[]>([]); const demands = ref<any[]>([])
const demandFilter = ref<number | null>(route.query.demandId ? Number(route.query.demandId) : null)
const form = reactive<any>({ quantity: 1, taxRate: 0.13, freight: 0, installFee: 0, floorPrice: 0, warranty: '12个月', paymentTerms: '预付30%，发货前40%，验收后30%' })
const total = computed(() => Number(form.unitPrice || 0) * Number(form.quantity || 1) + Number(form.freight || 0) + Number(form.installFee || 0))
const statuses = [{ label: '全部', value: '' }, { label: '草稿', value: 'DRAFT' }, { label: '审批中', value: 'APPROVAL' }, { label: '已发送', value: 'SENT' }, { label: '还价中', value: 'BARGAINING' }, { label: '已确认', value: 'CONFIRMED' }, { label: '已过期', value: 'EXPIRED' }]
async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value, keyword: keyword.value, status: status.value }
    if (demandFilter.value) params.demandId = demandFilter.value
    const d: any = await http.get('/admin/quotes', { params }); rows.value = d.records || []; totalCount.value = d.total || 0
  } finally { loading.value = false }
}
async function loadOptions() {
  try { const d: any = await http.get('/admin/products', { params: { size: 200 } }); products.value = d.records || [] } catch {}
  try { const d: any = await http.get('/admin/demands', { params: { size: 200 } }); demands.value = d.records || [] } catch {}
}
function search() { page.value = 1; load() }
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { demandId: demandFilter.value || undefined, quantity: 1, taxRate: 0.13, freight: 0, installFee: 0, floorPrice: 0, warranty: '12个月', paymentTerms: '预付30%，发货前40%，验收后30%', validUntil: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10) })
  dialog.value = true
}
function onDemand(event: Event) {
  const id = Number((event.target as HTMLSelectElement).value)
  const d = demands.value.find((x) => x.id === id)
  if (d) form.customerId = d.customerId
}
function onProduct(event: Event) {
  const id = Number((event.target as HTMLSelectElement).value)
  const p = products.value.find((x) => x.id === id)
  if (p) { form.unitPrice = Number(p.guidePrice || 0); form.floorPrice = Math.round(Number(p.guidePrice || 0) * 0.88); form.productName = p.model }
}
async function save() {
  if (!form.demandId || !form.productId || !form.unitPrice) return toast('需求、产品、单价必填', 'error')
  await http.post('/admin/quotes', form); toast('报价已创建', 'success'); dialog.value = false; load()
}
function pdf(row: any) { downloadFile(`/admin/quotes/${row.id}/pdf`, `${row.quoteNo}.pdf`) }
async function remove(row: any) { if (!confirm(`确认删除报价「${row.quoteNo}」？已生成合同的报价不可删除。`)) return; try { await http.delete(`/admin/quotes/${row.id}`); toast('已删除', 'success'); load() } catch {} }
onMounted(() => {
  loadOptions().then(() => { if (demandFilter.value) openCreate() })
  load()
})
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.quote-row { border-bottom: 1px solid #f2f4f7; padding: 6px 0 8px; }
.quote-row:last-child { border-bottom: none; }
.quote-main { display: block; width: 100%; text-align: left; border: none; background: transparent; padding: 6px 0; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 11px; color: #344054; margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 3px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
