<template>
  <div class="rich-editor" :style="{ minHeight: height }">
    <Toolbar class="rich-toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      v-model="html"
      class="rich-body"
      :style="{ height: height, overflowY: 'hidden' }"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, shallowRef } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  modelValue?: string
  height?: string
  placeholder?: string
}>(), {
  modelValue: '',
  height: '320px',
  placeholder: '请输入正文；可粘贴网页/Word 内容，图片自动上传对象存储'
})
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const editorRef = shallowRef<any>()
const toolbarConfig = {
  excludeKeys: []
}
const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file: File, insertFn: (url: string, alt: string, href: string) => void) => {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('category', 'richtext')
        try {
          const record: any = await http.post('/admin/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          if (!record?.url) throw new Error('上传未返回地址')
          insertFn(record.url, record.originalName || file.name, record.url)
        } catch (e: any) {
          ElMessage.error('图片上传失败：' + (e?.message || '未知错误'))
        }
      }
    }
  }
}
const html = computed({
  get: () => props.modelValue || '',
  set: (value: string) => emit('update:modelValue', value)
})
function handleCreated(editor: any) {
  editorRef.value = editor
}
onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped>
.rich-editor { border: 1px solid var(--xh-line); border-radius: 6px; overflow: hidden; background: #fff; width: 100%; }
.rich-toolbar { border-bottom: 1px solid var(--xh-line); }
.rich-body { overflow-y: auto; }
:deep(.w-e-text-container) { background: #fff; }
:deep(.w-e-text-container [data-slate-editor]) { min-height: 240px; }
</style>
