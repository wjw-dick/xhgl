App({
  globalData: {
    apiBase: 'http://127.0.0.1:8080/api',
    token: '',
    user: null,
    customer: null,
    companyName: '鑫火锅炉'
  },

  onLaunch() {
    const token = wx.getStorageSync('xh_mp_token') || ''
    const user = wx.getStorageSync('xh_mp_user') || null
    const customer = wx.getStorageSync('xh_mp_customer') || null
    const companyName = wx.getStorageSync('xh_mp_company_name') || '鑫火锅炉'
    this.globalData.token = token
    this.globalData.user = user
    this.globalData.customer = customer
    this.globalData.companyName = companyName
    wx.removeStorageSync('xh_mp_mode')
    this.syncTabBar()
  },

  onShow() {
    this.syncTabBar()
  },

  setAuth(data) {
    this.globalData.token = data.token || ''
    this.globalData.user = data.user || null
    this.globalData.customer = data.customer || null
    this.globalData.companyName = (data.company && data.company.name) || this.globalData.companyName
    wx.setStorageSync('xh_mp_token', this.globalData.token)
    wx.setStorageSync('xh_mp_user', this.globalData.user)
    wx.setStorageSync('xh_mp_customer', this.globalData.customer)
    wx.setStorageSync('xh_mp_company_name', this.globalData.companyName)
    this.syncTabBar()
  },

  clearAuth() {
    this.globalData.token = ''
    this.globalData.user = null
    this.globalData.customer = null
    wx.removeStorageSync('xh_mp_token')
    wx.removeStorageSync('xh_mp_user')
    wx.removeStorageSync('xh_mp_customer')
    wx.removeStorageSync('xh_mp_mode')
  },

  syncTabBar() {
    wx.request({
      url: this.globalData.apiBase + '/public/menus?target=MP',
      method: 'GET',
      success(res) {
        const payload = res.data || {}
        if (payload.code === 0 && Array.isArray(payload.data) && payload.data.length >= 4) {
          const titles = payload.data.slice(0, 4).map((m) => m.title)
          titles.forEach((title, index) => {
            wx.setTabBarItem({ index, text: title })
          })
        }
      }
    })
  }
})
