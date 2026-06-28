<template>
  <div class="container">
    <el-card>
      <template #header><h2>我的举报</h2></template>

      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="targetType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.targetType === 'PRODUCT' ? '商品' : row.targetType === 'USER' ? '用户' : row.targetType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID" width="80" />
        <el-table-column prop="reason" label="举报原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerNote" label="处理备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.handlerNote">{{ row.handlerNote }}</span>
            <span v-else style="color:#c0c4cc">等待处理</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="160" />
      </el-table>

      <el-empty v-if="!loading && reports.length === 0" description="暂无举报记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reportApi } from '@/api'

const reports = ref([])
const loading = ref(false)

const statusLabelMap = { PENDING: '待处理', RESOLVED: '已处理', DISMISSED: '已驳回' }
const statusTagTypeMap = { PENDING: 'warning', RESOLVED: 'success', DISMISSED: 'info' }
function statusLabel(s) { return statusLabelMap[s] || s }
function statusTagType(s) { return statusTagTypeMap[s] || 'info' }

onMounted(async () => {
  loading.value = true
  try {
    const res = await reportApi.getMyReports()
    reports.value = res.data || []
  } catch { /* handled */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.container { max-width: 900px; margin: 24px auto; padding: 0 16px; }
h2 { margin: 0; }
</style>
