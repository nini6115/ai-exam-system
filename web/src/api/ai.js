/**
 * AI 判卷接口 —— 对应 AiGradingController（/ai，admin/teacher）
 */
import request from '@/utils/request'

/** 触发整场考试批量 AI 判卷（同步、可重复调用重试）→ 逐题失败结果 */
export const gradeExam = (examId) => request.post(`/ai/exams/${examId}/grade`)

/** 答卷复核视图：逐题 AI/人工/系统判分详情 */
export const getSheetDetails = (examId, sheetId) =>
  request.get(`/ai/exams/${examId}/sheets/${sheetId}/details`)

/** 人工改分一道主观题 → 该卷最新成绩汇总 */
export const manualGrade = (detailId, data) => request.put(`/ai/details/${detailId}`, data)
