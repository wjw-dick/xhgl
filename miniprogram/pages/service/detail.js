const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { detail: null, records: [], statusLabel: '', typeLabel: '', pill: '', canReview: false, score: 5, reviewComment: '', stars: [1, 2, 3, 4, 5] },
  onLoad(options) { this.id = options.id; this.load() },
  async load() {
    try {
      const detail = await api.get('/customer/tickets/' + this.id)
      this.setData({
        detail,
        statusLabel: fmt.label(detail.ticket.status), typeLabel: fmt.label(detail.ticket.type), pill: fmt.pillClass(detail.ticket.status),
        canReview: ['WAIT_REVIEW', 'COMPLETED'].includes(detail.ticket.status),
        score: detail.ticket.satisfactionScore || 5, reviewComment: detail.ticket.review || '',
        records: (detail.records || []).map((r) => ({ ...r, timeText: fmt.dateTime(r.createdAt) }))
      })
    } catch (e) {}
  },
  selectScore(e) { this.setData({ score: Number(e.currentTarget.dataset.score) }) },
  onReviewInput(e) { this.setData({ reviewComment: e.detail.value }) },
  async submitReview() {
    try {
      await api.post('/customer/tickets/' + this.id + '/review', { score: this.data.score, comment: this.data.reviewComment })
      wx.showToast({ title: '评价已提交', icon: 'success' }); this.load()
    } catch (e) {}
  }
})
