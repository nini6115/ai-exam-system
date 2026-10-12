<script setup>
/**
 * 发布考试：ExamPublishDTO 全字段
 * 防作弊参数 + 指定考生；duration>110 分钟给黄色提示（登录 token 2 小时硬过期）
 */
defineOptions({ name: 'ExamPublish' })

import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { publishExam } from '@/api/exam'
import { listPapers } from '@/api/paper'
import UserSelectDialog from '@/components/UserSelectDialog.vue'

const router = useRouter()
const saving = ref(false)
const formRef = ref()
const selectDialogRef = ref()

const papers = ref([])

const form = reactive({
  name: '',
  paperId: null,
  timeRange: [],
  duration: null,
  allowLateMinutes: 0,
  maxAttempts: 1,
  randomOrder: 0,
  randomOptions: 0,
  maxScreenSwitch: 5,
  screenSwitchAction: 1,
  awayTimeout: 30,
  forbidCopy: 1,
  showScoreAfter: 0,
})

const rules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择考试时间范围', trigger: 'change' }],
}

/** 登录 token 2 小时硬过期：时长超过 110 分钟考试中途必然掉线 */
const durationWarning = computed(() => form.duration && form.duration > 110)

// 已选考生
const students = ref([])

function openSelect() {
  selectDialogRef.value.open(students.value)
}

function onStudentsConfirm(list) {
  students.value = list
}

async function submit() {
  await formRef.value.validate()
  if (!students.value.length) {
    ElMessage.warning('请选择考生（至少 1 人）')
    return
  }
  saving.value = true
  try {
    await publishExam({
      name: form.name.trim(),
      paperId: form.paperId,
      startTime: dayjs(form.timeRange[0]).format('YYYY-MM-DD HH:mm:ss'),
      endTime: dayjs(form.timeRange[1]).format('YYYY-MM-DD HH:mm:ss'),
      duration: form.duration ?? undefined,
      allowLateMinutes: form.allowLateMinutes,
      maxAttempts: form.maxAttempts,
      randomOrder: form.randomOrder,
      randomOptions: form.randomOptions,
      maxScreenSwitch: form.maxScreenSwitch,
      screenSwitchAction: form.screenSwitchAction,
      awayTimeout: form.awayTimeout,
      forbidCopy: form.forbidCopy,
      showScoreAfter: form.showScoreAfter,
      userIds: students.value.map((s) => s.userId),
    })
    ElMessage.success('发布成功')
    router.replace('/exam')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    // 试卷下拉取前 100 条（服务端上限）
    const data = await listPapers({ pageNum: 1, pageSize: 100 })
    papers.value = data?.list || []
  } catch {
    /* 拦截器已提示 */
  }
})
</script>

<template>
  <el-card shadow="never" style="max-width: 900px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
      <el-divider content-position="left">基本信息</el-divider>

      <el-form-item label="考试名称" prop="name">
        <el-input v-model="form.name" maxlength="100" placeholder="如：2026 春期期中考试" />
      </el-form-item>
      <el-form-item label="选择试卷" prop="paperId">
        <el-select v-model="form.paperId" filterable placeholder="选择试卷" style="width: 100%">
          <el-option
            v-for="paper in papers"
            :key="paper.id"
            :label="`${paper.name}（${paper.questionCount} 题 / ${paper.totalScore} 分 / ${paper.duration} 分钟）`"
            :value="paper.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="考试时间范围" prop="timeRange">
        <el-date-picker
          v-model="form.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="答题时长(分钟)">
        <el-input-number v-model="form.duration" :min="5" :max="240" placeholder="默认取试卷时长" style="width: 200px" />
        <el-alert
          v-if="durationWarning"
          type="warning"
          :closable="false"
          show-icon
          title="时长超过 110 分钟：登录凭证 2 小时硬过期，学生考试中途将需要重新登录（已保存答案不丢失，但不建议）"
          style="margin-left: 12px; flex: 1"
        />
      </el-form-item>
      <el-form-item label="迟到限制(分钟)">
        <el-input-number v-model="form.allowLateMinutes" :min="0" :max="120" style="width: 200px" />
        <span class="form-tip">开始后多少分钟内允许进入考试，0 = 不允许迟到进入</span>
      </el-form-item>

      <el-divider content-position="left">考生</el-divider>

      <el-form-item label="指定考生">
        <el-button @click="openSelect">选择考生</el-button>
        <el-tag v-if="students.length" type="success" style="margin-left: 12px">
          已选 {{ students.length }} 人
        </el-tag>
        <el-tag v-else type="info" style="margin-left: 12px">未选择</el-tag>
      </el-form-item>

      <el-divider content-position="left">防作弊设置</el-divider>

      <el-form-item label="最大切屏次数">
        <el-input-number v-model="form.maxScreenSwitch" :min="0" :max="99" style="width: 160px" />
        <span class="form-tip">0 = 不限制</span>
      </el-form-item>
      <el-form-item label="切屏超限处置">
        <el-radio-group v-model="form.screenSwitchAction">
          <el-radio :value="1">警告（可继续作答）</el-radio>
          <el-radio :value="2">强制交卷</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="离开超时(秒)">
        <el-input-number v-model="form.awayTimeout" :min="0" :max="600" style="width: 160px" />
        <span class="form-tip">页面离开超过该秒数记为「离开超时」，0 = 不检测</span>
      </el-form-item>
      <el-form-item label="题目乱序">
        <el-switch v-model="form.randomOrder" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="选项乱序">
        <el-switch v-model="form.randomOptions" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="禁止复制">
        <el-switch v-model="form.forbidCopy" :active-value="1" :inactive-value="0" />
        <span class="form-tip">答题页禁用复制/右键（尽力而为）</span>
      </el-form-item>
      <el-form-item label="最大考试次数">
        <el-input-number v-model="form.maxAttempts" :min="1" :max="10" style="width: 160px" />
      </el-form-item>
      <el-form-item label="交卷后显示成绩">
        <el-switch v-model="form.showScoreAfter" :active-value="1" :inactive-value="0" />
        <span class="form-tip">关闭时学生交卷后需等教师发布成绩</span>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">发布考试</el-button>
        <el-button @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>

    <UserSelectDialog ref="selectDialogRef" @confirm="onStudentsConfirm" />
  </el-card>
</template>

<style lang="scss" scoped>
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
