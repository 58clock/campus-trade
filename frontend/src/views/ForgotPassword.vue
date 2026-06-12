<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header><h2>忘记密码</h2></template>
      <!-- 步骤1: 发送验证码 -->
      <el-form v-if="step === 1" :model="form" label-width="100px">
        <el-form-item label="注册邮箱">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" @click="sendCode">发送验证码</el-button>
        </el-form-item>
      </el-form>
      <!-- 步骤2: 重置密码 -->
      <el-form v-else :model="form" label-width="100px">
        <el-form-item label="验证码">
          <el-input v-model="form.code" placeholder="请输入6位验证码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" placeholder="6-30位字符" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" @click="resetPassword">重置密码</el-button>
        </el-form-item>
      </el-form>
      <div class="auth-links">
        <router-link to="/login">返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// TODO: A - 实现忘记密码流程
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const step = ref(1)
const form = reactive({ email: '', code: '', newPassword: '' })

async function sendCode() {
  try {
    await authApi.sendResetCode(form.email)
    ElMessage.success('验证码已发送，请查看控制台')
    step.value = 2
  } catch { /* handled */ }
}

async function resetPassword() {
  try {
    await authApi.resetPassword(form)
    ElMessage.success('密码重置成功')
    router.push('/login')
  } catch { /* handled */ }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 60px); }
.auth-card { width: 420px; }
.auth-links { display: flex; justify-content: center; margin-top: 12px; }
</style>
