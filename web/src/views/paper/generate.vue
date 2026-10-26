<script setup>
/**
 * 智能组卷：按「题型+难度+数量+每题分值」配置行随机抽题
 * 实时合计预估总分，成功后跳预览页
 */
defineOptions({ name: 'PaperGenerate' })

import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generatePaper } from '@/api/paper'
import { listCategories } from '@/api/question'
import { QUESTION_TYPE } from '@/constants'

const router = useRouter()
const saving = ref(false)
const formRef = ref()
const categories = ref([])

const form = reactive({
  title: '',
  description: '',
  duration: 60,
  passScore: null,
  category: '',
  config: [newConfigRow()],
})

function newConfigRow() {
  return { type: 1, difficulty: null, count: 5, scorePerQuestion: 2, category: '' }
}

const rules = {
  title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
}

const totalScore = computed(() =>
  form.config.reduce((sum, c) => sum + (Number(c.count) || 0) * (Number(c.scorePerQuestion) || 0), 0),
)

const totalCount = computed(() => form.config.reduce((sum, c) => sum + (Number(c.count) || 0), 0))

onMounted(async () => {
  try {
    categories.value = (await listCategories()) || []
  } catch {
    /* 拦截器已提示 */
  }
})

function addRow() {
  form.config.push(newConfigRow())
}

function removeRow(i) {
  if (form.config.length <= 1) {
    ElMessage.warning('至少保留一条抽题配置')
    return
  }
  form.config.splice(i, 1)
}

async function submit() {
  await formRef.value.validate()
  if (form.config.some((c) => !c.count || c.count <= 0 || !c.scorePerQuestion || c.scorePerQuestion <= 0)) {
    ElMessage.warning('抽题配置中的数量和每题分值必须大于 0')
    return
  }
  saving.value = true
  try {
    const paperId = await generatePaper({
      title: form.title.trim(),
      description: form.description || undefined,
      duration: form.duration,
      passScore: form.passScore ?? undefined,
      category: form.category || undefined,
      config: form.config.map((c) => ({
        type: c.type,
        difficulty: c.difficulty ?? undefined,
        count: c.count,
        scorePerQuestion: c.scorePerQuestion,
        category: c.category || undefined,
      })),
    })
    ElMessage.success('组卷成功')
    router.replace(`/paper/detail/${paperId}`)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 860px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="试卷名称" prop="title">
            <el-input v-model="form.title" maxlength="100" placeholder="如：2026 春期期中测验" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="考试时长" prop="duration">
            <el-input-number v-model="form.duration" :min="5" :max="110" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="及格分">
            <el-input-number v-model="form.passScore" :min="0" placeholder="默认总分 60%" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="试卷描述">
            <el-input v-model="form.description" maxlength="200" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="限制分类">
            <el-select v-model="form.category" placeholder="全部分类" clearable filterable style="width: 100%">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="抽题配置">
        <div style="width: 100%">
          <div
            v-for="(row, i) in form.config"
            :key="i"
            style="display: flex; gap: 8px; align-items: center; margin-bottom: 8px; flex-wrap: wrap"
          >
            <el-select v-model="row.type" style="width: 110px" placeholder="题型">
              <el-option v-for="(label, value) in QUESTION_TYPE" :key="value" :label="label" :value="Number(value)" />
            </el-select>
            <el-select v-model="row.difficulty" style="width: 100px" placeholder="难度(全)">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
            <el-input-number v-model="row.count" :min="1" :max="100" style="width: 120px" placeholder="数量" />
            <el-input-number v-model="row.scorePerQuestion" :min="0.5" :max="100" :step="0.5" style="width: 140px" />
            <span style="color: var(--el-text-color-secondary); font-size: 12px">分/题</span>
            <el-button type="danger" plain @click="removeRow(i)">删除</el-button>
          </div>
          <el-button @click="addRow">+ 添加抽题规则</el-button>
        </div>
      </el-form-item>

      <el-form-item>
        <el-tag type="info" size="large" effect="plain">
          预计 {{ totalCount }} 题，共 {{ totalScore }} 分（抽题数不足时组卷会失败，请保证题库存量）
        </el-tag>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">生成试卷</el-button>
        <el-button @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
