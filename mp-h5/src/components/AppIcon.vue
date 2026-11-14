<template>
  <svg class="app-icon" :width="size" :height="size" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path v-for="(d, i) in paths" :key="i" :d="d" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 20 })
const iconPaths: Record<string, string[]> = {
  home: ['M4 10.5 12 4l8 6.5', 'M6 9.5V20h12V9.5', 'M10 20v-5h4v5'],
  doc: ['M7 3h7l4 4v14H7z', 'M14 3v5h5', 'M10 12h5', 'M10 16h5'],
  list: ['M8 6h11', 'M8 12h11', 'M8 18h11', 'M4.5 6h.01', 'M4.5 12h.01', 'M4.5 18h.01'],
  tool: ['M14.5 5.5a4 4 0 0 0-5.2 5.2L4 16l4 4 5.3-5.3a4 4 0 0 0 5.2-5.2l-2.4 2.4-2.1-2.1z'],
  user: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z', 'M5 20c.7-4 3.2-6 7-6s6.3 2 7 6'],
  users: ['M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M3 19c.6-3.5 2.7-5.5 6-5.5s5.4 2 6 5.5', 'M16 7a2.6 2.6 0 1 1 0 5.2', 'M17 13.5c2.3.3 3.7 2 4 5.5'],
  grid: ['M4 4h6v6H4z', 'M14 4h6v6h-6z', 'M4 14h6v6H4z', 'M14 14h6v6h-6z'],
  layers: ['M12 3 3 8l9 5 9-5z', 'M3 12l9 5 9-5', 'M3 16l9 5 9-5'],
  chart: ['M4 20V10', 'M10 20V5', 'M16 20v-7', 'M22 20H2'],
  settings: ['M4 7h10', 'M18 7h2', 'M14 4v6', 'M4 17h3', 'M11 17h9', 'M7 14v6'],
  bell: ['M6 17h12l-1.5-2V10a4.5 4.5 0 0 0-9 0v5z', 'M9 17a3 3 0 0 0 6 0'],
  search: ['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z', 'M16 16l4 4'],
  arrow: ['M9 5l7 7-7 7'],
  plus: ['M12 5v14', 'M5 12h14'],
  package: ['M4 8 12 3l8 5v12l-8 3-8-3z', 'M4 8l8 5 8-5', 'M12 13v7'],
  shield: ['M12 3 5 6v6c0 4 3 7 7 9 4-2 7-5 7-9V6z', 'M9 12l2 2 4-4'],
  file: ['M7 3h7l4 4v14H7z', 'M14 3v5h5'],
  calendar: ['M4 6h16v15H4z', 'M8 3v6', 'M16 3v6', 'M4 10h16'],
  chat: ['M5 5h14v10H9l-4 4z', 'M9 9h6', 'M9 12h4'],
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
  money: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M9 9l3 3 3-3', 'M12 12v5', 'M9 14h6'],
  book: ['M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3z', 'M8 4v13', 'M11 8h5', 'M11 12h5'],
  globe: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M3 12h18', 'M12 3c2.5 2.5 3.8 5.5 3.8 9s-1.3 6.5-3.8 9c-2.5-2.5-3.8-5.5-3.8-9S9.5 5.5 12 3Z'],
  cloud: ['M7 18h10a4 4 0 0 0 0-8 6 6 0 0 0-11.4 1.5A3.5 3.5 0 0 0 7 18Z'],
  alert: ['M12 4 3 20h18z', 'M12 9v5', 'M12 17v.01'],
  refresh: ['M20 11a8 8 0 1 1-2.3-6.6', 'M20 4v7h-7'],
  share: ['M18 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M6 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M18 22a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M8.6 10.5l6.8-4', 'M8.6 13.5l6.8 4'],
  check: ['M5 12l4 4L19 6'],
  close: ['M6 6l12 12', 'M18 6 6 18']
}
const paths = computed(() => iconPaths[props.name] || iconPaths.doc)
</script>

<style scoped>
.app-icon { stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; flex-shrink: 0; }
</style>
