<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>提交采购需求</h1></div>
    <div class="page-body">
      <div v-if="success" class="card success">
        <div class="success-icon">✓</div>
        <h2>需求已提交</h2>
        <p>工作人员将在 2 小时内联系您。</p>
        <div class="mono demand-no">{{ success.demandNo }}</div>
        <button class="btn btn-primary btn-block" @click="router.replace('/demands')">查看我的需求</button>
        <button class="btn btn-ghost btn-block" style="margin-top:8px" @click="reset">再提交一条</button>
      </div>
      <template v-else>
        <div class="card">
          <div class="form-grid">
            <div class="field full"><label>企业名称 *</label><input v-model="form.customerName" placeholder="采购单位全称" /></div>
            <div class="field"><label>联系人 *</label><input v-model="form.contactName" placeholder="姓名" /></div>
            <div class="field"><label>联系电话 *</label><input v-model="form.phone" placeholder="手机号" /></div>
            <div class="field"><label>锅炉类型</label><select v-model="form.boilerType"><option v-for="o in boilerTypes" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
            <div class="field"><label>燃料</label><select v-model="form.fuelType"><option v-for="o in fuelTypes" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
            <div class="field"><label>蒸发量/热功率</label><select v-model="form.capacity"><option v-for="o in capacities" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
            <div class="field"><label>压力等级</label><select v-model="form.pressureLevel"><option v-for="o in pressures" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
            <div class="field"><label>预算下限</label><input v-model.number="form.budgetMin" type="number" placeholder="元" /></div>
            <div class="field"><label>预算上限</label><input v-model.number="form.budgetMax" type="number" placeholder="元" /></div>
            <div class="field"><label>期望地区 *</label><input v-model="form.region" placeholder="省/市" /></div>
            <div class="field"><label>期望时间</label><input v-model="form.expectTime" type="date" /></div>
            <div class="field full"><label>用途说明</label><input v-model="form.usageDesc" placeholder="食品蒸汽/化工工艺/供暖" /></div>
            <div class="field full"><label>补充说明</label><textarea v-model="form.description" placeholder="现场条件、排放要求等" /></div>
          </div>
          <div class="notice blue" style="margin-top:14px">提交后将自动生成需求编号；如存在近期相似需求会提示确认，避免撞单。</div>
          <button class="btn btn-primary btn-block" style="margin-top:14px" :disabled="loading" @click="submit()">{{ loading ? '提交中…' : '提交需求' }}</button>
        </div>
      </template>
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
const form = reactive<any>({ boilerType: 'steam', fuelType: 'gas', capacity: '6 t/h', pressureLevel: 'low', source: 'MP' })
const loading = ref(false); const success = ref<any>(null)
const boilerTypes = [{ label: '蒸汽锅炉', value: 'steam' }, { label: '热水锅炉', value: 'hotwater' }, { label: '导热油炉', value: 'thermal' }]
const fuelTypes = [{ label: '燃气', value: 'gas' }, { label: '燃油', value: 'oil' }, { label: '电', value: 'electric' }, { label: '生物质', value: 'biomass' }, { label: '燃煤', value: 'coal' }]
const capacities = ['2 t/h', '4 t/h', '6 t/h', '10 t/h', '20 t/h'].map((v) => ({ label: v, value: v }))
const pressures = [{ label: '常压', value: 'atmospheric' }, { label: '低压', value: 'low' }, { label: '中压', value: 'medium' }, { label: '高压', value: 'high' }]
async function submit(force = false) {
  if (!form.customerName || !form.phone || !form.region) return toast('企业名称、电话、地区必填', 'error')
  loading.value = true
  try {
    success.value = await http.post('/public/demands', { ...form, force })
    toast('需求已提交', 'success')
  } catch (e: any) {
    if (e?.response?.status === 409) {
      if (window.confirm((e.response.data?.message || '存在近期相似需求') + '\n仍要提交吗？')) submit(true)
    }
  } finally { loading.value = false }
}
function reset() { success.value = null; Object.assign(form, { boilerType: 'steam', fuelType: 'gas', capacity: '6 t/h', pressureLevel: 'low', source: 'MP' }) }
onMounted(() => { form.customerName = store.customer?.name || ''; form.contactName = store.user?.name || store.customer?.name || '' })
</script>
<style scoped>
.success { text-align: center; padding: 28px 18px; }
.success-icon { width: 50px; height: 50px; border-radius: 50%; background: #e9f5ee; color: var(--green); font-size: 24px; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.success h2 { margin: 0 0 4px; }
.success p { color: var(--steel); font-size: 12px; }
.demand-no { background: #f3f5f8; padding: 10px; border-radius: 8px; margin: 14px 0; font-weight: 700; font-size: 14px; }
</style>
