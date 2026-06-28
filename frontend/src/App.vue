<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <el-menu
      mode="horizontal"
      :ellipsis="false"
      router
    >
      <el-menu-item index="/">
        <el-icon><Shop /></el-icon>
        <span>校园二手交易</span>
      </el-menu-item>

      <div class="flex-grow" />

      <template v-if="userStore.isLoggedIn">
        <el-menu-item index="/publish">发布</el-menu-item>
        <el-menu-item index="/messages">消息</el-menu-item>
        <el-sub-menu>
          <template #title>
            <el-avatar :size="24" :src="userStore.user?.avatar" />
            <span style="margin-left:8px">{{ userStore.user?.nickname }}</span>
          </template>
          <el-menu-item index="/profile">个人中心</el-menu-item>
          <el-menu-item index="/my-products">我的发布</el-menu-item>
          <el-menu-item index="/my-orders">我的订单</el-menu-item>
          <el-menu-item index="/my-reports">我的举报</el-menu-item>
          <el-menu-item index="/skills/recommend">AI 推荐</el-menu-item>
          <el-menu-item index="/skills/pricing">定价建议</el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/dashboard">后台管理</el-menu-item>
          <el-menu-item @click="handleLogout">退出登录</el-menu-item>
        </el-sub-menu>
      </template>

      <template v-else>
        <el-menu-item index="/login">登录</el-menu-item>
        <el-menu-item index="/register">注册</el-menu-item>
      </template>
    </el-menu>

    <!-- 主内容区 -->
    <router-view />
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { Shop } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.flex-grow { flex-grow: 1; }
#app { min-height: 100vh; background: #f5f7fa; }
</style>
