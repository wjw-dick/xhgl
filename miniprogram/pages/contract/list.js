const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { rows: [] },
  onShow() { if (api.ensureLogin()) this.load() },
  async load() {
    try {
      const data = await api.get('/customer/contracts')
      this.setData({ rows: (data || []).map((c) => ({ ...c, statusLabel: fmt.label(c.status), signLabel: fmt.label(c.signStatus), pill: fmt.pillClass(c.status), amountText: fmt.money(c.amount) })) })
    } catch (e) {}
  },
  open(e) { wx.navigateTo({ url: '/pages/contract/detail?id=' + e.currentTarget.dataset.id }) }
})
