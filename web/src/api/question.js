/**
 * 题库接口 —— 对应 QuestionController（/question）
 * 题型：1单选 2多选 3判断 4填空 5简答；难度：1简单 2中等 3困难
 */
import request from '@/utils/request'

/** 分页查询题目（筛选 type/difficulty/category/title） */
export const listQuestions = (params) => request.get('/question/list', { params })

/** 题目分类去重列表（下拉用） */
export const listCategories = () => request.get('/question/category')

/** 题目详情（含答案，编辑回填用） */
export const getQuestion = (id) => request.get(`/question/${id}`)

/** 新增题目 */
export const addQuestion = (data) => request.post('/question', data)

/** 修改题目 */
export const updateQuestion = (data) => request.put('/question', data)

/** 删除题目（逻辑删除） */
export const deleteQuestion = (id) => request.delete(`/question/${id}`)
