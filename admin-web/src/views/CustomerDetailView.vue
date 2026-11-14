<template>
  <div class="page">
    <div class="page-head">
      <div>
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回客户列表</el-button>
        <h1 class="page-title" style="margin-top:8px">{{ customer?.name || '客户关联视图' }}</h1>
        <p class="page-sub">
          <span class="mono">{{ customer?.customerNo }}</span>
          <span style="margin:0 8px">·</span>{{ customer?.industry || '未分类' }}
          <span style="margin:0 8px">·</span>{{ customer?.region || '—' }}
          <span :class="['pill', statusPill(customer?.status)]" style="margin-left:10px">{{ statusLabel(customer?.status) }}</span>
        </p>
      </div>
      <div style="display:flex;gap:8px">
        <el-button v-if="!readOnly" @click="openContact"><el-icon><Plus /></el-icon> 新增联系人</el-button>
        <el-button v-if="!readOnly" type="danger" plain @click="removeCustomer">删除</el-button>
      </div>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="data">
      <div class="stat-grid">
        <div class="stat-card"><div class="stat-label">成交金额</div><div class="stat-value money">{{ fmtMoney(data.dealAmount) }}</div></div>
        <div class="stat-card"><div class="stat-label">需求</div><div class="stat-value">{{ data.demands.length }}</div><div class="stat-note">方案 {{ data.solutions.length }} · 报价 {{ data.quotes.length }}</div></div>
        <div class="stat-card"><div class="stat-label">合同</div><div class="stat-value">{{ data.contracts.length }}</div><div class="stat-note">回款记录 {{ data.payments.length }}</div></div>
        <div class="stat-card"><div class="stat-label">售后工单</div><div class="stat-value">{{ data.tickets.length }}</div><div class="stat-note">设备 {{ data.equipment.length }} 台</div></div>
      </div>

      <div class="card" style="padding:0">
        <el-tabs v-model="tab">
          <el-tab-pane label="需求与咨询" name="demand">
            <el-table :data="data.demands" stripe size="small">
              <el-table-column prop="demandNo" label="需求编号" width="190" />
              <el-table-column label="选型" min-width="200"><template #default="{ row }">{{ statusLabel(row.boilerType) }} / {{ row.capacity }} / {{ statusLabel(row.fuelType) }}</template></el-table-column>
              <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column prop="region" label="地区" width="130" />
              <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/demands/${row.id}`)">详情</el-button></template></el-table-column>
            </el-table>
            <div class="sub-title">咨询记录</div>
            <el-table :data="data.consultations" size="small" stripe>
              <el-table-column prop="channel" label="渠道" width="80" />
              <el-table-column prop="question" label="问题" min-width="240" show-overflow-tooltip />
              <el-table-column prop="answer" label="答复" min-width="240" show-overflow-tooltip />
              <el-table-column label="状态" width="80"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="方案与报价" name="quote">
            <el-table :data="data.solutions" size="small" stripe>
              <el-table-column prop="solutionNo" label="方案编号" width="200" />
              <el-table-column prop="name" label="方案名称" min-width="240" />
              <el-table-column prop="version" label="版本" width="70" />
              <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
            </el-table>
            <div class="sub-title">报价记录</div>
            <el-table :data="data.quotes" size="small" stripe>
              <el-table-column prop="quoteNo" label="报价单号" width="190" />
              <el-table-column prop="productName" label="产品" min-width="150" />
              <el-table-column label="总额" width="130"><template #default="{ row }"><span class="money">{{ fmtMoney(row.totalAmount) }}</span></template></el-table-column>
              <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/quotes/${row.id}`)">详情</el-button></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="合同与回款" name="contract">
            <el-table :data="data.contracts" size="small" stripe>
              <el-table-column prop="contractNo" label="合同编号" width="200" />
              <el-table-column prop="title" label="合同名称" min-width="240" />
              <el-table-column label="金额" width="130"><template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template></el-table-column>
              <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/contracts/${row.id}`)">详情</el-button></template></el-table-column>
            </el-table>
            <div class="sub-title">回款计划</div>
            <el-table :data="data.payments" size="small" stripe>
              <el-table-column prop="phase" label="期次" width="100" />
              <el-table-column label="应收" width="130"><template #default="{ row }">{{ fmtMoney(row.amount) }}</template></el-table-column>
              <el-table-column label="实收" width="130"><template #default="{ row }">{{ fmtMoney(row.receivedAmount) }}</template></el-table-column>
              <el-table-column prop="planDate" label="计划日期" width="110"><template #default="{ row }">{{ fmtDate(row.planDate) }}</template></el-table-column>
              <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="售后与设备" name="service">
            <el-table :data="data.tickets" size="small" stripe>
              <el-table-column prop="ticketNo" label="工单号" width="190" />
              <el-table-column prop="description" label="故障描述" min-width="240" />
              <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column prop="technicianName" label="技师" width="90" />
            </el-table>
            <div class="sub-title">在役设备台账</div>
            <el-table :data="data.equipment" size="small" stripe>
              <el-table-column prop="equipmentNo" label="设备编号" width="190" />
              <el-table-column prop="model" label="型号" width="150" />
              <el-table-column prop="capacity" label="容量" width="90" />
              <el-table-column prop="installAddr" label="安装地址" min-width="200" />
              <el-table-column label="质保到期" width="110"><template #default="{ row }">{{ fmtDate(row.warrantyEnd) }}</template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="联系人 / 资质 / 回访" name="other">
            <div class="grid-2">
              <div>
                <div class="sub-title">联系人</div>
                <div v-for="c in data.contacts" :key="c.id" class="contact-item">
                  <div><strong>{{ c.name }}</strong> <span class="muted">{{ c.title }}</span><span v-if="c.primaryFlag" class="pill" style="margin-left:8px">主联系人</span></div>
                  <div class="muted">{{ c.phoneMask || '—' }} · {{ c.email || '—' }}</div>
                  <el-button v-if="!readOnly" link type="danger" @click="removeContact(c)">删除</el-button>
                </div>
              </div>
              <div>
                <div class="sub-title">资质证书</div>
                <div v-for="c in data.certificates" :key="c.id" class="contact-item">
                  <div><strong>{{ statusLabel(c.certType) }}</strong> <span class="mono">{{ c.certNo }}</span></div>
                  <div class="muted">有效期至 {{ fmtDate(c.validTo) }} · {{ statusLabel(c.status) }}</div>
                </div>
                <div v-if="!data.certificates.length" class="muted">暂无客户设备维保资质</div>
              </div>
            </div>
            <div class="sub-title">回访记录</div>
            <el-table :data="data.visits" size="small" stripe>
              <el-table-column prop="planDate" label="计划日期" width="110" />
              <el-table-column prop="content" label="回访内容" min-width="260" />
              <el-table-column prop="score" label="评分" width="70" />
              <el-table-column label="状态" width="90"><template #default="{ row }"><span :class="['pill', statusPill(row.status)]">{{ statusLabel(row.status) }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <el-dialog v-model="contactVisible" title="新增联系人" width="560px">
        <el-form :model="contactForm" label-position="top" class="form-two">
          <el-form-item label="姓名" required><el-input v-model="contactForm.name" /></el-form-item>
          <el-form-item label="职务"><el-input v-model="contactForm.title" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="contactForm.phoneEnc" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="contactForm.email" /></el-form-item>
          <el-form-item label="主联系人"><el-switch v-model="contactForm.primaryFlag" :active-value="1" :inactive-value="0" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="contactVisible=false">取消</el-button><el-button type="primary" @click="saveContact">保存</el-button></template>
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
import { fmtDate, fmtMoney, statusLabel, statusPill } from '@/utils/format'

const route = useRoute(); const router = useRouter(); const store = authStore()
const readOnly = computed(() => false)
const id = Number(route.params.id)
const loading = ref(false); const data = ref<any>(null); const customer = computed(() => data.value?.customer)
const tab = ref('demand')
const contactVisible = ref(false)
const contactForm = reactive<any>({ name: '', title: '', phoneEnc: '', email: '', primaryFlag: 0, customerId: id })

async function load() {
  loading.value = true
  try { data.value = await http.get(`/admin/customers/${id}/aggregate`) } finally { loading.value = false }
}
function openContact() { Object.keys(contactForm).forEach((k) => delete contactForm[k]); Object.assign(contactForm, { customerId: id, name: '', title: '', phoneEnc: '', email: '', primaryFlag: 0 }); contactVisible.value = true }
async function saveContact() {
  if (!contactForm.name) return ElMessage.warning('联系人姓名必填')
  await http.post(`/admin/customers/${id}/contacts`, contactForm)
  ElMessage.success('联系人已保存'); contactVisible.value = false; load()
}
async function removeContact(c: any) {
  await ElMessageBox.confirm(`确认删除联系人 ${c.name}？`, '操作确认', { type: 'warning' })
  await http.delete(`/admin/customers/${id}/contacts/${c.id}`); ElMessage.success('已删除'); load()
}
async function removeCustomer() {
  await ElMessageBox.confirm(
    `确认删除客户档案「${customer.value?.name}」？将从数据库物理删除该客户、登录账号与联系人，后续可使用相同信息重新注册。已关联业务数据的客户不可删除，请先处理业务或改为禁用。`,
    '删除客户',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await http.delete(`/admin/customers/${id}`)
  ElMessage.success('客户档案已删除')
  router.replace('/customers')
}
onMounted(load)
</script>

<style scoped>
.sub-title { font-weight: 600; margin: 18px 16px 10px; color: #354052; }
:deep(.el-tabs__header) { padding: 0 16px; margin-bottom: 6px; }
:deep(.el-tabs__content) { padding: 0 16px 16px; }
.contact-item { border: 1px solid var(--xh-line); border-radius: 6px; padding: 10px 12px; margin-bottom: 8px; display: grid; grid-template-columns: 1fr auto; gap: 6px; }
</style>
