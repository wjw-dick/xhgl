<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">报表中心</h1><p class="page-sub">商机漏斗、成交与回款归属、回款账龄、售后台账多维度统计，支持 Excel 导出。</p></div>
      <div class="toolbar" style="margin:0">
        <el-button @click="downloadFile('/admin/reports/export', '经营报表.xlsx')"><el-icon><Download /></el-icon> 导出 Excel</el-button>
        <el-button type="primary" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>
    <el-skeleton v-if="loading && !data" :rows="5" animated />
    <template v-else-if="data">
      <div class="stat-grid">
        <div class="stat-card"><div class="stat-label">成交额</div><div class="stat-value money">{{ fmtMoney(data.overview.contractAmount) }}</div><div class="stat-note">合同 {{ data.overview.contracts }} 份</div></div>
        <div class="stat-card"><div class="stat-label">回款额</div><div class="stat-value money success">{{ fmtMoney(data.overview.received) }}</div><div class="stat-note">回款率 {{ data.overview.receivableRate }}%</div></div>
        <div class="stat-card"><div class="stat-label">待回款</div><div class="stat-value money danger">{{ fmtMoney(data.overview.outstanding) }}</div></div>
        <div class="stat-card"><div class="stat-label">成交转化率</div><div class="stat-value">{{ data.overview.conversionRate }}%</div><div class="stat-note">需求 {{ data.overview.demands }} / 成交 {{ data.overview.deals }}</div></div>
        <div class="stat-card"><div class="stat-label">售后满意度</div><div class="stat-value">{{ data.overview.avgSatisfaction }} 分</div><div class="stat-note">工单 {{ data.overview.tickets }} 单</div></div>
      </div>
      <div class="section-grid">
        <div class="card"><h3 class="card-title">月度成交与回款</h3><ChartBox :option="monthlyOption" height="320px" /></div>
        <div class="card"><h3 class="card-title">商机漏斗</h3><ChartBox :option="funnelOption" height="320px" /></div>
      </div>
      <div class="grid-2">
        <div class="card" style="padding:0">
          <div class="card-title" style="padding:18px 18px 0">业绩归属（负责人）</div>
          <el-table :data="data.ownerRanking" stripe size="small">
            <el-table-column prop="name" label="负责人" min-width="130" />
            <el-table-column prop="contracts" label="合同数" width="90" />
            <el-table-column label="成交额" width="150"><template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template></el-table-column>
            <el-table-column label="回款额" width="150"><template #default="{ row }"><span class="money success">{{ fmtMoney(row.received) }}</span></template></el-table-column>
          </el-table>
        </div>
        <div class="card">
          <h3 class="card-title">售后运营指标</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="平均响应">{{ data.service.avgResponseHours }} 小时</el-descriptions-item>
            <el-descriptions-item label="满意度">{{ data.service.avgSatisfaction }} 分</el-descriptions-item>
            <el-descriptions-item label="备件消耗">{{ data.service.partsConsumed }} 件</el-descriptions-item>
            <el-descriptions-item label="工单总量">{{ data.overview.tickets }} 单</el-descriptions-item>
            <el-descriptions-item label="报修">{{ data.service.byType.REPAIR || 0 }}</el-descriptions-item>
            <el-descriptions-item label="保养">{{ data.service.byType.MAINTAIN || 0 }}</el-descriptions-item>
            <el-descriptions-item label="年检">{{ data.service.byType.INSPECT || 0 }}</el-descriptions-item>
            <el-descriptions-item label="安装">{{ data.service.byType.INSTALL || 0 }}</el-descriptions-item>
          </el-descriptions>
          <h3 class="card-title" style="margin-top:18px">回款状态</h3>
          <div class="status-row" v-for="(v, k) in data.paymentStatus" :key="k">
            <span>{{ statusLabel(String(k)) }}</span><strong>{{ v }}</strong>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http, { downloadFile } from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'
import { fmtMoney, statusLabel } from '@/utils/format'

const data = ref<any>(null); const loading = ref(false)
const monthlyOption = computed(() => ({
  tooltip: { trigger: 'axis' }, legend: { data: ['成交额', '回款额'] },
  grid: { left: 16, right: 20, top: 40, bottom: 10, containLabel: true },
  xAxis: { type: 'category', data: (data.value?.monthly || []).map((x: any) => x.month) },
  yAxis: { type: 'value', axisLabel: { formatter: (v: number) => (v / 10000) + '万' } },
  series: [
    { name: '成交额', type: 'bar', barWidth: 20, itemStyle: { color: '#1f6feb', borderRadius: [4,4,0,0] }, data: (data.value?.monthly || []).map((x: any) => Number(x.contractAmount || 0)) },
    { name: '回款额', type: 'line', smooth: true, lineStyle: { color: '#16a34a' }, itemStyle: { color: '#16a34a' }, data: (data.value?.monthly || []).map((x: any) => Number(x.received || 0)) }
  ]
}))
const funnelOption = computed(() => {
  const stages = [['新需求','NEW'],['咨询中','CONSULTING'],['介绍中','INTRODUCING'],['谈价中','TALKING'],['成交','DEAL'],['流失','LOST']]
  return {
    tooltip: {}, grid: { left: 20, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value' }, yAxis: { type: 'category', data: stages.map((s) => s[0]) },
    series: [{ type: 'bar', barWidth: 16, label: { show: true, position: 'right' },
      itemStyle: { color: (p: any) => p.dataIndex === 4 ? '#16a34a' : p.dataIndex === 5 ? '#e5484d' : '#1f6feb', borderRadius: [0,4,4,0] },
      data: stages.map((s) => data.value?.funnel?.[s[1]] || 0) }]
  }
})
async function load() {
  loading.value = true
  try { data.value = await http.get('/admin/reports') } finally { loading.value = false }
}
onMounted(load)
</script>
<style scoped>.status-row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px dashed var(--xh-line);font-size:13px}</style>
