<template>
  <div class="container">
    <el-card>
      <template #header><h2>我的消息</h2></template>

      <el-list v-if="messages.length > 0">
        <el-list-item v-for="msg in messages" :key="msg.id" class="message-item">
          <div class="msg-content">
            <p><strong>{{ msg.senderName || '用户' + msg.senderId }}:</strong> {{ msg.content }}</p>
            <small>{{ msg.createdAt }}</small>

            <div class="actions">
              <el-button type="text" @click="openReply(msg)">回复</el-button>
            </div>
          </div>
        </el-list-item>
      </el-list>

      <el-empty v-else description="暂无消息" />
    </el-card>

    <el-dialog v-model="replyVisible" title="回复留言" width="400px">
      <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="输入回复内容..." />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { messageApi } from '@/api'
import { ElMessage } from 'element-plus'

const messages = ref([])
const replyVisible = ref(false)
const replyContent = ref('')
const currentParent = ref(null)

onMounted(fetchMessages)

async function fetchMessages() {
  try {
    const res = await messageApi.myMessages()
    messages.value = res.data || []
  } catch (e) {
    ElMessage.error('获取消息失败')
  }
}

function openReply(msg) {
  currentParent.value = msg
  replyContent.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyContent.value.trim()) return
  try {
    await messageApi.replyToMessage({
      parentId: currentParent.value.id,
      content: replyContent.value
    })
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchMessages() // 刷新列表
  } catch (e) {
    ElMessage.error('回复失败')
  }
}
</script>

<style scoped>
.container { max-width: 800px; margin: 24px auto; padding: 0 16px; }
.message-item { padding: 16px 0; border-bottom: 1px solid #eee; }
.actions { margin-top: 8px; }
</style>