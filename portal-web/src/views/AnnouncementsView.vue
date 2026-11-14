<template>
  <div class="container page-pad">
    <div class="list-head">
      <div><span class="kicker">NEWS &amp; NOTICES</span><h1>资讯公告</h1><p>系统与企业的资质公示、产品动态、维保提醒与行业资讯。</p></div>
      <router-link to="/products" class="btn btn-ghost btn-sm">浏览产品 →</router-link>
    </div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="list.length" class="news-grid">
      <router-link v-for="a in list" :key="a.id" :to="`/announcements/${a.id}`" class="news-card">
        <div class="news-top">
          <span class="source">{{ a.source === 'SYSTEM' ? '系统公告' : '企业公告' }}</span>
          <span class="date">{{ date(a.publishTime) }}</span>
        </div>
        <h2>{{ a.title }}</h2>
        <p>{{ excerpt(a.content) }}</p>
        <span class="read">阅读全文 →</span>
      </router-link>
    </div>
    <div v-else class="empty"><h3>暂无公告</h3><p>平台与企业公告将在此发布。</p></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { date } from '@/utils'

const store = sessionStore(); const list = ref<any[]>([]); const loading = ref(false)
function excerpt(html?: string) { return (html || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').slice(0, 110) + '…' }
onMounted(async () => {
  loading.value = true
  try { list.value = await http.get('/public/announcements') } finally { loading.value = false }
})
</script>

<style scoped>
.list-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 30px; }
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
.list-head h1 { font-size: 36px; margin: 10px 0 6px; letter-spacing: -1px; }
.list-head p { color: var(--steel); font-size: 13px; margin: 0; }
.news-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.news-card { background: #fff; border: 1px solid var(--line); padding: 26px; transition: all .16s ease; }
.news-card:hover { border-color: var(--accent); transform: translateY(-2px); }
.news-top { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); }
.source { color: var(--accent); border: 1px solid #f0cfc3; padding: 2px 7px; }
.news-card h2 { font-size: 20px; margin: 16px 0 8px; line-height: 1.45; }
.news-card p { color: var(--steel); font-size: 12px; line-height: 1.9; margin: 0 0 18px; min-height: 66px; }
.read { font-size: 11px; color: var(--accent); font-weight: 600; }
@media (max-width: 800px) { .news-grid { grid-template-columns: 1fr; } .list-head h1 { font-size: 30px; } }
</style>
