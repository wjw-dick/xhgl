<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">对象存储配置</h1><p class="page-sub">产品图片、方案 PDF、报价单、合同扫描件、检测报告等统一存入所选存储驱动；切换实时生效。</p></div>
      <el-tag :type="currentActive ? 'success' : 'info'" effect="plain">当前生效：{{ currentDriver || 'LOCAL' }}</el-tag>
    </div>
    <div class="card">
      <h3 class="card-title">选择存储驱动（单选）</h3>
      <div class="driver-grid">
        <div
          v-for="d in drivers"
          :key="d.value"
          class="driver-card"
          :class="{ active: driver === d.value }"
          @click="selectDriver(d.value)"
        >
          <div class="driver-icon"><el-icon><component :is="d.icon" /></el-icon></div>
          <strong>{{ d.label }}</strong>
          <span>{{ d.desc }}</span>
          <el-radio :model-value="driver === d.value" @click.stop="selectDriver(d.value)" />
        </div>
      </div>
    </div>

    <el-collapse-transition>
      <div v-if="driver" class="card driver-form">
        <h3 class="card-title">{{ driverLabel }} 参数</h3>
        <el-alert v-if="existingDriver && existingDriver !== driver" type="warning" :closable="false" show-icon
          title="切换存储驱动属于高风险操作，保存前需二次确认；历史文件地址保持可读，新文件写入新驱动。" style="margin-bottom:14px" />
        <el-form :model="config" label-position="top" class="form-two">
          <el-form-item v-for="field in driverFields" :key="field.key" :label="field.label" :class="{ full: field.full }">
            <el-input
              v-model="config[field.key]"
              :type="field.secret ? 'password' : 'text'"
              :show-password="field.secret"
              :placeholder="field.placeholder"
              @input="tested = false"
            >
              <template #prefix><el-icon><component :is="field.icon || 'Key'" /></el-icon></template>
            </el-input>
          </el-form-item>
        </el-form>
        <div class="test-row">
          <el-button :loading="testing" @click="test"><el-icon><Connection /></el-icon> 连接测试</el-button>
          <span v-if="tested" class="success">✓ 连接测试通过，可以保存并应用</span>
          <span v-else class="muted">请先通过连接测试，保存按钮才会点亮</span>
          <div style="flex:1" />
          <el-button type="primary" :disabled="!tested" :loading="saving" @click="save">保存并应用</el-button>
        </div>
      </div>
    </el-collapse-transition>

    <div class="card">
      <h3 class="card-title">安全说明</h3>
      <div class="notice">
        AK/SK / Secret 密钥由服务端 AES 加密存储，页面仅显示掩码；后端响应不回传明文。连接测试失败时无法保存，防止错误配置影响上传。
      </div>
      <div class="driver-status">
        <span>状态：<strong>{{ storageStatus }}</strong></span>
        <span>最近更新：{{ fmtDateTime(storageUpdatedAt) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { fmtDateTime } from '@/utils/format'

const drivers = [
  { value: 'LOCAL', label: '本地磁盘', icon: 'FolderOpened', desc: '默认开发/单机方案，文件保存在服务器磁盘' },
  { value: 'S3', label: 'AWS S3', icon: 'Cloudy', desc: '亚马逊云对象存储，S3 兼容协议' },
  { value: 'OSS', label: '阿里云 OSS', icon: 'DataBoard', desc: '阿里云对象存储，国内访问稳定' },
  { value: 'COS', label: '腾讯云 COS', icon: 'MostlyCloudy', desc: '腾讯云对象存储，与微信生态协同' },
  { value: 'MINIO', label: 'MinIO', icon: 'Coin', desc: '私有化 S3 兼容存储，Docker 默认方案' }
]
const driver = ref('')
const existingDriver = ref('')
const config = reactive<Record<string, any>>({})
const tested = ref(false)
const testing = ref(false)
const saving = ref(false)
const currentDriver = ref('')
const currentActive = ref(false)
const storageStatus = ref('—')
const storageUpdatedAt = ref('')
const driverLabel = computed(() => drivers.find((d) => d.value === driver.value)?.label || '')
const driverFields = computed(() => {
  switch (driver.value) {
    case 'LOCAL': return [{ key: 'basePath', label: '本地存储目录', placeholder: './uploads', full: true, icon: 'Folder' }]
    case 'S3':
    case 'MINIO': return [
      { key: 'endpoint', label: 'Endpoint（含协议）', placeholder: 'https://s3.amazonaws.com 或 http://minio:9000', full: true, icon: 'Link' },
      { key: 'bucket', label: 'Bucket', placeholder: 'xinhuo-files', icon: 'Box' },
      { key: 'region', label: 'Region', placeholder: 'us-east-1', icon: 'Location' },
      { key: 'accessKey', label: 'Access Key', placeholder: 'AK', secret: true, icon: 'Key' },
      { key: 'secretKey', label: 'Secret Key', placeholder: 'SK（保存后显示掩码）', secret: true, icon: 'Lock' },
      { key: 'publicBase', label: '公网访问前缀', placeholder: 'https://cdn.example.com', full: true, icon: 'Link' }
    ]
    case 'OSS': return [
      { key: 'endpoint', label: 'Endpoint', placeholder: 'https://oss-cn-hangzhou.aliyuncs.com', full: true, icon: 'Link' },
      { key: 'bucket', label: 'Bucket', placeholder: 'xinhuo-files', icon: 'Box' },
      { key: 'accessKeyId', label: 'AccessKeyId', placeholder: 'AK', secret: true, icon: 'Key' },
      { key: 'accessKeySecret', label: 'AccessKeySecret', placeholder: 'SK', secret: true, icon: 'Lock' },
      { key: 'publicBase', label: '公网访问前缀', placeholder: 'https://cdn.example.com', full: true, icon: 'Link' }
    ]
    case 'COS': return [
      { key: 'region', label: 'Region', placeholder: 'ap-guangzhou', icon: 'Location' },
      { key: 'bucket', label: 'Bucket', placeholder: 'xinhuo-1250000000', icon: 'Box' },
      { key: 'secretId', label: 'SecretId', placeholder: 'SecretId', secret: true, icon: 'Key' },
      { key: 'secretKey', label: 'SecretKey', placeholder: 'SecretKey', secret: true, icon: 'Lock' },
      { key: 'publicBase', label: '公网访问前缀', placeholder: 'https://cdn.example.com', full: true, icon: 'Link' }
    ]
    default: return []
  }
})
async function load() {
  const data: any = await http.get('/admin/storage')
  currentDriver.value = data.driver
  existingDriver.value = data.driver
  currentActive.value = data.active === 1
  storageStatus.value = data.status
  storageUpdatedAt.value = data.updatedAt
  driver.value = data.driver
  Object.assign(config, data.config || {})
}
function selectDriver(value: string) {
  if (driver.value === value && Object.keys(config).length > 0) return
  driver.value = value
  tested.value = false
  const existing = config
  Object.keys(existing).forEach((k) => delete existing[k])
  if (value === 'LOCAL') Object.assign(config, { basePath: './uploads' })
  else Object.assign(config, { endpoint: '', bucket: '', region: '', accessKey: '', secretKey: '', publicBase: '' })
}
async function test() {
  testing.value = true
  try {
    await http.post('/admin/storage/test', { driver: driver.value, config })
    tested.value = true
    ElMessage.success('连接测试通过')
  } catch { tested.value = false } finally { testing.value = false }
}
async function save() {
  if (!tested.value) return
  if (existingDriver.value && existingDriver.value !== driver.value) {
    await ElMessageBox.confirm(`确认从 ${existingDriver.value} 切换到 ${driver.value}？历史文件仍可通过原地址访问。`, '切换存储驱动', { type: 'warning' })
  }
  saving.value = true
  try {
    await http.put('/admin/storage', { driver: driver.value, config })
    ElMessage.success('存储配置已保存并实时生效')
    await load()
  } finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.driver-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.driver-card { border: 1px solid var(--xh-line); border-radius: 8px; padding: 18px 14px; background: #fbfdff; cursor: pointer; position: relative; transition: all .15s ease; min-height: 130px; }
.driver-card:hover { border-color: #bcd3f7; transform: translateY(-1px); }
.driver-card.active { border-color: var(--xh-primary); background: #f3f8ff; box-shadow: 0 0 0 2px rgba(31,111,235,.12); }
.driver-icon { width: 40px; height: 40px; border-radius: 8px; background: #eaf2ff; color: var(--xh-primary); display: flex; align-items: center; justify-content: center; font-size: 22px; margin-bottom: 10px; }
.driver-card strong { display: block; font-size: 14px; }
.driver-card span { display: block; color: var(--xh-muted); font-size: 11px; line-height: 1.6; margin-top: 6px; }
.driver-card .el-radio { position: absolute; right: 10px; top: 10px; }
.test-row { display: flex; align-items: center; gap: 12px; border-top: 1px dashed var(--xh-line); padding-top: 14px; }
.driver-form { animation: slideDown .2s ease; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: none; } }
.driver-status { display: flex; gap: 24px; margin-top: 14px; font-size: 12px; color: var(--xh-muted); }
@media (max-width: 1200px) { .driver-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
