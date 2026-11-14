<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>用户管理</h1><button class="btn btn-primary btn-sm" @click="openCreate">＋ 新增</button></div>
    <div class="page-body">
      <div class="search-row"><input v-model="keyword" class="search-input" placeholder="姓名 / 用户名 / 电话" @keyup.enter="search" /><button class="btn btn-ghost btn-sm" @click="search">搜索</button></div>
      <div class="filter-chips">
        <button :class="{ active: roleCode === '' }" @click="roleCode = ''; search()">全部</button>
        <button v-for="r in roles" :key="r.code" :class="{ active: roleCode === r.code }" @click="roleCode = r.code; search()">{{ r.name }}</button>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="rows.length" class="card" style="padding:4px 16px;margin-top:12px">
        <div v-for="u in rows" :key="u.id" class="user-row">
          <div class="row-top"><strong>{{ u.name }}</strong><span :class="['pill', statusPill(u.status)]">{{ label(u.status) }}</span></div>
          <div class="row-mid"><span class="mono">{{ u.username }}</span> · {{ roleName(u.roleCode) }}</div>
          <div class="row-bottom"><span>{{ u.phone || '—' }} · {{ u.email || '—' }}</span><span>{{ u.customerId ? `客户 #${u.customerId}` : '—' }}</span></div>
          <div class="row-actions">
            <button class="link-btn" @click="openEdit(u)">编辑</button>
            <button class="link-btn" @click="resetPassword(u)">重置密码</button>
            <button class="link-btn danger" @click="remove(u)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty"><div class="big">户</div>暂无用户</div>
      <div v-if="total > size" class="pager-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
      </div>
    </div>

    <div v-if="dialog" class="modal-mask" @click.self="dialog=false">
      <div class="modal-card">
        <div class="modal-head"><h3>{{ editing ? '编辑用户' : '新增用户' }}</h3><button class="modal-close" @click="dialog=false">×</button></div>
        <div class="form-grid">
          <div class="field"><label>登录账号 *</label><input v-model="form.username" :disabled="!!editing" /></div>
          <div class="field"><label>姓名 *</label><input v-model="form.name" /></div>
          <div class="field"><label>电话</label><input v-model="form.phone" /></div>
          <div class="field"><label>邮箱</label><input v-model="form.email" /></div>
          <div class="field full"><label>角色 *</label><select v-model="form.roleCode"><option v-for="r in roles" :key="r.code" :value="r.code">{{ r.name }}</option></select></div>
          <div v-if="form.roleCode === 'CUSTOMER'" class="field full"><label>绑定客户档案 *</label><select v-model.number="form.customerId"><option value="">请选择客户</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}（{{ c.customerNo }}）</option></select></div>
          <div class="field full"><label>登录密码</label><input v-model="form.password" type="password" :placeholder="editing ? '留空则不修改' : '不填默认 123456'" /></div>
          <div class="field full"><label>状态</label><select v-model="form.status"><option value="NORMAL">正常</option><option value="DISABLED">停用</option></select></div>
          <div class="field full"><label>备注</label><textarea v-model="form.remark" rows="2" /></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="dialog=false">取消</button><button class="btn btn-primary" style="flex:1" @click="save">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { label, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const router = useRouter()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(20)
const keyword = ref(''); const roleCode = ref(''); const loading = ref(false)
const roles = ref<any[]>([]); const customers = ref<any[]>([]); const dialog = ref(false); const editing = ref<any>(null)
const form = reactive<any>({ roleCode: 'ADMIN', status: 'NORMAL' })
function roleName(code: string) { return roles.value.find((r) => r.code === code)?.name || code }
async function load() {
  loading.value = true
  try { const d: any = await http.get('/admin/users', { params: { page: page.value, size: size.value, keyword: keyword.value, roleCode: roleCode.value } }); rows.value = d.records || []; total.value = d.total || 0 } finally { loading.value = false }
}
async function loadOptions() {
  try { roles.value = await http.get('/admin/users/roles') } catch {}
  try { customers.value = await http.get('/admin/customers/options') } catch {}
}
function search() { page.value = 1; load() }
function openCreate() { editing.value = null; Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { roleCode: 'ADMIN', status: 'NORMAL' }); dialog.value = true }
function openEdit(row: any) { editing.value = row; Object.assign(form, row); form.password = ''; dialog.value = true }
async function save() {
  if (!form.username || !form.name || !form.roleCode) return toast('账号、姓名与角色必填', 'error')
  if (form.roleCode === 'CUSTOMER' && !form.customerId) return toast('客户账号必须绑定客户档案', 'error')
  if (editing.value) await http.put(`/admin/users/${editing.value.id}`, form)
  else await http.post('/admin/users', form)
  toast('用户已保存', 'success'); dialog.value = false; load()
}
async function resetPassword(row: any) {
  const password = prompt(`为 ${row.name} 设置新密码`, '123456')
  if (!password) return
  await http.post(`/admin/users/${row.id}/reset-password`, { password }); toast('密码已重置', 'success')
}
async function remove(row: any) { if (!confirm(`确认删除用户 ${row.username}？`)) return; try { await http.delete(`/admin/users/${row.id}`); toast('已删除', 'success'); load() } catch {} }
onMounted(() => { load(); loadOptions() })
</script>

<style scoped>
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; font-size: 12px; background: #fff; outline: none; }
.filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 10px 0 0; }
.filter-chips button { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 5px 11px; font-size: 11px; white-space: nowrap; color: var(--steel); }
.filter-chips button.active { background: var(--ink); color: #fff; border-color: var(--ink); }
.user-row { border-bottom: 1px solid #f2f4f7; padding: 10px 0; }
.user-row:last-child { border-bottom: none; }
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-top strong { font-size: 13px; }
.row-mid { font-size: 10px; color: var(--steel); margin-top: 4px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 3px; }
.row-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 4px; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
.pager-row { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 16px 0; font-size: 11px; color: var(--steel); }
</style>
