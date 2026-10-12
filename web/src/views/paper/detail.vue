<script setup>
/**
 * 试卷预览（只读）：按 section 分组展示题目（不含答案，后端不返回）
 */
defineOptions({ name: 'PaperDetail' })

import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPaper } from '@/api/paper'
import { LETTERS } from '@/constants'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paper = ref(null)

const grouped = computed(() => {
  const questions = paper.value?.questions || []
  const map = new Map()
  for (const q of questions) {
    const key = q.section || '未分组'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(q)
  }
  return [...map.entries()].map(([section, list]) => ({ section, list }))
})

onMounted(async () => {
  loading.value = true
  try {
    paper.value = await getPaper(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <el-card v-if="paper" shadow="never">
      <template #header>
        <div style="display: flex; align-items: center">
          <div>
            <h3 style="margin: 0">{{ paper.name }}</h3>
            <div style="color: var(--el-text-color-secondary); font-size: 13px; margin-top: 4px">
              总分 {{ paper.totalScore }} 分 · 及格 {{ paper.passScore }} 分 · {{ paper.questionCount }} 题 ·
              时长 {{ paper.duration }} 分钟 · 创建于 {{ paper.createTime }}
            </div>
          </div>
          <div style="margin-left: auto; display: flex; gap: 8px">
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" @click="router.push('/exam/publish')">用此卷发布考试</el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="预览不显示参考答案；试卷发布后题目与分值不可修改"
        style="margin-bottom: 16px"
      />

      <p v-if="paper.description" style="color: var(--el-text-color-secondary)">{{ paper.description }}</p>

      <div v-for="group in grouped" :key="group.section" style="margin-bottom: 20px">
        <el-divider content-position="left">
          {{ group.section }}（{{ group.list.length }} 题 / {{ group.list.reduce((s, q) => s + Number(q.questionScore || 0), 0) }} 分）
        </el-divider>

        <div v-for="(q, i) in group.list" :key="q.questionId" class="paper-question">
          <div class="paper-question__title">
            {{ i + 1 }}. {{ q.title }}
            <el-tag size="small" type="info" disable-transitions style="margin-left: 8px">
              {{ q.questionScore }} 分
            </el-tag>
          </div>
          <div v-if="q.options?.length" class="paper-question__options">
            <div v-for="(opt, oi) in q.options" :key="oi">{{ LETTERS[oi] }}. {{ opt }}</div>
          </div>
          <div v-else-if="q.type === 3" class="paper-question__options">
            <span>对 / 错</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.paper-question {
  padding: 10px 4px;
  border-bottom: 1px dashed var(--el-border-color-lighter);

  &__title {
    line-height: 1.6;
  }

  &__options {
    margin-top: 6px;
    padding-left: 20px;
    color: var(--el-text-color-regular);
    line-height: 1.9;
  }
}
</style>
