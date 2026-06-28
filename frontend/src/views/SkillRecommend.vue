<template>
  <div class="container">
    <h2 style="margin-bottom:8px">智能推荐 <el-tag type="warning" size="small">Skill</el-tag></h2>
    <p style="color:#999;margin-bottom:24px">根据你的浏览历史推荐商品，同时展示全站热门</p>

    <!-- 浏览偏好 -->
    <el-card v-if="analysis && analysis.totalBrowses > 0" class="analysis-card">
      <template #header><span style="font-weight:600">你的浏览偏好</span></template>
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
    <div style="margin-top:24px">
      <h3 class="section-title">
        <el-icon><Star /></el-icon> 猜你喜欢
        <span class="section-sub">根据你的浏览历史</span>
      </h3>
      <div v-if="personalized.length > 0" class="product-grid">
        <el-card v-for="p in personalized" :key="'p'+p.productId" class="product-card" shadow="hover"
          @click="$router.push(`/product/${p.productId}`)">
          <div class="product-header"><h3>{{ p.title }}</h3><el-tag size="small" type="success">{{ Math.round(p.score * 100) }}%</el-tag></div>
          <p class="price">¥{{ p.price }}</p>
          <p class="meta">{{ catName(p.category) }} · {{ p.viewCount }} 次浏览</p>
          <el-progress :percentage="Math.round(p.score * 100)" :stroke-width="6" color="#67C23A" style="margin:8px 0" />
          <p class="reason reason-personal"><el-icon><InfoFilled /></el-icon> {{ p.reason }}</p>
        </el-card>
      </div>
      <el-card v-else class="empty-card">
        <el-empty description="还没有浏览记录" :image-size="60">
          <p style="color:#999;font-size:13px">去首页逛逛商品详情，系统会根据你的浏览偏好智能推荐</p>
          <el-button type="primary" size="small" style="margin-top:8px" @click="$router.push('/')">去逛逛</el-button>
        </el-empty>
      </el-card>
    </div>

    <!-- 版块二：全站热门 -->
    <div style="margin-top:32px">
      <h3 class="section-title">
        <el-icon><TrendCharts /></el-icon> 全站热门
        <span class="section-sub">大家都在看</span>
      </h3>
      <div v-if="hot.length > 0" class="product-grid">
        <el-card v-for="p in hot" :key="'h'+p.productId" class="product-card" shadow="hover"
          @click="$router.push(`/product/${p.productId}`)">
          <div class="product-header"><h3>{{ p.title }}</h3><el-tag size="small" type="warning">{{ Math.round(p.score * 100) }}%</el-tag></div>
          <p class="price">¥{{ p.price }}</p>
          <p class="meta">{{ catName(p.category) }} · {{ p.viewCount }} 次浏览</p>
          <el-progress :percentage="Math.round(p.score * 100)" :stroke-width="6" color="#E6A23C" style="margin:8px 0" />
          <p class="reason reason-hot"><el-icon><TrendCharts /></el-icon> {{ p.reason }}</p>
        </el-card>
      </div>
      <el-card v-else class="empty-card">
        <el-empty description="暂无热门商品" :image-size="60" />
      </el-card>
    </div>

    <!-- 数据来源调试面板 -->
    <el-card v-if="debug" style="margin-top:32px;background:#fafafa" shadow="never">
      <template #header>
        <span style="font-weight:600">数据来源: {{ debug.source_table }}</span>
        <el-tag size="small" style="margin-left:8px">调试信息</el-tag>
      </template>
      <p style="font-size:13px;color:#666;margin-bottom:12px">
        从 <b>browse_history</b> 表查到 <b>{{ debug.total_rows }}</b> 条浏览记录<br/>
        从 <b>product</b> 表查到 <b>{{ debug.viewed_products?.length || 0 }}</b> 个浏览过的商品 → 用于提取关键词模糊搜索
      </p>
      <el-collapse>
        <el-collapse-item title="browse_history 表原始数据 ({{ debug.total_rows }} 行)" name="1">
          <el-table :data="debug.rows" size="small" border stripe max-height="300">
            <el-table-column prop="userId" label="userId" width="70" />
            <el-table-column prop="productId" label="productId" width="90" />
            <el-table-column prop="category" label="category" width="110" />
            <el-table-column prop="productTitle" label="product表查到的标题" min-width="200" />
            <el-table-column prop="createdAt" label="createdAt" width="180" />
          </el-table>
        </el-collapse-item>
        <el-collapse-item title="viewed_products (从 product 表 SQL: SELECT * FROM product WHERE id IN (...))" name="2">
          <el-table :data="debug.viewed_products" size="small" border stripe max-height="300">
            <el-table-column prop="id" label="id" width="60" />
            <el-table-column prop="title" label="title" min-width="250" />
            <el-table-column prop="category" label="category" width="110" />
            <el-table-column prop="keyword" label="extractKeyword(标题) → LIKE查询" width="180" />
          </el-table>
        </el-collapse-item>
        <el-collapse-item title="猜你喜欢 推荐结果 ({{ personalized.length }} 条)" name="3">
          <el-table :data="personalized" size="small" border stripe max-height="300">
            <el-table-column prop="productId" label="productId" width="80" />
            <el-table-column prop="title" label="title" min-width="200" />
            <el-table-column prop="category" label="category" width="110" />
            <el-table-column label="score" width="70"><template #default="{row}">{{ Math.round(row.score * 100) }}%</template></el-table-column>
          </el-table>
        </el-collapse-item>
        <el-collapse-item title="全站热门 推荐结果 ({{ hot.length }} 条)" name="4">
          <el-table :data="hot" size="small" border stripe max-height="300">
            <el-table-column prop="productId" label="productId" width="80" />
            <el-table-column prop="title" label="title" min-width="200" />
            <el-table-column prop="category" label="category" width="110" />
            <el-table-column prop="viewCount" label="viewCount" width="90" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { skillApi } from '@/api'
import { Star, TrendCharts, InfoFilled } from '@element-plus/icons-vue'

const personalized = ref([])
const hot = ref([])
const analysis = ref(null)
const debug = ref(null)

const catMap = { BOOK: '书籍', ELECTRONICS: '电子产品', LIFESTYLE: '生活用品', SPORTS: '运动用品', OTHER: '其他' }
function catName(key) { return catMap[key] || key }

onMounted(async () => {
  try {
    const res = await skillApi.recommend(8)
    if (res.data) {
      personalized.value = res.data.personalized || []
      hot.value = res.data.hot || []
      analysis.value = res.data.analysis || null
      debug.value = res.data.debug || null
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
.empty-card { text-align: center; padding: 24px; }
.product-header { display: flex; justify-content: space-between; align-items: flex-start; }
.product-header h3 { flex: 1; margin-right: 8px; font-size: 14px; }
.price { color: #f56c6c; font-size: 18px; font-weight: bold; margin: 4px 0; }
.meta { color: #999; font-size: 12px; }
.reason { font-size: 11px; margin-top: 6px; display: flex; align-items: center; gap: 2px; }
.reason-personal { color: #67C23A; }
.reason-hot { color: #E6A23C; }
</style>
