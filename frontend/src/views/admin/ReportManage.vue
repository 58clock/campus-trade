<template>
  <div class="container">
    <el-card>
      <template #header><h2>举报处理</h2></template>

      <el-form :inline="true" style="margin-bottom:16px">
        <el-form-item>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:140px" @change="search">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="RESOLVED" />
            <el-option label="已驳回" value="DISMISSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="reporterName" label="举报人" width="100" />
        <el-table-column prop="targetType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.targetType === 'product' ? '商品' : row.targetType === 'user' ? '用户' : row.targetType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID" width="80" />
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'RESOLVED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PENDING' ? '待处理' : row.status === 'RESOLVED' ? '已处理' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerNote" label="处理备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" @click="handleResolve(row)">标记已处理</el-button>
              <el-button type="danger" size="small" @click="handleDismiss(row)">驳回</el-button>
            </template>
            <span v-else style="color:#909399">已处理</span>
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
        @current-change="fetchReports"
        @size-change="fetchReports"
      />
    </el-card>

    <!-- 处理备注弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-input v-model="note" type="textarea" :rows="3" placeholder="请输入处理备注（可选）" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const reports = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const statusFilter = ref(null)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const note = ref('')
const currentRow = ref(null)
const currentAction = ref('')

onMounted(() => fetchReports())

function fetchReports() {
  loading.value = true
  adminApi.listReports({ page: page.value, size: size.value, status: statusFilter.value })
    .then(res => {
      reports.value = res.data?.records || []
      total.value = res.data?.total || 0
    })
    .finally(() => { loading.value = false })
}

function search() {
  page.value = 1
  fetchReports()
}

function resetSearch() {
  statusFilter.value = null
  page.value = 1
  fetchReports()
}

function handleResolve(row) {
  currentRow.value = row
  currentAction.value = 'resolve'
  dialogTitle.value = '标记为已处理'
  note.value = ''
  dialogVisible.value = true
}

function handleDismiss(row) {
  currentRow.value = row
  currentAction.value = 'dismiss'
  dialogTitle.value = '驳回举报'
  note.value = ''
  dialogVisible.value = true
}

async function confirmHandle() {
  await adminApi.handleReport(currentRow.value.id, currentAction.value, note.value || undefined)
  ElMessage.success(currentAction.value === 'resolve' ? '已标记为处理' : '已驳回')
  dialogVisible.value = false
  fetchReports()
}
</script>

<style scoped>
.container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
h2 { margin: 0; }
</style>
