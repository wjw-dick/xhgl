<template>
  <div class="container page-pad" style="max-width: 860px">
    <div v-if="loading" class="loading"><div class="spinner" />加载报价…</div>
    <div v-else-if="error" class="empty"><h3>报价链接无效</h3><p>{{ error }}</p></div>
    <template v-else-if="data">
      <div class="share-head">
        <div>
          <span class="kicker">QUOTATION</span>
          <h1>锅炉设备报价单</h1>
          <p class="mono">{{ quote.quoteNo }} · 版本 {{ quote.version }} · {{ data.company?.name }}</p>
        </div>
        <span :class="['pill', statusPill(quote.status)]">{{ label(quote.status) }}</span>
      </div>
      <div class="quote-box">
        <div class="quote-table">
          <div class="quote-row head"><span>产品型号</span><span>数量</span><span>单价</span><span>金额</span></div>
          <div class="quote-row"><span>{{ quote.productName }}</span><span>{{ quote.quantity }}</span><span>{{ money(quote.unitPrice) }}</span><span>{{ money(quote.amount) }}</span></div>
        </div>
        <div class="fee-list">
          <div><span>运费</span><strong>{{ money(quote.freight) }}</strong></div>
          <div><span>安装调试费</span><strong>{{ money(quote.installFee) }}</strong></div>
          <div class="total"><span>含税总价</span><strong>{{ money(quote.totalAmount) }}</strong></div>
        </div>
        <div class="quote-meta">
          <div class="spec"><span>付款条件</span><strong>{{ quote.paymentTerms }}</strong></div>
          <div class="spec"><span>质保期</span><strong>{{ quote.warranty }}</strong></div>
          <div class="spec"><span>有效期至</span><strong>{{ date(quote.validUntil) }}</strong></div>
          <div class="spec"><span>客户</span><strong>{{ quote.customerName }}</strong></div>
        </div>
      </div>

      <div class="card" style="margin-top:18px">
        <h3 style="margin:0 0 10px">议价记录</h3>
        <div v-for="n in data.negotiations" :key="n.id" class="mini-row">
          <span>第 {{ n.roundNo }} 轮 · {{ n.party === 'CUSTOMER' ? '客户还价' : '我方报价' }}<em>{{ n.remark }}</em></span>
          <strong>{{ money(n.amount) }}</strong>
        </div>
        <div v-if="!data.negotiations?.length" class="muted">暂无议价记录。</div>
      </div>

      <div v-if="data.canConfirm" class="action-grid">
        <div class="card">
          <h3>在线确认报价</h3>
          <p class="muted">确认后系统自动生成合同草稿，工作人员将与您预约签署。</p>
          <button class="btn btn-primary btn-block" :disabled="submitting" @click="confirm">确认本报价</button>
        </div>
        <div class="card">
          <h3>还价 / 留言</h3>
          <div class="field"><label>期望总价（元）</label><input v-model.number="bargainAmount" type="number" /></div>
          <div class="field" style="margin-top:10px"><label>说明</label><input v-model="bargainRemark" placeholder="希望包含运费/安装/维保等" /></div>
          <button class="btn btn-ghost btn-block" style="margin-top:12px" @click="bargain">提交还价</button>
        </div>
      </div>
      <div v-else-if="quote.status === 'CONFIRMED'" class="notice green" style="margin-top:18px">该报价已确认，合同草稿已生成。感谢您的信任。</div>
      <div v-else-if="quote.status === 'EXPIRED'" class="notice" style="margin-top:18px">报价已过有效期，无法直接确认；请联系顾问重新报价。</div>
      <div class="notice blue" style="margin-top:18px">本报价单由鑫火锅炉平台生成，最终以双方签署合同为准。锅炉制造许可、监检、能效报告随合同附交。</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, money, date, statusPill } from '@/utils'

const route = useRoute(); const token = route.query.token as string
const data = ref<any>(null); const loading = ref(true); const error = ref(''); const submitting = ref(false)
const quote = computed(() => data.value?.quote || {})
const bargainAmount = ref<number>(); const bargainRemark = ref('')
async function load() { data.value = await http.get('/public/share/quote', { params: { token } }) }
async function confirm() {
  if (!window.confirm('确认该报价？确认后将生成合同草稿。')) return
  submitting.value = true
  try {
    await http.post('/public/share/quote/confirm', null, { params: { token } })
    toast('报价已确认，合同草稿已生成', 'success'); await load()
  } finally { submitting.value = false }
}
async function bargain() {
  if (!bargainAmount.value) return toast('请输入还价金额', 'error')
  await http.post('/public/share/quote/bargain', { amount: bargainAmount.value, remark: bargainRemark.value }, { params: { token } })
  toast('还价已提交，顾问会尽快联系您', 'success'); await load()
}
onMounted(async () => {
  try { await load(); bargainAmount.value = Number(quote.value.totalAmount || 0) * 0.95 }
  catch (e: any) { error.value = e?.message || '链接无效' } finally { loading.value = false }
})
</script>

<style scoped>
.share-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 22px; }
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
.share-head h1 { font-size: 32px; margin: 8px 0 4px; }
.share-head p { color: var(--steel); font-size: 11px; margin: 0; }
.quote-box { background: #fff; border: 1px solid var(--line); padding: 24px; }
.quote-table { border: 1px solid var(--line); }
.quote-row { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; }
.quote-row.head { background: var(--ink); color: #fff; }
.quote-row span { padding: 11px 14px; font-size: 12px; border-right: 1px solid var(--line); }
.quote-row.head span { border-color: rgba(255,255,255,.12); }
.fee-list { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; padding: 16px 0; border-bottom: 1px solid var(--line); }
.fee-list div { display: flex; gap: 30px; font-size: 12px; }
.fee-list div span { color: var(--steel); }
.fee-list .total { font-size: 16px; margin-top: 6px; }
.fee-list .total strong { color: var(--accent); font-size: 26px; }
.quote-meta { padding-top: 8px; }
.mini-row { display: flex; justify-content: space-between; gap: 14px; border-bottom: 1px dashed var(--line); padding: 9px 0; font-size: 12px; }
.mini-row em { font-style: normal; color: var(--steel-2); font-size: 10px; margin-left: 8px; }
.action-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 18px; }
.action-grid h3 { margin: 0 0 8px; font-size: 16px; }
.action-grid p { font-size: 12px; }
@media (max-width: 700px) { .action-grid { grid-template-columns: 1fr; } .share-head h1 { font-size: 26px; } .quote-row { grid-template-columns: 1.5fr 1fr 1fr 1fr; } }
</style>
