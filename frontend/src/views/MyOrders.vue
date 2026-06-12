<template>
  <div class="container">
    <el-card>
      <template #header>
        <h2>我的订单</h2>
        <el-radio-group v-model="tab" @change="fetchOrders">
          <el-radio-button value="bought">我买的</el-radio-button>
          <el-radio-button value="sold">我卖的</el-radio-button>
        </el-radio-group>
      </template>
      <!-- TODO: C - 分状态展示订单列表 + 操作按钮 -->
      <el-table :data="orders" stripe>
        <el-table-column prop="productTitle" label="商品" />
        <el-table-column prop="amount" label="金额" width="100" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default>
            <!-- TODO: C - 付款/发货/收货/取消 按钮（根据状态显示不同按钮） -->
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// TODO: C - 实现我的订单（分买/卖 tab，分状态筛选，操作按钮）
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api'

const tab = ref('bought')
const orders = ref([])

onMounted(fetchOrders)
async function fetchOrders() {
  try {
    const api = tab.value === 'bought' ? orderApi.bought : orderApi.sold
    const res = await api({ page: 1, size: 50 })
    orders.value = res.data?.records || []
  } catch { /* handled */ }
}
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
</style>
