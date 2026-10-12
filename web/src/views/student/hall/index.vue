<script setup>
/**
 * 考试大厅（student）
 * /exam/my/list 分页 + 状态筛选 + 按钮状态机 + 进入考试确认
 */
defineOptions({ name: 'StudentHall' })

import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { listMyExams } from '@/api/exam'
import { usePagination } from '@/hooks/usePagination'
import { EXAM_STATUS, EXAM_STATUS_TAG, SHEET_STATUS } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'

const router = useRouter()
const p = usePagination(listMyExams, { status: null })

onMounted(() => p.search())

/** 进行中且答卷在答题中 → 可「继续考试」；否则按次数判断 */
function canEnter(row) {
  if (row.status !== 2) return false
  if (row.sheetStatus === 1) return true // 答题中：进入恢复
  return row.myAttempts < row.maxAttempts
}

function enterText(row) {
  if (row.sheetStatus === 1) return '继续考试'
  return '进入考试'
}

function disabledTip(row) {
  if (row.status === 1) return `未开始（${row.startTime} 开考）`
  if (row.status === 3) return '已结束'
  if (row.status === 2 && row.myAttempts >= row.maxAttempts) return '考试次数已用完'
  return ''
}

async function enterExam(row) {
  await ElMessageBox.confirm(
    row.sheetStatus === 1
      ? '本场考试正在作答中，继续将回到答题页（题目顺序不变，已保存答案不丢失）。'
      : `进入「${row.examName}」后开始计时，请合理安排作答时间。确认进入？`,
    '进入考试',
    { confirmButtonText: '进入', type: 'info' },
  )
  router.push(`/exam-taking/${row.examId}`)
}
</script>

<template>
  <el-card shadow="never">
    <el-form inline @submit.prevent="p.search()">
      <el-form-item label="状态">
        <el-select v-model="p.query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="未开始" :value="1" />
          <el-option label="进行中" :value="2" />
          <el-option label="已结束" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="p.search()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
      <el-table-column prop="examName" label="考试名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="paperName" label="试卷" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <EnumTag :label-map="EXAM_STATUS" :tag-map="EXAM_STATUS_TAG" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" min-width="150" />
      <el-table-column prop="endTime" label="结束时间" min-width="150" />
      <el-table-column label="时长" width="80">
        <template #default="{ row }">{{ row.duration }} 分</template>
      </el-table-column>
      <el-table-column label="次数" width="80">
        <template #default="{ row }">{{ row.myAttempts }}/{{ row.maxAttempts }}</template>
      </el-table-column>
      <el-table-column label="成绩" width="90">
        <template #default="{ row }">
          <span v-if="row.scoreVisible && row.bestScore != null" style="color: var(--el-color-success); font-weight: 600">
            {{ row.bestScore }}
          </span>
          <span v-else style="color: var(--el-text-color-placeholder)">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-tooltip :content="canEnter(row) ? '' : disabledTip(row)" :disabled="canEnter(row)" placement="top">
            <span>
              <el-button
                link
                type="primary"
                :disabled="!canEnter(row)"
                @click="enterExam(row)"
              >
                {{ canEnter(row) ? enterText(row) : '不可进入' }}
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-if="row.myAttempts > 0"
            link
            @click="router.push(`/student/result/${row.examId}`)"
          >
            结果
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-card__footer">
      <el-pagination
        v-model:current-page="p.pageNum.value"
        v-model:page-size="p.pageSize.value"
        :total="p.total.value"
        layout="total, prev, pager, next"
        @current-change="p.onCurrentChange"
      />
    </div>
  </el-card>
</template>
