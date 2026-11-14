<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/spares')">‹</button><h1>库存流水</h1></div>
    <div class="page-body">
      <div class="filter-chips">
        <button :class="{ active: type === '' }" @click="type = ''; search()">全部</button>
        <button :class="{ active: type === 'IN' }" @click="type = 'IN'; search()">入库</button>
        <button :class="{ active: type === 'OUT' }" @click="type = 'OUT'; search()">领用</button>
        <button :class="{ active: type === 'STOCKTAKE' }" @click="type = 'STOCKTAKE'; search()">盘点</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="r in rows" :key="r.id" class="list-row">
          <div class="main"><strong>{{ typeLabel(r.type) }} · 备件 {{ r.partId }}</strong><span>{{ dateTime(r.createdAt) }} · {{ r.operator || '—' }} · 工单 {{ r.ticketId || '—' }}</span></div>
          <div style="text-align:right"><strong class="money">{{ r.quantity }}</strong><span style="display:block;font-size:9px;color:var(--steel-2)">结存 {{ r.stockAfter }}</span></div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">流</div>暂无流水</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { dateTime } from '@/utils'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const type = ref(''); const loading = ref(false)
function typeLabel(v: string) { return ({ IN: '入库', OUT: '领用', STOCKTAKE: '盘点' } as any)[v] || v }
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/spare-records', { params: { page: page.value, size: size.value, type: type.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
function search() { page.value = 1; load() }
onMounted(load)
</script>

<style scoped>
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 2px 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
