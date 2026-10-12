<script setup>
/**
 * 考试列表（admin/teacher）
 * 后端无考试修改/删除接口：只提供发布与详情
 */
defineOptions({ name: 'Exam' })

import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listExams } from '@/api/exam'
import { usePagination } from '@/hooks/usePagination'
import { EXAM_STATUS, EXAM_STATUS_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'

const router = useRouter()
const p = usePagination(listExams, { name: '' })

onMounted(() => p.search())
</script>

<template>
  <el-card shadow="never">
    <el-form inline @submit.prevent="p.search()">
      <el-form-item label="考试名">
        <el-input v-model="p.query.name" placeholder="考试名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="p.search()">查询</el-button>
        <el-button @click="p.reset()">重置</el-button>
        <el-button type="primary" @click="router.push('/exam/publish')">发布考试</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="考试发布后不支持修改和删除（后端暂未提供相应接口），发布前请仔细确认"
      style="margin-bottom: 12px"
    />

    <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
      <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="paperName" label="试卷" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <EnumTag :label-map="EXAM_STATUS" :tag-map="EXAM_STATUS_TAG" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" min-width="150" />
      <el-table-column prop="endTime" label="结束时间" min-width="150" />
      <el-table-column label="时长" width="90">
        <template #default="{ row }">{{ row.duration }} 分钟</template>
      </el-table-column>
      <el-table-column prop="studentCount" label="考生数" width="80" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/exam/detail/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-card__footer">
      <el-pagination
        v-model:current-page="p.pageNum.value"
        v-model:page-size="p.pageSize.value"
        :total="p.total.value"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="p.onCurrentChange"
        @size-change="p.onSizeChange"
      />
    </div>
  </el-card>
</template>
