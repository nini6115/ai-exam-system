<script setup>
/**
 * 手动组卷：左侧题目筛选分页多选，右侧已选列表（可排序）
 * 提交 PaperCreateDTO：只传 questionIds（后端按题型分组入卷，分值取题目默认分值）
 */
defineOptions({ name: 'PaperManual' })

import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPaper } from '@/api/paper'
import { listQuestions, listCategories } from '@/api/question'
import { usePagination } from '@/hooks/usePagination'
import { QUESTION_TYPE, DIFFICULTY, LETTERS } from '@/constants'

const router = useRouter()
const saving = ref(false)
const categories = ref([])
const formRef = ref()

const form = reactive({
  title: '',
  description: '',
  duration: 60,
  passScore: null,
})

const rules = {
  title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
}

// ---------- 左侧候选题目（/question/list 含答案与默认分值） ----------
const p = usePagination(listQuestions, { type: null, difficulty: null, category: '', title: '' })

// ---------- 右侧已选题目 ----------
const selected = ref([]) // QuestionVO 数组（含 id/type/title/score）

const selectedIds = computed(() => new Set(selected.value.map((q) => q.id)))
const totalScore = computed(() => selected.value.reduce((sum, q) => sum + (Number(q.score) || 0), 0))

/** 按题型分组展示 */
const grouped = computed(() => {
  const map = new Map()
  for (const q of selected.value) {
    if (!map.has(q.type)) map.set(q.type, [])
    map.get(q.type).push(q)
  }
  return [...map.entries()].map(([type, list]) => ({ type, list }))
})

function toggleSelect(row, checked) {
  if (checked) selected.value.push(row)
  else selected.value = selected.value.filter((q) => q.id !== row.id)
}

function move(q, dir) {
  const idx = selected.value.findIndex((x) => x.id === q.id)
  const target = idx + dir
  if (target < 0 || target >= selected.value.length) return
  const arr = selected.value
  ;[arr[idx], arr[target]] = [arr[target], arr[idx]]
}

function removeSelected(q) {
  selected.value = selected.value.filter((x) => x.id !== q.id)
}

onMounted(() => {
  p.search()
  listCategories()
    .then((list) => (categories.value = list || []))
    .catch(() => {})
})

async function submit() {
  await formRef.value.validate()
  if (!selected.value.length) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  saving.value = true
  try {
    const paperId = await createPaper({
      title: form.title.trim(),
      description: form.description || undefined,
      duration: form.duration,
      passScore: form.passScore ?? undefined,
      questionIds: selected.value.map((q) => q.id),
    })
    ElMessage.success('组卷成功')
    router.replace(`/paper/detail/${paperId}`)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-row :gutter="16">
    <!-- 左：候选题目 -->
    <el-col :xs="24" :md="12">
      <el-card shadow="never" header="选择题目">
        <el-form inline @submit.prevent="p.search()">
          <el-form-item>
            <el-select v-model="p.query.type" placeholder="题型" clearable style="width: 100px">
              <el-option v-for="(label, value) in QUESTION_TYPE" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="p.query.difficulty" placeholder="难度" clearable style="width: 90px">
              <el-option v-for="(label, value) in DIFFICULTY" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="p.query.category" placeholder="分类" clearable filterable style="width: 110px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-input v-model="p.query.title" placeholder="题干关键字" clearable style="width: 130px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="p.search()">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
          <el-table-column width="50">
            <template #default="{ row }">
              <el-checkbox
                :model-value="selectedIds.has(row.id)"
                @change="(val) => toggleSelect(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="题干" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.title }}</template>
          </el-table-column>
          <el-table-column label="题型" width="90">
            <template #default="{ row }">{{ QUESTION_TYPE[row.type] }}</template>
          </el-table-column>
          <el-table-column prop="score" label="分值" width="60" />
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
    </el-col>

    <!-- 右：试卷信息 + 已选题目 -->
    <el-col :xs="24" :md="12">
      <el-card shadow="never">
        <template #header>
          <span>试卷信息（已选 {{ selected.length }} 题，共 {{ totalScore }} 分）</span>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="试卷名称" prop="title">
            <el-input v-model="form.title" maxlength="100" />
          </el-form-item>
          <el-row :gutter="8">
            <el-col :span="12">
              <el-form-item label="考试时长" prop="duration">
                <el-input-number v-model="form.duration" :min="5" :max="110" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="及格分">
                <el-input-number v-model="form.passScore" :min="0" :max="totalScore || 100" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="试卷描述">
            <el-input v-model="form.description" maxlength="200" />
          </el-form-item>
        </el-form>

        <el-empty v-if="!selected.length" description="从左侧勾选题目加入试卷" :image-size="70" />
        <div v-for="group in grouped" :key="group.type" style="margin-bottom: 12px">
          <el-divider content-position="left">
            {{ QUESTION_TYPE[group.type] }}（{{ group.list.length }} 题 / {{ group.list.reduce((s, q) => s + Number(q.score || 0), 0) }} 分）
          </el-divider>
          <div
            v-for="(q, i) in group.list"
            :key="q.id"
            style="display: flex; align-items: center; gap: 8px; padding: 4px 0"
          >
            <span style="color: var(--el-text-color-secondary)">{{ i + 1 }}.</span>
            <span class="text-ellipsis" style="flex: 1" :title="q.title">{{ q.title }}</span>
            <el-tag size="small" type="info" disable-transitions>{{ q.score }} 分</el-tag>
            <el-button link size="small" @click="move(q, -1)">上移</el-button>
            <el-button link size="small" @click="move(q, 1)">下移</el-button>
            <el-button link type="danger" size="small" @click="removeSelected(q)">移除</el-button>
          </div>
        </div>

        <el-button
          type="primary"
          :loading="saving"
          :disabled="!selected.length"
          style="margin-top: 8px"
          @click="submit"
        >
          创建试卷
        </el-button>
      </el-card>
    </el-col>
  </el-row>
</template>
