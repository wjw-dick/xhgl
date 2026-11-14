import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { authStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '工作台', icon: 'DataBoard' } },
      { path: 'customers', component: () => import('@/views/CustomersView.vue'), meta: { title: '客户档案', icon: 'User' } },
      { path: 'customers/:id', component: () => import('@/views/CustomerDetailView.vue'), meta: { title: '客户关联视图', icon: 'User' } },
      { path: 'products', component: () => import('@/views/ResourcePage.vue'), meta: { title: '锅炉产品', icon: 'Box', configKey: 'products' } },
      { path: 'certificates', component: () => import('@/views/ResourcePage.vue'), meta: { title: '资质证书', icon: 'Document', configKey: 'certificates' } },
      { path: 'demands', component: () => import('@/views/DemandsView.vue'), meta: { title: '需求线索', icon: 'Tickets' } },
      { path: 'demands/:id', component: () => import('@/views/DemandDetailView.vue'), meta: { title: '需求详情', icon: 'Tickets' } },
      { path: 'consultations', component: () => import('@/views/ConsultationsView.vue'), meta: { title: '售前咨询', icon: 'ChatDotRound' } },
      { path: 'solutions', component: () => import('@/views/SolutionsView.vue'), meta: { title: '方案与案例', icon: 'Reading' } },
      { path: 'quotes', component: () => import('@/views/QuotesView.vue'), meta: { title: '报价议价', icon: 'Money' } },
      { path: 'quotes/:id', component: () => import('@/views/QuoteDetailView.vue'), meta: { title: '报价详情', icon: 'Money' } },
      { path: 'contracts', component: () => import('@/views/ContractsView.vue'), meta: { title: '合同履约', icon: 'Files' } },
      { path: 'contracts/:id', component: () => import('@/views/ContractDetailView.vue'), meta: { title: '合同履约详情', icon: 'Files' } },
      { path: 'tickets', component: () => import('@/views/TicketsView.vue'), meta: { title: '售后服务', icon: 'Service' } },
      { path: 'maintenance', component: () => import('@/views/MaintenanceView.vue'), meta: { title: '维保计划', icon: 'Calendar' } },
      { path: 'spares', component: () => import('@/views/SparesView.vue'), meta: { title: '备品备件', icon: 'Setting' } },
      { path: 'spare-records', component: () => import('@/views/SpareRecordsView.vue'), meta: { title: '库存流水', icon: 'Tickets' } },
      { path: 'knowledge', component: () => import('@/views/ResourcePage.vue'), meta: { title: '服务知识库', icon: 'Collection', configKey: 'knowledge' } },
      { path: 'cases', component: () => import('@/views/ResourcePage.vue'), meta: { title: '成功案例', icon: 'Picture', configKey: 'cases' } },
      { path: 'visits', component: () => import('@/views/ResourcePage.vue'), meta: { title: '客户回访', icon: 'Phone', configKey: 'visits' } },
      { path: 'referrals', component: () => import('@/views/ResourcePage.vue'), meta: { title: '转介绍', icon: 'Share', configKey: 'referrals' } },
      { path: 'faqs', component: () => import('@/views/ResourcePage.vue'), meta: { title: 'FAQ 知识库', icon: 'QuestionFilled', configKey: 'faqs' } },
      { path: 'contract-templates', component: () => import('@/views/ResourcePage.vue'), meta: { title: '合同模板', icon: 'DocumentCopy', configKey: 'contractTemplates' } },
      { path: 'reports', component: () => import('@/views/ReportsView.vue'), meta: { title: '报表中心', icon: 'TrendCharts' } },
      { path: 'warnings', component: () => import('@/views/WarningsView.vue'), meta: { title: '预警中心', icon: 'Warning' } },
      { path: 'notifications', component: () => import('@/views/NotificationsView.vue'), meta: { title: '消息通知', icon: 'Bell' } },
      { path: 'announcements', component: () => import('@/views/AnnouncementsView.vue'), meta: { title: '公告管理', icon: 'Bell' } },
      { path: 'users', component: () => import('@/views/UsersView.vue'), meta: { title: '用户管理', icon: 'Avatar', adminOnly: true } },
      { path: 'dicts', component: () => import('@/views/DictsView.vue'), meta: { title: '数据字典', icon: 'Grid', adminOnly: true } },
      { path: 'menus', component: () => import('@/views/MenusView.vue'), meta: { title: '菜单配置', icon: 'Menu', adminOnly: true } },
      { path: 'i18n', component: () => import('@/views/I18nView.vue'), meta: { title: '多语言', icon: 'Switch', adminOnly: true } },
      { path: 'storage', component: () => import('@/views/StorageView.vue'), meta: { title: '对象存储', icon: 'Cloudy', adminOnly: true } },
      { path: 'wechat', component: () => import('@/views/WechatView.vue'), meta: { title: '微信配置', icon: 'ChatLineRound', adminOnly: true } },
      { path: 'settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '系统设置', icon: 'Tools', adminOnly: true } },
      { path: 'audits', component: () => import('@/views/AuditsView.vue'), meta: { title: '审计日志', icon: 'List', adminOnly: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const store = authStore()
  if (to.meta.public) return true
  if (!store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.adminOnly && !store.isAdmin) return { path: '/dashboard' }
  return true
})

export default router
