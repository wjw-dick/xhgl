<template>
  <div>
    <section class="sub-hero">
      <div class="container">
        <span class="kicker">PRODUCT CENTER</span>
        <h1>工业锅炉产品中心</h1>
        <p>按锅炉类型、燃料与蒸发量精准筛选；所有在售型号均绑定制造许可、监检与能效报告，支持在线核验。</p>
      </div>
    </section>
    <div class="container page-pad">
      <div class="filter-card">
        <div class="filter-group">
          <span class="filter-label">锅炉类型</span>
          <button v-for="o in boilerTypes" :key="o.value" class="chip" :class="{ active: boilerType === o.value }" @click="toggle('boilerType', o.value)">{{ o.label }}</button>
        </div>
        <div class="filter-group">
          <span class="filter-label">燃料类型</span>
          <button v-for="o in fuelTypes" :key="o.value" class="chip" :class="{ active: fuelType === o.value }" @click="toggle('fuelType', o.value)">{{ o.label }}</button>
        </div>
        <div class="filter-group">
          <span class="filter-label">蒸发量/热功率</span>
          <button v-for="o in capacities" :key="o.value" class="chip" :class="{ active: capacity === o.value }" @click="toggle('capacity', o.value)">{{ o.label }}</button>
        </div>
        <div class="filter-search">
          <input v-model="keyword" placeholder="搜索型号、用途或产品编码" @keyup.enter="search" />
          <button class="btn btn-primary btn-sm" @click="search">搜索</button>
          <button class="reset" @click="reset">重置</button>
        </div>
      </div>

      <div class="result-head">
        <span>共 <strong>{{ total }}</strong> 个在售型号</span>
        <span class="muted">按需选型 · 支持非标定制</span>
      </div>
      <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
      <div v-else-if="products.length" class="product-grid">
        <ProductCard v-for="p in products" :key="p.id" :product="p" />
      </div>
      <div v-else class="empty"><h3>未找到匹配产品</h3><p>可清除筛选条件，或提交需求由工程师定制选型。</p><router-link to="/demand" class="btn btn-primary">提交选型需求</router-link></div>
      <div v-if="total > size" class="pager">
        <button :disabled="page <= 1" @click="go(page - 1)">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button :disabled="page >= Math.ceil(total / size)" @click="go(page + 1)">下一页</button>
      </div>
      <div class="selection-band">
        <div>
          <span class="eyebrow">NOT SURE?</span>
          <h3>不确定选型参数？让工程师帮您匹配</h3>
          <p>提供蒸汽/热水用量、燃料类型、现场海拔与排放要求，2 小时内给出选型建议。</p>
        </div>
        <router-link to="/demand" class="btn btn-primary">免费选型 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import ProductCard from '@/components/ProductCard.vue'
import { sessionStore } from '@/stores/session'

const store = sessionStore()
const products = ref<any[]>([]); const total = ref(0); const page = ref(1); const size = ref(12)
const loading = ref(false); const keyword = ref(''); const boilerType = ref(''); const fuelType = ref(''); const capacity = ref('')
const boilerTypes = [{ label: '蒸汽锅炉', value: 'steam' }, { label: '热水锅炉', value: 'hotwater' }, { label: '导热油炉', value: 'thermal' }, { label: '余热锅炉', value: 'wasteheat' }]
const fuelTypes = [{ label: '燃气', value: 'gas' }, { label: '燃油', value: 'oil' }, { label: '电', value: 'electric' }, { label: '生物质', value: 'biomass' }, { label: '燃煤', value: 'coal' }]
const capacities = [{ label: '2 t/h', value: '2' }, { label: '4 t/h', value: '4' }, { label: '6 t/h', value: '6' }, { label: '10 t/h', value: '10' }, { label: '20 t/h', value: '20' }]
function toggle(field: string, value: string) {
  if (field === 'boilerType') boilerType.value = boilerType.value === value ? '' : value
  if (field === 'fuelType') fuelType.value = fuelType.value === value ? '' : value
  if (field === 'capacity') capacity.value = capacity.value === value ? '' : value
  search()
}
async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/public/products', { params: { page: page.value, size: size.value, keyword: keyword.value, boilerType: boilerType.value, fuelType: fuelType.value, capacity: capacity.value } })
    products.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function go(p: number) { page.value = p; load(); window.scrollTo({ top: 200, behavior: 'smooth' }) }
function reset() { keyword.value = ''; boilerType.value = ''; fuelType.value = ''; capacity.value = ''; search() }
onMounted(load)
</script>

<style scoped>
.sub-hero { background: linear-gradient(135deg, #0f172a, #1b2a41); color: #fff; padding: 66px 0 54px; }
.kicker { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
.sub-hero h1 { font-size: 38px; margin: 12px 0 10px; letter-spacing: -1px; }
.sub-hero p { color: #a6b4c7; max-width: 640px; font-size: 14px; line-height: 1.9; margin: 0; }
.filter-card { border: 1px solid var(--line); background: #fff; padding: 22px 24px; margin-bottom: 28px; }
.filter-group { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding: 9px 0; border-bottom: 1px dashed #eeeae2; }
.filter-label { width: 110px; font-size: 11px; color: var(--steel); letter-spacing: .5px; flex: 0 0 110px; }
.chip { border: 1px solid #e0dcd3; background: #faf9f6; padding: 5px 12px; font-size: 12px; border-radius: 2px; color: #4b5563; }
.chip.active { background: var(--ink); border-color: var(--ink); color: #fff; }
.filter-search { display: flex; gap: 10px; align-items: center; padding-top: 14px; flex-wrap: wrap; }
.filter-search input { flex: 1; min-width: 220px; border: 1px solid #d8d4ca; border-radius: 3px; padding: 10px 12px; font-size: 13px; background: #fdfcf9; outline: none; }
.filter-search input:focus { border-color: var(--accent); }
.reset { border: none; background: transparent; font-size: 12px; color: var(--steel); }
.result-head { display: flex; justify-content: space-between; margin-bottom: 18px; font-size: 12px; color: var(--steel); }
.result-head strong { color: var(--accent); font-size: 16px; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 34px; font-size: 12px; color: var(--steel); }
.pager button { border: 1px solid var(--line); background: #fff; padding: 8px 16px; font-size: 12px; border-radius: 3px; }
.pager button:disabled { opacity: .45; cursor: not-allowed; }
.selection-band { margin-top: 54px; background: #101b2e; color: #fff; padding: 34px 36px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.selection-band h3 { margin: 8px 0 8px; font-size: 22px; }
.selection-band p { color: #97a5b9; font-size: 12px; margin: 0; }
.eyebrow { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: 1fr 1fr; } .sub-hero h1 { font-size: 30px; } .selection-band { flex-direction: column; align-items: flex-start; } }
@media (max-width: 600px) { .product-grid { grid-template-columns: 1fr; } .filter-label { width: 100%; flex-basis: 100%; } }
</style>
