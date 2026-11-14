const labels = {
  NEW: '新需求', CONSULTING: '咨询中', INTRODUCING: '介绍中', TALKING: '谈价中', DEAL: '成交', LOST: '流失',
  DRAFT: '草稿', PUSHED: '已推送', CONFIRMED: '已确认', SENT: '已发送', BARGAINING: '还价中', EXPIRED: '已过期',
  APPROVED: '已审批', PENDING: '待审批', UNSIGNED: '待签署', SIGNED: '已签署', PERFORMING: '履约中',
  ACCEPTED: '已受理', DISPATCHED: '已派单', PROCESSING: '处理中', WAIT_REVIEW: '待回访', COMPLETED: '已完成',
  PAID: '已收款', UNPAID: '待收款', OVERDUE: '已逾期', VALID: '有效', WARNING: '临期',
  steam: '蒸汽锅炉', hotwater: '热水锅炉', thermal: '导热油炉', wasteheat: '余热锅炉',
  gas: '燃气', oil: '燃油', electric: '电', biomass: '生物质', coal: '燃煤',
  atmospheric: '常压', low: '低压', medium: '中压', high: '高压',
  REPAIR: '报修', INSTALL: '安装', MAINTAIN: '保养', INSPECT: '年检', ANNUAL: '年检', PERIODIC: '定检'
}
function label(v) { return v ? labels[v] || v : '—' }
function money(v) {
  const n = Number(v)
  return Number.isNaN(n) ? '—' : '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function date(v) { return v ? String(v).slice(0, 10) : '—' }
function dateTime(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '—' }
function pillClass(v) {
  if (!v) return 'gray'
  if (['DEAL', 'CONFIRMED', 'SIGNED', 'COMPLETED', 'PAID', 'VALID', 'APPROVED', 'PERFORMING'].includes(v)) return 'green'
  if (['LOST', 'EXPIRED', 'OVERDUE'].includes(v)) return 'red'
  if (['NEW', 'DRAFT', 'PENDING', 'UNSIGNED', 'UNPAID', 'WARNING', 'SENT', 'BARGAINING'].includes(v)) return 'yellow'
  return 'blue'
}
module.exports = { label, money, date, dateTime, pillClass }
