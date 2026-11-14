<template>
  <div class="page">
    <div class="page-head">
      <div><h1 class="page-title">数据字典</h1><p class="page-sub">引用系统标准字典；可新增企业扩展项，不可修改标准项。</p></div>
      <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增自定义项</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-tabs v-model="tab">
        <el-tab-pane label="全局标准字典" name="global">
          <div style="padding:0 16px 16px">
            <el-select v-model="activeType" clearable placeholder="字典类型" style="width:260px;margin-bottom:10px">
              <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-table :data="filteredGlobal" stripe size="small">
              <el-table-column prop="dictType" label="类型" width="130"><template #default="{ row }">{{ typeLabel(row.dictType) }}</template></el-table-column>
              <el-table-column prop="dictKey" label="键" width="140" />
              <el-table-column prop="dictValue" label="值" width="160" />
              <el-table-column prop="sort" label="排序" width="70" />
              <el-table-column prop="status" label="状态" width="80"><template #default="{ row }"><span :class="['pill', row.status === 'NORMAL' ? 'green' : 'gray']">{{ statusLabel(row.status) }}</span></template></el-table-column>
              <el-table-column prop="remark" label="备注" min-width="200" />
            </el-table>
            <el-alert type="info" :closable="false" show-icon style="margin-top:12px" title="标准字典由系统统一维护，不可编辑或删除。" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="企业自定义扩展" name="custom">
          <div style="padding:0 16px 16px">
            <el-table :data="customItems" stripe size="small">
              <el-table-column prop="dictType" label="类型" width="140" />
              <el-table-column prop="dictKey" label="键" width="150" />
              <el-table-column prop="dictValue" label="值" width="180" />
              <el-table-column prop="sort" label="排序" width="70" />
              <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
            </el-table>
            <el-empty v-if="!customItems.length" description="暂无自定义扩展项">
              <el-button type="primary" @click="openCreate">新增扩展项</el-button>
            </el-empty>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="dialogVisible" title="新增自定义字典项" width="560px">
      <el-form :model="form" label-position="top">
        <el-form-item label="字典类型" required>
          <el-select v-model="form.dictType" filterable allow-create style="width:100%">
            <el-option v-for="t in types" :key="t.value" :label="`${t.label}（${t.value}）`" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="键" required><el-input v-model="form.dictKey" /></el-form-item>
        <el-form-item label="显示值" required><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { statusLabel } from '@/utils/format'

const types = [
  { label: '需求阶段', value: 'demand_status' }, { label: '锅炉系列', value: 'boiler_series' },
  { label: '工单类型', value: 'ticket_type' }, { label: '维保周期', value: 'maintenance_project' }
]
const tab = ref('global'); const activeType = ref('')
const globalItems = ref<any[]>([]); const customItems = ref<any[]>([])
const dialogVisible = ref(false); const form = reactive<any>({ sort: 99 })
const filteredGlobal = computed(() => globalItems.value.filter((x) => !activeType.value || x.dictType === activeType.value))
function typeLabel(type: string) { return types.find((t) => t.value === type)?.label || type }
async function load() { const d: any = await http.get('/admin/settings/dicts'); globalItems.value = d.global; customItems.value = d.custom }
function openCreate() { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { dictType: activeType.value || 'boiler_series', sort: 99 }); dialogVisible.value = true }
async function save() {
  if (!form.dictType || !form.dictKey || !form.dictValue) return ElMessage.warning('类型、键、值必填')
  await http.post('/admin/settings/dicts', form)
  ElMessage.success('扩展项已新增'); dialogVisible.value = false; load()
}
async function remove(row: any) { await ElMessageBox.confirm('确认删除该自定义字典项？', '危险操作', { type: 'warning' }); await http.delete(`/admin/settings/dicts/${row.id}`); ElMessage.success('已删除'); load() }
onMounted(load)
</script>
