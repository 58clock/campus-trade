<template>
  <div class="container">
    <el-card>
      <template #header><h2>消息中心</h2></template>
      <!-- TODO: C - 私信会话列表 -->
      <el-empty description="暂无消息">
        <template #default>
          <p>点击商品详情页留言区与买卖家沟通</p>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
// TODO: C - 实现消息列表
import { ref, onMounted } from 'vue'
import { messageApi } from '@/api'

const messages = ref([])

onMounted(async () => {
  try {
    const res = await messageApi.myMessages()
    messages.value = res.data || []
  } catch { /* handled */ }
})
</script>

<style scoped>
.container { max-width: 800px; margin: 24px auto; padding: 0 16px; }
</style>
