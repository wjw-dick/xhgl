const api = require('../../utils/api')
const fmt = require('../../utils/format')
const app = getApp()

Page({
  data: {
    companyName: '鑫火锅炉', customer: {}, user: {},
    overview: { demandCount: 0, solutionCount: 0, quoteCount: 0, ticketCount: 0, recentDemands: [], recentTickets: [], maintenanceWarnings: [] },
    announcements: [],
    quick: [
      { title: '提交需求', path: '/pages/demand/form', icon: '＋' }, { title: '咨询', path: '/pages/consult/consult', icon: '?' },
      { title: '报价', path: '/pages/quote/list', icon: '¥' }, { title: '合同', path: '/pages/contract/list', icon: '文' },
      { title: '方案', path: '/pages/solution/list', icon: '方' }, { title: '报修', path: '/pages/service/repair', icon: '修' },
      { title: '维保', path: '/pages/service/tickets', icon: '检' }, { title: '我的', path: '/pages/profile/index', icon: '我' }
    ]
  },
  onShow() {
    if (!api.ensureLogin()) return
    this.setData({ companyName: app.globalData.companyName, customer: app.globalData.customer || {}, user: app.globalData.user || {} })
    this.load()
  },
  async load() {
    try {
      const data = await api.get('/customer/overview')
      const overview = data || {}
      overview.recentDemands = (overview.recentDemands || []).map((d) => ({
        ...d, boilerTypeLabel: fmt.label(d.boilerType), fuelTypeLabel: fmt.label(d.fuelType), statusLabel: fmt.label(d.status), pill: fmt.pillClass(d.status)
      }))
      overview.recentTickets = (overview.recentTickets || []).map((t) => ({
        ...t, typeLabel: fmt.label(t.type), statusLabel: fmt.label(t.status), pill: fmt.pillClass(t.status)
      }))
      overview.maintenanceWarnings = (overview.maintenanceWarnings || []).map((m) => {
        const left = Math.round((new Date(m.nextDate + 'T00:00:00').getTime() - new Date().setHours(0,0,0,0)) / 86400000)
        return { ...m, projectLabel: fmt.label(m.project), nextDateText: fmt.date(m.nextDate), remainText: left < 0 ? '已逾期' : '剩余' + left + '天' }
      })
      this.setData({ overview })
    } catch (e) {}
    try {
      const rows = await api.get('/public/announcements', null, { silent: true })
      this.setData({ announcements: (rows || []).slice(0, 5).map((a) => ({ ...a, dateText: fmt.date(a.publishTime), sourceLabel: a.source === 'SYSTEM' ? '系统' : '企业' })) })
    } catch (e) {}
  },
  go(e) { wx.navigateTo({ url: e.currentTarget.dataset.path }) },
  goProfile() { wx.switchTab({ url: '/pages/profile/index' }) },
  goDemandForm() { wx.navigateTo({ url: '/pages/demand/form' }) },
  goConsult() { wx.navigateTo({ url: '/pages/consult/consult' }) },
  goDemands() { wx.switchTab({ url: '/pages/demand/list' }) },
  goSolutions() { wx.navigateTo({ url: '/pages/solution/list' }) },
  goQuotes() { wx.navigateTo({ url: '/pages/quote/list' }) },
  goService() { wx.switchTab({ url: '/pages/service/tickets' }) }
})
