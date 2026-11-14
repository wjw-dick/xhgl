<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>数据字典</h1><button class="btn btn-primary btn-sm" @click="dialog=true">＋ 扩展项</button></div>
    <div class="page-body">
      <div class="tabs">
        <button :class="{ active: tab === 'global' }" @click="tab = 'global'">全局字典</button>
        <button :class="{ active: tab === 'custom' }" @click="tab = 'custom'">自定义字典</button>
      </div>
      <div class="filter-chips">
        <button :class="{ active: activeType === '' }" @click="activeType = ''">全部</button>
        <button v-for="t in types" :key="t.value" :class="{ active: activeType === t.value }" @click="activeType = t.value">{{ t.label }}</button>
      </div>
      <div class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="d in filtered" :key="d.id || `${d.dictType}-${d.dictKey}`" class="list-row">
          <div class="main"><strong>{{ typeLabel(d.dictType) }} · {{ d.dictKey }}</strong><span>{{ d.dictValue }} · 排序 {{ d.sort ?? 0 }}</span></div>
          <button v-if="tab === 'custom'" class="link-btn danger" @click="remove(d)">删除</button>
        </div>
        <div v-if="!filtered.length" class="muted" style="font-size:11px;padding:12px 0">暂无字典项</div>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增自定义字典项</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>字典类型</label><select v-model="form.dictType"><option v-for="t in types" :key="t.value" :value="t.value">{{ t.label }}</option></select></div>
          <div class="field"><label>键</label><input v-model="form.dictKey" /></div>
          <div class="field"><label>值</label><input v-model="form.dictValue" /></div>
          <div class="field"><label>排序</label><input v-model.number="form.sort" type="number" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'

const router = useRouter()
const tab = ref('global'); const activeType = ref(''); const globalItems = ref<any[]>([]); const customItems = ref<any[]>([])
const dialog = ref(false); const form = reactive<any>({ dictType: 'boiler_series', sort: 99 })
const types = [
  { value: 'boiler_series', label: '锅炉系列' }, { value: 'industry', label: '行业' }, { value: 'customer_level', label: '客户级别' },
  { value: 'demand_source', label: '需求来源' }, { value: 'ticket_type', label: '工单类型' }, { value: 'spare_category', label: '备件分类' }
]
const filtered = computed(() => (tab.value === 'global' ? globalItems.value : customItems.value).filter((x) => !activeType.value || x.dictType === activeType.value))
function typeLabel(type: string) { return types.find((t) => t.value === type)?.label || type }
async function load() { const d: any = await http.get('/admin/settings/dicts'); globalItems.value = d.global || []; customItems.value = d.custom || [] }
async function save() {
  if (!form.dictType || !form.dictKey || !form.dictValue) return toast('类型、键、值必填', 'error')
  await http.post('/admin/settings/dicts', form); toast('扩展项已新增', 'success'); dialog.value = false; load()
}
async function remove(d: any) { if (!confirm('确认删除该自定义字典项？')) return; await http.delete(`/admin/settings/dicts/${d.id}`); toast('已删除', 'success'); load() }
onMounted(load)
</script>

<style scoped>
.tabs { display: flex; gap: 6px; padding: 2px 0 10px; }
.tabs button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 11px; color: var(--steel); }
.tabs button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: #eef2f6; color: var(--ink); border-color: var(--ink); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
</style>
