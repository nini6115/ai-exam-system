/**
 * 业务枚举与答案格式工具
 *
 * 答案格式契约（与后端 AnswerSheetServiceImpl.gradeObjective 对齐）：
 * - 单选：字母，如 "B"
 * - 多选：无分隔字母串，如 "ACD"（后端按字符排序比对，顺序无关）
 * - 判断："对" / "错"（中文）
 * - 填空/简答：原文（后端判分 trim）
 */

/** 选项字母表（最多支持 8 个选项） */
export const LETTERS = 'ABCDEFGH'

export const QUESTION_TYPE = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
export const QUESTION_TYPE_TAG = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }

export const DIFFICULTY = { 1: '简单', 2: '中等', 3: '困难' }
export const DIFFICULTY_TAG = { 1: 'success', 2: 'warning', 3: 'danger' }

/** 用户状态：1正常 0禁用 */
export const USER_STATUS = { 1: '正常', 0: '禁用' }
export const USER_STATUS_TAG = { 1: 'success', 0: 'danger' }

export const GENDER = { 0: '未知', 1: '男', 2: '女' }

/** 考试状态（落库值，展示状态按时间实时推导）：1未开始 2进行中 3已结束 */
export const EXAM_STATUS = { 1: '未开始', 2: '进行中', 3: '已结束' }
export const EXAM_STATUS_TAG = { 1: 'info', 2: 'success', 3: 'danger' }

/** 答卷状态：1答题中 2已交卷 3强制交卷 4超时自动交卷 */
export const SHEET_STATUS = { 1: '答题中', 2: '已交卷', 3: '强制交卷', 4: '超时自动交卷' }
export const SHEET_STATUS_TAG = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }

/** 判分方式：1系统 2人工 3 AI */
export const GRADED_BY = { 1: '系统', 2: '人工', 3: 'AI' }
export const GRADED_BY_TAG = { 1: 'info', 2: 'warning', 3: 'success' }

/** 防作弊上报类型：1切屏 2离开超时 */
export const CHEAT_TYPE = { 1: '切屏', 2: '离开超时' }

/** 防作弊处置动作：0仅记录 1前端警告 2服务端已强制交卷 */
export const CHEAT_ACTION = { 0: '记录', 1: '警告', 2: '强制交卷' }

/**
 * 多选题答案序列化：显示位字母数组 → 按字母排序拼接（"AD"+"CA" → "ACD"）
 * @param {string[]} letters 如 ['C','A','D']
 */
export function serializeMultiAnswer(letters) {
  return [...letters].sort().join('')
}

/**
 * 多选题答案反序列化："ACD" → ['A','C','D']
 * @param {string} answer
 */
export function parseMultiAnswer(answer) {
  return answer ? String(answer).split('') : []
}

/** 通用 map 取值（取不到返回原值），供 EnumTag 等使用 */
export function enumLabel(map, value) {
  return map[value] ?? value
}
