<script setup>
/**
 * 考试详情：基本信息 + 防作弊参数 + 考生名单；入口跳 AI 判卷/成绩分析
 */
defineOptions({ name: 'ExamDetail' })

import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExam } from '@/api/exam'
import { GENDER, EXAM_STATUS, EXAM_STATUS_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const exam = ref(null)

const antiCheatItems = computed(() => {
  const e = exam.value
  if (!e) return []
  return [
    { label: '最大切屏次数', value: e.maxScreenSwitch === 0 ? '不限制' : `${e.maxScreenSwitch} 次` },
    { label: '切屏超限处置', value: e.screenSwitchAction === 2 ? '强制交卷' : '警告' },
    { label: '离开超时', value: e.awayTimeout === 0 ? '不检测' : `${e.awayTimeout} 秒` },
    { label: '题目乱序', value: e.randomOrder === 1 ? '开启' : '关闭' },
    { label: '选项乱序', value: e.randomOptions === 1 ? '开启' : '关闭' },
    { label: '禁止复制', value: e.forbidCopy === 1 ? '开启' : '关闭' },
    { label: '最大考试次数', value: `${e.maxAttempts} 次` },
    { label: '迟到限制', value: `${e.allowLateMinutes} 分钟` },
    { label: '交卷后显示成绩', value: e.showScoreAfter === 1 ? '是' : '否' },
  ]
})

onMounted(async () => {
  loading.value = true
  try {
    exam.value = await getExam(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <el-card v-if="exam" shadow="never">
      <template #header>
        <div style="display: flex; align-items: center">
          <div>
            <h3 style="margin: 0; display: inline-flex; align-items: center; gap: 8px">
              {{ exam.name }}
              <EnumTag :label-map="EXAM_STATUS" :tag-map="EXAM_STATUS_TAG" :value="exam.status" />
            </h3>
          </div>
          <div style="margin-left: auto; display: flex; gap: 8px">
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" plain @click="router.push({ path: '/grading', query: { examId: exam.id } })">
              AI 判卷
            </el-button>
            <el-button type="primary" @click="router.push({ path: '/analysis', query: { examId: exam.id } })">
              成绩分析
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :xs="24" :md="10">
          <el-descriptions :column="1" border title="考试信息">
            <el-descriptions-item label="试卷">{{ exam.paperName }}</el-descriptions-item>
            <el-descriptions-item label="总分 / 及格分">{{ exam.totalScore }} / {{ exam.passScore }}</el-descriptions-item>
            <el-descriptions-item label="题数 / 时长">{{ exam.questionCount }} 题 / {{ exam.duration }} 分钟</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ exam.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ exam.endTime }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="2" border title="防作弊参数" style="margin-top: 16px">
            <el-descriptions-item v-for="item in antiCheatItems" :key="item.label" :label="item.label">
              {{ item.value }}
            </el-descriptions-item>
          </el-descriptions>
        </el-col>

        <el-col :xs="24" :md="14">
          <el-card shadow="never" :header="`考生名单（${exam.students?.length || 0} 人）`">
            <el-table :data="exam.students || []" stripe max-height="520">
              <el-table-column prop="realName" label="姓名" min-width="90" />
              <el-table-column prop="userNo" label="学号" min-width="100" />
              <el-table-column label="考试次数" width="90">
                <template #default="{ row }">{{ row.attempts }}</template>
              </el-table-column>
              <el-table-column label="最好成绩" width="100">
                <template #default="{ row }">{{ row.bestScore ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="assignTime" label="分配时间" min-width="150" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>
