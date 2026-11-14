export const labels: Record<string, string> = {
  NEW: '新需求', CONSULTING: '咨询中', INTRODUCING: '介绍中', TALKING: '谈价中', DEAL: '成交', LOST: '流失',
  DRAFT: '草稿', PUSHED: '已推送', CONFIRMED: '已确认', SENT: '已发送', BARGAINING: '还价中', EXPIRED: '已过期',
  APPROVED: '已审批', PENDING: '待审批', UNSIGNED: '待签署', SIGNED: '已签署', PERFORMING: '履约中',
  ACCEPTED: '已受理', DISPATCHED: '已派单', PROCESSING: '处理中', WAIT_REVIEW: '待回访', COMPLETED: '已完成',
  PAID: '已收款', UNPAID: '待收款', OVERDUE: '已逾期', VALID: '有效', WARNING: '临期',
  steam: '蒸汽锅炉', hotwater: '热水锅炉', thermal: '导热油炉', wasteheat: '余热锅炉',
  gas: '燃气', oil: '燃油', electric: '电', biomass: '生物质', coal: '燃煤',
  atmospheric: '常压', low: '低压', medium: '中压', high: '高压',
  REPAIR: '报修', INSTALL: '安装', MAINTAIN: '保养', INSPECT: '年检', ANNUAL: '年检', PERIODIC: '定检',
  manufacture: '制造许可', inspection: '产品监检', energy: '能效报告', install: '安装许可', periodic: '定检记录',
  PUBLISHED: '已发布', NORMAL: '正常'
}
export function label(value?: string) { return value ? labels[value] || value : '—' }
export function money(value: any) {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  return Number.isNaN(n) ? String(value) : '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
export function date(value?: any) { return value ? String(value).slice(0, 10) : '—' }
export function dateTime(value?: any) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
export function sanitizeHtml(html?: string) {
  if (!html) return ''
  const template = document.createElement('template')
  template.innerHTML = html
  template.content.querySelectorAll('script, style, iframe, object, embed').forEach((n) => n.remove())
  template.content.querySelectorAll('*').forEach((node) => {
    for (const attr of Array.from(node.attributes)) {
      if (attr.name.startsWith('on') || (['href', 'src'].includes(attr.name) && /javascript:|data:text\/html/.test(attr.value))) {
        node.removeAttribute(attr.name)
      }
    }
  })
  return template.innerHTML
}
export function statusPill(value?: string) {
  if (!value) return 'gray'
  if (['DEAL', 'CONFIRMED', 'SIGNED', 'COMPLETED', 'PAID', 'VALID', 'PUBLISHED', 'APPROVED', 'PERFORMING'].includes(value)) return 'green'
  if (['LOST', 'EXPIRED', 'REJECTED', 'OVERDUE'].includes(value)) return 'red'
  if (['NEW', 'DRAFT', 'PENDING', 'UNSIGNED', 'UNPAID', 'WARNING', 'SENT', 'BARGAINING'].includes(value)) return 'yellow'
  return 'blue'
}
export function imageList(value?: string) { return (value || '').split(',').filter(Boolean) }
export function firstImage(value?: string) { return imageList(value)[0] || '' }
