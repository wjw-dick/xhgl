<template>
  <div ref="root" :style="{ width: '100%', height }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(defineProps<{ option: any; height?: string }>(), { height: '300px' })
const root = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!root.value) return
  if (!chart) chart = echarts.init(root.value)
  chart.setOption(props.option || {}, true)
}
function resize() {
  chart?.resize()
}
watch(() => props.option, render, { deep: true })
onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>
