<template>
  <ResourceView v-if="config" :config="config" />
  <div v-else class="page"><el-empty description="页面配置不存在" /></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { crudConfigs } from '@/crud/configs'
import ResourceView from './ResourceView.vue'

const route = useRoute()
const config = computed(() => crudConfigs[(route.meta.configKey as string) || ''] as any)
</script>
