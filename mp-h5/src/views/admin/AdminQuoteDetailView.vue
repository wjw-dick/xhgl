<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>报价详情</h1><button class="link-btn danger" @click="removeQuote">删除</button></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="quote" class="page-body">
      <div class="card">
        <div class="row-top"><strong class="mono">{{ quote.quoteNo }}</strong><span :class="['pill', statusPill(quote.status)]">{{ label(quote.status) }}</span></div>
        <div class="row-mid">{{ quote.customerName }} · 版本 {{ quote.version }} · 有效期 {{ date(quote.validUntil) }}</div>
        <div v-if="quote.approvalStatus === 'PENDING'" class="notice" style="margin-top:10px">当前报价待审批，审批通过后方可发送或成交。</div>
        <div v-if="belowFloor" class="notice" style="margin-top:10px;border-color:#b33426;background:#fbe9e6;color:#7f1d1d">当前报价低于底价红线，必须审批通过后方可发送/成交。</div>
        <div class="info-grid">
          <div><span>产品型号</span><strong>{{ quote.productName || '—' }}</strong></div>
          <div><span>数量</span><strong>{{ quote.quantity }}</strong></div>
          <div><span>单价</span><strong>{{ money(quote.unitPrice) }}</strong></div>
          <div><span>税率</span><strong>{{ Number(quote.taxRate || 0) * 100 }}%</strong></div>
          <div><span>含税总价</span><strong>{{ money(quote.totalAmount) }}</strong></div>
          <div><span>底价红线</span><strong>{{ money(quote.floorPrice) }}</strong></div>
          <div><span>付款条件</span><strong>{{ quote.paymentTerms || '—' }}</strong></div>
          <div><span>质保期</span><strong>{{ quote.warranty || '—' }}</strong></div>
        </div>
        <div class="share-line">客户分享链接：<span class="mono">{{ shareLink }}</span><button class="link-btn" @click="copyShare">复制</button></div>
        <div class="action-grid">
          <button class="btn btn-ghost btn-sm" @click="pdf">PDF</button>
          <button class="btn btn-ghost btn-sm" @click="newVersion">新版本</button>
          <button v-if="canSubmitApproval" class="btn btn-ghost btn-sm" @click="submitApproval">提交审批</button>
          <button v-if="canApprove" class="btn btn-primary btn-sm" @click="approve(true)">审批通过</button>
          <button v-if="canApprove" class="btn btn-ghost btn-sm" style="color:#b33426" @click="approve(false)">审批驳回</button>
          <button v-if="canSend" class="btn btn-primary btn-sm" @click="send">发送客户</button>
          <button v-if="canConfirm" class="btn btn-primary btn-sm" @click="confirm">登记确认</button>
          <button v-if="quote.status === 'CONFIRMED' && !contract" class="btn btn-primary btn-sm" @click="convert">生成合同</button>
          <router-link v-if="contract" :to="`/admin/contracts/${contract.id}`" class="btn btn-ghost btn-sm">打开合同</router-link>
        </div>
      </div>

      <div class="section-label">议价记录 <button class="link-btn" @click="negotiationVisible=true">＋ 新增</button></div>
      <div class="card">
        <div v-for="n in negotiations" :key="n.id" class="list-row">
          <div class="main"><strong>第 {{ n.roundNo }} 轮 · {{ n.party === 'CUSTOMER' ? '客户还价' : '我方报价' }}</strong><span>{{ n.operator }} · {{ n.remark || '—' }}</span></div>
          <span class="money">{{ money(n.amount) }}</span>
        </div>
        <div v-if="!negotiations.length" class="muted" style="font-size:11px">暂无议价记录</div>
      </div>

      <div class="section-label">历史版本</div>
      <div class="card">
        <router-link v-for="v in versions" :key="v.id" :to="`/admin/quotes/${v.id}`" class="list-row">
          <div class="main"><strong class="mono">{{ v.quoteNo }}</strong><span>v{{ v.version }} · {{ label(v.status) }}</span></div>
          <span class="money">{{ money(v.totalAmount) }}</span>
        </router-link>
      </div>
    </div>

    <div v-if="negotiationVisible" class="modal-mask" @click.self="negotiationVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增议价记录</h3><button class="modal-close" @click="negotiationVisible=false">×</button></div>
        <div class="field"><label>议价方</label><select v-model="negotiationForm.party"><option value="COMPANY">我方重新报价</option><option value="CUSTOMER">客户还价</option></select></div>
        <div class="field" style="margin-top:10px"><label>议价总额（元）</label><input v-model.number="negotiationForm.amount" type="number" /></div>
        <div class="field" style="margin-top:10px"><label>说明</label><textarea v-model="negotiationForm.remark" rows="3" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="negotiationVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="addNegotiation">保存</button></div>
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

const route = useRoute(); const router = useRouter(); const id = Number(route.params.id)
const detail = ref<any>(null); const loading = ref(false)
const quote = computed(() => detail.value?.quote)
const negotiations = computed(() => detail.value?.negotiations || [])
const versions = computed(() => detail.value?.versions || [])
const contract = computed(() => detail.value?.contracts?.[0])
const belowFloor = computed(() => quote.value?.floorPrice != null && Number(quote.value?.totalAmount || 0) < Number(quote.value.floorPrice))
const canSubmitApproval = computed(() => belowFloor.value && ['DRAFT', 'REJECTED', 'APPROVAL'].includes(quote.value?.status))
const canApprove = computed(() => quote.value?.approvalStatus === 'PENDING')
const canSend = computed(() => ['DRAFT', 'REJECTED', 'BARGAINING'].includes(quote.value?.status) && (!belowFloor.value || quote.value?.approvalStatus === 'APPROVED'))
const canConfirm = computed(() => ['SENT', 'BARGAINING'].includes(quote.value?.status))
const shareLink = computed(() => `http://127.0.0.1:5174/share/quote?token=${quote.value?.shareToken || ''}`)
const negotiationVisible = ref(false); const negotiationForm = reactive<any>({ party: 'CUSTOMER', amount: 0, remark: '' })
async function load() {
  loading.value = true
  try { detail.value = await http.get(`/admin/quotes/${id}`) } finally { loading.value = false }
}
function pdf() { downloadFile(`/admin/quotes/${id}/pdf`, `${quote.value.quoteNo}.pdf`) }
async function newVersion() { const d: any = await http.post(`/admin/quotes/${id}/new-version`); toast(`已生成新版本 ${d.quoteNo}`, 'success'); router.push(`/admin/quotes/${d.id}`) }
async function submitApproval() { await http.post(`/admin/quotes/${id}/approval`); toast('已提交审批', 'success'); load() }
async function approve(approved: boolean) {
  const comment = window.prompt(approved ? '请输入审批通过意见' : '请输入驳回原因') || ''
  await http.post(`/admin/quotes/${id}/approve`, { approved, comment }); toast(approved ? '审批已通过' : '审批已驳回', 'success'); load()
}
async function send() { if (!window.confirm('发送后客户可在线确认报价，确认后自动生成合同草稿。')) return; await http.post(`/admin/quotes/${id}/send`); toast('报价已发送', 'success'); load() }
async function confirm() { if (!window.confirm('确认客户已同意该报价？将生成合同草稿。')) return; await http.post(`/admin/quotes/${id}/confirm`); toast('报价已确认', 'success'); load() }
async function convert() { await http.post(`/admin/quotes/${id}/convert-contract`); toast('合同草稿已生成', 'success'); load() }
async function addNegotiation() { await http.post(`/admin/quotes/${id}/negotiate`, negotiationForm); toast('议价记录已保存', 'success'); negotiationVisible.value = false; load() }
async function copyShare() { await navigator.clipboard.writeText(shareLink.value); toast('链接已复制', 'success') }
async function removeQuote() { if (!window.confirm(`确认删除报价「${quote.value?.quoteNo}」？`)) return; try { await http.delete(`/admin/quotes/${id}`); toast('已删除', 'success'); router.replace('/admin/quotes') } catch {} }
onMounted(load)
</script>

<style scoped>
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-mid { font-size: 11px; color: var(--steel); margin-top: 5px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; margin-top: 10px; }
.info-grid div { background: #fff; padding: 9px 10px; }
.info-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.info-grid strong { font-size: 11px; }
.share-line { font-size: 9px; color: var(--steel); word-break: break-all; margin-top: 10px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.action-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
</style>
