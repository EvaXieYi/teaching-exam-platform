import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// /admin 教师和管理员；/student 学生；/exam/:id 全屏答题（无侧边栏）
const routes = [
  { path: '/login', component: () => import('../views/login/Login.vue') },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    children: [
      { path: '', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', component: () => import('../views/admin/Users.vue') },
      { path: 'students', component: () => import('../views/admin/Students.vue') },
      { path: 'knowledge', component: () => import('../views/admin/Knowledge.vue') },
      { path: 'questions', component: () => import('../views/admin/Questions.vue') },
      { path: 'questions/edit/:id?', component: () => import('../views/admin/QuestionEdit.vue') },
      { path: 'papers', component: () => import('../views/admin/Papers.vue') },
      { path: 'papers/edit/:id?', component: () => import('../views/admin/PaperEdit.vue') },
      { path: 'exams', component: () => import('../views/admin/Exams.vue') },
      { path: 'exams/edit/:id?', component: () => import('../views/admin/ExamEdit.vue') },
      { path: 'marking', component: () => import('../views/admin/Marking.vue') },
      { path: 'marking/:recordId', component: () => import('../views/admin/MarkingDetail.vue') },
      { path: 'scores', component: () => import('../views/admin/Scores.vue') },
      { path: 'analysis', component: () => import('../views/admin/Analysis.vue') },
      { path: 'logs', component: () => import('../views/admin/Logs.vue') }
    ]
  },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    children: [
      { path: '', component: () => import('../views/student/Home.vue') },
      { path: 'records', component: () => import('../views/student/Records.vue') },
      { path: 'knowledge', component: () => import('../views/student/Knowledge.vue') },
      { path: 'review/:recordId', component: () => import('../views/student/Review.vue') }
    ]
  },
  { path: '/exam/:examId', component: () => import('../views/exam/ExamTaking.vue') },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 未登录去登录页；学生不能进 /admin，教师不能进 /student 和答题页
router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.path === '/login') {
    if (store.token) return store.homePath()
    return true
  }
  if (!store.token) return '/login'
  if (!store.user) {
    try { await store.loadMe() } catch { store.logout(); return '/login' }
  } else if (!store.avatarLoaded) {
    store.loadAvatar()
  }
  if (to.path.startsWith('/admin') && store.isStudent) return '/student'
  if (to.path.startsWith('/student') && !store.isStudent) return '/admin'
  if (to.path.startsWith('/exam') && !store.isStudent) return '/admin'
  return true
})

export default router
