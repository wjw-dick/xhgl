const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { solution: null, content: '', statusLabel: '', pill: '' },
  onLoad(options) { this.id = options.id; this.load() },
  async load() {
    try {
      const solution = await api.get('/customer/solutions/' + this.id)
      this.setData({ solution, content: solution.content || '', statusLabel: fmt.label(solution.status), pill: fmt.pillClass(solution.status) })
    } catch (e) {}
  },
  confirm() {
    wx.showModal({ title: '确认方案', content: '确认后进入报价谈价阶段。', success: async (res) => {
      if (!res.confirm) return
      try { await api.post('/customer/solutions/' + this.id + '/confirm'); wx.showToast({ title: '方案已确认', icon: 'success' }); this.load() } catch (e) {}
    }})
  }
})
