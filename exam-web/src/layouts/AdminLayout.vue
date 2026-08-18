<template>
  <div class="layout">
    <aside class="side">
      <div class="brand">教学考试平台</div>
      <el-menu :default-active="active" router background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/admin"><el-icon><Odometer /></el-icon>首页</el-menu-item>
        <el-menu-item v-if="user.isAdmin" index="/admin/users"><el-icon><User /></el-icon>账号管理</el-menu-item>
        <el-menu-item index="/admin/students"><el-icon><UserFilled /></el-icon>学生管理</el-menu-item>
        <el-menu-item index="/admin/knowledge"><el-icon><Share /></el-icon>知识点</el-menu-item>
        <el-menu-item index="/admin/questions"><el-icon><Collection /></el-icon>题库</el-menu-item>
        <el-menu-item index="/admin/papers"><el-icon><Document /></el-icon>试卷</el-menu-item>
        <el-menu-item index="/admin/exams"><el-icon><Calendar /></el-icon>考试</el-menu-item>
        <el-menu-item index="/admin/marking"><el-icon><EditPen /></el-icon>阅卷</el-menu-item>
        <el-menu-item index="/admin/scores"><el-icon><Trophy /></el-icon>成绩</el-menu-item>
        <el-menu-item index="/admin/analysis"><el-icon><DataAnalysis /></el-icon>学情分析</el-menu-item>
        <el-menu-item v-if="user.isAdmin" index="/admin/logs"><el-icon><List /></el-icon>操作日志</el-menu-item>
      </el-menu>
    </aside>
    <section class="main">
      <header class="top">
        <span>{{ user.user?.realName }} · {{ roleLabel }}</span>
        <el-button text @click="onLogout">退出</el-button>
      </header>
      <div class="body">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const active = computed(() => route.path)
const roleLabel = computed(() => user.isAdmin ? '管理员' : '教师')

function onLogout() {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100%; }
.side { width: 220px; background: var(--sidebar); color: #fff; flex-shrink: 0; }
.brand { height: 56px; display: flex; align-items: center; padding: 0 20px; font-weight: 700; letter-spacing: 0.04em; }
.side :deep(.el-menu) { border-right: 0; }
.side :deep(.el-menu-item.is-active) { background: #1e3a8a; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.top {
  height: 56px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; justify-content: flex-end; padding: 0 20px; gap: 8px;
}
.body { padding: 20px; }
</style>
