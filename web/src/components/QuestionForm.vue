<script setup>
/**
 * 题目新增/编辑弹窗：按题型动态渲染
 * - 单选/多选：选项编辑器（2~8 行可增删）+ 答案字母
 * - 判断：固定「对」「错」
 * - 填空/简答：答案文本/多行
 * 用法：<QuestionForm ref="formRef" :categories="cats" :question="editTarget" @success="load" />
 *       formRef.value.open() 打开（question=null 为新增）
 */
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addQuestion, updateQuestion } from '@/api/question'

const props = defineProps({
  question: { type: Object, default: null }, // null=新增
  categories: { type: Array, default: () => [] },
})
const emit = defineEmits(['success'])

const visible = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  type: 1,
  difficulty: 2,
  category: '',
  title: '',
  options: [],
  answer: '',
  analysis: '',
  score: 2,
})

/** 多选答案中间态（字母数组），提交时序列化为 "ACD" */
const multiAnswer = ref([])
/** 判断题答案中间态 */
const judgeAnswer = ref('')

const isChoice = computed(() => form.type === 1 || form.type === 2)

const rules = {
  title: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

/** 切题型时重置答案相关字段（保留题干/分类/难度/分值） */
watch(
  () => form.type,
  (t) => {
    if (t === 1 || t === 2) {
      if (form.options.length < 2) form.options = ['', '', '', '']
      form.answer = ''
      multiAnswer.value = []
    } else {
      form.options = []
      form.answer = ''
      multiAnswer.value = []
      judgeAnswer.value = ''
    }
  },
)

function addOption() {
  if (form.options.length >= 8) {
    ElMessage.warning('最多 8 个选项')
    return
  }
  form.options.push('')
}

function removeOption(i) {
  if (form.options.length <= 2) {
    ElMessage.warning('至少保留 2 个选项')
    return
  }
  form.options.splice(i, 1)
  // 选项删除后清理失效答案字母
  const maxLetter = 'ABCDEFGH'[form.options.length - 1]
  form.answer = form.answer && form.answer <= maxLetter ? form.answer : ''
  multiAnswer.value = multiAnswer.value.filter((l) => l <= maxLetter)
}

function buildAnswer() {
  if (form.type === 1) return form.answer
  if (form.type === 2) return [...multiAnswer.value].sort().join('')
  if (form.type === 3) return judgeAnswer.value
  return String(form.answer).trim()
}

/** 校验动态部分（选项/答案），返回错误文案或 null */
function validateDynamic() {
  if (!form.title.trim()) return '请输入题干'
  if (!form.score || form.score <= 0) return '分值必须大于 0'
  if (isChoice.value && form.options.some((o) => !o || !o.trim())) return '请填写完整所有选项'
  if (form.type === 1 && !form.answer) return '请选择正确答案'
  if (form.type === 2 && multiAnswer.value.length < 2) return '多选题至少选择 2 个正确答案'
  if (form.type === 3 && !judgeAnswer.value) return '请选择正确答案'
  if ((form.type === 4 || form.type === 5) && !buildAnswer()) return '请输入参考答案'
  return null
}

function open() {
  if (props.question) {
    const q = props.question
    form.type = q.type
    form.difficulty = q.difficulty
    form.category = q.category || ''
    form.title = q.title || ''
    form.options = q.type === 1 || q.type === 2 ? [...(q.options || [])] : []
    form.analysis = q.analysis || ''
    form.score = Number(q.score) || 2
    if (q.type === 1) {
      form.answer = q.answer || ''
    } else if (q.type === 2) {
      form.answer = ''
      multiAnswer.value = String(q.answer || '').split('').filter(Boolean)
    } else if (q.type === 3) {
      judgeAnswer.value = q.answer || ''
    } else {
      form.answer = q.answer || ''
    }
  } else {
    form.type = 1
    form.difficulty = 2
    form.category = ''
    form.title = ''
    form.options = ['', '', '', '']
    form.answer = ''
    form.analysis = ''
    form.score = 2
    multiAnswer.value = []
    judgeAnswer.value = ''
  }
  visible.value = true
}

async function save() {
  const err = validateDynamic()
  if (err) {
    ElMessage.warning(err)
    return
  }
  const payload = {
    type: form.type,
    difficulty: form.difficulty,
    category: form.category || undefined,
    title: form.title.trim(),
    options: isChoice.value ? form.options.map((o) => o.trim()) : undefined,
    answer: buildAnswer(),
    analysis: form.analysis || undefined,
    score: form.score,
  }
  saving.value = true
  try {
    if (props.question) {
      await updateQuestion({ id: props.question.id, ...payload })
      ElMessage.success('修改成功')
    } else {
      await addQuestion(payload)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="props.question ? '编辑题目' : '新增题目'"
    width="680px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="题型" prop="type">
            <el-select v-model="form.type" :disabled="!!props.question" style="width: 100%">
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="判断题" :value="3" />
              <el-option label="填空题" :value="4" />
              <el-option label="简答题" :value="5" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="难度" prop="difficulty">
            <el-select v-model="form.difficulty" style="width: 100%">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="分值" prop="score">
            <el-input-number v-model="form.score" :min="0.5" :max="100" :step="0.5" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="分类">
        <el-select
          v-model="form.category"
          placeholder="选择或输入新分类"
          clearable
          filterable
          allow-create
          default-first-option
          style="width: 100%"
        >
          <el-option v-for="c in props.categories" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>

      <el-form-item label="题干" prop="title">
        <el-input v-model="form.title" type="textarea" :rows="3" maxlength="1000" show-word-limit />
      </el-form-item>

      <!-- 单选/多选：选项编辑器 -->
      <template v-if="isChoice">
        <el-form-item label="选项">
          <div style="width: 100%">
            <div v-for="(_, i) in form.options" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <span style="line-height: 32px; width: 22px; text-align: center; flex-shrink: 0">
                {{ 'ABCDEFGH'[i] }}
              </span>
              <el-input v-model="form.options[i]" :placeholder="`选项 ${'ABCDEFGH'[i]} 内容`" maxlength="200" />
              <el-button type="danger" plain @click="removeOption(i)">删</el-button>
            </div>
            <el-button size="small" @click="addOption">+ 添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="正确答案">
          <el-radio-group v-if="form.type === 1" v-model="form.answer">
            <el-radio-button v-for="(_, i) in form.options" :key="i" :value="'ABCDEFGH'[i]">
              {{ 'ABCDEFGH'[i] }}
            </el-radio-button>
          </el-radio-group>
          <el-checkbox-group v-else v-model="multiAnswer">
            <el-checkbox-button v-for="(_, i) in form.options" :key="i" :value="'ABCDEFGH'[i]">
              {{ 'ABCDEFGH'[i] }}
            </el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>
      </template>

      <!-- 判断 -->
      <el-form-item v-if="form.type === 3" label="正确答案">
        <el-radio-group v-model="judgeAnswer">
          <el-radio-button value="对">对</el-radio-button>
          <el-radio-button value="错">错</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 填空/简答 -->
      <el-form-item v-if="form.type === 4" label="参考答案">
        <el-input v-model="form.answer" placeholder="学生答案须与参考答案精确匹配（自动去首尾空格）" maxlength="200" />
      </el-form-item>
      <el-form-item v-if="form.type === 5" label="参考答案">
        <el-input
          v-model="form.answer"
          type="textarea"
          :rows="4"
          maxlength="2000"
          show-word-limit
          placeholder="大模型将按参考答案对主观题判分"
        />
      </el-form-item>

      <el-form-item label="解析">
        <el-input v-model="form.analysis" type="textarea" :rows="2" maxlength="500" placeholder="选填" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>
