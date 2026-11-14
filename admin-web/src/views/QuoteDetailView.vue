<template>
  <div class="page">
    <div class="page-head">
      <div>
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回报价列表</el-button>
        <h1 class="page-title" style="margin-top:8px">报价单 {{ quote?.quoteNo }}</h1>
        <p class="page-sub">
          <span :class="['pill', statusPill(quote?.status)]">{{ statusLabel(quote?.status) }}</span>
          <span style="margin-left:10px">{{ quote?.customerName }}</span>
          <span class="muted" style="margin-left:8px">版本 {{ quote?.version }} · 有效期至 {{ fmtDate(quote?.validUntil) }}</span>
          <span v-if="quote?.approvalStatus === 'PENDING'" class="pill yellow" style="margin-left:8px">待审批</span>
        </p>
      </div>
      <div class="toolbar" style="margin:0">
        <el-button @click="downloadPdf"><el-icon><Download /></el-icon> PDF</el-button>
        <el-button v-if="!readOnly" @click="newVersion"><el-icon><CopyDocument /></el-icon> 新版本</el-button>
        <el-button v-if="!readOnly && canSubmitApproval" type="warning" @click="submitApproval">提交审批</el-button>
        <template v-if="canApprove">
          <el-button type="success" @click="approve(true)">审批通过</el-button>
          <el-button type="danger" plain @click="approve(false)">审批驳回</el-button>
        </template>
        <el-button v-if="!readOnly && canSend" type="primary" @click="send">发送给客户</el-button>
        <el-button v-if="!readOnly && canConfirm" type="success" @click="confirm">登记客户确认</el-button>
        <el-button v-if="contract" @click="router.push(`/contracts/${contract.id}`)">打开合同</el-button>
        <el-button v-else-if="!readOnly && quote?.status === 'CONFIRMED'" type="primary" @click="convert">生成合同草稿</el-button>
      </div>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="quote">
      <div class="section-grid">
        <div class="card">
          <h3 class="card-title">报价明细</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="产品型号">{{ quote.productName }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ quote.quantity }}</el-descriptions-item>
            <el-descriptions-item label="单价">{{ fmtMoney(quote.unitPrice) }}</el-descriptions-item>
            <el-descriptions-item label="税率">{{ Number(quote.taxRate || 0) * 100 }}%</el-descriptions-item>
            <el-descriptions-item label="货值">{{ fmtMoney(quote.amount) }}</el-descriptions-item>
            <el-descriptions-item label="运费">{{ fmtMoney(quote.freight) }}</el-descriptions-item>
            <el-descriptions-item label="安装调试">{{ fmtMoney(quote.installFee) }}</el-descriptions-item>
            <el-descriptions-item label="含税总价"><strong class="money">{{ fmtMoney(quote.totalAmount) }}</strong></el-descriptions-item>
            <el-descriptions-item label="底价红线"><span class="danger">{{ fmtMoney(quote.floorPrice) }}</span></el-descriptions-item>
            <el-descriptions-item label="审批状态"><span :class="['pill', statusPill(quote.approvalStatus)]">{{ statusLabel(quote.approvalStatus) }}</span></el-descriptions-item>
            <el-descriptions-item label="付款条件" :span="2">{{ quote.paymentTerms }}</el-descriptions-item>
            <el-descriptions-item label="质保期">{{ quote.warranty }}</el-descriptions-item>
            <el-descriptions-item label="客户确认时间">{{ fmtDateTime(quote.confirmTime) }}</el-descriptions-item>
            <el-descriptions-item label="审批意见" :span="2">{{ quote.approvalComment || '—' }}</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="belowFloor" type="error" :closable="false" show-icon style="margin-top:12px"
            title="当前报价低于底价红线，必须审批通过后方可发送/成交。" />
          <div class="notice" style="margin-top:12px">
            客户分享链接：{{ shareLink }}
            <el-button link type="primary" @click="copyShare">复制</el-button>
          </div>
        </div>
        <div class="card">
          <h3 class="card-title">客户与需求</h3>
          <div v-if="detail.customer" class="kanban-item" style="margin-bottom:10px">
            <div class="title">{{ detail.customer.name }}</div>
            <div class="meta">{{ detail.customer.industry }} · {{ detail.customer.region }}</div>
            <div class="meta" style="margin-top:6px">联系人：{{ detail.demand?.contactName }} · {{ detail.demand?.contactPhoneMask }}</div>
          </div>
          <div v-if="detail.demand" class="kanban-item">
            <div class="title">{{ detail.demand.demandNo }}</div>
            <div class="meta">{{ statusLabel(detail.demand.boilerType) }} / {{ detail.demand.capacity }} / {{ statusLabel(detail.demand.fuelType) }}</div>
            <el-button link type="primary" @click="router.push(`/demands/${detail.demand.id}`)">打开需求</el-button>
          </div>
          <h3 class="card-title" style="margin-top:18px">历史版本</h3>
          <div v-for="v in detail.versions" :key="v.id" class="version-row">
            <span class="mono">{{ v.quoteNo }}</span>
            <span>v{{ v.version }}</span>
            <span class="money">{{ fmtMoney(v.totalAmount) }}</span>
            <span :class="['pill', statusPill(v.status)]">{{ statusLabel(v.status) }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">议价记录 <el-button v-if="!readOnly && quote.status !== 'EXPIRED'" link type="primary" style="margin-left:auto" @click="negotiationVisible=true">新增议价</el-button></h3>
        <el-timeline style="padding-left:8px">
          <el-timeline-item v-for="n in detail.negotiations" :key="n.id" :timestamp="`第 ${n.roundNo} 轮 · ${n.operator}`" placement="top">
            <div class="kanban-item">
              <div class="row"><strong>{{ n.party === 'CUSTOMER' ? '客户还价' : '我方报价' }}</strong><span class="money">{{ fmtMoney(n.amount) }}</span></div>
              <div class="meta" style="margin-top:6px">{{ n.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!detail.negotiations.length" description="暂无议价记录" />
      </div>

      <el-dialog v-model="negotiationVisible" title="新增议价记录" width="520px">
        <el-form :model="negotiationForm" label-position="top">
          <el-form-item label="议价方"><el-select v-model="negotiationForm.party" style="width:100%"><el-option label="我方重新报价" value="COMPANY" /><el-option label="客户还价" value="CUSTOMER" /></el-select></el-form-item>
          <el-form-item label="议价总额（元）"><el-input-number v-model="negotiationForm.amount" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="negotiationForm.remark" type="textarea" :rows="3" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="negotiationVisible=false">取消</el-button><el-button type="primary" @click="addNegotiation">保存</el-button></template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { downloadFile } from '@/api/http'
import { authStore } from '@/stores/auth'
import { fmtDate, fmtDateTime, fmtMoney, statusLabel, statusPill } from '@/utils/format'

const route = useRoute(); const router = useRouter(); const store = authStore()
const id = Number(route.params.id)
const loading = ref(false); const detail = ref<any>(null)
const quote = computed(() => detail.value?.quote)
const readOnly = computed(() => false)
const belowFloor = computed(() => quote.value?.floorPrice != null && Number(quote.value?.totalAmount || 0) < Number(quote.value.floorPrice))
const canSubmitApproval = computed(() => belowFloor.value && ['DRAFT', 'REJECTED', 'APPROVAL'].includes(quote.value?.status))
const canApprove = computed(() => store.isAdmin && quote.value?.approvalStatus === 'PENDING')
const canSend = computed(() => ['DRAFT', 'REJECTED', 'BARGAINING'].includes(quote.value?.status) && (!belowFloor.value || quote.value?.approvalStatus === 'APPROVED'))
const canConfirm = computed(() => ['SENT', 'BARGAINING'].includes(quote.value?.status))
const contract = computed(() => detail.value?.contracts?.[0])
const shareLink = computed(() => `http://127.0.0.1:5174/share/quote?token=${quote.value?.shareToken || ''}`)
const negotiationVisible = ref(false)
const negotiationForm = reactive<any>({ party: 'CUSTOMER', amount: 0, remark: '' })
async function load() {
  loading.value = true
  try { detail.value = await http.get(`/admin/quotes/${id}`) } finally { loading.value = false }
}
function downloadPdf() { downloadFile(`/admin/quotes/${id}/pdf`, `${quote.value.quoteNo}.pdf`) }
async function newVersion() {
  const data: any = await http.post(`/admin/quotes/${id}/new-version`)
  ElMessage.success(`已生成新版本 ${data.quoteNo}`); router.push(`/quotes/${data.id}`)
}
async function submitApproval() {
  await http.post(`/admin/quotes/${id}/approval`)
  ElMessage.success('已提交审批'); load()
}
async function approve(approved: boolean) {
  const comment = await ElMessageBox.prompt(approved ? '请输入审批通过意见' : '请输入驳回原因', approved ? '审批通过' : '审批驳回', { inputPlaceholder: '留痕到审计日志' })
  await http.post(`/admin/quotes/${id}/approve`, { approved, comment: comment.value || '' })
  ElMessage.success(approved ? '审批已通过' : '审批已驳回'); load()
}
async function send() {
  await ElMessageBox.confirm('发送后客户可在门户/小程序在线确认报价，确认后自动生成合同草稿。', '发送报价')
  await http.post(`/admin/quotes/${id}/send`); ElMessage.success('报价已发送'); load()
}
async function confirm() {
  await ElMessageBox.confirm('确认客户已同意该报价？将生成合同草稿。', '客户确认')
  await http.post(`/admin/quotes/${id}/confirm`); ElMessage.success('报价已确认，合同草稿已生成'); load()
}
async function convert() {
  await http.post(`/admin/quotes/${id}/convert-contract`); ElMessage.success('合同草稿已生成'); load()
}
async function addNegotiation() {
  await http.post(`/admin/quotes/${id}/negotiate`, negotiationForm)
  ElMessage.success('议价记录已保存'); negotiationVisible.value = false; load()
}
async function copyShare() {
  await navigator.clipboard.writeText(shareLink.value); ElMessage.success('链接已复制')
}
onMounted(load)
</script>

<style scoped>
.version-row { display: grid; grid-template-columns: 1.6fr .5fr 1fr .8fr; gap: 8px; padding: 8px 0; border-bottom: 1px dashed var(--xh-line); font-size: 12px; align-items: center; }
</style>
