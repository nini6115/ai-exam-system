/**
 * 本地认证信息读写
 *
 * 后端 token 有效期 2 小时且无续期接口，登录时记录 loginAt，
 * 前端在剩余 1 分钟时即视为过期，避免考试/操作中途被硬踢。
 */
const TOKEN_KEY = 'ai_exam_token'
const USER_KEY = 'ai_exam_user'

/** 后端 Redis token TTL：7200 秒 */
export const TOKEN_TTL_MS = 7200 * 1000
/** 提前量：到期前 60 秒即判定过期 */
const EXPIRE_AHEAD_MS = 60 * 1000

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/** 登录成功后调用：写入 token 与用户信息（附登录时间戳） */
export function saveAuth(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify({ ...user, loginAt: Date.now() }))
}

/** 仅刷新用户信息（如修改资料后），不重置 loginAt */
export function saveUser(user) {
  const prev = getUser() || {}
  localStorage.setItem(USER_KEY, JSON.stringify({ ...prev, ...user, loginAt: prev.loginAt || Date.now() }))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 导航期预判：过期返回 true（页面内 API 的 401 仍由 axios 拦截器兜底） */
export function isTokenExpired() {
  const user = getUser()
  if (!user || !user.loginAt) return true
  return Date.now() - user.loginAt > TOKEN_TTL_MS - EXPIRE_AHEAD_MS
}
