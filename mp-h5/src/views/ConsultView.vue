<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>在线咨询</h1></div>
    <div class="page-body">
      <div class="card">
        <div class="form-grid">
          <div class="field"><label>联系人</label><input v-model="form.contactName" /></div>
          <div class="field"><label>电话</label><input v-model="form.phone" /></div>
          <div class="field full"><label>咨询内容 *</label><textarea v-model="form.question" rows="5" placeholder="请描述锅炉用途、蒸汽用量、燃料与期望时间" /></div>
        </div>
        <button class="btn btn-primary btn-block" style="margin-top:12px" :disabled="loading" @click="submit">{{ loading ? '发送中…' : '发送咨询' }}</button>
      </div>
      <div v-if="result" class="card answer-card">
        <div class="answer-head"><span class="bot">鑫</span><div><strong>{{ result.autoAnswered ? 'FAQ 即时回复' : '已转人工顾问' }}</strong><span>{{ result.autoAnswered ? '命中知识库' : '顾问会尽快回复' }}</span></div></div>
        <p>{{ result.answer }}</p>
      </div>
      <div class="card">
        <div class="card-title"><h2>常见问题</h2></div>
        <button v-for="f in faqs" :key="f.id" class="faq" @click="form.question = f.question"><strong>{{ f.question }}</strong><span>{{ f.answer }}</span></button>
        <div v-if="!faqs.length" class="empty">FAQ 加载中</div>
      </div>
      <div class="footer-tip">急事可致电 400-800-6688</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const router = useRouter(); const store = sessionStore()
const form = reactive<any>({ question: '' }); const loading = ref(false); const result = ref<any>(null); const faqs = ref<any[]>([])
async function submit() {
  if (!form.question) return toast('请输入咨询内容', 'error')
  loading.value = true
  try {
    result.value = await http.post('/public/consultations', { ...form, source: 'MP', customerName: store.customer?.name })
    toast('咨询已提交', 'success')
  } finally { loading.value = false }
}
onMounted(async () => {
  form.contactName = store.user?.name || store.customer?.name || ''
  try { faqs.value = await http.get('/public/faqs') } catch {}
})
</script>
<style scoped>
.answer-card { background: #f6fbf8; border-color: #cfe3d7; }
.answer-head { display: flex; gap: 10px; align-items: center; }
.bot { width: 32px; height: 32px; background: var(--ink); color: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 13px; }
.answer-head strong { display: block; font-size: 12px; }
.answer-head span { font-size: 10px; color: var(--steel); }
.answer-card p { margin: 12px 0 0; font-size: 13px; line-height: 1.8; color: #234d38; white-space: pre-line; }
.faq { display: block; width: 100%; text-align: left; border: none; border-bottom: 1px dashed var(--line); background: transparent; padding: 10px 0; }
.faq strong { display: block; font-size: 12px; }
.faq span { display: block; font-size: 11px; color: var(--steel); overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.faq:last-child { border-bottom: none; }
</style>
