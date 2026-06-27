<template>
  <div>
    <el-card>
      <template #header><h2>订单管理</h2></template>

      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item>
          <el-input v-model="keyword" placeholder="搜索商品标题" clearable style="width:200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:140px" @change="search">
            <el-option label="待支付" value="PENDING_PAYMENT" />
            <el-option label="待发货" value="PENDING_SHIPMENT" />
            <el-option label="待收货" value="PENDING_RECEIPT" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="orders" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="productTitle" label="商品" min-width="120" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="买家" width="90" />
        <el-table-column prop="sellerName" label="卖家" width="90" />
        <el-table-column prop="amount" label="金额" width="90">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'CANCELLED' && row.status !== 'COMPLETED'"
              type="danger" size="small" @click="handleCancel(row)">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top:16px;justify-content:flex-end"
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[12, 24, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchOrders"
        @size-change="fetchOrders"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref(null)

const statusLabelMap = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: '待发货',
  PENDING_RECEIPT: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const statusTagTypeMap = {
  PENDING_PAYMENT: 'warning',
  PENDING_SHIPMENT: 'primary',
  PENDING_RECEIPT: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger',
}

function statusLabel(s) { return statusLabelMap[s] || s }
function statusTagType(s) { return statusTagTypeMap[s] || 'info' }

onMounted(() => fetchOrders())

function fetchOrders() {
  loading.value = true
  adminApi.listOrders({
    page: page.value,
    size: size.value,
    keyword: keyword.value || undefined,
    status: statusFilter.value,
  }).then(res => {
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  }).finally(() => { loading.value = false })
}

function search() {
  page.value = 1
  fetchOrders()
}

function resetSearch() {
  keyword.value = ''
  statusFilter.value = null
  page.value = 1
  fetchOrders()
}

function handleCancel(row) {
  ElMessageBox.confirm(`确定要取消订单 #${row.id}「${row.productTitle}」吗？`, '警告', {
    confirmButtonText: '确定取消',
    cancelButtonText: '返回',
    type: 'error',
  }).then(async () => {
    await adminApi.cancelOrder(row.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  }).catch(() => {})
}
</script>

<style scoped>
h2 { margin: 0; }
</style>
