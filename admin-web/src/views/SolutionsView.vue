<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">方案与案例</h1><p class="page-sub">选型推荐、方案版本、客户确认与分享留痕；支持成功案例库。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 创建方案</el-button>
        <el-button @click="router.push('/cases')"><el-icon><Picture /></el-icon> 成功案例库</el-button>
      </div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="方案编号 / 名称 / 产品" @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="状态" @change="search"><el-option label="草稿" value="DRAFT" /><el-option label="已推送" value="PUSHED" /><el-option label="客户已确认" value="CONFIRMED" /></el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="solutionNo" label="方案编号" width="200" />
        <el-table-column prop="name" label="方案名称" min-width="260" />
        <el-table-column prop="productName" label="选型产品" width="150" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column prop="viewCount" label="查看" width="70" />
        <el-table-column label="客户确认" width="150"><template #default="{ row }">{{ fmtDateTime(row.confirmTime) }}</template></el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">查看</el-button>
            <el-button v-if="!readOnly" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!readOnly && row.status === 'DRAFT'" link type="success" @click="send(row)">推送客户</el-button>
            <el-button v-if="!readOnly" link @click="newVersion(row)">新版本</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="removeSolution(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑方案' : '创建方案'" width="900px" top="4vh" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="关联需求" required>
          <el-select v-model="form.demandId" filterable style="width:100%" placeholder="选择需求线索" :disabled="editing">
            <el-option v-for="d in demands" :key="d.id" :label="`${d.demandNo} · ${d.contactName} · ${statusLabel(d.boilerType)}${d.capacity}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选型产品">
          <el-select v-model="form.productId" filterable style="width:100%" @change="onProduct">
            <el-option v-for="p in products" :key="p.id" :label="`${p.model} · ${p.capacity} · ${statusLabel(p.fuelType)}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="方案名称" class="full" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="方案正文" class="full"><RichTextEditor v-model="form.content" height="340px" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="viewVisible" title="方案详情与版本" size="760px">
      <template v-if="detail">
        <div class="page-head" style="margin-bottom:10px">
          <div><h2 style="margin:0">{{ detail.solution.name }}</h2><p class="page-sub mono">{{ detail.solution.solutionNo }} · 版本 {{ detail.solution.version }}</p></div>
          <el-button v-if="!readOnly && detail.solution.status === 'DRAFT'" type="success" @click="send(detail.solution)">推送客户</el-button>
        </div>
        <div class="notice" v-if="detail.solution.status !== 'CONFIRMED'">分享链接：{{ shareLink(detail.solution) }}</div>
        <div class="rich-content" v-html="sanitizeHtml(detail.solution.content)"></div>
        <h3 class="card-title" style="margin-top:24px">历史版本</h3>
        <el-table :data="detail.versions" size="small" stripe>
          <el-table-column prop="version" label="版本" width="70" />
          <el-table-column prop="solutionNo" label="方案编号" width="200" />
          <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
          <el-table-column label="确认时间" width="150"><template #default="{ row }">{{ fmtDateTime(row.confirmTime) }}</template></el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import { fmtDateTime, sanitizeHtml, statusLabel, statusPill } from '@/utils/format'
import RichTextEditor from '@/components/RichTextEditor.vue'

const router = useRouter(); const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const loading = ref(false)
const dialogVisible = ref(false); const editing = ref(false); const form = reactive<any>({})
const products = ref<any[]>([]); const demands = ref<any[]>([])
const viewVisible = ref(false); const detail = ref<any>(null)
function shareLink(s: any) { return `${location.origin}/share/solution?token=${s.shareToken}` }
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/solutions', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } })
    rows.value = data.records; total.value = data.total
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
  Object.assign(form, { content: '<h2>项目概述</h2><p></p><h2>选型依据</h2><p></p><h2>合规材料</h2><p>制造许可、产品监检证明、能效测试报告齐全。</p>' })
  editing.value = false; dialogVisible.value = true
}
function openEdit(row: any) { Object.assign(form, row); editing.value = true; dialogVisible.value = true }
function onProduct(id: number) { if (!form.name) form.name = `${products.value.find((p) => p.id === id)?.model} 锅炉方案` }
async function save() {
  if (!form.demandId || !form.name) return ElMessage.warning('关联需求与方案名称必填')
  if (editing.value) await http.put(`/admin/solutions/${form.id}`, form)
  else await http.post('/admin/solutions', form)
  ElMessage.success('方案已保存'); dialogVisible.value = false; load()
}
async function openView(row: any) {
  detail.value = await http.get(`/admin/solutions/${row.id}`); viewVisible.value = true
}
async function send(row: any) {
  await ElMessageBox.confirm('推送后客户可在门户/小程序查看，并收到站内与公众号模板通知。确认推送？', '推送方案')
  await http.post(`/admin/solutions/${row.id}/send`); ElMessage.success('方案已推送'); load()
}
async function newVersion(row: any) {
  const data: any = await http.post(`/admin/solutions/${row.id}/new-version`)
  ElMessage.success(`已创建版本 ${data.version}`); load()
}
async function removeSolution(row: any) {
  await ElMessageBox.confirm(
    `确认删除方案「${row.name}」？删除后不可恢复。已关联报价的方案不可删除，请先删除报价。`,
    '删除方案',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/solutions/${row.id}`)
  ElMessage.success('方案已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load()
}
onMounted(() => { load(); loadOptions() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
