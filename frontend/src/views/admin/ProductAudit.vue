<template>
  <div class="container">
    <el-card>
      <template #header><h2>商品审核</h2></template>
      <!-- TODO: D - 搜索栏 + 状态筛选 -->
      <el-table :data="products" stripe>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="price" label="价格" width="100" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="200">
          <template #default>
            <!-- TODO: D - 强制下架/删除按钮 -->
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// TODO: D - 实现商品审核
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const products = ref([])

onMounted(async () => {
  try {
    const res = await adminApi.listProducts({ page: 1, size: 50 })
    products.value = res.data?.records || []
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
</style>
