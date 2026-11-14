<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">备品备件</h1><p class="page-sub">备件档案、安全库存、入库/出库/盘点与领用关联工单；低于安全库存自动预警。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增备件</el-button>
        <el-button @click="router.push('/spare-records')"><el-icon><Tickets /></el-icon> 库存流水</el-button>
      </div>
    </div>
    <div v-if="alerts.length" class="card">
      <h3 class="card-title"><el-icon color="#e5484d"><Warning /></el-icon> 库存预警</h3>
      <div class="alerts">
        <div v-for="p in alerts" :key="p.id" class="alert-item">
          <span>{{ p.name }}（{{ p.code }}）</span>
          <strong class="danger">库存 {{ p.stock }} / 安全 {{ p.safetyStock }}</strong>
        </div>
      </div>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="编码 / 名称 / 型号 / 供应商" @keyup.enter="search" />
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="code" label="备件编码" width="150" />
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="spec" label="规格" width="130" />
        <el-table-column prop="supplier" label="供应商" min-width="170" show-overflow-tooltip />
        <el-table-column label="单价" width="110"><template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template></el-table-column>
        <el-table-column label="库存" width="100"><template #default="{ row }"><span :class="{ danger: row.stock < row.safetyStock }">{{ row.stock }}</span></template></el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!readOnly" link type="primary" @click="openStock(row, 'IN')">入库</el-button>
            <el-button v-if="!readOnly" link type="warning" @click="openStock(row, 'OUT')">领用</el-button>
            <el-button v-if="!readOnly" link @click="openStock(row, 'STOCKTAKE')">盘点</el-button>
            <el-button v-if="!readOnly" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑备件' : '新增备件'" width="760px">
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="备件编码" required><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="供应商"><el-input v-model="form.supplier" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="form.unitPrice" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="form.safetyStock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="初始库存" v-if="!editing"><el-input-number v-model="form.stock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="备件图片" class="full"><ImageUpload v-model="form.image" category="spare" :limit="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="stockVisible" :title="stockTitle" width="480px">
      <el-form label-position="top">
        <el-form-item :label="stockType === 'STOCKTAKE' ? '盘点后库存' : '数量'">
          <el-input-number v-model="stockForm.quantity" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联工单 ID"><el-input-number v-model="stockForm.ticketId" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="stockForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="stockVisible=false">取消</el-button><el-button type="primary" @click="doStock">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import ImageUpload from '@/components/ImageUpload.vue'
import { fmtMoney } from '@/utils/format'

const router = useRouter(); const store = authStore(); const readOnly = computed(() => false)
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const loading = ref(false); const alerts = ref<any[]>([])
const dialogVisible = ref(false); const editing = ref(false); const form = reactive<any>({})
const stockVisible = ref(false); const stockForm = reactive<any>({}); const stockType = ref('IN'); const currentPart = ref<any>(null)
const stockTitle = computed(() => ({ IN: '备件入库', OUT: '备件领用', STOCKTAKE: '库存盘点' }[stockType.value] || '库存操作'))
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/spares', { params: { page: page.value, size: size.value, keyword: keyword.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
async function loadAlerts() { try { alerts.value = await http.get('/admin/spare-records/alerts') } catch { alerts.value = [] } }
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; search() }
function openCreate() { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { stock: 0, safetyStock: 5, status: 'NORMAL' }); editing.value = false; dialogVisible.value = true }
function openEdit(row: any) { Object.assign(form, row); editing.value = true; dialogVisible.value = true }
async function save() {
  if (!form.code || !form.name) return ElMessage.warning('备件编码与名称必填')
  if (editing.value) await http.put(`/admin/spares/${form.id}`, form)
  else await http.post('/admin/spares', form)
  ElMessage.success('备件已保存'); dialogVisible.value = false; load(); loadAlerts()
}
function openStock(row: any, type: string) { currentPart.value = row; stockType.value = type; Object.keys(stockForm).forEach((k) => delete stockForm[k]); stockForm.quantity = type === 'STOCKTAKE' ? row.stock : 1; stockVisible.value = true }
async function doStock() {
  await http.post('/admin/spare-records/operation', { partId: currentPart.value.id, type: stockType.value, ...stockForm })
  ElMessage.success('库存操作成功'); stockVisible.value = false; load(); loadAlerts()
}
onMounted(() => { load(); loadAlerts() })
</script>
<style scoped>
.pager{display:flex;justify-content:flex-end;padding:14px}
.alerts{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:8px}
.alert-item{border:1px solid #fde2e2;background:#fff7f7;border-radius:6px;padding:10px;display:flex;justify-content:space-between;gap:8px;font-size:12px}
</style>
