<template>
  <div class="file-upload" :class="{ disabled }">
    <el-upload
      drag
      multiple
      :show-file-list="false"
      :http-request="upload"
      :disabled="disabled"
      accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.zip"
    >
      <div class="drop-zone" @drop.prevent>
        <el-icon class="drop-icon"><UploadFilled /></el-icon>
        <div class="drop-title">将证书 PDF / 合同扫描件 / 检测报告拖到这里，或点击选择</div>
        <div class="drop-sub">支持 PDF、Word、Excel、图片、ZIP，单文件不超过 30MB</div>
      </div>
    </el-upload>
    <div v-if="files.length" class="file-list">
      <div v-for="(file, index) in files" :key="file.url + index" class="file-row">
        <div class="file-icon"><el-icon><Document /></el-icon></div>
        <div class="file-main">
          <div class="file-name">{{ file.name || '附件' }}</div>
          <div class="file-meta">{{ file.sizeText || '—' }} <span v-if="file.uploading">上传中…</span></div>
        </div>
        <a v-if="file.url" :href="file.url" target="_blank" class="file-link">下载/预览</a>
        <el-button v-if="!disabled" link type="danger" @click="remove(index)">删除</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const props = withDefaults(defineProps<{
  modelValue?: string
  category?: string
  disabled?: boolean
}>(), {
  modelValue: '',
  category: 'attachment',
  disabled: false
})
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()
const files = ref<any[]>([])

watch(() => props.modelValue, (value) => {
  const current = files.value.filter((f) => f.url).map((f) => f.url).join(',')
  if (current !== (value || '')) {
    files.value = (value || '').split(',').map((v) => v.trim()).filter(Boolean).map((url) => ({
      name: decodeURIComponent(url.split('/').pop() || '附件'),
      url
    }))
  }
}, { immediate: true })

async function upload(options: any) {
  const file = options.file
  if (file.size > 30 * 1024 * 1024) {
    ElMessage.error('单文件不能超过 30MB')
    return
  }
  const item = {
    name: file.name,
    sizeText: (file.size / 1024 / 1024).toFixed(2) + ' MB',
    uploading: true,
    url: ''
  }
  files.value.push(item)
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', props.category)
  try {
    const record: any = await http.post('/admin/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    item.url = record.url
    item.name = record.originalName || file.name
    item.uploading = false
    sync()
  } catch (e: any) {
    files.value = files.value.filter((f) => f !== item)
    ElMessage.error('附件上传失败：' + (e?.message || '未知错误'))
  }
}
function remove(index: number) {
  files.value.splice(index, 1)
  sync()
}
function sync() {
  emit('update:modelValue', files.value.filter((f) => f.url).map((f) => f.url).join(','))
}
</script>

<style scoped>
.file-upload { width: 100%; }
.file-upload.disabled { opacity: .6; pointer-events: none; }
.drop-zone { padding: 22px 16px; text-align: center; }
.drop-icon { font-size: 34px; color: #8ba6c9; }
.drop-title { font-size: 13px; color: #354052; margin-top: 8px; }
.drop-sub { font-size: 11px; color: var(--xh-muted); margin-top: 4px; }
.file-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.file-row { display: flex; align-items: center; gap: 10px; border: 1px solid var(--xh-line); border-radius: 6px; padding: 8px 10px; background: #fff; }
.file-icon { width: 34px; height: 34px; border-radius: 6px; background: #f2f7ff; display: flex; align-items: center; justify-content: center; color: var(--xh-primary); }
.file-main { flex: 1; min-width: 0; }
.file-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-meta { font-size: 11px; color: var(--xh-muted); margin-top: 2px; }
.file-link { font-size: 12px; color: var(--xh-primary); }
:deep(.el-upload-dragger) { padding: 0; border-radius: 6px; background: #fbfdff; }
</style>
