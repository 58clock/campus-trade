<template>
  <div class="container">
    <el-card>
      <template #header><h2>个人中心</h2></template>
      <el-row :gutter="24">
        <!-- 头像区域 -->
        <el-col :span="8" style="text-align:center">
          <el-avatar :size="120" :src="user?.avatar" />
          <div style="margin-top:12px">
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload">
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>
        </el-col>
        <!-- 信息区域 -->
        <el-col :span="16">
          <el-form :model="form" label-width="80px">
            <el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
            <el-form-item label="学校"><el-input v-model="form.school" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
// TODO: A - 实现个人信息管理
import { reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const user = reactive({ avatar: '' })
const form = reactive({ username: '', nickname: '', school: '', phone: '', email: '' })

onMounted(async () => {
  try {
    const res = await userApi.getProfile()
    Object.assign(form, res.data)
    Object.assign(user, res.data)
  } catch { /* handled */ }
})

async function handleSave() {
  try {
    await userApi.updateProfile(form)
    userStore.setUser({ ...userStore.user, ...form })
    ElMessage.success('保存成功')
  } catch { /* handled */ }
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
</script>

<style scoped>
.container { max-width: 800px; margin: 24px auto; padding: 0 16px; }
</style>
