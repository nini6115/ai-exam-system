<script setup>
/**
 * 工作台（批次1为占位：欢迎卡 + 按角色快捷入口，批次4接入数据概览）
 */
defineOptions({ name: 'Dashboard' })

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listExams } from '@/api/exam'
import { useUserStore } from '@/store/user'
import { EXAM_STATUS, EXAM_STATUS_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'

const router = useRouter()
const userStore = useUserStore()

const recentExams = ref([])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const entries = computed(() => {
  if (userStore.roleCodes.includes('student')) {
    return [{ title: '考试大厅', desc: '查看并进入可参加的考试', path: '/student/exams', icon: 'AlarmClock' }]
  }
  return [
    { title: '题库管理', desc: '维护单选/多选/判断/填空/简答题目', path: '/question', icon: 'Collection' },
    { title: '智能组卷', desc: '按题型与难度自动抽题生成试卷', path: '/paper/generate', icon: 'Notebook' },
    { title: '发布考试', desc: '选择试卷、考生与防作弊策略', path: '/exam/publish', icon: 'Bell' },
    { title: 'AI 判卷', desc: '大模型批量判分与人工复核', path: '/grading', icon: 'MagicStick' },
    { title: '成绩分析', desc: '统计概览、成绩明细与 CSV 导出', path: '/analysis', icon: 'TrendCharts' },
  ]
})

onMounted(async () => {
  if (userStore.roleCodes.includes('student')) return
  try {
    // 最近 5 场考试概览（后端暂无聚合统计接口，取列表前几条）
    const data = await listExams({ pageNum: 1, pageSize: 5 })
    recentExams.value = data?.list || []
  } catch {
    /* 拦截器已提示 */
  }
})
</script>

<template>
  <div class="dashboard">
    <el-card shadow="never" class="dashboard__welcome">
      <h2 style="margin: 0 0 6px">{{ greeting }}，{{ userStore.displayName }}</h2>
      <p style="margin: 0; color: var(--el-text-color-secondary)">
        欢迎使用 AI 辅助在线考试系统，祝工作顺利。
      </p>
    </el-card>

    <el-row :gutter="16" class="dashboard__entries">
      <el-col v-for="item in entries" :key="item.path" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="dashboard__entry" @click="router.push(item.path)">
          <div class="dashboard__entry-inner">
            <el-icon :size="28" color="var(--el-color-primary)"><component :is="item.icon" /></el-icon>
            <div>
              <div class="dashboard__entry-title">{{ item.title }}</div>
              <div class="dashboard__entry-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近考试（教师端） -->
    <el-card v-if="recentExams.length" shadow="never" header="最近考试">
      <el-table :data="recentExams" stripe>
        <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="paperName" label="试卷" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <EnumTag :label-map="EXAM_STATUS" :tag-map="EXAM_STATUS_TAG" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="150" />
        <el-table-column prop="studentCount" label="考生数" width="80" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/exam/detail/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  &__welcome {
    margin-bottom: 16px;
  }

  &__entries {
    .el-col {
      margin-bottom: 16px;
    }
  }

  &__entry {
    cursor: pointer;

    &-inner {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    &-title {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 4px;
    }

    &-desc {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
