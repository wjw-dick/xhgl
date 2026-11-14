const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: {
    rows: [], loading: false, status: '',
    filters: [{ label: '全部', value: '' }, { label: '新需求', value: 'NEW' }, { label: '咨询中', value: 'CONSULTING' }, { label: '介绍中', value: 'INTRODUCING' }, { label: '谈价中', value: 'TALKING' }, { label: '成交', value: 'DEAL' }]
  },
  onShow() { if (api.ensureLogin()) this.load() },
  async load() {
    this.setData({ loading: true })
    try {
      const all = await api.get('/customer/demands')
      const rows = (this.data.status ? (all || []).filter((d) => d.status === this.data.status) : (all || [])).map((d) => ({
        ...d, boilerTypeLabel: fmt.label(d.boilerType), fuelTypeLabel: fmt.label(d.fuelType), pressureLabel: fmt.label(d.pressureLevel),
        statusLabel: fmt.label(d.status), pill: fmt.pillClass(d.status), expectDate: fmt.date(d.expectTime), createdDate: fmt.date(d.createdAt)
      }))
      this.setData({ rows })
    } catch (e) {} finally { this.setData({ loading: false }) }
  },
  selectStatus(e) { this.setData({ status: e.currentTarget.dataset.value }, () => this.load()) },
  goForm() { wx.navigateTo({ url: '/pages/demand/form' }) },
  openDetail(e) { wx.navigateTo({ url: '/pages/demand/detail?id=' + e.currentTarget.dataset.id }) }
})
