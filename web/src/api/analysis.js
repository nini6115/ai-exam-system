/**
 * 成绩分析接口 —— 对应 AnalysisController（/analysis，admin/teacher）
 */
import request from '@/utils/request'

/** 统计概览：参考/缺考、均分极值、及格率、分数段分布 */
export const getStats = (examId) => request.get(`/analysis/${examId}/stats`)

/** 成绩明细分页（全名单含未考，keyword 搜姓名/学号） */
export const listScores = (examId, params) => request.get(`/analysis/${examId}/scores`, { params })

/** CSV 成绩导出（blob，交给 download.js 处理，注意后端业务错误也是 HTTP 200） */
export const exportScores = (examId, keyword) =>
  request.get(`/analysis/${examId}/export`, { params: { keyword }, responseType: 'blob' })
