<template>
  <div class="container">
    <el-card>
      <template #header><h2>商品审核</h2></template>

      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item>
          <el-input v-model="keyword" placeholder="搜索商品标题" clearable style="width:240px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:140px" @change="search">
            <el-option label="在售" value="ON_SALE" />
            <el-option label="锁定" value="LOCKED" />
            <el-option label="已下架" value="OFF_SHELF" />
            <el-option label="已删除" value="DELETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="products" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="sellerName" label="卖家" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ON_SALE' || row.status === 'LOCKED'" type="warning" size="small" @click="handleOffShelf(row)">
              强制下架
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
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
        @current-change="fetchProducts"
        @size-change="fetchProducts"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const products = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref(null)

const statusLabelMap = { ON_SALE: '在售', LOCKED: '锁定', OFF_SHELF: '已下架', DELETED: '已删除' }
const statusTagTypeMap = { ON_SALE: 'success', LOCKED: 'warning', OFF_SHELF: 'info', DELETED: 'danger' }

function statusLabel(s) { return statusLabelMap[s] || s }
function statusTagType(s) { return statusTagTypeMap[s] || 'info' }

onMounted(() => fetchProducts())

function fetchProducts() {
  loading.value = true
  adminApi.listProducts({ page: page.value, size: size.value, keyword: keyword.value || undefined, status: statusFilter.value })
    .then(res => {
      products.value = res.data?.records || []
      total.value = res.data?.total || 0
    })
    .finally(() => { loading.value = false })
}

function search() {
  page.value = 1
  fetchProducts()
}

function resetSearch() {
  keyword.value = ''
  statusFilter.value = null
  page.value = 1
  fetchProducts()
}

function handleOffShelf(row) {
  ElMessageBox.confirm(`确定要强制下架商品「${row.title}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await adminApi.forceOffShelf(row.id)
    ElMessage.success('已下架')
    fetchProducts()
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除商品「${row.title}」吗？此操作不可恢复。`, '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error',
  }).then(async () => {
    await adminApi.deleteProduct(row.id)
    ElMessage.success('已删除')
    fetchProducts()
  }).catch(() => {})
}
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
h2 { margin: 0; }
</style>
