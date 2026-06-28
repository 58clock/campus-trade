<template>
  <div class="container">
    <h2 style="margin-bottom:16px">智能推荐 <el-tag type="warning" size="small">Skill</el-tag></h2>
    <p style="color:#999;margin-bottom:16px">根据你的浏览历史推荐相似商品。多浏览同类商品可提高推荐精准度。</p>

    <div v-if="items.length > 0" class="product-grid">
      <el-card v-for="p in items" :key="p.productId" class="product-card" shadow="hover" @click="$router.push(`/product/${p.productId}`)">
        <div class="product-info">
          <h3>{{ p.title }}</h3>
          <p class="price">¥{{ p.price }}</p>
          <p class="meta">
            {{ p.category }} · {{ p.viewCount }} 次浏览
            <el-progress :percentage="Math.round(p.score * 100)" :stroke-width="6" style="width:100px;margin-left:8px" />
          </p>
          <p class="score">推荐指数 {{ (p.score * 100).toFixed(0) }}%</p>
        </div>
      </el-card>
    </div>

    <el-empty v-else description="暂无推荐" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { skillApi } from '@/api'

const items = ref([])

onMounted(async () => {
  try {
    const res = await skillApi.recommend(8)
    items.value = res.data || []
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.product-card { cursor: pointer; transition: transform .2s; }
.product-card:hover { transform: translateY(-4px); }
.price { color: #f56c6c; font-size: 20px; font-weight: bold; margin: 4px 0; }
.meta { color: #999; font-size: 12px; display: flex; align-items: center; }
.score { color: #e6a23c; font-size: 13px; margin-top: 4px; }
</style>
