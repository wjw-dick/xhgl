<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ config.title }}</h1>
        <p class="page-sub">{{ config.subtitle }}</p>
      </div>
      <div class="toolbar" style="margin: 0">
        <el-button v-if="!readOnly" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
        <el-upload
          v-if="config.importable && !readOnly"
          :show-file-list="false"
          :http-request="importRequest"
          accept=".xlsx,.xls"
        >
          <el-button><el-icon><Upload /></el-icon> 批量导入</el-button>
        </el-upload>
        <el-button v-if="config.exportable" @click="exportData">
          <el-icon><Download /></el-icon> 导出 Excel
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-head">
        <el-button link type="primary" @click="filterOpen = !filterOpen">
          <el-icon><Filter /></el-icon> 筛选
          <el-icon><ArrowUp v-if="filterOpen" /><ArrowDown v-else /></el-icon>
        </el-button>
      </div>
      <div v-show="filterOpen" class="filter-grid">
        <el-input v-model="keyword" clearable :placeholder="config.keywordPlaceholder || '关键词搜索'" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <template v-for="f in config.filters" :key="f.prop">
          <el-select v-if="f.type === 'select'" v-model="filters[f.prop]" clearable :placeholder="f.label" @change="search">
            <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-input v-else v-model="filters[f.prop]" clearable :placeholder="f.label" @keyup.enter="search" />
        </template>
      </div>
      <div class="filter-actions">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="card" style="padding: 0">
      <el-skeleton v-if="loading && rows.length === 0" :rows="6" animated style="padding: 24px" />
      <el-table v-else :data="rows" stripe style="width: 100%" empty-text="暂无数据，点击右上角新增">
        <el-table-column
          v-for="col in config.columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.width ? undefined : 140"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-image
              v-if="col.format === 'image'"
              :src="firstImage(row[col.prop])"
              :preview-src-list="imageList(row[col.prop])"
              fit="cover"
              style="width: 46px; height: 46px; border-radius: 6px"
              preview-teleported
            >
              <template #error><div class="img-placeholder"><el-icon><Box /></el-icon></div></template>
            </el-image>
            <span v-else-if="col.format === 'status'" :class="['pill', statusPill(row[col.prop])]">
              {{ statusLabel(row[col.prop]) }}
            </span>
            <span v-else-if="col.format === 'money'" class="money">{{ fmtMoney(row[col.prop]) }}</span>
            <span v-else-if="col.format === 'date'">{{ fmtDate(row[col.prop]) }}</span>
            <span v-else-if="col.format === 'datetime'">{{ fmtDateTime(row[col.prop]) }}</span>
            <span v-else-if="col.format === 'number'">{{ fmtNumber(row[col.prop]) }}</span>
            <span v-else>{{ row[col.prop] ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!readOnly" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑${config.title}` : `新增${config.title}`"
      width="880px"
      top="4vh"
      destroy-on-close
      class="form-dialog"
    >
      <el-form :model="form" label-position="top" class="form-two" ref="formRef">
        <el-form-item
          v-for="field in config.fields"
          :key="field.prop"
          :label="field.label"
          :class="{ full: field.full || ['richtext', 'images', 'file'].includes(field.type || '') }"
          :required="field.required"
        >
          <el-select
            v-if="field.type === 'select'"
            v-model="form[field.prop]"
            :placeholder="field.placeholder || `请选择${field.label}`"
            clearable
            style="width: 100%"
          >
            <el-option v-for="o in field.options" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form[field.prop]"
            :rows="field.rows || 3"
            :placeholder="field.placeholder || `请输入${field.label}`"
            type="textarea"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form[field.prop]"
            :placeholder="field.placeholder || `请输入${field.label}`"
            style="width: 100%"
            controls-position="right"
          />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="field.placeholder || `请选择${field.label}`"
            style="width: 100%"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="form[field.prop]"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="field.placeholder || `请选择${field.label}`"
            style="width: 100%"
          />
          <RichTextEditor v-else-if="field.type === 'richtext'" v-model="form[field.prop]" />
          <ImageUpload v-else-if="field.type === 'images'" v-model="form[field.prop]" :category="field.category || config.key" />
          <FileUpload v-else-if="field.type === 'file'" v-model="form[field.prop]" :category="field.category || config.key" />
          <el-input
            v-else
            v-model="form[field.prop]"
            :placeholder="field.placeholder || `请输入${field.label}`"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { downloadFile } from '@/api/http'
import { authStore } from '@/stores/auth'
import type { CrudConfig } from '@/crud/configs'
import { fmtDate, fmtDateTime, fmtMoney, fmtNumber, statusLabel, statusPill } from '@/utils/format'
import RichTextEditor from '@/components/RichTextEditor.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import FileUpload from '@/components/FileUpload.vue'

const props = defineProps<{ config: CrudConfig }>()
const store = authStore()
const readOnly = computed(() => false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const filters = reactive<Record<string, any>>({})
const filterOpen = ref(true)
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const form = ref<any>({})
const formRef = ref<any>()

function firstImage(value?: string) {
  return (value || '').split(',').filter(Boolean)[0] || ''
}
function imageList(value?: string) {
  return (value || '').split(',').filter(Boolean)
}
function resetForm() {
  form.value = { ...(props.config.defaults || {}) }
  props.config.fields.forEach((f) => {
    if (!(f.prop in form.value)) form.value[f.prop] = f.type === 'number' ? undefined : ''
  })
}
function openCreate() {
  editing.value = false
  resetForm()
  dialogVisible.value = true
}
function openEdit(row: any) {
  editing.value = true
  form.value = { ...row }
  dialogVisible.value = true
}
async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) params[k] = v
    })
    const data: any = await http.get(`/admin/${props.config.api}`, { params })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
function search() {
  page.value = 1
  load()
}
function resetFilters() {
  keyword.value = ''
  Object.keys(filters).forEach((k) => delete filters[k])
  search()
}
async function save() {
  for (const field of props.config.fields) {
    if (field.required && !form.value[field.prop]) {
      ElMessage.warning(`请填写${field.label}`)
      return
    }
  }
  saving.value = true
  try {
    const payload = { ...form.value }
    delete payload.id
    delete payload.createdAt; delete payload.updatedAt; delete payload.deleted
    if (editing.value) {
      await http.put(`/admin/${props.config.api}/${form.value.id}`, payload)
    } else {
      await http.post(`/admin/${props.config.api}`, payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除该条${props.config.title}记录？删除后不可恢复。`, '危险操作', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger'
  })
  await http.delete(`/admin/${props.config.api}/${row.id}`)
  ElMessage.success('已删除')
  load()
}
function exportData() {
  const params = new URLSearchParams()
  if (keyword.value) params.set('keyword', keyword.value)
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) params.set(k, String(v))
  })
  downloadFile(`/admin/${props.config.api}/export?${params.toString()}`, `${props.config.title}.xlsx`)
}
async function importRequest(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const result: any = await http.post(`/admin/${props.config.api}/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(`导入完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
    if (result.errors?.length) {
      ElMessageBox.alert(result.errors.join('\n'), '导入失败明细', { type: 'warning' })
    }
    load()
  } catch (e: any) {
    ElMessage.error('导入失败：' + (e?.message || '未知错误'))
  }
}

onMounted(() => {
  props.config.filters.forEach((f) => filters[f.prop] = '')
  load()
})
</script>

<style scoped>
.filter-head { display: flex; justify-content: flex-start; }
.pager { display: flex; justify-content: flex-end; padding: 14px 16px; }
.img-placeholder { width: 46px; height: 46px; background: #f1f5fa; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #9aa4b2; }
.form-dialog :deep(.el-dialog__body) { max-height: 72vh; overflow-y: auto; }
</style>
