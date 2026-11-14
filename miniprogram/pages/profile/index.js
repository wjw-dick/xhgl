const app = getApp()

Page({
  data: { user: {}, customer: {}, companyName: '' },
  onShow() {
    const g = app.globalData
    if (!g.token) { wx.reLaunch({ url: '/pages/login/login' }); return }
    this.setData({ user: g.user || {}, customer: g.customer || {}, companyName: g.companyName })
  },
  goDemands() { wx.switchTab({ url: '/pages/demand/list' }) },
  goSolutions() { wx.navigateTo({ url: '/pages/solution/list' }) },
  goQuotes() { wx.navigateTo({ url: '/pages/quote/list' }) },
  goContracts() { wx.navigateTo({ url: '/pages/contract/list' }) },
  goService() { wx.switchTab({ url: '/pages/service/tickets' }) },
  goConsult() { wx.navigateTo({ url: '/pages/consult/consult' }) },
  goAgreement() { wx.navigateTo({ url: '/pages/agreement/agreement' }) },
  logout() {
    wx.showModal({ title: '退出登录', content: '确认退出当前账号？', success: (res) => {
      if (!res.confirm) return
      app.clearAuth(); wx.reLaunch({ url: '/pages/login/login' })
    }})
  }
})
