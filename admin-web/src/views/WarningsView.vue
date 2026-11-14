<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">预警中心</h1><p class="page-sub">资质证书、维保/定检、回款逾期、需求跟进超时统一红黄灯预警；每日自动扫描。</p></div>
      <el-button type="primary" :loading="scanning" @click="scan"><el-icon><Refresh /></el-icon> 立即扫描</el-button>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-label">预警总数</div><div class="stat-value danger">{{ data.total || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">资质证书</div><div class="stat-value">{{ data.certificates?.length || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">维保/年检</div><div class="stat-value">{{ data.maintenance?.length || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">回款逾期</div><div class="stat-value">{{ data.payments?.length || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">需求超时</div><div class="stat-value">{{ data.demandFollows?.length || 0 }}</div></div>
    </div>
    <div class="card" style="padding:0">
      <el-tabs v-model="tab">
        <el-tab-pane label="资质证书" name="cert">
          <el-table :data="data.certificates || []" stripe size="small">
            <el-table-column prop="certificate.certType" label="类型" width="130"><template #default="{ row }">{{ statusLabel(row.certificate.certType) }}</template></el-table-column>
            <el-table-column prop="certificate.certNo" label="证书编号" width="220" />
            <el-table-column prop="certificate.issuer" label="发证机关" min-width="200" />
            <el-table-column label="到期日" width="120"><template #default="{ row }">{{ fmtDate(row.certificate.validTo) }}</template></el-table-column>
            <el-table-column label="剩余" width="100"><template #default="{ row }"><span :class="['pill', row.level === 'RED' ? 'red' : 'yellow']">{{ row.remainDays < 0 ? '已过期' : row.remainDays + '天' }}</span></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="维保/定检" name="maintain">
          <el-table :data="data.maintenance || []" stripe size="small">
            <el-table-column prop="plan.equipmentNo" label="设备" width="190" />
            <el-table-column prop="plan.project" label="项目" width="100"><template #default="{ row }">{{ statusLabel(row.plan.project) }}</template></el-table-column>
            <el-table-column prop="plan.customerName" label="客户" min-width="190" />
            <el-table-column label="到期" width="120"><template #default="{ row }">{{ fmtDate(row.plan.nextDate) }}</template></el-table-column>
            <el-table-column label="剩余" width="100"><template #default="{ row }"><span :class="['pill', row.level === 'RED' ? 'red' : 'yellow']">{{ row.remainDays < 0 ? '已逾期' : row.remainDays + '天' }}</span></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="回款逾期" name="payment">
          <el-table :data="data.payments || []" stripe size="small">
            <el-table-column prop="contract.contractNo" label="合同" width="200" />
            <el-table-column prop="contract.customerName" label="客户" min-width="190" />
            <el-table-column prop="plan.phase" label="期次" width="110" />
            <el-table-column label="应收" width="130"><template #default="{ row }">{{ fmtMoney(row.plan.amount) }}</template></el-table-column>
            <el-table-column prop="overdueDays" label="逾期天数" width="100" />
            <el-table-column label="等级" width="90"><template #default="{ row }"><span :class="['pill', row.level === 'RED' ? 'red' : 'yellow']">{{ row.level === 'RED' ? '红色' : '黄色' }}</span></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="需求跟进" name="demand">
          <el-table :data="data.demandFollows || []" stripe size="small">
            <el-table-column prop="demand.demandNo" label="需求编号" width="200" />
            <el-table-column prop="demand.contactName" label="联系人" width="100" />
            <el-table-column prop="demand.region" label="地区" width="140" />
            <el-table-column label="阶段" width="90"><template #default="{ row }"><span class="pill">{{ statusLabel(row.demand.status) }}</span></template></el-table-column>
            <el-table-column prop="overdueDays" label="超时天数" width="100" />
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/demands/${row.demand.id}`)">跟进</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { fmtDate, fmtMoney, statusLabel } from '@/utils/format'

const router = useRouter()
const data = ref<any>({}); const tab = ref('cert'); const scanning = ref(false)
async function load() { data.value = await http.get('/admin/warnings') }
async function scan() {
  scanning.value = true
  try {
    const r: any = await http.post('/admin/warnings/scan')
    ElMessage.success(`扫描完成：证书过期 ${r.expiredCertificates}，报价过期 ${r.expiredQuotes}，回款逾期 ${r.overduePayments}`)
    load()
  } finally { scanning.value = false }
}
onMounted(load)
</script>
