<template>
  <div class="container">
    <!-- 商品详情 -->
    <el-row :gutter="24">
      <el-col :span="12">
        <!-- TODO: B - 图片轮播 -->
        <img :src="product.images?.[0]" class="main-img" />
      </el-col>
      <el-col :span="12">
        <h1>{{ product.title }}</h1>
        <p class="price">¥{{ product.price }}</p>
        <p class="original">原价 ¥{{ product.originalPrice }}</p>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="分类">{{ product.category }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ product.conditionLevel }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ product.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="浏览">{{ product.viewCount }}次</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <el-button type="danger" size="large" @click="handleBuy">我想要</el-button>
          <el-button size="large" @click="handleReport">举报</el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 商品描述 -->
    <el-card style="margin-top:16px">
      <template #header>商品描述</template>
      <p>{{ product.description }}</p>
    </el-card>

    <!-- 留言区 -->
    <el-card style="margin-top:16px">
      <template #header>留言咨询</template>
      <!-- TODO: C - 留言列表 + 发布留言表单 -->
    </el-card>
  </div>
</template>

<script setup>
// TODO: B (商品部分) + C (留言部分)
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { productApi, orderApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const product = ref({})

onMounted(async () => {
  try {
    const res = await productApi.getById(route.params.id)
    product.value = res.data
  } catch { /* handled */ }
})

async function handleBuy() {
  // TODO: C - 创建订单
  try {
    await orderApi.create(product.value.id)
    ElMessage.success('已下单，请前往支付')
  } catch { /* handled */ }
}

function handleReport() { /* TODO: D - 举报弹窗 */ }
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
.main-img { width: 100%; height: 400px; object-fit: cover; border-radius: 8px; }
.price { color: #f56c6c; font-size: 28px; font-weight: bold; }
.original { color: #999; text-decoration: line-through; }
</style>
