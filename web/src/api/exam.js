/**
 * 考试/防作弊接口 —— 对应 ExamController（/exam）
 *
 * 防作弊上报为纯 REST（无 WebSocket）：前端监听切屏后调 reportCheat，
 * 返回 {switchCount,maxScreenSwitch,exceeded,action}，action 2=服务端已强制交卷。
 */
import request from '@/utils/request'

/** 发布考试 ExamPublishDTO → 考试ID（admin/teacher） */
export const publishExam = (data) => request.post('/exam/publish', data)

/** 教师端考试列表分页（筛选 name） */
export const listExams = (params) => request.get('/exam/list', { params })

/** 考试详情（含试卷摘要+学生列表） */
export const getExam = (id) => request.get(`/exam/${id}`)

/** 学生进入考试：创建答卷，返回乱序题目（幂等，答题中重复调用恢复同一答卷） */
export const startExam = (id) => request.post(`/exam/${id}/start`)

/** 定时保存答案（草稿写 Redis）body:{questionId,answer} */
export const saveAnswer = (id, data) => request.post(`/exam/${id}/save`, data)

/** 交卷：草稿落库+客观题自动判分 */
export const submitExam = (id) => request.post(`/exam/${id}/submit`)

/** 学生考试大厅分页（筛选 status） */
export const listMyExams = (params) => request.get('/exam/my/list', { params })

/** 调整考试时间 body:{startTime,endTime}（admin/teacher） */
export const updateExamTime = (id, data) => request.put(`/exam/${id}/time`, data)

/** 发布成绩：交卷学生的成绩对学生端可见（admin/teacher，幂等） */
export const publishScore = (id) => request.post(`/exam/${id}/publish-score`, {})

/** 防作弊上报 body:{type,description,detail} → CheatReportVO */
export const reportCheat = (id, data) => request.post(`/exam/${id}/cheat`, data)
