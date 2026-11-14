import {
  boilerTypeOptions, capacityOptions, certTypeOptions, complianceOptions, fuelTypeOptions,
  normalStatusOptions, onlineOptions, pressureOptions, publishOptions, referralStatusOptions,
  validStatusOptions, visitStatusOptions
} from './options'

export interface CrudOption { label: string; value: any }
export interface CrudField {
  prop: string
  label: string
  type?: 'input' | 'textarea' | 'number' | 'select' | 'date' | 'datetime' | 'richtext' | 'images' | 'file'
  options?: CrudOption[]
  required?: boolean
  placeholder?: string
  full?: boolean
  rows?: number
  category?: string
}
export interface CrudColumn {
  prop: string
  label: string
  width?: number | string
  format?: 'money' | 'date' | 'datetime' | 'status' | 'image' | 'number'
}
export interface CrudFilter {
  prop: string
  label: string
  type?: 'input' | 'select'
  options?: CrudOption[]
}
export interface CrudConfig {
  key: string
  api: string
  title: string
  subtitle: string
  keywordPlaceholder?: string
  columns: CrudColumn[]
  fields: CrudField[]
  filters: CrudFilter[]
  importable?: boolean
  exportable?: boolean
  defaults?: Record<string, any>
}

export const crudConfigs: Record<string, CrudConfig> = {
  products: {
    key: 'products',
    api: 'products',
    title: '锅炉产品档案',
    subtitle: '型号、参数、图片与合规资质统一维护；缺少制造许可/监检/能效的产品不可上架推荐。',
    keywordPlaceholder: '型号 / 产品编码 / 用途',
    columns: [
      { prop: 'images', label: '图片', width: 84, format: 'image' },
      { prop: 'code', label: '产品编码', width: 140 },
      { prop: 'model', label: '型号', width: 150 },
      { prop: 'boilerType', label: '锅炉类型', width: 100, format: 'status' },
      { prop: 'fuelType', label: '燃料', width: 88, format: 'status' },
      { prop: 'capacity', label: '蒸发量/热功率', width: 110 },
      { prop: 'pressureLevel', label: '压力等级', width: 100, format: 'status' },
      { prop: 'guidePrice', label: '指导价', width: 120, format: 'money' },
      { prop: 'onlineStatus', label: '上架', width: 88, format: 'status' },
      { prop: 'complianceStatus', label: '合规', width: 110, format: 'status' }
    ],
    fields: [
      { prop: 'code', label: '产品编码', required: true, placeholder: '如 HLJ-P-STEAM-6' },
      { prop: 'model', label: '产品型号', required: true, placeholder: '如 WNS6-1.25-Q' },
      { prop: 'boilerType', label: '锅炉类型', type: 'select', options: boilerTypeOptions, required: true },
      { prop: 'fuelType', label: '燃料类型', type: 'select', options: fuelTypeOptions, required: true },
      { prop: 'capacity', label: '额定蒸发量/热功率', type: 'select', options: capacityOptions, required: true },
      { prop: 'pressureLevel', label: '压力等级', type: 'select', options: pressureOptions, required: true },
      { prop: 'usageDesc', label: '适用场景', placeholder: '如食品加工蒸汽供应' },
      { prop: 'guidePrice', label: '指导价（元）', type: 'number' },
      { prop: 'onlineStatus', label: '上架状态', type: 'select', options: onlineOptions },
      { prop: 'complianceStatus', label: '合规状态', type: 'select', options: complianceOptions },
      { prop: 'techParams', label: '技术参数', type: 'textarea', rows: 4, placeholder: '热效率、NOx、控制方式等，可用 JSON 或分行文本', full: true },
      { prop: 'certSummary', label: '资质摘要', type: 'textarea', rows: 2, placeholder: '制造许可 / 监检 / 能效报告编号与有效期', full: true },
      { prop: 'images', label: '产品图片（独立整行上传区）', type: 'images', category: 'product', full: true },
      { prop: 'remark', label: '备注', type: 'textarea', rows: 2, full: true }
    ],
    filters: [
      { prop: 'boiler_type', label: '锅炉类型', type: 'select', options: boilerTypeOptions },
      { prop: 'fuel_type', label: '燃料类型', type: 'select', options: fuelTypeOptions },
      { prop: 'pressure_level', label: '压力等级', type: 'select', options: pressureOptions },
      { prop: 'online_status', label: '上架状态', type: 'select', options: onlineOptions }
    ],
    exportable: true,
    importable: true,
    defaults: { onlineStatus: 'ONLINE', complianceStatus: 'OK' }
  },
  certificates: {
    key: 'certificates',
    api: 'certificates',
    title: '资质证书台账',
    subtitle: '制造许可、产品监检、能效报告、安装许可、定检记录统一归档，到期自动红黄灯预警。',
    keywordPlaceholder: '证书编号 / 发证机关 / 备注',
    columns: [
      { prop: 'certType', label: '证书类型', width: 130, format: 'status' },
      { prop: 'certNo', label: '证书编号', width: 220 },
      { prop: 'issuer', label: '发证机关', width: 220 },
      { prop: 'validFrom', label: '有效期起', width: 110, format: 'date' },
      { prop: 'validTo', label: '有效期止', width: 110, format: 'date' },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'certType', label: '证书类型', type: 'select', options: certTypeOptions, required: true },
      { prop: 'certNo', label: '证书编号', required: true },
      { prop: 'issuer', label: '发证机关', full: true },
      { prop: 'validFrom', label: '有效期起', type: 'date' },
      { prop: 'validTo', label: '有效期止', type: 'date', required: true },
      { prop: 'productId', label: '关联产品 ID', type: 'number' },
      { prop: 'customerId', label: '关联客户 ID', type: 'number' },
      { prop: 'equipmentId', label: '关联设备 ID', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', options: validStatusOptions },
      { prop: 'fileUrl', label: '证书扫描件（列表式附件上传区）', type: 'file', category: 'certificate', full: true },
      { prop: 'remark', label: '备注', type: 'textarea', rows: 2, full: true }
    ],
    filters: [
      { prop: 'cert_type', label: '证书类型', type: 'select', options: certTypeOptions },
      { prop: 'status', label: '状态', type: 'select', options: validStatusOptions }
    ],
    exportable: true, importable: true,
    defaults: { status: 'VALID' }
  },
  knowledge: {
    key: 'knowledge',
    api: 'knowledge',
    title: '服务知识库',
    subtitle: '故障处理手册、维保规范与 FAQ，售后技师可快速检索。',
    keywordPlaceholder: '标题 / 关键词 / 正文',
    columns: [
      { prop: 'title', label: '标题', width: 280 },
      { prop: 'category', label: '分类', width: 120 },
      { prop: 'keywords', label: '关键词', width: 220 },
      { prop: 'viewCount', label: '浏览', width: 80, format: 'number' },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'title', label: '标题', required: true, full: true },
      { prop: 'category', label: '分类', placeholder: '故障处理 / 维保 / 选型' },
      { prop: 'keywords', label: '关键词', placeholder: '逗号分隔' },
      { prop: 'status', label: '状态', type: 'select', options: publishOptions },
      { prop: 'content', label: '正文', type: 'richtext', full: true }
    ],
    filters: [
      { prop: 'category', label: '分类' },
      { prop: 'status', label: '状态', type: 'select', options: publishOptions }
    ],
    exportable: true,
    defaults: { status: 'PUBLISHED' }
  },
  cases: {
    key: 'cases',
    api: 'cases',
    title: '成功案例库',
    subtitle: '按行业 / 场景 / 参数 / 效果沉淀案例，用于方案与门户展示。',
    keywordPlaceholder: '标题 / 行业 / 场景',
    columns: [
      { prop: 'title', label: '案例标题', width: 280 },
      { prop: 'industry', label: '行业', width: 120 },
      { prop: 'scene', label: '场景', width: 160 },
      { prop: 'effect', label: '效果', width: 240 },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'title', label: '案例标题', required: true, full: true },
      { prop: 'industry', label: '行业' },
      { prop: 'scene', label: '应用场景' },
      { prop: 'params', label: '设备参数', placeholder: '型号 / 蒸发量 / 压力' },
      { prop: 'effect', label: '运行效果', placeholder: '节能 / 排放 / 稳定性' },
      { prop: 'status', label: '状态', type: 'select', options: publishOptions },
      { prop: 'images', label: '案例图片', type: 'images', category: 'case', full: true },
      { prop: 'content', label: '案例正文', type: 'richtext', full: true }
    ],
    filters: [
      { prop: 'industry', label: '行业' },
      { prop: 'status', label: '状态', type: 'select', options: publishOptions }
    ],
    defaults: { status: 'PUBLISHED' }
  },
  visits: {
    key: 'visits',
    api: 'visits',
    title: '客户回访',
    subtitle: '成交后回访计划、回访记录与满意度沉淀。',
    keywordPlaceholder: '客户名称 / 回访内容',
    columns: [
      { prop: 'customerName', label: '客户', width: 230 },
      { prop: 'planDate', label: '计划日期', width: 110, format: 'date' },
      { prop: 'visitTime', label: '实际回访', width: 140, format: 'datetime' },
      { prop: 'content', label: '回访内容', width: 300 },
      { prop: 'score', label: '评分', width: 70, format: 'number' },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'customerId', label: '客户 ID', type: 'number' },
      { prop: 'customerName', label: '客户名称', required: true },
      { prop: 'contractId', label: '合同 ID', type: 'number' },
      { prop: 'ownerId', label: '回访负责人 ID', type: 'number' },
      { prop: 'planDate', label: '计划回访日期', type: 'date' },
      { prop: 'visitTime', label: '实际回访时间', type: 'datetime' },
      { prop: 'score', label: '满意度评分（1-5）', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', options: visitStatusOptions },
      { prop: 'content', label: '回访记录', type: 'textarea', rows: 4, full: true }
    ],
    filters: [{ prop: 'status', label: '状态', type: 'select', options: visitStatusOptions }],
    defaults: { status: 'PLANNED' }
  },
  referrals: {
    key: 'referrals',
    api: 'referrals',
    title: '转介绍商机',
    subtitle: '老客户转介绍线索登记、跟进与成交奖励。',
    keywordPlaceholder: '被介绍企业 / 联系人 / 需求',
    columns: [
      { prop: 'sourceCustomerName', label: '来源客户', width: 210 },
      { prop: 'targetCompany', label: '被介绍企业', width: 230 },
      { prop: 'contactName', label: '联系人', width: 100 },
      { prop: 'phoneMask', label: '电话', width: 130 },
      { prop: 'demandDesc', label: '需求', width: 260 },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'sourceCustomerId', label: '来源客户 ID', type: 'number' },
      { prop: 'sourceCustomerName', label: '来源客户名称' },
      { prop: 'targetCompany', label: '被介绍企业', required: true },
      { prop: 'contactName', label: '联系人' },
      { prop: 'phoneMask', label: '联系电话' },
      { prop: 'demandDesc', label: '需求描述', type: 'textarea', rows: 3, full: true },
      { prop: 'status', label: '状态', type: 'select', options: referralStatusOptions },
      { prop: 'reward', label: '奖励约定', full: true }
    ],
    filters: [{ prop: 'status', label: '状态', type: 'select', options: referralStatusOptions }],
    defaults: { status: 'NEW' }
  },
  faqs: {
    key: 'faqs',
    api: 'faqs',
    title: 'FAQ 知识库',
    subtitle: '在线咨询 FAQ 初筛；关键词命中后自动答复并沉淀客户档案。',
    keywordPlaceholder: '问题 / 关键词',
    columns: [
      { prop: 'category', label: '分类', width: 110 },
      { prop: 'question', label: '问题', width: 320 },
      { prop: 'keywords', label: '关键词', width: 220 },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'category', label: '分类' },
      { prop: 'keywords', label: '关键词', placeholder: '逗号分隔，用于自动初筛' },
      { prop: 'status', label: '状态', type: 'select', options: normalStatusOptions },
      { prop: 'question', label: '问题', required: true, full: true },
      { prop: 'answer', label: '答复', type: 'textarea', rows: 5, full: true }
    ],
    filters: [
      { prop: 'category', label: '分类' },
      { prop: 'status', label: '状态', type: 'select', options: normalStatusOptions }
    ],
    defaults: { status: 'NORMAL' }
  },
  contractTemplates: {
    key: 'contractTemplates',
    api: 'contract-templates',
    title: '合同模板',
    subtitle: '锅炉销售合同 / 安装调试合同模板，支持变量替换。',
    keywordPlaceholder: '模板名称',
    columns: [
      { prop: 'name', label: '模板名称', width: 260 },
      { prop: 'status', label: '状态', width: 90, format: 'status' }
    ],
    fields: [
      { prop: 'name', label: '模板名称', required: true, full: true },
      { prop: 'status', label: '状态', type: 'select', options: normalStatusOptions },
      { prop: 'content', label: '模板正文（可用 {{customerName}} 等变量）', type: 'richtext', full: true }
    ],
    filters: [{ prop: 'status', label: '状态', type: 'select', options: normalStatusOptions }],
    defaults: { status: 'NORMAL' }
  }
}
