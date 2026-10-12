<script setup>
/**
 * 应用根组件：
 * - 承载路由出口
 * - 统一处理 auth:expired（axios 拦截器广播）：普通页跳登录带回跳；
 *   考试页改弹确认框（重登后 start 幂等恢复答题）
 */
import { onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { bus } from '@/utils/bus'

const route = useRoute()
const router = useRouter()

let handling = false

async function onAuthExpired() {
  if (handling) return
  handling = true
  setTimeout(() => {
    handling = false
  }, 3000)

  if (route.path.startsWith('/exam-taking')) {
    try {
      await ElMessageBox.confirm(
        '登录已过期。重新登录后将自动回到本场考试继续作答（已保存的答案不会丢失）。',
        '登录已过期',
        { confirmButtonText: '重新登录', cancelButtonText: '稍后处理', type: 'warning' },
      )
    } catch {
      return
    }
  } else if (route.path === '/login') {
    return
  }
  router.replace({ path: '/login', query: { redirect: route.fullPath } })
}

onMounted(() => bus.on('auth:expired', onAuthExpired))
onUnmounted(() => bus.off('auth:expired', onAuthExpired))
</script>

<template>
  <router-view />
</template>
