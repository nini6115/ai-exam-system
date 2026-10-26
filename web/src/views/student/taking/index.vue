<script setup>
/**
 * 学生答题页（独立全屏，不套 Layout）
 *
 * 交互流：
 * 1. 进入：POST /exam/{id}/start（幂等，刷新后重调恢复同一答卷/题序）
 * 2. 倒计时：基于服务器 endTime；≤5 分钟红色警告；归零自动交卷
 * 3. 选项乱序：每题一次 Fisher-Yates 置换，提交时映射回原始字母
 * 4. 自动保存：30s 轮询 dirtySet（silent，失败保留待下轮重试）
 * 5. 防作弊：visibilitychange 切屏/离开超时上报；action=1 弹警告，action=2 已被强制交卷→跳结果页
 * 6. 交卷：未答题统计确认 → flush → submit（"您已交卷"视为成功）
 *
 * 答案内部状态：单选存显示位索引，多选存显示位索引数组，保存时映射回题库原始字母。
 */
defineOptions({ name: 'ExamTaking' })

import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { startExam, saveAnswer, submitExam, reportCheat } from '@/api/exam'
import { useCountdown } from '@/hooks/useCountdown'
import { LETTERS, QUESTION_TYPE } from '@/constants'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId

// ---------- 考试数据 ----------
const loading = ref(true)
const exam = ref(null) // ExamStartVO
const questions = computed(() => exam.value?.questions || [])

/** 每题选项乱序映射：{ [questionId]: number[] } perm[显示位] = 原始索引 */
const permMap = {}

/** 答案内部状态 */
const singleAnswers = reactive({}) // questionId -> 显示位索引(number)
const multiAnswers = reactive({}) // questionId -> 显示位索引数组
const judgeAnswers = reactive({}) // questionId -> '对' | '错'
const textAnswers = reactive({}) // questionId -> 原文

/** 当前题索引（questions 内） */
const current = ref(0)
const currentQuestion = computed(() => questions.value[current.value] || {})

/** 按题型分组（答题卡分组展示） */
const sections = computed(() => {
  const map = new Map()
  questions.value.forEach((q, idx) => {
    const key = q.section || '题目'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(idx)
  })
  return [...map.entries()].map(([name, indexes]) => ({ name, indexes }))
})

// ---------- 答案序列化（显示位 → 题库原始字母） ----------
function permOf(q) {
  return permMap[q.questionId] || q.options?.map((_, i) => i) || []
}

function serializeAnswer(q) {
  if (q.type === 1) {
    const display = singleAnswers[q.questionId]
    return display == null ? '' : LETTERS[permOf(q)[display]]
  }
  if (q.type === 2) {
    const displays = multiAnswers[q.questionId] || []
    return [...displays].map((d) => LETTERS[permOf(q)[d]]).sort().join('')
  }
  if (q.type === 3) {
    return judgeAnswers[q.questionId] || ''
  }
  return (textAnswers[q.questionId] ?? '').trim()
}

function isAnswered(q) {
  return !!serializeAnswer(q)
}

const answeredCount = computed(() => questions.value.filter((q) => isAnswered(q)).length)

// ---------- 防抖动的"收藏"待保存集合 ----------
const dirty = new Set()
const saveState = ref('idle') // idle | saving | failed

function markDirty(q) {
  dirty.add(q.questionId)
}

// ---------- 倒计时 ----------
let countdown = null
let fiveMinWarned = false

function onCountdownEnd() {
  ElMessage.warning('考试时间到，正在自动交卷…')
  doSubmit(true)
}

/** 剩余不足 5 分钟一次性警告 */
watch(
  () => countdown?.urgent.value,
  (val) => {
    if (val && !fiveMinWarned) {
      fiveMinWarned = true
      ElMessage.warning('注意：剩余时间不足 5 分钟，请尽快完成作答')
    }
  },
)

// ---------- 自动保存 ----------
const AUTO_SAVE_INTERVAL = 30 * 1000
let saveTimer = null
/** 已被收卷（保存时后端告知已交卷/已结束）后停止一切请求 */
const collected = ref(false)

async function flushSaves() {
  if (collected.value || !dirty.size) return
  saveState.value = 'saving'
  const ids = [...dirty]
  for (const qid of ids) {
    const q = questions.value.find((x) => x.questionId === qid)
    if (!q) {
      dirty.delete(qid)
      continue
    }
    try {
      await saveAnswer(examId, { questionId: qid, answer: serializeAnswer(q) }, { silent: true })
      dirty.delete(qid)
    } catch (e) {
      const msg = String(e?.message || '')
      if (msg.includes('已交卷') || msg.includes('时间已到') || msg.includes('已结束')) {
        // 已被收卷：等同交卷完成
        collected.value = true
        stopAll()
        router.replace(`/student/result/${examId}`)
        return
      }
      // 网络/服务错误：题留在 dirty，下轮重试；只提示一次
      saveState.value = 'failed'
      return
    }
  }
  saveState.value = 'idle'
}

function startAutoSave() {
  saveTimer = setInterval(flushSaves, AUTO_SAVE_INTERVAL)
}

// ---------- 防作弊 ----------
const switchCount = ref(0)
const maxScreenSwitch = computed(() => exam.value?.maxScreenSwitch ?? 0)
let hiddenAt = null
let awayTimer = null
/** 同一次离开只报一种类型 */
let awayReported = false
/** 超限警告防重入 */
let warningShowing = false
let listenersBound = false

async function report(type, awaySeconds) {
  if (collected.value) return
  try {
    const vo = await reportCheat(examId, {
      type,
      description:
        type === 1 ? `切屏至标签页隐藏，${awaySeconds ?? 0} 秒后返回` : `离开页面超过 ${exam.value?.awayTimeout ?? 0} 秒`,
      detail: JSON.stringify({ hiddenAt, visibleAt: Date.now(), awaySeconds: awaySeconds ?? 0 }),
    })
    switchCount.value = vo.switchCount ?? switchCount.value
    if (vo.exceeded && vo.action === 1 && !warningShowing) {
      warningShowing = true
      ElMessageBox.alert(
        `切屏次数已达上限（${vo.switchCount}/${vo.maxScreenSwitch}），再次切屏将按考试规则处置，请专注作答！`,
        '切屏警告',
        { type: 'warning' },
      ).finally(() => {
        warningShowing = false
      })
    }
    if (vo.action === 2) {
      // 服务端已强制交卷
      collected.value = true
      stopAll()
      ElMessageBox.alert('切屏次数超限，本场考试已被强制交卷。', '考试结束', { type: 'error' }).finally(() => {
        router.replace(`/student/result/${examId}?type=force`)
      })
    }
  } catch {
    /* 上报失败忽略（silent 由拦截器处理网络提示） */
  }
}

function onVisibilityChange() {
  if (collected.value) return
  const awayTimeout = exam.value?.awayTimeout ?? 0
  if (document.hidden) {
    hiddenAt = Date.now()
    awayReported = false
    if (awayTimeout > 0) {
      awayTimer = setTimeout(() => {
        awayReported = true
        report(2, awayTimeout)
      }, awayTimeout * 1000)
    }
  } else {
    if (awayTimer) {
      clearTimeout(awayTimer)
      awayTimer = null
    }
    if (hiddenAt != null && !awayReported) {
      const seconds = Math.round((Date.now() - hiddenAt) / 1000)
      if (seconds >= 1) report(1, seconds)
    }
    hiddenAt = null
  }
}

function onBeforeUnload(e) {
  e.preventDefault()
  e.returnValue = ''
}

/** 禁止复制（forbidCopy=1，教学级尽力而为） */
function onCopyBlock(e) {
  e.preventDefault()
  ElMessage.warning('本场考试禁止复制')
}

function onKeyBlock(e) {
  if ((e.ctrlKey || e.metaKey) && ['c', 'v', 'x', 'a', 'p'].includes(e.key.toLowerCase())) {
    e.preventDefault()
  }
}

function bindCheatListeners() {
  if (listenersBound) return
  listenersBound = true
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('beforeunload', onBeforeUnload)
  if (exam.value?.forbidCopy === 1) {
    document.addEventListener('copy', onCopyBlock)
    document.addEventListener('paste', onCopyBlock)
    document.addEventListener('cut', onCopyBlock)
    document.addEventListener('contextmenu', onCopyBlock)
    document.addEventListener('selectstart', onCopyBlock)
    document.addEventListener('keydown', onKeyBlock)
  }
}

// ---------- 生命周期清理 ----------
function stopAll() {
  countdown?.stop()
  if (saveTimer) {
    clearInterval(saveTimer)
    saveTimer = null
  }
  if (awayTimer) {
    clearTimeout(awayTimer)
    awayTimer = null
  }
  if (listenersBound) {
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener('beforeunload', onBeforeUnload)
    document.removeEventListener('copy', onCopyBlock)
    document.removeEventListener('paste', onCopyBlock)
    document.removeEventListener('cut', onCopyBlock)
    document.removeEventListener('contextmenu', onCopyBlock)
    document.removeEventListener('selectstart', onCopyBlock)
    document.removeEventListener('keydown', onKeyBlock)
    listenersBound = false
  }
}

onBeforeUnmount(stopAll)

// ---------- 进入考试 ----------
/** 是否为刷新恢复（首次进入后为 true，用于页头提示） */
const resumed = ref(false)

async function init() {
  loading.value = true
  try {
    const vo = await startExam(examId)
    exam.value = vo
    resumed.value = performance.getEntriesByType('navigation').length > 0 && performance.getEntriesByType('navigation')[0].type !== 'navigate'

    // 初始化答案容器与选项乱序
    for (const q of vo.questions || []) {
      if (q.type === 1) singleAnswers[q.questionId] = null
      if (q.type === 2) multiAnswers[q.questionId] = []
      if (q.type === 3) judgeAnswers[q.questionId] = ''
      if (q.type === 4 || q.type === 5) textAnswers[q.questionId] = ''
      if ((q.type === 1 || q.type === 2) && vo.randomOptions === 1 && q.options?.length) {
        // Fisher-Yates：perm[显示位] = 原始索引，仅生成一次（刷新后 start 幂等同 seed 不变题序）
        const n = q.options.length
        const perm = Array.from({ length: n }, (_, i) => i)
        for (let i = n - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[perm[i], perm[j]] = [perm[j], perm[i]]
        }
        permMap[q.questionId] = perm
      }
    }

    countdown = useCountdown(vo.endTime, { onEnd: onCountdownEnd })
    countdown.start()
    startAutoSave()
    bindCheatListeners()
  } catch (e) {
    // 业务错误（未开始/已结束/不在名单/次数用完）拦截器已 toast
    router.replace('/student/exams')
    return
  } finally {
    loading.value = false
  }
}

onMounted(init)

// ---------- 交卷 ----------
const submitting = ref(false)

async function doSubmit(auto = false) {
  if (submitting.value || collected.value) return
  submitting.value = true
  try {
    const unanswered = questions.value.length - answeredCount.value
    if (!auto) {
      await ElMessageBox.confirm(
        unanswered > 0
          ? `共 ${questions.value.length} 题，还有 ${unanswered} 题未作答（含刷新后未重新作答的题目），确认交卷？`
          : `共 ${questions.value.length} 题，全部作答完成。确认交卷？`,
        '交卷确认',
        { confirmButtonText: '确认交卷', type: 'warning' },
      )
    }
    // 交卷前冲刷未保存答案
    await flushSaves()
    if (collected.value) return
    try {
      await submitExam(examId)
    } catch (e) {
      const msg = String(e?.message || '')
      if (msg.includes('您已交卷') || msg.includes('已结束') || msg.includes('时间已到')) {
        // 幂等：重复交卷/已超时收卷等同成功
      } else {
        throw e
      }
    }
    collected.value = true
    stopAll()
    ElMessage.success(auto ? '考试时间到，已自动交卷' : '交卷成功')
    router.replace(`/student/result/${examId}`)
  } catch {
    /* 确认取消或提交失败（拦截器已提示），留在本页 */
  } finally {
    submitting.value = false
  }
}

// ---------- 答题卡跳题 ----------
function jumpTo(index) {
  if (index < 0 || index >= questions.value.length) return
  flushSaves() // 切题前冲刷
  current.value = index
}
</script>

<template>
  <div v-loading="loading" class="taking-page">
    <template v-if="exam">
      <!-- 顶部栏 -->
      <div class="taking-header">
        <div class="taking-header__title">
          {{ exam.examName }}
          <el-tag size="small" type="info" disable-transitions style="margin-left: 8px">
            {{ questions.length }} 题
          </el-tag>
        </div>

        <div class="taking-header__center">
          <el-alert
            v-if="resumed"
            title="页面已刷新：请重新检查所有已答题目（此前保存的答案交卷时仍会提交）"
            type="warning"
            :closable="true"
            show-icon
            style="padding: 4px 12px"
          />
          <span v-else-if="saveState === 'failed'" style="color: var(--el-color-danger); font-size: 13px">
            答案保存失败，将在下轮自动重试
          </span>
        </div>

        <div class="taking-header__right">
          <span
            class="taking-countdown"
            :class="{ 'taking-countdown--urgent': countdown?.urgent.value }"
          >
            {{ countdown?.text.value }}
          </span>
          <span class="taking-switch">
            切屏 {{ switchCount }}/{{ maxScreenSwitch === 0 ? '不限' : maxScreenSwitch }}
          </span>
          <el-button type="primary" :loading="submitting" @click="doSubmit()">交卷</el-button>
        </div>
      </div>

      <!-- 主体 -->
      <div class="taking-body">
        <!-- 左：题目卡 -->
        <div class="taking-main">
          <div class="question-card">
            <div class="question-card__meta">
              第 {{ current + 1 }} / {{ questions.length }} 题
              <el-tag size="small" disable-transitions>{{ QUESTION_TYPE[currentQuestion.type] }}</el-tag>
              <el-tag size="small" type="info" disable-transitions>{{ currentQuestion.questionScore }} 分</el-tag>
            </div>

            <div class="question-card__title">{{ currentQuestion.title }}</div>

            <!-- 单选 -->
            <div v-if="currentQuestion.type === 1" class="question-card__options">
              <div
                v-for="(_, displayIdx) in currentQuestion.options"
                :key="displayIdx"
                class="option-item"
                :class="{ 'option-item--active': singleAnswers[currentQuestion.questionId] === displayIdx }"
                @click="singleAnswers[currentQuestion.questionId] = displayIdx; markDirty(currentQuestion)"
              >
                <span class="option-item__letter">{{ LETTERS[displayIdx] }}</span>
                {{ currentQuestion.options[permOf(currentQuestion)[displayIdx]] }}
              </div>
            </div>

            <!-- 多选 -->
            <div v-else-if="currentQuestion.type === 2" class="question-card__options">
              <div
                v-for="(_, displayIdx) in currentQuestion.options"
                :key="displayIdx"
                class="option-item"
                :class="{ 'option-item--active': (multiAnswers[currentQuestion.questionId] || []).includes(displayIdx) }"
                @click="
                  (() => {
                    const arr = multiAnswers[currentQuestion.questionId]
                    const i = arr.indexOf(displayIdx)
                    i >= 0 ? arr.splice(i, 1) : arr.push(displayIdx)
                    markDirty(currentQuestion)
                  })()
                "
              >
                <span class="option-item__letter">{{ LETTERS[displayIdx] }}</span>
                {{ currentQuestion.options[permOf(currentQuestion)[displayIdx]] }}
              </div>
              <p class="question-card__tip">多选题：选出全部正确选项</p>
            </div>

            <!-- 判断 -->
            <div v-else-if="currentQuestion.type === 3" class="question-card__options">
              <div
                v-for="opt in ['对', '错']"
                :key="opt"
                class="option-item"
                :class="{ 'option-item--active': judgeAnswers[currentQuestion.questionId] === opt }"
                style="width: 140px"
                @click="judgeAnswers[currentQuestion.questionId] = opt; markDirty(currentQuestion)"
              >
                {{ opt }}
              </div>
            </div>

            <!-- 填空 -->
            <div v-else-if="currentQuestion.type === 4" class="question-card__input">
              <el-input
                v-model="textAnswers[currentQuestion.questionId]"
                placeholder="请输入答案（须与参考答案精确匹配）"
                maxlength="200"
                @input="markDirty(currentQuestion)"
              />
            </div>

            <!-- 简答 -->
            <div v-else class="question-card__input">
              <el-input
                v-model="textAnswers[currentQuestion.questionId]"
                type="textarea"
                :rows="8"
                maxlength="2000"
                show-word-limit
                placeholder="请输入答案"
                @input="markDirty(currentQuestion)"
              />
            </div>

            <div class="question-card__nav">
              <el-button :disabled="current === 0" @click="jumpTo(current - 1)">上一题</el-button>
              <el-button :disabled="current === questions.length - 1" @click="jumpTo(current + 1)">下一题</el-button>
            </div>
          </div>
        </div>

        <!-- 右：答题卡 -->
        <div class="taking-aside">
          <div class="answer-sheet">
            <div class="answer-sheet__head">
              答题卡（已答 {{ answeredCount }}/{{ questions.length }}）
            </div>
            <div v-for="section in sections" :key="section.name" class="answer-sheet__group">
              <div class="answer-sheet__name">{{ section.name }}</div>
              <div class="answer-sheet__grid">
                <span
                  v-for="idx in section.indexes"
                  :key="idx"
                  class="sheet-cell"
                  :class="{
                    'sheet-cell--active': current === idx,
                    'sheet-cell--answered': isAnswered(questions[idx]),
                  }"
                  @click="jumpTo(idx)"
                >
                  {{ idx + 1 }}
                </span>
              </div>
            </div>
            <div class="answer-sheet__legend">
              <span class="sheet-cell sheet-cell--answered">已答</span>
              <span class="sheet-cell">未答</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.taking-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

.taking-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 20px;
  height: 56px;
  background: #001529;
  color: #fff;
  flex-shrink: 0;

  &__title {
    font-size: 15px;
    font-weight: 600;
    white-space: nowrap;
  }

  &__center {
    flex: 1;
    display: flex;
    justify-content: center;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.taking-countdown {
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #fff;

  &--urgent {
    color: var(--el-color-danger);
    animation: blink 1s infinite;
  }
}

@keyframes blink {
  50% {
    opacity: 0.55;
  }
}

.taking-switch {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
}

.taking-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.taking-main {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.question-card {
  max-width: 860px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 24px;

  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    margin-bottom: 16px;
  }

  &__title {
    font-size: 17px;
    line-height: 1.7;
    margin-bottom: 20px;
    white-space: pre-wrap;
  }

  &__options {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__input {
    max-width: 640px;
  }

  &__tip {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__nav {
    margin-top: 28px;
    display: flex;
    justify-content: space-between;
  }
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  line-height: 1.6;
  transition: all 0.15s;

  &:hover {
    border-color: var(--el-color-primary);
  }

  &--active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  &__letter {
    flex-shrink: 0;
    width: 22px;
    height: 22px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--el-border-color);
    border-radius: 50%;
    font-size: 12px;
  }

  &--active .option-item__letter {
    background: var(--el-color-primary);
    border-color: var(--el-color-primary);
    color: #fff;
  }
}

.taking-aside {
  width: 280px;
  flex-shrink: 0;
  overflow: auto;
  padding: 16px 16px 16px 0;
}

.answer-sheet {
  background: #fff;
  border-radius: 8px;
  padding: 16px;

  &__head {
    font-weight: 600;
    margin-bottom: 12px;
  }

  &__group {
    margin-bottom: 12px;
  }

  &__name {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-bottom: 8px;
  }

  &__grid {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__legend {
    display: flex;
    gap: 8px;
    margin-top: 8px;

    .sheet-cell {
      cursor: default;
    }
  }
}

.sheet-cell {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;

  &:hover {
    border-color: var(--el-color-primary);
  }

  &--active {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary);
  }

  &--answered {
    background: var(--el-color-success);
    border-color: var(--el-color-success);
    color: #fff;
  }
}
</style>
