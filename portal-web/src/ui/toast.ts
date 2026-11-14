import { reactive } from 'vue'

export const uiState = reactive({
  toast: null as { message: string; type: string } | null,
  pushToast(message: string, type = 'info') {
    this.toast = { message, type }
    window.setTimeout(() => { this.toast = null }, 3600)
  },
  clearToast() {
    this.toast = null
  }
})

export function toast(message: string, type = 'info') {
  uiState.pushToast(message, type)
}
