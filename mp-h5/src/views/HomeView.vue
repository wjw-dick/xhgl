<template>
  <div class="page">
    <section class="hero home-hero">
      <div class="hero-top">
        <div>
          <span class="eyebrow">{{ store.companyName }} · CLIENT DESK</span>
          <h1>{{ store.user?.name || store.customer?.name || '您好' }}</h1>
          <p>需求、方案、报价、合同与售后进度，一站式查看。</p>
        </div>
        <router-link to="/profile" class="avatar">{{ (store.user?.name || store.customer?.name || '客').slice(0, 1) }}</router-link>
      </div>
      <div class="hero-actions">
        <router-link to="/demands/new" class="btn btn-accent btn-sm">＋ 提交需求</router-link>
        <router-link to="/consult" class="btn btn-light btn-sm">在线咨询</router-link>
      </div>
    </section>
    <div class="page-body">
      <div class="overview-panel">
        <router-link to="/demands"><span>我的需求</span><strong>{{ overview?.demandCount || 0 }}</strong><em>查看进度</em></router-link>
        <router-link to="/solutions"><span>方案</span><strong>{{ overview?.solutionCount || 0 }}</strong><em>待确认</em></router-link>
        <router-link to="/quotes"><span>报价</span><strong>{{ overview?.quoteCount || 0 }}</strong><em>在线确认</em></router-link>
        <router-link to="/service"><span>工单</span><strong>{{ overview?.ticketCount || 0 }}</strong><em>售后进度</em></router-link>
      </div>

      <div class="action-tiles">
        <router-link to="/demands/new" class="action-tile">
          <span class="tile-icon"><AppIcon name="plus" :size="17" /></span>
          <span><strong>提交新需求</strong><span>登记锅炉采购参数</span></span>
        </router-link>
        <router-link to="/consult" class="action-tile">
          <span class="tile-icon"><AppIcon name="chat" :size="17" /></span>
          <span><strong>在线咨询</strong><span>工程师快速响应</span></span>
        </router-link>
      </div>

      <div class="section-label">常用服务</div>
      <div class="card service-list">
        <router-link v-for="item in services" :key="item.path + item.title" :to="item.path" class="service-row">
          <span class="service-icon"><AppIcon :name="item.icon" :size="18" /></span>
          <span class="service-copy"><strong>{{ item.title }}</strong><span>{{ item.desc }}</span></span>
          <AppIcon name="arrow" :size="16" class="service-arrow" />
        </router-link>
      </div>

      <div v-if="overview?.maintenanceWarnings?.length" class="card" style="margin-top:14px">
        <div class="card-title"><h2>维保 / 定检提醒</h2><router-link to="/service">全部</router-link></div>
        <div v-for="m in overview.maintenanceWarnings" :key="m.id" class="list-row">
          <div class="main"><strong>{{ m.equipmentNo }} · {{ label(m.project) }}</strong><span>到期 {{ date(m.nextDate) }} · 周期 {{ m.cycleMonths }} 个月</span></div>
          <span :class="['pill', daysLeft(m.nextDate) < 0 ? 'red' : 'yellow']">{{ daysLeft(m.nextDate) < 0 ? '已逾期' : '剩余' + daysLeft(m.nextDate) + '天' }}</span>
        </div>
      </div>

      <div class="section-label">最近动态</div>
      <div class="card">
        <div class="card-title"><h2>最近需求</h2><router-link to="/demands">全部</router-link></div>
        <router-link v-for="d in overview?.recentDemands || []" :key="d.id" to="/demands" class="list-row">
          <div class="main"><strong class="mono">{{ d.demandNo }}</strong><span>{{ label(d.boilerType) }} · {{ d.capacity }} · {{ label(d.fuelType) }}</span></div>
          <span :class="['pill', statusPill(d.status)]">{{ label(d.status) }}</span>
        </router-link>
        <div v-if="!overview?.recentDemands?.length" class="empty"><div class="big">▤</div>还没有需求，去提交第一条吧<router-link to="/demands/new" style="color:var(--accent)"> 提交 →</router-link></div>
      </div>

      <div class="card">
        <div class="card-title"><h2>售后工单</h2><router-link to="/service">全部</router-link></div>
        <div v-for="t in overview?.recentTickets || []" :key="t.id" class="list-row">
          <div class="main"><strong>{{ t.ticketNo }} · {{ label(t.type) }}</strong><span>{{ t.description }}</span></div>
          <span :class="['pill', statusPill(t.status)]">{{ label(t.status) }}</span>
        </div>
        <div v-if="!overview?.recentTickets?.length" class="empty"><div class="big">✚</div>暂无售后工单</div>
      </div>

      <div class="card">
        <div class="card-title"><h2>公告</h2><router-link to="/profile">更多</router-link></div>
        <div v-for="a in announcements" :key="a.id" class="list-row">
          <div class="main"><strong>{{ a.title }}</strong><span>{{ date(a.publishTime) }} · {{ a.source === 'SYSTEM' ? '系统' : '企业' }}</span></div>
        </div>
        <div v-if="!announcements.length" class="empty">暂无公告</div>
      </div>
      <div class="footer-tip">数据仅本人/本企业可见</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import AppIcon from '@/components/AppIcon.vue'
import { sessionStore } from '@/stores/session'
import { label, date, daysLeft, statusPill } from '@/utils'

const store = sessionStore()
const overview = ref<any>(null); const announcements = ref<any[]>([])
const services = [
  { title: '锅炉方案', desc: '选型建议与在线确认', path: '/solutions', icon: 'layers' },
  { title: '报价单', desc: '查看报价与议价记录', path: '/quotes', icon: 'money' },
  { title: '合同', desc: '签署与履约进度', path: '/contracts', icon: 'file' },
  { title: '售后服务', desc: '报修、工单与满意度', path: '/service', icon: 'tool' },
  { title: '维保提醒', desc: '保养、年检与定检计划', path: '/service', icon: 'calendar' }
]
onMounted(async () => {
  try { overview.value = await http.get('/customer/overview') } catch {}
  try { announcements.value = (await http.get('/public/announcements')) as any[] } catch {}
})
</script>
<style scoped>
.home-hero { padding-bottom: 26px; }
.hero-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.hero h1 { font-size: 27px; }
.avatar { width: 36px; height: 36px; border-radius: 9px; background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.18); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; }
.service-list { padding: 2px 16px; }
.service-row { display: grid; grid-template-columns: 32px 1fr 16px; gap: 11px; align-items: center; padding: 13px 0; border-bottom: 1px solid #f0ece4; }
.service-row:last-child { border-bottom: none; }
.service-icon { width: 32px; height: 32px; border-radius: 9px; background: #f5f1ea; color: var(--ink-2); display: flex; align-items: center; justify-content: center; }
.service-copy strong { display: block; font-size: 12px; font-weight: 650; }
.service-copy span { display: block; font-size: 9px; color: var(--steel-2); margin-top: 1px; }
.service-arrow { color: var(--steel-2); }
</style>
