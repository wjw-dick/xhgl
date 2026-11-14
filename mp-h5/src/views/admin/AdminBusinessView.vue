<template>
  <div class="page">
    <div class="topbar"><h1>业务办理</h1><span class="right">ALL MODULES</span></div>
    <div class="page-body">
      <div class="notice">与 PC 后台使用同一套管理员接口，数据实时同步。点击模块进入移动端办理页。</div>

      <div class="section-label">客户与售前</div>
      <div class="module-grid">
        <router-link v-for="m in groups.customer" :key="m.path" :to="m.path" class="module-card">
          <span class="module-icon"><AppIcon :name="m.icon" :size="19" /></span>
          <strong>{{ m.title }}</strong><em>{{ m.desc }}</em>
        </router-link>
      </div>

      <div class="section-label">产品与方案</div>
      <div class="module-grid">
        <router-link v-for="m in groups.product" :key="m.path" :to="m.path" class="module-card">
          <span class="module-icon"><AppIcon :name="m.icon" :size="19" /></span>
          <strong>{{ m.title }}</strong><em>{{ m.desc }}</em>
        </router-link>
      </div>

      <div class="section-label">成交履约与售后</div>
      <div class="module-grid">
        <router-link v-for="m in groups.fulfillment" :key="m.path" :to="m.path" class="module-card">
          <span class="module-icon"><AppIcon :name="m.icon" :size="19" /></span>
          <strong>{{ m.title }}</strong><em>{{ m.desc }}</em>
        </router-link>
      </div>

      <div class="section-label">运营与系统</div>
      <div class="module-grid">
        <router-link v-for="m in groups.system" :key="m.path" :to="m.path" class="module-card">
          <span class="module-icon"><AppIcon :name="m.icon" :size="19" /></span>
          <strong>{{ m.title }}</strong><em>{{ m.desc }}</em>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
const resourcePath = (key: string) => `/admin/resource/${key}`
const groups = {
  customer: [
    { title: '客户档案', path: '/admin/customers', icon: 'users', desc: '建档、联系人、聚合视图' },
    { title: '需求线索', path: '/admin/demands', icon: 'doc', desc: '认领、分配、跟进、推进' },
    { title: '售前咨询', path: '/admin/consultations', icon: 'chat', desc: '答复、转需求、删除' },
    { title: '客户回访', path: resourcePath('visits'), icon: 'calendar', desc: '计划、记录、满意度' },
    { title: '转介绍', path: resourcePath('referrals'), icon: 'share', desc: '商机登记与跟进' },
    { title: '成功案例', path: resourcePath('cases'), icon: 'file', desc: '案例库维护' }
  ],
  product: [
    { title: '锅炉产品', path: resourcePath('products'), icon: 'package', desc: '型号、参数、上下架' },
    { title: '资质证书', path: resourcePath('certificates'), icon: 'shield', desc: '台账与到期管理' },
    { title: '方案与案例', path: '/admin/solutions', icon: 'layers', desc: '方案、推送、版本' },
    { title: '报价议价', path: '/admin/quotes', icon: 'money', desc: '报价、议价、审批' },
    { title: '合同模板', path: resourcePath('contractTemplates'), icon: 'file', desc: '模板维护' },
    { title: '服务知识库', path: resourcePath('knowledge'), icon: 'book', desc: '故障与维保知识' },
    { title: 'FAQ 知识库', path: resourcePath('faqs'), icon: 'chat', desc: '常见问题维护' }
  ],
  fulfillment: [
    { title: '合同履约', path: '/admin/contracts', icon: 'file', desc: '审批、签署、回款、发票' },
    { title: '售后服务', path: '/admin/tickets', icon: 'tool', desc: '派单、记录、完工、评价' },
    { title: '维保计划', path: '/admin/maintenance', icon: 'calendar', desc: '保养、年检、定检' },
    { title: '备品备件', path: '/admin/spares', icon: 'package', desc: '库存与出入库' },
    { title: '库存流水', path: '/admin/spare-records', icon: 'layers', desc: '领用与预警' },
    { title: '报表中心', path: '/admin/reports', icon: 'chart', desc: '经营、回款、售后' }
  ],
  system: [
    { title: '消息通知', path: '/admin/notifications', icon: 'bell', desc: '消息与模板日志' },
    { title: '公告管理', path: '/admin/announcements', icon: 'bell', desc: '企业公告发布' },
    { title: '预警中心', path: '/admin/warnings', icon: 'alert', desc: '证照、报价、回款、维保' },
    { title: '用户管理', path: '/admin/users', icon: 'user', desc: '账号、角色、重置密码' },
    { title: '系统设置', path: '/admin/settings', icon: 'settings', desc: '企业资料与参数' },
    { title: '数据字典', path: '/admin/dicts', icon: 'book', desc: '全局与自定义字典' },
    { title: '菜单配置', path: '/admin/menus', icon: 'menu', desc: '后台/移动导航' },
    { title: '多语言', path: '/admin/i18n', icon: 'globe', desc: '语言启停' },
    { title: '对象存储', path: '/admin/storage', icon: 'cloud', desc: '5 种存储驱动' },
    { title: '微信配置', path: '/admin/wechat', icon: 'chat', desc: '配置、菜单、素材' },
    { title: '审计日志', path: '/admin/audits', icon: 'file', desc: '关键操作留痕' }
  ]
}
</script>

<style scoped>
.module-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.module-card { background: var(--surface); border: 1px solid var(--line); border-radius: 10px; padding: 13px; display: flex; flex-direction: column; gap: 4px; min-height: 104px; }
.module-icon { width: 30px; height: 30px; border-radius: 8px; background: #f5f1ea; color: var(--ink-2); display: flex; align-items: center; justify-content: center; margin-bottom: 3px; }
.module-card strong { font-size: 12px; font-weight: 650; letter-spacing: -.2px; }
.module-card em { font-style: normal; font-size: 9px; color: var(--steel-2); line-height: 1.55; }
</style>
