<template>
  <div class="container">
    <el-card>
      <template #header><h2>用户管理</h2></template>

      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item>
          <el-input v-model="keyword" placeholder="用户名/昵称/邮箱" clearable style="width:240px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:130px" @change="search">
            <el-option label="正常" :value="1" />
            <el-option label="封禁" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button v-if="row.role !== 'ADMIN'" :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleBan(row)">
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
            <el-button type="primary" size="small" @click="handleResetPassword(row)">
              重置密码
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
        @current-change="fetchUsers"
        @size-change="fetchUsers"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref(null)

onMounted(() => fetchUsers())

function fetchUsers() {
  loading.value = true
  adminApi.listUsers({ page: page.value, size: size.value, keyword: keyword.value || undefined, status: statusFilter.value })
    .then(res => {
      users.value = res.data?.records || []
      total.value = res.data?.total || 0
    })
    .finally(() => { loading.value = false })
}

function search() {
  page.value = 1
  fetchUsers()
}

function resetSearch() {
  keyword.value = ''
  statusFilter.value = null
  page.value = 1
  fetchUsers()
}

function toggleBan(row) {
  const isBan = row.status === 1
  const action = isBan ? '封禁' : '解封'
  ElMessageBox.confirm(`确定要${action}用户「${row.nickname || row.username}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    if (isBan) {
      await adminApi.banUser(row.id)
    } else {
      await adminApi.unbanUser(row.id)
    }
    ElMessage.success(`${action}成功`)
    fetchUsers()
  }).catch(() => {})
}

function handleResetPassword(row) {
  ElMessageBox.confirm(`确定要重置用户「${row.nickname || row.username}」的密码为 123456 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await adminApi.resetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  }).catch(() => {})
}
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
h2 { margin: 0; }
</style>
