<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>菜单配置</h1><button class="btn btn-primary btn-sm" @click="addNew">＋ 新增</button></div>
    <div class="page-body">
      <div class="tabs">
        <button v-for="t in targets" :key="t.value" :class="{ active: target === t.value }" @click="target = t.value; load()">{{ t.label }}</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else class="card" style="padding:4px 16px">
        <div v-for="(m, index) in list" :key="m.id || index" class="menu-row">
          <div class="main"><strong>{{ m.title }}</strong><span>{{ m.path }} · {{ m.menuType }} · 排序 {{ m.sort }}</span><span>{{ m.visible ? '显示' : '隐藏' }}</span></div>
          <div class="menu-actions">
            <button class="link-btn" @click="move(index, -1)">↑</button>
            <button class="link-btn" @click="move(index, 1)">↓</button>
            <button class="link-btn" @click="edit(m)">编辑</button>
            <button class="link-btn danger" @click="remove(m)">删除</button>
          </div>
        </div>
        <div v-if="!list.length" class="muted" style="font-size:11px;padding:12px 0">该端暂无菜单</div>
      </div>
      <button class="btn btn-ghost btn-sm" style="margin-top:10px" @click="saveOrder">保存排序</button>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ form.id ? '编辑菜单' : '新增菜单' }}</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>标题 *</label><input v-model="form.title" /></div>
          <div class="field full"><label>路径 *</label><input v-model="form.path" /></div>
          <div class="field"><label>图标</label><input v-model="form.icon" /></div>
          <div class="field"><label>类型</label><select v-model="form.menuType"><option value="PAGE">页面</option><option value="EXTERNAL">外链</option></select></div>
          <div class="field full"><label>外链 URL</label><input v-model="form.externalUrl" /></div>
          <div class="field"><label>排序</label><input v-model.number="form.sort" type="number" /></div>
          <div class="field"><label>显示</label><select v-model.number="form.visible"><option :value="1">显示</option><option :value="0">隐藏</option></select></div>
          <div class="field"><label>父级 ID</label><input v-model.number="form.parentId" type="number" /></div>
        </div>
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
const targets = [{ value: 'ADMIN', label: 'PC 后台' }, { value: 'MP', label: '小程序' }, { value: 'H5', label: 'H5' }]
const target = ref('ADMIN'); const list = ref<any[]>([]); const loading = ref(false); const dialog = ref(false)
const form = reactive<any>({ id: null, title: '', icon: 'Menu', path: '/', menuType: 'PAGE', externalUrl: '', sort: 1, visible: 1, parentId: 0 })
async function load() { loading.value = true; try { list.value = await http.get('/admin/menus', { params: { target: target.value } }) } finally { loading.value = false } }
function addNew() { Object.assign(form, { id: null, title: '新菜单', icon: 'Menu', path: '/', menuType: 'PAGE', externalUrl: '', sort: list.value.length + 1, visible: 1, parentId: 0, target: target.value }); dialog.value = true }
function edit(m: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, m); dialog.value = true }
async function save() {
  if (!form.title || !form.path) return toast('标题与路径必填', 'error')
  const payload = { ...form, target: target.value, parentId: form.parentId || 0 }
  if (payload.id) await http.put(`/admin/menus/${payload.id}`, payload)
  else await http.post('/admin/menus', payload)
  toast('菜单已保存', 'success'); dialog.value = false; load()
}
async function remove(m: any) { if (!m.id) return; if (!confirm(`确认删除菜单「${m.title}」？`)) return; await http.delete(`/admin/menus/${m.id}`); toast('已删除', 'success'); load() }
async function move(index: number, delta: number) {
  const next = index + delta
  if (next < 0 || next >= list.value.length) return
  const [item] = list.value.splice(index, 1); list.value.splice(next, 0, item)
}
async function saveOrder() {
  const items = list.value.filter((m) => m.id).map((m, i) => ({ id: m.id, sort: i + 1, parentId: m.parentId || 0 }))
  await http.post('/admin/menus/reorder', items); toast('排序已保存', 'success'); load()
}
onMounted(load)
</script>

<style scoped>
.tabs { display: flex; gap: 6px; padding: 2px 0 10px; }
.tabs button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 11px; color: var(--steel); }
.tabs button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.menu-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.menu-row:last-child { border-bottom: none; }
.menu-actions { display: flex; gap: 8px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
</style>
