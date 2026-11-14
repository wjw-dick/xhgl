<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">多语言</h1><p class="page-sub">语言启用/停用与默认语言；内容编辑保留中/EN 双语录入入口。</p></div>
      <el-button type="primary" @click="dialogVisible=true"><el-icon><Plus /></el-icon> 新增语言</el-button>
    </div>
    <div class="card">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="code" label="语言代码" width="130" />
        <el-table-column prop="name" label="语言名称" width="180" />
        <el-table-column prop="defaultFlag" label="默认语言" width="100"><template #default="{ row }"><span :class="['pill', row.defaultFlag === 'Y' ? 'green' : 'gray']">{{ row.defaultFlag === 'Y' ? '是' : '否' }}</span></template></el-table-column>
        <el-table-column label="启用" width="120">
          <template #default="{ row }"><el-switch :model-value="row.enabled === 1" @change="(v: boolean) => toggle(row, v)" /></template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
      </el-table>
    </div>
    <div class="card">
      <h3 class="card-title">内容多语言说明</h3>
      <div class="notice">
        默认简体中文。产品、方案、案例、公告等内容在编辑页提供「中 / EN」小 tab；未填语言项在保存前红点提示。前台顶栏语言切换将同步 H5、portal-web 与小程序文案。
      </div>
      <div class="lang-preview">
        <div v-for="row in rows" :key="row.code" class="lang-card" :class="{ disabled: row.enabled !== 1 }">
          <strong>{{ row.name }}</strong><span>{{ row.code }}</span>
          <div class="red-dot" v-if="row.code !== 'zh-CN' && row.enabled === 1" title="部分内容未录入"></div>
        </div>
      </div>
    </div>
    <el-dialog v-model="dialogVisible" title="新增语言" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="语言代码"><el-input v-model="form.code" placeholder="如 en / ja" /></el-form-item>
        <el-form-item label="语言名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const rows = ref<any[]>([]); const loading = ref(false); const dialogVisible = ref(false)
const form = reactive<any>({ sort: 99 })
async function load() { loading.value = true; try { rows.value = await http.get('/admin/settings/languages') } finally { loading.value = false } }
async function toggle(row: any, value: boolean) { await http.put(`/admin/settings/languages/${row.id}`, { enabled: value ? 1 : 0 }); row.enabled = value ? 1 : 0; ElMessage.success('已更新') }
async function save() { if (!form.code || !form.name) return ElMessage.warning('代码和名称必填'); await http.post('/admin/settings/languages', form); ElMessage.success('语言已新增'); dialogVisible.value = false; form.code = ''; form.name = ''; load() }
onMounted(load)
</script>
<style scoped>
.lang-preview{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:10px;margin-top:16px}
.lang-card{position:relative;border:1px solid var(--xh-line);border-radius:6px;padding:12px;display:flex;justify-content:space-between;gap:8px}
.lang-card.disabled{opacity:.55}
.red-dot{position:absolute;right:8px;top:8px;width:7px;height:7px;border-radius:50%;background:#e5484d}
</style>
