/** 题型常量：与后端 Question.questionType 枚举值一致，所有页面统一从这里取。 */
export const QUESTION_TYPES = [
  { v: 'SINGLE', l: '单选' }, { v: 'MULTIPLE', l: '多选' }, { v: 'JUDGE', l: '判断' },
  { v: 'FILL', l: '填空' }, { v: 'ESSAY', l: '简答' }, { v: 'TERM', l: '关键词解释' }
]
export const typeLabel = (v) => QUESTION_TYPES.find(t => t.v === v)?.l || v
/** 主观题：需要教师阅卷（简答、关键词解释）。 */
export const isSubjective = (v) => v === 'ESSAY' || v === 'TERM'
/** 选择类题型：有选项列表。 */
export const isChoice = (v) => ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(v)
