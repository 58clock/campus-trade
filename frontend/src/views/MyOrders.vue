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

      <el-table :data="orders" stripe>
        <el-table-column prop="productTitle" label="商品" />
        <el-table-column prop="amount" label="金额" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <template v-if="tab === 'bought'">
              <el-button v-if="scope.row.status === 'PENDING_PAYMENT'" type="primary" size="small" @click="handlePay(scope.row.id)">立即付款</el-button>
              <el-button v-if="scope.row.status === 'PENDING_PAYMENT' || scope.row.status === 'PENDING_SHIPMENT'" size="small" @click="handleCancel(scope.row.id)">取消订单</el-button>
              <el-button v-if="scope.row.status === 'PENDING_RECEIPT'" type="success" size="small" @click="handleReceive(scope.row.id)">确认收货</el-button>
              <el-button v-if="scope.row.status === 'COMPLETED'" type="warning" size="small" @click="handleReview(scope.row.id)">去评价</el-button>
            </template>

            <template v-else>
              <el-button v-if="scope.row.status === 'PENDING_SHIPMENT'" type="primary" size="small" @click="handleShip(scope.row.id)">确认发货</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tab = ref('bought')
const orders = ref([])

onMounted(fetchOrders)

async function fetchOrders() {
  const api = tab.value === 'bought' ? orderApi.bought : orderApi.sold
  const res = await api({ page: 1, size: 50 })
  orders.value = res.data?.records || []
}

// 操作方法
async function handlePay(id) {
  await orderApi.pay(id);
  fetchOrders();
}

async function handleShip(id) {
  await orderApi.ship(id);
  fetchOrders();
}

async function handleReceive(id) {
  await orderApi.receive(id);
  fetchOrders();
}

async function handleCancel(id) {
  try {
    await ElMessageBox.prompt('请输入取消原因', '取消订单', { confirmButtonText: '确定', cancelButtonText: '取消' })
    await orderApi.cancel(id, { reason: '用户主动取消' })
    fetchOrders()
  } catch {}
}

// 辅助：根据状态显示不同颜色的 Tag
function getStatusType(status) {
  const map = { PENDING_PAYMENT: 'info', PENDING_SHIPMENT: 'warning', PENDING_RECEIPT: 'primary', COMPLETED: 'success', CANCELLED: 'danger' }
  return map[status] || 'info'
}
</script>