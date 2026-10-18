<script setup>
/**
 * 成绩分析（admin/teacher）
 * 统计卡 + 分数段分布（批次4前先用进度条呈现）+ 成绩明细 + CSV 导出
 */
defineOptions({ name: 'Analysis' })

import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStats, listScores, exportScores } from '@/api/analysis'
import { listExams, publishScore } from '@/api/exam'
import { usePagination } from '@/hooks/usePagination'
import { downloadBlobResponse } from '@/utils/download'
import { SHEET_STATUS, SHEET_STATUS_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'
import EChart from '@/components/EChart.vue'

const route = useRoute()

const exams = ref([])
const examId = ref(null)
const stats = ref(null)
const exporting = ref(false)
const publishing = ref(false)

/** 发布成绩：交卷学生的成绩对学生端可见（幂等，重复点击安全） */
async function doPublishScore() {
  await ElMessageBox.confirm(
    '发布后本场考试已交卷学生的成绩将对学生端可见，确认发布？',
    '发布成绩',
    { type: 'warning', confirmButtonText: '发布' },
  )
  publishing.value = true
  try {
    await publishScore(examId.value)
    ElMessage.success('成绩已发布，学生端可见')
  } finally {
    publishing.value = false
  }
}

const scoreP = usePagination((params) => listScores(examId.value, params), { keyword: '' })

/** 分数段柱状图 option */
const segmentOption = computed(() => ({
  grid: { left: 40, right: 16, top: 24, bottom: 28 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: (stats.value?.scoreSegments || []).map((s) => s.label),
    axisLabel: { fontSize: 11 },
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      type: 'bar',
      name: '人数',
      data: (stats.value?.scoreSegments || []).map((s) => Number(s.studentCount || 0)),
      barMaxWidth: 46,
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' },
    },
  ],
}))

/** 实考/缺考环形图 option */
const attendOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      label: { show: true, formatter: '{b}: {c} 人' },
      data: [
        { name: '实考', value: Number(stats.value?.attendedCount || 0), itemStyle: { color: '#409eff' } },
        { name: '缺考', value: Number(stats.value?.absentCount || 0), itemStyle: { color: '#dcdfe6' } },
      ],
    },
  ],
}))

async function loadExams() {
  try {
    const data = await listExams({ pageNum: 1, pageSize: 100 })
    exams.value = data?.list || []
    if (route.query.examId) examId.value = Number(route.query.examId)
    if (examId.value) await onExamChange()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onExamChange() {
  stats.value = null
  if (!examId.value) return
  stats.value = await getStats(examId.value)
  scoreP.search()
}

async function doExport() {
  exporting.value = true
  try {
    const response = await exportScores(examId.value, scoreP.query.keyword || undefined)
    // 成功返回 text/csv，业务错误返回 HTTP 200 + JSON（由 downloadBlobResponse 按 content-type 分流）
    await downloadBlobResponse(response, '成绩明细.csv')
  } finally {
    exporting.value = false
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
      <el-form-item v-if="examId">
        <el-button type="success" :loading="publishing" @click="doPublishScore">发布成绩</el-button>
        <el-button type="primary" :loading="exporting" @click="doExport">导出 CSV</el-button>
      </el-form-item>
    </el-form>

    <template v-if="stats">
      <!-- 统计卡 -->
      <el-row :gutter="12" style="margin-bottom: 16px">
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.assignedCount }}</div>
            <div class="stat-card__label">应考人数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.attendedCount }}</div>
            <div class="stat-card__label">实考人数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.absentCount }}</div>
            <div class="stat-card__label">缺考人数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.passRate != null ? `${stats.passRate}%` : '—' }}</div>
            <div class="stat-card__label">及格率</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="12" style="margin-bottom: 16px">
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.avgScore ?? '—' }}</div>
            <div class="stat-card__label">平均分</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.maxScore ?? '—' }}</div>
            <div class="stat-card__label">最高分</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.minScore ?? '—' }}</div>
            <div class="stat-card__label">最低分</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-card__value">{{ stats.passedCount ?? '—' }}</div>
            <div class="stat-card__label">及格人数</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表：分数段柱状图 + 实考/缺考环形图 -->
      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :xs="24" :md="14">
          <el-card shadow="never" header="分数段分布">
            <EChart v-if="stats.scoreSegments?.length" :option="segmentOption" :height="260" />
            <el-empty v-else description="暂无成绩数据" :image-size="60" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="10">
          <el-card shadow="never" header="参考情况">
            <EChart :option="attendOption" :height="260" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 成绩明细 -->
      <el-form inline @submit.prevent="scoreP.search()">
        <el-form-item>
          <el-input v-model="scoreP.query.keyword" placeholder="姓名/学号搜索" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="scoreP.search()">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="scoreP.loading.value" :data="scoreP.list.value" stripe>
        <el-table-column prop="realName" label="学生" min-width="100" />
        <el-table-column prop="userNo" label="学号" min-width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <EnumTag :label-map="SHEET_STATUS" :tag-map="SHEET_STATUS_TAG" :value="row.sheetStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" min-width="150">
          <template #default="{ row }">{{ row.submitTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观题得分" width="110">
          <template #default="{ row }">{{ row.objectiveScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总得分" width="90">
          <template #default="{ row }">{{ row.totalScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="是否及格" width="90">
          <template #default="{ row }">
            <el-tag
              v-if="row.isPassed != null"
              :type="row.isPassed === 1 ? 'success' : 'danger'"
              size="small"
              disable-transitions
            >
              {{ row.isPassed === 1 ? '及格' : '不及格' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="screenSwitchCount" label="切屏次数" width="90">
          <template #default="{ row }">{{ row.screenSwitchCount ?? 0 }}</template>
        </el-table-column>
      </el-table>
      <div class="page-card__footer">
        <el-pagination
          v-model:current-page="scoreP.pageNum.value"
          v-model:page-size="scoreP.pageSize.value"
          :total="scoreP.total.value"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="scoreP.onCurrentChange"
          @size-change="scoreP.onSizeChange"
        />
      </div>
    </template>
    <el-empty v-else description="选择考试后展示成绩统计" />
  </el-card>
</template>

<style lang="scss" scoped>
.stat-card {
  text-align: center;

  &__value {
    font-size: 26px;
    font-weight: 700;
    color: var(--el-color-primary);
  }

  &__label {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

</style>
