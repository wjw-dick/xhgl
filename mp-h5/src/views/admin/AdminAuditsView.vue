<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>审计日志</h1><button class="link-btn" @click="exportExcel">导出</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="动作 / 对象 / 详情" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="search-row" style="margin-top:8px">
        <input v-model="username" class="search-input" placeholder="操作人" @keyup.enter="search" />
        <input v-model="module" class="search-input" placeholder="模块" @keyup.enter="search" />
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="a in rows" :key="a.id" class="audit-row">
          <div class="row-top"><strong>{{ a.module }} · {{ a.action }}</strong><span class="mono">{{ dateTime(a.createdAt) }}</span></div>
          <div class="row-mid">{{ a.username }} · {{ a.target || '—' }} · {{ a.ip || '—' }}</div>
          <div class="row-bottom">{{ a.detail || '—' }}</div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">审</div>暂无日志</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { downloadFile } from '@/api/download'
import { dateTime } from '@/utils'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const username = ref(''); const module = ref(''); const loading = ref(false)
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/audits', { params: { page: page.value, size: size.value, keyword: keyword.value, username: username.value, module: module.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function exportExcel() { downloadFile('/admin/audits/export', '审计日志.xlsx') }
onMounted(load)
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.audit-row { border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.audit-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 12px; }
.row-mid { font-size: 10px; color: var(--steel); margin-top: 4px; }
.row-bottom { font-size: 10px; color: var(--steel-2); margin-top: 3px; line-height: 1.6; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
