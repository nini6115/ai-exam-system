/**
 * 路由实例与全局守卫
 *
 * 守卫顺序：public 放行 → 无 token/预判过期 → 登录（带回跳）→
 * 有 token 无角色信息（脏数据）→ 登录 → meta.roles 不匹配 → 403
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { routes } from './routes'
import { getToken, getUser, isTokenExpired, clearAuth } from '@/utils/auth'
import { useUserStore } from '@/store/user'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - AI在线考试系统` : 'AI在线考试系统'

  if (to.meta.public) return true

  const token = getToken()
  if (!token || isTokenExpired()) {
    clearAuth()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const userStore = useUserStore()
  // localStorage 有 token 但角色为空：恢复 store，仍无角色则视为脏数据重新登录
  if (!userStore.roleCodes.length) {
    const stored = getUser()
    if (stored?.roles?.length) {
      userStore.$patch({
        userId: stored.userId,
        username: stored.username || '',
        realName: stored.realName || '',
        avatar: stored.avatar || '',
        roles: stored.roles,
        token,
      })
    } else {
      clearAuth()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.roles && !to.meta.roles.some((r) => userStore.roleCodes.includes(r))) {
    ElMessage.warning('无权访问该页面')
    return '/403'
  }

  return true
})

export default router
