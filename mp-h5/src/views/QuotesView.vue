<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>我的报价</h1></div>
    <div class="page-body">
      <div v-if="loading" class="loading"><div class="spinner" /></div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px">
        <button v-for="q in rows" :key="q.id" class="quote-row" @click="open(q)">
          <div class="row-top"><span class="mono">{{ q.quoteNo }}</span><span :class="['pill', statusPill(q.status)]">{{ label(q.status) }}</span></div>
          <div class="quote-mid"><strong>{{ q.productName }}</strong><span>{{ money(q.totalAmount) }}</span></div>
          <div class="row-bottom"><span>版本 v{{ q.version }} · 有效期 {{ date(q.validUntil) }}</span><span>›</span></div>
        </button>
      </div>
      <div v-else class="empty"><div class="big">¥</div>暂无报价</div>
    </div>
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ detail.quote.quoteNo }}</h3><button class="modal-close" @click="detail = null">×</button></div>
        <div class="quote-detail">
          <div><span>产品</span><strong>{{ detail.quote.productName }}</strong></div>
          <div><span>数量</span><strong>{{ detail.quote.quantity }}</strong></div>
          <div><span>含税总价</span><strong class="price">{{ money(detail.quote.totalAmount) }}</strong></div>
          <div><span>有效期</span><strong>{{ date(detail.quote.validUntil) }}</strong></div>
          <div><span>付款条件</span><strong>{{ detail.quote.paymentTerms }}</strong></div>
          <div><span>状态</span><strong>{{ label(detail.quote.status) }}</strong></div>
        </div>
        <div v-for="n in detail.negotiations" :key="n.id" class="list-row"><div class="main"><strong>第{{ n.roundNo }}轮 {{ n.party === 'CUSTOMER' ? '客户还价' : '我方报价' }}</strong><span>{{ n.remark }}</span></div><strong>{{ money(n.amount) }}</strong></div>
        <div v-if="['SENT','BARGAINING'].includes(detail.quote.status)" class="bargain">
          <div class="field"><label>期望总价（元）</label><input v-model.number="bargainAmount" type="number" /></div>
          <div class="field"><label>留言</label><input v-model="bargainRemark" placeholder="希望包含运费/维保" /></div>
          <div class="actions">
            <button class="btn btn-ghost btn-sm" @click="bargain">提交还价</button>
            <button class="btn btn-primary btn-sm" @click="confirm">确认报价</button>
          </div>
        </div>
        <div v-else-if="detail.quote.status === 'CONFIRMED'" class="notice blue" style="margin-top:12px">报价已确认，合同草稿已生成。</div>
        <div v-else-if="detail.quote.status === 'EXPIRED'" class="notice" style="margin-top:12px">报价已过期，请联系顾问重新报价。</div>
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
const bargainAmount = ref<number>(); const bargainRemark = ref('')
async function load() { loading.value = true; try { rows.value = await http.get('/customer/quotes') } finally { loading.value = false } }
async function open(q: any) { detail.value = await http.get(`/customer/quotes/${q.id}`); bargainAmount.value = Number(q.totalAmount || 0) * 0.95; bargainRemark.value = '' }
async function bargain() {
  if (!detail.value || !bargainAmount.value) return toast('请输入还价金额', 'error')
  await http.post(`/customer/quotes/${detail.value.quote.id}/bargain`, { amount: bargainAmount.value, remark: bargainRemark.value })
  toast('还价已提交', 'success'); detail.value = await http.get(`/customer/quotes/${detail.value.quote.id}`); load()
}
async function confirm() {
  if (!detail.value) return
  if (!window.confirm('确认该报价并生成合同草稿？')) return
  await http.post(`/customer/quotes/${detail.value.quote.id}/confirm`); toast('报价已确认', 'success'); detail.value = null; load()
}
onMounted(load)
</script>
<style scoped>
.quote-row { display: block; width: 100%; border: none; background: transparent; text-align: left; padding: 12px 0; border-bottom: 1px solid #f2f4f7; }
.quote-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel); }
.quote-mid { display: flex; justify-content: space-between; align-items: baseline; margin: 6px 0 3px; }
.quote-mid strong { font-size: 14px; }
.quote-mid span { font-size: 16px; font-weight: 700; color: var(--accent); }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); }
.quote-detail { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border-radius: 8px; overflow: hidden; border: 1px solid var(--line); }
.quote-detail div { background: #fff; padding: 9px 10px; }
.quote-detail span { display: block; font-size: 9px; color: var(--steel-2); }
.quote-detail strong { font-size: 12px; }
.quote-detail .price { color: var(--accent); font-size: 16px; }
.bargain { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.actions { display: flex; gap: 8px; justify-content: flex-end; }
</style>
