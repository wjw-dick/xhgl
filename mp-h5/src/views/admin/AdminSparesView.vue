<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>备品备件</h1><div style="display:flex;gap:8px"><button class="link-btn" @click="router.push('/admin/spare-records')">流水</button><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新增</button></div></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="编码 / 名称 / 型号 / 供应商" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div v-if="alerts.length" class="notice" style="margin-top:10px;border-color:#e5484d;background:#fbe9e6;color:#7f1d1d">库存预警：{{ alerts.map((a) => `${a.name} 库存 ${a.stock}/${a.safetyStock}`).join('、') }}</div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="p in rows" :key="p.id" class="spare-row">
          <div class="row-top"><strong>{{ p.name }}</strong><span :class="['pill', p.stock < p.safetyStock ? 'red' : 'green']">库存 {{ p.stock }}</span></div>
          <div class="row-mid"><span class="mono">{{ p.code }}</span> · {{ p.model || '—' }} · {{ p.spec || '—' }}</div>
          <div class="row-bottom"><span>安全库存 {{ p.safetyStock }}</span><span>{{ money(p.unitPrice) }}</span></div>
          <div class="row-actions">
            <button class="link-btn" @click="openStock(p, 'IN')">入库</button>
            <button class="link-btn" @click="openStock(p, 'OUT')">领用</button>
            <button class="link-btn" @click="openStock(p, 'STOCKTAKE')">盘点</button>
            <button class="link-btn" @click="openEdit(p)">编辑</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">件</div>暂无备件</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ editing ? '编辑备件' : '新增备件' }}</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>备件编码 *</label><input v-model="form.code" /></div>
          <div class="field"><label>名称 *</label><input v-model="form.name" /></div>
          <div class="field"><label>型号</label><input v-model="form.model" /></div>
          <div class="field"><label>规格</label><input v-model="form.spec" /></div>
          <div class="field"><label>单位</label><input v-model="form.unit" /></div>
          <div class="field"><label>供应商</label><input v-model="form.supplier" /></div>
          <div class="field"><label>单价</label><input v-model.number="form.unitPrice" type="number" /></div>
          <div class="field"><label>安全库存</label><input v-model.number="form.safetyStock" type="number" /></div>
          <div v-if="!editing" class="field"><label>初始库存</label><input v-model.number="form.stock" type="number" /></div>
          <div class="field full"><label>备件图片</label><AdminUploader v-model="form.image" image category="spare" :limit="4" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存</button></div>
      </div>
    </div>

    <div v-if="stockVisible" class="modal-mask" @click.self="stockVisible=false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ stockTitle }}</h3><button class="modal-close" @click="stockVisible=false">×</button></div>
        <div class="field"><label>{{ stockType === 'STOCKTAKE' ? '盘点后库存' : '数量' }}</label><input v-model.number="stockForm.quantity" type="number" /></div>
        <div class="field" style="margin-top:10px"><label>关联工单 ID</label><input v-model.number="stockForm.ticketId" type="number" /></div>
        <div class="field" style="margin-top:10px"><label>备注</label><textarea v-model="stockForm.remark" rows="2" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="stockVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="doStock">确认</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import AdminUploader from '@/components/AdminUploader.vue'
import { money } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const loading = ref(false); const alerts = ref<any[]>([])
const dialog = ref(false); const editing = ref<any>(null); const form = reactive<any>({})
const stockVisible = ref(false); const stockType = ref('IN'); const currentPart = ref<any>(null); const stockForm = reactive<any>({})
const stockTitle = ref('入库')
function stockLabel(type: string) { return ({ IN: '入库', OUT: '领用', STOCKTAKE: '盘点' } as any)[type] || type }
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/spares', { params: { page: page.value, size: size.value, keyword: keyword.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function loadAlerts() { try { alerts.value = await http.get('/admin/spare-records/alerts') } catch { alerts.value = [] } }
function search() { page.value = 1; load() }
function openCreate() { editing.value = null; Object.keys(form).forEach((k) => delete form[k]); dialog.value = true }
function openEdit(row: any) { editing.value = row; Object.assign(form, row); dialog.value = true }
async function save() {
  if (!form.code || !form.name) return toast('备件编码与名称必填', 'error')
  if (editing.value) await http.put(`/admin/spares/${editing.value.id}`, form)
  else await http.post('/admin/spares', form)
  toast('备件已保存', 'success'); dialog.value = false; load(); loadAlerts()
}
function openStock(part: any, type: string) {
  currentPart.value = part; stockType.value = type; stockTitle.value = `${part.name} · ${stockLabel(type)}`
  Object.keys(stockForm).forEach((k) => delete stockForm[k])
  if (type === 'STOCKTAKE') stockForm.quantity = part.stock
  stockVisible.value = true
}
async function doStock() {
  await http.post('/admin/spare-records/operation', { partId: currentPart.value.id, type: stockType.value, ...stockForm })
  toast('库存已更新', 'success'); stockVisible.value = false; load(); loadAlerts()
}
onMounted(() => { load(); loadAlerts() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.spare-row { border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.spare-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 13px; }
.row-mid { font-size: 10px; color: var(--steel); margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 4px; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
