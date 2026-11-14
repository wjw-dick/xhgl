<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>预警中心</h1><button class="link-btn" :disabled="scanning" @click="scan">{{ scanning ? '扫描中…' : '立即扫描' }}</button></div>
    <div class="page-body">
      <div class="grid-2">
        <div class="stat-card"><span>预警总数</span><strong>{{ data.total || 0 }}</strong><em>全部风险项</em></div>
        <div class="stat-card"><span>资质证书</span><strong>{{ data.certificates?.length || 0 }}</strong><em>临期/过期</em></div>
        <div class="stat-card"><span>维保年检</span><strong>{{ data.maintenance?.length || 0 }}</strong><em>即将到期</em></div>
        <div class="stat-card"><span>回款逾期</span><strong>{{ data.payments?.length || 0 }}</strong><em>需催收</em></div>
        <div class="stat-card"><span>需求超时</span><strong>{{ data.demandFollows?.length || 0 }}</strong><em>待跟进</em></div>
      </div>

      <div class="section-label">资质证书</div>
      <div class="card" style="padding:4px 16px">
        <div v-for="w in data.certificates || []" :key="w.certificate?.id" class="list-row">
          <div class="main"><strong>{{ w.certificate?.certNo }}</strong><span>{{ label(w.certificate?.certType) }} · 到期 {{ date(w.certificate?.validTo) }}</span></div>
          <span :class="['pill', w.level === 'RED' ? 'red' : 'yellow']">{{ w.remainDays < 0 ? '已过期' : w.remainDays + '天' }}</span>
        </div>
        <div v-if="!data.certificates?.length" class="muted" style="font-size:11px">暂无预警</div>
      </div>

      <div class="section-label">维保 / 年检</div>
      <div class="card" style="padding:4px 16px">
        <div v-for="w in data.maintenance || []" :key="w.plan?.id" class="list-row">
          <div class="main"><strong>{{ w.plan?.equipmentNo }}</strong><span>{{ w.plan?.customerName }} · {{ label(w.plan?.project) }} · 到期 {{ date(w.plan?.nextDate) }}</span></div>
          <span :class="['pill', w.level === 'RED' ? 'red' : 'yellow']">{{ w.remainDays < 0 ? '已逾期' : w.remainDays + '天' }}</span>
        </div>
        <div v-if="!data.maintenance?.length" class="muted" style="font-size:11px">暂无预警</div>
      </div>

      <div class="section-label">回款逾期</div>
      <div class="card" style="padding:4px 16px">
        <div v-for="w in data.payments || []" :key="w.plan?.id" class="list-row">
          <div class="main"><strong>{{ w.contract?.contractNo }}</strong><span>{{ w.plan?.phase }} · 应收 {{ money(w.plan?.amount) }}</span></div>
          <span :class="['pill', w.level === 'RED' ? 'red' : 'yellow']">逾期 {{ w.overdueDays }} 天</span>
        </div>
        <div v-if="!data.payments?.length" class="muted" style="font-size:11px">暂无预警</div>
      </div>

      <div class="section-label">需求超时</div>
      <div class="card" style="padding:4px 16px">
        <router-link v-for="w in data.demandFollows || []" :key="w.demand?.id" :to="`/admin/demands/${w.demand?.id}`" class="list-row">
          <div class="main"><strong class="mono">{{ w.demand?.demandNo }}</strong><span>{{ w.demand?.contactName }} · {{ w.demand?.region }}</span></div>
          <span class="pill red">超时 {{ w.overdueDays }} 天</span>
        </router-link>
        <div v-if="!data.demandFollows?.length" class="muted" style="font-size:11px">暂无预警</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { date, label, money } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter(); const data = ref<any>({}); const scanning = ref(false)
async function load() { data.value = await http.get('/admin/warnings') }
async function scan() { scanning.value = true; try { const r: any = await http.post('/admin/warnings/scan'); toast(`扫描完成，新增 ${r.created ?? 0} 条提醒`, 'success'); await load() } finally { scanning.value = false } }
onMounted(load)
</script>
