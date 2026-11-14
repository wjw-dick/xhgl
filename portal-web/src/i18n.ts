import { computed } from 'vue'
import { sessionStore } from '@/stores/session'

const messages: Record<string, Record<string, string>> = {
  'zh-CN': {
    navHome: '首页', navProducts: '产品中心', navDemand: '提交需求', navVerify: '资质核验', navNews: '资讯公告', navAccount: '客户中心',
    heroKicker: 'TSG 11 · 特种设备合规', heroTitle: '工业锅炉选型、报价与全生命周期服务',
    heroDesc: '从采购需求、售前咨询、方案选型、报价议价到合同履约、安装验收与年检定检，一套系统贯通锅炉业务全流程。',
    heroCta: '提交采购需求', heroSecondary: '查看产品中心', heroProof1: '制造许可', heroProof2: '产品监检', heroProof3: '能效报告',
    contact: '联系顾问', phone: '400-800-6688', login: '用户登录', logout: '退出',
    sectionProducts: '锅炉产品', sectionProductsDesc: '燃气、燃油、电、生物质、燃煤锅炉，按蒸发量、压力与场景精准选型。',
    sectionCases: '成功案例', sectionCasesDesc: '食品、化工、医药、供热等行业的稳定运行实践。',
    sectionNews: '资讯公告', sectionNewsDesc: '资质公示、产品动态与维保提醒。',
    viewAll: '查看全部', learnMore: '了解更多', demandSubmit: '在线提交需求', backHome: '返回首页', loading: '加载中…', noData: '暂无数据',
    formCompany: '企业名称', formContact: '联系人', formPhone: '联系电话', formSubmit: '提交需求', formSubmitting: '提交中…',
    accountTitle: '客户中心', accountOverview: '概览', accountDemands: '我的需求', accountSolutions: '我的方案', accountQuotes: '我的报价', accountContracts: '我的合同', accountService: '售后服务', accountProfile: '企业档案'
  },
  en: {
    navHome: 'Home', navProducts: 'Products', navDemand: 'Requirement', navVerify: 'Verify', navNews: 'News', navAccount: 'Account',
    heroKicker: 'TSG 11 · Special Equipment Compliance', heroTitle: 'Industrial Boiler Selection, Quotation & Lifecycle Service',
    heroDesc: 'From procurement requirement, pre-sales consultation and solution design to quotation, contract fulfillment, installation and periodic inspection — one platform for the whole boiler business.',
    heroCta: 'Submit Requirement', heroSecondary: 'View Products', heroProof1: 'Manufacturing License', heroProof2: 'Inspection Certificate', heroProof3: 'Energy Efficiency Report',
    contact: 'Contact Sales', phone: '400-800-6688', login: 'Sign In', logout: 'Sign Out',
    sectionProducts: 'Boiler Products', sectionProductsDesc: 'Gas, oil, electric, biomass and coal boilers selected by capacity, pressure and application.',
    sectionCases: 'Case Studies', sectionCasesDesc: 'Stable operation practices in food, chemical, pharmaceutical and district heating.',
    sectionNews: 'News & Notices', sectionNewsDesc: 'Qualification disclosure, product updates and maintenance reminders.',
    viewAll: 'View All', learnMore: 'Learn More', demandSubmit: 'Submit Requirement', backHome: 'Back Home', loading: 'Loading…', noData: 'No data',
    formCompany: 'Company', formContact: 'Contact', formPhone: 'Phone', formSubmit: 'Submit', formSubmitting: 'Submitting…',
    accountTitle: 'Customer Center', accountOverview: 'Overview', accountDemands: 'Requirements', accountSolutions: 'Solutions', accountQuotes: 'Quotations', accountContracts: 'Contracts', accountService: 'Service', accountProfile: 'Company Profile'
  }
}

export function useI18n() {
  const store = sessionStore()
  const t = (key: string) => messages[store.lang]?.[key] || messages['zh-CN'][key] || key
  const lang = computed({
    get: () => store.lang,
    set: (value: string) => store.setLang(value)
  })
  return { t, lang }
}
