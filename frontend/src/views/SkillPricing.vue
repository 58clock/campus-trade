<template>
  <div class="container">
    <h2 style="margin-bottom:16px">AI 定价建议 <el-tag type="warning" size="small">Skill</el-tag></h2>
    <p style="color:#999;margin-bottom:24px">根据同类同成色商品的市场数据，给出合理售价建议</p>

    <el-card style="max-width:500px">
      <el-form label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="category" placeholder="选择商品分类">
            <el-option label="书籍" value="BOOK" />
            <el-option label="电子产品" value="ELECTRONICS" />
            <el-option label="生活用品" value="LIFESTYLE" />
            <el-option label="运动用品" value="SPORTS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="成色">
          <el-select v-model="condition" placeholder="选择商品成色">
            <el-option label="全新" value="NEW" />
            <el-option label="几乎全新" value="LIKE_NEW" />
            <el-option label="良好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!canQuery" @click="fetchPrice">查询建议</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" style="max-width:500px;margin-top:16px">
      <template #header>定价分析结果</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="分类">{{ result.category }}</el-descriptions-item>
        <el-descriptions-item label="成色">{{ result.conditionLevel }}</el-descriptions-item>
        <el-descriptions-item label="参考样本">{{ result.sampleCount }} 件同类商品</el-descriptions-item>
        <el-descriptions-item label="市场均价">
          <span style="font-weight:bold">¥{{ result.marketAvgPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="建议售价">
          <span style="color:#e6a23c;font-weight:bold;font-size:18px">¥{{ result.suggestedPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="建议区间">
          ¥{{ result.rangeLow }} ~ ¥{{ result.rangeHigh }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { skillApi } from '@/api'

const category = ref('')
const condition = ref('')
const result = ref(null)

const canQuery = computed(() => category.value && condition.value)

async function fetchPrice() {
  try {
    const res = await skillApi.suggestPrice(category.value, condition.value)
    result.value = res.data
  } catch { /* handled */ }
}
</script>

<style scoped>
.container { max-width: 700px; margin: 24px auto; padding: 0 16px; }
</style>
