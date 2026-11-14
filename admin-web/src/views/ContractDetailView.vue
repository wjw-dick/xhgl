<template>
  <div class="page">
    <div class="page-head">
      <div>
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回合同列表</el-button>
        <h1 class="page-title" style="margin-top:8px">{{ contract?.contractNo }}</h1>
        <p class="page-sub">
          <span :class="['pill', statusPill(contract?.status)]">{{ statusLabel(contract?.status) }}</span>
          <span class="muted" style="margin-left:10px">{{ contract?.customerName }}</span>
          <span class="muted" style="margin-left:8px">签署：{{ statusLabel(contract?.signStatus) }} · 审批：{{ statusLabel(contract?.auditStatus) }}</span>
        </p>
      </div>
      <div class="toolbar" style="margin:0">
        <el-button @click="downloadPdf"><el-icon><Download /></el-icon> 合同 PDF</el-button>
        <el-button @click="downloadDoc('acceptance-pdf', '验收单')">验收单 PDF</el-button>
        <el-button @click="downloadDoc('installation-pdf', '安装调试记录')">安装记录 PDF</el-button>
        <el-button v-if="store.isAdmin && contract?.auditStatus === 'PENDING'" type="success" @click="approve(true)">审批通过</el-button>
        <el-button v-if="store.isAdmin && contract?.auditStatus === 'PENDING'" type="danger" plain @click="approve(false)">驳回</el-button>
        <el-button v-if="!readOnly && contract?.auditStatus === 'APPROVED' && contract?.signStatus === 'UNSIGNED'" type="primary" @click="sign">合同签署</el-button>
        <el-button v-if="!readOnly" @click="archive">归档扫描件</el-button>
      </div>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="contract">
      <div class="stat-grid">
        <div class="stat-card"><div class="stat-label">合同金额</div><div class="stat-value money">{{ fmtMoney(contract.amount) }}</div></div>
        <div class="stat-card"><div class="stat-label">已回款</div><div class="stat-value money success">{{ fmtMoney(received) }}</div><div class="stat-note">回款率 {{ receivedRate }}%</div></div>
        <div class="stat-card"><div class="stat-label">待回款</div><div class="stat-value money danger">{{ fmtMoney(outstanding) }}</div></div>
        <div class="stat-card"><div class="stat-label">履约进度</div><div class="stat-value">{{ detail.order?.productionProgress || '—' }}</div><div class="stat-note">{{ detail.order?.deliveryStatus || '待发货' }}</div></div>
      </div>
      <div class="card" style="padding:0">
        <el-tabs v-model="tab">
          <el-tab-pane label="合同概览" name="overview">
            <div class="grid-2" style="padding:16px">
              <el-descriptions :column="1" border size="small" title="合同信息">
                <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
                <el-descriptions-item label="合同名称">{{ contract.title }}</el-descriptions-item>
                <el-descriptions-item label="客户">{{ contract.customerName }}</el-descriptions-item>
                <el-descriptions-item label="金额">{{ fmtMoney(contract.amount) }}</el-descriptions-item>
                <el-descriptions-item label="付款条件">{{ contract.paymentTerms }}</el-descriptions-item>
                <el-descriptions-item label="模板">{{ contract.templateName }}</el-descriptions-item>
                <el-descriptions-item label="签署时间">{{ fmtDateTime(contract.signTime) }}</el-descriptions-item>
                <el-descriptions-item label="归档">{{ contract.archiveUrl || '未归档' }}</el-descriptions-item>
              </el-descriptions>
              <div>
                <el-descriptions :column="1" border size="small" title="订单 / 生产发货">
                  <el-descriptions-item label="订单号">{{ detail.order?.orderNo || '未生成订单' }}</el-descriptions-item>
                  <el-descriptions-item label="生产进度">{{ detail.order?.productionProgress || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="物流状态">{{ detail.order?.deliveryStatus || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="预计交付">{{ fmtDate(detail.order?.expectedDelivery) }}</el-descriptions-item>
                </el-descriptions>
                <el-form v-if="detail.order && !readOnly" :model="orderForm" label-position="top" style="margin-top:14px">
                  <el-form-item label="生产进度"><el-input v-model="orderForm.productionProgress" placeholder="如 生产完成80%" /></el-form-item>
                  <el-form-item label="物流状态"><el-select v-model="orderForm.deliveryStatus" style="width:100%"><el-option label="待发货" value="PENDING" /><el-option label="已发货" value="SHIPPED" /><el-option label="已到货" value="ARRIVED" /></el-select></el-form-item>
                  <el-button type="primary" @click="updateOrder">更新进度并通知客户</el-button>
                </el-form>
              </div>
            </div>
            <div class="rich-content" style="padding:0 16px 16px" v-html="sanitizeHtml(contract.content)"></div>
          </el-tab-pane>

          <el-tab-pane label="收款与账龄" name="payment">
            <div style="padding:16px">
              <div style="margin-bottom:10px"><el-button v-if="!readOnly" type="primary" size="small" @click="openPlan">新增收款计划</el-button></div>
              <el-table :data="detail.payments" stripe size="small">
                <el-table-column prop="phase" label="期次" width="110" />
                <el-table-column label="应收" width="130"><template #default="{ row }">{{ fmtMoney(row.amount) }}</template></el-table-column>
                <el-table-column label="实收" width="130"><template #default="{ row }">{{ fmtMoney(row.receivedAmount) }}</template></el-table-column>
                <el-table-column prop="planDate" label="计划日期" width="110" /><el-table-column prop="receivedDate" label="实收日期" width="110" />
                <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
                <el-table-column label="操作" width="100"><template #default="{ row }"><el-button v-if="!readOnly && row.status !== 'PAID'" link type="success" @click="receive(row)">登记收款</el-button></template></el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="发票" name="invoice">
            <div style="padding:16px">
              <el-button v-if="!readOnly" type="primary" size="small" style="margin-bottom:10px" @click="openInvoice">开具发票</el-button>
              <el-table :data="detail.invoices" stripe size="small">
                <el-table-column prop="invoiceNo" label="发票号" width="180" />
                <el-table-column prop="title" label="抬头" min-width="200" />
                <el-table-column label="金额" width="130"><template #default="{ row }">{{ fmtMoney(row.amount) }}</template></el-table-column>
                <el-table-column prop="issuedAt" label="开票日期" width="110" />
                <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
                <el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-if="!readOnly && row.status !== 'VOID'" link type="danger" @click="voidInvoice(row)">作废</el-button></template></el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="安装验收" name="install">
            <div style="padding:16px">
              <el-table :data="detail.installations" stripe size="small">
                <el-table-column prop="stage" label="节点" width="120">
                  <template #default="{ row }">{{ {SHIPPED:'发货',ARRIVED:'到货',INSTALL:'安装',ACCEPTANCE:'验收'}[row.stage] || row.stage }}</template>
                </el-table-column>
                <el-table-column prop="planDate" label="计划日期" width="110" />
                <el-table-column prop="actualDate" label="实际日期" width="110" />
                <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
                <el-table-column prop="acceptanceSigner" label="验收签字" width="120" />
                <el-table-column label="操作" width="100"><template #default="{ row }"><el-button v-if="!readOnly" link type="primary" @click="openInstall(row)">更新节点</el-button></template></el-table-column>
              </el-table>
              <div class="notice" style="margin-top:12px">验收节点置为“已完成”后，系统自动建档在役设备台账、初始化定期保养/年检/定检计划与质保期，并移交售后。</div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="设备台账" name="equipment">
            <div style="padding:16px">
              <el-table :data="detail.equipment" stripe size="small">
                <el-table-column prop="equipmentNo" label="设备编号" width="190" />
                <el-table-column prop="model" label="型号" width="150" />
                <el-table-column prop="capacity" label="容量" width="100" />
                <el-table-column prop="installAddr" label="安装地址" min-width="200" />
                <el-table-column prop="installDate" label="安装日期" width="110" />
                <el-table-column label="质保到期" width="110"><template #default="{ row }">{{ fmtDate(row.warrantyEnd) }}</template></el-table-column>
              </el-table>
              <el-empty v-if="!detail.equipment.length" description="验收完成后自动建档" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <el-dialog v-model="planVisible" title="新增收款计划" width="520px">
        <el-form :model="planForm" label-position="top">
          <el-form-item label="期次"><el-input v-model="planForm.phase" placeholder="定金 / 发货款 / 验收尾款" /></el-form-item>
          <el-form-item label="应收金额"><el-input-number v-model="planForm.amount" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="计划日期"><el-date-picker v-model="planForm.planDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="planVisible=false">取消</el-button><el-button type="primary" @click="savePlan">保存</el-button></template>
      </el-dialog>

      <el-dialog v-model="invoiceVisible" title="开具发票" width="560px">
        <el-form :model="invoiceForm" label-position="top" class="form-two">
          <el-form-item label="发票号"><el-input v-model="invoiceForm.invoiceNo" /></el-form-item>
          <el-form-item label="金额"><el-input-number v-model="invoiceForm.amount" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="抬头" class="full"><el-input v-model="invoiceForm.title" /></el-form-item>
          <el-form-item label="税号" class="full"><el-input v-model="invoiceForm.taxNo" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="invoiceVisible=false">取消</el-button><el-button type="primary" @click="saveInvoice">开具</el-button></template>
      </el-dialog>

      <el-dialog v-model="installVisible" title="更新履约节点" width="620px">
        <el-form :model="installForm" label-position="top" class="form-two">
          <el-form-item label="节点"><el-input v-model="installForm.stage" disabled /></el-form-item>
          <el-form-item label="状态"><el-select v-model="installForm.status" style="width:100%"><el-option label="待处理" value="PENDING" /><el-option label="已完成" value="DONE" /></el-select></el-form-item>
          <el-form-item label="计划日期"><el-date-picker v-model="installForm.planDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="实际日期"><el-date-picker v-model="installForm.actualDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="验收签字人"><el-input v-model="installForm.acceptanceSigner" /></el-form-item>
          <el-form-item label="验收资料 URL"><el-input v-model="installForm.acceptanceUrl" /></el-form-item>
          <el-form-item label="备注 / 安装地址" class="full"><el-input v-model="installForm.remark" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="installVisible=false">取消</el-button><el-button type="primary" @click="saveInstall">保存</el-button></template>
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
import { fmtDate, fmtDateTime, fmtMoney, sanitizeHtml, statusLabel, statusPill } from '@/utils/format'

const route = useRoute(); const router = useRouter(); const store = authStore()
const id = Number(route.params.id)
const loading = ref(false); const detail = ref<any>(null); const tab = ref('overview')
const readOnly = computed(() => false)
const contract = computed(() => detail.value?.contract)
const received = computed(() => (detail.value?.payments || []).reduce((s: number, p: any) => s + Number(p.receivedAmount || 0), 0))
const outstanding = computed(() => (detail.value?.payments || []).filter((p: any) => p.status !== 'PAID').reduce((s: number, p: any) => s + Number(p.amount || 0) - Number(p.receivedAmount || 0), 0))
const receivedRate = computed(() => contract.value?.amount ? Math.round(received.value * 10000 / Number(contract.value.amount)) / 100 : 0)
const planVisible = ref(false); const planForm = reactive<any>({})
const invoiceVisible = ref(false); const invoiceForm = reactive<any>({})
const installVisible = ref(false); const installForm = reactive<any>({})
const orderForm = reactive<any>({})
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
  const comment = await ElMessageBox.prompt(approved ? '审批意见' : '驳回原因', approved ? '合同审批通过' : '合同驳回', { inputPlaceholder: '留痕到审计日志' })
  await http.post(`/admin/contracts/${id}/approve`, { approved, comment: comment.value || '' })
  ElMessage.success('审批状态已更新'); load()
}
async function sign() {
  await ElMessageBox.confirm('确认合同已完成签署？系统将自动生成订单、收款计划与安装履约节点。', '合同签署')
  await http.post(`/admin/contracts/${id}/sign`, { signType: 'ELECTRONIC' })
  ElMessage.success('合同已签署，订单与收款计划已生成'); load()
}
async function archive() {
  const url = await ElMessageBox.prompt('请输入合同扫描件对象存储 URL（可通过附件上传后粘贴）', '合同归档', { inputValue: contract.value.archiveUrl || '' })
  await http.post(`/admin/contracts/${id}/archive`, { archiveUrl: url.value })
  ElMessage.success('合同已归档'); load()
}
async function updateOrder() { await http.put(`/admin/orders/${detail.value.order.id}`, orderForm); ElMessage.success('订单进度已更新'); load() }
function openInstall(row: any) { Object.keys(installForm).forEach((k) => delete installForm[k]); Object.assign(installForm, row); installVisible.value = true }
async function saveInstall() {
  await http.post(`/admin/contracts/${id}/installations/${installForm.id}/update`, installForm)
  ElMessage.success('履约节点已更新'); installVisible.value = false; load()
}
function openPlan() { Object.keys(planForm).forEach((k) => delete planForm[k]); planVisible.value = true }
async function savePlan() { await http.post(`/admin/contracts/${id}/payment-plans`, planForm); ElMessage.success('收款计划已新增'); planVisible.value = false; load() }
async function receive(row: any) {
  const amount = await ElMessageBox.prompt(`登记 ${row.phase} 实收金额`, '登记回款', { inputValue: String(row.amount || 0) })
  await http.post(`/admin/contracts/payment-plans/${row.id}/receive`, { amount: amount.value })
  ElMessage.success('回款已登记'); load()
}
function openInvoice() { Object.keys(invoiceForm).forEach((k) => delete invoiceForm[k]); invoiceForm.amount = outstanding.value; invoiceForm.title = contract.value.customerName; invoiceVisible.value = true }
async function saveInvoice() { await http.post(`/admin/contracts/${id}/invoices`, invoiceForm); ElMessage.success('发票已记录'); invoiceVisible.value = false; load() }
async function voidInvoice(row: any) { await ElMessageBox.confirm('确认作废该发票？', '危险操作', { type: 'warning' }); await http.post(`/admin/contracts/invoices/${row.id}/void`); ElMessage.success('已作废'); load() }
onMounted(load)
</script>

<style scoped>
:deep(.el-tabs__header) { padding: 0 16px; }
</style>
