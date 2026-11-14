<template>
  <div class="page">
    <div class="topbar"><h1>我的需求</h1><router-link to="/demands/new" class="btn btn-primary btn-sm">＋ 新需求</router-link></div>
    <div class="page-body">
      <div class="filter-chips">
        <button v-for="s in filters" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; load()">{{ s.label }}</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="margin-top:12px;padding:4px 16px">
        <button v-for="d in rows" :key="d.id" class="demand-row" @click="openDetail(d)">
          <div class="row-top"><strong class="mono">{{ d.demandNo }}</strong><span :class="['pill', statusPill(d.status)]">{{ label(d.status) }}</span></div>
          <div class="row-mid">{{ label(d.boilerType) }} · {{ d.capacity }} · {{ label(d.fuelType) }} · {{ label(d.pressureLevel) }}</div>
          <div class="row-bottom"><span>{{ d.region }} · 期望 {{ date(d.expectTime) }}</span><span>{{ date(d.createdAt) }}</span></div>
        </button>
      </div>
      <div v-else class="empty"><div class="big">▤</div>暂无需求<router-link to="/demands/new" style="color:var(--accent)"> 提交需求 →</router-link></div>
    </div>
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ detail.demand.demandNo }}</h3><button class="modal-close" @click="detail = null">×</button></div>
        <div class="detail-grid">
          <div><span>锅炉类型</span><strong>{{ label(detail.demand.boilerType) }}</strong></div>
          <div><span>容量</span><strong>{{ detail.demand.capacity }}</strong></div>
          <div><span>燃料</span><strong>{{ label(detail.demand.fuelType) }}</strong></div>
          <div><span>压力</span><strong>{{ label(detail.demand.pressureLevel) }}</strong></div>
          <div><span>地区</span><strong>{{ detail.demand.region }}</strong></div>
          <div><span>状态</span><strong>{{ label(detail.demand.status) }}</strong></div>
        </div>
        <div class="section-label">跟进动态</div>
        <div v-for="f in detail.followRecords || []" :key="f.id" class="timeline-row"><span>{{ dateTime(f.followTime) }}</span><p>{{ f.content }}</p></div>
        <div class="section-label">方案 / 报价</div>
        <div v-for="s in detail.solutions || []" :key="'s'+s.id" class="list-row"><div class="main"><strong>{{ s.name }}</strong><span>版本 {{ s.version }}</span></div><span class="pill">{{ label(s.status) }}</span></div>
        <div v-for="q in detail.quotes || []" :key="'q'+q.id" class="list-row"><div class="main"><strong>{{ q.quoteNo }}</strong><span>{{ money(q.totalAmount) }}</span></div><span class="pill">{{ label(q.status) }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import { label, date, dateTime, money, statusPill } from '@/utils'

const rows = ref<any[]>([]); const loading = ref(false); const status = ref(''); const detail = ref<any>(null)
const filters = [
  { label: '全部', value: '' }, { label: '新需求', value: 'NEW' }, { label: '咨询中', value: 'CONSULTING' },
  { label: '介绍中', value: 'INTRODUCING' }, { label: '谈价中', value: 'TALKING' }, { label: '成交', value: 'DEAL' }
]
async function load() {
  loading.value = true
  try {
    const data: any[] = await http.get('/customer/demands')
    rows.value = status.value ? data.filter((d) => d.status === status.value) : data
  } finally { loading.value = false }
}
async function openDetail(d: any) {
  detail.value = await http.get(`/customer/demands/${d.id}`)
}
onMounted(load)
</script>
<style scoped>
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 2px 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.demand-row { display: block; width: 100%; text-align: left; border: none; border-bottom: 1px solid #f2f4f7; background: transparent; padding: 12px 0; }
.demand-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 12px; margin-top: 5px; color: #344054; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 4px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
.detail-grid div { background: #fff; padding: 9px 10px; }
.detail-grid span { display: block; font-size: 10px; color: var(--steel-2); }
.detail-grid strong { font-size: 12px; }
.timeline-row { display: grid; grid-template-columns: 90px 1fr; gap: 8px; border-bottom: 1px dashed var(--line); padding: 7px 0; font-size: 11px; }
.timeline-row span { color: var(--steel-2); }
.timeline-row p { margin: 0; }
</style>
