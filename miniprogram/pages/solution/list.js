const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { rows: [] },
  onShow() { if (api.ensureLogin()) this.load() },
  async load() {
    try {
      const data = await api.get('/customer/solutions')
      this.setData({ rows: (data || []).map((s) => ({ ...s, statusLabel: fmt.label(s.status), pill: fmt.pillClass(s.status), confirmTimeText: s.confirmTime ? '已确认 ' + fmt.date(s.confirmTime) : '待确认' })) })
    } catch (e) {}
  },
  open(e) { wx.navigateTo({ url: '/pages/solution/detail?id=' + e.currentTarget.dataset.id }) },
  async confirm(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({ title: '确认方案', content: '确认后进入报价谈价阶段。', success: async (res) => {
      if (!res.confirm) return
      try { await api.post('/customer/solutions/' + id + '/confirm'); wx.showToast({ title: '已确认', icon: 'success' }); this.load() } catch (err) {}
    }})
  }
})
