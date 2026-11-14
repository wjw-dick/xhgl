<template>
  <div class="container page-pad" style="max-width:820px">
    <div v-if="loading" class="loading"><div class="spinner" /></div>
    <div v-else-if="error" class="empty"><h3>查询链接无效</h3><p>{{ error }}</p></div>
    <template v-else-if="data">
      <div class="share-head"><div><span class="kicker">REQUIREMENT PROGRESS</span><h1>{{ data.demand.demandNo }}</h1><p>{{ data.customerName }} · {{ label(data.demand.status) }}</p></div><span :class="['pill', statusPill(data.demand.status)]">{{ label(data.demand.status) }}</span></div>
      <div class="card">
        <div class="spec"><span>锅炉选型</span><strong>{{ label(data.demand.boilerType) }} · {{ data.demand.capacity }} · {{ label(data.demand.fuelType) }}</strong></div>
        <div class="spec"><span>压力等级</span><strong>{{ label(data.demand.pressureLevel) }}</strong></div>
        <div class="spec"><span>用途</span><strong>{{ data.demand.usageDesc || '—' }}</strong></div>
        <div class="spec"><span>期望地区</span><strong>{{ data.demand.region || '—' }}</strong></div>
        <div class="spec"><span>期望时间</span><strong>{{ date(data.demand.expectTime) }}</strong></div>
      </div>
      <div class="card" style="margin-top:16px">
        <h3>方案与报价进度</h3>
        <div v-for="s in data.solutions" :key="s.id" class="mini-row"><span>{{ s.name }} · v{{ s.version }}</span><strong>{{ label(s.status) }}</strong></div>
        <div v-for="q in data.quotes" :key="q.id" class="mini-row"><span>{{ q.quoteNo }} · {{ money(q.totalAmount) }}</span><strong>{{ label(q.status) }}</strong></div>
        <div v-if="!data.solutions?.length && !data.quotes?.length" class="muted">方案与报价正在准备中。</div>
      </div>
      <div class="notice blue" style="margin-top:16px">分享查询仅展示该需求的方案状态与已发送报价摘要，不包含内部审批意见与其他客户信息。</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { label, statusPill, money, date } from '@/utils'

const route = useRoute(); const data = ref<any>(null); const loading = ref(true); const error = ref('')
onMounted(async () => {
  try {
    data.value = await http.get('/public/share/demand', { params: { demandNo: route.query.demandNo, token: route.query.token, source: route.query.source || 'link' } })
  } catch (e: any) { error.value = e?.message || '链接无效' } finally { loading.value = false }
})
</script>

<style scoped>
.share-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
.share-head h1 { font-size: 32px; margin: 8px 0 4px; }
.share-head p { color: var(--steel); font-size: 11px; margin: 0; }
.mini-row { display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px dashed var(--line); padding: 9px 0; font-size: 12px; }
.card h3 { margin: 0 0 8px; font-size: 16px; }
</style>
