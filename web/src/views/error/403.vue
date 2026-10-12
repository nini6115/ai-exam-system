<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <h1 class="error-page__code">403</h1>
    <p class="error-page__text">抱歉，您没有权限访问该页面</p>
    <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
  </div>
</template>

<style lang="scss" scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: 12px;

  &__code {
    margin: 0;
    font-size: 96px;
    color: var(--el-color-info-light-3);
  }

  &__text {
    margin: 0 0 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
