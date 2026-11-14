<template>
  <div>
    <section class="verify-hero">
      <div class="container">
        <span class="kicker">QUALIFICATION VERIFICATION</span>
        <h1>锅炉资质公开核验</h1>
        <p>输入产品/证书编号与关键字，快速核验制造许可、产品监检与能效报告真伪、状态和有效期。仅返回摘要信息，不泄露客户与合同数据。</p>
        <div class="verify-badges"><span>制造许可</span><span>产品监检</span><span>能效报告</span><span>安装许可</span><span>定检记录</span></div>
      </div>
    </section>
    <div class="container page-pad">
      <div class="verify-layout">
        <div class="verify-form card">
          <h2>核验查询</h2>
          <div class="field"><label>产品 / 证书编号 *</label><input v-model="certNo" placeholder="如 GL-HLJ-CERT-2026-0001 或产品编码 HLJ-P-STEAM-6" /></div>
          <div class="field" style="margin-top:14px"><label>核验关键字 *</label><input v-model="keyword" placeholder="至少 2 个字符，如企业名/设备型号/发证机关" @keyup.enter="verify" /></div>
          <button class="btn btn-primary btn-block" style="margin-top:18px" :disabled="loading" @click="verify">{{ loading ? '核验中…' : '立即核验' }}</button>
          <div class="verify-hint">
            演示：输入 <span class="mono clickable" @click="fillDemo">GL-HLJ-CERT-2026-0001</span>，关键字填 <span class="mono">鑫火</span>
          </div>
          <div class="notice blue" style="margin-top:16px">本页面为公开核验入口，接口按 IP 限流，防止批量撞库。锅炉特种设备最终以监管平台与证书原件为准。</div>
        </div>

        <div v-if="result" class="result-card">
          <div class="result-top" :class="result.valid ? 'valid' : 'invalid'">
            <div class="seal">{{ result.valid ? '核验通过' : '核验未通过' }}</div>
            <div>
              <span class="muted">核验结论</span>
              <h2>{{ result.valid ? '资质有效，可继续核验其他材料' : '未查询到有效资质' }}</h2>
              <p>{{ result.enterprise }} · {{ result.productModel }}</p>
            </div>
          </div>
          <div class="result-body">
            <div class="result-spec"><span>证书类型</span><strong>{{ label(result.certType) }}</strong></div>
            <div class="result-spec"><span>脱敏证书编号</span><strong class="mono">{{ result.certNo }}</strong></div>
            <div class="result-spec"><span>发证机关</span><strong>{{ result.issuer }}</strong></div>
            <div class="result-spec"><span>有效期至</span><strong>{{ date(result.validTo) }}</strong></div>
            <div class="result-spec"><span>状态</span><strong><span :class="['pill', result.status === 'VALID' ? 'green' : result.status === 'WARNING' ? 'yellow' : 'red']">{{ label(result.status) }}</span></strong></div>
            <div class="result-spec"><span>产品容量</span><strong>{{ result.productCapacity || '—' }}</strong></div>
          </div>
          <div class="result-note">{{ result.notice }}</div>
          <div class="result-actions">
            <button class="btn btn-ghost btn-sm" @click="copyResult">复制核验摘要</button>
            <router-link to="/demand" class="btn btn-primary btn-sm">向厂家提交需求</router-link>
          </div>
        </div>
        <div v-else class="verify-aside">
          <div class="card">
            <h3>采购方为什么要核验？</h3>
            <p>锅炉属特种设备，制造单位须持制造许可，产品须经监检并具备能效报告；安装须由持证单位实施。签约前核验可避免买到无证、套牌或过期资质设备。</p>
            <div class="checks">
              <div><span>01</span><p>核对制造许可级别与有效期</p></div>
              <div><span>02</span><p>核对产品监检证明与设备型号</p></div>
              <div><span>03</span><p>核对能效报告与排放指标</p></div>
              <div><span>04</span><p>核对安装许可与定检记录</p></div>
            </div>
          </div>
          <div class="card" style="margin-top:16px">
            <h3>隐私与防刷</h3>
            <p class="muted" style="font-size:12px">核验结果不含客户名称、联系人、合同金额等敏感信息；公开证书编号以脱敏形式返回，接口按 IP 每分钟限流。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import http from '@/api/http'
import { toast } from '@/ui/toast'
import { label, date } from '@/utils'

const certNo = ref(''); const keyword = ref(''); const loading = ref(false); const result = ref<any>(null)
function fillDemo() { certNo.value = 'GL-HLJ-CERT-2026-0001'; keyword.value = '鑫火' }
async function verify() {
  if (!certNo.value || keyword.value.trim().length < 2) return toast('请输入编号与至少2个字符的关键字', 'error')
  loading.value = true
  try {
    result.value = await http.get('/public/verify', { params: { certNo: certNo.value.trim(), keyword: keyword.value.trim() } })
    toast('核验完成', 'success')
  } catch { result.value = null } finally { loading.value = false }
}
async function copyResult() {
  if (!result.value) return
  const text = `鑫火锅炉资质核验：${result.value.certType} ${result.value.certNo}，状态 ${label(result.value.status)}，有效期至 ${date(result.value.validTo)}，${result.value.enterprise}`
  await navigator.clipboard.writeText(text); toast('已复制核验摘要', 'success')
}
</script>

<style scoped>
.verify-hero { background: radial-gradient(circle at 80% 20%, rgba(217,99,59,.15), transparent 30%), linear-gradient(135deg, #0f172a, #1c2c43); color: #fff; padding: 62px 0 52px; }
.kicker { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
.verify-hero h1 { font-size: 38px; margin: 12px 0 10px; letter-spacing: -1px; }
.verify-hero p { max-width: 700px; color: #a6b4c7; font-size: 13px; line-height: 1.9; margin: 0; }
.verify-badges { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 18px; }
.verify-badges span { border: 1px solid rgba(255,255,255,.14); padding: 3px 10px; font-size: 10px; color: #c2cddd; }
.verify-layout { display: grid; grid-template-columns: .9fr 1.1fr; gap: 28px; align-items: start; }
.verify-form h2 { margin: 0 0 18px; font-size: 22px; }
.verify-hint { font-size: 10px; color: var(--steel-2); margin-top: 10px; line-height: 1.8; }
.clickable { color: var(--accent); cursor: pointer; }
.result-card { background: #fff; border: 1px solid var(--line); overflow: hidden; }
.result-top { display: flex; gap: 18px; padding: 28px; align-items: center; }
.result-top.valid { background: #f1faf5; border-bottom: 1px solid #cfe3d7; }
.result-top.invalid { background: #fdf2f0; border-bottom: 1px solid #f1d0ca; }
.seal { width: 86px; height: 86px; border: 3px double var(--green); color: var(--green); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; letter-spacing: 1px; text-align: center; flex: 0 0 86px; transform: rotate(-9deg); }
.result-top.invalid .seal { border-color: #b33426; color: #b33426; }
.result-top h2 { margin: 4px 0 2px; font-size: 22px; }
.result-top p { color: var(--steel); font-size: 11px; margin: 0; }
.result-body { padding: 18px 28px; }
.result-spec { display: flex; justify-content: space-between; gap: 20px; padding: 8px 0; border-bottom: 1px dashed var(--line); font-size: 12px; }
.result-spec span { color: var(--steel); }
.result-note { background: var(--paper); padding: 12px 28px; font-size: 10px; color: var(--steel); }
.result-actions { padding: 16px 28px 24px; display: flex; gap: 10px; justify-content: flex-end; }
.verify-aside h3 { font-size: 16px; margin: 0 0 8px; }
.verify-aside p { color: var(--steel); font-size: 12px; line-height: 1.9; }
.checks { margin-top: 14px; display: flex; flex-direction: column; gap: 10px; }
.checks div { display: flex; gap: 10px; align-items: center; }
.checks span { width: 26px; height: 26px; border: 1px solid var(--line); display: flex; align-items: center; justify-content: center; font-size: 10px; color: var(--accent); flex: 0 0 26px; }
.checks p { margin: 0; font-size: 12px; }
@media (max-width: 900px) { .verify-layout { grid-template-columns: 1fr; } .verify-hero h1 { font-size: 30px; } }
</style>
