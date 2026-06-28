<template>
  <div>
    <h2 style="margin-bottom:16px">数据统计面板</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:24px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#e6f7ff"><el-icon size="28" color="#1890ff"><Goods /></el-icon></div>
            <div class="stat-body">
              <div class="stat-label">商品总数</div>
              <div class="stat-value">{{ stats.totalProducts || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#f6ffed"><el-icon size="28" color="#52c41a"><Document /></el-icon></div>
            <div class="stat-body">
              <div class="stat-label">订单总数</div>
              <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#fff7e6"><el-icon size="28" color="#fa8c16"><User /></el-icon></div>
            <div class="stat-body">
              <div class="stat-label">用户数</div>
              <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#fff1f0"><el-icon size="28" color="#f5222d"><Money /></el-icon></div>
            <div class="stat-body">
              <div class="stat-label">总交易额</div>
              <div class="stat-value">¥{{ (stats.totalSales || 0).toLocaleString() }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card>
          <template #header>近一周交易额</template>
          <div ref="weeklyChart" style="height:320px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>订单状态分布</template>
          <div ref="statusChart" style="height:320px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="14">
        <el-card>
          <template #header>分类商品分布</template>
          <div ref="categoryChart" style="height:280px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>快速概览</template>
          <div style="padding:16px">
            <el-progress
              v-for="item in statusProgress" :key="item.label"
              :percentage="item.percent"
              :color="item.color"
              :stroke-width="16"
              style="margin-bottom:16px"
            >
              <span>{{ item.label }} — {{ item.count }}</span>
            </el-progress>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'
import { Goods, Document, User, Money } from '@element-plus/icons-vue'

const stats = reactive({ totalProducts: 0, totalOrders: 0, totalUsers: 0, totalSales: 0 })
const weeklyChart = ref(null)
const statusChart = ref(null)
const categoryChart = ref(null)

let weeklyInstance = null
let statusInstance = null
let categoryInstance = null

const statusLabelMap = {
  PENDING_PAYMENT: '待支付', PENDING_SHIPMENT: '待发货',
  PENDING_RECEIPT: '待收货', COMPLETED: '已完成', CANCELLED: '已取消',
}
const statusColors = {
  PENDING_PAYMENT: '#e6a23c', PENDING_SHIPMENT: '#409eff',
  PENDING_RECEIPT: '#909399', COMPLETED: '#67c23a', CANCELLED: '#f56c6c',
}

const statusProgress = computed(() => {
  const dist = stats.orderStatusDistribution || {}
  const total = Object.values(dist).reduce((a, b) => a + b, 0) || 1
  return Object.entries(dist).map(([key, count]) => ({
    label: statusLabelMap[key] || key,
    count,
    percent: Math.round((count / total) * 100),
    color: statusColors[key] || '#909399',
  }))
})

function handleResize() {
  weeklyInstance?.resize()
  statusInstance?.resize()
  categoryInstance?.resize()
}

onMounted(async () => {
  try {
    const res = await adminApi.getStatistics()
    Object.assign(stats, res.data)
    await nextTick()
    renderCharts(res.data)
    window.addEventListener('resize', handleResize)
  } catch { /* handled */ }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  weeklyInstance?.dispose()
  statusInstance?.dispose()
  categoryInstance?.dispose()
})

function renderCharts(data) {
  if (weeklyChart.value) {
    weeklyInstance = echarts.init(weeklyChart.value)
    const weeklyData = data.weeklySales || []
    weeklyInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: weeklyData.map(d => d.day) },
      yAxis: { type: 'value' },
      series: [{
        name: '交易额', type: 'line', data: weeklyData.map(d => d.total),
        smooth: true, areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#409EFF' },
      }],
    })
  }

  if (statusChart.value) {
    statusInstance = echarts.init(statusChart.value)
    const statusData = data.orderStatusDistribution || {}
    const pieData = Object.entries(statusData).map(([key, value]) => ({
      name: statusLabelMap[key] || key, value,
      itemStyle: { color: statusColors[key] },
    }))
    statusInstance.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 10 },
      series: [{
        type: 'pie', radius: ['45%', '75%'], center: ['50%', '45%'],
        data: pieData,
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
      }],
    })
  }

  if (categoryChart.value) {
    categoryInstance = echarts.init(categoryChart.value)
    const catData = data.categoryDistribution || {}
    const catLabelMap = { BOOK: '书籍', ELECTRONICS: '电子产品', LIFESTYLE: '生活用品', SPORTS: '运动用品', OTHER: '其他' }
    const barData = Object.entries(catData).map(([key, value]) => ({
      name: catLabelMap[key] || key, value,
    }))
    categoryInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: barData.map(d => d.name) },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar', data: barData.map(d => d.value),
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
      }],
    })
  }
}
</script>

<style scoped>
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
</style>
