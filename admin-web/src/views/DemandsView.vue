<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">需求线索</h1><p class="page-sub">采购需求登记、查重、待认领、分配转派与跟进超时提醒。</p></div>
      <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 录入需求</el-button>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-label">全部需求</div><div class="stat-value">{{ funnel.total || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">待跟进（新/咨询）</div><div class="stat-value">{{ (funnel.counts?.NEW || 0) + (funnel.counts?.CONSULTING || 0) }}</div></div>
      <div class="stat-card"><div class="stat-label">谈价中</div><div class="stat-value">{{ funnel.counts?.TALKING || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">成交转化率</div><div class="stat-value">{{ funnel.conversionRate || 0 }}%</div></div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="需求编号 / 联系人 / 地区" @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="需求阶段" @change="search">
          <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="source" clearable placeholder="来源" @change="search">
          <el-option label="H5门户" value="H5" /><el-option label="小程序" value="MP" /><el-option label="公众号" value="OA" /><el-option label="电话" value="PHONE" /><el-option label="人工录入" value="MANUAL" />
        </el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end">
          <el-radio-group v-model="scope" @change="search">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pool">待认领</el-radio-button>
          </el-radio-group>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="demandNo" label="需求编号" width="190" />
        <el-table-column label="客户 / 联系人" min-width="210">
          <template #default="{ row }">
            <div style="font-weight:600">{{ customerName(row.customerId) }}</div>
            <div class="muted">{{ row.contactName }} · {{ row.contactPhoneMask }}</div>
          </template>
        </el-table-column>
        <el-table-column label="选型需求" min-width="210">
          <template #default="{ row }">{{ statusLabel(row.boilerType) }} · {{ row.capacity }} · {{ statusLabel(row.fuelType) }} · {{ statusLabel(row.pressureLevel) }}</template>
        </el-table-column>
        <el-table-column label="预算" width="120"><template #default="{ row }">{{ fmtMoney(row.budgetMin) }} ~ {{ fmtMoney(row.budgetMax) }}</template></el-table-column>
        <el-table-column prop="region" label="地区" width="130" />
        <el-table-column label="来源" width="80"><template #default="{ row }">{{ row.source }}</template></el-table-column>
        <el-table-column label="负责人" width="100"><template #default="{ row }">{{ row.ownerName || '待认领' }}</template></el-table-column>
        <el-table-column label="阶段" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column label="下次跟进" width="150">
          <template #default="{ row }">
            <span :class="{ danger: row.followNextTime && new Date(row.followNextTime) < new Date() }">{{ fmtDateTime(row.followNextTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/demands/${row.id}`)">详情</el-button>
            <el-button v-if="!row.ownerId && !readOnly" link type="success" @click="claim(row)">认领</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="removeDemand(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" title="录入采购需求" width="900px" top="4vh" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="关联客户" required>
          <el-select v-model="form.customerId" filterable style="width:100%" placeholder="请选择客户档案" @change="onCustomer">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${c.customerNo}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系人电话（脱敏展示）"><el-input v-model="form.contactPhoneMask" placeholder="如 138****1111" /></el-form-item>
        <el-form-item label="需求来源"><el-select v-model="form.source" style="width:100%"><el-option label="H5门户" value="H5" /><el-option label="小程序" value="MP" /><el-option label="公众号" value="OA" /><el-option label="电话" value="PHONE" /><el-option label="人工录入" value="MANUAL" /></el-select></el-form-item>
        <el-form-item label="锅炉类型"><el-select v-model="form.boilerType" style="width:100%"><el-option v-for="o in boilerTypes" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="燃料类型"><el-select v-model="form.fuelType" style="width:100%"><el-option v-for="o in fuelTypes" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="蒸发量/热功率"><el-select v-model="form.capacity" allow-create filterable style="width:100%"><el-option v-for="o in capacities" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="压力等级"><el-select v-model="form.pressureLevel" style="width:100%"><el-option v-for="o in pressures" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="预算下限（元）"><el-input-number v-model="form.budgetMin" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="预算上限（元）"><el-input-number v-model="form.budgetMax" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="期望地区"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="期望交付时间"><el-date-picker v-model="form.expectTime" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="用途说明" class="full"><el-input v-model="form.usageDesc" /></el-form-item>
        <el-form-item label="需求补充" class="full"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存需求</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import { boilerTypeOptions, capacityOptions, fuelTypeOptions, pressureOptions } from '@/crud/options'
import { fmtDateTime, fmtMoney, statusLabel, statusPill } from '@/utils/format'

const router = useRouter(); const store = authStore()
const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const source = ref(''); const scope = ref('all')
const loading = ref(false); const funnel = ref<any>({})
const customers = ref<any[]>([]); const dialogVisible = ref(false); const saving = ref(false)
const form = reactive<any>({ source: 'MANUAL', boilerType: 'steam', fuelType: 'gas' })
const boilerTypes = boilerTypeOptions; const fuelTypes = fuelTypeOptions; const capacities = capacityOptions; const pressures = pressureOptions
const statuses = [
  { label: '新需求', value: 'NEW' }, { label: '咨询中', value: 'CONSULTING' }, { label: '介绍中', value: 'INTRODUCING' },
  { label: '谈价中', value: 'TALKING' }, { label: '成交', value: 'DEAL' }, { label: '流失', value: 'LOST' }
]
function customerName(id: number) { return customers.value.find((c) => c.id === id)?.name || `客户#${id}` }
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/demands', { params: {
      page: page.value, size: size.value, keyword: keyword.value, status: status.value,
      source: source.value, unassigned: scope.value === 'pool' ? 'true' : undefined
    } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
async function loadFunnel() {
  try { funnel.value = await http.get('/admin/demands/funnel') } catch { funnel.value = {} }
}
async function loadCustomers() {
  try { customers.value = await http.get('/admin/customers/options') } catch { customers.value = [] }
}
function search() { page.value = 1; load() }
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { source: 'MANUAL', boilerType: 'steam', fuelType: 'gas', status: 'NEW' })
  dialogVisible.value = true
}
function onCustomer(id: number) {
  const c = customers.value.find((x) => x.id === id)
  if (c) { form.contactName = form.contactName || c.name }
}
async function save() {
  if (!form.customerId) return ElMessage.warning('请选择关联客户')
  saving.value = true
  try {
    await http.post('/admin/demands', form)
    ElMessage.success('需求已登记并生成编号'); dialogVisible.value = false; load(); loadFunnel()
  } finally { saving.value = false }
}
async function claim(row: any) {
  await ElMessageBox.confirm(`确认认领需求 ${row.demandNo}？认领后将归属到您的客户。`, '认领确认')
  await http.post(`/admin/demands/${row.id}/claim`); ElMessage.success('认领成功'); load()
}
async function removeDemand(row: any) {
  await ElMessageBox.confirm(
    `确认删除需求「${row.demandNo}」？将物理删除该需求及跟进记录。已关联方案或报价的需求不可删除，请先删除下游记录。`,
    '删除需求',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/demands/${row.id}`)
  ElMessage.success('需求已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load(); loadFunnel()
}
onMounted(() => { load(); loadFunnel(); loadCustomers() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
