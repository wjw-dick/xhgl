import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { sessionStore } from '@/stores/session'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true, title: '登录' } },
  { path: '/register', component: () => import('@/views/RegisterView.vue'), meta: { public: true, title: '注册客户' } },
  { path: '/home', component: () => import('@/views/HomeView.vue'), meta: { auth: true, tab: true, title: '首页' } },
  { path: '/demands', component: () => import('@/views/DemandsView.vue'), meta: { auth: true, tab: true, title: '需求' } },
  { path: '/demands/new', component: () => import('@/views/DemandFormView.vue'), meta: { auth: true, title: '提交需求' } },
  { path: '/consult', component: () => import('@/views/ConsultView.vue'), meta: { auth: true, title: '在线咨询' } },
  { path: '/solutions', component: () => import('@/views/SolutionsView.vue'), meta: { auth: true, title: '我的方案' } },
  { path: '/quotes', component: () => import('@/views/QuotesView.vue'), meta: { auth: true, title: '我的报价' } },
  { path: '/contracts', component: () => import('@/views/ContractsView.vue'), meta: { auth: true, title: '我的合同' } },
  { path: '/service', component: () => import('@/views/ServiceView.vue'), meta: { auth: true, tab: true, title: '售后' } },
  { path: '/profile', component: () => import('@/views/ProfileView.vue'), meta: { auth: true, tab: true, title: '我的' } },
  { path: '/agreement', component: () => import('@/views/AgreementView.vue'), meta: { title: '协议与隐私' } },

  { path: '/admin', redirect: '/admin/dashboard' },
  { path: '/admin/dashboard', component: () => import('@/views/admin/AdminDashboardView.vue'), meta: { auth: true, admin: true, tab: true, title: '管理台' } },
  { path: '/admin/customers', component: () => import('@/views/admin/AdminCustomersView.vue'), meta: { auth: true, admin: true, tab: true, title: '客户档案' } },
  { path: '/admin/customers/:id', component: () => import('@/views/admin/AdminCustomerDetailView.vue'), meta: { auth: true, admin: true, title: '客户详情' } },
  { path: '/admin/business', component: () => import('@/views/admin/AdminBusinessView.vue'), meta: { auth: true, admin: true, tab: true, title: '业务办理' } },
  { path: '/admin/profile', component: () => import('@/views/admin/AdminProfileView.vue'), meta: { auth: true, admin: true, tab: true, title: '我的' } },
  { path: '/admin/demands', component: () => import('@/views/admin/AdminDemandsView.vue'), meta: { auth: true, admin: true, title: '需求线索' } },
  { path: '/admin/demands/:id', component: () => import('@/views/admin/AdminDemandDetailView.vue'), meta: { auth: true, admin: true, title: '需求详情' } },
  { path: '/admin/consultations', component: () => import('@/views/admin/AdminConsultationsView.vue'), meta: { auth: true, admin: true, title: '售前咨询' } },
  { path: '/admin/solutions', component: () => import('@/views/admin/AdminSolutionsView.vue'), meta: { auth: true, admin: true, title: '方案与案例' } },
  { path: '/admin/quotes', component: () => import('@/views/admin/AdminQuotesView.vue'), meta: { auth: true, admin: true, title: '报价议价' } },
  { path: '/admin/quotes/:id', component: () => import('@/views/admin/AdminQuoteDetailView.vue'), meta: { auth: true, admin: true, title: '报价详情' } },
  { path: '/admin/contracts', component: () => import('@/views/admin/AdminContractsView.vue'), meta: { auth: true, admin: true, title: '合同履约' } },
  { path: '/admin/contracts/:id', component: () => import('@/views/admin/AdminContractDetailView.vue'), meta: { auth: true, admin: true, title: '合同详情' } },
  { path: '/admin/tickets', component: () => import('@/views/admin/AdminTicketsView.vue'), meta: { auth: true, admin: true, title: '售后服务' } },
  { path: '/admin/tickets/:id', component: () => import('@/views/admin/AdminTicketDetailView.vue'), meta: { auth: true, admin: true, title: '工单详情' } },
  { path: '/admin/maintenance', component: () => import('@/views/admin/AdminMaintenanceView.vue'), meta: { auth: true, admin: true, title: '维保计划' } },
  { path: '/admin/spares', component: () => import('@/views/admin/AdminSparesView.vue'), meta: { auth: true, admin: true, title: '备品备件' } },
  { path: '/admin/spare-records', component: () => import('@/views/admin/AdminSpareRecordsView.vue'), meta: { auth: true, admin: true, title: '库存流水' } },
  { path: '/admin/resource/:key', component: () => import('@/views/admin/AdminResourceView.vue'), meta: { auth: true, admin: true, title: '资料管理' } },
  { path: '/admin/announcements', component: () => import('@/views/admin/AdminAnnouncementsView.vue'), meta: { auth: true, admin: true, title: '公告管理' } },
  { path: '/admin/users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { auth: true, admin: true, title: '用户管理' } },
  { path: '/admin/reports', component: () => import('@/views/admin/AdminReportsView.vue'), meta: { auth: true, admin: true, title: '报表中心' } },
  { path: '/admin/warnings', component: () => import('@/views/admin/AdminWarningsView.vue'), meta: { auth: true, admin: true, title: '预警中心' } },
  { path: '/admin/notifications', component: () => import('@/views/admin/AdminNotificationsView.vue'), meta: { auth: true, admin: true, title: '消息通知' } },
  { path: '/admin/audits', component: () => import('@/views/admin/AdminAuditsView.vue'), meta: { auth: true, admin: true, title: '审计日志' } },
  { path: '/admin/settings', component: () => import('@/views/admin/AdminSettingsView.vue'), meta: { auth: true, admin: true, title: '系统设置' } },
  { path: '/admin/dicts', component: () => import('@/views/admin/AdminDictsView.vue'), meta: { auth: true, admin: true, title: '数据字典' } },
  { path: '/admin/menus', component: () => import('@/views/admin/AdminMenusView.vue'), meta: { auth: true, admin: true, title: '菜单配置' } },
  { path: '/admin/i18n', component: () => import('@/views/admin/AdminI18nView.vue'), meta: { auth: true, admin: true, title: '多语言' } },
  { path: '/admin/storage', component: () => import('@/views/admin/AdminStorageView.vue'), meta: { auth: true, admin: true, title: '对象存储' } },
  { path: '/admin/wechat', component: () => import('@/views/admin/AdminWechatView.vue'), meta: { auth: true, admin: true, title: '微信配置' } },

  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to) => {
  const store = sessionStore()
  if (store.isAdmin && (to.path === '/home' || to.path === '/profile' || to.path === '/register')) {
    return { path: '/admin/dashboard' }
  }
  if (to.meta.auth && !store.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !store.isAdmin) return { path: '/home' }
  if (store.isAdmin && to.meta.auth && !to.meta.admin) return { path: '/admin/dashboard' }
  document.title = `${to.meta.title || '鑫火锅炉'} · 鑫火锅炉`
  return true
})

export default router
