<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>我的方案</h1></div>
    <div class="page-body">
      <div v-if="loading" class="loading"><div class="spinner" /></div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px">
        <div v-for="s in rows" :key="s.id" class="solution-row">
          <div class="row-top"><span class="mono">{{ s.solutionNo }}</span><span :class="['pill', statusPill(s.status)]">{{ label(s.status) }}</span></div>
          <h3>{{ s.name }}</h3>
          <p>{{ s.productName }} · 版本 {{ s.version }} · 浏览 {{ s.viewCount }}</p>
          <div class="actions">
            <button class="btn btn-ghost btn-sm" @click="open(s)">查看方案</button>
            <button v-if="s.status !== 'CONFIRMED'" class="btn btn-primary btn-sm" @click="confirm(s)">确认方案</button>
            <span v-else class="pill green">已确认</span>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">方</div>暂无方案<p>工作人员推送后会出现在这里。</p></div>
    </div>
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ detail.name }}</h3><button class="modal-close" @click="detail = null">×</button></div>
        <div class="rich-content" v-html="sanitizeHtml(detail.content)"></div>
        <button v-if="detail.status !== 'CONFIRMED'" class="btn btn-primary btn-block" style="margin-top:14px" @click="confirm(detail); detail = null">确认方案</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, statusPill, sanitizeHtml } from '@/utils'

const router = useRouter(); const rows = ref<any[]>([]); const loading = ref(false); const detail = ref<any>(null)
async function load() { loading.value = true; try { rows.value = await http.get('/customer/solutions') } finally { loading.value = false } }
async function open(s: any) { detail.value = await http.get(`/customer/solutions/${s.id}`) }
async function confirm(s: any) {
  if (!window.confirm(`确认方案「${s.name}」？确认后进入报价谈价。`)) return
  await http.post(`/customer/solutions/${s.id}/confirm`); toast('方案已确认', 'success'); load()
}
onMounted(load)
</script>
<style scoped>
.solution-row { padding: 13px 0; border-bottom: 1px solid #f2f4f7; }
.solution-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel); }
.solution-row h3 { font-size: 14px; margin: 7px 0 3px; }
.solution-row p { color: var(--steel); font-size: 10px; margin: 0 0 9px; }
.actions { display: flex; gap: 8px; align-items: center; }
.rich-content { line-height: 1.85; font-size: 13px; color: #394150; }
.rich-content h2 { font-size: 16px; margin: 14px 0 6px; }
.rich-content h3 { font-size: 14px; margin: 12px 0 5px; }
.rich-content img { max-width: 100%; border-radius: 8px; }
</style>
