const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { rows: [] },
  onShow() { if (api.ensureLogin()) this.load() },
  async load() {
    try {
      const data = await api.get('/customer/quotes')
      this.setData({ rows: (data || []).map((q) => ({ ...q, statusLabel: fmt.label(q.status), pill: fmt.pillClass(q.status), amountText: fmt.money(q.totalAmount), validDate: fmt.date(q.validUntil) })) })
    } catch (e) {}
  },
  open(e) { wx.navigateTo({ url: '/pages/quote/detail?id=' + e.currentTarget.dataset.id }) }
})
