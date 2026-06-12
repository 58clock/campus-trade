<template>
  <div class="container">
    <el-card>
      <template #header><h2>举报处理</h2></template>
      <!-- TODO: D - 状态筛选 -->
      <el-table :data="reports" stripe>
        <el-table-column prop="reporterName" label="举报人" width="100" />
        <el-table-column prop="targetType" label="类型" width="100" />
        <el-table-column prop="targetId" label="目标ID" width="80" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'RESOLVED' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <!-- TODO: D - 处理/驳回按钮 -->
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// TODO: D - 实现举报管理
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const reports = ref([])

onMounted(async () => {
  try {
    const res = await adminApi.listReports({ page: 1, size: 50 })
    reports.value = res.data?.records || []
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
</style>
