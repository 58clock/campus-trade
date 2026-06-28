<template>
  <div class="container">
    <h2 style="margin-bottom:8px">智能推荐 <el-tag type="warning" size="small">Skill</el-tag></h2>
    <p style="color:#999;margin-bottom:24px">根据你的浏览历史推荐相似商品，多浏览同类商品可提高推荐精准度</p>

    <!-- 用户行为分析 -->
    <el-card v-if="analysis" class="analysis-card">
      <template #header>
        <span style="font-weight:600">浏览行为分析</span>
        <el-tag v-if="analysis.mode === 'hot'" type="info" size="small" style="margin-left:8px">冷启动模式</el-tag>
        <el-tag v-else type="success" size="small" style="margin-left:8px">个性化模式</el-tag>
      </template>

      <div v-if="analysis.mode === 'hot'">
        <el-alert title="你还没有浏览记录，推荐全站热门商品。多逛逛商品详情页，推荐会更精准。" type="info" :closable="false" show-icon />
      </div>

      <div v-else>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-statistic title="总浏览量" :value="analysis.totalBrowses" suffix="次" />
            <el-statistic title="浏览商品数" :value="analysis.uniqueProducts" suffix="件" style="margin-top:12px" />
            <p v-if="analysis.priceRange" style="color:#999;margin-top:8px;font-size:13px">
              浏览价格区间：¥{{ analysis.priceRange }}
            </p>
          </el-col>
          <el-col :span="12">
            <p style="font-size:13px;color:#666;margin-bottom:8px">分类偏好</p>
            <div v-for="c in analysis.topCategories" :key="c.category" class="cat-bar">
              <span class="cat-label">{{ catName(c.category) }}</span>
              <el-progress :percentage="c.percentage" :stroke-width="14" style="flex:1;margin:0 8px" />
              <span class="cat-count">{{ c.count }}次</span>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 推荐列表 -->
    <h3 v-if="items.length > 0" style="margin:24px 0 12px">为你推荐</h3>
    <div v-if="items.length > 0" class="product-grid">
      <el-card v-for="p in items" :key="p.productId" class="product-card" shadow="hover" @click="$router.push(`/product/${p.productId}`)">
        <div class="product-info">
          <div class="product-header">
            <h3>{{ p.title }}</h3>
            <el-tag v-if="p.score >= 0.60" size="small" type="success">{{ Math.round(p.score * 100) }}% 匹配</el-tag>
            <el-tag v-else size="small" type="info">{{ Math.round(p.score * 100) }}% 匹配</el-tag>
          </div>
          <p class="price">¥{{ p.price }}</p>
          <p class="meta">{{ catName(p.category) }} · {{ p.viewCount }} 次浏览</p>
          <el-progress :percentage="Math.round(p.score * 100)" :stroke-width="6" :color="p.score >= 0.60 ? '#67C23A' : '#909399'" style="margin:8px 0" />
          <p class="reason" :class="{ 'reason-supplement': p.score < 0.60 }">
            <el-icon style="vertical-align:middle"><InfoFilled /></el-icon>
            {{ p.reason }}
          </p>
        </div>
      </el-card>
    </div>

    <el-empty v-if="items.length === 0" description="暂无推荐" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { skillApi } from '@/api'
import { InfoFilled } from '@element-plus/icons-vue'

const items = ref([])
const analysis = ref(null)

const catMap = { BOOK: '书籍', ELECTRONICS: '电子产品', LIFESTYLE: '生活用品', SPORTS: '运动用品', OTHER: '其他' }
function catName(key) { return catMap[key] || key }

onMounted(async () => {
  try {
    const res = await skillApi.recommend(8)
    if (res.data) {
      items.value = res.data.items || []
      analysis.value = res.data.analysis || null
    }
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
.analysis-card { margin-bottom: 8px; }
.cat-bar { display: flex; align-items: center; margin-bottom: 8px; }
.cat-label { width: 70px; font-size: 13px; color: #606266; }
.cat-count { width: 36px; font-size: 12px; color: #999; text-align: right; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.product-card { cursor: pointer; transition: transform .2s; }
.product-card:hover { transform: translateY(-4px); }
.product-header { display: flex; justify-content: space-between; align-items: flex-start; }
.product-header h3 { flex: 1; margin-right: 8px; }
.price { color: #f56c6c; font-size: 20px; font-weight: bold; margin: 4px 0; }
.meta { color: #999; font-size: 12px; }
.reason { color: #409EFF; font-size: 12px; margin-top: 6px; }
.reason-supplement { color: #909399; }
</style>
