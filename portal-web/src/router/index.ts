import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { sessionStore } from '@/stores/session'

const routes: RouteRecordRaw[] = [
  { path: '/', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/products', component: () => import('@/views/ProductsView.vue'), meta: { title: '产品中心' } },
  { path: '/products/:id', component: () => import('@/views/ProductDetailView.vue'), meta: { title: '产品详情' } },
  { path: '/demand', component: () => import('@/views/DemandView.vue'), meta: { title: '提交采购需求' } },
  { path: '/consult', component: () => import('@/views/ConsultView.vue'), meta: { title: '在线咨询' } },
  { path: '/verify', component: () => import('@/views/VerifyView.vue'), meta: { title: '资质核验' } },
  { path: '/institution', component: () => import('@/views/InstitutionView.vue'), meta: { title: '企业门户' } },
  { path: '/announcements', component: () => import('@/views/AnnouncementsView.vue'), meta: { title: '资讯公告' } },
  { path: '/announcements/:id', component: () => import('@/views/AnnouncementDetailView.vue'), meta: { title: '公告详情' } },
  { path: '/track', component: () => import('@/views/TrackView.vue'), meta: { title: '进度查询' } },
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { title: '用户登录', public: true } },
  { path: '/register', component: () => import('@/views/RegisterView.vue'), meta: { title: '用户注册', public: true } },
  { path: '/account', component: () => import('@/views/AccountView.vue'), meta: { title: '客户中心', auth: true } },
  { path: '/share/solution', component: () => import('@/views/ShareSolutionView.vue'), meta: { title: '方案确认' } },
  { path: '/share/quote', component: () => import('@/views/ShareQuoteView.vue'), meta: { title: '报价确认' } },
  { path: '/share/demand', component: () => import('@/views/ShareDemandView.vue'), meta: { title: '需求进度' } },
  { path: '/agreement', component: () => import('@/views/AgreementView.vue'), meta: { title: '协议与隐私' } },
  { path: '/oauth', component: () => import('@/views/OAuthView.vue'), meta: { title: '微信授权登录' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to) => {
  const store = sessionStore()
  const lang = (to.query.lang as string) || ''
  if (lang) store.setLang(lang)
  if (to.meta.auth && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  document.title = `${to.meta.title || '鑫火锅炉'}｜鑫火锅炉`
  return true
})

export default router
