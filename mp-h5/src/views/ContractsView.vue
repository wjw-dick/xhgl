<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>我的合同</h1></div>
    <div class="page-body">
      <div v-if="loading" class="loading"><div class="spinner" /></div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px">
        <button v-for="c in rows" :key="c.id" class="contract-row" @click="open(c)">
          <div class="row-top"><strong class="mono">{{ c.contractNo }}</strong><span :class="['pill', statusPill(c.status)]">{{ label(c.status) }}</span></div>
          <div class="row-mid">{{ c.title }}</div>
          <div class="row-bottom"><span>{{ money(c.amount) }}</span><span>签署：{{ label(c.signStatus) }} ›</span></div>
        </button>
      </div>
      <div v-else class="empty"><div class="big">文</div>暂无合同</div>
    </div>
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ detail.contract.contractNo }}</h3><button class="modal-close" @click="detail = null">×</button></div>
        <div class="contract-info">
          <div><span>合同金额</span><strong>{{ money(detail.contract.amount) }}</strong></div>
          <div><span>审批</span><strong>{{ label(detail.contract.auditStatus) }}</strong></div>
          <div><span>签署</span><strong>{{ label(detail.contract.signStatus) }}</strong></div>
          <div><span>履约</span><strong>{{ label(detail.contract.status) }}</strong></div>
        </div>
        <div class="section-label">订单进度</div>
        <div v-if="detail.order" class="list-row"><div class="main"><strong>{{ detail.order.orderNo }}</strong><span>生产 {{ detail.order.productionProgress }} · 物流 {{ detail.order.deliveryStatus }}</span></div></div>
        <div v-else class="empty">暂无订单</div>
        <div class="section-label">收款计划</div>
        <div v-for="p in detail.payments" :key="p.id" class="list-row"><div class="main"><strong>{{ p.phase }}</strong><span>计划 {{ date(p.planDate) }}</span></div><span>{{ money(p.amount) }} · {{ label(p.status) }}</span></div>
        <div class="section-label">履约节点</div>
        <div v-for="i in detail.installations" :key="i.id" class="list-row"><div class="main"><strong>{{ {SHIPPED:'发货',ARRIVED:'到货',INSTALL:'安装',ACCEPTANCE:'验收'}[i.stage] || i.stage }}</strong><span>计划 {{ date(i.planDate) }}</span></div><span class="pill">{{ label(i.status) }}</span></div>
        <button v-if="detail.contract.auditStatus === 'APPROVED' && detail.contract.signStatus !== 'SIGNED'" class="btn btn-primary btn-block" style="margin-top:14px" @click="sign">在线签署合同</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, statusPill, money, date } from '@/utils'

const router = useRouter(); const rows = ref<any[]>([]); const loading = ref(false); const detail = ref<any>(null)
async function load() { loading.value = true; try { rows.value = await http.get('/customer/contracts') } finally { loading.value = false } }
async function open(c: any) { detail.value = await http.get(`/customer/contracts/${c.id}`) }
async function sign() {
  if (!detail.value) return
  if (!window.confirm('确认在线签署合同？')) return
  await http.post(`/customer/contracts/${detail.value.contract.id}/sign`); toast('合同已签署', 'success'); detail.value = await http.get(`/customer/contracts/${detail.value.contract.id}`); load()
}
onMounted(load)
</script>
<style scoped>
.contract-row { display: block; width: 100%; border: none; background: transparent; text-align: left; padding: 12px 0; border-bottom: 1px solid #f2f4f7; }
.contract-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; font-size: 11px; }
.row-mid { font-size: 13px; margin: 5px 0; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); }
.contract-info { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
.contract-info div { background: #fff; padding: 9px 10px; }
.contract-info span { display: block; font-size: 9px; color: var(--steel-2); }
.contract-info strong { font-size: 12px; }
</style>
