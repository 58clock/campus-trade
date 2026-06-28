<template>
  <div class="container">
    <!-- 商品详情 -->
    <el-row :gutter="24">
      <el-col :span="12">
        <img :src="product.images?.[0] || '/placeholder.png'" class="main-img" />
      </el-col>
      <el-col :span="12">
        <h1>{{ product.title }}</h1>
        <p class="price">¥{{ product.price }}</p>
        <p class="original">原价 ¥{{ product.originalPrice }}</p>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="分类">{{ product.category }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ product.conditionLevel }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ product.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="浏览">{{ product.viewCount }}次</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <el-button type="danger" size="large" @click="handleBuy">我想要</el-button>
          <el-button size="large" @click="openReportDialog">举报</el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 商品描述 -->
    <el-card style="margin-top:16px">
      <template #header>商品描述</template>
      <p>{{ product.description || '暂无描述' }}</p>
    </el-card>

    <!-- 留言区 -->
    <el-card style="margin-top:16px">
      <template #header>留言咨询 ({{ messages.length }})</template>

      <!-- 发布留言 -->
      <div v-if="userStore.isLoggedIn" style="margin-bottom:16px">
        <el-input v-model="newMsg" type="textarea" :rows="2" placeholder="输入留言..." />
        <el-button type="primary" size="small" style="margin-top:8px" @click="handleSendMsg">发布</el-button>
      </div>
      <el-alert v-else title="登录后即可留言" type="info" :closable="false" style="margin-bottom:16px" />

      <!-- 留言列表 -->
      <div v-if="messages.length > 0">
        <div v-for="m in messages" :key="m.id" class="msg-item">
          <div class="msg-header">
            <span class="msg-sender">{{ m.senderName }}</span>
            <span class="msg-time">{{ m.createdAt }}</span>
          </div>
          <p class="msg-content">{{ m.content }}</p>
          <el-button v-if="userStore.isLoggedIn" text size="small" type="primary" @click="toggleReply(m)">回复</el-button>

          <!-- 回复输入框 -->
          <div v-if="replyTarget === m.id" style="margin:8px 0 0 24px">
            <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="回复 {{ m.senderName }}..." />
            <el-button size="small" type="primary" style="margin-top:4px" @click="handleReply(m.id)">发送</el-button>
            <el-button size="small" style="margin-top:4px" @click="replyTarget = null">取消</el-button>
          </div>

          <!-- 子回复 -->
          <div v-if="m.replies?.length" style="margin-left:24px; border-left:2px solid #eee; padding-left:12px">
            <div v-for="r in m.replies" :key="r.id" class="msg-item" style="background:#f9f9f9">
              <div class="msg-header">
                <span class="msg-sender">{{ r.senderName }}</span>
                <span class="msg-time">{{ r.createdAt }}</span>
              </div>
              <p class="msg-content">{{ r.content }}</p>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无留言" :image-size="60" />
    </el-card>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportVisible" title="举报商品" width="420px">
      <el-form>
        <el-form-item label="举报类型">
          <el-select v-model="reportType" placeholder="请选择">
            <el-option label="商品信息不实" value="PRODUCT" />
            <el-option label="违规内容" value="PRODUCT" />
            <el-option label="卖家欺诈" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报原因">
          <el-input v-model="reportReason" type="textarea" :rows="3" placeholder="请描述举报原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { productApi, orderApi, messageApi, reportApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const product = ref({})
const messages = ref([])
const newMsg = ref('')
const replyTarget = ref(null)
const replyContent = ref('')
const reportVisible = ref(false)
const reportType = ref('PRODUCT')
const reportReason = ref('')

onMounted(async () => {
  try {
    const [pRes, mRes] = await Promise.all([
      productApi.getById(route.params.id),
      messageApi.getProductMessages(route.params.id),
    ])
    product.value = pRes.data
    messages.value = mRes.data || []
  } catch { /* handled */ }
})

async function handleBuy() {
  try {
    await orderApi.create(product.value.id)
    ElMessage.success('已下单，请前往支付')
  } catch { /* handled */ }
}

async function handleSendMsg() {
  if (!newMsg.value.trim()) return
  try {
    await messageApi.sendMessage(product.value.id, newMsg.value)
    ElMessage.success('留言成功')
    newMsg.value = ''
    refreshMessages()
  } catch { /* handled */ }
}

function toggleReply(m) {
  replyTarget.value = replyTarget.value === m.id ? null : m.id
  replyContent.value = ''
}

async function handleReply(msgId) {
  if (!replyContent.value.trim()) return
  try {
    await messageApi.reply(msgId, replyContent.value)
    ElMessage.success('回复成功')
    replyTarget.value = null
    replyContent.value = ''
    refreshMessages()
  } catch { /* handled */ }
}

function openReportDialog() {
  reportReason.value = ''
  reportVisible.value = true
}

async function handleReport() {
  if (!reportReason.value.trim()) return ElMessage.warning('请填写举报原因')
  try {
    await reportApi.submit(reportType.value, product.value.id, reportReason.value)
    ElMessage.success('举报已提交')
    reportVisible.value = false
  } catch { /* handled */ }
}

async function refreshMessages() {
  try {
    const res = await messageApi.getProductMessages(route.params.id)
    messages.value = res.data || []
  } catch { /* handled */ }
}
</script>

<style scoped>
.container { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
.main-img { width: 100%; height: 400px; object-fit: cover; border-radius: 8px; }
.price { color: #f56c6c; font-size: 28px; font-weight: bold; }
.original { color: #999; text-decoration: line-through; }
.msg-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.msg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.msg-sender { font-weight: 600; color: #303133; }
.msg-time { font-size: 12px; color: #999; }
.msg-content { color: #606266; line-height: 1.6; }
</style>
