const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { detail: null, negotiations: [], statusLabel: '', pill: '', amountText: '', unitPriceText: '', freightText: '', installText: '', validDate: '', canConfirm: false },
  onLoad(options) { this.id = options.id; this.load() },
  async load() {
    try {
      const detail = await api.get('/customer/quotes/' + this.id)
      const q = detail.quote
      this.setData({
        detail,
        statusLabel: fmt.label(q.status), pill: fmt.pillClass(q.status),
        amountText: fmt.money(q.totalAmount), unitPriceText: fmt.money(q.unitPrice),
        freightText: fmt.money(q.freight), installText: fmt.money(q.installFee), validDate: fmt.date(q.validUntil),
        canConfirm: ['SENT', 'BARGAINING'].includes(q.status),
        negotiations: (detail.negotiations || []).map((n) => ({ ...n, partyLabel: n.party === 'CUSTOMER' ? '客户还价' : '我方报价', amountText: fmt.money(n.amount) }))
      })
    } catch (e) {}
  },
  confirm() {
    wx.showModal({ title: '确认报价', content: '确认后自动生成合同草稿。', success: async (res) => {
      if (!res.confirm) return
      try { await api.post('/customer/quotes/' + this.id + '/confirm'); wx.showToast({ title: '已确认', icon: 'success' }); this.load() } catch (e) {}
    }})
  },
  bargain() {
    wx.showModal({ title: '提交还价', editable: true, placeholderText: '请输入期望总价（元）', success: async (res) => {
      if (!res.confirm || !res.content) return
      try { await api.post('/customer/quotes/' + this.id + '/bargain', { amount: Number(res.content), remark: '小程序在线还价' }); wx.showToast({ title: '还价已提交', icon: 'success' }); this.load() } catch (e) {}
    }})
  }
})
