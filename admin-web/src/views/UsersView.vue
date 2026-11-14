<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">用户与角色</h1><p class="page-sub">系统用户、角色分配与用户绑定；用户数配额：{{ used }}/{{ store.company?.maxUsers || '—' }}。</p></div>
      <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增用户</el-button>
    </div>
    <div class="filter-bar">
      <div class="filter-grid">
        <el-input v-model="keyword" clearable placeholder="姓名 / 用户名 / 电话" @keyup.enter="search" />
        <el-select v-model="roleCode" clearable placeholder="角色" @change="search">
          <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
        </el-select>
        <div style="display:flex;gap:8px;justify-content:flex-end"><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></div>
      </div>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="username" label="登录账号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="130"><template #default="{ row }">{{ roleName(row.roleCode) }}</template></el-table-column>
        <el-table-column label="绑定客户" width="170"><template #default="{ row }">{{ row.customerId ? customerName(row.customerId) : '—' }}</template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link @click="resetPassword(row)">重置密码</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '新增用户'" width="680px" destroy-on-close>
      <el-form :model="form" label-position="top" class="form-two">
        <el-form-item label="登录账号" required><el-input v-model="form.username" :disabled="editing" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleCode" style="width:100%" @change="onRole">
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.roleCode === 'CUSTOMER'" label="绑定客户档案" required>
          <el-select v-model="form.customerId" filterable style="width:100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${c.customerNo}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录密码"><el-input v-model="form.password" type="password" show-password :placeholder="editing ? '留空则不修改' : '不填默认 123456'" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="正常" value="NORMAL" /><el-option label="停用" value="DISABLED" /></el-select></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import { statusLabel, statusPill } from '@/utils/format'

const store = authStore()
const rows = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(10)
const keyword = ref(''); const roleCode = ref(''); const loading = ref(false)
const roles = ref<any[]>([]); const customers = ref<any[]>([])
const dialogVisible = ref(false); const editing = ref(false); const form = reactive<any>({})
const used = computed(() => total.value)
function roleName(code: string) { return roles.value.find((r) => r.code === code)?.name || code }
function customerName(id: number) { return customers.value.find((c) => c.id === id)?.name || `客户#${id}` }
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/admin/users', { params: { page: page.value, size: size.value, keyword: keyword.value, roleCode: roleCode.value } })
    rows.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
async function loadOptions() {
  roles.value = await http.get('/admin/users/roles')
  try { customers.value = await http.get('/admin/customers/options') } catch {}
}
function search() { page.value = 1; load() }
function reset() { keyword.value = ''; roleCode.value = ''; search() }
function openCreate() { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { roleCode: 'ADMIN', status: 'NORMAL' }); editing.value = false; dialogVisible.value = true }
function openEdit(row: any) { Object.assign(form, row); editing.value = true; dialogVisible.value = true }
function onRole(code: string) { if (code !== 'CUSTOMER') form.customerId = undefined }
async function save() {
  if (!form.username || !form.name || !form.roleCode) return ElMessage.warning('账号、姓名、角色必填')
  if (editing.value) await http.put(`/admin/users/${form.id}`, form)
  else await http.post('/admin/users', form)
  ElMessage.success('用户已保存'); dialogVisible.value = false; load()
}
async function resetPassword(row: any) {
  const result = await ElMessageBox.prompt(`为 ${row.name} 设置新密码`, '重置密码', { inputValue: '123456' })
  await http.post(`/admin/users/${row.id}/reset-password`, { password: result.value })
  ElMessage.success('密码已重置'); load()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除用户 ${row.name}？`, '危险操作', { type: 'warning' })
  await http.delete(`/admin/users/${row.id}`); ElMessage.success('已删除'); load()
}
onMounted(() => { load(); loadOptions() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;padding:14px}</style>
