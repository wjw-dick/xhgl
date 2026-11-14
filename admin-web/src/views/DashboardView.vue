<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">业务工作台</h1>
        <p class="page-sub">需求 → 咨询 → 方案 → 报价 → 成交履约 → 售后的全链路经营概览</p>
      </div>
      <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <el-skeleton v-if="loading && !data" :rows="5" animated />
    <template v-else-if="data">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">客户档案</div>
          <div class="stat-value">{{ fmtNumber(data.customers) }}</div>
          <div class="stat-note">活跃需求 {{ data.activeDemands }} 条 · 产品 {{ data.products }} 个</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">合同成交额</div>
          <div class="stat-value">{{ fmtMoney(data.contractAmount) }}</div>
          <div class="stat-note">合同 {{ data.contracts }} 份</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已回款</div>
          <div class="stat-value money" style="color: #16a34a">{{ fmtMoney(data.received) }}</div>
          <div class="stat-note">待回款 {{ fmtMoney(data.outstanding) }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">售后待处理</div>
          <div class="stat-value" :class="{ danger: data.pendingTickets > 0 }">{{ data.pendingTickets }}</div>
          <div class="stat-note">满意度 {{ data.avgSatisfaction }} 分 · 证照临期 {{ data.dueCertificates }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">维保到期</div>
          <div class="stat-value">{{ data.dueMaintenance }}</div>
          <div class="stat-note">30 天内到期计划</div>
        </div>
      </div>

      <div class="section-grid">
        <div class="card">
          <h3 class="card-title">近 6 个月成交与回款</h3>
          <ChartBox :option="trendOption" height="300px" />
        </div>
        <div class="card">
          <h3 class="card-title">商机漏斗</h3>
          <ChartBox :option="funnelOption" height="300px" />
        </div>
      </div>

      <div class="section-grid">
        <div class="card">
          <h3 class="card-title">
            最近需求
            <el-button link type="primary" style="margin-left:auto" @click="router.push('/demands')">查看全部</el-button>
          </h3>
          <el-table :data="data.recentDemands" size="small" stripe>
            <el-table-column prop="demandNo" label="需求编号" width="190" />
            <el-table-column prop="contactName" label="联系人" width="90" />
            <el-table-column label="选型" min-width="160">
              <template #default="{ row }">{{ statusLabel(row.boilerType) }} · {{ row.capacity }} · {{ statusLabel(row.fuelType) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }"><el-button link type="primary" @click="router.push(`/demands/${row.id}`)">跟进</el-button></template>
            </el-table-column>
          </el-table>
        </div>
        <div class="card">
          <h3 class="card-title">售后工单</h3>
          <el-table :data="data.recentTickets" size="small" stripe>
            <el-table-column prop="ticketNo" label="工单号" width="190" />
            <el-table-column prop="customerName" label="客户" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">快捷入口</h3>
        <div class="quick-grid">
          <button v-for="q in quickActions" :key="q.path" class="quick-card" @click="router.push(q.path)">
            <el-icon :size="22"><component :is="q.icon" /></el-icon>
            <strong>{{ q.title }}</strong>
            <span>{{ q.desc }}</span>
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'
import { fmtMoney, fmtNumber, statusLabel, statusPill } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const data = ref<any>(null)
const quickActions = [
  { title: '录入需求', desc: '电话/人工录入需求线索', path: '/demands', icon: 'Tickets' },
  { title: '新建报价', desc: '产品、运费、安装调试费', path: '/quotes', icon: 'Money' },
  { title: '合同履约', desc: '生产、发货、回款、验收', path: '/contracts', icon: 'Files' },
  { title: '售后派单', desc: '受理、派单、完工回访', path: '/tickets', icon: 'Service' },
  { title: '预警中心', desc: '资质、维保、回款超期', path: '/warnings', icon: 'Warning' },
  { title: '报表导出', desc: '成交/回款/售后统计', path: '/reports', icon: 'TrendCharts' }
]
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['成交额', '回款额'], top: 0 },
  grid: { left: 16, right: 20, top: 40, bottom: 10, containLabel: true },
  xAxis: { type: 'category', data: (data.value?.trend || []).map((x: any) => x.month) },
  yAxis: { type: 'value', axisLabel: { formatter: (v: number) => (v / 10000) + '万' } },
  series: [
    { name: '成交额', type: 'bar', barWidth: 18, itemStyle: { color: '#1f6feb', borderRadius: [4, 4, 0, 0] },
      data: (data.value?.trend || []).map((x: any) => Number(x.contractAmount || 0)) },
    { name: '回款额', type: 'line', smooth: true, symbolSize: 7, lineStyle: { width: 2, color: '#16a34a' },
      itemStyle: { color: '#16a34a' }, data: (data.value?.trend || []).map((x: any) => Number(x.received || 0)) }
  ]
}))
const funnelOption = computed(() => {
  const counts = data.value?.funnel || {}
  const stages = [
    { name: '新需求', value: 'NEW', color: '#8ab4f8' },
    { name: '咨询中', value: 'CONSULTING', color: '#6ea0f0' },
    { name: '介绍中', value: 'INTRODUCING', color: '#4d8bea' },
    { name: '谈价中', value: 'TALKING', color: '#2f76df' },
    { name: '成交', value: 'DEAL', color: '#16a34a' },
    { name: '流失', value: 'LOST', color: '#e5484d' }
  ]
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 20, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: stages.map((s) => s.name) },
    series: [{
      type: 'bar',
      data: stages.map((s) => ({ value: counts[s.value] || 0, itemStyle: { color: s.color, borderRadius: [0, 4, 4, 0] } })),
      label: { show: true, position: 'right' },
      barWidth: 16
    }]
  }
})
async function load() {
  loading.value = true
  try {
    data.value = await http.get('/admin/dashboard')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.card-title .el-button { margin-left: auto; }
.quick-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.quick-card { text-align: left; border: 1px solid var(--xh-line); background: #fbfdff; border-radius: 8px; padding: 14px; cursor: pointer; display: flex; flex-direction: column; gap: 5px; color: #354052; transition: all .15s ease; }
.quick-card:hover { border-color: #bcd3f7; background: #f2f7ff; transform: translateY(-1px); }
.quick-card .el-icon { color: var(--xh-primary); }
.quick-card strong { font-size: 13px; }
.quick-card span { font-size: 11px; color: var(--xh-muted); }
</style>
