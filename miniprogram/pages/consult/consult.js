const api = require('../../utils/api')
const app = getApp()

Page({
  data: { form: { question: '', contactName: '', phone: '' }, result: null, faqs: [], loading: false },
  onLoad() {
    const customer = app.globalData.customer || {}
    const user = app.globalData.user || {}
    this.setData({ 'form.contactName': user.name || customer.name || '' })
    this.loadFaqs()
  },
  async loadFaqs() {
    try { const rows = await api.get('/public/faqs'); this.setData({ faqs: rows || [] }) } catch (e) {}
  },
  onInput(e) { this.setData({ ['form.' + e.currentTarget.dataset.key]: e.detail.value }) },
  selectFaq(e) { this.setData({ 'form.question': e.currentTarget.dataset.q }) },
  async submit() {
    if (!this.data.form.question) return wx.showToast({ title: '请输入咨询内容', icon: 'none' })
    this.setData({ loading: true })
    try {
      const result = await api.post('/public/consultations', { ...this.data.form, source: 'MP', customerName: app.globalData.customer && app.globalData.customer.name })
      this.setData({ result })
      wx.showToast({ title: '咨询已提交', icon: 'success' })
    } catch (e) {} finally { this.setData({ loading: false }) }
  }
})
