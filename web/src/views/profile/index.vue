<script setup>
/**
 * 个人中心：资料编辑 + 修改密码
 * 改密成功后后端会强制下线该用户全部 token，前端同步本地登出并回登录页
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '@/api/user'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'Profile' })

const router = useRouter()
const userStore = useUserStore()

// ---------- 资料编辑 ----------
const profileRef = ref()
const profileSaving = ref(false)
const profileForm = reactive({
  realName: userStore.realName,
  gender: 0,
  phone: '',
  email: '',
  avatar: '',
})

// 回显已保存的资料，否则改完再看是空白，像"没保存"
onMounted(async () => {
  const vo = await getProfile()
  Object.assign(profileForm, {
    realName: vo.realName || '',
    gender: vo.gender ?? 0,
    phone: vo.phone || '',
    email: vo.email || '',
    avatar: vo.avatar || '',
  })
})

const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

async function saveProfile() {
  await profileRef.value.validate()
  profileSaving.value = true
  try {
    await updateProfile({ ...profileForm })
    userStore.syncProfile({ realName: profileForm.realName, avatar: profileForm.avatar })
    ElMessage.success('资料已更新')
  } finally {
    profileSaving.value = false
  }
}

// ---------- 修改密码 ----------
const pwdRef = ref()
const pwdSaving = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function savePassword() {
  await pwdRef.value.validate()
  pwdSaving.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    await ElMessageBox.alert('密码修改成功，请使用新密码重新登录', '提示', { type: 'success' })
    await userStore.logout()
    router.replace('/login')
  } finally {
    pwdSaving.value = false
  }
}
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" header="基本资料">
          <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="90px">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" maxlength="50" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio :value="0">未知</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" maxlength="20" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" maxlength="100" />
            </el-form-item>
            <el-form-item label="头像地址">
              <div style="display: flex; align-items: center; gap: 8px; width: 100%">
                <el-avatar :size="40" :src="profileForm.avatar || undefined">
                  {{ (profileForm.realName || '用').slice(0, 1) }}
                </el-avatar>
                <el-input v-model="profileForm.avatar" maxlength="255" placeholder="选填，图片 URL" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" header="修改密码">
          <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
