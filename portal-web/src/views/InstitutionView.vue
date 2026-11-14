<template>
  <div>
    <section class="inst-hero">
      <div class="container">
        <div class="inst-badge">企业专属门户</div>
        <h1>{{ data?.company?.name || store.companyName }}</h1>
        <p>{{ data?.config?.seo_description || '专注工业锅炉制造与全生命周期服务，提供选型、方案、报价、安装与维保。' }}</p>
        <div class="inst-meta">
          <span>{{ data?.company?.industry || '锅炉制造与销售' }}</span>
          <span>服务热线 {{ data?.config?.service_phone || '400-800-6688' }}</span>
          <span>工业锅炉全生命周期服务</span>
        </div>
        <div class="inst-actions">
          <router-link :to="`/demand`" class="btn btn-primary">提交采购需求</router-link>
          <router-link :to="`/consult`" class="btn btn-light">在线咨询</router-link>
        </div>
      </div>
    </section>
    <div class="container page-pad">
      <div class="section-head">
        <div><span class="eyebrow">PRODUCTS</span><h2>企业产品体系</h2><p>在售型号均完成资质绑定，支持按参数筛选与预约咨询。</p></div>
        <router-link :to="`/products`" class="text-link">全部产品 →</router-link>
      </div>
      <div v-if="data?.products?.length" class="product-grid">
        <ProductCard v-for="p in data.products" :key="p.id" :product="p" />
      </div>
      <div v-else class="empty">暂无上架产品</div>

      <div class="two-col" style="margin-top:60px">
        <div>
          <div class="section-head" style="margin-bottom:24px"><div><span class="eyebrow">CASES</span><h2>成功案例</h2></div></div>
          <div class="case-list">
            <div v-for="(c, i) in data?.cases || []" :key="c.id" class="case-row">
              <img v-if="firstImage(c.images)" class="case-thumb" :src="firstImage(c.images)" :alt="c.title" />
              <div class="case-index">0{{ i + 1 }}</div>
              <div><h3>{{ c.title }}</h3><p>{{ c.effect }}</p><span>{{ c.industry }} · {{ c.params }}</span></div>
            </div>
          </div>
        </div>
        <div>
          <div class="section-head" style="margin-bottom:24px"><div><span class="eyebrow">QUALIFICATIONS</span><h2>资质公示</h2></div></div>
          <div class="cert-box">
            <div v-for="c in data?.certificates || []" :key="c.certNo" class="cert-item">
              <div class="cert-type">{{ label(c.certType) }}</div>
              <div><strong class="mono">{{ c.certNo }}</strong><span>{{ c.issuer }}</span></div>
              <div class="cert-valid"><span :class="['pill', c.status === 'VALID' ? 'green' : 'yellow']">{{ label(c.status) }}</span><em>{{ date(c.validTo) }}</em></div>
            </div>
            <div v-if="!data?.certificates?.length" class="muted">资质材料归档中，可联系顾问提供。</div>
            <router-link to="/verify" class="text-link" style="margin-top:14px">公开核验入口 →</router-link>
          </div>
          <div class="contact-box">
            <h3>联系企业</h3>
            <div><span>服务热线</span><strong>{{ data?.config?.service_phone || '400-800-6688' }}</strong></div>
            <div><span>微信公众号</span><strong>{{ data?.config?.wechat_oa || 'xinhuo-boiler' }}</strong></div>
            <div><span>ICP 备案</span><strong>{{ data?.config?.icp || '黑ICP备2026000001号' }}</strong></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import ProductCard from '@/components/ProductCard.vue'
import { sessionStore } from '@/stores/session'
import { label, date, firstImage } from '@/utils'

const store = sessionStore(); const data = ref<any>(null)
onMounted(async () => {
  try {
    data.value = await http.get('/public/institution')
    if (data.value?.company?.name) store.setCompany(data.value.company.name)
  } catch {}
})
</script>

<style scoped>
.inst-hero { background: radial-gradient(circle at 85% 10%, rgba(217,99,59,.18), transparent 28%), linear-gradient(135deg, #0f172a, #1c2c43); color: #fff; padding: 72px 0 58px; }
.inst-badge { display: inline-block; border: 1px solid rgba(255,255,255,.18); padding: 4px 10px; font-size: 10px; letter-spacing: 1px; color: #c8d3e2; }
.inst-hero h1 { font-size: 42px; margin: 18px 0 12px; letter-spacing: -1px; }
.inst-hero p { max-width: 700px; color: #a6b4c7; font-size: 14px; line-height: 1.9; }
.inst-meta { display: flex; gap: 20px; flex-wrap: wrap; font-size: 11px; color: #8fa0b8; margin: 16px 0 24px; }
.inst-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.two-col { display: grid; grid-template-columns: 1.2fr .8fr; gap: 36px; align-items: start; }
.case-list { border-top: 1px solid var(--line); }
.case-row { display: flex; gap: 16px; padding: 20px 0; border-bottom: 1px solid var(--line); align-items: flex-start; }
.case-thumb { width: 110px; height: 78px; object-fit: cover; border-radius: 8px; border: 1px solid var(--line); flex-shrink: 0; }
.case-index { font-size: 11px; color: var(--accent); padding-top: 3px; }
.case-row h3 { margin: 0 0 5px; font-size: 16px; }
.case-row p { color: var(--steel); font-size: 12px; margin: 0 0 5px; }
.case-row span { font-size: 10px; color: var(--steel-2); }
.cert-box, .contact-box { background: #fff; border: 1px solid var(--line); padding: 20px; margin-bottom: 16px; }
.cert-item { display: flex; gap: 12px; padding: 11px 0; border-bottom: 1px dashed var(--line); align-items: center; }
.cert-item:last-of-type { border-bottom: none; }
.cert-type { font-size: 10px; background: var(--accent-soft); color: var(--accent); padding: 4px 8px; white-space: nowrap; }
.cert-item > div:nth-child(2) { flex: 1; display: flex; flex-direction: column; gap: 2px; font-size: 11px; }
.cert-item span { color: var(--steel); }
.cert-valid { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; }
.cert-valid em { font-style: normal; font-size: 10px; color: var(--steel-2); }
.contact-box h3 { font-size: 16px; margin: 0 0 12px; }
.contact-box div { display: flex; justify-content: space-between; gap: 16px; padding: 7px 0; border-bottom: 1px dashed var(--line); font-size: 12px; }
.contact-box div span { color: var(--steel); }
.eyebrow { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: 1fr 1fr; } .two-col { grid-template-columns: 1fr; } .inst-hero h1 { font-size: 31px; } }
@media (max-width: 600px) { .product-grid { grid-template-columns: 1fr; } }
</style>
