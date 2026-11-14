function request(path, method = 'GET', data = null, options = {}) {
  const app = getApp()
  return new Promise((resolve, reject) => {
    if (!options.silent) wx.showNavigationBarLoading()
    wx.request({
      url: app.globalData.apiBase + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
      },
      success(res) {
        wx.hideNavigationBarLoading()
        const payload = res.data || {}
        if (res.statusCode === 401) {
          app.clearAuth()
          wx.reLaunch({ url: '/pages/login/login' })
          return reject(new Error(payload.message || '登录已失效'))
        }
        if (payload.code === 0) return resolve(payload.data)
        if (!options.silent) wx.showToast({ title: payload.message || '请求失败', icon: 'none', duration: 2500 })
        reject({ statusCode: res.statusCode, message: payload.message || '请求失败', data: payload })
      },
      fail(err) {
        wx.hideNavigationBarLoading()
        if (!options.silent) wx.showToast({ title: '网络异常，请检查服务地址', icon: 'none' })
        reject(err)
      }
    })
  })
}

function ensureLogin() {
  const app = getApp()
  if (!app.globalData.token) {
    wx.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

module.exports = {
  get: (path, data) => request(path, 'GET', data),
  post: (path, data, options) => request(path, 'POST', data, options),
  put: (path, data) => request(path, 'PUT', data),
  ensureLogin
}
