<template>
  <div class="container">
    <el-card>
      <template #header><h2>我的发布</h2></template>
      <el-table :data="products" stripe>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="price" label="价格" width="100" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="createdAt" label="发布时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <!-- TODO: B - 编辑/下架/删除按钮 -->
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// TODO: B - 实现我的发布列表，操作按钮
import { ref, onMounted } from 'vue'
import { productApi } from '@/api'

const products = ref([])

onMounted(async () => {
  try {
    const res = await productApi.myProducts({ page: 1, size: 50 })
    products.value = res.data?.records || []
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
</style>
