const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { tab: 'ticket', tickets: [], maintenance: [], pending: 0, due: 0 },
  onShow() { if (api.ensureLogin()) this.load() },
  async load() {
    try {
      const tickets = await api.get('/customer/tickets')
      const maintenance = await api.get('/customer/maintenance')
      const pending = (tickets || []).filter((t) => ['ACCEPTED', 'DISPATCHED', 'PROCESSING'].includes(t.status)).length
      const due = (maintenance || []).filter((m) => {
        const left = Math.round((new Date(m.nextDate + 'T00:00:00').getTime() - new Date().setHours(0,0,0,0)) / 86400000)
        return left <= 30
      }).length
      this.setData({
        tickets: (tickets || []).map((t) => ({ ...t, typeLabel: fmt.label(t.type), statusLabel: fmt.label(t.status), pill: fmt.pillClass(t.status) })),
        maintenance: (maintenance || []).map((m) => {
          const left = Math.round((new Date(m.nextDate + 'T00:00:00').getTime() - new Date().setHours(0,0,0,0)) / 86400000)
          return { ...m, projectLabel: fmt.label(m.project), nextDate: fmt.date(m.nextDate), remainText: left < 0 ? '已逾期' : '剩余' + left + '天', level: left < 0 ? 'red' : left <= 30 ? 'yellow' : 'green' }
        }),
        pending, due
      })
    } catch (e) {}
  },
  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }) },
  goRepair() { wx.navigateTo({ url: '/pages/service/repair' }) },
  openDetail(e) { wx.navigateTo({ url: '/pages/service/detail?id=' + e.currentTarget.dataset.id }) }
})
