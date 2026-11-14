<template>
  <div class="container page-pad" style="max-width:680px">
    <div class="card" style="text-align:center;padding:44px">
      <div v-if="loading" class="spinner" />
      <h2>{{ loading ? '正在完成微信授权…' : error ? '微信授权失败' : '微信授权成功' }}</h2>
      <p class="muted">{{ error || '正在将 UnionID 与客户档案绑定，并跳转客户中心。' }}</p>
      <router-link v-if="!loading && error" to="/login" class="btn btn-primary">返回账号登录</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'

const route = useRoute(); const router = useRouter(); const store = sessionStore()
const loading = ref(true); const error = ref('')
onMounted(async () => {
  const code = route.query.code as string
  if (!code) { error.value = '缺少微信授权 code'; loading.value = false; return }
  try {
    const data: any = await http.post('/public/auth/wechat-oauth', { code })
    store.setLoginData(data)
    router.replace('/account')
  } catch (e: any) {
    error.value = e?.message || '微信授权失败，请返回账号登录'
    loading.value = false
  }
})
</script>
