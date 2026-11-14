const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { detail: null, payments: [], installations: [], statusLabel: '', auditLabel: '', signLabel: '', pill: '', amountText: '', canSign: false },
  onLoad(options) { this.id = options.id; this.load() },
  async load() {
    try {
      const detail = await api.get('/customer/contracts/' + this.id)
      const c = detail.contract
      this.setData({
        detail,
        statusLabel: fmt.label(c.status), auditLabel: fmt.label(c.auditStatus), signLabel: fmt.label(c.signStatus),
        pill: fmt.pillClass(c.status), amountText: fmt.money(c.amount),
        canSign: c.auditStatus === 'APPROVED' && c.signStatus !== 'SIGNED',
        payments: (detail.payments || []).map((p) => ({ ...p, amountText: fmt.money(p.amount), planDate: fmt.date(p.planDate), statusLabel: fmt.label(p.status), pill: fmt.pillClass(p.status) })),
        installations: (detail.installations || []).map((i) => ({ ...i, stageLabel: { SHIPPED: '发货', ARRIVED: '到货', INSTALL: '安装', ACCEPTANCE: '验收' }[i.stage] || i.stage, planDate: fmt.date(i.planDate), statusLabel: fmt.label(i.status), pill: fmt.pillClass(i.status) }))
      })
    } catch (e) {}
  },
  sign() {
    wx.showModal({ title: '在线签署', content: '确认签署合同？签署后进入履约。', success: async (res) => {
      if (!res.confirm) return
      try { await api.post('/customer/contracts/' + this.id + '/sign'); wx.showToast({ title: '已签署', icon: 'success' }); this.load() } catch (e) {}
    }})
  }
})
