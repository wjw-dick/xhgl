<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>多语言</h1><button class="btn btn-primary btn-sm" @click="dialog=true">＋ 新增</button></div>
    <div class="page-body">
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else class="card" style="padding:4px 16px">
        <div v-for="l in rows" :key="l.id" class="list-row">
          <div class="main"><strong>{{ l.name }}</strong><span>{{ l.code }} · {{ l.enabled ? '已启用' : '已停用' }}</span></div>
          <select v-model.number="l.enabled" class="switch-select" @change="toggle(l)"><option :value="1">启用</option><option :value="0">停用</option></select>
        </div>
        <div v-if="!rows.length" class="muted" style="font-size:11px;padding:12px 0">暂无语言</div>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增语言</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="field"><label>语言代码</label><input v-model="form.code" placeholder="如 en" /></div>
        <div class="field" style="margin-top:10px"><label>语言名称</label><input v-model="form.name" placeholder="如 English" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const loading = ref(false); const dialog = ref(false)
const form = reactive<any>({ code: '', name: '' })
async function load() { loading.value = true; try { rows.value = await http.get('/admin/settings/languages') } finally { loading.value = false } }
async function toggle(l: any) { await http.put(`/admin/settings/languages/${l.id}`, { enabled: l.enabled }); toast('已更新', 'success') }
async function save() { if (!form.code || !form.name) return toast('代码和名称必填', 'error'); await http.post('/admin/settings/languages', form); toast('语言已新增', 'success'); dialog.value = false; form.code = ''; form.name = ''; load() }
onMounted(load)
</script>

<style scoped>
.switch-select { border: 1px solid var(--line); border-radius: 8px; padding: 6px 8px; font-size: 11px; }
</style>
