/**
 * 操作日志接口 —— 对应 SysOperLogController（/system/log，admin）
 */
import request from '@/utils/request'

/** 操作日志分页（模块/操作人/结果码/时间范围筛选） */
export const listOperLogs = (params) => request.get('/system/log/list', { params })
