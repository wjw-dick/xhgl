<template>
  <div>
    <section class="account-hero">
      <div class="container account-head">
        <div>
          <span class="kicker">CUSTOMER CENTER</span>
          <h1>{{ store.user?.name || overview?.customer?.name || '用户中心' }}</h1>
          <p><span class="mono">{{ store.customer?.customerNo || '—' }}</span> · {{ store.customer?.name || store.companyName }} · 数据仅本人/本企业可见</p>
        </div>
        <div class="account-actions">
          <router-link to="/demand" class="btn btn-primary">提交新需求</router-link>
          <button class="btn btn-light" @click="logout">退出登录</button>
        </div>
      </div>
    </section>
    <div class="container account-body">
      <aside class="account-nav">
        <button v-for="s in sections" :key="s.key" :class="{ active: tab === s.key }" @click="switchTab(s.key)">
          <span>{{ s.title }}</span>
          <em v-if="s.key === 'notifications' && unread">{{ unread }}</em>
        </button>
        <div class="nav-foot">
          <span>服务热线</span>
          <strong>400-800-6688</strong>
          <router-link to="/consult">在线咨询</router-link>
        </div>
      </aside>

      <main class="account-main">
        <div v-if="loading" class="loading"><div class="spinner" />加载中…</div>
        <template v-else>
          <section v-if="tab === 'overview'">
            <div class="account-stats">
              <div><span>我的需求</span><strong>{{ overview?.demandCount || 0 }}</strong><router-link to="/demand">提交需求 →</router-link></div>
              <div><span>方案</span><strong>{{ overview?.solutionCount || 0 }}</strong><button @click="switchTab('solutions')">查看方案 →</button></div>
              <div><span>待确认报价</span><strong>{{ unconfirmedQuotes }}</strong><button @click="switchTab('quotes')">查看报价 →</button></div>
              <div><span>售后工单</span><strong>{{ overview?.ticketCount || 0 }}</strong><button @click="switchTab('service')">查看进度 →</button></div>
            </div>
            <div class="overview-grid">
              <div class="panel">
                <h2>最近需求</h2>
                <div v-for="d in overview?.recentDemands || []" :key="d.id" class="list-row">
                  <div><strong class="mono">{{ d.demandNo }}</strong><span>{{ label(d.boilerType) }} · {{ d.capacity }} · {{ label(d.fuelType) }}</span></div>
                  <span :class="['pill', statusPill(d.status)]">{{ label(d.status) }}</span>
                </div>
                <div v-if="!overview?.recentDemands?.length" class="muted">暂无需求</div>
              </div>
              <div class="panel">
                <h2>维保/定检提醒</h2>
                <div v-for="m in overview?.maintenanceWarnings || []" :key="m.id" class="list-row">
                  <div><strong>{{ m.equipmentNo }} · {{ label(m.project) }}</strong><span>到期 {{ date(m.nextDate) }}</span></div>
                  <span :class="['pill', daysLeft(m.nextDate) < 0 ? 'red' : 'yellow']">{{ daysLeft(m.nextDate) < 0 ? '已逾期' : '剩余' + daysLeft(m.nextDate) + '天' }}</span>
                </div>
                <div v-if="!overview?.maintenanceWarnings?.length" class="muted">暂无临期维保计划</div>
              </div>
            </div>
            <div class="panel">
              <h2>最近工单</h2>
              <div v-for="t in overview?.recentTickets || []" :key="t.id" class="list-row">
                <div><strong class="mono">{{ t.ticketNo }}</strong><span>{{ t.description }}</span></div>
                <span :class="['pill', statusPill(t.status)]">{{ label(t.status) }}</span>
              </div>
              <div v-if="!overview?.recentTickets?.length" class="muted">暂无工单</div>
            </div>
          </section>

          <section v-else-if="tab === 'demands'">
            <div class="section-title"><h2>我的采购需求</h2><router-link to="/demand" class="btn btn-primary btn-sm">提交需求</router-link></div>
            <div class="table-wrap">
              <table class="portal-table">
                <thead><tr><th>需求编号</th><th>选型</th><th>地区</th><th>提交时间</th><th>阶段</th></tr></thead>
                <tbody><tr v-for="d in demands" :key="d.id">
                  <td class="mono">{{ d.demandNo }}</td><td>{{ label(d.boilerType) }} / {{ d.capacity }} / {{ label(d.fuelType) }}</td>
                  <td>{{ d.region }}</td><td>{{ date(d.createdAt) }}</td><td><span :class="['pill', statusPill(d.status)]">{{ label(d.status) }}</span></td>
                </tr></tbody>
              </table>
              <div v-if="!demands.length" class="empty"><h3>暂无需求</h3><router-link to="/demand" class="btn btn-primary">立即提交</router-link></div>
            </div>
          </section>

          <section v-else-if="tab === 'solutions'">
            <div class="section-title"><h2>我的方案</h2></div>
            <div class="solution-grid">
              <div v-for="s in solutions" :key="s.id" class="solution-card">
                <div class="solution-head"><span class="mono">{{ s.solutionNo }}</span><span :class="['pill', statusPill(s.status)]">{{ label(s.status) }}</span></div>
                <h3>{{ s.name }}</h3>
                <p>{{ s.productName }} · 版本 {{ s.version }} · 浏览 {{ s.viewCount }}</p>
                <div class="solution-actions">
                  <button class="btn btn-ghost btn-sm" @click="openSolution(s)">查看方案</button>
                  <button v-if="s.status !== 'CONFIRMED'" class="btn btn-primary btn-sm" @click="confirmSolution(s)">确认方案</button>
                  <span v-else class="pill green">已确认</span>
                </div>
              </div>
              <div v-if="!solutions.length" class="empty"><h3>暂无方案</h3><p>业务人员推送后会出现在这里。</p></div>
            </div>
          </section>

          <section v-else-if="tab === 'quotes'">
            <div class="section-title"><h2>我的报价</h2></div>
            <div class="table-wrap">
              <table class="portal-table">
                <thead><tr><th>报价单号</th><th>产品</th><th>版本</th><th>含税总价</th><th>有效期</th><th>状态</th><th>操作</th></tr></thead>
                <tbody><tr v-for="q in quotes" :key="q.id">
                  <td class="mono">{{ q.quoteNo }}</td><td>{{ q.productName }}</td><td>v{{ q.version }}</td>
                  <td><strong>{{ money(q.totalAmount) }}</strong></td><td>{{ date(q.validUntil) }}</td>
                  <td><span :class="['pill', statusPill(q.status)]">{{ label(q.status) }}</span></td>
                  <td><button class="link-btn" @click="openQuote(q)">详情</button></td>
                </tr></tbody>
              </table>
              <div v-if="!quotes.length" class="empty"><h3>暂无报价</h3></div>
            </div>
          </section>

          <section v-else-if="tab === 'contracts'">
            <div class="section-title"><h2>我的合同</h2></div>
            <div class="table-wrap">
              <table class="portal-table">
                <thead><tr><th>合同编号</th><th>合同名称</th><th>金额</th><th>签署</th><th>履约</th><th>操作</th></tr></thead>
                <tbody><tr v-for="c in contracts" :key="c.id">
                  <td class="mono">{{ c.contractNo }}</td><td>{{ c.title }}</td><td><strong>{{ money(c.amount) }}</strong></td>
                  <td><span :class="['pill', statusPill(c.signStatus)]">{{ label(c.signStatus) }}</span></td>
                  <td><span :class="['pill', statusPill(c.status)]">{{ label(c.status) }}</span></td>
                  <td><button class="link-btn" @click="openContract(c)">查看进度</button></td>
                </tr></tbody>
              </table>
              <div v-if="!contracts.length" class="empty"><h3>暂无合同</h3></div>
            </div>
          </section>

          <section v-else-if="tab === 'service'">
            <div class="section-title"><h2>售后服务</h2><button class="btn btn-primary btn-sm" @click="openTicketForm">在线报修</button></div>
            <div class="table-wrap">
              <table class="portal-table">
                <thead><tr><th>工单号</th><th>类型</th><th>设备</th><th>描述</th><th>技师</th><th>状态</th><th>操作</th></tr></thead>
                <tbody><tr v-for="t in tickets" :key="t.id">
                  <td class="mono">{{ t.ticketNo }}</td><td>{{ label(t.type) }}</td><td>{{ t.equipmentNo || '—' }}</td>
                  <td>{{ t.description }}</td><td>{{ t.technicianName || '待派单' }}</td>
                  <td><span :class="['pill', statusPill(t.status)]">{{ label(t.status) }}</span></td>
                  <td><button class="link-btn" @click="openTicket(t)">详情</button></td>
                </tr></tbody>
              </table>
              <div v-if="!tickets.length" class="empty"><h3>暂无工单</h3><p>如需报修，请点击上方「在线报修」。</p></div>
            </div>
            <div class="panel" style="margin-top:18px">
              <h2>维保 / 年检计划</h2>
              <div v-for="m in maintenance" :key="m.id" class="list-row">
                <div><strong>{{ m.equipmentNo }} · {{ label(m.project) }}</strong><span>下次到期 {{ date(m.nextDate) }} · 周期 {{ m.cycleMonths }} 个月</span></div>
                <span :class="['pill', daysLeft(m.nextDate) < 0 ? 'red' : daysLeft(m.nextDate) <= 30 ? 'yellow' : 'green']">{{ daysLeft(m.nextDate) < 0 ? '已逾期' : '剩余' + daysLeft(m.nextDate) + '天' }}</span>
              </div>
              <div v-if="!maintenance.length" class="muted">暂无维保计划（设备验收后自动生成）</div>
            </div>
          </section>

          <section v-else-if="tab === 'notifications'">
            <div class="section-title"><h2>消息通知</h2><button class="btn btn-ghost btn-sm" @click="readAll">全部已读</button></div>
            <div class="notice-list">
              <div v-for="n in notifications" :key="n.id" class="notice-row" :class="{ unread: n.readFlag === 0 }">
                <div><strong>{{ n.title }}</strong><p>{{ n.content }}</p><span>{{ dateTime(n.createdAt) }}</span></div>
                <button v-if="n.readFlag === 0" class="link-btn" @click="readNotification(n)">标记已读</button>
              </div>
              <div v-if="!notifications.length" class="empty"><h3>暂无消息</h3></div>
            </div>
          </section>

          <section v-else-if="tab === 'profile'">
            <div class="section-title">
              <h2>企业档案</h2>
              <button class="btn btn-ghost btn-sm" @click="openProfileEdit">编辑资料</button>
            </div>
            <div class="profile-grid">
              <div class="panel">
                <h3>企业与联系人</h3>
                <div class="spec"><span>企业名称</span><strong>{{ profileData?.customer?.name }}</strong></div>
                <div class="spec"><span>客户编号</span><strong class="mono">{{ profileData?.customer?.customerNo }}</strong></div>
                <div class="spec"><span>行业 / 规模</span><strong>{{ profileData?.customer?.industry || '未填写' }} / {{ profileData?.customer?.scale || '未填写' }}</strong></div>
                <div class="spec"><span>地区</span><strong>{{ profileData?.customer?.region || '未填写' }}</strong></div>
                <div v-for="c in profileData?.contacts || []" :key="c.id" class="contact-row"><strong>{{ c.name }}</strong><span>{{ c.title }} · {{ c.phoneMask }}</span></div>
              </div>
              <div class="panel">
                <h3>在役设备</h3>
                <div v-for="e in equipment" :key="e.id" class="equipment-row">
                  <div><strong>{{ e.equipmentNo }}</strong><span>{{ e.model }} · {{ e.capacity }}</span><span>{{ e.installAddr }}</span></div>
                  <span class="pill" :class="daysLeft(e.warrantyEnd) > 0 ? 'green' : 'red'">质保至 {{ date(e.warrantyEnd) }}</span>
                </div>
                <div v-if="!equipment.length" class="muted">暂无设备台账</div>
              </div>
            </div>
            <div class="notice blue" style="margin-top:18px">所属行业、地区可自行修改；企业名称、规模、联系人手机号或账号如需变更，请联系业务人员提交工单，平台将记录审计。</div>
          </section>
        </template>
      </main>
    </div>

    <div v-if="modal" class="modal-mask" @click.self="closeModal">
      <div class="modal-card" :class="{ wide: modal.type === 'quote' || modal.type === 'contract' || modal.type === 'ticket' }">
        <div class="modal-head"><h3>{{ modal.title }}</h3><button class="modal-close" @click="closeModal">×</button></div>
        <div v-if="modal.type === 'profileEdit'" class="modal-body">
          <div class="form-grid">
            <div class="field"><label>所属行业</label><input v-model="profileForm.industry" maxlength="64" placeholder="如：食品加工" /></div>
            <div class="field"><label>地区</label><input v-model="profileForm.region" maxlength="64" placeholder="如：黑龙江哈尔滨" /></div>
          </div>
          <div class="notice blue" style="margin-top:16px">企业名称与联系人如需变更，请联系业务人员；所属行业和地区可自行维护。</div>
          <div class="modal-actions">
            <button class="btn btn-ghost" @click="closeModal">取消</button>
            <button class="btn btn-primary" :disabled="savingProfile" @click="saveProfile">{{ savingProfile ? '保存中…' : '保存' }}</button>
          </div>
        </div>
        <div v-if="modal.type === 'quote' && quoteDetail" class="modal-body">
          <div class="spec"><span>报价单号</span><strong class="mono">{{ quoteDetail.quote.quoteNo }}</strong></div>
          <div class="spec"><span>产品</span><strong>{{ quoteDetail.quote.productName }}</strong></div>
          <div class="spec"><span>含税总价</span><strong>{{ money(quoteDetail.quote.totalAmount) }}</strong></div>
          <div class="spec"><span>有效期</span><strong>{{ date(quoteDetail.quote.validUntil) }}</strong></div>
          <div class="spec"><span>付款条件</span><strong>{{ quoteDetail.quote.paymentTerms }}</strong></div>
          <div class="spec"><span>状态</span><strong><span :class="['pill', statusPill(quoteDetail.quote.status)]">{{ label(quoteDetail.quote.status) }}</span></strong></div>
          <h4>议价记录</h4>
          <div v-for="n in quoteDetail.negotiations" :key="n.id" class="mini-row"><span>第{{ n.roundNo }}轮 · {{ n.party === 'CUSTOMER' ? '客户还价' : '我方报价' }}</span><strong>{{ money(n.amount) }}</strong></div>
          <div v-if="['SENT','BARGAINING'].includes(quoteDetail.quote.status)" class="bargain-box">
            <div class="field"><label>还价金额（元）</label><input v-model.number="bargainAmount" type="number" /></div>
            <div class="field"><label>留言</label><input v-model="bargainRemark" placeholder="希望优惠/包含维保等" /></div>
            <button class="btn btn-ghost btn-sm" @click="bargain">提交还价</button>
          </div>
          <div class="modal-actions">
            <button v-if="['SENT','BARGAINING'].includes(quoteDetail.quote.status)" class="btn btn-primary" @click="confirmQuote">确认报价</button>
          </div>
        </div>
        <div v-if="modal.type === 'contract' && contractDetail" class="modal-body">
          <div class="spec"><span>合同编号</span><strong class="mono">{{ contractDetail.contract.contractNo }}</strong></div>
          <div class="spec"><span>金额</span><strong>{{ money(contractDetail.contract.amount) }}</strong></div>
          <div class="spec"><span>审批 / 签署</span><strong>{{ label(contractDetail.contract.auditStatus) }} / {{ label(contractDetail.contract.signStatus) }}</strong></div>
          <h4>收款计划</h4>
          <div v-for="p in contractDetail.payments" :key="p.id" class="mini-row"><span>{{ p.phase }} · 计划 {{ date(p.planDate) }}</span><strong>{{ money(p.amount) }} · {{ label(p.status) }}</strong></div>
          <h4>履约节点</h4>
          <div v-for="i in contractDetail.installations" :key="i.id" class="mini-row"><span>{{ {SHIPPED:'发货',ARRIVED:'到货',INSTALL:'安装',ACCEPTANCE:'验收'}[i.stage] || i.stage }}</span><strong>{{ label(i.status) }}</strong></div>
          <div v-if="contractDetail.order" class="notice blue" style="margin-top:12px">生产进度：{{ contractDetail.order.productionProgress }} · 物流：{{ contractDetail.order.deliveryStatus }}</div>
          <div class="modal-actions">
            <button v-if="contractDetail.contract.auditStatus === 'APPROVED' && contractDetail.contract.signStatus !== 'SIGNED'" class="btn btn-primary" @click="signContract">在线签署合同</button>
          </div>
        </div>
        <div v-if="modal.type === 'ticket' && ticketDetail" class="modal-body">
          <div class="spec"><span>工单号</span><strong class="mono">{{ ticketDetail.ticket.ticketNo }}</strong></div>
          <div class="spec"><span>状态</span><strong><span :class="['pill', statusPill(ticketDetail.ticket.status)]">{{ label(ticketDetail.ticket.status) }}</span></strong></div>
          <div class="spec"><span>故障描述</span><strong>{{ ticketDetail.ticket.description }}</strong></div>
          <div class="spec"><span>技师</span><strong>{{ ticketDetail.ticket.technicianName || '待派单' }}</strong></div>
          <h4>处理记录</h4>
          <div v-for="r in ticketDetail.records" :key="r.id" class="timeline-row"><span>{{ dateTime(r.createdAt) }}</span><p>{{ r.action }}：{{ r.content }}</p></div>
          <div v-if="['WAIT_REVIEW','COMPLETED'].includes(ticketDetail.ticket.status)" class="review-box">
            <h4>服务评价</h4>
            <div class="stars"><button v-for="i in 5" :key="i" :class="{ active: reviewScore >= i }" @click="reviewScore = i">★</button></div>
            <textarea v-model="reviewComment" rows="2" placeholder="对本次服务的评价" />
            <button class="btn btn-primary btn-sm" @click="submitReview">提交评价</button>
          </div>
        </div>
        <div v-if="modal.type === 'ticketForm'" class="modal-body">
          <div class="field"><label>设备</label><select v-model="ticketForm.equipmentId"><option :value="undefined">未选择设备</option><option v-for="e in equipment" :key="e.id" :value="e.id">{{ e.equipmentNo }} · {{ e.model }}</option></select></div>
          <div class="field" style="margin-top:12px"><label>报修类型</label><select v-model="ticketForm.type"><option value="REPAIR">报修</option><option value="MAINTAIN">保养</option><option value="INSPECT">年检</option></select></div>
          <div class="field" style="margin-top:12px"><label>优先级</label><select v-model="ticketForm.priority"><option value="NORMAL">普通</option><option value="HIGH">紧急</option><option value="LOW">低</option></select></div>
          <div class="field" style="margin-top:12px"><label>故障描述 *</label><textarea v-model="ticketForm.description" rows="3" placeholder="请描述现象、报警代码与现场情况" /></div>
          <div class="field" style="margin-top:12px"><label>联系人 / 电话</label><input v-model="ticketForm.contactName" placeholder="联系人姓名" /></div>
          <div class="field" style="margin-top:12px">
            <label>现场照片</label>
            <label class="photo-pick">
              {{ uploadingPhotos ? '上传中…' : '＋ 添加照片' }}
              <input type="file" accept="image/*" multiple @change="uploadTicketPhotos" />
            </label>
            <div class="photo-list"><img v-for="p in ticketPhotos" :key="p" :src="p" alt="报修照片" /></div>
          </div>
          <div class="notice blue" style="margin-top:14px">工单受理后自动进入售后派单；24 小时内同类未完结工单会提示合并，避免重复报修。</div>
          <div class="modal-actions"><button class="btn btn-primary" :disabled="submitting || uploadingPhotos" @click="submitTicket">提交报修</button></div>
        </div>
        <div v-if="modal.type === 'solution' && solutionDetail" class="modal-body">
          <h4>{{ solutionDetail.name }}</h4>
          <div class="rich-html" v-html="sanitizeHtml(solutionDetail.content)"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { sessionStore } from '@/stores/session'
import { toast } from '@/ui/toast'
import { label, money, date, dateTime, statusPill, sanitizeHtml } from '@/utils'

const store = sessionStore(); const router = useRouter()
const sections = [
  { key: 'overview', title: '概览' }, { key: 'demands', title: '我的需求' }, { key: 'solutions', title: '我的方案' },
  { key: 'quotes', title: '我的报价' }, { key: 'contracts', title: '我的合同' }, { key: 'service', title: '售后服务' },
  { key: 'notifications', title: '消息通知' }, { key: 'profile', title: '企业档案' }
]
const tab = ref('overview'); const loading = ref(false)
const overview = ref<any>(null); const demands = ref<any[]>([]); const solutions = ref<any[]>([]); const quotes = ref<any[]>([])
const contracts = ref<any[]>([]); const tickets = ref<any[]>([]); const maintenance = ref<any[]>([]); const equipment = ref<any[]>([])
const notifications = ref<any[]>([]); const profileData = ref<any>(null)
const loaded = new Set<string>()
const modal = ref<any>(null)
const quoteDetail = ref<any>(null); const contractDetail = ref<any>(null); const ticketDetail = ref<any>(null); const solutionDetail = ref<any>(null)
const bargainAmount = ref<number>(); const bargainRemark = ref('')
const reviewScore = ref(5); const reviewComment = ref('')
const ticketForm = reactive<any>({ type: 'REPAIR', priority: 'NORMAL', description: '', contactName: '', equipmentId: undefined, photos: '' })
const ticketPhotos = ref<string[]>([])
const uploadingPhotos = ref(false)
const submitting = ref(false)
const profileForm = reactive({ industry: '', region: '' })
const savingProfile = ref(false)
const unread = computed(() => notifications.value.filter((n) => n.readFlag === 0).length)
const unconfirmedQuotes = computed(() => quotes.value.filter((q) => ['SENT', 'BARGAINING'].includes(q.status)).length)

async function loadOverview() {
  overview.value = await http.get('/customer/overview')
  notifications.value = ((await http.get('/customer/notifications', { params: { page: 1, size: 20 } })) as any).records
}
async function loadSection(key: string) {
  if (loaded.has(key)) return
  const map: Record<string, string> = { demands: '/customer/demands', solutions: '/customer/solutions', quotes: '/customer/quotes', contracts: '/customer/contracts', service: '/customer/tickets', notifications: '/customer/notifications', profile: '/customer/profile' }
  if (key === 'overview' || key === 'notifications') return
  if (key === 'profile') { profileData.value = await http.get('/customer/profile'); equipment.value = await http.get('/customer/equipment'); loaded.add(key); return }
  if (key === 'service') {
    tickets.value = await http.get('/customer/tickets'); maintenance.value = await http.get('/customer/maintenance'); equipment.value = await http.get('/customer/equipment')
    loaded.add(key); return
  }
  if (key === 'notifications') { notifications.value = ((await http.get('/customer/notifications', { params: { page: 1, size: 20 } })) as any).records; return }
  const data: any = await http.get(map[key])
  if (key === 'demands') demands.value = data
  if (key === 'solutions') solutions.value = data
  if (key === 'quotes') quotes.value = data
  if (key === 'contracts') contracts.value = data
  loaded.add(key)
}
async function switchTab(key: string) {
  tab.value = key
  await loadSection(key)
}
function openProfileEdit() {
  profileForm.industry = profileData.value?.customer?.industry || ''
  profileForm.region = profileData.value?.customer?.region || ''
  modal.value = { type: 'profileEdit', title: '编辑企业档案' }
}
async function saveProfile() {
  if (savingProfile.value) return
  savingProfile.value = true
  try {
    const data: any = await http.put('/customer/profile', {
      industry: profileForm.industry.trim(),
      region: profileForm.region.trim()
    })
    profileData.value = data
    store.updateCustomerFields({
      industry: data?.customer?.industry ?? null,
      region: data?.customer?.region ?? null
    })
    toast('企业档案已更新', 'success')
    closeModal()
  } finally { savingProfile.value = false }
}
function logout() { store.logout(); router.push('/login') }
function daysLeft(value: string) {
  if (!value) return 999
  return Math.round((new Date(value + 'T00:00:00').getTime() - new Date().setHours(0,0,0,0)) / 86400000)
}
function closeModal() { modal.value = null; quoteDetail.value = null; contractDetail.value = null; ticketDetail.value = null; solutionDetail.value = null }
async function openQuote(q: any) {
  quoteDetail.value = await http.get(`/customer/quotes/${q.id}`)
  bargainAmount.value = Number(q.totalAmount || 0) * 0.95
  bargainRemark.value = ''
  modal.value = { type: 'quote', title: `报价详情 · ${q.quoteNo}` }
}
async function openContract(c: any) { contractDetail.value = await http.get(`/customer/contracts/${c.id}`); modal.value = { type: 'contract', title: `合同进度 · ${c.contractNo}` } }
async function openTicket(t: any) { ticketDetail.value = await http.get(`/customer/tickets/${t.id}`); reviewScore.value = t.satisfactionScore || 5; reviewComment.value = t.review || ''; modal.value = { type: 'ticket', title: `工单详情 · ${t.ticketNo}` } }
function openTicketForm() { Object.assign(ticketForm, { type: 'REPAIR', priority: 'NORMAL', description: '', contactName: '', equipmentId: undefined, photos: '' }); ticketPhotos.value = []; modal.value = { type: 'ticketForm', title: '在线报修' } }
async function uploadTicketPhotos(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  uploadingPhotos.value = true
  for (const file of files) {
    const fd = new FormData(); fd.append('file', file); fd.append('category', 'ticket')
    try {
      const record: any = await http.post('/customer/files/upload', fd)
      if (record?.url) ticketPhotos.value.push(record.url)
    } catch {}
  }
  ticketForm.photos = ticketPhotos.value.join(',')
  uploadingPhotos.value = false
  input.value = ''
}
async function openSolution(s: any) { solutionDetail.value = await http.get(`/customer/solutions/${s.id}`); modal.value = { type: 'solution', title: '方案详情' } }
async function confirmSolution(s: any) {
  if (!window.confirm(`确认方案「${s.name}」？确认后将进入谈价报价阶段。`)) return
  await http.post(`/customer/solutions/${s.id}/confirm`); toast('方案已确认', 'success'); solutions.value = await http.get('/customer/solutions'); loadOverview()
}
async function confirmQuote() {
  if (!quoteDetail.value) return
  if (!window.confirm('确认该报价并生成合同草稿？')) return
  await http.post(`/customer/quotes/${quoteDetail.value.quote.id}/confirm`); toast('报价已确认', 'success'); closeModal(); quotes.value = await http.get('/customer/quotes'); contracts.value = await http.get('/customer/contracts'); loadOverview()
}
async function bargain() {
  if (!quoteDetail.value || !bargainAmount.value) return toast('请输入还价金额', 'error')
  await http.post(`/customer/quotes/${quoteDetail.value.quote.id}/bargain`, { amount: bargainAmount.value, remark: bargainRemark.value })
  toast('还价已提交', 'success'); quoteDetail.value = await http.get(`/customer/quotes/${quoteDetail.value.quote.id}`); quotes.value = await http.get('/customer/quotes')
}
async function signContract() {
  if (!contractDetail.value) return
  if (!window.confirm('确认在线签署合同？签署后进入履约阶段。')) return
  await http.post(`/customer/contracts/${contractDetail.value.contract.id}/sign`); toast('合同已签署', 'success'); contractDetail.value = await http.get(`/customer/contracts/${contractDetail.value.contract.id}`); contracts.value = await http.get('/customer/contracts'); loadOverview()
}
async function submitTicket() {
  if (!ticketForm.description) return toast('请填写故障描述', 'error')
  submitting.value = true
  try {
    await http.post('/customer/tickets', ticketForm); toast('报修已提交，工单已受理', 'success'); closeModal(); tickets.value = await http.get('/customer/tickets'); loadOverview()
  } finally { submitting.value = false }
}
async function submitReview() {
  if (!ticketDetail.value) return
  await http.post(`/customer/tickets/${ticketDetail.value.ticket.id}/review`, { score: reviewScore.value, comment: reviewComment.value })
  toast('评价已提交', 'success'); closeModal(); tickets.value = await http.get('/customer/tickets'); loadOverview()
}
async function readNotification(n: any) { await http.post(`/customer/notifications/${n.id}/read`); n.readFlag = 1 }
async function readAll() {
  for (const n of notifications.value.filter((x) => x.readFlag === 0)) await http.post(`/customer/notifications/${n.id}/read`)
  notifications.value.forEach((n) => n.readFlag = 1); toast('已全部标记已读', 'success')
}
onMounted(async () => {
  loading.value = true
  try {
    await loadOverview()
    await loadSection('demands')
  } finally { loading.value = false }
})
</script>

<style scoped>
.account-hero { background: linear-gradient(135deg, #0f172a, #1b2a41); color: #fff; padding: 46px 0 36px; }
.account-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; }
.kicker { font-size: 10px; color: #e28a63; letter-spacing: 2px; }
.account-head h1 { font-size: 32px; margin: 10px 0 4px; letter-spacing: -.8px; }
.account-head p { color: #93a3b8; font-size: 11px; margin: 0; }
.account-actions { display: flex; gap: 8px; }
.account-body { display: grid; grid-template-columns: 200px 1fr; gap: 30px; padding: 34px 0 80px; align-items: start; }
.account-nav { background: #fff; border: 1px solid var(--line); padding: 10px; position: sticky; top: 96px; }
.account-nav button { width: 100%; border: none; background: transparent; text-align: left; padding: 11px 12px; font-size: 13px; color: var(--steel); display: flex; justify-content: space-between; align-items: center; }
.account-nav button.active { background: var(--ink); color: #fff; }
.account-nav button em { font-style: normal; background: var(--accent); color: #fff; font-size: 10px; border-radius: 50%; min-width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; }
.nav-foot { border-top: 1px solid var(--line); margin-top: 10px; padding: 14px 10px 6px; display: flex; flex-direction: column; gap: 3px; }
.nav-foot span { font-size: 10px; color: var(--steel-2); }
.nav-foot strong { font-size: 17px; }
.nav-foot a { font-size: 11px; color: var(--accent); margin-top: 4px; }
.account-main { min-height: 480px; }
.account-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.account-stats > div { background: #fff; border: 1px solid var(--line); padding: 18px; }
.account-stats span { display: block; font-size: 10px; color: var(--steel-2); }
.account-stats strong { display: block; font-size: 25px; margin: 3px 0 4px; }
.account-stats a, .account-stats button { border: none; background: transparent; padding: 0; color: var(--accent); font-size: 10px; }
.overview-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.panel { background: #fff; border: 1px solid var(--line); padding: 20px; }
.panel h2 { font-size: 15px; margin: 0 0 12px; }
.section-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.section-title h2 { font-size: 22px; margin: 0; }
.list-row { display: flex; justify-content: space-between; gap: 14px; padding: 10px 0; border-bottom: 1px dashed var(--line); align-items: center; font-size: 12px; }
.list-row strong { display: block; }
.list-row span { display: block; color: var(--steel-2); font-size: 10px; }
.table-wrap { background: #fff; border: 1px solid var(--line); overflow-x: auto; }
.portal-table { width: 100%; border-collapse: collapse; min-width: 760px; }
.portal-table th { background: #faf9f6; text-align: left; padding: 11px 14px; font-size: 10px; color: var(--steel); letter-spacing: .4px; border-bottom: 1px solid var(--line); font-weight: 600; }
.portal-table td { padding: 12px 14px; border-bottom: 1px solid #f0ede7; font-size: 12px; }
.portal-table tr:hover td { background: #fcfbf8; }
.link-btn { border: none; background: transparent; color: var(--accent); font-size: 11px; padding: 0; }
.solution-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.solution-card { background: #fff; border: 1px solid var(--line); padding: 20px; }
.solution-head { display: flex; justify-content: space-between; align-items: center; font-size: 10px; }
.solution-card h3 { font-size: 16px; margin: 12px 0 6px; }
.solution-card p { color: var(--steel); font-size: 11px; margin: 0 0 14px; }
.solution-actions { display: flex; gap: 8px; align-items: center; }
.notice-list { display: flex; flex-direction: column; gap: 8px; }
.notice-row { display: flex; justify-content: space-between; gap: 14px; background: #fff; border: 1px solid var(--line); padding: 14px 16px; }
.notice-row.unread { border-left: 3px solid var(--accent); background: #fdfaf8; }
.notice-row strong { font-size: 13px; }
.notice-row p { color: var(--steel); font-size: 12px; margin: 4px 0; }
.notice-row span { font-size: 10px; color: var(--steel-2); }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel h3 { font-size: 16px; margin: 0 0 12px; }
.contact-row, .equipment-row { display: flex; justify-content: space-between; gap: 12px; border-top: 1px dashed var(--line); padding: 9px 0; font-size: 12px; }
.equipment-row > div { display: flex; flex-direction: column; gap: 2px; }
.equipment-row span { font-size: 10px; color: var(--steel-2); }
.modal-mask { position: fixed; inset: 0; background: rgba(15,23,42,.55); z-index: 300; display: flex; align-items: center; justify-content: center; padding: 18px; }
.modal-card { background: #fff; width: 520px; max-width: 100%; max-height: 88vh; overflow-y: auto; }
.modal-card.wide { width: 720px; }
.modal-head { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid var(--line); position: sticky; top: 0; background: #fff; }
.modal-head h3 { margin: 0; font-size: 16px; }
.modal-close { border: none; background: transparent; font-size: 22px; color: var(--steel); }
.modal-body { padding: 18px 22px 24px; }
.modal-body h4 { font-size: 13px; margin: 16px 0 8px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
.mini-row, .timeline-row { display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px dashed var(--line); padding: 7px 0; font-size: 11px; }
.timeline-row { display: grid; grid-template-columns: 120px 1fr; }
.timeline-row p { margin: 0; }
.bargain-box { background: #faf9f6; padding: 12px; margin-top: 14px; display: grid; grid-template-columns: 1fr 1fr auto; gap: 8px; align-items: end; }
.bargain-box .field input { padding: 8px 10px; }
.review-box { border-top: 1px solid var(--line); margin-top: 14px; }
.stars button { border: none; background: transparent; color: #d7d2c7; font-size: 20px; padding: 0 2px; }
.stars button.active { color: var(--accent); }
.review-box textarea { width: 100%; border: 1px solid var(--line); padding: 8px; font-size: 12px; margin: 8px 0; }
.photo-pick { display: inline-flex; align-items: center; padding: 7px 14px; border: 1px dashed var(--line); border-radius: 6px; font-size: 12px; color: var(--steel); cursor: pointer; }
.photo-pick input { position: absolute; width: 0; height: 0; opacity: 0; }
.photo-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.photo-list img { width: 60px; height: 60px; object-fit: cover; border: 1px solid var(--line); border-radius: 6px; }
@media (max-width: 900px) {
  .account-body { grid-template-columns: 1fr; }
  .account-nav { position: static; display: flex; overflow-x: auto; padding: 6px; }
  .account-nav button { white-space: nowrap; width: auto; padding: 8px 12px; }
  .nav-foot { display: none; }
  .account-stats { grid-template-columns: 1fr 1fr; }
  .overview-grid, .solution-grid, .profile-grid { grid-template-columns: 1fr; }
  .account-head { flex-direction: column; align-items: flex-start; }
  .bargain-box { grid-template-columns: 1fr; }
}
</style>
