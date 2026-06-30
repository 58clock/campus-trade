<template>
  <div class="container">
    <el-card>
      <template #header><h2>我的发布</h2></template>
      <el-table :data="products" stripe v-loading="loading">
        <el-table-column prop="title" label="标题" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="180" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" :disabled="row.status !== 'ON_SALE'" @click="handleOffShelf(row)">下架</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑商品" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="editForm.category" style="width:100%">
            <el-option label="书籍" value="BOOK" />
            <el-option label="电子产品" value="ELECTRONICS" />
            <el-option label="生活用品" value="LIFESTYLE" />
            <el-option label="运动用品" value="SPORTS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="成色" required>
          <el-select v-model="editForm.conditionLevel" style="width:100%">
            <el-option label="全新" value="NEW" />
            <el-option label="几乎全新" value="LIKE_NEW" />
            <el-option label="良好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价" required>
          <el-input-number v-model="editForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="editForm.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { productApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const products = ref([])
const loading = ref(false)
const editDialogVisible = ref(false)
const submitting = ref(false)
const currentProductId = ref(null)
const editForm = ref({
  title: '', category: '', conditionLevel: '',
  price: null, originalPrice: null, description: ''
})

const statusMap = {
  ON_SALE:   { label: '在售',   type: 'success' },
  OFF_SHELF: { label: '已下架',  type: 'warning' },
  SOLD:      { label: '已售出',  type: 'info' }
}
function statusTagType(s) { return statusMap[s]?.type || 'info' }
function statusLabel(s)   { return statusMap[s]?.label || s }

async function fetchProducts() {
  loading.value = true
  try {
    const res = await productApi.myProducts({ page: 1, size: 50 })
    products.value = res.data?.records || []
  } finally { loading.value = false }
}
onMounted(fetchProducts)

function handleEdit(row) {
  currentProductId.value = row.id
  editForm.value = {
    title: row.title,
    category: row.category,
    conditionLevel: row.conditionLevel || '',
    price: row.price,
    originalPrice: row.originalPrice,
    description: row.description || ''
  }
  editDialogVisible.value = true
}

async function handleUpdate() {
  if (!editForm.value.title) return ElMessage.warning('请输入标题')
  if (!editForm.value.category) return ElMessage.warning('请选择分类')
  if (!editForm.value.conditionLevel) return ElMessage.warning('请选择成色')
  if (editForm.value.price === null || editForm.value.price <= 0) return ElMessage.warning('请输入有效价格')
  submitting.value = true
  try {
    await productApi.update(currentProductId.value, editForm.value)
    ElMessage.success('编辑成功')
    editDialogVisible.value = false
    await fetchProducts()
  } finally { submitting.value = false }
}

async function handleOffShelf(row) {
  try {
    await ElMessageBox.confirm('确认下架该商品？', '提示', { type: 'warning' })
    await productApi.offShelf(row.id)
    ElMessage.success('已下架')
    await fetchProducts()
  } catch { /* 取消或错误 */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该商品？此操作不可恢复。', '提示', { type: 'warning' })
    await productApi.delete(row.id)
    ElMessage.success('已删除')
    await fetchProducts()
  } catch { /* 取消或错误 */ }
}
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
</style>