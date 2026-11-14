const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    form: { description: '', contactName: '', equipmentId: null }, equipment: [], equipmentIndex: -1,
    equipmentId: null, typeIndex: 0, priorityIndex: 0,
    types: [{ label: '报修', value: 'REPAIR' }, { label: '保养', value: 'MAINTAIN' }, { label: '年检', value: 'INSPECT' }],
    priorities: [{ label: '普通', value: 'NORMAL' }, { label: '紧急', value: 'HIGH' }, { label: '低', value: 'LOW' }],
    photos: [], uploading: false, loading: false
  },
  onLoad() { this.loadEquipment(); this.setData({ 'form.contactName': (app.globalData.user && app.globalData.user.name) || (app.globalData.customer && app.globalData.customer.name) || '' }) },
  async loadEquipment() { try { const rows = await api.get('/customer/equipment'); this.setData({ equipment: rows || [] }) } catch (e) {} },
  onInput(e) { this.setData({ ['form.' + e.currentTarget.dataset.key]: e.detail.value }) },
  onEquipment(e) { const i = Number(e.detail.value); this.setData({ equipmentIndex: i, 'form.equipmentId': this.data.equipment[i].id }) },
  onType(e) { this.setData({ typeIndex: Number(e.detail.value) }) },
  onPriority(e) { this.setData({ priorityIndex: Number(e.detail.value) }) },
  choosePhotos() {
    if (this.data.uploading) return
    const remain = 6 - this.data.photos.length
    if (remain <= 0) return wx.showToast({ title: '最多上传 6 张照片', icon: 'none' })
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => this.uploadPhotos(res.tempFiles.map((f) => f.tempFilePath))
    })
  },
  uploadPhotos(paths) {
    if (!paths.length) return
    this.setData({ uploading: true })
    const app = getApp()
    const authHeader = { Authorization: app.globalData.token ? 'Bearer ' + app.globalData.token : '' }
    const tasks = paths.map((path) => new Promise((resolve) => {
      wx.uploadFile({
        url: app.globalData.apiBase + '/customer/files/upload',
        filePath: path,
        name: 'file',
        formData: { category: 'ticket' },
        header: authHeader,
        success: (res) => {
          let payload = {}
          try { payload = JSON.parse(res.data || '{}') } catch (e) { payload = {} }
          if (payload.code === 0 && payload.data && payload.data.url) {
            this.setData({ photos: this.data.photos.concat(payload.data.url) })
            return resolve(true)
          }
          wx.showToast({ title: payload.message || '照片上传失败', icon: 'none' })
          resolve(false)
        },
        fail: () => { wx.showToast({ title: '网络异常，照片上传失败', icon: 'none' }); resolve(false) }
      })
    }))
    Promise.all(tasks).then((results) => {
      this.setData({ uploading: false })
      const uploaded = results.filter(Boolean).length
      if (uploaded) wx.showToast({ title: '已上传 ' + uploaded + ' 张照片', icon: 'none' })
    })
  },
  removePhoto(e) {
    const photos = this.data.photos.slice()
    photos.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ photos })
  },
  previewPhoto(e) {
    const index = Number(e.currentTarget.dataset.index)
    wx.previewImage({ current: this.data.photos[index], urls: this.data.photos })
  },
  async submit() {
    if (!this.data.form.description) return wx.showToast({ title: '请填写故障描述', icon: 'none' })
    if (this.data.uploading) return wx.showToast({ title: '照片上传中，请稍候', icon: 'none' })
    this.setData({ loading: true })
    try {
      await api.post('/customer/tickets', { ...this.data.form, photos: this.data.photos.join(','), type: this.data.types[this.data.typeIndex].value, priority: this.data.priorities[this.data.priorityIndex].value })
      wx.showModal({ title: '报修成功', content: '工单已受理，售后会尽快派单。', showCancel: false, success: () => wx.switchTab({ url: '/pages/service/tickets' }) })
    } catch (e) { if (e.statusCode === 409) wx.showModal({ title: '重复报修提示', content: e.message, showCancel: false }) } finally { this.setData({ loading: false }) }
  }
})
