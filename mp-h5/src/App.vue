<template>
  <div class="app-shell" :class="{ 'with-tab': route.meta.tab }">
    <router-view />
    <TabBar v-if="route.meta.tab" />
    <div v-if="ui.toast" class="toast" :class="ui.toast.type">{{ ui.toast.message }}</div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import TabBar from '@/components/TabBar.vue'
import { uiState } from '@/ui/toast'

const route = useRoute()
const ui = uiState
</script>
