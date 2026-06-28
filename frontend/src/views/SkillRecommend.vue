<template>
  <div class="container">
    <h2 style="margin-bottom:8px">智能推荐 <el-tag type="warning" size="small">Skill</el-tag></h2>
    <p style="color:#999;margin-bottom:24px">根据你的浏览历史推荐商品，同时展示全站热门</p>

    <!-- 浏览行为分析 -->
    <el-card v-if="analysis && analysis.totalBrowses > 0" class="analysis-card">
      <template #header>
        <span style="font-weight:600">你的浏览偏好</span>
      </template>
      <div style="display:flex;align-items:center;gap:24px;flex-wrap:wrap">
        <span>共浏览 <b>{{ analysis.totalBrowses }}</b> 次 · <b>{{ analysis.uniqueProducts }}</b> 件商品</span>
        <div v-for="c in analysis.topCategories" :key="c.category" style="display:flex;align-items:center;gap:4px">
          <span style="font-size:13px;color:#606266">{{ catName(c.category) }}</span>
          <el-progress :percentage="c.percentage" :stroke-width="8" :show-text="false" style="width:60px" />
          <span style="font-size:12px;color:#999">{{ c.count }}次</span>
        </div>
      </div>
    </el-card>

    <!-- 版块一：猜你喜欢 -->
    <div v-if="personalized.length > 0" style="margin-top:24px">
      <h3 class="section-title">
        <el-icon><Star /></el-icon> 猜你喜欢
        <span class="section-sub">根据你的浏览历史</span>
      </h3>
      <div class="product-grid">
        <el-card v-for="p in personalized" :key="p.productId" class="product-card" shadow="hover"
          @click="$router.push(`/product/${p.productId}`)">
          <div class="product-info">
            <div class="product-header">
              <h3>{{ p.title }}</h3>
              <el-tag size="small" type="success">{{ Math.round(p.score * 100) }}% 匹配</el-tag>
            </div>
            <p class="price">¥{{ p.price }}</p>
            <p class="meta">{{ catName(p.category) }} · {{ p.viewCount }} 次浏览</p>
            <el-progress :percentage="Math.round(p.score * 100)" :stroke-width="6" color="#67C23A" style="margin:8px 0" />
            <p class="reason"><el-icon><InfoFilled /></el-icon> {{ p.reason }}</p>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 无浏览历史提示 -->
    <el-card v-if="personalized.length === 0 && analysis" style="margin-top:24px">
      <el-empty description="还没有浏览记录" :image-size="80">
        <p style="color:#999;font-size:13px">去首页逛逛商品详情，系统就能根据你的偏好智能推荐</p>
        <el-button type="primary" size="small" style="margin-top:8px" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
    </el-card>

    <!-- 版块二：全站热门 -->
    <div v-if="hot.length > 0" style="margin-top:32px">
      <h3 class="section-title">
        <el-icon><TrendCharts /></el-icon> 全站热门
        <span class="section-sub">大家都在看</span>
      </h3>
      <div class="product-grid">
        <el-card v-for="p in hot" :key="p.productId" class="product-card" shadow="hover"
          @click="$router.push(`/product/${p.productId}`)">
          <div class="product-info">
            <div class="product-header">
              <h3>{{ p.title }}</h3>
              <el-tag size="small" type="warning">{{ Math.round(p.score * 100) }}% 热度</el-tag>
            </div>
            <p class="price">¥{{ p.price }}</p>
            <p class="meta">{{ catName(p.category) }} · {{ p.viewCount }} 次浏览</p>
            <el-progress :percentage="Math.round(p.score * 100)" :stroke-width="6" color="#E6A23C" style="margin:8px 0" />
            <p class="reason reason-hot"><el-icon><TrendCharts /></el-icon> {{ p.reason }}</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { skillApi } from '@/api'
import { Star, TrendCharts, InfoFilled } from '@element-plus/icons-vue'

const personalized = ref([])
const hot = ref([])
const analysis = ref(null)

const catMap = { BOOK: '书籍', ELECTRONICS: '电子产品', LIFESTYLE: '生活用品', SPORTS: '运动用品', OTHER: '其他' }
function catName(key) { return catMap[key] || key }

onMounted(async () => {
  try {
    const res = await skillApi.recommend(8)
    if (res.data) {
      personalized.value = res.data.personalized || []
      hot.value = res.data.hot || []
      analysis.value = res.data.analysis || null
    }
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
.analysis-card { margin-bottom: 8px; }
.section-title { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; font-size: 18px; }
.section-sub { font-size: 13px; color: #999; font-weight: normal; margin-left: 4px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.product-card { cursor: pointer; transition: transform .2s; }
.product-card:hover { transform: translateY(-4px); }
.product-header { display: flex; justify-content: space-between; align-items: flex-start; }
.product-header h3 { flex: 1; margin-right: 8px; font-size: 14px; }
.price { color: #f56c6c; font-size: 18px; font-weight: bold; margin: 4px 0; }
.meta { color: #999; font-size: 12px; }
.reason { color: #67C23A; font-size: 11px; margin-top: 6px; display: flex; align-items: center; gap: 2px; }
.reason-hot { color: #E6A23C; }
</style>
