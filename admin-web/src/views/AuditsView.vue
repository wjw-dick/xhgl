<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">审计日志</h1><p class="page-sub">系统全部关键操作留痕，可检索导出；越权访问同样记录。</p></div>
      <el-button @click="downloadFile('/admin/audits/export', '审计日志.xlsx')"><el-icon><Download /></el-icon> 导出</el-button>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="动作 / 对象 / 详情" @keyup.enter="search" />
        <el-input v-model="username" clearable placeholder="操作人" @keyup.enter="search" />
        <el-input v-model="module" clearable placeholder="模块（如 客户档案）" @keyup.enter="search" />
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="createdAt" label="时间" width="170"><template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="action" label="动作" width="160" />
        <el-table-column prop="target" label="对象" width="170" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http, { downloadFile } from '@/api/http'
import { fmtDateTime } from '@/utils/format'

const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const username = ref(''); const module = ref(''); const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/audits', { params: { page: page.value, size: size.value, keyword: keyword.value, username: username.value, module: module.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; username.value = ''; module.value = ''; search() }
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
