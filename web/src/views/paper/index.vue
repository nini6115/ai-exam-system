<script setup>
/**
 * 试卷列表（admin/teacher）
 * 后端无试卷修改/删除接口：只提供「智能组卷 / 手动组卷 / 预览」
 */
defineOptions({ name: 'Paper' })

import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listPapers } from '@/api/paper'
import { usePagination } from '@/hooks/usePagination'

const router = useRouter()
const p = usePagination(listPapers, { name: '' })

onMounted(() => p.search())
</script>

<template>
  <el-card shadow="never">
    <el-form inline @submit.prevent="p.search()">
      <el-form-item label="试卷名">
        <el-input v-model="p.query.name" placeholder="试卷名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="p.search()">查询</el-button>
        <el-button @click="p.reset()">重置</el-button>
        <el-button type="success" @click="router.push('/paper/generate')">智能组卷</el-button>
        <el-button type="success" plain @click="router.push('/paper/manual')">手动组卷</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="试卷创建后不支持修改和删除（后端暂未提供相应接口），组卷前请确认配置"
      style="margin-bottom: 12px"
    />

    <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
      <el-table-column prop="name" label="试卷名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="passScore" label="及格分" width="80" />
      <el-table-column label="时长" width="90">
        <template #default="{ row }">{{ row.duration }} 分钟</template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="80" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/paper/detail/${row.id}`)">预览</el-button>
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
