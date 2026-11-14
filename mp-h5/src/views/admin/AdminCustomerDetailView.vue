<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>客户详情</h1><button class="link-btn danger" @click="removeCustomer">删除</button></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
    <div v-else-if="data" class="page-body">
      <div class="card">
        <div class="row-top"><strong style="font-size:18px">{{ data.customer.name }}</strong><span :class="['pill', statusPill(data.customer.status)]">{{ label(data.customer.status) }}</span></div>
        <div class="row-mid">{{ data.customer.industry || '未填行业' }} · {{ data.customer.scale || '—' }} · {{ data.customer.region || '未填地区' }}</div>
        <div class="row-bottom"><span class="mono">{{ data.customer.customerNo }}</span><span>{{ data.customer.ownerName || '待认领' }}</span></div>
        <div class="info-grid">
          <div><span>详细地址</span><strong>{{ data.customer.address || '—' }}</strong></div>
          <div><span>客户来源</span><strong>{{ data.customer.source || '—' }}</strong></div>
          <div><span>客户分类</span><strong>{{ data.customer.category || '—' }}</strong></div>
          <div><span>级别</span><strong>{{ data.customer.level || '—' }}</strong></div>
        </div>
      </div>

      <div class="grid-4" style="margin-top:12px">
        <div class="stat-card"><span>需求</span><strong>{{ data.demands?.length || 0 }}</strong><em>方案 {{ data.solutions?.length || 0 }}</em></div>
        <div class="stat-card"><span>报价</span><strong>{{ data.quotes?.length || 0 }}</strong><em>合同 {{ data.contracts?.length || 0 }}</em></div>
        <div class="stat-card"><span>回款</span><strong>{{ data.payments?.length || 0 }}</strong><em>成交 {{ money(data.dealAmount) }}</em></div>
        <div class="stat-card"><span>售后</span><strong>{{ data.tickets?.length || 0 }}</strong><em>设备 {{ data.equipment?.length || 0 }}</em></div>
      </div>

      <div class="card" style="margin-top:12px">
        <div class="card-title"><h2>联系人</h2><button class="link-btn" @click="openContact">＋ 新增</button></div>
        <div v-for="c in data.contacts || []" :key="c.id" class="list-row">
          <div class="main"><strong>{{ c.name }} {{ c.primaryFlag ? '· 主联系人' : '' }}</strong><span>{{ c.title || '联系人' }} · {{ c.phoneMask || '—' }}</span></div>
          <button class="link-btn danger" @click="removeContact(c)">删除</button>
        </div>
      </div>

      <div class="section-label">需求线索</div>
      <div class="card" style="padding:4px 16px">
        <router-link v-for="d in data.demands || []" :key="d.id" :to="`/admin/demands/${d.id}`" class="list-row">
          <div class="main"><strong class="mono">{{ d.demandNo }}</strong><span>{{ label(d.boilerType) }} · {{ d.capacity }} · {{ label(d.status) }}</span></div><em>›</em>
        </router-link>
        <div v-if="!(data.demands || []).length" class="empty" style="padding:20px">暂无需求</div>
      </div>

      <div class="section-label">报价与合同</div>
      <div class="card" style="padding:4px 16px">
        <router-link v-for="q in data.quotes || []" :key="q.id" :to="`/admin/quotes/${q.id}`" class="list-row">
          <div class="main"><strong class="mono">{{ q.quoteNo }}</strong><span>{{ money(q.totalAmount) }} · {{ label(q.status) }}</span></div><em>›</em>
        </router-link>
        <router-link v-for="c in data.contracts || []" :key="c.id" :to="`/admin/contracts/${c.id}`" class="list-row">
          <div class="main"><strong class="mono">{{ c.contractNo }}</strong><span>{{ c.title }} · {{ label(c.status) }}</span></div><em>›</em>
        </router-link>
        <div v-if="!(data.quotes || []).length && !(data.contracts || []).length" class="empty" style="padding:20px">暂无报价或合同</div>
      </div>

      <div class="section-label">售后与设备</div>
      <div class="card" style="padding:4px 16px">
        <router-link v-for="t in data.tickets || []" :key="t.id" :to="`/admin/tickets/${t.id}`" class="list-row">
          <div class="main"><strong class="mono">{{ t.ticketNo }}</strong><span>{{ label(t.type) }} · {{ label(t.status) }}</span></div><em>›</em>
        </router-link>
        <div v-for="e in data.equipment || []" :key="e.id" class="list-row">
          <div class="main"><strong>{{ e.equipmentNo }}</strong><span>{{ e.model }} · {{ label(e.status) }}</span></div>
        </div>
        <div v-if="!(data.tickets || []).length && !(data.equipment || []).length" class="empty" style="padding:20px">暂无工单或设备</div>
      </div>
    </div>

    <div v-if="contactVisible" class="modal-mask" @click.self="contactVisible = false">
      <div class="modal-card">
        <div class="modal-head"><h3>新增联系人</h3><button class="modal-close" @click="contactVisible = false">×</button></div>
        <div class="form-grid">
          <div class="field full"><label>姓名 *</label><input v-model="contactForm.name" /></div>
          <div class="field"><label>职务</label><input v-model="contactForm.title" /></div>
          <div class="field"><label>电话</label><input v-model="contactForm.phoneEnc" /></div>
          <div class="field full"><label>邮箱</label><input v-model="contactForm.email" /></div>
          <div class="field full"><label><input v-model.number="contactForm.primaryFlag" type="checkbox" style="width:auto;margin-right:6px" />设为主联系人</label></div>
        </div>
        <div class="form-actions"><button class="btn btn-ghost" @click="contactVisible=false">取消</button><button class="btn btn-primary" style="flex:1" @click="saveContact">保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { label, money, statusPill } from '@/utils'
import { toast } from '@/ui/toast'

const route = useRoute(); const router = useRouter(); const id = Number(route.params.id)
const data = ref<any>(null); const loading = ref(false); const contactVisible = ref(false)
const contactForm = reactive<any>({ name: '', title: '', phoneEnc: '', email: '', primaryFlag: 0, customerId: id })
async function load() {
  loading.value = true
  try { data.value = await http.get(`/admin/customers/${id}/aggregate`) } finally { loading.value = false }
}
function openContact() { Object.assign(contactForm, { name: '', title: '', phoneEnc: '', email: '', primaryFlag: 0 }); contactVisible.value = true }
async function saveContact() {
  if (!contactForm.name) return toast('请输入联系人姓名', 'error')
  await http.post(`/admin/customers/${id}/contacts`, contactForm)
  toast('联系人已保存', 'success'); contactVisible.value = false; load()
}
async function removeContact(c: any) {
  if (!confirm(`确认删除联系人 ${c.name}？`)) return
  await http.delete(`/admin/customers/${id}/contacts/${c.id}`); toast('已删除', 'success'); load()
}
async function removeCustomer() {
  if (!confirm(`确认删除客户「${data.value?.customer?.name}」？删除后不可恢复。`)) return
  try { await http.delete(`/admin/customers/${id}`); toast('客户已删除', 'success'); router.replace('/admin/customers') } catch {}
}
onMounted(load)
</script>

<style scoped>
.row-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.row-mid { font-size: 11px; color: #344054; margin-top: 5px; }
.row-bottom { display: flex; justify-content: space-between; font-size: 10px; color: var(--steel-2); margin-top: 4px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; margin-top: 12px; }
.info-grid div { background: #fff; padding: 9px 10px; }
.info-grid span { display: block; font-size: 9px; color: var(--steel-2); }
.info-grid strong { font-size: 11px; }
.list-row em { font-style: normal; color: var(--steel-2); }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.link-btn.danger { color: #b33426; }
</style>
