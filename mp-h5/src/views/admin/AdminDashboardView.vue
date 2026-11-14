<template>
  <div class="page admin-page">
    <div class="topbar">
      <h1>管理台</h1>
      <span class="right">ADMIN · {{ store.user?.name || '管理员' }}</span>
      <button class="icon-btn" :disabled="loading" aria-label="刷新" @click="load"><AppIcon name="refresh" :size="17" /></button>
    </div>
    <div class="page-body">
      <div class="greeting">
        <span class="eyebrow">BUSINESS WORKSPACE</span>
        <h1>业务工作台</h1>
        <p>{{ store.companyName }} · 客户、需求、合同与售后全局概览</p>
      </div>

      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <template v-else>
        <div class="overview-panel">
          <router-link to="/admin/customers"><span>客户档案</span><strong>{{ data?.customers ?? 0 }}</strong><em>建档客户</em></router-link>
          <router-link to="/admin/demands"><span>活跃需求</span><strong>{{ data?.activeDemands ?? 0 }}</strong><em>跟进中</em></router-link>
          <router-link to="/admin/contracts"><span>合同</span><strong>{{ data?.contracts ?? 0 }}</strong><em>履约中</em></router-link>
          <router-link to="/admin/tickets"><span>待处理</span><strong>{{ data?.pendingTickets ?? 0 }}</strong><em>售后工单</em></router-link>
        </div>

        <div class="grid-2" style="margin-top:10px">
          <div class="stat-card"><span>合同成交额</span><strong>{{ money(data?.contractAmount) }}</strong><em>累计合同金额</em></div>
          <div class="stat-card"><span>已回款</span><strong style="color:var(--green)">{{ money(data?.received) }}</strong><em>待回款 {{ money(data?.outstanding) }}</em></div>
          <div class="stat-card"><span>维保到期</span><strong>{{ data?.dueMaintenance ?? 0 }}</strong><em>30 天内计划</em></div>
          <div class="stat-card"><span>证照临期</span><strong>{{ data?.dueCertificates ?? 0 }}</strong><em>满意度 {{ data?.avgSatisfaction ?? '—' }}</em></div>
        </div>

        <div class="section-label">快捷办理</div>
        <div class="admin-actions">
          <router-link v-for="q in quickLinks" :key="q.path + q.title" :to="q.path" class="admin-action">
            <span class="admin-action-icon"><AppIcon :name="q.icon" :size="18" /></span>
            <span class="admin-action-copy"><strong>{{ q.title }}</strong><span>{{ q.desc }}</span></span>
            <AppIcon name="arrow" :size="15" class="admin-action-arrow" />
          </router-link>
        </div>

        <div class="section-label">最近动态</div>
        <div class="card">
          <div class="card-title"><h2>最近需求</h2><router-link to="/admin/demands">查看全部</router-link></div>
          <router-link v-for="d in data?.recentDemands || []" :key="d.id" :to="`/admin/demands/${d.id}`" class="list-row">
            <div class="main"><strong class="mono">{{ d.demandNo }}</strong><span>{{ d.contactName }} · {{ label(d.boilerType) }} · {{ d.capacity }}</span></div>
            <span :class="['pill', statusPill(d.status)]">{{ label(d.status) }}</span>
          </router-link>
          <div v-if="!(data?.recentDemands || []).length" class="empty"><div class="big">▤</div>暂无需求</div>
        </div>

        <div class="card">
          <div class="card-title"><h2>售后工单</h2><router-link to="/admin/tickets">查看全部</router-link></div>
          <router-link v-for="t in data?.recentTickets || []" :key="t.id" :to="`/admin/tickets/${t.id}`" class="list-row">
            <div class="main"><strong class="mono">{{ t.ticketNo }}</strong><span>{{ t.customerName }} · {{ label(t.type) }}</span></div>
            <span :class="['pill', statusPill(t.status)]">{{ label(t.status) }}</span>
          </router-link>
          <div v-if="!(data?.recentTickets || []).length" class="empty"><div class="big">✚</div>暂无工单</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import AppIcon from '@/components/AppIcon.vue'
import { sessionStore } from '@/stores/session'
import { label, money, statusPill } from '@/utils'

const store = sessionStore(); const data = ref<any>(null); const loading = ref(false)
const quickLinks = [
  { title: '客户档案', desc: '建档、联系人与关联视图', path: '/admin/customers', icon: 'users' },
  { title: '需求线索', desc: '认领、分配与阶段推进', path: '/admin/demands', icon: 'doc' },
  { title: '报价议价', desc: '审批、发送与客户确认', path: '/admin/quotes', icon: 'money' },
  { title: '合同履约', desc: '签署、回款、发票与验收', path: '/admin/contracts', icon: 'file' },
  { title: '售后服务', desc: '派单、完工与客户评价', path: '/admin/tickets', icon: 'tool' },
  { title: '维保计划', desc: '保养、年检与定检', path: '/admin/maintenance', icon: 'calendar' },
  { title: '报表中心', desc: '经营、回款与售后指标', path: '/admin/reports', icon: 'chart' },
  { title: '全部业务', desc: '产品、证书、系统与配置', path: '/admin/business', icon: 'grid' }
]
async function load() {
  loading.value = true
  try { data.value = await http.get('/admin/dashboard') } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.greeting { padding: 8px 2px 18px; }
.eyebrow { font-size: 9px; color: var(--accent); letter-spacing: 2px; font-weight: 650; }
.greeting h1 { font-size: 26px; margin: 7px 0 5px; letter-spacing: -1px; font-weight: 650; }
.greeting p { font-size: 10px; color: var(--steel); margin: 0; }
.icon-btn { width: 32px; height: 32px; border: 1px solid var(--line); background: var(--surface); border-radius: 9px; display: flex; align-items: center; justify-content: center; color: var(--steel); padding: 0; }
.admin-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.admin-action { display: grid; grid-template-columns: 32px 1fr 15px; align-items: center; gap: 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 10px; padding: 12px; }
.admin-action-icon { width: 32px; height: 32px; border-radius: 9px; background: #f5f1ea; color: var(--ink-2); display: flex; align-items: center; justify-content: center; }
.admin-action-copy strong { display: block; font-size: 11px; font-weight: 650; }
.admin-action-copy span { display: block; font-size: 8px; color: var(--steel-2); line-height: 1.5; margin-top: 1px; }
.admin-action-arrow { color: var(--steel-2); }
</style>
