<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>需求线索</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 录入</button></div>
    <div class="page-body">
      <div class="search-row">
        <input v-model="keyword" class="search-input" placeholder="需求编号 / 联系人 / 地区" @keyup.enter="search" />
        <button class="btn btn-ghost btn-sm" @click="search">搜索</button>
      </div>
      <div class="filter-chips">
        <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; search()">{{ s.label }}</button>
      </div>

      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="d in rows" :key="d.id" class="demand-row">
          <button class="demand-main" @click="router.push(`/admin/demands/${d.id}`)">
            <div class="row-top"><strong class="mono">{{ d.demandNo }}</strong><span :class="['pill', statusPill(d.status)]">{{ label(d.status) }}</span></div>
            <div class="row-mid">{{ label(d.boilerType) }} · {{ d.capacity }} · {{ label(d.fuelType) }} · {{ label(d.pressureLevel) }}</div>
            <div class="row-bottom"><span>{{ d.region }} · {{ d.ownerName || '待认领' }}</span><span>{{ date(d.createdAt) }}</span></div>
          </button>
          <div class="row-actions">
            <button v-if="!d.ownerId" class="link-btn" @click="claim(d)">认领</button>
            <button class="link-btn" @click="router.push(`/admin/demands/${d.id}`)">跟进</button>
            <button class="link-btn danger" @click="remove(d)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">▤</div>暂无需求</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog = false">
      <div class="modal-card">
        <div class="modal-head"><h3>录入采购需求</h3><button class="modal-close" @click="dialog = false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>关联客户 *</label><select v-model.number="form.customerId"><option value="">请选择客户</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option></select></div>
          <div class="field"><label>联系人</label><input v-model="form.contactName" /></div>
          <div class="field"><label>联系电话（脱敏）</label><input v-model="form.contactPhoneMask" placeholder="138****1111" /></div>
          <div class="field"><label>需求来源</label><select v-model="form.source"><option value="H5">H5门户</option><option value="MP">小程序</option><option value="OA">公众号</option><option value="PHONE">电话</option><option value="MANUAL">人工录入</option></select></div>
          <div class="field"><label>锅炉类型</label><select v-model="form.boilerType"><option value="steam">蒸汽锅炉</option><option value="hotwater">热水锅炉</option><option value="thermal">导热油炉</option><option value="wasteheat">余热锅炉</option></select></div>
          <div class="field"><label>燃料类型</label><select v-model="form.fuelType"><option value="gas">燃气</option><option value="oil">燃油</option><option value="electric">电</option><option value="biomass">生物质</option><option value="coal">燃煤</option></select></div>
          <div class="field"><label>容量</label><input v-model="form.capacity" placeholder="如 6 t/h" /></div>
          <div class="field"><label>压力等级</label><select v-model="form.pressureLevel"><option value="atmospheric">常压</option><option value="low">低压</option><option value="medium">中压</option><option value="high">高压</option></select></div>
          <div class="field"><label>预算下限</label><input v-model.number="form.budgetMin" type="number" /></div>
          <div class="field"><label>预算上限</label><input v-model.number="form.budgetMax" type="number" /></div>
          <div class="field"><label>期望地区</label><input v-model="form.region" /></div>
          <div class="field"><label>期望交付</label><input v-model="form.expectTime" type="date" /></div>
          <div class="field full"><label>用途说明</label><input v-model="form.usageDesc" /></div>
          <div class="field full"><label>需求补充</label><textarea v-model="form.description" rows="3" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存需求' }}</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { date, label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const status = ref(''); const loading = ref(false)
const dialog = ref(false); const saving = ref(false); const customers = ref<any[]>([])
const form = reactive<any>({ source: 'MANUAL', boilerType: 'steam', fuelType: 'gas', pressureLevel: 'low' })
const statuses = [{ label: '全部', value: '' }, { label: '新需求', value: 'NEW' }, { label: '咨询中', value: 'CONSULTING' }, { label: '介绍中', value: 'INTRODUCING' }, { label: '谈价中', value: 'TALKING' }, { label: '成交', value: 'DEAL' }, { label: '流失', value: 'LOST' }]
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/demands', { params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value } })
    rows.value = data.records || []; total.value = data.total || 0
  } finally { loading.value = false }
}
async function loadCustomers() { try { customers.value = await http.get('/admin/customers/options') } catch {} }
function search() { page.value = 1; load() }
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { source: 'MANUAL', boilerType: 'steam', fuelType: 'gas', pressureLevel: 'low' })
  dialog.value = true
}
async function save() {
  if (!form.customerId) return toast('请选择关联客户', 'error')
  saving.value = true
  try { await http.post('/admin/demands', form); toast('需求已录入', 'success'); dialog.value = false; load() } finally { saving.value = false }
}
async function claim(row: any) {
  if (!confirm(`确认认领需求 ${row.demandNo}？`)) return
  await http.post(`/admin/demands/${row.id}/claim`); toast('认领成功', 'success'); load()
}
async function remove(row: any) {
  if (!confirm(`确认删除需求「${row.demandNo}」？有关联方案或报价时不可删除。`)) return
  try { await http.delete(`/admin/demands/${row.id}`); toast('已删除', 'success'); load() } catch {}
}
onMounted(() => { load(); loadCustomers() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.demand-row { border-bottom: 1px solid #f2f4f7; padding: 6px 0 8px; }
.demand-row:last-child { border-bottom: none; }
.demand-main { display: block; width: 100%; text-align: left; border: none; background: transparent; padding: 6px 0; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 11px; margin-top: 4px; color: #344054; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 3px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
