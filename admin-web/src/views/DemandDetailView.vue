<template>
  <div class="page">
    <div class="page-head">
      <div>
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回需求列表</el-button>
        <h1 class="page-title" style="margin-top:8px">需求 {{ demand?.demandNo }}</h1>
        <p class="page-sub">
          <span :class="['pill', statusPill(demand?.status)]">{{ statusLabel(demand?.status) }}</span>
          <span style="margin-left:10px">{{ customer?.name }}</span>
          <span class="muted" style="margin-left:8px">{{ demand?.contactName }} · {{ demand?.contactPhoneMask }}</span>
          <span class="muted" style="margin-left:8px">负责人：{{ demand?.ownerName || '待认领' }}</span>
        </p>
      </div>
      <div class="toolbar" style="margin:0">
        <el-button v-if="!readOnly" @click="openEdit"><el-icon><Edit /></el-icon> 编辑需求</el-button>
        <el-button v-if="canAssign" @click="assignVisible=true"><el-icon><UserFilled /></el-icon> 分配/转派</el-button>
        <el-button v-if="!readOnly" type="primary" @click="openSolution"><el-icon><DocumentAdd /></el-icon> 创建方案</el-button>
        <el-button v-if="!readOnly" type="success" @click="openQuote"><el-icon><Money /></el-icon> 创建报价</el-button>
        <el-button v-if="!readOnly" type="danger" plain @click="removeDemand"><el-icon><Delete /></el-icon> 删除</el-button>
      </div>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="demand">
      <div class="section-grid">
        <div class="card">
          <h3 class="card-title">需求要素</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="锅炉类型">{{ statusLabel(demand.boilerType) }}</el-descriptions-item>
            <el-descriptions-item label="燃料类型">{{ statusLabel(demand.fuelType) }}</el-descriptions-item>
            <el-descriptions-item label="蒸发量/热功率">{{ demand.capacity || '—' }}</el-descriptions-item>
            <el-descriptions-item label="压力等级">{{ statusLabel(demand.pressureLevel) }}</el-descriptions-item>
            <el-descriptions-item label="预算区间">{{ fmtMoney(demand.budgetMin) }} ~ {{ fmtMoney(demand.budgetMax) }}</el-descriptions-item>
            <el-descriptions-item label="期望地区">{{ demand.region || '—' }}</el-descriptions-item>
            <el-descriptions-item label="期望时间">{{ fmtDate(demand.expectTime) }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ demand.source }}</el-descriptions-item>
            <el-descriptions-item label="下次跟进">{{ fmtDateTime(demand.followNextTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后跟进">{{ fmtDateTime(demand.lastFollowTime) }}</el-descriptions-item>
            <el-descriptions-item label="用途说明" :span="2">{{ demand.usageDesc || '—' }}</el-descriptions-item>
            <el-descriptions-item label="补充说明" :span="2">{{ demand.description || '—' }}</el-descriptions-item>
          </el-descriptions>
          <h3 class="card-title" style="margin-top:22px">阶段推进</h3>
          <div class="stage-line">
            <button v-for="s in stageButtons" :key="s.value" class="stage-btn" :disabled="readOnly" @click="changeStatus(s.value)">
              <span>{{ s.label }}</span><i>→</i>
            </button>
          </div>
          <div v-if="!stageButtons.length" class="muted">当前阶段不可推进，可放弃或查看历史。</div>
        </div>

        <div class="card">
          <h3 class="card-title">下一步动作</h3>
          <div class="notice">
            {{ nextAction }}
          </div>
          <h3 class="card-title" style="margin-top:18px">快速选型推荐</h3>
          <div v-if="recommendations.length" class="kanban-list">
            <div v-for="item in recommendations" :key="item.product.id" class="kanban-item">
              <div class="row">
                <div>
                  <div class="title">{{ item.product.model }}</div>
                  <div class="meta">{{ statusLabel(item.product.boilerType) }} · {{ item.product.capacity }} · {{ statusLabel(item.product.fuelType) }}</div>
                </div>
                <span class="pill">{{ item.score }} 分</span>
              </div>
              <div style="margin-top:8px;display:flex;gap:8px">
                <el-button size="small" @click="useProduct(item.product, 'solution')">用于方案</el-button>
                <el-button size="small" type="primary" plain @click="useProduct(item.product, 'quote')">用于报价</el-button>
              </div>
            </div>
          </div>
          <div v-else class="muted">暂无匹配产品，请检查产品库或人工选择。</div>
        </div>
      </div>

      <div class="card">
        <el-tabs v-model="tab">
          <el-tab-pane label="跟进记录" name="follow">
            <div class="follow-form" v-if="!readOnly">
              <el-form :model="followForm" label-position="top" class="form-two">
                <el-form-item label="跟进方式">
                  <el-select v-model="followForm.method" style="width:100%">
                    <el-option label="电话" value="PHONE" /><el-option label="微信" value="WECHAT" />
                    <el-option label="到店/到厂" value="VISIT" /><el-option label="在线" value="ONLINE" />
                  </el-select>
                </el-form-item>
                <el-form-item label="下次跟进时间">
                  <el-date-picker v-model="followForm.nextTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
                </el-form-item>
                <el-form-item label="跟进内容" class="full"><el-input v-model="followForm.content" type="textarea" :rows="3" /></el-form-item>
              </el-form>
              <div style="text-align:right"><el-button type="primary" @click="addFollow">保存跟进</el-button></div>
            </div>
            <el-timeline style="margin-top:16px;padding-left:8px">
              <el-timeline-item v-for="f in data.followRecords" :key="f.id" :timestamp="fmtDateTime(f.followTime)" placement="top">
                <div class="kanban-item">
                  <div class="row"><strong>{{ f.userName || '系统' }} · {{ f.method }}</strong></div>
                  <div style="margin-top:6px">{{ f.content }}</div>
                  <div v-if="f.nextTime" class="meta">下次跟进：{{ fmtDateTime(f.nextTime) }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="!data.followRecords.length" description="暂无跟进记录" />
          </el-tab-pane>
          <el-tab-pane label="方案" name="solution">
            <el-table :data="data.solutions" stripe size="small">
              <el-table-column prop="solutionNo" label="方案编号" width="200" />
              <el-table-column prop="name" label="方案名称" min-width="260" />
              <el-table-column prop="version" label="版本" width="70" />
              <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column label="查看" width="70"><template #default="{ row }">{{ row.viewCount }}</template></el-table-column>
              <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="router.push('/solutions')">管理</el-button></template></el-table-column>
            </el-table>
            <el-empty v-if="!data.solutions.length" description="尚未创建方案">
              <el-button v-if="!readOnly" type="primary" @click="openSolution">创建方案</el-button>
            </el-empty>
          </el-tab-pane>
          <el-tab-pane label="报价" name="quote">
            <el-table :data="data.quotes" stripe size="small">
              <el-table-column prop="quoteNo" label="报价单号" width="190" />
              <el-table-column prop="productName" label="产品" min-width="150" />
              <el-table-column prop="version" label="版本" width="70" />
              <el-table-column label="含税总价" width="130"><template #default="{ row }"><span class="money">{{ fmtMoney(row.totalAmount) }}</span></template></el-table-column>
              <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column label="有效期" width="110"><template #default="{ row }">{{ fmtDate(row.validUntil) }}</template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/quotes/${row.id}`)">详情</el-button></template></el-table-column>
            </el-table>
            <el-empty v-if="!data.quotes.length" description="尚未创建报价">
              <el-button v-if="!readOnly" type="primary" @click="openQuote">创建报价</el-button>
            </el-empty>
          </el-tab-pane>
        </el-tabs>
      </div>

      <el-dialog v-model="editVisible" title="编辑需求" width="820px" top="5vh">
        <el-form :model="editForm" label-position="top" class="form-two">
          <el-form-item label="锅炉类型"><el-select v-model="editForm.boilerType" style="width:100%"><el-option v-for="o in boilerTypeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
          <el-form-item label="燃料类型"><el-select v-model="editForm.fuelType" style="width:100%"><el-option v-for="o in fuelTypeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
          <el-form-item label="蒸发量"><el-input v-model="editForm.capacity" /></el-form-item>
          <el-form-item label="压力等级"><el-select v-model="editForm.pressureLevel" style="width:100%"><el-option v-for="o in pressureOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
          <el-form-item label="预算下限"><el-input-number v-model="editForm.budgetMin" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="预算上限"><el-input-number v-model="editForm.budgetMax" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="地区"><el-input v-model="editForm.region" /></el-form-item>
          <el-form-item label="期望时间"><el-date-picker v-model="editForm.expectTime" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="用途说明" class="full"><el-input v-model="editForm.usageDesc" /></el-form-item>
          <el-form-item label="补充说明" class="full"><el-input v-model="editForm.description" type="textarea" :rows="3" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" @click="saveEdit">保存</el-button></template>
      </el-dialog>

      <el-dialog v-model="assignVisible" title="分配 / 转派需求" width="520px">
        <el-form label-position="top">
          <el-form-item label="负责人">
            <el-select v-model="assignForm.ownerId" filterable style="width:100%">
              <el-option v-for="u in owners" :key="u.id" :label="`${u.name}（${u.roleCode}）`" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="分配说明"><el-input v-model="assignForm.remark" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="assignVisible=false">取消</el-button><el-button type="primary" @click="assign">确认</el-button></template>
      </el-dialog>

      <el-dialog v-model="solutionVisible" title="创建锅炉方案" width="880px" top="5vh" destroy-on-close>
        <el-form :model="solutionForm" label-position="top" class="form-two">
          <el-form-item label="方案名称" class="full" required><el-input v-model="solutionForm.name" /></el-form-item>
          <el-form-item label="选型产品" class="full">
            <el-select v-model="solutionForm.productId" filterable style="width:100%" placeholder="请选择产品库型号">
              <el-option v-for="p in products" :key="p.id" :label="`${p.model} · ${p.capacity} · ${statusLabel(p.fuelType)}`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="方案正文" class="full">
            <RichTextEditor v-model="solutionForm.content" height="300px" />
          </el-form-item>
        </el-form>
        <template #footer><el-button @click="solutionVisible=false">取消</el-button><el-button type="primary" @click="saveSolution">保存方案</el-button></template>
      </el-dialog>

      <el-dialog v-model="quoteVisible" title="创建报价单" width="880px" top="4vh" destroy-on-close>
        <el-form :model="quoteForm" label-position="top" class="form-two">
          <el-form-item label="产品型号" required>
            <el-select v-model="quoteForm.productId" filterable style="width:100%" @change="onProduct">
              <el-option v-for="p in products" :key="p.id" :label="`${p.model} · ${p.capacity}`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="单价（元）"><el-input-number v-model="quoteForm.unitPrice" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="数量"><el-input-number v-model="quoteForm.quantity" :min="1" style="width:100%" /></el-form-item>
          <el-form-item label="税率"><el-input-number v-model="quoteForm.taxRate" :min="0" :max="1" :step="0.01" style="width:100%" /></el-form-item>
          <el-form-item label="运费（元）"><el-input-number v-model="quoteForm.freight" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="安装调试费（元）"><el-input-number v-model="quoteForm.installFee" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="底价红线（元）"><el-input-number v-model="quoteForm.floorPrice" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="有效期至"><el-date-picker v-model="quoteForm.validUntil" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="付款条件" class="full"><el-input v-model="quoteForm.paymentTerms" placeholder="预付30%，发货前40%，验收后30%" /></el-form-item>
          <el-form-item label="质保期"><el-input v-model="quoteForm.warranty" /></el-form-item>
          <el-form-item label="报价合计"><strong class="money" style="line-height:32px">{{ fmtMoney(quoteTotal) }}</strong></el-form-item>
        </el-form>
        <el-alert v-if="quoteTotal < quoteForm.floorPrice" type="warning" :closable="false" show-icon
          title="当前报价低于底价红线，提交后将自动进入审批流，审批通过前不可发送。" />
        <template #footer><el-button @click="quoteVisible=false">取消</el-button><el-button type="primary" @click="saveQuote">保存报价</el-button></template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'
import { boilerTypeOptions, capacityOptions, fuelTypeOptions, pressureOptions } from '@/crud/options'
import { fmtDate, fmtDateTime, fmtMoney, statusLabel, statusPill } from '@/utils/format'
import RichTextEditor from '@/components/RichTextEditor.vue'

const route = useRoute(); const router = useRouter(); const store = authStore()
const id = Number(route.params.id)
const loading = ref(false); const data = ref<any>(null); const tab = ref('follow')
const recommendations = ref<any[]>([]); const products = ref<any[]>([]); const owners = ref<any[]>([])
const followForm = reactive<any>({ method: 'PHONE', content: '', nextTime: '' })
const editVisible = ref(false); const editForm = reactive<any>({})
const assignVisible = ref(false); const assignForm = reactive<any>({})
const solutionVisible = ref(false); const solutionForm = reactive<any>({})
const quoteVisible = ref(false); const quoteForm = reactive<any>({ quantity: 1, taxRate: 0.13, freight: 0, installFee: 0, floorPrice: 0 })
const demand = computed(() => data.value?.demand)
const customer = computed(() => data.value?.customer)
const readOnly = computed(() => false)
const canAssign = computed(() => store.isAdmin || store.isAdmin)
const transitions: Record<string, { label: string; value: string }[]> = {
  NEW: [{ label: '推进入咨询', value: 'CONSULTING' }, { label: '标记流失', value: 'LOST' }],
  CONSULTING: [{ label: '推进入介绍', value: 'INTRODUCING' }, { label: '标记流失', value: 'LOST' }],
  INTRODUCING: [{ label: '推进入谈价', value: 'TALKING' }, { label: '标记流失', value: 'LOST' }],
  TALKING: [{ label: '确认成交', value: 'DEAL' }, { label: '标记流失', value: 'LOST' }],
  DEAL: [], LOST: [{ label: '重新激活', value: 'NEW' }]
}
const stageButtons = computed(() => transitions[demand.value?.status] || [])
const nextAction = computed(() => {
  const map: Record<string, string> = {
    NEW: '尽快电话联系客户确认需求，并在 2 小时内完成首次跟进。',
    CONSULTING: '结合选型推荐生成方案，向客户介绍锅炉参数、案例与合规资质。',
    INTRODUCING: '方案确认后进入报价谈价，注意底价红线与审批流。',
    TALKING: '跟进客户还价，逐轮留痕；确认报价后自动生成合同草稿。',
    DEAL: '跟进合同回款、生产发货、安装验收，验收后自动转售后。',
    LOST: '记录流失原因；如客户需求变化可重新激活。'
  }
  return map[demand.value?.status] || '—'
})
const quoteTotal = computed(() => Number(quoteForm.unitPrice || 0) * Number(quoteForm.quantity || 1) + Number(quoteForm.freight || 0) + Number(quoteForm.installFee || 0))

async function load() {
  loading.value = true
  try {
    data.value = await http.get(`/admin/demands/${id}`)
    await Promise.all([loadRecommend(), loadProducts(), loadOwners()])
  } finally { loading.value = false }
}
async function loadRecommend() {
  try { recommendations.value = await http.get('/admin/demands/recommend', { params: { demandId: id } }) } catch { recommendations.value = [] }
}
async function loadProducts() {
  try { const d: any = await http.get('/admin/products', { params: { size: 200, online_status: 'ONLINE' } }); products.value = d.records } catch { products.value = [] }
}
async function loadOwners() {
  if (!canAssign.value) return
  try { const d: any = await http.get('/admin/users', { params: { size: 200 } }); owners.value = d.records.filter((u: any) => u.roleCode !== 'CUSTOMER') } catch { owners.value = [] }
}
async function addFollow() {
  if (!followForm.content) return ElMessage.warning('请填写跟进内容')
  await http.post(`/admin/demands/${id}/follow`, followForm)
  ElMessage.success('跟进已保存'); followForm.content = ''; load()
}
async function changeStatus(target: string) {
  let remark = ''
  if (target === 'LOST' || target === 'DEAL') {
    const result = await ElMessageBox.prompt('请输入本次流转说明', target === 'DEAL' ? '确认成交' : '标记流失', { inputPlaceholder: '说明将写入跟进记录与审计日志' })
    remark = result.value || ''
  }
  await http.post(`/admin/demands/${id}/status`, { status: target, remark })
  ElMessage.success('阶段已更新'); load()
}
function openEdit() {
  Object.keys(editForm).forEach((k) => delete editForm[k])
  Object.assign(editForm, demand.value, { status: undefined, ownerId: undefined, ownerName: undefined, followNextTime: undefined })
  editVisible.value = true
}
async function saveEdit() {
  await http.put(`/admin/demands/${id}`, editForm)
  ElMessage.success('需求已更新'); editVisible.value = false; load()
}
async function assign() {
  if (!assignForm.ownerId) return ElMessage.warning('请选择负责人')
  await http.post(`/admin/demands/${id}/assign`, assignForm)
  ElMessage.success('已分配'); assignVisible.value = false; load()
}
function openSolution() {
  Object.keys(solutionForm).forEach((k) => delete solutionForm[k])
  Object.assign(solutionForm, { demandId: id, name: `${customer.value?.name || ''}锅炉选型方案`, productId: recommendations.value[0]?.product?.id || undefined, content: '<h2>项目概述</h2><p>根据客户需求配置锅炉设备，详见选型参数。</p>' })
  solutionVisible.value = true
}
async function saveSolution() {
  if (!solutionForm.name) return ElMessage.warning('请填写方案名称')
  await http.post('/admin/solutions', solutionForm)
  ElMessage.success('方案已创建'); solutionVisible.value = false; load()
}
function openQuote() {
  Object.keys(quoteForm).forEach((k) => delete quoteForm[k])
  Object.assign(quoteForm, { demandId: id, customerId: demand.value.customerId, quantity: 1, taxRate: 0.13, freight: 0, installFee: 0, floorPrice: 0, paymentTerms: '预付30%，发货前40%，验收后30%', warranty: '12个月', validUntil: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10) })
  const first = recommendations.value[0]?.product
  if (first) onProduct(first.id)
  quoteVisible.value = true
}
function onProduct(productId: number) {
  const p = products.value.find((x) => x.id === productId)
  if (p) {
    quoteForm.productId = p.id
    quoteForm.unitPrice = Number(p.guidePrice || 0)
    quoteForm.floorPrice = Math.round(Number(p.guidePrice || 0) * 0.88)
    quoteForm.productName = p.model
  }
}
async function saveQuote() {
  if (!quoteForm.productId || !quoteForm.unitPrice) return ElMessage.warning('请选择产品并填写单价')
  const q = { ...quoteForm, productName: products.value.find((p) => p.id === quoteForm.productId)?.model }
  await http.post('/admin/quotes', q)
  ElMessage.success('报价单已创建，请在报价列表中发送或提交审批'); quoteVisible.value = false; load()
}
function useProduct(product: any, target: 'solution' | 'quote') {
  if (target === 'solution') { openSolution(); solutionForm.productId = product.id; solutionForm.name = `${product.model} 选型方案` }
  else { openQuote(); onProduct(product.id) }
}
async function removeDemand() {
  await ElMessageBox.confirm(
    `确认删除需求「${demand.value?.demandNo}」？将物理删除该需求及跟进记录。已关联方案或报价的需求不可删除，请先删除下游记录。`,
    '删除需求',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/demands/${id}`)
  ElMessage.success('需求已删除')
  router.replace('/demands')
}
onMounted(load)
</script>

<style scoped>
.stage-line { display: flex; flex-wrap: wrap; gap: 8px; }
.stage-btn { border: 1px solid #bcd3f7; background: #f4f8ff; color: var(--xh-primary); border-radius: 6px; padding: 8px 12px; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; }
.stage-btn:hover { background: #eaf2ff; }
.stage-btn:disabled { opacity: .5; cursor: not-allowed; }
.stage-btn i { font-style: normal; color: #8ba6c9; }
.follow-form { border: 1px dashed var(--xh-line); border-radius: 6px; padding: 14px 14px 4px; margin-bottom: 16px; }
:deep(.el-tabs__header) { margin-bottom: 10px; }
</style>
