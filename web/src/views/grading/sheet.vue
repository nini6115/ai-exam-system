<script setup>
/**
 * 答卷复核：逐题判分详情 + 主观题行内改分
 * 改分返回 SheetScoreSummaryVO 刷新页头三分数
 */
defineOptions({ name: 'GradingSheet' })

import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSheetDetails, manualGrade } from '@/api/ai'
import { QUESTION_TYPE, GRADED_BY, GRADED_BY_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const details = ref([]) // List<GradeDetailVO>
const summary = ref(null) // SheetScoreSummaryVO（改分后由后端返回）

// 改分中间态：{ [detailId]: { score, comment } }
const editMap = reactive({})
const savingIds = reactive({})

const objectiveScore = computed(() => {
  if (summary.value) return summary.value.objectiveScore
  return details.value
    .filter((d) => d.type !== 5)
    .reduce((s, d) => s + Number(d.score || 0), 0)
})

const subjectiveScore = computed(() => {
  if (summary.value) return summary.value.subjectiveScore
  const items = details.value.filter((d) => d.type === 5)
  return items.length ? items.reduce((s, d) => s + Number(d.score || 0), 0) : null
})

const totalScore = computed(() => {
  if (summary.value) return summary.value.totalScore
  return details.value.reduce((s, d) => s + Number(d.score || 0), 0)
})

/** 可人工改分的：主观题或 AI 判分题 */
function canAdjust(d) {
  return d.type === 5 || d.gradedBy === 3
}

function startEdit(d) {
  editMap[d.detailId] = {
    score: Number(d.score || 0),
    comment: d.gradeComment || '',
  }
}

function cancelEdit(d) {
  delete editMap[d.detailId]
}

async function saveGrade(d) {
  const edit = editMap[d.detailId]
  if (!edit) return
  if (edit.score == null || edit.score < 0 || edit.score > Number(d.fullScore)) {
    ElMessage.warning(`分数需在 0 ~ ${d.fullScore} 之间`)
    return
  }
  savingIds[d.detailId] = true
  try {
    summary.value = await manualGrade(d.detailId, { score: edit.score, comment: edit.comment || undefined })
    delete editMap[d.detailId]
    await load()
    ElMessage.success('已改分（判分方式更新为人工）')
  } finally {
    savingIds[d.detailId] = false
  }
}

async function load() {
  loading.value = true
  try {
    details.value = (await getSheetDetails(route.params.examId, route.params.sheetId)) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; align-items: center">
          <span>答卷复核（答卷 #{{ route.params.sheetId }}）</span>
          <div style="margin-left: auto; display: flex; gap: 24px">
            <el-statistic title="客观题" :value="Number(objectiveScore ?? 0)" :precision="1" />
            <el-statistic title="主观题" :value="subjectiveScore == null ? '待判' : Number(subjectiveScore)" :precision="1" />
            <el-statistic title="总分" :value="Number(totalScore ?? 0)" :precision="1" />
          </div>
          <el-button style="margin-left: 24px" @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-alert
        v-if="subjectiveScore == null"
        type="warning"
        :closable="false"
        show-icon
        title="仍有待判主观题，成绩未定稿：请回到 AI 判卷页执行批量判卷"
        style="margin-bottom: 8px"
      />

      <div v-for="(d, i) in details" :key="d.detailId" class="grade-item">
        <div class="grade-item__head">
          <span style="font-weight: 600">{{ i + 1 }}. {{ d.title }}</span>
          <span style="display: inline-flex; gap: 8px; align-items: center">
            <el-tag size="small" disable-transitions>{{ QUESTION_TYPE[d.type] }}</el-tag>
            <EnumTag :label-map="GRADED_BY" :tag-map="GRADED_BY_TAG" :value="d.gradedBy" />
            <el-tag size="small" :type="d.isCorrect === 1 ? 'success' : d.isCorrect === 0 ? 'danger' : 'info'" disable-transitions>
              {{ d.score ?? '—' }} / {{ d.fullScore }} 分
            </el-tag>
          </span>
        </div>

        <div class="grade-item__body">
          <p><b>学生答案：</b>{{ d.userAnswer || '（未作答）' }}</p>
          <p><b>参考答案：</b>{{ d.correctAnswer || '—' }}</p>
          <p v-if="d.gradeComment"><b>判分评语：</b>{{ d.gradeComment }}</p>
        </div>

        <div class="grade-item__actions">
          <template v-if="editMap[d.detailId]">
            <el-input-number
              v-model="editMap[d.detailId].score"
              :min="0"
              :max="Number(d.fullScore)"
              :step="0.5"
              size="small"
              style="width: 130px"
            />
            <el-input
              v-model="editMap[d.detailId].comment"
              size="small"
              placeholder="评语（选填）"
              maxlength="500"
              style="width: 320px"
            />
            <el-button size="small" type="primary" :loading="savingIds[d.detailId]" @click="saveGrade(d)">保存</el-button>
            <el-button size="small" @click="cancelEdit(d)">取消</el-button>
          </template>
          <el-button v-else-if="canAdjust(d)" size="small" type="primary" plain @click="startEdit(d)">
            人工改分
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.grade-item {
  padding: 14px 4px;
  border-bottom: 1px dashed var(--el-border-color-lighter);

  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__body p {
    margin: 6px 0;
    color: var(--el-text-color-regular);
    line-height: 1.7;
    word-break: break-all;
  }

  &__actions {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-top: 8px;
    flex-wrap: wrap;
  }
}
</style>
