export interface Option { label: string; value: any }

export const boilerTypeOptions: Option[] = [
  { label: '蒸汽锅炉', value: 'steam' },
  { label: '热水锅炉', value: 'hotwater' },
  { label: '导热油炉', value: 'thermal' },
  { label: '余热锅炉', value: 'wasteheat' }
]
export const fuelTypeOptions: Option[] = [
  { label: '燃气', value: 'gas' },
  { label: '燃油', value: 'oil' },
  { label: '电', value: 'electric' },
  { label: '生物质', value: 'biomass' },
  { label: '燃煤', value: 'coal' }
]
export const pressureOptions: Option[] = [
  { label: '常压', value: 'atmospheric' },
  { label: '低压', value: 'low' },
  { label: '中压', value: 'medium' },
  { label: '高压', value: 'high' }
]
export const capacityOptions: Option[] = [
  { label: '1 t/h', value: '1 t/h' },
  { label: '2 t/h', value: '2 t/h' },
  { label: '4 t/h', value: '4 t/h' },
  { label: '6 t/h', value: '6 t/h' },
  { label: '10 t/h', value: '10 t/h' },
  { label: '20 t/h', value: '20 t/h' },
  { label: '1.4 MW', value: '1.4 MW' },
  { label: '2.8 MW', value: '2.8 MW' }
]
export const certTypeOptions: Option[] = [
  { label: '制造许可', value: 'manufacture' },
  { label: '产品监检证明', value: 'inspection' },
  { label: '能效测试报告', value: 'energy' },
  { label: '安装许可', value: 'install' },
  { label: '定期检验记录', value: 'periodic' }
]
export const onlineOptions: Option[] = [
  { label: '已上架', value: 'ONLINE' },
  { label: '已下架', value: 'OFFLINE' }
]
export const complianceOptions: Option[] = [
  { label: '资质齐全', value: 'OK' },
  { label: '缺少资质（禁止推荐）', value: 'MISSING' }
]
export const normalStatusOptions: Option[] = [
  { label: '正常', value: 'NORMAL' },
  { label: '停用', value: 'DISABLED' }
]
export const publishOptions: Option[] = [
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' }
]
export const visitStatusOptions: Option[] = [
  { label: '待回访', value: 'PLANNED' },
  { label: '已完成', value: 'DONE' }
]
export const referralStatusOptions: Option[] = [
  { label: '新线索', value: 'NEW' },
  { label: '跟进中', value: 'FOLLOWING' },
  { label: '已成交', value: 'DEAL' },
  { label: '已流失', value: 'LOST' }
]
export const validStatusOptions: Option[] = [
  { label: '有效', value: 'VALID' },
  { label: '临期', value: 'WARNING' },
  { label: '已过期', value: 'EXPIRED' }
]
