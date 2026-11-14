<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">库存流水</h1><p class="page-sub">备件入库、出库、领用与盘点记录，关联工单可追溯。</p></div>
      <el-button @click="router.push('/spares')"><el-icon><ArrowLeft /></el-icon> 返回备件档案</el-button>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-select v-model="type" clearable placeholder="操作类型" @change="search"><el-option label="入库" value="IN" /><el-option label="出库/领用" value="OUT" /><el-option label="盘点" value="STOCKTAKE" /></el-select>
        <el-input v-model="partId" clearable placeholder="备件 ID" @keyup.enter="search" />
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="createdAt" label="时间" width="170"><template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="partId" label="备件ID" width="90" />
        <el-table-column label="类型" width="100"><template #default="{ row }">{{ {IN:'入库',OUT:'领用',STOCKTAKE:'盘点'}[row.type] || row.type }}</template></el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column prop="ticketId" label="关联工单" width="110" />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="stockAfter" label="结存" width="90" />
        <el-table-column prop="remark" label="备注" min-width="200" />
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { fmtDateTime } from '@/utils/format'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const type = ref(''); const partId = ref(''); const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/spare-records', { params: { page: page.value, size: size.value, type: type.value, partId: partId.value || undefined } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { type.value = ''; partId.value = ''; search() }
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
