<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">客户档案</h1><p class="page-sub">一企一档，联系人多人维护；关联需求、方案、报价、合同、回款与售后全视图。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增客户</el-button>
        <el-upload v-if="!readOnly" :show-file-list="false" :http-request="importCustomers" accept=".xlsx,.xls">
          <el-button><el-icon><Upload /></el-icon> 批量导入</el-button>
        </el-upload>
        <el-button @click="exportCustomers"><el-icon><Download /></el-icon> 导出</el-button>
      </div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="企业名称 / 客户编号 / 地址" @keyup.enter="search"><template #prefix><el-icon><Search /></el-icon></template></el-input>
        <el-select v-model="status" clearable placeholder="客户状态" @change="search"><el-option label="正常" value="NORMAL" /><el-option label="停用" value="DISABLED" /></el-select>
        <el-select v-model="industry" clearable placeholder="行业" @change="search">
          <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
        </el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-skeleton v-if="loading && rows.length===0" :rows="6" animated style="padding:24px" />
      <el-table v-else :data="rows" stripe>
        <el-table-column prop="customerNo" label="客户编号" width="180" />
        <el-table-column label="企业名称" min-width="220">
          <template #default="{ row }"><el-link type="primary" @click="router.push(`/customers/${row.id}`)">{{ row.name }}</el-link></template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="120" />
        <el-table-column prop="scale" label="规模" width="90" />
        <el-table-column prop="region" label="地区" width="130" />
        <el-table-column label="来源" width="90"><template #default="{ row }">{{ sourceLabel(row.source) }}</template></el-table-column>
        <el-table-column prop="level" label="级别" width="70"><template #default="{ row }"><span class="pill">{{ row.level || '—' }}</span></template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/customers/${row.id}`)">关联视图</el-button>
            <el-button v-if="!readOnly" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!readOnly && row.status !== 'DISABLED'" link type="danger" @click="disable(row)">禁用</el-button>
            <el-button v-if="!readOnly" link type="danger" @click="removeCustomer(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑客户' : '新增客户'" width="820px" top="5vh" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="企业名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="所属行业"><el-input v-model="form.industry" /></el-form-item>
        <el-form-item label="企业规模"><el-select v-model="form.scale" clearable style="width:100%"><el-option v-for="s in ['大型','中型','小型']" :key="s" :label="s" :value="s" /></el-select></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="客户来源"><el-select v-model="form.source" style="width:100%">
          <el-option label="H5门户" value="H5" /><el-option label="小程序" value="MP" /><el-option label="公众号" value="OA" /><el-option label="电话" value="PHONE" /><el-option label="人工录入" value="MANUAL" />
        </el-select></el-form-item>
        <el-form-item label="客户分类"><el-input v-model="form.category" placeholder="重点客户 / 潜力客户" /></el-form-item>
        <el-form-item label="客户级别"><el-select v-model="form.level" style="width:100%"><el-option label="A" value="A" /><el-option label="B" value="B" /><el-option label="C" value="C" /></el-select></el-form-item>
        <el-form-item label="负责人 ID"><el-input-number v-model="form.ownerId" style="width:100%" /></el-form-item>
        <el-form-item label="详细地址" class="full"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { downloadFile } from '@/api/http'
import { authStore } from '@/stores/auth'
import { statusLabel, statusPill } from '@/utils/format'

const router = useRouter()
const store = authStore()
const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const status = ref(''); const industry = ref(''); const loading = ref(false)
const dialogVisible = ref(false); const editing = ref(false); const saving = ref(false); const form = reactive<any>({})
const industries = ['食品加工', '化工', '医药', '造纸', '纺织', '食品饮料', '建材', '集中供热', '汽车零部件']
function sourceLabel(source?: string) {
  const map: Record<string, string> = {
    REGISTER: '客户注册', H5: 'H5门户', MP: '小程序', OA: '公众号',
    PHONE: '电话', MANUAL: '人工录入', WECHAT: '微信'
  }
  return source ? (map[source] || source) : '—'
}

async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/customers', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value, industry: industry.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; status.value = ''; industry.value = ''; search() }
function openCreate() {
  editing.value = false
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { status: 'NORMAL', level: 'B', source: 'MANUAL' })
  dialogVisible.value = true
}
function openEdit(row: any) { editing.value = true; Object.assign(form, row); dialogVisible.value = true }
async function save() {
  if (!form.name) return ElMessage.warning('企业名称必填')
  if (editing.value) await http.put(`/admin/customers/${form.id}`, form)
  else await http.post('/admin/customers', form)
  ElMessage.success('客户档案已保存'); dialogVisible.value = false; load()
}
async function disable(row: any) {
  await ElMessageBox.confirm(`确认禁用客户 ${row.name}？禁用后不可新增业务。`, '危险操作', { type: 'warning' })
  await http.post(`/admin/customers/${row.id}/disable`); ElMessage.success('已禁用'); load()
}
async function removeCustomer(row: any) {
  await ElMessageBox.confirm(
    `确认删除客户档案「${row.name}」？将从数据库物理删除该客户、登录账号与联系人，后续可使用相同信息重新注册。已关联业务数据的客户不可删除，请先处理业务或改为禁用。`,
    '删除客户',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/customers/${row.id}`)
  ElMessage.success('客户档案已删除')
  if (rows.value.length === 1 && page.value > 1) page.value--
  load()
}
function exportCustomers() { downloadFile('/admin/customers/export', '客户档案.xlsx') }
async function importCustomers(options: any) {
  const fd = new FormData(); fd.append('file', options.file)
  try {
    const result: any = await http.post('/admin/customers/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success(`导入完成：成功 ${result.success}，失败 ${result.failed}`)
    if (result.errors?.length) ElMessageBox.alert(result.errors.join('\n'), '导入失败明细', { type: 'warning' })
    load()
  } catch (e: any) { ElMessage.error('导入失败：' + (e?.message || '未知错误')) }
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
