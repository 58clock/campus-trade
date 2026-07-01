<template>
  <div class="container">
    <el-card>
      <template #header>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="个人信息" name="info" />
          <el-tab-pane label="信誉评价" name="reviews" />
        </el-tabs>
      </template>

      <!-- Tab 1: 个人信息 -->
      <div v-if="activeTab === 'info'">
        <el-row :gutter="24">
          <el-col :span="8" style="text-align:center">
            <el-avatar :size="120" :src="user.avatar" />
            <div style="margin-top:12px">
              <el-upload :show-file-list="false" :before-upload="handleAvatarUpload">
                <el-button size="small">更换头像</el-button>
              </el-upload>
            </div>
            <div class="reputation-badge">
              信誉分：<span class="score" :class="scoreClass">{{ user.reputationScore ?? '--' }}</span>
            </div>
          </el-col>
          <el-col :span="16">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
              <el-form-item label="用户名">
                <el-input v-model="form.username" disabled />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="form.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="学校">
                <el-input v-model="form.school" placeholder="请输入学校" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
      </div>

      <!-- Tab 2: 信誉评价 -->
      <div v-else v-loading="reviewLoading">
        <div class="reputation-header">
          <span>综合评价：</span>
          <span class="score large" :class="scoreClass">{{ reputation?.reputationScore ?? '--' }}</span>
        </div>
        <el-divider />
        <div v-if="reviews.length > 0">
          <div v-for="r in reviews" :key="r.id" class="review-item">
            <div class="review-top">
              <el-avatar :size="36" :src="r.reviewerAvatar" />
              <span class="reviewer-name">{{ r.reviewerName }}</span>
              <el-rate :model-value="r.rating" disabled show-score size="small" />
              <span class="review-date">{{ r.createdAt?.slice(0, 10) }}</span>
            </div>
            <div class="review-content">{{ r.content }}</div>
          </div>
          <el-pagination
            v-if="reviewTotal > reviewPageSize"
            layout="prev, pager, next"
            :total="reviewTotal"
            :page-size="reviewPageSize"
            v-model:current-page="reviewPage"
            @current-change="fetchReviews"
          />
        </div>
        <el-empty v-else description="暂无评价" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi, reviewApi } from '@/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('info')
const saving = ref(false)
const reviewLoading = ref(false)
const formRef = ref(null)

const user = reactive({ avatar: '', reputationScore: null })
const form = reactive({ username: '', nickname: '', school: '', phone: '', email: '' })
const reputation = ref(null)
const reviews = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewPageSize = 12

const rules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

const scoreClass = computed(() => {
  const s = user.reputationScore ?? reputation.value?.reputationScore
  if (s == null) return ''
  if (s >= 80) return 'score-high'
  if (s >= 60) return 'score-mid'
  return 'score-low'
})

onMounted(async () => {
  try {
    const res = await userApi.getProfile()
    Object.assign(form, res.data)
    Object.assign(user, res.data)
  } catch { /* handled */ }
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await userApi.updateProfile(form)
    userStore.setUser({ ...userStore.user, ...form })
    ElMessage.success('保存成功')
  } catch {
    // 拦截器已统一处理
  } finally {
    saving.value = false
  }
}

async function handleAvatarUpload(file) {
  try {
    const res = await userApi.uploadAvatar(file)
    user.avatar = res.data
    userStore.setUser({ ...userStore.user, avatar: res.data })
    ElMessage.success('头像更新成功')
  } catch { /* handled */ }
  return false
}

async function fetchReputation() {
  const userId = userStore.user?.id
  if (!userId) return
  try {
    const res = await reviewApi.getUserReputation(userId)
    reputation.value = res.data
  } catch { /* handled */ }
}

async function fetchReviews() {
  const userId = userStore.user?.id
  if (!userId) return
  reviewLoading.value = true
  try {
    const res = await reviewApi.getUserReviews(userId, { page: reviewPage.value, size: reviewPageSize })
    reviews.value = res.data.records
    reviewTotal.value = res.data.total
  } catch { /* handled */ }
  finally {
    reviewLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'reviews') {
    fetchReputation()
    fetchReviews()
  }
})
</script>

<style scoped>
.container { max-width: 800px; margin: 24px auto; padding: 0 16px; }
.reputation-badge { margin-top: 16px; font-size: 14px; }
.score { font-weight: bold; font-size: 18px; }
.score.large { font-size: 28px; }
.score-high { color: #67c23a; }
.score-mid { color: #e6a23c; }
.score-low { color: #f56c6c; }
.reputation-header { display: flex; align-items: center; gap: 8px; font-size: 16px; margin-top: 8px; }
.review-item { padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.review-item:last-child { border-bottom: none; }
.review-top { display: flex; align-items: center; gap: 10px; }
.reviewer-name { font-weight: 500; }
.review-date { color: #999; font-size: 13px; margin-left: auto; }
.review-content { margin-top: 8px; margin-left: 46px; color: #606266; font-size: 14px; }
</style>
