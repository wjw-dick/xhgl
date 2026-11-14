import { reactive } from 'vue'

let timer: number | undefined
export const uiState = reactive({
  toast: null as { message: string; type: string } | null,
  pushToast(message: string, type = 'info') {
    this.toast = { message, type }
    window.clearTimeout(timer)
    timer = window.setTimeout(() => { this.toast = null }, 3000)
  },
  clearToast() { this.toast = null }
})
export function toast(message: string, type = 'info') { uiState.pushToast(message, type) }
