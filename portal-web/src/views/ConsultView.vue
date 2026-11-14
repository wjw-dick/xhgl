<template>
  <div class="container page-pad">
    <div class="consult-head">
      <span class="kicker">PRE-SALES CONSULTATION</span>
      <h1>在线咨询</h1>
      <p>7×24 小时提交问题；FAQ 命中即时答复，未命中自动转人工并沉淀到您的客户档案。</p>
    </div>
    <div class="consult-layout">
      <div class="consult-main">
        <div class="card form-card">
          <div class="form-grid">
            <div class="field"><label>企业名称</label><input v-model="form.customerName" placeholder="选填，便于顾问查阅档案" /></div>
            <div class="field"><label>联系人</label><input v-model="form.contactName" placeholder="姓名" /></div>
            <div class="field"><label>联系电话</label><input v-model="form.phone" placeholder="便于人工回访" /></div>
            <div class="field"><label>咨询主题</label><select v-model="topic"><option value="选型">锅炉选型</option><option value="价格">价格与预算</option><option value="资质">资质与合规</option><option value="安装">安装与验收</option><option value="售后">售后与维保</option></select></div>
            <div class="field full"><label>咨询内容 *</label><textarea v-model="form.question" rows="5" :placeholder="questionPlaceholder" /></div>
          </div>
          <div class="form-actions"><button class="btn btn-primary" :disabled="submitting" @click="submit">{{ submitting ? '提交中…' : '发送咨询' }}</button></div>
        </div>
        <div v-if="result" class="answer-card">
          <div class="answer-head">
            <div class="bot-avatar">鑫</div>
            <div><strong>{{ result.autoAnswered ? '智能 FAQ 即时回复' : '已转人工顾问' }}</strong><span>{{ result.autoAnswered ? '该问题已命中知识库，如需进一步确认可继续转人工' : '顾问将在工作时间内尽快回复' }}</span></div>
            <span class="pill" :class="result.autoAnswered ? 'green' : 'yellow'">{{ result.autoAnswered ? '已答复' : '等待顾问' }}</span>
          </div>
          <div class="answer-content">{{ result.answer }}</div>
          <div class="answer-tip">咨询记录已提交，可在客户中心查看后续答复。</div>
        </div>
      </div>
      <aside class="faq-aside">
        <div class="card">
          <h3>常见问题</h3>
          <button v-for="f in faqs" :key="f.id" class="faq-item" @click="fillFaq(f)">
            <strong>{{ f.question }}</strong>
            <span>{{ f.answer }}</span>
          </button>
          <div v-if="!faqs.length" class="muted">FAQ 正在加载。</div>
        </div>
        <div class="consult-hotline">
          <span>急事直接致电</span>
          <strong>400-800-6688</strong>
          <em>或微信公众号回复“找顾问”</em>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const route = useRoute(); const store = sessionStore()
const form = reactive<any>({ question: '', contactName: store.user?.name || store.customer?.name || '' }); const submitting = ref(false); const result = ref<any>(null)
const faqs = ref<any[]>([]); const topic = ref('选型')
const questionPlaceholder = ref('请描述锅炉用途、蒸汽/热水用量、燃料、场地与期望时间，便于工程师判断。')
function fillFaq(f: any) { form.question = f.question }
async function loadFaqs() {
  try { faqs.value = await http.get('/public/faqs') } catch {}
}
async function submit() {
  if (!form.question) return toast('请输入咨询内容', 'error')
  submitting.value = true
  try {
    result.value = await http.post('/public/consultations', { ...form, source: 'H5' })
    toast('咨询已提交', 'success')
  } finally { submitting.value = false }
}
onMounted(async () => {
  await loadFaqs()
  const productId = route.query.product
  if (productId) {
    try {
      const data: any = await http.get(`/public/products/${productId}`)
      questionPlaceholder.value = `我想咨询 ${data.product.model}（${data.product.capacity}）的选型、价格与交付周期。`
      form.question = questionPlaceholder.value
    } catch {}
  }
})
</script>

<style scoped>
.consult-head { max-width: 680px; margin-bottom: 30px; }
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
.consult-head h1 { font-size: 36px; margin: 10px 0 8px; letter-spacing: -1px; }
.consult-head p { color: var(--steel); font-size: 13px; margin: 0; }
.consult-layout { display: grid; grid-template-columns: 1.5fr .8fr; gap: 28px; align-items: start; }
.form-card { padding: 28px; }
.answer-card { margin-top: 18px; border: 1px solid #cfe3d7; background: #f6fbf8; padding: 22px; }
.answer-head { display: flex; align-items: center; gap: 12px; }
.bot-avatar { width: 36px; height: 36px; background: var(--ink); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.answer-head strong { display: block; font-size: 13px; }
.answer-head span { display: block; color: var(--steel); font-size: 11px; margin-top: 2px; }
.answer-head .pill { margin-left: auto; }
.answer-content { margin: 14px 0 8px; font-size: 14px; line-height: 1.9; color: #234d38; white-space: pre-line; }
.answer-tip { font-size: 10px; color: var(--steel-2); }
.faq-item { display: block; width: 100%; text-align: left; border: none; border-bottom: 1px dashed var(--line); background: transparent; padding: 11px 0; }
.faq-item strong { display: block; font-size: 12px; color: var(--ink); }
.faq-item span { display: block; color: var(--steel); font-size: 11px; line-height: 1.7; margin-top: 3px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.faq-item:hover strong { color: var(--accent); }
.faq-aside h3 { font-size: 16px; margin: 0 0 8px; }
.consult-hotline { margin-top: 16px; background: #101b2e; color: #fff; padding: 22px; }
.consult-hotline span { font-size: 10px; color: #8fa0b8; letter-spacing: 1px; }
.consult-hotline strong { display: block; font-size: 24px; margin: 4px 0; }
.consult-hotline em { font-style: normal; font-size: 10px; color: #8fa0b8; }
@media (max-width: 800px) { .consult-layout { grid-template-columns: 1fr; } .consult-head h1 { font-size: 30px; } }
</style>
