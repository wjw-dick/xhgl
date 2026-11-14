const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    loading: false,
    form: { username: '', password: '' }
  },
  onInput(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.key]: e.detail.value })
  },
  goRegister() {
    wx.redirectTo({ url: '/pages/register/register' })
  },
  async login() {
    if (!this.data.form.username || !this.data.form.password) {
      wx.showToast({ title: '请输入账号与密码', icon: 'none' }); return
    }
    this.setData({ loading: true })
    try {
      const data = await api.post('/public/auth/login', this.data.form)
      app.setAuth(data)
      wx.showToast({ title: '登录成功', icon: 'success' })
      wx.switchTab({ url: '/pages/index/index' })
    } catch (e) {
      // 错误已由 api 提示
    } finally { this.setData({ loading: false }) }
  },
  wechatLogin() {
    wx.login({
      success: async (res) => {
        if (!res.code) return wx.showToast({ title: '微信登录失败', icon: 'none' })
        try {
          const data = await api.post('/public/auth/wechat', { code: res.code })
          app.setAuth(data)
          wx.switchTab({ url: '/pages/index/index' })
        } catch (e) {}
      }
    })
  }
})
