/**
 * 用户 store —— token、用户信息、角色
 * 手写 localStorage 持久化（不引持久化插件），登录时写、守卫/拦截器清
 */
import { defineStore } from 'pinia'
import { getToken, getUser, saveAuth, saveUser, clearAuth } from '@/utils/auth'
import { logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => {
    const stored = getUser() || {}
    return {
      token: getToken(),
      userId: stored.userId ?? null,
      username: stored.username || '',
      realName: stored.realName || '',
      avatar: stored.avatar || '',
      roles: stored.roles || [],
    }
  },

  getters: {
    /** 角色码数组，如 ['admin','teacher']，鉴权一律用 includes */
    roleCodes(state) {
      return (state.roles || []).map((r) => r.roleCode).filter(Boolean)
    },
    isAdmin() {
      return this.roleCodes.includes('admin')
    },
    /** 管理端角色（admin 或 teacher） */
    isStaff() {
      return this.roleCodes.includes('admin') || this.roleCodes.includes('teacher')
    },
    displayName(state) {
      return state.realName || state.username
    },
  },

  actions: {
    /** 登录成功后写入（LoginVO: token/userId/username/realName/avatar/roles） */
    loginSuccess(loginVO) {
      const { token, roles, ...user } = loginVO
      this.token = token
      this.userId = user.userId
      this.username = user.username || ''
      this.realName = user.realName || ''
      this.avatar = user.avatar || ''
      this.roles = roles || []
      saveAuth(token, { ...user, roles: roles || [] })
    },

    /** 修改资料后同步（不重置 loginAt） */
    syncProfile(profile) {
      if (profile.realName !== undefined) this.realName = profile.realName
      if (profile.avatar !== undefined) this.avatar = profile.avatar
      saveUser({ userId: this.userId, username: this.username, realName: this.realName, avatar: this.avatar, roles: this.roles })
    },

    /** 退出登录：尽力调后端清 token，无论成败都清本地 */
    async logout() {
      try {
        await logoutApi()
      } catch {
        /* 后端不可达也要完成本地登出 */
      }
      this.resetLocal()
    },

    resetLocal() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.realName = ''
      this.avatar = ''
      this.roles = []
      clearAuth()
    },
  },
})
