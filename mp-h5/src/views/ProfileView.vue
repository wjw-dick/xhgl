<template>
  <div class="page">
    <section class="hero profile-hero">
      <div class="profile-head">
        <div class="avatar">{{ (store.user?.name || 'U').slice(0, 1) }}</div>
        <div>
          <span class="eyebrow">ACCOUNT · 客户中心</span>
          <h1>{{ store.user?.name || store.customer?.name }}</h1>
          <p>用户 · {{ store.customer?.name || store.companyName }}</p>
        </div>
      </div>
    </section>
    <div class="page-body">
      <div class="card">
        <div class="card-title">
          <h2>企业档案</h2>
          <div class="title-actions">
            <button class="edit-link" @click="openEdit">编辑</button>
            <router-link to="/consult">在线咨询</router-link>
          </div>
        </div>
        <div class="list-row"><div class="main"><strong>客户编号</strong><span>{{ profile?.customer?.customerNo }}</span></div></div>
        <div class="list-row"><div class="main"><strong>所属行业</strong><span>{{ profile?.customer?.industry || '未填写' }}{{ profile?.customer?.scale ? ' · ' + profile.customer.scale : '' }}</span></div></div>
        <div class="list-row"><div class="main"><strong>地区</strong><span>{{ profile?.customer?.region || '未填写' }}</span></div></div>
        <div v-for="c in profile?.contacts || []" :key="c.id" class="list-row"><div class="main"><strong>{{ c.name }}</strong><span>{{ c.title }} · {{ c.phoneMask }}</span></div></div>
      </div>
      <div class="card">
        <div class="card-title"><h2>我的业务</h2></div>
        <router-link to="/demands" class="menu-row"><span class="menu-label"><AppIcon name="doc" :size="17" />我的需求</span><em>›</em></router-link>
        <router-link to="/solutions" class="menu-row"><span class="menu-label"><AppIcon name="layers" :size="17" />我的方案</span><em>›</em></router-link>
        <router-link to="/quotes" class="menu-row"><span class="menu-label"><AppIcon name="money" :size="17" />我的报价</span><em>›</em></router-link>
        <router-link to="/contracts" class="menu-row"><span class="menu-label"><AppIcon name="file" :size="17" />我的合同</span><em>›</em></router-link>
        <router-link to="/service" class="menu-row"><span class="menu-label"><AppIcon name="tool" :size="17" />售后服务与维保</span><em>›</em></router-link>
      </div>
      <div class="card">
        <div class="card-title"><h2>设置与帮助</h2></div>
        <router-link to="/agreement" class="menu-row"><span class="menu-label"><AppIcon name="shield" :size="17" />用户协议与隐私政策</span><em>›</em></router-link>
        <div class="menu-row"><span class="menu-label"><AppIcon name="globe" :size="17" />语言</span><em>简体中文</em></div>
        <div class="menu-row"><span class="menu-label"><AppIcon name="chat" :size="17" />客服热线</span><em>400-800-6688</em></div>
        <div class="menu-row"><span class="menu-label"><AppIcon name="settings" :size="17" />版本</span><em>v1.0.0</em></div>
      </div>
      <button class="btn btn-ghost btn-block" style="margin-top:14px" @click="logout">退出登录</button>
      <div class="footer-tip">鑫火锅炉 · 客户仅本人/本企业数据可见</div>
    </div>
    <div v-if="editOpen" class="modal-mask" @click.self="editOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <h3>编辑企业档案</h3>
          <button class="modal-close" @click="editOpen = false">×</button>
        </div>
        <div class="field"><label>所属行业</label><input v-model="editIndustry" maxlength="64" placeholder="如：食品加工" /></div>
        <div class="field" style="margin-top:12px"><label>地区</label><input v-model="editRegion" maxlength="64" placeholder="如：黑龙江哈尔滨" /></div>
        <div class="notice blue" style="margin-top:12px">企业名称与联系人如需变更，请联系业务人员；所属行业和地区可自行维护。</div>
        <div class="edit-actions">
          <button class="btn btn-ghost" @click="editOpen = false">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="saveEdit">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import AppIcon from '@/components/AppIcon.vue'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'

const store = sessionStore(); const router = useRouter(); const profile = ref<any>(null)
const editOpen = ref(false); const saving = ref(false)
const editIndustry = ref(''); const editRegion = ref('')
function openEdit() {
  editIndustry.value = profile.value?.customer?.industry || ''
  editRegion.value = profile.value?.customer?.region || ''
  editOpen.value = true
}
async function saveEdit() {
  if (saving.value) return
  saving.value = true
  try {
    const data: any = await http.put('/customer/profile', {
      industry: editIndustry.value.trim(),
      region: editRegion.value.trim()
    })
    profile.value = data
    store.updateCustomerFields({
      industry: data?.customer?.industry ?? null,
      region: data?.customer?.region ?? null
    })
    toast('企业档案已更新', 'success')
    editOpen.value = false
  } finally { saving.value = false }
}
function logout() { store.logout(); router.replace('/login') }
onMounted(async () => {
  try { profile.value = await http.get('/customer/profile') } catch {}
})
</script>
<style scoped>
.profile-hero { padding-bottom: 26px; }
.profile-head { display: flex; align-items: center; gap: 12px; }
.avatar { width: 48px; height: 48px; border-radius: 11px; background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.18); display: flex; align-items: center; justify-content: center; font-size: 18px; color: #fff; }
.profile-head .eyebrow { font-size: 8px; }
.profile-head h1 { font-size: 20px; margin: 3px 0 2px; }
.profile-head p { color: #b7bac1; font-size: 10px; margin: 0; }
.menu-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0ece4; font-size: 12px; }
.menu-row:last-child { border-bottom: none; }
.menu-label { display: flex; align-items: center; gap: 10px; color: var(--ink-2); }
.menu-row em { font-style: normal; color: var(--steel-2); font-size: 11px; }
.title-actions { display: flex; align-items: center; gap: 12px; }
.edit-link { border: none; background: transparent; color: var(--accent); font-size: 10px; padding: 0; }
.edit-actions { display: flex; gap: 10px; margin-top: 18px; }
.edit-actions .btn { flex: 1; }
</style>
