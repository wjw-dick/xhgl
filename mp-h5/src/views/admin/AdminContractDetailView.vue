<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>合同详情</h1><button class="link-btn danger" @click="removeContract">删除</button></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="contract" class="page-body">
      <div class="card">
        <div class="row-top"><strong class="mono">{{ contract.contractNo }}</strong><span :class="['pill', statusPill(contract.status)]">{{ label(contract.status) }}</span></div>
        <div class="row-mid">{{ contract.customerName }} · {{ contract.title }}</div>
        <div class="info-grid">
          <div><span>合同金额</span><strong>{{ money(contract.amount) }}</strong></div>
          <div><span>已回款</span><strong>{{ money(received) }}</strong></div>
          <div><span>待回款</span><strong>{{ money(outstanding) }}</strong></div>
          <div><span>回款率</span><strong>{{ receivedRate }}%</strong></div>
          <div><span>审批</span><strong>{{ label(contract.auditStatus) }}</strong></div>
          <div><span>签署</span><strong>{{ label(contract.signStatus) }}</strong></div>
          <div><span>付款条件</span><strong>{{ contract.paymentTerms || '—' }}</strong></div>
          <div><span>签署时间</span><strong>{{ dateTime(contract.signTime) }}</strong></div>
        </div>
        <div class="action-grid">
          <button class="btn btn-ghost btn-sm" @click="downloadPdf">合同 PDF</button>
          <button class="btn btn-ghost btn-sm" @click="downloadDoc('acceptance-pdf', '验收单')">验收单</button>
          <button class="btn btn-ghost btn-sm" @click="downloadDoc('installation-pdf', '安装记录')">安装记录</button>
          <button v-if="contract.auditStatus === 'PENDING'" class="btn btn-primary btn-sm" @click="approve(true)">审批通过</button>
          <button v-if="contract.auditStatus === 'PENDING'" class="btn btn-ghost btn-sm" style="color:#b33426" @click="approve(false)">驳回</button>
          <button v-if="contract.auditStatus === 'APPROVED' && contract.signStatus === 'UNSIGNED'" class="btn btn-primary btn-sm" @click="sign">合同签署</button>
          <button class="btn btn-ghost btn-sm" @click="archive">归档</button>
        </div>
      </div>

      <div class="tabs">
        <button v-for="t in tabs" :key="t.value" :class="{ active: tab === t.value }" @click="tab = t.value">{{ t.label }}</button>
      </div>

      <template v-if="tab === 'overview'">
        <div class="card">
          <div class="section-label" style="margin-top:0">订单 / 生产发货</div>
          <div v-if="order" class="list-row"><div class="main"><strong class="mono">{{ order.orderNo }}</strong><span>生产 {{ order.productionProgress || '—' }} · {{ order.deliveryStatus || '待发货' }} · 预计 {{ date(order.expectedDelivery) }}</span></div></div>
          <div v-if="order" class="form-grid" style="margin-top:10px">
            <div class="field"><label>生产进度</label><input v-model="orderForm.productionProgress" placeholder="如 生产完成80%" /></div>
            <div class="field"><label>物流状态</label><select v-model="orderForm.deliveryStatus"><option value="PENDING">待发货</option><option value="SHIPPED">已发货</option><option value="ARRIVED">已到货</option></select></div>
          </div>
          <button v-if="order" class="btn btn-primary btn-sm" style="margin-top:8px" @click="updateOrder">更新进度并通知客户</button>
          <div v-if="!order" class="muted" style="font-size:11px;margin-top:8px">合同签署后自动生成订单。</div>
          <div class="rich-content" v-html="sanitizeHtml(contract.content)"></div>
        </div>
      </template>

      <template v-else-if="tab === 'payment'">
        <div class="card">
          <div class="card-title"><h2>收款计划</h2><button class="link-btn" @click="openPlan">＋ 新增</button></div>
          <div v-for="p in payments" :key="p.id" class="list-row">
            <div class="main"><strong>{{ p.phase }}</strong><span>应收 {{ money(p.amount) }} · 实收 {{ money(p.receivedAmount) }} · 计划 {{ date(p.planDate) }}</span></div>
            <div style="text-align:right"><span :class="['pill', statusPill(p.status)]">{{ label(p.status) }}</span><button v-if="p.status !== 'PAID'" class="link-btn" style="display:block;margin-top:4px" @click="receive(p)">登记收款</button></div>
          </div>
          <div v-if="!payments.length" class="muted" style="font-size:11px">暂无收款计划</div>
        </div>
      </template>

      <template v-else-if="tab === 'invoice'">
        <div class="card">
          <div class="card-title"><h2>发票</h2><button class="link-btn" @click="openInvoice">＋ 开具</button></div>
          <div v-for="i in invoices" :key="i.id" class="list-row">
            <div class="main"><strong>{{ i.invoiceNo || '未填发票号' }}</strong><span>{{ i.title }} · {{ money(i.amount) }} · {{ date(i.issuedAt) }}</span></div>
            <div style="text-align:right"><span :class="['pill', statusPill(i.status)]">{{ label(i.status) }}</span><button v-if="i.status !== 'VOID'" class="link-btn" style="display:block;margin-top:4px" @click="voidInvoice(i)">作废</button></div>
          </div>
          <div v-if="!invoices.length" class="muted" style="font-size:11px">暂无发票</div>
        </div>
      </template>

      <template v-else-if="tab === 'install'">
        <div class="card">
          <div class="card-title"><h2>安装验收</h2></div>
          <div v-for="i in installations" :key="i.id" class="list-row">
            <div class="main"><strong>{{ stageLabel(i.stage) }}</strong><span>计划 {{ date(i.planDate) }} · 实际 {{ date(i.actualDate) }} · 签字 {{ i.acceptanceSigner || '—' }}</span></div>
            <div style="text-align:right"><span :class="['pill', statusPill(i.status)]">{{ label(i.status) }}</span><button class="link-btn" style="display:block;margin-top:4px" @click="openInstall(i)">更新节点</button></div>
          </div>
          <div v-if="!installations.length" class="muted" style="font-size:11px">暂无履约节点</div>
        </div>
      </template>

      <template v-else>
        <div class="card">
          <div class="card-title"><h2>设备台账</h2></div>
          <div v-for="e in equipment" :key="e.id" class="list-row">
            <div class="main"><strong>{{ e.equipmentNo }}</strong><span>{{ e.model }} · {{ e.capacity }} · 质保至 {{ date(e.warrantyEnd) }}</span></div>
          </div>
          <div v-if="!equipment.length" class="muted" style="font-size:11px">验收完成后自动建档</div>
        </div>
      </template>
    </div>

    <div v-if="planVisible" class="modal-mask" @click.self="planVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增收款计划</h3><button class="modal-close" @click="planVisible=false">×</button></div>
        <div class="field"><label>期次</label><input v-model="planForm.phase" placeholder="定金 / 发货款 / 验收尾款" /></div>
        <div class="field" style="margin-top:10px"><label>应收金额</label><input v-model.number="planForm.amount" type="number" /></div>
        <div class="field" style="margin-top:10px"><label>计划日期</label><input v-model="planForm.planDate" type="date" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="planVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="savePlan">保存</button></div>
      </div>
    </div>

    <div v-if="invoiceVisible" class="modal-mask" @click.self="invoiceVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>开具发票</h3><button class="modal-close" @click="invoiceVisible=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>发票号</label><input v-model="invoiceForm.invoiceNo" /></div>
          <div class="field"><label>金额</label><input v-model.number="invoiceForm.amount" type="number" /></div>
          <div class="field full"><label>抬头</label><input v-model="invoiceForm.title" /></div>
          <div class="field full"><label>税号</label><input v-model="invoiceForm.taxNo" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="invoiceVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveInvoice">开具</button></div>
      </div>
    </div>

    <div v-if="installVisible" class="modal-mask" @click.self="installVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>更新履约节点</h3><button class="modal-close" @click="installVisible=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>节点</label><input v-model="installForm.stage" disabled /></div>
          <div class="field"><label>状态</label><select v-model="installForm.status"><option value="PENDING">待处理</option><option value="DONE">已完成</option></select></div>
          <div class="field"><label>计划日期</label><input v-model="installForm.planDate" type="date" /></div>
          <div class="field"><label>实际日期</label><input v-model="installForm.actualDate" type="date" /></div>
          <div class="field"><label>验收签字人</label><input v-model="installForm.acceptanceSigner" /></div>
          <div class="field"><label>验收资料 URL</label><input v-model="installForm.acceptanceUrl" /></div>
          <div class="field full"><label>备注 / 安装地址</label><input v-model="installForm.remark" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="installVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveInstall">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import { date, dateTime, label, money, statusPill, sanitizeHtml } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter(); const id = Number(route.params.id)
const detail = ref<any>(null); const loading = ref(false); const tab = ref('overview')
const contract = computed(() => detail.value?.contract)
const order = computed(() => detail.value?.order)
const payments = computed(() => detail.value?.payments || [])
const invoices = computed(() => detail.value?.invoices || [])
const installations = computed(() => detail.value?.installations || [])
const equipment = computed(() => detail.value?.equipment || [])
const received = computed(() => payments.value.reduce((s: number, p: any) => s + Number(p.receivedAmount || 0), 0))
const outstanding = computed(() => payments.value.filter((p: any) => p.status !== 'PAID').reduce((s: number, p: any) => s + Number(p.amount || 0) - Number(p.receivedAmount || 0), 0))
const receivedRate = computed(() => contract.value?.amount ? Math.round(received.value * 10000 / Number(contract.value.amount)) / 100 : 0)
const tabs = [{ value: 'overview', label: '概览' }, { value: 'payment', label: '收款' }, { value: 'invoice', label: '发票' }, { value: 'install', label: '安装验收' }, { value: 'equipment', label: '设备' }]
const planVisible = ref(false); const planForm = reactive<any>({})
const invoiceVisible = ref(false); const invoiceForm = reactive<any>({})
const installVisible = ref(false); const installForm = reactive<any>({})
const orderForm = reactive<any>({})
function stageLabel(stage: string) { return ({ SHIPPED: '发货', ARRIVED: '到货', INSTALL: '安装', ACCEPTANCE: '验收' } as any)[stage] || stage }
async function load() {
  loading.value = true
  try {
    detail.value = await http.get(`/admin/contracts/${id}`)
    if (detail.value.order) Object.assign(orderForm, { productionProgress: detail.value.order.productionProgress, deliveryStatus: detail.value.order.deliveryStatus })
  } finally { loading.value = false }
}
function downloadPdf() { downloadFile(`/admin/contracts/${id}/pdf`, `${contract.value.contractNo}.pdf`) }
function downloadDoc(path: string, name: string) { downloadFile(`/admin/contracts/${id}/${path}`, `${contract.value.contractNo}-${name}.pdf`) }
async function approve(approved: boolean) {
  const comment = prompt(approved ? '请输入审批通过意见' : '请输入驳回原因') || ''
  await http.post(`/admin/contracts/${id}/approve`, { approved, comment }); toast('审批状态已更新', 'success'); load()
}
async function sign() { if (!confirm('确认合同已完成签署？将自动生成订单、收款计划与安装履约节点。')) return; await http.post(`/admin/contracts/${id}/sign`, { signType: 'ELECTRONIC' }); toast('合同已签署', 'success'); load() }
async function archive() { const url = prompt('请输入合同扫描件 URL', contract.value.archiveUrl || '') || ''; await http.post(`/admin/contracts/${id}/archive`, { archiveUrl: url }); toast('合同已归档', 'success'); load() }
async function updateOrder() { await http.put(`/admin/orders/${order.value.id}`, orderForm); toast('订单进度已更新', 'success'); load() }
function openPlan() { Object.keys(planForm).forEach((k) => delete planForm[k]); planVisible.value = true }
async function savePlan() { await http.post(`/admin/contracts/${id}/payment-plans`, planForm); toast('收款计划已新增', 'success'); planVisible.value = false; load() }
async function receive(row: any) { const amount = prompt(`登记 ${row.phase} 实收金额`, String(row.amount || 0)); if (amount === null) return; await http.post(`/admin/contracts/payment-plans/${row.id}/receive`, { amount }); toast('回款已登记', 'success'); load() }
function openInvoice() { Object.keys(invoiceForm).forEach((k) => delete invoiceForm[k]); Object.assign(invoiceForm, { amount: outstanding.value, title: contract.value.customerName }); invoiceVisible.value = true }
async function saveInvoice() { await http.post(`/admin/contracts/${id}/invoices`, invoiceForm); toast('发票已记录', 'success'); invoiceVisible.value = false; load() }
async function voidInvoice(row: any) { if (!confirm('确认作废该发票？')) return; await http.post(`/admin/contracts/invoices/${row.id}/void`); toast('已作废', 'success'); load() }
function openInstall(row: any) { Object.keys(installForm).forEach((k) => delete installForm[k]); Object.assign(installForm, row); installVisible.value = true }
async function saveInstall() { await http.post(`/admin/contracts/${id}/installations/${installForm.id}/update`, installForm); toast('履约节点已更新', 'success'); installVisible.value = false; load() }
async function removeContract() { if (!confirm(`确认删除合同「${contract.value?.contractNo}」？将级联删除关联履约数据。`)) return; try { await http.delete(`/admin/contracts/${id}`); toast('已删除', 'success'); router.replace('/admin/contracts') } catch {} }
onMounted(load)
</script>

<style scoped>
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-mid { font-size: 11px; color: var(--steel); margin-top: 5px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; margin-top: 10px; }
.info-grid div { background: #fff; padding: 9px 10px; }
.info-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.info-grid strong { font-size: 11px; }
.action-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.tabs { display: flex; gap: 6px; overflow-x: auto; padding: 12px 0 2px; }
.tabs button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.tabs button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.rich-content { font-size: 11px; line-height: 1.8; margin-top: 12px; }
</style>
