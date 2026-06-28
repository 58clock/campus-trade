<template>
  <div class="container">
    <!-- 搜索栏 + 筛选 -->
    <el-card style="margin-bottom:16px">
      <el-row :gutter="12" align="middle">
        <el-col :span="8">
          <el-input v-model="query.keyword" placeholder="搜索商品..." clearable @clear="search" @keyup.enter="search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="query.category" placeholder="分类" clearable @change="search">
            <el-option label="书籍" value="BOOK" />
            <el-option label="电子产品" value="ELECTRONICS" />
            <el-option label="生活用品" value="LIFESTYLE" />
            <el-option label="运动用品" value="SPORTS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-input-number v-model="query.minPrice" placeholder="最低价" :min="0" @change="search" />
        </el-col>
        <el-col :span="3">
          <el-input-number v-model="query.maxPrice" placeholder="最高价" :min="0" @change="search" />
        </el-col>
        <el-col :span="3">
          <el-select v-model="query.sort" placeholder="排序" @change="search">
            <el-option label="最新" value="newest" />
            <el-option label="价格升序" value="price_asc" />
            <el-option label="价格降序" value="price_desc" />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-button type="primary" @click="search">搜索</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- AI 推荐区 -->
    <el-card v-if="userStore.isLoggedIn && recommends.length > 0" style="margin-bottom:16px">
      <template #header>
        <span style="font-weight:600">猜你喜欢</span>
        <el-tag type="warning" size="small" style="margin-left:8px">AI 推荐</el-tag>
      </template>
      <div class="product-grid">
        <el-card v-for="p in recommends" :key="p.productId" class="product-card" @click="goDetail(p.productId)">
          <div class="product-info">
            <h3>{{ p.title }}</h3>
            <p class="price">¥{{ p.price }}</p>
            <p class="meta">{{ p.category }} · 热度 {{ p.viewCount }}</p>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- 商品列表 -->
    <div v-if="products.length > 0" class="product-grid">
      <el-card v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
        <img :src="p.images?.[0] || '/placeholder.png'" class="product-img" />
        <div class="product-info">
          <h3>{{ p.title }}</h3>
          <p class="price">¥{{ p.price }}</p>
          <p class="meta">{{ p.category }} · {{ p.conditionLevel }} · {{ p.viewCount }}次浏览</p>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      @current-change="fetchProducts"
      style="justify-content:center; margin-top:16px"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productApi, skillApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const products = ref([])
const recommends = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 12
const query = reactive({ keyword: '', category: '', minPrice: null, maxPrice: null, sort: '' })

function goDetail(id) { router.push(`/product/${id}`) }
function search() { currentPage.value = 1; fetchProducts() }

async function fetchProducts() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.category) params.category = query.category
    if (query.minPrice !== null) params.minPrice = query.minPrice
    if (query.maxPrice !== null) params.maxPrice = query.maxPrice
    if (query.sort) params.sort = query.sort
    const res = await productApi.list(params)
    products.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function fetchRecommend() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await skillApi.recommend(4)
    recommends.value = res.data || []
  } catch { /* handled */ }
}

onMounted(() => { fetchProducts(); fetchRecommend() })
</script>

<style scoped>
.container { max-width: 1200px; margin: 24px auto; padding: 0 16px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-card { cursor: pointer; transition: transform .2s; }
.product-card:hover { transform: translateY(-4px); }
.product-img { width: 100%; height: 180px; object-fit: cover; border-radius: 8px; }
.product-info { margin-top: 8px; }
.product-info h3 { font-size: 15px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price { color: #f56c6c; font-size: 18px; font-weight: bold; }
.meta { color: #999; font-size: 12px; margin-top: 4px; }
</style>
