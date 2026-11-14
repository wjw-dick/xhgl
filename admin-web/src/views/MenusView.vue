<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">菜单可视化配置</h1><p class="page-sub">后台导航、小程序 tabBar、H5 门户导航统一管理；保存后前台实时读取，无需发版。</p></div>
      <div class="toolbar" style="margin:0">
        <el-radio-group v-model="target" @change="load">
          <el-radio-button label="ADMIN">后台导航</el-radio-button>
          <el-radio-button label="MP">小程序 tabBar</el-radio-button>
          <el-radio-button label="H5">H5 门户导航</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="addNew"><el-icon><Plus /></el-icon> 新增菜单</el-button>
      </div>
    </div>
    <div class="menu-editor">
      <div class="card tree-panel">
        <div class="card-title">菜单列表 <span class="muted" style="font-size:12px;margin-left:8px">拖拽调整顺序</span></div>
        <div class="menu-list" @dragover.prevent>
          <div
            v-for="(m, index) in list"
            :key="m.id || 'new-' + index"
            class="menu-row"
            :class="{ active: selectedIndex === index }"
            draggable="true"
            @dragstart="dragIndex = index"
            @dragover.prevent
            @drop="drop(index)"
            @click="select(index)"
          >
            <el-icon><component :is="m.icon || 'Menu'" /></el-icon>
            <span class="menu-title">{{ m.title || '未命名菜单' }}</span>
            <span class="muted mono">{{ m.path }}</span>
            <span v-if="m.visible === 0" class="pill gray">隐藏</span>
            <el-button link type="danger" @click.stop="removeLocal(index)">×</el-button>
          </div>
        </div>
        <el-empty v-if="!list.length" description="暂无菜单，点击新增" />
      </div>

      <div class="card property-panel">
        <div class="card-title">属性面板</div>
        <el-form v-if="current" :model="current" label-position="top">
          <el-form-item label="菜单标题"><el-input v-model="current.title" @input="syncPreview" /></el-form-item>
          <el-form-item label="图标（Element Plus 图标名）"><el-input v-model="current.icon" placeholder="如 User / Tickets" /></el-form-item>
          <el-form-item label="页面路径"><el-input v-model="current.path" placeholder="/customers 或 /pages/index/index" /></el-form-item>
          <el-form-item label="菜单类型">
            <el-select v-model="current.menuType" style="width:100%">
              <el-option label="站内页面" value="PAGE" /><el-option label="外链" value="LINK" /><el-option label="小程序" value="MINIPROGRAM" />
            </el-select>
          </el-form-item>
          <el-form-item label="外链 URL"><el-input v-model="current.externalUrl" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="current.sort" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="显示"><el-switch v-model="current.visible" :active-value="1" :inactive-value="0" /></el-form-item>
        </el-form>
        <el-empty v-else description="请选择左侧菜单" />
        <div class="property-actions">
          <el-button type="primary" @click="saveCurrent">保存当前</el-button>
          <el-button @click="saveOrder">保存排序</el-button>
        </div>
      </div>

      <div class="card preview-panel">
        <div class="card-title">实时预览</div>
        <div v-if="target === 'MP'" class="phone-frame">
          <div class="phone-status">鑫火锅炉</div>
          <div class="phone-body">
            <div class="phone-hero">页面预览</div>
            <div class="phone-placeholder" v-for="i in 2" :key="i" />
          </div>
          <div class="phone-tabbar">
            <div v-for="(m, i) in previewItems" :key="i" class="tab-item">
              <el-icon><component :is="m.icon || 'Menu'" /></el-icon>
              <span>{{ m.title }}</span>
            </div>
          </div>
        </div>
        <div v-else-if="target === 'H5'" class="pc-frame">
          <div class="pc-nav">
            <div class="pc-logo">鑫火锅炉</div>
            <div class="pc-links">
              <span v-for="(m, i) in previewItems" :key="i">{{ m.title }}</span>
            </div>
          </div>
          <div class="pc-content">
            <div class="pc-title">工业锅炉全生命周期服务</div>
            <div class="pc-line" /><div class="pc-line short" />
          </div>
        </div>
        <div v-else class="admin-preview">
          <div class="side-preview">
            <div v-for="(m, i) in previewItems" :key="i" class="side-item">
              <el-icon><component :is="m.icon || 'Menu'" /></el-icon><span>{{ m.title }}</span>
            </div>
          </div>
          <div class="side-content"><div class="pc-line" /><div class="pc-line short" /><div class="pc-line" /></div>
        </div>
        <div class="notice" style="margin-top:12px">前台读取接口：<span class="mono">GET /api/public/menus?target={{ target }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { authStore } from '@/stores/auth'

const store = authStore()
const target = ref('ADMIN')
const list = ref<any[]>([])
const selectedIndex = ref(0)
const dragIndex = ref(-1)
const current = computed(() => list.value[selectedIndex.value])
const previewItems = computed(() => list.value.filter((m) => m.visible !== 0).slice(0, 6))
async function load() {
  list.value = await http.get('/admin/menus', { params: { target: target.value } })
  selectedIndex.value = 0
}
function select(index: number) { selectedIndex.value = index }
function addNew() {
  const temp = { id: null, title: '新菜单', icon: 'Menu', path: '/', menuType: 'PAGE', externalUrl: '', sort: list.value.length + 1, visible: 1, target: target.value, parentId: 0 }
  list.value.push(temp)
  selectedIndex.value = list.value.length - 1
}
function removeLocal(index: number) {
  ElMessageBox.confirm('确认移除该菜单？未保存前仅本地生效。', '操作确认', { type: 'warning' }).then(() => {
    list.value.splice(index, 1)
    if (selectedIndex.value >= list.value.length) selectedIndex.value = Math.max(0, list.value.length - 1)
  }).catch(() => {})
}
function drop(index: number) {
  if (dragIndex.value < 0 || dragIndex.value === index) return
  const [moved] = list.value.splice(dragIndex.value, 1)
  list.value.splice(index, 0, moved)
  dragIndex.value = -1
}
function syncPreview() {}
async function saveCurrent() {
  if (!current.value) return
  if (!current.value.title || !current.value.path) return ElMessage.warning('标题与路径必填')
  const payload = { ...current.value, target: target.value, parentId: current.value.parentId || 0 }
  if (payload.id) {
    delete payload.createdAt; delete payload.updatedAt; delete payload.deleted
    const saved: any = await http.put(`/admin/menus/${payload.id}`, payload)
    Object.assign(current.value, saved)
  } else {
    const saved: any = await http.post('/admin/menus', payload)
    list.value[selectedIndex.value] = saved
  }
  ElMessage.success('菜单已保存，前台实时生效')
  load()
}
async function saveOrder() {
  const items = list.value.filter((m) => m.id).map((m, i) => ({ id: m.id, sort: i + 1, parentId: m.parentId || 0 }))
  await http.post('/admin/menus/reorder', items)
  ElMessage.success('排序已保存')
  load()
}
onMounted(load)
</script>

<style scoped>
.menu-editor { display: grid; grid-template-columns: 1.1fr 1fr 1.1fr; gap: 16px; align-items: start; }
.menu-row { display: flex; align-items: center; gap: 10px; padding: 10px 8px; border: 1px solid var(--xh-line); border-radius: 6px; margin-bottom: 6px; cursor: pointer; background: #fff; }
.menu-row.active { border-color: #bcd3f7; background: #f4f8ff; }
.menu-title { font-weight: 600; min-width: 80px; }
.menu-row .mono { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #7b8798; }
.property-actions { display: flex; gap: 8px; margin-top: 12px; }
.phone-frame { width: 260px; margin: 0 auto; border: 8px solid #1a2436; border-radius: 26px; overflow: hidden; background: #f7f9fc; }
.phone-status { background: #fff; text-align: center; font-size: 11px; color: #7b8798; padding: 8px 0; }
.phone-body { min-height: 330px; padding: 10px; background: linear-gradient(180deg, #eaf2ff, #f7f9fc); }
.phone-hero { height: 84px; background: linear-gradient(135deg, #1f6feb, #58a6ff); border-radius: 10px; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; }
.phone-placeholder { height: 72px; background: #fff; border-radius: 10px; margin-top: 8px; }
.phone-tabbar { display: flex; background: #fff; border-top: 1px solid #e6ebf2; padding: 6px 0; }
.tab-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; font-size: 10px; color: #4b5565; }
.pc-frame { border: 1px solid #d7e0eb; border-radius: 8px; overflow: hidden; }
.pc-nav { display: flex; align-items: center; gap: 18px; padding: 10px 14px; background: #101b31; color: #fff; }
.pc-logo { font-size: 12px; font-weight: 700; color: #8ab4f8; }
.pc-links { display: flex; gap: 12px; font-size: 11px; color: #c9d4e7; flex-wrap: wrap; }
.pc-content { padding: 20px; min-height: 260px; background: linear-gradient(180deg, #eaf2ff, #fff); }
.pc-title { font-size: 18px; font-weight: 700; color: #172033; margin-bottom: 16px; }
.pc-line { height: 8px; background: #dbe5f1; border-radius: 4px; margin-bottom: 8px; width: 86%; }
.pc-line.short { width: 58%; }
.admin-preview { display: flex; border: 1px solid #d7e0eb; border-radius: 8px; overflow: hidden; min-height: 300px; }
.side-preview { width: 132px; background: #101b31; padding: 10px; }
.side-item { display: flex; align-items: center; gap: 6px; color: #c9d4e7; font-size: 10px; padding: 7px 6px; border-radius: 4px; }
.side-item:first-child { background: #1f6feb; color: #fff; }
.side-content { flex: 1; padding: 16px; background: #f7f9fc; }
@media (max-width: 1200px) { .menu-editor { grid-template-columns: 1fr; } }
</style>
