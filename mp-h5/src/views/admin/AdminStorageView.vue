<template>
  <div class="page">
    <div class="topbar"><button class="back-btn" @click="router.push('/admin/business')">‹</button><h1>对象存储</h1><span class="right">{{ currentDriver || 'LOCAL' }}</span></div>
    <div class="page-body">
      <div class="card">
        <div class="card-title"><h2>选择存储驱动</h2><span class="pill">{{ currentDriver || '—' }}</span></div>
        <div class="driver-grid">
          <button v-for="d in drivers" :key="d.value" class="driver-card" :class="{ active: driver === d.value }" @click="selectDriver(d.value)">
            <strong>{{ d.label }}</strong><span>{{ d.desc }}</span>
          </button>
        </div>
      </div>
      <div v-if="driver" class="card">
        <div class="card-title"><h2>{{ driverLabel }} 参数</h2></div>
        <div class="form-grid">
          <div v-for="f in driverFields" :key="f.key" class="field" :class="{ full: f.full }">
            <label>{{ f.label }}</label>
            <input v-model="config[f.key]" :type="f.secret ? 'password' : 'text'" :placeholder="f.placeholder" @input="tested = false" />
          </div>
        </div>
        <div class="test-row">
          <button class="btn btn-ghost btn-sm" :disabled="testing" @click="test">{{ testing ? '测试中…' : '连接测试' }}</button>
          <span v-if="tested" style="color:var(--green);font-size:11px">✓ 连接测试通过</span>
          <span v-else style="color:var(--steel-2);font-size:11px">请先通过测试</span>
        </div>
        <button class="btn btn-primary btn-sm" style="margin-top:10px" :disabled="!tested || saving" @click="save">{{ saving ? '保存中…' : '保存并应用' }}</button>
      </div>
      <div class="notice" style="margin-top:12px">AK/SK/密钥由服务端加密存储，页面仅显示掩码；连接测试失败时无法保存。</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { toast } from '@/ui/toast'

const router = useRouter()
const drivers = [
  { value: 'LOCAL', label: '本地磁盘', desc: '服务器磁盘存储' },
  { value: 'S3', label: 'AWS S3', desc: 'S3 兼容对象存储' },
  { value: 'OSS', label: '阿里云 OSS', desc: '阿里云对象存储' },
  { value: 'COS', label: '腾讯云 COS', desc: '腾讯云对象存储' },
  { value: 'MINIO', label: 'MinIO', desc: '私有化 S3 存储' }
]
const driver = ref(''); const existingDriver = ref(''); const currentDriver = ref('')
const config = reactive<Record<string, any>>({}); const tested = ref(false); const testing = ref(false); const saving = ref(false)
const driverLabel = computed(() => drivers.find((d) => d.value === driver.value)?.label || '')
const driverFields = computed(() => {
  switch (driver.value) {
    case 'LOCAL': return [{ key: 'basePath', label: '本地存储目录', placeholder: './uploads', full: true }]
    case 'S3':
    case 'MINIO': return [
      { key: 'endpoint', label: 'Endpoint（含协议）', placeholder: 'https://s3.amazonaws.com', full: true },
      { key: 'bucket', label: 'Bucket', placeholder: 'xinhuo-files' },
      { key: 'region', label: 'Region', placeholder: 'us-east-1' },
      { key: 'accessKey', label: 'Access Key', placeholder: 'AK', secret: true },
      { key: 'secretKey', label: 'Secret Key', placeholder: 'SK', secret: true },
      { key: 'publicBase', label: '公网访问前缀', placeholder: 'https://cdn.example.com', full: true }
    ]
    case 'OSS': return [
      { key: 'endpoint', label: 'Endpoint', placeholder: 'https://oss-cn-hangzhou.aliyuncs.com', full: true },
      { key: 'bucket', label: 'Bucket', placeholder: 'xinhuo-files' },
      { key: 'accessKeyId', label: 'AccessKeyId', placeholder: 'AK', secret: true },
      { key: 'accessKeySecret', label: 'AccessKeySecret', placeholder: 'SK', secret: true },
      { key: 'publicBase', label: '公网访问前缀', placeholder: 'https://cdn.example.com', full: true }
    ]
    case 'COS': return [
      { key: 'region', label: 'Region', placeholder: 'ap-guangzhou' },
      { key: 'bucket', label: 'Bucket', placeholder: 'xinhuo-1250000000' },
      { key: 'secretId', label: 'SecretId', placeholder: 'SecretId', secret: true },
      { key: 'secretKey', label: 'SecretKey', placeholder: 'SecretKey', secret: true },
      { key: 'publicBase', label: '公网访问前缀', placeholder: 'https://cdn.example.com', full: true }
    ]
    default: return []
  }
})
async function load() {
  const data: any = await http.get('/admin/storage')
  currentDriver.value = data.driver; existingDriver.value = data.driver; driver.value = data.driver
  Object.keys(config).forEach((k) => delete config[k]); Object.assign(config, data.config || {})
}
function selectDriver(value: string) {
  if (driver.value === value && Object.keys(config).length) return
  driver.value = value; tested.value = false
  Object.keys(config).forEach((k) => delete config[k])
  if (value === 'LOCAL') Object.assign(config, { basePath: './uploads' })
  else Object.assign(config, { endpoint: '', bucket: '', region: '', accessKey: '', secretKey: '', publicBase: '' })
}
async function test() { testing.value = true; try { await http.post('/admin/storage/test', { driver: driver.value, config }); tested.value = true; toast('连接测试通过', 'success') } catch { tested.value = false } finally { testing.value = false } }
async function save() {
  if (!tested.value) return
  if (existingDriver.value && existingDriver.value !== driver.value && !confirm(`确认从 ${existingDriver.value} 切换到 ${driver.value}？历史文件仍可访问。`)) return
  saving.value = true
  try { await http.put('/admin/storage', { driver: driver.value, config }); toast('存储配置已保存', 'success'); await load() } finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.driver-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.driver-card { border: 1px solid var(--line); border-radius: 9px; padding: 10px; background: #fbfdff; text-align: left; }
.driver-card.active { border-color: var(--accent); background: var(--accent-soft); }
.driver-card strong { display: block; font-size: 12px; }
.driver-card span { display: block; font-size: 9px; color: var(--steel-2); margin-top: 3px; }
.test-row { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
</style>
