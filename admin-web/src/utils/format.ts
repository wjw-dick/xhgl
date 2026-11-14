export function fmtMoney(value: any): string {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  if (Number.isNaN(n)) return String(value)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtNumber(value: any): string {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  return Number.isNaN(n) ? String(value) : n.toLocaleString('zh-CN')
}

export function fmtDate(value: any): string {
  if (!value) return '—'
  return String(value).slice(0, 10)
}

export function fmtDateTime(value: any): string {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function phoneMask(value?: string): string {
  return value || '—'
}

const labels: Record<string, string> = {
  NEW: '新需求', CONSULTING: '咨询中', INTRODUCING: '介绍中', TALKING: '谈价中', DEAL: '成交', LOST: '流失',
  NORMAL: '正常', DISABLED: '停用', TRIAL: '试用', EXPIRED: '已过期',
  ONLINE: '已上架', OFFLINE: '已下架', OK: '合规', MISSING: '缺资质',
  VALID: '有效', WARNING: '临期', DRAFT: '草稿', PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回',
  SENT: '已发送', BARGAINING: '客户还价', CONFIRMED: '客户已确认', APPROVAL: '审批中', NONE: '无',
  ACCEPTED: '已受理', DISPATCHED: '已派单', PROCESSING: '处理中', WAIT_REVIEW: '待回访', COMPLETED: '已完成',
  UNSIGNED: '未签署', SIGNED: '已签署', PERFORMING: '履约中', DONE: '已完成',
  PAID: '已收款', UNPAID: '未收款', OVERDUE: '逾期', ISSUED: '已开票', VOID: '已作废',
  PUBLISHED: '已发布', ACTIVE: '启用', ONLINE_STATUS: '上线', PLANNED: '待回访', '0': '停用', '1': '启用',
  steam: '蒸汽锅炉', hotwater: '热水锅炉', thermal: '导热油炉', wasteheat: '余热锅炉',
  gas: '燃气', oil: '燃油', electric: '电', biomass: '生物质', coal: '燃煤',
  atmospheric: '常压', low: '低压', medium: '中压', high: '高压',
  manufacture: '制造许可', inspection: '产品监检', energy: '能效报告', install: '安装许可', periodic: '定检记录',
  REPAIR: '报修', INSTALL: '安装', MAINTAIN: '保养', INSPECT: '年检', ANNUAL: '年检', PERIODIC: '定检',
  FOLLOWING: '跟进中', SHIPPED: '已发货', ARRIVED: '已到货', PENDING_DELIVERY: '待发货'
}

export function statusLabel(value?: string): string {
  if (!value) return '—'
  return labels[value] || value
}

export function statusPill(value?: string): string {
  if (!value) return 'gray'
  if (['NEW', 'DRAFT', 'UNPAID', 'PLANNED', 'ACCEPTED', 'PENDING', 'WARNING', 'UNSIGNED', 'TRIAL', 'WARNING'].includes(value)) return 'yellow'
  if (['DISABLED', 'LOST', 'EXPIRED', 'OVERDUE', 'REJECTED', 'VOID', 'MISSING', 'RED'].includes(value)) return 'red'
  if (['DEAL', 'CONFIRMED', 'SIGNED', 'PAID', 'COMPLETED', 'DONE', 'VALID', 'PUBLISHED', 'ONLINE', 'NORMAL', 'OK', 'APPROVED', 'ACTIVE', 'ISSUED'].includes(value)) return 'green'
  if (['CONSULTING', 'INTRODUCING', 'TALKING', 'SENT', 'BARGAINING', 'PROCESSING', 'PERFORMING', 'APPROVAL', 'DISPATCHED', 'WAIT_REVIEW'].includes(value)) return 'blue'
  return 'gray'
}

export function daysUntil(value?: string): number | null {
  if (!value) return null
  const target = new Date(String(value).slice(0, 10) + 'T00:00:00')
  if (Number.isNaN(target.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((target.getTime() - today.getTime()) / 86400000)
}

export function htmlToText(html?: string): string {
  return (html || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
}

export function sanitizeHtml(html?: string): string {
  if (!html) return ''
  const template = document.createElement('template')
  template.innerHTML = html
  template.content.querySelectorAll('script, style, iframe, object, embed').forEach((node) => node.remove())
  template.content.querySelectorAll('*').forEach((node) => {
    for (const attr of Array.from(node.attributes)) {
      if (attr.name.startsWith('on') || ['href', 'src'].includes(attr.name) && /javascript:|data:text\/html/.test(attr.value)) {
        node.removeAttribute(attr.name)
      }
    }
  })
  return template.innerHTML
}
