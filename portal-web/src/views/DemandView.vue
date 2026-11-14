<template>
  <div>
    <section class="sub-hero">
      <div class="container hero-inner">
        <div>
          <span class="kicker">PROCUREMENT REQUIREMENT</span>
          <h1>提交采购需求</h1>
          <p>2 小时内响应，工程师根据锅炉类型、燃料、蒸发量、压力与用途匹配选型建议，并附合规材料清单。</p>
          <div class="hero-trust"><span>✓ 免费选型</span><span>✓ 需求编号可追踪</span><span>✓ 资质公开核验</span></div>
        </div>
        <div class="hero-side">
          <strong>400-800-6688</strong>
          <span>选型顾问热线 · 8:00-20:00</span>
        </div>
      </div>
    </section>
    <div class="container page-pad">
      <div v-if="success" class="success-card">
        <div class="success-icon">✓</div>
        <h2>需求提交成功</h2>
        <p>工作人员将在 2 小时内联系您；请保存需求编号与查询凭证。</p>
        <div class="success-grid">
          <div><span>需求编号</span><strong class="mono">{{ success.demandNo }}</strong></div>
          <div><span>客户编号</span><strong class="mono">{{ success.customerNo }}</strong></div>
        </div>
        <div class="success-actions">
          <router-link :to="`/track?demandNo=${success.demandNo}&token=${success.trackToken}`" class="btn btn-primary">查询进度</router-link>
          <router-link to="/products" class="btn btn-ghost">继续浏览产品</router-link>
          <button class="btn btn-ghost" @click="resetForm">再提交一条需求</button>
        </div>
        <div class="notice green">查询链接已生成，可复制后在微信打开。请不要重复提交相同需求。</div>
      </div>
      <template v-else>
        <div class="form-layout">
          <div class="form-card">
            <div class="form-head">
              <div><h2>采购需求信息</h2><p>带 * 为必填项；提交后自动生成需求编号并进入待认领池等待分配。</p></div>
              <span class="step">01 / 需求登记</span>
            </div>
            <div class="form-grid">
              <div class="field full">
                <label>企业名称 *</label>
                <input v-model="form.customerName" placeholder="请输入采购单位全称，用于一企一档与查重" />
              </div>
              <div class="field"><label>联系人 *</label><input v-model="form.contactName" placeholder="姓名" /></div>
              <div class="field"><label>联系电话 *</label><input v-model="form.phone" placeholder="手机号，仅用于业务联系" /></div>
              <div class="field"><label>锅炉类型 *</label><select v-model="form.boilerType"><option v-for="o in boilerTypes" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
              <div class="field"><label>燃料类型 *</label><select v-model="form.fuelType"><option v-for="o in fuelTypes" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
              <div class="field"><label>额定蒸发量 / 热功率 *</label><select v-model="form.capacity"><option v-for="o in capacities" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
              <div class="field"><label>压力等级 *</label><select v-model="form.pressureLevel"><option v-for="o in pressures" :key="o.value" :value="o.value">{{ o.label }}</option></select></div>
              <div class="field"><label>预算下限（元）</label><input v-model.number="form.budgetMin" type="number" placeholder="如 400000" /></div>
              <div class="field"><label>预算上限（元）</label><input v-model.number="form.budgetMax" type="number" placeholder="如 550000" /></div>
              <div class="field"><label>期望地区 *</label><input v-model="form.region" placeholder="省 / 市 / 区" /></div>
              <div class="field"><label>期望交付时间</label><input v-model="form.expectTime" type="date" /></div>
              <div class="field full"><label>用途说明</label><input v-model="form.usageDesc" placeholder="如：食品杀菌蒸汽、化工工艺蒸汽、供暖热水" /></div>
              <div class="field full"><label>补充说明</label><textarea v-model="form.description" rows="4" placeholder="现场条件、海拔、排放要求、现有锅炉情况等" /></div>
            </div>
            <label class="consent"><input v-model="agreed" type="checkbox" /> 我已阅读并同意<router-link to="/agreement" target="_blank">《用户协议》与《隐私政策》</router-link>，同意将信息用于选型咨询与业务联系。</label>
            <div class="form-actions">
              <button class="btn btn-primary" :disabled="submitting" @click="submit()">{{ submitting ? '提交中…' : '提交采购需求' }}</button>
              <router-link to="/consult" class="btn btn-ghost">先咨询工程师</router-link>
            </div>
          </div>
          <aside class="aside">
            <div class="aside-card dark">
              <span class="kicker">WHY US</span>
              <h3>需求提交后会发生什么？</h3>
              <div class="timeline">
                <div><span>01</span><p><strong>需求编号生成</strong>进入待认领池，负责人认领或主管分配。</p></div>
                <div><span>02</span><p><strong>2 小时首响</strong>电话/微信确认参数与现场条件。</p></div>
                <div><span>03</span><p><strong>方案与报价</strong>工程师推送选型方案，客户在线确认后进入报价。</p></div>
                <div><span>04</span><p><strong>合同与履约</strong>报价确认后生成合同草稿，发货安装验收全节点可查。</p></div>
              </div>
            </div>
            <div class="aside-card">
              <h3>提交前可先自查</h3>
              <ul class="check-list">
                <li>蒸汽/热水实际用量与峰值</li>
                <li>可用燃料与供应价格</li>
                <li>安装场地尺寸与承重</li>
                <li>当地排放与年检要求</li>
                <li>预算区间与付款方式</li>
              </ul>
              <router-link to="/verify" class="text-link">核验锅炉厂家资质 →</router-link>
            </div>
          </aside>
        </div>
      </template>
    </div>

    <div v-if="duplicate" class="modal-mask" @click.self="duplicate = null">
      <div class="modal-card">
        <h3>检测到近期相似需求</h3>
        <p>{{ duplicate }}</p>
        <div class="modal-actions">
          <router-link to="/track" class="btn btn-ghost">查询已有需求</router-link>
          <button class="btn btn-primary" @click="forceSubmit">仍要提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const route = useRoute(); const store = sessionStore()
const form = reactive<any>({ boilerType: 'steam', fuelType: 'gas', capacity: '6 t/h', pressureLevel: 'low', source: 'H5' })
const boilerTypes = [{ label: '蒸汽锅炉', value: 'steam' }, { label: '热水锅炉', value: 'hotwater' }, { label: '导热油炉', value: 'thermal' }, { label: '余热锅炉', value: 'wasteheat' }]
const fuelTypes = [{ label: '燃气', value: 'gas' }, { label: '燃油', value: 'oil' }, { label: '电', value: 'electric' }, { label: '生物质', value: 'biomass' }, { label: '燃煤', value: 'coal' }]
const capacities = [
  { label: '2 t/h', value: '2 t/h' }, { label: '4 t/h', value: '4 t/h' }, { label: '6 t/h', value: '6 t/h' },
  { label: '10 t/h', value: '10 t/h' }, { label: '20 t/h', value: '20 t/h' }, { label: '1.4 MW', value: '1.4 MW' }, { label: '2.8 MW', value: '2.8 MW' }
]
const pressures = [{ label: '常压', value: 'atmospheric' }, { label: '低压', value: 'low' }, { label: '中压', value: 'medium' }, { label: '高压', value: 'high' }]
const agreed = ref(true); const submitting = ref(false); const success = ref<any>(null); const duplicate = ref<string | null>(null)
async function submit(force = false) {
  if (!agreed.value) return toast('请先阅读并同意用户协议与隐私政策', 'error')
  if (!form.customerName || !form.phone || !form.region) return toast('企业名称、联系电话、期望地区为必填项', 'error')
  submitting.value = true
  try {
    const data: any = await http.post('/public/demands', { ...form, force }, {
      headers: { 'Idempotency-Key': crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) }
    })
    success.value = data
    duplicate.value = null
    toast('需求已提交', 'success')
  } catch (e: any) {
    if (e?.response?.status === 409) {
      duplicate.value = e.response.data?.message || '检测到近期相似需求，请勿重复提交'
    }
  } finally { submitting.value = false }
}
function forceSubmit() { submit(true) }
function resetForm() {
  success.value = null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { boilerType: 'steam', fuelType: 'gas', capacity: '6 t/h', pressureLevel: 'low', source: 'H5' })
}
onMounted(async () => {
  document.title = '提交采购需求｜鑫火锅炉'
  if (store.customer) {
    form.customerName = store.customer.name
    form.contactName = store.user?.name || store.customer.name
  }
  const productId = route.query.product
  if (productId) {
    try {
      const data: any = await http.get(`/public/products/${productId}`)
      const p = data.product
      Object.assign(form, { boilerType: p.boilerType, fuelType: p.fuelType, capacity: p.capacity, pressureLevel: p.pressureLevel, usageDesc: p.usageDesc, description: `关注型号：${p.model}` })
    } catch {}
  }
})
</script>

<style scoped>
.sub-hero { background: linear-gradient(135deg, #0f172a, #1d2d45); color: #fff; padding: 56px 0 46px; }
.hero-inner { display: flex; align-items: flex-end; justify-content: space-between; gap: 30px; }
.kicker { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
.sub-hero h1 { font-size: 38px; margin: 12px 0 10px; letter-spacing: -1px; }
.sub-hero p { color: #a6b4c7; max-width: 620px; font-size: 14px; line-height: 1.9; margin: 0; }
.hero-trust { display: flex; gap: 18px; margin-top: 18px; font-size: 11px; color: #c2cddd; flex-wrap: wrap; }
.hero-side { text-align: right; }
.hero-side strong { display: block; font-size: 26px; letter-spacing: .5px; color: #fff; }
.hero-side span { font-size: 10px; color: #8fa0b8; }
.form-layout { display: grid; grid-template-columns: 1.7fr .85fr; gap: 28px; align-items: start; }
.form-card { background: #fff; border: 1px solid var(--line); padding: 32px; }
.form-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 24px; }
.form-head h2 { margin: 0 0 4px; font-size: 22px; }
.form-head p { margin: 0; color: var(--steel); font-size: 12px; }
.step { font-size: 10px; color: var(--steel-2); letter-spacing: 1px; white-space: nowrap; }
.consent { display: flex; gap: 8px; align-items: flex-start; margin-top: 20px; font-size: 11px; color: var(--steel); line-height: 1.7; }
.consent a { color: var(--accent); }
.consent input { margin-top: 3px; }
.aside-card { background: #fff; border: 1px solid var(--line); padding: 24px; margin-bottom: 16px; }
.aside-card.dark { background: #101b2e; color: #fff; border-color: #101b2e; }
.aside-card h3 { font-size: 18px; margin: 8px 0 16px; }
.timeline { display: flex; flex-direction: column; gap: 16px; }
.timeline div { display: flex; gap: 10px; }
.timeline span { font-size: 10px; color: var(--accent); border: 1px solid rgba(226,138,99,.4); width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; flex: 0 0 24px; }
.timeline p { margin: 0; font-size: 11px; color: #98a7bc; line-height: 1.7; }
.timeline strong { display: block; color: #e5ebf4; margin-bottom: 2px; font-size: 12px; }
.check-list { padding-left: 18px; color: var(--steel); font-size: 12px; line-height: 2; }
.success-card { max-width: 760px; margin: 0 auto; background: #fff; border: 1px solid var(--line); padding: 44px; text-align: center; }
.success-icon { width: 56px; height: 56px; border-radius: 50%; background: #e9f5ee; color: var(--green); display: flex; align-items: center; justify-content: center; font-size: 26px; margin: 0 auto 16px; }
.success-card h2 { margin: 0 0 8px; }
.success-card p { color: var(--steel); font-size: 13px; }
.success-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 24px 0; text-align: left; }
.success-grid div { border: 1px solid var(--line); padding: 14px; }
.success-grid span { display: block; font-size: 10px; color: var(--steel-2); }
.success-grid strong { display: block; margin-top: 4px; font-size: 15px; }
.success-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.modal-mask { position: fixed; inset: 0; background: rgba(15,23,42,.55); z-index: 300; display: flex; align-items: center; justify-content: center; padding: 20px; }
.modal-card { background: #fff; max-width: 480px; width: 100%; padding: 30px; border-top: 4px solid var(--accent); }
.modal-card h3 { margin: 0 0 10px; }
.modal-card p { color: var(--steel); font-size: 13px; line-height: 1.8; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
@media (max-width: 900px) { .form-layout { grid-template-columns: 1fr; } .hero-inner { flex-direction: column; align-items: flex-start; } .hero-side { text-align: left; } .sub-hero h1 { font-size: 30px; } .form-card { padding: 22px; } }
</style>
