<template>
  <div>
    <section class="hero">
      <div class="hero-grid container">
        <div class="hero-copy">
          <span class="kicker"><i /> {{ t('heroKicker') }}</span>
          <h1>{{ t('heroTitle') }}</h1>
          <p class="lead">{{ t('heroDesc') }}</p>
          <div class="hero-actions">
            <router-link to="/demand" class="btn btn-primary">{{ t('heroCta') }} <span>→</span></router-link>
            <router-link to="/products" class="btn btn-light">{{ t('heroSecondary') }}</router-link>
          </div>
          <div class="hero-proof">
            <div><strong>98.2<em>%</em></strong><span>高效热效率</span></div>
            <div><strong>≤30<em>mg</em></strong><span>低氮排放</span></div>
            <div><strong>24<em>h</em></strong><span>售后响应</span></div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="visual-badge">WNS 系列 · 低氮冷凝</div>
          <svg viewBox="0 0 520 400" class="hero-svg" aria-label="工业锅炉示意">
            <defs>
              <linearGradient id="vessel" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0" stop-color="#e2e8ef" /><stop offset=".48" stop-color="#9eacbb" /><stop offset=".52" stop-color="#7f8e9d" /><stop offset="1" stop-color="#b9c4cf" />
              </linearGradient>
              <linearGradient id="pipe" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#f1f5f9" /><stop offset="1" stop-color="#64748b" />
              </linearGradient>
              <radialGradient id="glow" cx="50%" cy="50%" r="50%">
                <stop offset="0" stop-color="#e2703a" stop-opacity=".55" /><stop offset="1" stop-color="#e2703a" stop-opacity="0" />
              </radialGradient>
            </defs>
            <ellipse cx="250" cy="350" rx="210" ry="28" fill="#050b14" opacity=".35" />
            <rect x="86" y="104" width="340" height="196" rx="32" fill="url(#vessel)" stroke="#556270" stroke-width="2" />
            <rect x="100" y="118" width="312" height="36" rx="18" fill="rgba(255,255,255,.28)" />
            <rect x="118" y="138" width="276" height="8" rx="4" fill="rgba(15,23,42,.18)" />
            <circle cx="178" cy="215" r="40" fill="#0f172a" stroke="#dbe3ea" stroke-width="4" />
            <circle cx="178" cy="215" r="32" fill="none" stroke="#8ca0b2" stroke-width="1.5" />
            <circle cx="178" cy="215" r="2.4" fill="#f8fafc" />
            <path d="M178 215 L198 190" stroke="#e2703a" stroke-width="3" stroke-linecap="round" />
            <circle cx="268" cy="196" r="30" fill="#0f172a" stroke="#dbe3ea" stroke-width="4" />
            <path d="M268 196 L268 176" stroke="#f8fafc" stroke-width="2" stroke-linecap="round" />
            <text x="268" y="240" text-anchor="middle" fill="#e2e8f0" font-size="11" font-family="sans-serif">1.25 MPa</text>
            <rect x="306" y="188" width="88" height="6" rx="3" fill="rgba(15,23,42,.22)" />
            <rect x="306" y="208" width="66" height="6" rx="3" fill="rgba(15,23,42,.15)" />
            <rect x="306" y="228" width="76" height="6" rx="3" fill="rgba(15,23,42,.1)" />
            <rect x="112" y="284" width="288" height="16" rx="8" fill="#475569" />
            <rect x="130" y="304" width="24" height="58" rx="6" fill="#64748b" />
            <rect x="358" y="304" width="24" height="58" rx="6" fill="#64748b" />
            <path d="M150 100 V54 H232" fill="none" stroke="url(#pipe)" stroke-width="20" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M360 118 H440 V208 H424 V242" fill="none" stroke="url(#pipe)" stroke-width="18" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M430 242 H442 V280" fill="none" stroke="#cbd5e1" stroke-width="9" stroke-linecap="round" />
            <circle cx="252" cy="62" r="7" fill="#e2703a" />
            <circle cx="196" cy="112" r="4" fill="#e2703a" opacity=".9" />
            <circle cx="112" cy="206" r="5" fill="#e2703a" opacity=".8" />
            <ellipse cx="254" cy="204" rx="240" ry="170" fill="url(#glow)" opacity=".5" />
            <path d="M280 82 C280 59 298 55 298 32 M302 82 C302 59 320 55 320 32" fill="none" stroke="rgba(255,255,255,.42)" stroke-width="4" stroke-linecap="round" class="steam-line" />
          </svg>
          <div class="visual-caption">
            <span>智能群控</span><span>低氮燃烧</span><span>远程运维</span>
          </div>
        </div>
      </div>
      <div class="hero-bottom">
        <div class="container compliance-strip">
          <div class="compliance-label">特种设备合规材料</div>
          <div class="compliance-items">
            <span><i>✓</i> 制造许可</span><span><i>✓</i> 产品监检证明</span><span><i>✓</i> 能效测试报告</span><span><i>✓</i> 安装许可</span><span><i>✓</i> 年检/定检台账</span>
          </div>
          <router-link to="/verify" class="compliance-link">公开核验 →</router-link>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="container">
        <div class="section-head">
          <div>
            <span class="eyebrow">PRODUCT SYSTEM</span>
            <h2>{{ t('sectionProducts') }}</h2>
            <p>{{ t('sectionProductsDesc') }}</p>
          </div>
          <router-link to="/products" class="btn btn-ghost btn-sm">{{ t('viewAll') }} <span>→</span></router-link>
        </div>
        <div class="product-grid">
          <ProductCard v-for="p in home?.products || []" :key="p.id" :product="p" />
        </div>
        <div v-if="!home?.products?.length && !loading" class="empty"><h3>{{ t('noData') }}</h3><p>产品正在上架，请稍后再来。</p></div>
        <div v-if="loading" class="loading"><div class="spinner" />{{ t('loading') }}</div>
      </div>
    </section>

    <section class="section dark process-section">
      <div class="container">
        <div class="section-head">
          <div>
            <span class="eyebrow" style="color:#e28a63">FULL LIFECYCLE</span>
            <h2>从一条需求，到锅炉全生命周期服务</h2>
            <p>每个节点都在线留痕：需求登记、售前咨询、方案推送、报价确认、合同签署、发货安装、验收与售后。</p>
          </div>
          <router-link to="/demand" class="btn btn-primary">{{ t('demandSubmit') }}</router-link>
        </div>
        <div class="process-grid">
          <div v-for="(step, i) in process" :key="step.title" class="process-step">
            <span class="step-no">0{{ i + 1 }}</span>
            <div class="step-icon"><span v-html="step.icon" /></div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="section alt">
      <div class="container">
        <div class="section-head">
          <div>
            <span class="eyebrow">CASE STUDIES</span>
            <h2>{{ t('sectionCases') }}</h2>
            <p>{{ t('sectionCasesDesc') }}</p>
          </div>
          <router-link to="/products" class="btn btn-ghost btn-sm">浏览产品 <span>→</span></router-link>
        </div>
        <div class="case-grid">
          <div v-for="(c, index) in home?.cases || []" :key="c.id" class="case-card">
            <div class="case-visual" :class="`case-${index % 3}`">
              <span class="case-industry">{{ c.industry }}</span>
              <img v-if="firstImage(c.images)" class="case-photo" :src="firstImage(c.images)" :alt="c.title" />
              <svg v-else viewBox="0 0 300 140" class="case-art">
                <rect x="40" y="45" width="120" height="58" rx="10" fill="rgba(17,24,39,.75)" />
                <circle cx="100" cy="74" r="16" fill="none" stroke="#e2e8f0" stroke-width="2" />
                <path d="M100 74 L110 64" stroke="#e2703a" stroke-width="2" stroke-linecap="round" />
                <path d="M160 58 H230 V100 H210" fill="none" stroke="rgba(15,23,42,.5)" stroke-width="8" stroke-linecap="round" />
                <text x="52" y="92" fill="#f8fafc" font-size="9" font-family="sans-serif">{{ c.params }}</text>
              </svg>
            </div>
            <div class="case-body">
              <h3>{{ c.title }}</h3>
              <p class="case-effect">{{ c.effect }}</p>
              <div class="case-meta"><span>{{ c.scene }}</span><span>{{ c.params }}</span></div>
            </div>
          </div>
        </div>
        <div v-if="!home?.cases?.length && !loading" class="empty"><h3>{{ t('noData') }}</h3></div>
      </div>
    </section>

    <section class="section">
      <div class="container home-bottom-grid">
        <div>
          <div class="section-head" style="margin-bottom:24px">
            <div><span class="eyebrow">NEWS</span><h2>{{ t('sectionNews') }}</h2><p>{{ t('sectionNewsDesc') }}</p></div>
            <router-link to="/announcements" class="text-link">{{ t('viewAll') }} →</router-link>
          </div>
          <div class="news-list">
            <router-link v-for="a in home?.announcements || []" :key="a.id" :to="`/announcements/${a.id}`" class="news-item">
              <span class="news-date">{{ date(a.publishTime) }}</span>
              <div><h3>{{ a.title }}</h3><p>{{ excerpt(a.content) }}</p></div>
              <span class="news-arrow">→</span>
            </router-link>
            <div v-if="!home?.announcements?.length && !loading" class="empty">暂无公告</div>
          </div>
        </div>
        <aside class="cta-card">
          <div class="cta-kicker">免费选型 · 2 小时响应</div>
          <h3>不确定选多大锅炉？<br />让工程师帮您算。</h3>
          <p>提供蒸汽/热水用量、燃料价格与现场条件，我们将输出选型建议、运行成本测算与合规材料清单。</p>
          <div class="cta-actions">
            <router-link to="/demand" class="btn btn-primary btn-block">提交选型需求</router-link>
            <router-link to="/consult" class="btn btn-ghost btn-block">在线咨询工程师</router-link>
          </div>
          <div class="cta-contact">
            <span>选型顾问热线</span><strong>400-800-6688</strong>
          </div>
        </aside>
      </div>
    </section>

    <section class="institution-strip">
      <div class="container institution-inner">
        <div class="inst-title">
          <span class="eyebrow" style="color:#d9a58d">ENTERPRISE PORTAL</span>
          <h2>{{ home?.company?.name || store.companyName }}</h2>
          <p>{{ home?.config?.seo_description || '锅炉制造与销售企业，提供选型、报价、安装与维保全流程服务。' }}</p>
        </div>
        <div class="inst-points">
          <div><strong>{{ home?.productCount || '—' }}</strong><span>在售型号</span></div>
          <div><strong>{{ home?.caseCount || '—' }}</strong><span>成功案例</span></div>
          <div><strong>5</strong><span>合作存储驱动</span></div>
        </div>
        <router-link :to="`/institution`" class="btn btn-dark">进入企业门户 →</router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import ProductCard from '@/components/ProductCard.vue'
import { sessionStore } from '@/stores/session'
import { useI18n } from '@/i18n'
import { date, firstImage } from '@/utils'

const store = sessionStore()
const { t } = useI18n()
const home = ref<any>(null)
const loading = ref(false)
const process = [
  { title: '需求登记', desc: '提交锅炉类型、燃料、蒸发量、压力、用途与预算，自动生成需求编号。', icon: '⌁' },
  { title: '售前咨询', desc: '工程师依据 FAQ 与现场条件初筛，需求自动沉淀到客户档案。', icon: '◎' },
  { title: '方案选型', desc: '按参数匹配产品库，输出富文本方案与合规材料清单，客户在线确认。', icon: '▤' },
  { title: '报价谈价', desc: '多版本报价与逐轮议价留痕，底价红线自动审批，客户在线确认。', icon: '¥' },
  { title: '成交履约', desc: '合同签署、生产发货、定金/发货款/尾款与安装验收节点可视。', icon: '▣' },
  { title: '售后维保', desc: '报修派单、现场记录、年检定检与备件库存，验收后自动建档。', icon: '✚' }
]
function excerpt(html?: string) {
  return (html || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').slice(0, 72) + '…'
}
onMounted(async () => {
  loading.value = true
  try {
    home.value = await http.get('/public/portal/home')
    if (home.value?.company?.name) store.setCompany(home.value.company.name)
    document.title = `${home.value?.company?.name || '鑫火锅炉'}｜工业锅炉制造与全生命周期服务`
  } finally { loading.value = false }
})
</script>

<style scoped>
.hero {
  background: radial-gradient(circle at 82% 22%, rgba(217,99,59,.16), transparent 32%), linear-gradient(135deg, #0b1220 0%, #16243a 55%, #0d1728 100%);
  color: #fff; position: relative; overflow: hidden;
}
.hero::before {
  content: ''; position: absolute; inset: 0; background-image: linear-gradient(rgba(255,255,255,.035) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.035) 1px, transparent 1px); background-size: 42px 42px; pointer-events: none;
}
.hero-grid { display: grid; grid-template-columns: 1.05fr .95fr; gap: 40px; align-items: center; padding: 86px 0 64px; position: relative; z-index: 1; }
.kicker { display: inline-flex; align-items: center; gap: 8px; border: 1px solid rgba(255,255,255,.15); border-radius: 999px; padding: 5px 12px; font-size: 10px; letter-spacing: 1.4px; color: #c7d2e1; }
.kicker i { width: 5px; height: 5px; background: var(--accent); border-radius: 50%; display: inline-block; }
h1 { font-size: 48px; line-height: 1.22; letter-spacing: -1.4px; margin: 22px 0 18px; font-weight: 700; max-width: 620px; }
.lead { color: #aeb9c9; font-size: 15px; line-height: 1.95; max-width: 560px; }
.hero-actions { display: flex; gap: 12px; margin-top: 30px; flex-wrap: wrap; }
.hero-proof { display: flex; gap: 36px; margin-top: 42px; padding-top: 24px; border-top: 1px solid rgba(255,255,255,.1); }
.hero-proof strong { display: block; font-size: 26px; font-weight: 700; color: #fff; letter-spacing: -.5px; }
.hero-proof strong em { font-style: normal; font-size: 12px; color: #98a6bb; margin-left: 2px; }
.hero-proof span { font-size: 10px; color: #8795a9; letter-spacing: .4px; }
.hero-visual { position: relative; }
.visual-badge { position: absolute; top: -12px; right: 12px; font-size: 10px; color: #b9c6d8; border: 1px solid rgba(255,255,255,.16); padding: 5px 10px; letter-spacing: .5px; background: rgba(11,18,32,.75); z-index: 2; }
.hero-svg { width: 100%; height: auto; display: block; filter: drop-shadow(0 30px 40px rgba(0,0,0,.35)); }
.steam-line { animation: steam 4s ease-in-out infinite; }
@keyframes steam { 0%,100% { opacity: .25; transform: translateY(4px); } 50% { opacity: .7; transform: translateY(-6px); } }
.visual-caption { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.visual-caption span { font-size: 10px; color: #93a3b8; border: 1px solid rgba(255,255,255,.12); padding: 3px 8px; }
.hero-bottom { position: relative; z-index: 1; background: rgba(255,255,255,.04); border-top: 1px solid rgba(255,255,255,.08); }
.compliance-strip { height: 62px; display: flex; align-items: center; gap: 26px; }
.compliance-label { font-size: 10px; color: #8291a6; letter-spacing: 1.4px; white-space: nowrap; }
.compliance-items { display: flex; flex-wrap: wrap; gap: 22px; flex: 1; }
.compliance-items span { font-size: 11px; color: #aeb9c9; display: inline-flex; align-items: center; gap: 6px; }
.compliance-items i { font-style: normal; color: var(--accent); }
.compliance-link { font-size: 11px; color: #d9a58d; white-space: nowrap; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.process-section { background: #0f172a; }
.process-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1px; background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.08); }
.process-step { background: #101a2c; padding: 26px 24px 30px; position: relative; }
.step-no { position: absolute; right: 20px; top: 18px; font-size: 11px; color: #4a5d76; letter-spacing: 1px; }
.step-icon { width: 38px; height: 38px; border: 1px solid rgba(255,255,255,.15); display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 18px; margin-bottom: 16px; }
.process-step h3 { font-size: 15px; margin: 0 0 8px; color: #f4f7fb; }
.process-step p { font-size: 11px; color: #8495ad; line-height: 1.8; margin: 0; }
.case-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.case-card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); overflow: hidden; }
.case-visual { height: 160px; background: linear-gradient(135deg, #dfe5eb, #cdd6df); position: relative; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.case-photo { width: 100%; height: 100%; object-fit: cover; display: block; }
.case-visual.case-1 { background: linear-gradient(135deg, #ece5dd, #d9cec2); }
.case-visual.case-2 { background: linear-gradient(135deg, #e3e8e4, #ccd7d0); }
.case-industry { position: absolute; top: 12px; left: 12px; font-size: 10px; background: rgba(17,24,39,.72); color: #fff; padding: 3px 9px; border-radius: 2px; }
.case-art { width: 78%; height: auto; }
.case-body { padding: 18px 20px 20px; }
.case-body h3 { font-size: 16px; margin: 0 0 8px; line-height: 1.45; }
.case-effect { font-size: 12px; color: var(--steel); margin: 0 0 14px; line-height: 1.7; }
.case-meta { display: flex; gap: 8px; flex-wrap: wrap; border-top: 1px solid #efebe3; padding-top: 10px; font-size: 10px; color: var(--steel-2); }
.home-bottom-grid { display: grid; grid-template-columns: 1.6fr .9fr; gap: 46px; align-items: start; }
.news-list { border-top: 1px solid var(--line); }
.news-item { display: grid; grid-template-columns: 74px 1fr 24px; gap: 14px; padding: 18px 0; border-bottom: 1px solid var(--line); align-items: start; }
.news-date { font-size: 10px; color: var(--steel-2); padding-top: 4px; letter-spacing: .3px; }
.news-item h3 { font-size: 15px; margin: 0 0 4px; font-weight: 600; }
.news-item p { font-size: 11px; color: var(--steel); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 540px; }
.news-arrow { color: var(--steel-2); padding-top: 2px; }
.news-item:hover h3 { color: var(--accent); }
.cta-card { background: #101b2e; color: #fff; padding: 32px 30px 28px; position: relative; overflow: hidden; }
.cta-card::after { content: ''; position: absolute; width: 180px; height: 180px; right: -70px; bottom: -80px; border: 1px solid rgba(255,255,255,.1); border-radius: 50%; }
.cta-kicker { font-size: 10px; color: #e28a63; letter-spacing: 1.2px; }
.cta-card h3 { font-size: 24px; line-height: 1.42; margin: 14px 0 14px; letter-spacing: -.4px; }
.cta-card p { color: #97a5b9; font-size: 12px; line-height: 1.85; margin-bottom: 22px; }
.cta-actions { display: flex; flex-direction: column; gap: 9px; position: relative; z-index: 1; }
.cta-contact { margin-top: 26px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,.1); }
.cta-contact span { display: block; font-size: 10px; color: #8495ad; }
.cta-contact strong { font-size: 22px; color: #fff; letter-spacing: .5px; }
.institution-strip { background: var(--paper-2); border-top: 1px solid var(--line); padding: 44px 0; }
.institution-inner { display: flex; align-items: center; gap: 40px; }
.inst-title { flex: 1; }
.inst-title h2 { font-size: 26px; margin: 8px 0 6px; }
.inst-title p { color: var(--steel); font-size: 12px; margin: 0; max-width: 560px; }
.inst-points { display: flex; gap: 30px; }
.inst-points div strong { display: block; font-size: 24px; }
.inst-points div span { font-size: 10px; color: var(--steel-2); }
@media (max-width: 1000px) {
  .hero-grid { grid-template-columns: 1fr; padding-top: 60px; }
  .hero-visual { max-width: 540px; margin: 0 auto; width: 100%; }
  .product-grid, .case-grid, .process-grid { grid-template-columns: 1fr 1fr; }
  .home-bottom-grid { grid-template-columns: 1fr; }
  .institution-inner { flex-wrap: wrap; }
  .inst-points { order: 3; }
}
@media (max-width: 700px) {
  h1 { font-size: 34px; letter-spacing: -1px; }
  .hero-proof { gap: 22px; }
  .product-grid, .case-grid, .process-grid { grid-template-columns: 1fr; }
  .compliance-strip { flex-wrap: wrap; padding: 14px 0; height: auto; gap: 10px; }
  .compliance-items { gap: 12px; }
  .news-item { grid-template-columns: 1fr 20px; }
  .news-date { grid-column: 1 / -1; }
  .inst-points { gap: 20px; }
}
</style>
