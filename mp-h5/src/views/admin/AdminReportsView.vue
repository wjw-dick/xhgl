<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>报表中心</h1><button class="link-btn" @click="exportExcel">导出</button></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="data" class="page-body">
      <div class="grid-2">
        <div class="stat-card"><span>成交额</span><strong>{{ money(data.overview?.contractAmount) }}</strong><em>合同 {{ data.overview?.contracts || 0 }} 份</em></div>
        <div class="stat-card"><span>回款额</span><strong>{{ money(data.overview?.received) }}</strong><em>回款率 {{ data.overview?.receivableRate || 0 }}%</em></div>
        <div class="stat-card"><span>待回款</span><strong>{{ money(data.overview?.outstanding) }}</strong><em>账龄风险</em></div>
        <div class="stat-card"><span>转化率</span><strong>{{ data.overview?.conversionRate || 0 }}%</strong><em>需求 {{ data.overview?.demands || 0 }} / 成交 {{ data.overview?.deals || 0 }}</em></div>
      </div>
      <div class="section-label">业绩归属</div>
      <div class="card" style="padding:4px 16px">
        <div v-for="o in data.ownerRanking || []" :key="o.name" class="list-row">
          <div class="main"><strong>{{ o.name }}</strong><span>合同 {{ o.contracts }} 份</span></div>
          <div style="text-align:right"><strong class="money">{{ money(o.amount) }}</strong><span style="display:block;font-size:9px;color:var(--green)">回款 {{ money(o.received) }}</span></div>
        </div>
        <div v-if="!data.ownerRanking?.length" class="muted" style="font-size:11px">暂无数据</div>
      </div>
      <div class="section-label">售后运营</div>
      <div class="card">
        <div class="info-grid">
          <div><span>平均响应</span><strong>{{ data.service?.avgResponseHours || 0 }} 小时</strong></div>
          <div><span>满意度</span><strong>{{ data.service?.avgSatisfaction || 0 }} 分</strong></div>
          <div><span>备件消耗</span><strong>{{ data.service?.partsConsumed || 0 }} 件</strong></div>
          <div><span>工单总量</span><strong>{{ data.overview?.tickets || 0 }} 单</strong></div>
          <div><span>报修 / 保养</span><strong>{{ data.service?.byType?.REPAIR || 0 }} / {{ data.service?.byType?.MAINTAIN || 0 }}</strong></div>
          <div><span>安装 / 年检</span><strong>{{ data.service?.byType?.INSTALL || 0 }} / {{ data.service?.byType?.INSPECT || 0 }}</strong></div>
        </div>
      </div>
      <div class="section-label">回款状态</div>
      <div class="card">
        <div v-for="(v, k) in data.paymentStatus || {}" :key="k" class="list-row"><div class="main"><strong>{{ label(String(k)) }}</strong></div><span>{{ v }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import { label, money } from '@/utils'

const router = useRouter(); const data = ref<any>(null); const loading = ref(false)
async function load() { loading.value = true; try { data.value = await http.get('/admin/reports') } finally { loading.value = false } }
function exportExcel() { downloadFile('/admin/reports/export', '经营报表.xlsx') }
onMounted(load)
</script>

<style scoped>
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
.info-grid div { background: #fff; padding: 9px 10px; }
.info-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.info-grid strong { font-size: 11px; }
</style>
