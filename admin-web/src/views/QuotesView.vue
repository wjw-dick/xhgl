<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">报价议价</h1><p class="page-sub">多版本报价、逐轮议价、底价红线审批、在线确认与一键生成合同草稿。</p></div>
      <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新建报价</el-button>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="报价单号 / 客户 / 产品" @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="状态" @change="search">
          <el-option label="草稿" value="DRAFT" /><el-option label="审批中" value="APPROVAL" /><el-option label="已发送" value="SENT" />
          <el-option label="客户还价" value="BARGAINING" /><el-option label="已确认" value="CONFIRMED" /><el-option label="已过期" value="EXPIRED" />
        </el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="quoteNo" label="报价单号" width="190" />
        <el-table-column prop="customerName" label="客户" min-width="190" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品" width="140" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="含税总价" width="130"><template #default="{ row }"><span class="money">{{ fmtMoney(row.totalAmount) }}</span></template></el-table-column>
        <el-table-column label="底价红线" width="120"><template #default="{ row }">{{ fmtMoney(row.floorPrice) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column prop="validUntil" label="有效期" width="110"><template #default="{ row }">{{ fmtDate(row.validUntil) }}</template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/quotes/${row.id}`)">详情</el-button>
            <el-button link @click="downloadPdf(row)">PDF</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="removeQuote(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="totalCount" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建报价单" width="880px" top="4vh" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="关联需求" required>
          <el-select v-model="form.demandId" filterable style="width:100%" @change="onDemand">
            <el-option v-for="d in demands" :key="d.id" :label="`${d.demandNo} · ${d.contactName} · ${statusLabel(d.boilerType)}${d.capacity}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品型号" required>
          <el-select v-model="form.productId" filterable style="width:100%" @change="onProduct">
            <el-option v-for="p in products" :key="p.id" :label="`${p.model} · ${p.capacity}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价（元）"><el-input-number v-model="form.unitPrice" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="税率"><el-input-number v-model="form.taxRate" :min="0" :max="1" :step="0.01" style="width:100%" /></el-form-item>
        <el-form-item label="运费（元）"><el-input-number v-model="form.freight" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="安装调试费（元）"><el-input-number v-model="form.installFee" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="底价红线（元）"><el-input-number v-model="form.floorPrice" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="有效期至"><el-date-picker v-model="form.validUntil" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="质保期"><el-input v-model="form.warranty" /></el-form-item>
        <el-form-item label="付款条件" class="full"><el-input v-model="form.paymentTerms" /></el-form-item>
        <el-form-item label="报价合计" class="full"><strong class="money">{{ fmtMoney(total) }}</strong></el-form-item>
      </el-form>
      <el-alert v-if="total < form.floorPrice" type="warning" :closable="false" show-icon
        title="低于底价红线，保存后自动进入审批流；审批通过前不可发送。" />
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存报价</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { downloadFile } from '@/api/http'
import { authStore } from '@/stores/auth'
import { fmtDate, fmtMoney, statusLabel, statusPill } from '@/utils/format'

const router = useRouter(); const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const totalCount = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const loading = ref(false)
const dialogVisible = ref(false); const products = ref<any[]>([]); const demands = ref<any[]>([])
const form = reactive<any>({ quantity: 1, taxRate: 0.13, freight: 0, installFee: 0, floorPrice: 0, warranty: '12个月' })
const total = computed(() => Number(form.unitPrice || 0) * Number(form.quantity || 1) + Number(form.freight || 0) + Number(form.installFee || 0))
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/quotes', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } })
    rows.value = data.records; totalCount.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; status.value = ''; search() }
async function loadOptions() {
  try { const d: any = await http.get('/admin/products', { params: { size: 200 } }); products.value = d.records } catch {}
  try { const d: any = await http.get('/admin/demands', { params: { size: 200 } }); demands.value = d.records } catch {}
}
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { quantity: 1, taxRate: 0.13, freight: 0, installFee: 0, floorPrice: 0, warranty: '12个月', paymentTerms: '预付30%，发货前40%，验收后30%', validUntil: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10) })
  dialogVisible.value = true
}
function onDemand(id: number) {
  const d = demands.value.find((x) => x.id === id)
  if (d) form.customerId = d.customerId
}
function onProduct(id: number) {
  const p = products.value.find((x) => x.id === id)
  if (p) { form.unitPrice = Number(p.guidePrice || 0); form.floorPrice = Math.round(Number(p.guidePrice || 0) * 0.88); form.productName = p.model }
}
async function save() {
  if (!form.demandId || !form.productId || !form.unitPrice) return ElMessage.warning('需求、产品、单价必填')
  await http.post('/admin/quotes', form)
  ElMessage.success('报价已创建'); dialogVisible.value = false; load()
}
function downloadPdf(row: any) { downloadFile(`/admin/quotes/${row.id}/pdf`, `${row.quoteNo}.pdf`) }
async function removeQuote(row: any) {
  await ElMessageBox.confirm(
    `确认删除报价「${row.quoteNo}」？将物理删除该报价及议价记录。已生成合同的报价不可删除，请先删除合同。`,
    '删除报价',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/quotes/${row.id}`)
  ElMessage.success('报价已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load()
}
onMounted(() => { load(); loadOptions() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
