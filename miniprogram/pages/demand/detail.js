const api = require('../../utils/api')
const fmt = require('../../utils/format')

Page({
  data: { detail: null, follows: [], solutions: [], quotes: [], statusLabel: '', pill: '', boilerLabel: '', fuelLabel: '', pressureLabel: '', budgetText: '', expectDate: '' },
  onLoad(options) { this.id = options.id; this.load() },
  async load() {
    try {
      const detail = await api.get('/customer/demands/' + this.id)
      const demand = detail.demand
      this.setData({
        detail,
        statusLabel: fmt.label(demand.status),
        pill: fmt.pillClass(demand.status),
        boilerLabel: fmt.label(demand.boilerType),
        fuelLabel: fmt.label(demand.fuelType),
        pressureLabel: fmt.label(demand.pressureLevel),
        budgetText: fmt.money(demand.budgetMin) + ' ~ ' + fmt.money(demand.budgetMax),
        expectDate: fmt.date(demand.expectTime),
        follows: (detail.followRecords || []).map((f) => ({ ...f, timeText: fmt.dateTime(f.followTime) })),
        solutions: (detail.solutions || []).map((s) => ({ ...s, statusLabel: fmt.label(s.status), pill: fmt.pillClass(s.status) })),
        quotes: (detail.quotes || []).map((q) => ({ ...q, statusLabel: fmt.label(q.status), pill: fmt.pillClass(q.status), amountText: fmt.money(q.totalAmount) }))
      })
    } catch (e) {}
  }
})
