export const labels: Record<string, string> = {
  NEW: '新需求', CONSULTING: '咨询中', INTRODUCING: '介绍中', TALKING: '谈价中', DEAL: '成交', LOST: '流失',
  DRAFT: '草稿', PUSHED: '已推送', CONFIRMED: '已确认', SENT: '已发送', BARGAINING: '还价中', EXPIRED: '已过期',
  APPROVAL: '审批中', APPROVED: '已审批', PENDING: '待审批', REJECTED: '已驳回', NONE: '未审批',
  UNSIGNED: '待签署', SIGNED: '已签署', PERFORMING: '履约中', ARCHIVED: '已归档',
  ACCEPTED: '已受理', DISPATCHED: '已派单', PROCESSING: '处理中', WAIT_REVIEW: '待回访', COMPLETED: '已完成',
  PAID: '已收款', UNPAID: '待收款', OVERDUE: '已逾期', VALID: '有效', WARNING: '临期', ACTIVE: '有效', DISABLED: '停用',
  NORMAL: '正常', ONLINE: '已上架', OFFLINE: '已下架', PUBLISHED: '已发布', OK: '资质齐全', MISSING: '缺少资质',
  WAITING: '待回复', DONE: '已完成', CONVERTED: '已转需求', ISSUED: '已开具', VOID: '已作废', PLANNED: '待回访',
  HIGH: '紧急', LOW: '低', MANUAL: '人工录入', PHONE: '电话', H5: 'H5门户', MP: '小程序', OA: '公众号',
  REGISTER: '客户注册', MAINTAIN: '定期保养', ANNUAL: '年检', PERIODIC: '定检', STOCKTAKE: '盘点', IN: '入库', OUT: '领用',
  steam: '蒸汽锅炉', hotwater: '热水锅炉', thermal: '导热油炉', wasteheat: '余热锅炉',
  gas: '燃气', oil: '燃油', electric: '电', biomass: '生物质', coal: '燃煤',
  atmospheric: '常压', low: '低压', medium: '中压', high: '高压',
  REPAIR: '报修', INSTALL: '安装', INSPECT: '年检'
}
export function label(v?: string) { return v ? labels[v] || v : '—' }
export function money(v: any) { const n = Number(v); return Number.isNaN(n) ? '—' : '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
export function date(v?: any) { return v ? String(v).slice(0, 10) : '—' }
export function dateTime(v?: any) { return v ? String(v).replace('T', ' ').slice(0, 16) : '—' }
export function statusPill(v?: string) {
  if (!v) return 'gray'
  if (['DEAL', 'CONFIRMED', 'SIGNED', 'COMPLETED', 'PAID', 'VALID', 'APPROVED', 'PERFORMING', 'NORMAL', 'ONLINE', 'PUBLISHED', 'DONE', 'ISSUED', 'ACTIVE', 'OK'].includes(v)) return 'green'
  if (['LOST', 'EXPIRED', 'OVERDUE', 'REJECTED', 'DISABLED', 'MISSING', 'HIGH', 'VOID'].includes(v)) return 'red'
  if (['NEW', 'DRAFT', 'PENDING', 'UNSIGNED', 'UNPAID', 'WARNING', 'SENT', 'BARGAINING', 'APPROVAL', 'WAITING', 'PLANNED', 'CONVERTED'].includes(v)) return 'yellow'
  return 'blue'
}
export function daysLeft(value: string) {
  if (!value) return 999
  return Math.round((new Date(value + 'T00:00:00').getTime() - new Date().setHours(0, 0, 0, 0)) / 86400000)
}
export function sanitizeHtml(html?: string) {
  if (!html) return ''
  const t = document.createElement('template'); t.innerHTML = html
  t.content.querySelectorAll('script, style, iframe, object, embed').forEach((n) => n.remove())
  t.content.querySelectorAll('*').forEach((node) => {
    for (const attr of Array.from(node.attributes)) {
      if (attr.name.startsWith('on') || (['href', 'src'].includes(attr.name) && /javascript:|data:text\/html/.test(attr.value))) node.removeAttribute(attr.name)
    }
  })
  return t.innerHTML
}
