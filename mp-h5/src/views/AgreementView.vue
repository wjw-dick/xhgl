<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.back()">‹</button><h1>协议与隐私</h1></div>
    <div class="page-body">
      <div class="card">
        <span class="eyebrow">TERMS &amp; PRIVACY</span>
        <h2>用户协议与隐私政策</h2>
        <p>本协议适用于鑫火锅炉移动服务、小程序、公众号与客户中心。</p>
        <h3>账号与数据</h3>
        <p>客户仅可访问本人/本企业数据，越权访问将被拦截并记录审计。</p>
        <h3>业务效力</h3>
        <p>在线方案确认、报价确认与合同签署作为业务电子记录，最终以盖章合同与法定检验结论为准。</p>
        <h3>隐私保护</h3>
        <p>手机号等敏感字段加密存储、页面脱敏；微信 OpenID/UnionID 仅在授权后用于账号互通与业务通知，不向无关第三方出售。</p>
        <h3>特种设备合规</h3>
        <p>锅炉制造、安装、改造、修理与定期检验须遵守《特种设备安全法》、TSG 11、TSG 07。缺少制造许可/监检/能效报告的产品不得推荐成交。</p>
        <h3>联系</h3>
        <p>隐私与账号问题请联系客服热线 400-800-6688。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
<style scoped>
.eyebrow { font-size: 9px; color: var(--accent); letter-spacing: 1.6px; }
.card h2 { font-size: 20px; margin: 8px 0 12px; }
.card h3 { font-size: 14px; margin: 16px 0 6px; }
.card p { color: #3d4654; font-size: 12px; line-height: 1.9; margin: 4px 0; }
</style>
