<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">公告管理</h1><p class="page-sub">系统公告只读，可发布企业公告；门户、H5 与小程序同步展示。</p></div>
      <el-button type="primary" @click="dialogVisible=true"><el-icon><Plus /></el-icon> 发布企业公告</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="300" />
        <el-table-column prop="source" label="来源" width="100"><template #default="{ row }"><span class="pill">{{ row.source === 'SYSTEM' ? '系统' : '企业' }}</span></template></el-table-column>
        <el-table-column label="发布时间" width="170"><template #default="{ row }">{{ fmtDateTime(row.publishTime) }}</template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="row.source === 'COMPANY'" link type="danger" @click="remove(row)">删除</el-button><span v-else class="muted">只读</span></template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
    </div>
    <el-dialog v-model="dialogVisible" title="发布企业公告" width="820px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="正文"><RichTextEditor v-model="form.content" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">发布</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { fmtDateTime, statusLabel, statusPill } from '@/utils/format'

const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const loading = ref(false); const dialogVisible = ref(false); const form = reactive({ title: '', content: '' })
async function load() {
  loading.value = true
  try { const data: any = await http.get('/admin/announcements', { params: { page: page.value, size: size.value } }); rows.value = data.records; total.value = data.total } finally { loading.value = false }
}
async function save() { if (!form.title) return ElMessage.warning('标题必填'); await http.post('/admin/announcements', form); ElMessage.success('公告已发布'); dialogVisible.value = false; form.title = ''; form.content = ''; load() }
async function remove(row: any) { await ElMessageBox.confirm('确认删除该公告？', '危险操作', { type: 'warning' }); await http.delete(`/admin/announcements/${row.id}`); ElMessage.success('已删除'); load() }
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
