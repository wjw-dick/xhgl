<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>公告管理</h1><button class="btn btn-primary btn-sm" @click="dialog=true">＋ 发布</button></div>
    <div class="page-body">
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px">
        <div v-for="a in rows" :key="a.id" class="announcement-row">
          <div class="row-top"><strong>{{ a.title }}</strong><span :class="['pill', a.source === 'SYSTEM' ? 'blue' : 'green']">{{ a.source === 'SYSTEM' ? '系统' : '企业' }}</span></div>
          <p class="content" v-html="sanitizeHtml(a.content)"></p>
          <div class="row-bottom"><span>{{ dateTime(a.createdAt) }}</span><button v-if="a.source === 'COMPANY'" class="link-btn danger" @click="remove(a)">删除</button></div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">告</div>暂无公告</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>发布企业公告</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="field"><label>标题 *</label><input v-model="form.title" /></div>
        <div class="field" style="margin-top:10px"><label>公告内容</label><textarea v-model="form.content" rows="7" /></div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">发布</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { dateTime, sanitizeHtml } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const loading = ref(false); const dialog = ref(false); const form = reactive<any>({ title: '', content: '' })
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/announcements', { params: { page: page.value, size: size.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function save() {
  if (!form.title) return toast('请输入公告标题', 'error')
  await http.post('/admin/announcements', form); toast('公告已发布', 'success'); dialog.value = false; form.title = ''; form.content = ''; load()
}
async function remove(row: any) { if (!confirm('确认删除该公告？')) return; await http.delete(`/admin/announcements/${row.id}`); toast('已删除', 'success'); load() }
onMounted(load)
</script>

<style scoped>
.announcement-row { border-bottom: 1px solid #f2f4f7; padding: 12px 0; }
.announcement-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 13px; }
.content { font-size: 11px; color: var(--steel); line-height: 1.7; margin: 6px 0; max-height: 80px; overflow: hidden; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
