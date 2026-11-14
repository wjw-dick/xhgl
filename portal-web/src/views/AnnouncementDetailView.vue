<template>
  <div class="container page-pad" style="max-width: 920px">
    <div class="crumb"><router-link to="/announcements">资讯公告</router-link><span>/</span><em>公告详情</em></div>
    <div v-if="loading" class="loading"><div class="spinner" /></div>
    <article v-else-if="item" class="article">
      <span class="source">{{ item.source === 'SYSTEM' ? '系统公告' : '企业公告' }}</span>
      <h1>{{ item.title }}</h1>
      <div class="meta"><span>{{ dateTime(item.publishTime) }}</span><span>鑫火锅炉</span></div>
      <div class="rich-html" v-html="sanitizeHtml(item.content)"></div>
      <div class="article-foot">
        <router-link to="/demand" class="btn btn-primary">提交采购需求</router-link>
        <router-link to="/announcements" class="btn btn-ghost">返回公告列表</router-link>
      </div>
    </article>
    <div v-else class="empty"><h3>公告不存在</h3></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { dateTime, sanitizeHtml } from '@/utils'

const route = useRoute(); const store = sessionStore(); const item = ref<any>(null); const loading = ref(false)
onMounted(async () => {
  loading.value = true
  try {
    item.value = await http.get(`/public/announcements/${route.params.id}`)
    document.title = `${item.value.title}｜鑫火锅炉`
  } finally { loading.value = false }
})
</script>

<style scoped>
.crumb { display: flex; gap: 8px; font-size: 11px; color: var(--steel-2); margin-bottom: 26px; }
.crumb a:hover { color: var(--accent); }
.crumb em { font-style: normal; color: var(--ink); }
.article { background: #fff; border: 1px solid var(--line); padding: 44px 52px; }
.source { font-size: 10px; color: var(--accent); border: 1px solid #f0cfc3; padding: 3px 8px; }
.article h1 { font-size: 32px; line-height: 1.4; margin: 18px 0 12px; letter-spacing: -.6px; }
.meta { display: flex; gap: 18px; color: var(--steel-2); font-size: 11px; border-bottom: 1px solid var(--line); padding-bottom: 18px; margin-bottom: 24px; }
.rich-html { font-size: 15px; }
.article-foot { display: flex; gap: 10px; margin-top: 32px; padding-top: 22px; border-top: 1px solid var(--line); }
@media (max-width: 700px) { .article { padding: 26px 22px; } .article h1 { font-size: 24px; } }
</style>
