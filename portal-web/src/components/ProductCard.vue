<template>
  <router-link :to="`/products/${product.id}`" class="product-card card-hover">
    <div class="visual" :class="`type-${product.boilerType || 'steam'}`">
      <img v-if="firstImage(product.images)" :src="firstImage(product.images)" :alt="product.model" />
      <svg v-else viewBox="0 0 300 160" class="boiler-art" aria-hidden="true">
        <defs>
          <linearGradient :id="`grad-${product.id}`" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#1e293b" />
            <stop offset="100%" stop-color="#334155" />
          </linearGradient>
        </defs>
        <rect x="64" y="44" width="172" height="86" rx="12" :fill="`url(#grad-${product.id})`" />
        <rect x="72" y="52" width="156" height="14" rx="7" fill="rgba(255,255,255,.12)" />
        <circle cx="100" cy="90" r="18" fill="none" stroke="#f1f5f9" stroke-width="2" opacity=".8" />
        <path d="M100 90 L110 80" stroke="#d9633b" stroke-width="2" stroke-linecap="round" />
        <rect x="140" y="78" width="72" height="6" rx="3" fill="rgba(255,255,255,.22)" />
        <rect x="140" y="92" width="48" height="6" rx="3" fill="rgba(255,255,255,.14)" />
        <rect x="140" y="106" width="60" height="6" rx="3" fill="rgba(255,255,255,.1)" />
        <path d="M82 130 V148 M218 130 V148 M76 148 H90 M212 148 H226" stroke="#94a3b8" stroke-width="5" stroke-linecap="round" />
        <path d="M236 64 H272 V98 H252" fill="none" stroke="#94a3b8" stroke-width="5" stroke-linejoin="round" />
        <path d="M96 34 C96 22 106 20 106 10 M114 34 C114 22 124 20 124 10" fill="none" stroke="rgba(255,255,255,.4)" stroke-width="2" stroke-linecap="round" class="steam" />
      </svg>
      <span class="type-flag">{{ label(product.boilerType) }}</span>
    </div>
    <div class="body">
      <div class="model">{{ product.model }}</div>
      <div class="specs">
        <span>{{ label(product.fuelType) }}</span>
        <i />
        <span>{{ product.capacity || '—' }}</span>
        <i />
        <span>{{ label(product.pressureLevel) }}</span>
      </div>
      <p class="usage">{{ product.usageDesc || '工业蒸汽 / 供热应用' }}</p>
      <div class="foot">
        <div class="price"><em>指导价</em><strong>{{ money(product.guidePrice) }}</strong></div>
        <span class="more">查看详情 <i>→</i></span>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { sessionStore } from '@/stores/session'
import { label, money, firstImage } from '@/utils'

defineProps<{ product: any }>()
const store = sessionStore()
</script>

<style scoped>
.product-card { display: block; background: #fff; border: 1px solid var(--line); border-radius: var(--radius); overflow: hidden; }
.visual { position: relative; height: 180px; display: flex; align-items: center; justify-content: center; overflow: hidden; background: linear-gradient(135deg, #eef1f4, #e4e8ec); }
.visual img { width: 100%; height: 100%; object-fit: cover; }
.visual.type-steam { background: linear-gradient(135deg, #edf1f5, #dbe2e8); }
.visual.type-hotwater { background: linear-gradient(135deg, #f4eee9, #ebe2d9); }
.visual.type-thermal { background: linear-gradient(135deg, #eef0ee, #e0e5e0); }
.visual.type-wasteheat { background: linear-gradient(135deg, #f0edef, #e4dde2); }
.boiler-art { width: 82%; height: auto; filter: drop-shadow(0 14px 16px rgba(15,23,42,.12)); }
.steam { animation: steam 3.2s ease-in-out infinite; }
@keyframes steam { 0%,100% { opacity: .25; transform: translateY(2px); } 50% { opacity: .65; transform: translateY(-3px); } }
.type-flag { position: absolute; top: 12px; left: 12px; background: rgba(17,24,39,.78); color: #fff; font-size: 10px; letter-spacing: .5px; padding: 3px 9px; border-radius: 2px; }
.body { padding: 18px 20px 16px; }
.model { font-size: 17px; font-weight: 700; letter-spacing: -.2px; }
.specs { display: flex; align-items: center; gap: 8px; margin-top: 7px; color: var(--steel); font-size: 11px; }
.specs i { width: 2px; height: 2px; background: #b9b1a3; border-radius: 50%; }
.usage { color: var(--steel); font-size: 12px; margin: 10px 0 16px; min-height: 20px; }
.foot { border-top: 1px solid #efebe3; padding-top: 12px; display: flex; align-items: flex-end; justify-content: space-between; }
.price em { display: block; font-style: normal; color: var(--steel-2); font-size: 10px; letter-spacing: .5px; }
.price strong { font-size: 17px; color: var(--ink); font-weight: 700; }
.more { font-size: 11px; color: var(--accent); font-weight: 600; }
.more i { font-style: normal; margin-left: 3px; }
</style>
