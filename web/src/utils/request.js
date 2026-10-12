/**
 * axios 统一封装
 *
 * 后端约定（见 docs/superpowers/2026-10-04-开发规范.md）：
 * - 请求头 Authorization: <token>（裸 token，无 Bearer 前缀）
 * - 响应 AjaxResult{code,msg,data}：HTTP 状态恒为 200，仅未登录时返回 HTTP 401
 * - code：200成功 400参数校验失败 401未登录 403无权限 500业务异常
 * - 分页返回 data:{list,total,pageNum,pageSize}
 *
 * 自定义请求配置：
 * - silent: true  时 400/500/网络错误不弹全局提示，仅 reject（轮询类接口用）
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import { bus } from './bus'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

/** 防并发 401 重复跳转的模块级标志 */
let redirecting = false

function handleUnauthorized() {
  if (!redirecting) {
    redirecting = true
    clearAuth()
    // 跳转决策交给 App.vue（考试页有专用确认框流程），避免 request ↔ router 循环依赖
    bus.emit('auth:expired')
    setTimeout(() => {
      redirecting = false
    }, 3000)
  }
  return Promise.reject(new Error('登录已过期，请重新登录'))
}

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    // blob 下载（CSV 导出）拿不到 body.code，原样放行给 download.js 按 content-type 判定
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    // 非标准结构直接透传（理论上不会出现，防御性处理）
    if (body == null || typeof body.code !== 'number') {
      return body
    }
    const silent = response.config.silent === true
    if (body.code === 200) {
      return body.data
    }
    if (body.code === 401) {
      return handleUnauthorized()
    }
    if (body.code === 403) {
      if (!silent) ElMessage.error(body.msg || '无权限访问')
      return Promise.reject(new Error(body.msg || '无权限访问'))
    }
    // 400 参数校验 / 500 业务异常：msg 由后端拼好，直接展示
    if (!silent) ElMessage.error(body.msg || '操作失败')
    return Promise.reject(new Error(body.msg || '操作失败'))
  },
  (error) => {
    const silent = error.config?.silent === true
    if (error.response?.status === 401) {
      return handleUnauthorized()
    }
    if (!silent) {
      ElMessage.error(error.message?.includes('timeout') ? '请求超时，请重试' : '网络异常或服务器无响应')
    }
    return Promise.reject(error)
  },
)

export default service
