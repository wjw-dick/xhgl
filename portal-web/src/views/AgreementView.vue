<template>
  <div class="container page-pad" style="max-width:900px">
    <span class="kicker">TERMS &amp; PRIVACY</span>
    <h1>用户协议与隐私政策</h1>
    <p class="lead">本页面适用于鑫火锅炉门户、客户中心、移动 H5、微信小程序与公众号服务。锅炉属特种设备，平台仅提供业务信息化与在线协作工具，不替代行政许可、监检、检验与合同责任。</p>
    <div class="card article">
      <h2>一、用户协议</h2>
      <h3>1. 账号与访问</h3>
      <p>用户账号可通过自助注册或微信 UnionID 绑定。用户仅可查看本人/本企业需求、方案、报价、合同、回款与售后数据；禁止探测、批量撞库或越权访问他人数据。</p>
      <h3>2. 业务效力</h3>
      <p>在线报价确认、方案确认与合同签署为业务意向的电子记录，最终以双方盖章合同、付款凭证与法定检验结论为准。锅炉制造许可、产品监检、能效报告与安装许可由销售企业提供原件或监管平台核验。</p>
      <h3>3. 禁止行为</h3>
      <p>不得利用平台发布违法信息、伪造资质、绕过底价审批、诱导跳过法定检验，或以自动化工具攻击接口。平台将记录审计并保留追责权利。</p>
      <h2>二、隐私政策</h2>
      <h3>1. 收集范围</h3>
      <p>企业名称、联系人姓名、手机号、需求参数、地址、合同与售后记录；微信 OpenID/UnionID 仅在授权后用于账号互通和业务通知。</p>
      <h3>2. 使用目的</h3>
      <p>用于需求确认、方案报价、合同履约、售后维保与法定义务；不会向无关第三方出售个人信息。手机号、证件等敏感字段加密存储、页面脱敏。</p>
      <h3>3. 数据隔离与留存</h3>
      <p>系统按客户维度隔离数据。客户只能访问本人/本企业数据。审计日志、合同与检验资料按法律与业务需要留存。</p>
      <h3>4. 用户权利</h3>
      <p>可联系客服查询、更正企业联系人信息；账号停用或授权撤回可通过客服提出。微信授权撤回后，UnionID 登录将失效，历史业务记录仍依法保留。</p>
      <h2>三、特种设备合规声明</h2>
      <p>锅炉选型、制造、安装、改造、修理与定期检验须遵守《特种设备安全法》、TSG 11、TSG 07 等要求。缺少制造许可、产品监检或能效报告的产品不得上架推荐；在役锅炉须按周期完成年检/定检。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.kicker { font-size: 10px; color: var(--accent); letter-spacing: 2px; }
h1 { font-size: 34px; margin: 10px 0 8px; letter-spacing: -.8px; }
.lead { color: var(--steel); font-size: 13px; line-height: 1.9; max-width: 760px; }
.article { margin-top: 26px; padding: 40px 46px; }
.article h2 { font-size: 22px; margin: 28px 0 10px; }
.article h2:first-child { margin-top: 0; }
.article h3 { font-size: 15px; margin: 18px 0 6px; }
.article p { color: #3d4654; font-size: 13px; line-height: 1.95; margin: 6px 0; }
@media (max-width: 700px) { .article { padding: 24px; } h1 { font-size: 27px; } }
</style>
