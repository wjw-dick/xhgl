<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">合同履约</h1><p class="page-sub">合同审批/签署/归档、订单生产发货、收款计划、发票、安装验收与售后移交。</p></div>
      <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新建合同</el-button>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="合同编号 / 客户 / 名称" @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="履约状态" @change="search"><el-option label="草稿" value="DRAFT" /><el-option label="已审批" value="APPROVED" /><el-option label="履约中" value="PERFORMING" /><el-option label="已完成" value="DONE" /></el-select>
        <el-select v-model="auditStatus" clearable placeholder="审批状态" @change="search"><el-option label="待审批" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" /></el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="contractNo" label="合同编号" width="200" />
        <el-table-column prop="customerName" label="客户" min-width="190" show-overflow-tooltip />
        <el-table-column prop="title" label="合同名称" min-width="230" show-overflow-tooltip />
        <el-table-column label="金额" width="130"><template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template></el-table-column>
        <el-table-column label="审批" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.auditStatus)]">{{ statusLabel(row.auditStatus) }}</span></template></el-table-column>
        <el-table-column label="签署" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.signStatus)]">{{ statusLabel(row.signStatus) }}</span></template></el-table-column>
        <el-table-column label="履约" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column prop="signTime" label="签署时间" width="150"><template #default="{ row }">{{ fmtDateTime(row.signTime) }}</template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/contracts/${row.id}`)">履约详情</el-button>
            <el-button link @click="downloadPdf(row)">PDF</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="removeContract(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>
    <el-dialog v-model="dialogVisible" title="新建合同" width="760px" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="客户" required>
          <el-select v-model="form.customerId" filterable style="width:100%" @change="onCustomer">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同名称" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="合同金额"><el-input-number v-model="form.amount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="付款条件"><el-input v-model="form.paymentTerms" /></el-form-item>
        <el-form-item label="合同模板"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { downloadFile } from '@/api/http'
import { authStore } from '@/stores/auth'
import { fmtDateTime, fmtMoney, statusLabel, statusPill } from '@/utils/format'

const router = useRouter(); const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const auditStatus = ref(''); const loading = ref(false)
const dialogVisible = ref(false); const form = reactive<any>({}); const customers = ref<any[]>([])
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/contracts', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value, auditStatus: auditStatus.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; status.value = ''; auditStatus.value = ''; search() }
async function loadOptions() { try { customers.value = await http.get('/admin/customers/options') } catch {} }
function openCreate() { Object.keys(form).forEach((k) => delete form[k]); form.paymentTerms = '预付30%，发货前40%，验收后30%'; dialogVisible.value = true }
function onCustomer(id: number) { const c = customers.value.find((x) => x.id === id); if (c) form.title = `${c.name} 锅炉采购合同` }
async function save() {
  if (!form.customerId || !form.title) return ElMessage.warning('客户与合同名称必填')
  await http.post('/admin/contracts', form)
  ElMessage.success('合同草稿已创建'); dialogVisible.value = false; load()
}
function downloadPdf(row: any) { downloadFile(`/admin/contracts/${row.id}/pdf`, `${row.contractNo}.pdf`) }
async function removeContract(row: any) {
  await ElMessageBox.confirm(
    `确认删除合同「${row.contractNo}」？将同时物理删除关联订单、收款计划、发票、安装验收、设备与维保计划，删除后不可恢复。`,
    '删除合同',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/contracts/${row.id}`)
  ElMessage.success('合同及关联履约数据已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load()
}
onMounted(() => { load(); loadOptions() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
