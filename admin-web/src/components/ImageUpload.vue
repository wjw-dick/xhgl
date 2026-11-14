<template>
  <div class="image-upload" :class="{ disabled }">
    <div class="drop-hint" @dragover.prevent @drop.prevent="onDrop">
      <el-upload
        v-model:file-list="fileList"
        list-type="picture-card"
        :limit="limit"
        :disabled="disabled"
        :http-request="upload"
        :on-success="onSuccess"
        :on-remove="onRemove"
        :on-preview="onPreview"
        :on-exceed="() => ElMessage.warning(`最多上传 ${limit} 张图片`)"
        accept="image/*"
      >
        <el-icon><Plus /></el-icon>
        <template #tip>
          <div class="upload-tip">
            <span>拖拽图片到此区域，或点击上传；支持多图、缩略图预览、删除，自动压缩前请自查清晰度。</span>
            <span>{{ fileList.length }}/{{ limit }}</span>
          </div>
        </template>
      </el-upload>
    </div>
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const props = withDefaults(defineProps<{
  modelValue?: string
  limit?: number
  category?: string
  disabled?: boolean
}>(), {
  modelValue: '',
  limit: 8,
  category: 'product',
  disabled: false
})
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()
const fileList = ref<any[]>([])
const previewVisible = ref(false)
const previewList = ref<string[]>([])
const previewIndex = ref(0)

function buildFileList(value: string) {
  return (value || '').split(',').map((v) => v.trim()).filter(Boolean).map((url, index) => ({
    name: `图片${index + 1}`,
    url,
    uid: Date.now() + '-' + index,
    status: 'success'
  }))
}
watch(() => props.modelValue, (value) => {
  const current = fileList.value.map((f) => f.url).filter(Boolean).join(',')
  if (current !== (value || '')) fileList.value = buildFileList(value)
}, { immediate: true })

async function upload(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('category', props.category)
  try {
    options.onProgress({ percent: 30 })
    const record: any = await http.post('/admin/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    options.onProgress({ percent: 100 })
    options.onSuccess(record)
  } catch (e: any) {
    options.onError(e)
    ElMessage.error('上传失败：' + (e?.message || '未知错误'))
  }
}

function onSuccess(response: any, uploadFile: any) {
  uploadFile.url = response?.url || response?.data?.url
  sync()
}
function onRemove() {
  sync()
}
function sync() {
  const urls = fileList.value.map((f) => f.url).filter(Boolean).join(',')
  emit('update:modelValue', urls)
}
function onPreview(file: any) {
  previewList.value = fileList.value.map((f) => f.url).filter(Boolean)
  previewIndex.value = Math.max(0, previewList.value.indexOf(file.url))
  previewVisible.value = true
}
function onDrop(event: DragEvent) {
  if (props.disabled) return
  const files = Array.from(event.dataTransfer?.files || []).filter((f) => f.type.startsWith('image/'))
  if (!files.length) return
  const slots = props.limit - fileList.value.length
  files.slice(0, Math.max(0, slots)).forEach((file) => {
    const uploader: any = (document.querySelector('.image-upload .el-upload input[type=file]'))
    if (uploader) {
      const dt = new DataTransfer()
      dt.items.add(file)
      uploader.files = dt.files
      uploader.dispatchEvent(new Event('change', { bubbles: true }))
    }
  })
}
</script>

<style scoped>
.image-upload { border: 1px dashed #d7e0eb; border-radius: 6px; padding: 14px; background: #fbfdff; width: 100%; }
.image-upload.disabled { opacity: .65; pointer-events: none; }
.drop-hint { width: 100%; }
.upload-tip { display: flex; justify-content: space-between; gap: 12px; color: var(--xh-muted); font-size: 11px; line-height: 1.6; margin-top: 6px; }
:deep(.el-upload--picture-card), :deep(.el-upload-list--picture-card .el-upload-list__item) { width: 92px; height: 92px; margin: 0 8px 8px 0; }
:deep(.el-upload-list--picture-card) { display: flex; flex-wrap: wrap; }
</style>
