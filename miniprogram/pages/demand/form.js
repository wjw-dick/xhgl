const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    loading: false,
    form: { boilerType: 'steam', fuelType: 'gas', capacity: '6 t/h', pressureLevel: 'low', source: 'MP', customerName: '', contactName: '', phone: '', region: '', expectTime: '', usageDesc: '', description: '', budgetMin: '', budgetMax: '' },
    boilerIndex: 0, fuelIndex: 0, capacityIndex: 2, pressureIndex: 1,
    boilerTypes: [{ label: '蒸汽锅炉', value: 'steam' }, { label: '热水锅炉', value: 'hotwater' }, { label: '导热油炉', value: 'thermal' }, { label: '余热锅炉', value: 'wasteheat' }],
    fuelTypes: [{ label: '燃气', value: 'gas' }, { label: '燃油', value: 'oil' }, { label: '电', value: 'electric' }, { label: '生物质', value: 'biomass' }, { label: '燃煤', value: 'coal' }],
    capacities: ['2 t/h', '4 t/h', '6 t/h', '10 t/h', '20 t/h'].map((v) => ({ label: v, value: v })),
    pressures: [{ label: '常压', value: 'atmospheric' }, { label: '低压', value: 'low' }, { label: '中压', value: 'medium' }, { label: '高压', value: 'high' }]
  },
  onLoad() {
    const customer = app.globalData.customer || {}
    const user = app.globalData.user || {}
    this.setData({ 'form.customerName': customer.name || '', 'form.contactName': user.name || customer.name || '' })
  },
  onInput(e) { this.setData({ ['form.' + e.currentTarget.dataset.key]: e.detail.value }) },
  onBoiler(e) { this.setData({ boilerIndex: Number(e.detail.value), 'form.boilerType': this.data.boilerTypes[Number(e.detail.value)].value }) },
  onFuel(e) { this.setData({ fuelIndex: Number(e.detail.value), 'form.fuelType': this.data.fuelTypes[Number(e.detail.value)].value }) },
  onCapacity(e) { this.setData({ capacityIndex: Number(e.detail.value), 'form.capacity': this.data.capacities[Number(e.detail.value)].value }) },
  onPressure(e) { this.setData({ pressureIndex: Number(e.detail.value), 'form.pressureLevel': this.data.pressures[Number(e.detail.value)].value }) },
  onDate(e) { this.setData({ 'form.expectTime': e.detail.value }) },
  async submit() {
    if (!this.data.form.customerName || !this.data.form.phone || !this.data.form.region) return wx.showToast({ title: '企业、电话、地区必填', icon: 'none' })
    this.setData({ loading: true })
    try {
      const data = await api.post('/public/demands', { ...this.data.form })
      wx.showModal({ title: '需求提交成功', content: '需求编号：' + data.demandNo + '\n工作人员将在2小时内联系您。', showCancel: false, success: () => wx.navigateBack() })
    } catch (e) {
      if (e.statusCode === 409) {
        wx.showModal({ title: '检测到相似需求', content: e.message + '\n仍要提交吗？', success: (res) => { if (res.confirm) this.forceSubmit() } })
      }
    } finally { this.setData({ loading: false }) }
  },
  async forceSubmit() {
    try { const data = await api.post('/public/demands', { ...this.data.form, force: true }); wx.showModal({ title: '已提交', content: '需求编号：' + data.demandNo, showCancel: false, success: () => wx.navigateBack() }) } catch (e) {}
  }
})
