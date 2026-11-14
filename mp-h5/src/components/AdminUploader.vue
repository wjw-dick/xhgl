<template>
  <div class="admin-uploader">
    <div v-if="urls.length" class="upload-list">
      <div v-for="(url, i) in urls" :key="url" class="upload-item">
        <img v-if="image" :src="url" :alt="`上传文件${i + 1}`" />
        <span v-else class="file-name">{{ decodeURIComponent(url.split('/').pop() || '附件') }}</span>
        <button type="button" class="remove" @click="remove(i)">×</button>
      </div>
    </div>
    <button v-if="!image || urls.length < limit" type="button" class="upload-btn" :disabled="uploading" @click="pick">
      {{ uploading ? '上传中…' : image ? '＋ 上传图片' : '＋ 上传附件' }}
    </button>
    <input ref="input" type="file" :accept="image ? 'image/*' : undefined" :multiple="image" hidden @change="onFile" />
    <div v-if="urls.length" class="upload-hint">{{ urls.length }}/{{ image ? limit : 1 }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import http from '@/api/http'
import { toast } from '@/ui/toast'

const props = withDefaults(defineProps<{ modelValue?: string; image?: boolean; limit?: number; category?: string }>(), {
  modelValue: '', image: true, limit: 8, category: 'admin'
})
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()
const input = ref<HTMLInputElement>(); const uploading = ref(false)
const urls = computed(() => (props.modelValue || '').split(',').map((x) => x.trim()).filter(Boolean))
function pick() { input.value?.click() }
async function onFile(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (!files.length) return
  uploading.value = true
  try {
    const next = [...urls.value]
    for (const file of files.slice(0, props.image ? props.limit - next.length : 1)) {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('category', props.category)
      const record: any = await http.post('/admin/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      if (record?.url) next.push(record.url)
    }
    emit('update:modelValue', next.join(','))
  } catch (e: any) {
    toast(e?.message || '上传失败', 'error')
  } finally {
    uploading.value = false
    target.value = ''
  }
}
function remove(index: number) {
  const next = urls.value.filter((_, i) => i !== index)
  emit('update:modelValue', next.join(','))
}
</script>

<style scoped>
.admin-uploader { width: 100%; }
.upload-list { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.upload-item { position: relative; width: 72px; height: 72px; border-radius: 8px; overflow: hidden; border: 1px solid var(--line); background: #f8fafc; }
.upload-item img { width: 100%; height: 100%; object-fit: cover; }
.file-name { display: block; padding: 8px; font-size: 9px; color: var(--steel); word-break: break-all; }
.remove { position: absolute; top: 2px; right: 2px; width: 18px; height: 18px; border: none; border-radius: 50%; background: rgba(15,23,42,.7); color: #fff; font-size: 12px; line-height: 18px; padding: 0; }
.upload-btn { width: 100%; border: 1px dashed #cbd5e1; border-radius: 9px; background: #fbfdff; color: var(--steel); font-size: 11px; padding: 10px; }
.upload-hint { font-size: 9px; color: var(--steel-2); margin-top: 4px; text-align: right; }
</style>
