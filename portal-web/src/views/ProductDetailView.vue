<template>
  <div class="container page-pad">
    <div class="crumb"><router-link to="/">首页</router-link><span>/</span><router-link to="/products">产品中心</router-link><span>/</span><em>{{ product?.model }}</em></div>
    <div v-if="loading" class="loading"><div class="spinner" />加载产品中…</div>
    <div v-else-if="!product" class="empty"><h3>产品不存在或已下架</h3><router-link to="/products" class="btn btn-primary">返回产品中心</router-link></div>
    <template v-else>
      <div class="detail-grid">
        <div class="gallery">
          <div class="main-visual">
            <img v-if="images.length" :src="images[activeImage]" :alt="product.model" />
            <div v-else class="visual-fallback">
              <svg viewBox="0 0 460 300">
                <ellipse cx="230" cy="258" rx="170" ry="18" fill="rgba(15,23,42,.12)" />
                <rect x="88" y="92" width="284" height="146" rx="24" fill="#334155" stroke="#1e293b" stroke-width="2" />
                <rect x="102" y="104" width="256" height="22" rx="11" fill="rgba(255,255,255,.14)" />
                <circle cx="158" cy="180" r="31" fill="#0f172a" stroke="#e2e8f0" stroke-width="3" />
                <path d="M158 180 L172 162" stroke="#d9633b" stroke-width="2.5" stroke-linecap="round" />
                <text x="118" y="221" fill="#e2e8f0" font-size="9" font-family="sans-serif">PRESSURE</text>
                <rect x="212" y="152" width="120" height="6" rx="3" fill="rgba(255,255,255,.22)" />
                <rect x="212" y="170" width="88" height="6" rx="3" fill="rgba(255,255,255,.16)" />
                <rect x="212" y="188" width="100" height="6" rx="3" fill="rgba(255,255,255,.12)" />
                <path d="M372 118 H430 V214" fill="none" stroke="#94a3b8" stroke-width="12" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
          </div>
          <div v-if="images.length > 1" class="thumbs">
            <button v-for="(img, i) in images" :key="img" :class="{ active: i === activeImage }" @click="activeImage = i"><img :src="img" alt="" /></button>
          </div>
          <div class="gallery-note">产品图片为实拍/示意，最终以合同技术附件为准</div>
        </div>
        <div class="info">
          <div class="tag-row">
            <span class="pill blue">{{ label(product.boilerType) }}</span>
            <span class="pill">{{ label(product.fuelType) }}</span>
            <span class="pill green">资质：{{ product.complianceStatus === 'OK' ? '齐全' : '待补' }}</span>
          </div>
          <h1>{{ product.model }}</h1>
          <p class="usage">{{ product.usageDesc || '工业蒸汽 / 供热应用' }}</p>
          <div class="price-box">
            <span>参考指导价</span>
            <strong>{{ money(product.guidePrice) }}</strong>
            <em>实际价格以选型配置、运费与安装条件为准</em>
          </div>
          <div class="key-specs">
            <div><span>额定蒸发量/热功率</span><strong>{{ product.capacity || '—' }}</strong></div>
            <div><span>压力等级</span><strong>{{ label(product.pressureLevel) }}</strong></div>
            <div><span>燃料</span><strong>{{ label(product.fuelType) }}</strong></div>
            <div><span>产品编码</span><strong class="mono">{{ product.code }}</strong></div>
          </div>
          <div class="actions">
            <router-link :to="`/demand?product=${product.id}`" class="btn btn-primary">提交采购需求</router-link>
            <router-link :to="`/consult?product=${product.id}`" class="btn btn-ghost">预约工程师咨询</router-link>
          </div>
          <div class="assurance">
            <span>✓ 制造许可</span><span>✓ 产品监检</span><span>✓ 能效报告</span><span>✓ 持证安装</span>
          </div>
        </div>
      </div>

      <div class="detail-bottom">
        <div class="detail-main">
          <section class="detail-card">
            <h2>技术参数</h2>
            <div class="params-grid">
              <div v-for="(v, k) in techParams" :key="k" class="param"><span>{{ k }}</span><strong>{{ v }}</strong></div>
              <div v-if="!Object.keys(techParams).length" class="param"><span>参数</span><strong>{{ product.techParams || '详见技术附件' }}</strong></div>
            </div>
          </section>
          <section class="detail-card">
            <h2>合规资质</h2>
            <p class="section-note">证书编号已脱敏展示；采购方可在资质核验页输入完整编号与关键字验证真伪、状态与有效期。</p>
            <div class="cert-list">
              <div v-for="c in certificates" :key="c.certNo" class="cert-row">
                <div class="cert-icon">证</div>
                <div><strong>{{ label(c.certType) }}</strong><span class="mono">{{ c.certNo }}</span><span class="muted">{{ c.issuer }}</span></div>
                <div class="cert-right"><span :class="['pill', c.status === 'VALID' ? 'green' : c.status === 'WARNING' ? 'yellow' : 'red']">{{ label(c.status) }}</span><em>有效至 {{ date(c.validTo) }}</em></div>
              </div>
              <div v-if="!certificates.length" class="muted">产品资质正在归档，请联系顾问获取。</div>
            </div>
            <router-link to="/verify" class="text-link" style="margin-top:14px">前往资质公开核验 →</router-link>
          </section>
        </div>
        <aside class="detail-aside">
          <div class="aside-card dark">
            <span class="eyebrow">SELECTION HELP</span>
            <h3>需要选型计算？</h3>
            <p>提供蒸汽用量、燃料、海拔与排放要求，工程师输出运行成本对比与合规清单。</p>
            <router-link :to="`/demand?product=${product.id}`" class="btn btn-primary btn-block">免费获取选型方案</router-link>
          </div>
          <div class="aside-card">
            <h3>相似型号</h3>
            <router-link v-for="p in similar" :key="p.id" :to="`/products/${p.id}`" class="similar-row">
              <div><strong>{{ p.model }}</strong><span>{{ label(p.fuelType) }} · {{ p.capacity }}</span></div>
              <span>→</span>
            </router-link>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { label, money, date, imageList } from '@/utils'

const route = useRoute(); const store = sessionStore()
const loading = ref(false); const data = ref<any>(null); const activeImage = ref(0)
const product = computed(() => data.value?.product)
const certificates = computed(() => data.value?.certificates || [])
const similar = computed(() => data.value?.similar || [])
const images = computed(() => imageList(product.value?.images))
const techParams = computed(() => {
  const raw = product.value?.techParams
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw)
    return typeof parsed === 'object' ? parsed : { 技术参数: raw }
  } catch {
    return { 技术参数: raw }
  }
})
onMounted(async () => {
  loading.value = true
  try {
    data.value = await http.get(`/public/products/${route.params.id}`)
    document.title = `${product.value?.model || '锅炉产品'}｜鑫火锅炉`
  } finally { loading.value = false }
})
</script>

<style scoped>
.crumb { display: flex; gap: 8px; font-size: 11px; color: var(--steel-2); margin-bottom: 26px; flex-wrap: wrap; }
.crumb a:hover { color: var(--accent); }
.crumb em { font-style: normal; color: var(--ink); }
.detail-grid { display: grid; grid-template-columns: 1.05fr .95fr; gap: 46px; align-items: start; }
.main-visual { background: #eef1f4; border: 1px solid var(--line); min-height: 420px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.main-visual img { width: 100%; height: 420px; object-fit: cover; }
.visual-fallback { width: 90%; }
.visual-fallback svg { width: 100%; height: auto; filter: drop-shadow(0 22px 24px rgba(15,23,42,.15)); }
.thumbs { display: flex; gap: 8px; margin-top: 10px; }
.thumbs button { width: 70px; height: 54px; padding: 0; border: 1px solid var(--line); background: #fff; }
.thumbs button.active { border-color: var(--accent); }
.thumbs img { width: 100%; height: 100%; object-fit: cover; }
.gallery-note { color: var(--steel-2); font-size: 10px; margin-top: 8px; }
.info h1 { font-size: 34px; margin: 14px 0 8px; letter-spacing: -1px; }
.usage { color: var(--steel); font-size: 14px; margin-bottom: 22px; }
.price-box { background: #fff; border: 1px solid var(--line); padding: 18px 20px; margin-bottom: 22px; }
.price-box span { display: block; font-size: 10px; color: var(--steel-2); letter-spacing: .6px; }
.price-box strong { display: block; font-size: 30px; margin: 2px 0 4px; letter-spacing: -1px; }
.price-box em { font-style: normal; font-size: 10px; color: var(--steel-2); }
.key-specs { border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); padding: 8px 0; margin-bottom: 24px; }
.key-specs div { display: flex; justify-content: space-between; padding: 7px 0; font-size: 12px; }
.key-specs span { color: var(--steel); }
.key-specs strong { font-weight: 600; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.assurance { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 18px; font-size: 10px; color: var(--steel); }
.assurance span { border: 1px solid var(--line); padding: 3px 8px; }
.detail-bottom { display: grid; grid-template-columns: 1.7fr .9fr; gap: 28px; margin-top: 54px; align-items: start; }
.detail-card { background: #fff; border: 1px solid var(--line); padding: 28px; margin-bottom: 20px; }
.detail-card h2 { font-size: 20px; margin: 0 0 18px; }
.section-note { color: var(--steel); font-size: 12px; margin: -8px 0 16px; }
.params-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); border: 1px solid var(--line); }
.param { background: #fff; padding: 12px 14px; display: flex; justify-content: space-between; gap: 16px; font-size: 12px; }
.param span { color: var(--steel); }
.param strong { text-align: right; }
.cert-list { display: flex; flex-direction: column; gap: 8px; }
.cert-row { display: flex; align-items: center; gap: 12px; border: 1px solid var(--line); padding: 12px; }
.cert-icon { width: 34px; height: 34px; background: var(--accent-soft); color: var(--accent); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; flex: 0 0 34px; }
.cert-row > div:nth-child(2) { flex: 1; display: flex; flex-direction: column; gap: 2px; font-size: 12px; }
.cert-row .mono { color: var(--steel); font-size: 10px; }
.cert-right { text-align: right; display: flex; flex-direction: column; gap: 4px; align-items: flex-end; }
.cert-right em { font-style: normal; font-size: 10px; color: var(--steel-2); }
.aside-card { background: #fff; border: 1px solid var(--line); padding: 24px; margin-bottom: 16px; }
.aside-card.dark { background: #101b2e; color: #fff; border-color: #101b2e; }
.aside-card h3 { font-size: 18px; margin: 8px 0 10px; }
.aside-card p { color: #97a5b9; font-size: 12px; line-height: 1.8; margin-bottom: 18px; }
.aside-card:not(.dark) h3 { margin-top: 0; }
.eyebrow { color: #e28a63; font-size: 10px; letter-spacing: 2px; }
.similar-row { display: flex; justify-content: space-between; gap: 12px; padding: 12px 0; border-bottom: 1px dashed var(--line); font-size: 12px; }
.similar-row:last-child { border-bottom: none; }
.similar-row strong { display: block; }
.similar-row span { color: var(--steel); font-size: 10px; }
@media (max-width: 900px) {
  .detail-grid, .detail-bottom { grid-template-columns: 1fr; }
  .main-visual { min-height: 300px; }
  .main-visual img { height: 300px; }
  .params-grid { grid-template-columns: 1fr; }
  .info h1 { font-size: 27px; }
}
</style>
