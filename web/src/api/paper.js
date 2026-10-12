/**
 * 试卷接口 —— 对应 ExamPaperController（/paper）
 * 注意：后端无试卷修改/删除接口，前端只做创建与查看
 */
import request from '@/utils/request'

/** 智能组卷（按题型/难度/分类随机抽题）→ 试卷ID */
export const generatePaper = (data) => request.post('/paper/generate', data)

/** 手动组卷（按题目ID列表）→ 试卷ID */
export const createPaper = (data) => request.post('/paper/create', data)

/** 分页查询试卷列表 */
export const listPapers = (params) => request.get('/paper/list', { params })

/** 试卷详情（基本信息+题目列表，不含答案） */
export const getPaper = (id) => request.get(`/paper/${id}`)
