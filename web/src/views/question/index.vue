<script setup>
/**
 * 题库管理（admin/teacher）
 * 筛选 + CRUD，新增/编辑走 QuestionForm（5 题型动态表单）
 */
defineOptions({ name: 'Question' })

import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listQuestions, getQuestion, deleteQuestion, listCategories } from '@/api/question'
import { usePagination } from '@/hooks/usePagination'
import { QUESTION_TYPE, QUESTION_TYPE_TAG, DIFFICULTY, DIFFICULTY_TAG } from '@/constants'
import EnumTag from '@/components/EnumTag.vue'
import QuestionForm from '@/components/QuestionForm.vue'

const p = usePagination(listQuestions, { type: null, difficulty: null, category: '', title: '' })

const categories = ref([])

async function loadCategories() {
  try {
    categories.value = (await listCategories()) || []
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(() => {
  p.search()
  loadCategories()
})

// ---------- 新增/编辑 ----------
const questionFormRef = ref()
const editQuestion = ref(null) // QuestionForm 的编辑数据

function openAdd() {
  editQuestion.value = null
  questionFormRef.value.open()
}

async function openEdit(row) {
  // 列表 VO 已含 options/answer，直接用；但为稳妥取详情
  const detail = await getQuestion(row.id)
  editQuestion.value = detail
  questionFormRef.value.open()
}

function onFormSuccess() {
  p.load()
  loadCategories() // 分类可能新建
}

// ---------- 删除 ----------
async function doDelete(row) {
  await ElMessageBox.confirm('确认删除该题目？已被试卷引用的题目删除后可能影响相关试卷。', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
  })
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  p.load()
}
</script>

<template>
  <div>
    <el-card shadow="never">
      <el-form inline @submit.prevent="p.search()">
        <el-form-item label="题型">
          <el-select v-model="p.query.type" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(label, value) in QUESTION_TYPE" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="p.query.difficulty" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="(label, value) in DIFFICULTY" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="p.query.category"
            placeholder="全部分类"
            clearable
            filterable
            allow-create
            style="width: 140px"
          >
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="p.query.title" placeholder="关键字" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="p.search()">查询</el-button>
          <el-button @click="p.reset()">重置</el-button>
          <el-button type="primary" @click="openAdd">新增题目</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
        <el-table-column label="题干" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title }}</template>
        </el-table-column>
        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            <EnumTag :label-map="QUESTION_TYPE" :tag-map="QUESTION_TYPE_TAG" :value="row.type" />
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">
            <EnumTag :label-map="DIFFICULTY" :tag-map="DIFFICULTY_TAG" :value="row.difficulty" />
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" min-width="110" />
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="doDelete(row)">删除</el-button>
            </div>
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

    <QuestionForm ref="questionFormRef" :question="editQuestion" :categories="categories" @success="onFormSuccess" />
  </div>
</template>
