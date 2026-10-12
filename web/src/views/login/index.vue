<script setup>
/**
 * 登录页
 * 流程：拉验证码 → 提交（验证码一次性，失败必须刷新）→ 按角色跳转
 */
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha, login } from '@/api/user'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const captcha = ref({ captchaKey: '', captchaImage: '' })

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  form.captchaCode = ''
  try {
    captcha.value = await getCaptcha()
  } catch {
    /* 拦截器已提示 */
  }
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const loginVO = await login({
      username: form.username,
      password: form.password,
      captchaKey: captcha.value.captchaKey,
      captchaCode: form.captchaCode,
    })
    userStore.loginSuccess(loginVO)
    ElMessage.success(`欢迎，${userStore.displayName}`)

    // 回跳优先；无回跳按角色落首页
    const redirect = route.query.redirect
    if (redirect && typeof redirect === 'string' && redirect.startsWith('/')) {
      router.replace(redirect)
    } else {
      router.replace(userStore.roleCodes.includes('student') ? '/student/exams' : '/dashboard')
    }
  } catch {
    // 验证码一次性：无论失败原因都换一张
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-card__title">AI在线考试系统</h2>
      <p class="login-card__sub">请使用系统账号登录</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="login-captcha">
            <el-input v-model="form.captchaCode" placeholder="验证码" :prefix-icon="Key" maxlength="6" />
            <img
              v-if="captcha.captchaImage"
              :src="captcha.captchaImage"
              alt="验证码"
              title="点击刷新"
              class="login-captcha__img"
              @click="refreshCaptcha"
            />
            <div v-else class="login-captcha__img login-captcha__loading" @click="refreshCaptcha">点击加载</div>
          </div>
        </el-form-item>
        <el-button type="primary" class="login-card__btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1f6feb 0%, #001529 100%);
}

.login-card {
  width: 380px;
  padding: 36px 32px 28px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);

  &__title {
    margin: 0 0 4px;
    text-align: center;
    color: var(--el-text-color-primary);
  }

  &__sub {
    margin: 0 0 24px;
    text-align: center;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  &__btn {
    width: 100%;
    margin-top: 4px;
  }
}

.login-captcha {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;

  &__img {
    width: 110px;
    height: 40px;
    flex-shrink: 0;
    border-radius: 4px;
    cursor: pointer;
    border: 1px solid var(--el-border-color);
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
