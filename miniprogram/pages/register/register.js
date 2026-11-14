const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    loading: false, error: '',
    form: { companyName: '', industry: '', region: '', contactName: '', phone: '', email: '', username: '', password: '' },
    confirmPassword: ''
  },
  onInput(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.key]: e.detail.value })
  },
  onConfirmInput(e) {
    this.setData({ confirmPassword: e.detail.value })
  },
  validate() {
    const f = this.data.form
    if (!f.contactName.trim()) return '请输入联系人姓名'
    if (!/^1[3-9][0-9]{9}$/.test(f.phone)) return '请输入正确的 11 位手机号'
    if (!/^[A-Za-z0-9_]{4,32}$/.test(f.username)) return '登录账号须为 4-32 位字母、数字或下划线'
    if (f.password.length < 8 || !/[A-Za-z]/.test(f.password) || !/[0-9]/.test(f.password)) {
      return '密码至少 8 位，且同时包含字母和数字'
    }
    if (f.password !== this.data.confirmPassword) return '两次输入的密码不一致'
    return ''
  },
  async submit() {
    const message = this.validate()
    if (message) { this.setData({ error: message }); return }
    this.setData({ loading: true, error: '' })
    try {
      const data = await api.post('/public/auth/register', {
        companyName: this.data.form.companyName.trim(),
        industry: this.data.form.industry.trim(),
        region: this.data.form.region.trim(),
        contactName: this.data.form.contactName.trim(),
        phone: this.data.form.phone,
        email: this.data.form.email.trim(),
        username: this.data.form.username.trim(),
        password: this.data.form.password
      })
      app.setAuth(data)
      wx.showToast({ title: '注册成功，已自动登录', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 800)
    } catch (e) {
      this.setData({ error: e.message || '注册失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
  goLogin() {
    wx.redirectTo({ url: '/pages/login/login' })
  }
})
