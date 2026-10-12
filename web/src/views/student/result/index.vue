<script setup>
/**
 * 考试结果页
 * type=normal 交卷成功 / force 强制交卷 / timeout 超时自动交卷
 * showScoreAfter=1 时展示本场最好成绩，否则提示等待教师发布
 */
defineOptions({ name: 'StudentResult' })

import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyExams } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId
const resultType = computed(() => route.query.type || 'normal')

const bestScore = ref(null)
const checked = ref(false)

const title = computed(() => {
  if (resultType.value === 'force') return '考试已被强制交卷'
  if (resultType.value === 'timeout') return '考试时间到，已自动交卷'
  return '交卷成功'
})

const description = computed(() => {
  if (resultType.value === 'force') {
    return '切屏/离开次数超过考场规则上限，本场考试已被系统强制收卷。'
  }
  return '你的答卷已提交，客观题已自动判分。'
})

onMounted(async () => {
  try {
    // 成绩可见时从考试大厅数据取本场最好成绩
    const data = await listMyExams({ pageNum: 1, pageSize: 100 })
    const mine = (data?.list || []).find((e) => String(e.examId) === String(examId))
    if (mine?.scoreVisible && mine.bestScore != null) {
      bestScore.value = mine.bestScore
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    checked.value = true
  }
})
</script>

<template>
  <div class="result-page">
    <el-card shadow="never" class="result-card">
      <div class="result-icon" :class="`result-icon--${resultType}`">
        <el-icon :size="52">
          <CircleCheckFilled v-if="resultType === 'normal'" />
          <WarningFilled v-else />
        </el-icon>
      </div>

      <h2 class="result-title">{{ title }}</h2>
      <p class="result-desc">{{ description }}</p>

      <div v-if="bestScore != null" class="result-score">
        <span>本场成绩</span>
        <b>{{ bestScore }}</b>
      </div>
      <p v-else-if="checked" class="result-waiting">成绩将在教师复核发布后可在考试大厅查看</p>

      <el-button type="primary" size="large" @click="router.replace('/student/exams')">返回考试大厅</el-button>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.result-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}

.result-card {
  width: 420px;
  text-align: center;
  padding: 16px 0;
}

.result-icon {
  margin-bottom: 8px;

  &--normal {
    color: var(--el-color-success);
  }

  &--force,
  &--timeout {
    color: var(--el-color-warning);
  }
}

.result-title {
  margin: 0 0 8px;
}

.result-desc {
  margin: 0 0 20px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}

.result-score {
  margin: 0 auto 20px;
  padding: 16px;
  width: 240px;
  background: var(--el-color-success-light-9);
  border-radius: 8px;

  span {
    display: block;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  b {
    font-size: 34px;
    color: var(--el-color-success);
  }
}

.result-waiting {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
