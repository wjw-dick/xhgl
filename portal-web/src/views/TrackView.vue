<template>
  <div class="container page-pad">
    <div class="track-head">
      <span class="kicker">REQUIREMENT TRACKING</span>
      <h1>需求进度查询</h1>
      <p>输入需求编号与手机号，或使用分享链接中的查询凭证，查看需求阶段、方案与报价。</p>
    </div>
    <div class="track-layout">
      <div class="track-form card">
        <div class="field"><label>需求编号</label><input v-model="demandNo" placeholder="如 HLJ-DEMAND-2026-0001" /></div>
        <div class="field" style="margin-top:14px"><label>手机号（后四位也可验证）</label><input v-model="phone" placeholder="提交需求时填写的联系电话" /></div>
        <button class="btn btn-primary btn-block" style="margin-top:18px" :disabled="loading" @click="query">{{ loading ? '查询中…' : '查询进度' }}</button>
        <div class="notice blue" style="margin-top:16px">查询凭证不会展示合同金额、内部审批意见或其他客户信息。</div>
      </div>
      <div v-if="result" class="track-result">
        <div class="result-head">
          <div><span class="muted mono">{{ result.demandNo }}</span><h2>{{ label(result.status) }}</h2></div>
          <span :class="['pill', statusPill(result.status)]">{{ label(result.status) }}</span>
        </div>
        <div class="progress">
          <div v-for="(s, i) in stages" :key="s.value" class="step" :class="{ done: stageIndex >= i, active: result.status === s.value }">
            <span class="dot" /><em>{{ s.label }}</em>
          </div>
        </div>
        <div class="summary">
          <div><span>锅炉选型</span><strong>{{ label(result.boilerType) }} · {{ result.capacity }} · {{ label(result.fuelType) }}</strong></div>
          <div><span>期望地区</span><strong>{{ result.region || '—' }}</strong></div>
          <div><span>提交时间</span><strong>{{ dateTime(result.createdAt) }}</strong></div>
        </div>
        <div v-if="result.solutions?.length" class="related">
          <h3>方案</h3>
          <div v-for="s in result.solutions" :key="s.id" class="related-row"><span>{{ s.name }} <em>v{{ s.version }}</em></span><strong>{{ label(s.status) }}</strong></div>
        </div>
        <div v-if="result.quotes?.length" class="related">
          <h3>已确认报价</h3>
          <div v-for="q in result.quotes" :key="q.id" class="related-row"><span>{{ q.quoteNo }}</span><strong>{{ money(q.totalAmount) }}</strong></div>
        </div>
        <div v-if="result.timeline?.length" class="timeline">
          <h3>关键进度</h3>
          <div v-for="t in result.timeline" :key="t.id" class="tl-row"><span>{{ dateTime(t.followTime) }}</span><p>{{ t.content }}</p></div>
        </div>
      </div>
      <div v-else class="empty"><h3>等待查询</h3><p>需求编号由提交成功后生成；分享链接自带查询凭证。</p><router-link to="/demand" class="btn btn-primary">提交采购需求</router-link></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, statusPill, money, dateTime } from '@/utils'

const route = useRoute()
const demandNo = ref((route.query.demandNo as string) || '')
const phone = ref((route.query.phone as string) || '')
const token = ref((route.query.token as string) || '')
const loading = ref(false); const result = ref<any>(null)
const stages = [{ label: '新需求', value: 'NEW' }, { label: '咨询中', value: 'CONSULTING' }, { label: '介绍中', value: 'INTRODUCING' }, { label: '谈价中', value: 'TALKING' }, { label: '成交', value: 'DEAL' }]
const stageIndex = computed(() => {
  const idx = stages.findIndex((s) => s.value === result.value?.status)
  return idx
})
async function query() {
  if (!demandNo.value) return toast('请输入需求编号', 'error')
  if (!phone.value && !token.value) return toast('请输入手机号或使用分享链接', 'error')
  loading.value = true
  try {
    result.value = await http.get('/public/demands/track', { params: { demandNo: demandNo.value, phone: phone.value, token: token.value } })
  } finally { loading.value = false }
}
onMounted(() => { if (demandNo.value) query() })
</script>

<style scoped>
.track-head { max-width: 720px; margin-bottom: 32px; }
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
.track-head h1 { font-size: 36px; margin: 10px 0 8px; letter-spacing: -1px; }
.track-head p { color: var(--steel); font-size: 13px; margin: 0; }
.track-layout { display: grid; grid-template-columns: .8fr 1.2fr; gap: 28px; align-items: start; }
.track-form { max-width: 420px; }
.track-result { background: #fff; border: 1px solid var(--line); padding: 30px; }
.result-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.result-head h2 { font-size: 25px; margin: 4px 0 0; }
.progress { display: flex; justify-content: space-between; position: relative; margin: 28px 0; }
.progress::before { content: ''; position: absolute; left: 8px; right: 8px; top: 5px; height: 1px; background: var(--line); }
.step { display: flex; flex-direction: column; align-items: center; gap: 8px; position: relative; z-index: 1; flex: 1; }
.step .dot { width: 10px; height: 10px; border: 1px solid var(--steel-2); background: #fff; border-radius: 50%; }
.step.done .dot { background: var(--accent); border-color: var(--accent); }
.step.active .dot { box-shadow: 0 0 0 4px var(--accent-soft); }
.step em { font-style: normal; font-size: 10px; color: var(--steel-2); }
.step.done em { color: var(--ink); }
.summary { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); }
.summary div { background: #fff; padding: 12px 14px; }
.summary span { display: block; font-size: 10px; color: var(--steel-2); }
.summary strong { display: block; margin-top: 4px; font-size: 12px; }
.related { margin-top: 22px; }
.related h3, .timeline h3 { font-size: 14px; margin: 0 0 8px; }
.related-row { display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px dashed var(--line); padding: 8px 0; font-size: 12px; }
.related-row em { font-style: normal; color: var(--steel-2); font-size: 10px; }
.timeline { margin-top: 22px; }
.tl-row { display: grid; grid-template-columns: 120px 1fr; gap: 12px; padding: 7px 0; border-bottom: 1px dashed var(--line); font-size: 11px; }
.tl-row span { color: var(--steel-2); }
.tl-row p { margin: 0; color: var(--steel); }
@media (max-width: 800px) { .track-layout { grid-template-columns: 1fr; } .track-form { max-width: none; } }
</style>
