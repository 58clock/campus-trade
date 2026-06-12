<template>
  <div class="container">
    <h2>数据统计面板</h2>

    <!-- 概览卡片 -->
    <el-row :gutter="16" style="margin-bottom:24px">
      <el-col :span="6">
        <el-statistic-card title="商品总数" :value="stats.totalProducts" />
      </el-col>
      <el-col :span="6">
        <el-statistic-card title="订单总数" :value="stats.totalOrders" />
      </el-col>
      <el-col :span="6">
        <el-statistic-card title="用户数" :value="stats.totalUsers" />
      </el-col>
      <el-col :span="6">
        <el-statistic-card title="总交易额" :value="'¥' + (stats.totalSales || 0)" />
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>近一周交易额</template>
          <div ref="weeklyChart" style="height:300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>订单状态分布</template>
          <div ref="statusChart" style="height:300px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// TODO: D - 实现 ECharts 图表 + 数据获取
import { ref, reactive, onMounted, nextTick } from 'vue'
import { adminApi } from '@/api'

const stats = reactive({ totalProducts: 0, totalOrders: 0, totalUsers: 0, totalSales: 0 })
const weeklyChart = ref(null)
const statusChart = ref(null)

onMounted(async () => {
  try {
    const res = await adminApi.getStatistics()
    Object.assign(stats, res.data)
    // TODO: D - 初始化 ECharts 图表
    await nextTick()
    renderCharts(res.data)
  } catch { /* handled */ }
})

function renderCharts(data) {
  // TODO: D - 使用 ECharts 渲染 weeklySales 折线图和 orderStatusDistribution 饼图
}
</script>

<style scoped>
.container { max-width: 1200px; margin: 24px auto; padding: 0 16px; }
</style>
