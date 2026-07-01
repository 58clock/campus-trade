<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header><h2>忘记密码</h2></template>
      <!-- 步骤1: 发送验证码 -->
      <el-form v-if="step === 1" ref="formRef1" :model="form" :rules="rules1" label-width="80px" @keyup.enter="sendCode">
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="sendCode">发送验证码</el-button>
        </el-form-item>
      </el-form>
      <!-- 步骤2: 重置密码 -->
      <el-form v-else ref="formRef2" :model="form" :rules="rules2" label-width="80px" @keyup.enter="resetPassword">
        <el-form-item label="验证码" prop="code">
          <el-input v-model="form.code" placeholder="请输入6位验证码" maxlength="6" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="6-30位字符" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="resetPassword">重置密码</el-button>
        </el-form-item>
        <div class="step-hint">
          <a href="javascript:void(0)" @click="step = 1">← 返回修改邮箱</a>
          <span v-if="countdown > 0" class="countdown">{{ countdown }}s 后可重发</span>
          <a v-else href="javascript:void(0)" @click="sendCode">重新发送验证码</a>
        </div>
      </el-form>
      <div class="auth-links">
        <router-link to="/login">返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const step = ref(1)
const loading = ref(false)
const countdown = ref(0)
const formRef1 = ref(null)
const formRef2 = ref(null)
const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const rules1 = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules2 = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度6-30位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

function startCountdown() {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function sendCode() {
  const ref = step.value === 1 ? formRef1 : formRef2
  const valid = ref ? await ref.value.validate().catch(() => false) : true
  if (!valid) return

  loading.value = true
  try {
    await authApi.sendResetCode(form.email)
    ElMessage.success('验证码已发送')
    step.value = 2
    startCountdown()
  } catch {
    // 拦截器已统一处理
  } finally {
    loading.value = false
  }
}

async function resetPassword() {
  const valid = await formRef2.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.resetPassword({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码重置成功，请登录')
    router.push('/login')
  } catch {
    // 拦截器已统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 60px); }
.auth-card { width: 420px; }
.auth-links { display: flex; justify-content: center; margin-top: 12px; }
.step-hint { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.countdown { color: #999; }
</style>
