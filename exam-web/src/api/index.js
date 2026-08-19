import http from './http'

/** 前端调用的后端接口，路径与 controller 上的 @RequestMapping 对应。 */
export const login = (data) => http.post('/api/auth/login', data)
export const me = () => http.get('/api/auth/me')
export const logout = () => http.post('/api/auth/logout')

export const dashboard = () => http.get('/api/dashboard')

export const pageUsers = (params) => http.get('/api/users', { params })
export const saveUser = (data) => data.id ? http.put(`/api/users/${data.id}`, data) : http.post('/api/users', data)

export const pageStudents = (params) => http.get('/api/students', { params })
export const studentOptions = () => http.get('/api/students/options')
export const saveStudent = (data) => data.id ? http.put(`/api/students/${data.id}`, data) : http.post('/api/students', data)
export const removeStudent = (id) => http.delete(`/api/students/${id}`)
export const importStudents = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/api/students/import', fd)
}

export const knowledgeTree = () => http.get('/api/knowledge-points/tree')
export const saveKnowledge = (data) => data.id ? http.put(`/api/knowledge-points/${data.id}`, data) : http.post('/api/knowledge-points', data)
export const removeKnowledge = (id) => http.delete(`/api/knowledge-points/${id}`)

export const listCategories = () => http.get('/api/question-categories')
export const saveCategory = (data) => data.id ? http.put(`/api/question-categories/${data.id}`, data) : http.post('/api/question-categories', data)
export const removeCategory = (id) => http.delete(`/api/question-categories/${id}`)

export const pageQuestions = (params) => http.get('/api/questions', { params })
export const getQuestion = (id) => http.get(`/api/questions/${id}`)
export const saveQuestion = (data) => data.id ? http.put(`/api/questions/${data.id}`, data) : http.post('/api/questions', data)
export const removeQuestion = (id) => http.delete(`/api/questions/${id}`)

export const pagePapers = (params) => http.get('/api/papers', { params })
export const paperOptions = () => http.get('/api/papers/options')
export const getPaper = (id) => http.get(`/api/papers/${id}`)
export const savePaper = (data) => data.id ? http.put(`/api/papers/${data.id}`, data) : http.post('/api/papers', data)
export const removePaper = (id) => http.delete(`/api/papers/${id}`)

export const pageExams = (params) => http.get('/api/exams', { params })
export const getExam = (id) => http.get(`/api/exams/${id}`)
export const saveExam = (data) => data.id ? http.put(`/api/exams/${data.id}`, data) : http.post('/api/exams', data)
export const publishExam = (id) => http.post(`/api/exams/${id}/publish`)
export const stopExam = (id) => http.post(`/api/exams/${id}/stop`)
export const examStatistics = (id) => http.get(`/api/exams/${id}/statistics`)

export const markingList = (params) => http.get('/api/marking/records', { params })
export const markingDetail = (id) => http.get(`/api/marking/records/${id}`)
export const markAnswer = (id, data) => http.post(`/api/marking/answers/${id}`, data)

export const scores = (params) => http.get('/api/scores', { params })
export const classKnowledge = (className) => http.get(`/api/analysis/classes/${encodeURIComponent(className)}/knowledge`)
export const studentKnowledgeAdmin = (studentId) => http.get(`/api/analysis/students/${studentId}/knowledge`)
export const logs = (params) => http.get('/api/logs', { params })

export const myExams = () => http.get('/api/student/exams')
export const startExam = (id) => http.post(`/api/student/exams/${id}/start`)
export const examQuestions = (recordId) => http.get(`/api/student/records/${recordId}/questions`)
export const saveAnswers = (recordId, data) => http.put(`/api/student/records/${recordId}/answers`, data)
export const submitExam = (recordId, data) => http.post(`/api/student/records/${recordId}/submit`, data)
export const reviewExam = (recordId) => http.get(`/api/student/records/${recordId}/review`)
export const myKnowledge = () => http.get('/api/student/knowledge-stats')
