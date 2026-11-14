<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">维保计划</h1><p class="page-sub">定期保养、年检、定检到期前红黄灯预警；完成后自动顺延下一周期。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增计划</el-button>
        <el-button @click="loadWarnings"><el-icon><Warning /></el-icon> 刷新预警</el-button>
      </div>
    </div>
    <div v-if="warnings.length" class="card">
      <h3 class="card-title">到期预警清单</h3>
      <div v-for="w in warnings" :key="w.plan.id" class="kanban-item" style="margin-bottom:8px">
        <div class="row">
          <div><strong>{{ w.plan.equipmentNo }}</strong> · {{ statusLabel(w.plan.project) }} · {{ w.plan.customerName }}</div>
          <span :class="['pill', w.level === 'RED' ? 'red' : 'yellow']">{{ w.remainDays < 0 ? `已逾期 ${-w.remainDays} 天` : `剩余 ${w.remainDays} 天` }}</span>
        </div>
        <div class="meta">到期日期：{{ fmtDate(w.plan.nextDate) }}</div>
      </div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="设备编号 / 客户" @keyup.enter="search" />
        <el-select v-model="project" clearable placeholder="维保项目" @change="search"><el-option label="定期保养" value="MAINTAIN" /><el-option label="年检" value="ANNUAL" /><el-option label="定检" value="PERIODIC" /></el-select>
        <el-select v-model="warnOnly" clearable placeholder="预警范围" @change="search"><el-option label="30天内到期" value="true" /></el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="equipmentNo" label="设备编号" width="190" />
        <el-table-column prop="customerName" label="客户" min-width="200" show-overflow-tooltip />
        <el-table-column prop="project" label="项目" width="100"><template #default="{ row }">{{ statusLabel(row.project) }}</template></el-table-column>
        <el-table-column prop="cycleMonths" label="周期(月)" width="80" />
        <el-table-column prop="lastDoneDate" label="上次完成" width="110"><template #default="{ row }">{{ fmtDate(row.lastDoneDate) }}</template></el-table-column>
        <el-table-column label="下次到期" width="120"><template #default="{ row }"><span :class="{ danger: daysUntil(row.nextDate)! < 0 }">{{ fmtDate(row.nextDate) }}</span></template></el-table-column>
        <el-table-column label="预警" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!readOnly && row.status === 'ACTIVE'" link type="success" @click="complete(row)">登记完成</el-button>
            <el-button link @click="pdf(row)">维保报告</el-button>
            <el-button v-if="!readOnly" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑维保计划' : '新增维保计划'" width="680px" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="设备 ID"><el-input-number v-model="form.equipmentId" style="width:100%" /></el-form-item>
        <el-form-item label="设备编号"><el-input v-model="form.equipmentNo" /></el-form-item>
        <el-form-item label="客户 ID"><el-input-number v-model="form.customerId" style="width:100%" /></el-form-item>
        <el-form-item label="客户名称"><el-input v-model="form.customerName" /></el-form-item>
        <el-form-item label="维保项目"><el-select v-model="form.project" style="width:100%"><el-option label="定期保养" value="MAINTAIN" /><el-option label="年检" value="ANNUAL" /><el-option label="定检" value="PERIODIC" /></el-select></el-form-item>
        <el-form-item label="周期（月）"><el-input-number v-model="form.cycleMonths" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="下次到期日"><el-date-picker v-model="form.nextDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="提醒天数"><el-input-number v-model="form.remindDays" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { downloadFile } from '@/api/http'
import { authStore } from '@/stores/auth'
import { daysUntil, fmtDate, statusLabel, statusPill } from '@/utils/format'

const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const project = ref(''); const warnOnly = ref(''); const loading = ref(false)
const warnings = ref<any[]>([])
const dialogVisible = ref(false); const editing = ref(false); const form = reactive<any>({})
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/maintenance', { params: { page: page.value, size: size.value, keyword: keyword.value, project: project.value, warn: warnOnly.value === 'true' ? true : undefined } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
async function loadWarnings() { try { warnings.value = await http.get('/admin/maintenance/warnings') } catch { warnings.value = [] } }
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; project.value = ''; warnOnly.value = ''; search() }
function openCreate() { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { project: 'MAINTAIN', cycleMonths: 6, remindDays: 30, status: 'ACTIVE' }); editing.value = false; dialogVisible.value = true }
function openEdit(row: any) { Object.assign(form, row); editing.value = true; dialogVisible.value = true }
async function save() {
  if (editing.value) await http.put(`/admin/maintenance/${form.id}`, form)
  else await http.post('/admin/maintenance', form)
  ElMessage.success('维保计划已保存'); dialogVisible.value = false; load(); loadWarnings()
}
async function complete(row: any) {
  await ElMessageBox.confirm(`确认登记 ${row.equipmentNo} ${statusLabel(row.project)} 已完成？系统将按周期顺延下次到期日。`, '维保完成')
  await http.post(`/admin/maintenance/${row.id}/complete`, {})
  ElMessage.success('已登记完成'); load(); loadWarnings()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该维保计划？', '危险操作', { type: 'warning' })
  await http.delete(`/admin/maintenance/${row.id}`); ElMessage.success('已删除'); load()
}
function pdf(row: any) { downloadFile(`/admin/maintenance/${row.id}/pdf`, `${row.equipmentNo || '维保'}-维保报告.pdf`) }
onMounted(() => { load(); loadWarnings() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
