<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">售前咨询</h1><p class="page-sub">在线/电话/到店咨询留痕，FAQ 初筛，咨询转需求并聚合到客户档案。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 录入咨询</el-button>
      </div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="问题 / 答复 / 客户" @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="状态" @change="search"><el-option label="待回复" value="WAITING" /><el-option label="已回复" value="DONE" /><el-option label="已转需求" value="CONVERTED" /></el-select>
        <el-select v-model="channel" clearable placeholder="渠道" @change="search"><el-option label="在线" value="ONLINE" /><el-option label="电话" value="PHONE" /><el-option label="到店" value="STORE" /></el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="customerName" label="客户" min-width="190" show-overflow-tooltip />
        <el-table-column prop="channel" label="渠道" width="80" />
        <el-table-column prop="question" label="问题" min-width="300" show-overflow-tooltip />
        <el-table-column prop="answer" label="答复" min-width="300" show-overflow-tooltip />
        <el-table-column prop="consultantName" label="顾问" width="90" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column prop="createdAt" label="时间" width="150"><template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">{{ row.status === 'WAITING' ? '回复' : '修改答复' }}</el-button>
            <el-button v-if="!readOnly && row.status !== 'CONVERTED'" link @click="openConvert(row)">转需求</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="replyVisible" title="回复咨询" width="680px" destroy-on-close>
      <div class="notice">{{ current?.question }}</div>
      <el-input v-model="answer" type="textarea" :rows="6" placeholder="请输入答复内容，将同步站内消息并推送客户档案" style="margin-top:14px" />
      <template #footer><el-button @click="replyVisible=false">取消</el-button><el-button type="primary" @click="reply">发送答复</el-button></template>
    </el-dialog>

    <el-dialog v-model="createVisible" title="录入售前咨询" width="720px" destroy-on-close>
      <el-form :model="createForm" label-position="top" class="form-two">
        <el-form-item label="客户 ID"><el-input-number v-model="createForm.customerId" style="width:100%" /></el-form-item>
        <el-form-item label="渠道"><el-select v-model="createForm.channel" style="width:100%"><el-option label="在线" value="ONLINE" /><el-option label="电话" value="PHONE" /><el-option label="到店" value="STORE" /></el-select></el-form-item>
        <el-form-item label="关联需求 ID"><el-input-number v-model="createForm.demandId" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="createForm.status" style="width:100%"><el-option label="待回复" value="WAITING" /><el-option label="已完成" value="DONE" /></el-select></el-form-item>
        <el-form-item label="咨询问题" class="full"><el-input v-model="createForm.question" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="答复" class="full"><el-input v-model="createForm.answer" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" @click="saveCreate">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="convertVisible" title="咨询转采购需求" width="820px" destroy-on-close>
      <el-form :model="demandForm" label-position="top" class="form-two">
        <el-form-item label="锅炉类型"><el-select v-model="demandForm.boilerType" style="width:100%"><el-option v-for="o in boilerTypeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="燃料类型"><el-select v-model="demandForm.fuelType" style="width:100%"><el-option v-for="o in fuelTypeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="蒸发量/热功率"><el-input v-model="demandForm.capacity" /></el-form-item>
        <el-form-item label="压力等级"><el-select v-model="demandForm.pressureLevel" style="width:100%"><el-option v-for="o in pressureOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="预算下限"><el-input-number v-model="demandForm.budgetMin" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="预算上限"><el-input-number v-model="demandForm.budgetMax" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="demandForm.region" /></el-form-item>
        <el-form-item label="用途说明" class="full"><el-input v-model="demandForm.usageDesc" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="convertVisible=false">取消</el-button><el-button type="primary" @click="convert">转需求</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import { boilerTypeOptions, fuelTypeOptions, pressureOptions } from '@/crud/options'
import { fmtDateTime, statusLabel, statusPill } from '@/utils/format'

const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const channel = ref(''); const loading = ref(false)
const current = ref<any>(null); const replyVisible = ref(false); const answer = ref('')
const createVisible = ref(false); const createForm = reactive<any>({ channel: 'ONLINE', status: 'WAITING' })
const convertVisible = ref(false); const demandForm = reactive<any>({ boilerType: 'steam', fuelType: 'gas' })
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/consultations', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value, channel: channel.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; status.value = ''; channel.value = ''; search() }
function openReply(row: any) { current.value = row; answer.value = row.answer || ''; replyVisible.value = true }
async function reply() {
  if (!answer.value) return ElMessage.warning('请输入答复内容')
  await http.post(`/admin/consultations/${current.value.id}/reply`, { answer: answer.value })
  ElMessage.success('答复已发送'); replyVisible.value = false; load()
}
function openCreate() { Object.keys(createForm).forEach((k) => delete createForm[k]); Object.assign(createForm, { channel: 'ONLINE', status: 'WAITING' }); createVisible.value = true }
async function saveCreate() { await http.post('/admin/consultations', createForm); ElMessage.success('咨询已录入'); createVisible.value = false; load() }
function openConvert(row: any) { current.value = row; Object.keys(demandForm).forEach((k) => delete demandForm[k]); Object.assign(demandForm, { boilerType: 'steam', fuelType: 'gas' }); convertVisible.value = true }
async function convert() {
  await http.post(`/admin/consultations/${current.value.id}/to-demand`, demandForm)
  ElMessage.success('已转换为需求线索'); convertVisible.value = false; load()
}
async function remove(row: any) {
  await ElMessageBox.confirm(
    `确认删除咨询记录「${row.customerName || row.question}」？删除后不可恢复。`,
    '删除咨询',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/consultations/${row.id}`)
  ElMessage.success('咨询记录已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
