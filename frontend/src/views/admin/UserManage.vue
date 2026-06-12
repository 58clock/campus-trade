<template>
  <div class="container">
    <el-card>
      <template #header><h2>用户管理</h2></template>
      <!-- TODO: D - 搜索栏 + 状态筛选 -->
      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <!-- TODO: D - 封禁/解封/重置密码按钮 -->
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// TODO: D - 实现用户管理（列表、搜索、封禁/解封、重置密码）
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const users = ref([])

onMounted(async () => {
  try {
    const res = await adminApi.listUsers({ page: 1, size: 50 })
    users.value = res.data?.records || []
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
</style>
