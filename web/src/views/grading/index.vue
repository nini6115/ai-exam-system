<script setup>
/**
 * AI 判卷（admin/teacher）
 * 选择考试 → 触发批量判卷（同步接口，loading + 防重复）→ 结果统计 + 失败明细 + 重试
 * 下方本场答卷列表行内「复核」进复核页
 */
defineOptions({ name: 'Grading' })

import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { gradeExam } from '@/api/ai'
import { listExams } from '@/api/exam'
import { listScores } from '@/api/analysis'
import { usePagination } from '@/hooks/usePagination'
import { SHEET_STATUS, SHEET_STATUS_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'

const route = useRoute()
const router = useRouter()

// ---------- 考试选择 ----------
const exams = ref([])
const examId = ref(null)

// ---------- 判卷 ----------
const grading = ref(false)
const gradeResult = ref(null) // GradeBatchResultVO

// ---------- 答卷列表（来自成绩明细接口） ----------
const scoreP = usePagination((params) => listScores(examId.value, params), { keyword: '' })

const canQuery = computed(() => !!examId.value)

async function loadExams() {
  try {
    const data = await listExams({ pageNum: 1, pageSize: 100 })
    exams.value = data?.list || []
    // 支持 /exam/detail 跳转携带 examId
    if (route.query.examId) examId.value = Number(route.query.examId)
  } catch {
    /* 拦截器已提示 */
  }
}

async function onExamChange() {
  gradeResult.value = null
  if (examId.value) scoreP.search()
}

async function startGrade() {
  if (!examId.value) {
    ElMessage.warning('请先选择考试')
    return
  }
  await ElMessageBox.confirm(
    '将对本场考试所有已交答卷的主观题执行 AI 判卷（客观题已由系统判分）。同步执行可能需要一些时间，确认开始？',
    '开始判卷',
    { type: 'info', confirmButtonText: '开始' },
  )
  grading.value = true
  gradeResult.value = null
  try {
    gradeResult.value = await gradeExam(examId.value)
    ElMessage.success('判卷执行完成')
    scoreP.search()
  } finally {
    grading.value = false
  }
}

onMounted(loadExams)
</script>

<template>
  <el-card shadow="never">
    <el-form inline>
      <el-form-item label="选择考试">
        <el-select
          v-model="examId"
          filterable
          placeholder="选择考试"
          style="width: 300px"
          @change="onExamChange"
        >
          <el-option v-for="e in exams" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="grading" :disabled="!canQuery" @click="startGrade">
          {{ grading ? '判卷执行中…' : '开始 AI 判卷' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 判卷结果 -->
    <template v-if="gradeResult">
      <el-row :gutter="12" style="margin-bottom: 16px">
        <el-col :span="8">
          <el-statistic title="待判题数" :value="gradeResult.pendingCount" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="成功" :value="gradeResult.successCount" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="失败" :value="gradeResult.failedCount" />
        </el-col>
      </el-row>

      <el-table
        v-if="gradeResult.failures?.length"
        :data="gradeResult.failures"
        stripe
        style="margin-bottom: 16px"
      >
        <el-table-column prop="sheetId" label="答卷ID" width="120" />
        <el-table-column prop="questionId" label="题目ID" width="120" />
        <el-table-column prop="reason" label="失败原因" min-width="300" show-overflow-tooltip />
      </el-table>
      <el-button
        v-if="gradeResult.failedCount > 0"
        style="margin-bottom: 16px"
        @click="startGrade"
      >
        重试失败的题目（重复执行安全）
      </el-button>
    </template>

    <!-- 答卷列表 -->
    <template v-if="examId">
      <el-divider content-position="left">本场答卷</el-divider>
      <el-form inline @submit.prevent="scoreP.search()">
        <el-form-item>
          <el-input v-model="scoreP.query.keyword" placeholder="姓名/学号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="scoreP.search()">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="scoreP.loading.value" :data="scoreP.list.value" stripe>
        <el-table-column prop="realName" label="学生" min-width="100" />
        <el-table-column prop="userNo" label="学号" min-width="100" />
        <el-table-column label="答卷状态" width="110">
          <template #default="{ row }">
            <EnumTag :label-map="SHEET_STATUS" :tag-map="SHEET_STATUS_TAG" :value="row.sheetStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观题得分" width="110" />
        <el-table-column prop="totalScore" label="总得分" width="100" />
        <el-table-column prop="submitTime" label="交卷时间" min-width="150" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="!row.sheetStatus || row.sheetStatus === 1"
              @click="router.push(`/grading/${examId}/sheet/${row.sheetId}`)"
            >
              复核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="page-card__footer">
        <el-pagination
          v-model:current-page="scoreP.pageNum.value"
          v-model:page-size="scoreP.pageSize.value"
          :total="scoreP.total.value"
          layout="total, prev, pager, next"
          @current-change="scoreP.onCurrentChange"
        />
      </div>
    </template>
    <el-empty v-else description="选择考试后展示答卷列表" />
  </el-card>
</template>
