<template>
  <footer class="portal-footer">
    <div class="container footer-top">
      <div class="footer-brand">
        <div class="footer-logo">
          <span class="mark">鑫</span>
          <div><strong>鑫火锅炉</strong><em>XINHUO BOILER SAAS</em></div>
        </div>
        <p>工业锅炉制造、选型、销售、安装与维保全生命周期数字化服务。所有锅炉产品资质可公开核验，安装与检验遵循 TSG 11、TSG 07 与《特种设备安全法》。</p>
        <div class="footer-badges">
          <span>制造许可</span><span>产品监检</span><span>能效报告</span><span>安装许可</span>
        </div>
      </div>
      <div class="footer-col">
        <strong>产品与方案</strong>
        <router-link to="/products">蒸汽锅炉</router-link>
        <router-link to="/products">热水锅炉</router-link>
        <router-link to="/products">导热油炉</router-link>
        <router-link to="/products">余热锅炉</router-link>
      </div>
      <div class="footer-col">
        <strong>服务支持</strong>
        <router-link to="/demand">提交采购需求</router-link>
        <router-link to="/consult">在线咨询</router-link>
        <router-link to="/verify">资质公开核验</router-link>
        <router-link to="/track">需求进度查询</router-link>
        <router-link to="/account">客户中心</router-link>
      </div>
      <div class="footer-col">
        <strong>联系我们</strong>
        <span class="footer-phone">400-800-6688</span>
        <span>周一至周日 8:00 - 20:00</span>
        <span>黑龙江省哈尔滨市香坊区工业街</span>
        <router-link to="/institution" class="footer-link">查看企业门户 →</router-link>
      </div>
    </div>
    <div class="container footer-bottom">
      <span>© {{ year }} 鑫火锅炉（黑龙江）有限公司 · 工业锅炉全生命周期服务平台</span>
      <div class="footer-legal">
        <router-link to="/agreement">用户协议</router-link>
        <router-link to="/agreement">隐私政策</router-link>
        <span>黑ICP备2026000001号</span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
const year = new Date().getFullYear()
</script>

<style scoped>
.portal-footer { background: #0b1220; color: #8e9bad; padding: 64px 0 24px; margin-top: 0; }
.footer-top { display: grid; grid-template-columns: 2fr 1fr 1fr 1.2fr; gap: 48px; padding-bottom: 42px; border-bottom: 1px solid rgba(255,255,255,.09); }
.footer-brand p { color: #8e9bad; font-size: 12px; line-height: 1.9; max-width: 420px; margin: 16px 0; }
.footer-logo { display: flex; gap: 12px; align-items: center; }
.mark { width: 36px; height: 36px; border: 1px solid rgba(255,255,255,.25); display: flex; align-items: center; justify-content: center; color: var(--accent); font-weight: 800; font-size: 17px; }
.footer-logo strong { display: block; color: #fff; font-size: 17px; letter-spacing: 2px; }
.footer-logo em { display: block; font-style: normal; font-size: 8px; color: #6d7c91; letter-spacing: 2px; margin-top: 3px; }
.footer-badges { display: flex; flex-wrap: wrap; gap: 6px; }
.footer-badges span { border: 1px solid rgba(255,255,255,.12); padding: 3px 8px; font-size: 10px; color: #9aa8bc; }
.footer-col { display: flex; flex-direction: column; gap: 10px; font-size: 12px; }
.footer-col strong { color: #e8edf4; font-size: 12px; letter-spacing: 1px; margin-bottom: 6px; }
.footer-col a, .footer-col span { color: #8e9bad; }
.footer-col a:hover { color: #fff; }
.footer-phone { color: var(--accent) !important; font-size: 22px; font-weight: 700; letter-spacing: .5px; }
.footer-link { color: var(--accent) !important; margin-top: 6px; }
.footer-bottom { padding-top: 20px; display: flex; justify-content: space-between; gap: 20px; font-size: 10px; color: #6d7c91; flex-wrap: wrap; }
.footer-legal { display: flex; gap: 18px; flex-wrap: wrap; }
.footer-legal a:hover { color: #fff; }
@media (max-width: 900px) {
  .footer-top { grid-template-columns: 1fr 1fr; gap: 30px; }
  .footer-brand { grid-column: 1 / -1; }
}
@media (max-width: 560px) {
  .portal-footer { padding-top: 44px; }
  .footer-top { grid-template-columns: 1fr; }
  .footer-bottom { flex-direction: column; }
}
</style>
