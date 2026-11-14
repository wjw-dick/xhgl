<template>
  <div class="container page-pad" style="max-width: 920px">
    <div v-if="loading" class="loading"><div class="spinner" />加载方案…</div>
    <div v-else-if="error" class="empty"><h3>分享链接无效</h3><p>{{ error }}</p><router-link to="/" class="btn btn-primary">返回首页</router-link></div>
    <template v-else-if="data">
      <div class="share-head">
        <div><span class="kicker">BOILER SOLUTION</span><span class="mono" style="margin-left:10px">{{ data.solution.solutionNo }}</span><h1>{{ data.solution.name }}</h1><p>{{ data.company?.name }} · 版本 {{ data.solution.version }}</p></div>
        <span :class="['pill', statusPill(data.solution.status)]">{{ label(data.solution.status) }}</span>
      </div>
      <div class="share-grid">
        <div class="card">
          <div class="rich-html" v-html="sanitizeHtml(data.solution.content)"></div>
        </div>
        <aside>
          <div class="side-card dark">
            <h3>项目摘要</h3>
            <div class="spec"><span>锅炉选型</span><strong>{{ label(data.demand?.boilerType) }} / {{ data.demand?.capacity }}</strong></div>
            <div class="spec"><span>燃料</span><strong>{{ label(data.demand?.fuelType) }}</strong></div>
            <div class="spec"><span>压力</span><strong>{{ label(data.demand?.pressureLevel) }}</strong></div>
            <div class="spec"><span>推荐型号</span><strong>{{ data.product?.model || data.solution.productName }}</strong></div>
          </div>
          <div class="side-card">
            <h3>合规材料</h3>
            <div v-for="c in data.certificates || []" :key="c.certType" class="mini-row"><span>{{ label(c.certType) }}</span><strong>{{ label(c.status) }}</strong></div>
            <div class="notice blue">证书编号已脱敏；可在门户资质核验页输入完整编号验证。</div>
          </div>
          <div class="side-card action-card">
            <h3>{{ data.solution.status === 'CONFIRMED' ? '方案已确认' : '确认采用本方案' }}</h3>
            <p>{{ data.solution.status === 'CONFIRMED' ? '感谢确认，业务人员将与您联系报价。' : '确认后业务人员将准备正式报价，进入谈价环节。' }}</p>
            <button v-if="data.solution.status !== 'CONFIRMED'" class="btn btn-primary btn-block" :disabled="submitting" @click="confirm">确认方案</button>
            <div class="contact">顾问电话：{{ data.company?.contactPhone || '400-800-6688' }}</div>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, statusPill, sanitizeHtml } from '@/utils'

const route = useRoute(); const token = route.query.token as string
const data = ref<any>(null); const loading = ref(true); const error = ref(''); const submitting = ref(false)
async function confirm() {
  submitting.value = true
  try {
    await http.post('/public/share/solution/confirm', null, { params: { token } })
    toast('方案已确认，业务人员将尽快联系您', 'success')
    data.value.solution.status = 'CONFIRMED'
  } finally { submitting.value = false }
}
onMounted(async () => {
  try { data.value = await http.get('/public/share/solution', { params: { token } }) }
  catch (e: any) { error.value = e?.message || '链接无效或已失效' } finally { loading.value = false }
})
</script>

<style scoped>
.share-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 24px; }
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
.share-head h1 { font-size: 32px; margin: 8px 0 4px; letter-spacing: -.6px; }
.share-head p { color: var(--steel); font-size: 12px; margin: 0; }
.share-grid { display: grid; grid-template-columns: 1.5fr .8fr; gap: 22px; align-items: start; }
.side-card { background: #fff; border: 1px solid var(--line); padding: 20px; margin-bottom: 14px; }
.side-card.dark { background: #101b2e; color: #fff; border-color: #101b2e; }
.side-card.dark .spec { border-color: rgba(255,255,255,.1); }
.side-card.dark .spec span { color: #93a3b8; }
.side-card h3 { font-size: 16px; margin: 0 0 10px; }
.mini-row { display: flex; justify-content: space-between; gap: 10px; border-bottom: 1px dashed var(--line); padding: 7px 0; font-size: 11px; }
.action-card p { color: var(--steel); font-size: 12px; }
.contact { margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--line); font-size: 10px; color: var(--steel); }
@media (max-width: 800px) { .share-grid { grid-template-columns: 1fr; } .share-head h1 { font-size: 26px; } }
</style>
