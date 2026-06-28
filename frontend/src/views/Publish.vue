<template>
  <div class="container">
    <el-card>
      <template #header><h2>发布商品</h2></template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入商品标题" maxlength="200" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="书籍" value="BOOK" />
            <el-option label="电子产品" value="ELECTRONICS" />
            <el-option label="生活用品" value="LIFESTYLE" />
            <el-option label="运动用品" value="SPORTS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="成色" required>
          <el-select v-model="form.conditionLevel" placeholder="请选择成色">
            <el-option label="全新" value="NEW" />
            <el-option label="几乎全新" value="LIKE_NEW" />
            <el-option label="良好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价" required>
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述商品详情" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload multiple :auto-upload="false" :on-change="handleImageChange" list-type="picture-card">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handlePublish">发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { productApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const form = reactive({
  title: '', category: '', conditionLevel: '',
  price: null, originalPrice: null, description: ''
})
const images = ref([])

function handleImageChange(file) { images.value.push(file.raw) }

async function handlePublish() {
  const fd = new FormData()
  fd.append('title', form.title)
  fd.append('category', form.category)
  fd.append('conditionLevel', form.conditionLevel)
  fd.append('price', form.price)
  fd.append('description', form.description || '')
  if (form.originalPrice) fd.append('originalPrice', form.originalPrice)
  images.value.forEach(f => fd.append('images', f))
  try {
    await productApi.create(fd)
    ElMessage.success('发布成功')
    router.push('/')
  } catch { /* handled */ }
}
</script>

<style scoped>
.container { max-width: 700px; margin: 24px auto; padding: 0 16px; }
</style>
