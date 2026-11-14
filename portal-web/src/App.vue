<template>
  <div class="portal-app">
    <PortalHeader />
    <main class="portal-main">
      <router-view />
    </main>
    <PortalFooter />
    <div v-if="ui.toast" class="portal-toast" :class="ui.toast.type">{{ ui.toast.message }}</div>
  </div>
</template>

<script setup lang="ts">
import PortalHeader from '@/components/PortalHeader.vue'
import PortalFooter from '@/components/PortalFooter.vue'
import { uiState } from '@/ui/toast'

const ui = uiState
</script>
