<template>
  <div class="layout">
    <aside class="side">
      <div class="brand">
        <img class="brand-logo" src="../assets/logo.png" alt="教学考试平台" />
        <span>教学考试平台</span>
      </div>
      <div class="side-user">
        <UserAvatar size="md" :clickable="false" />
        <div class="meta">
          <strong>{{ user.displayName }}</strong>
          <span class="role-tag">{{ roleLabel }}</span>
        </div>
      </div>
      <el-menu :default-active="active" router background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/admin"><el-icon><Odometer /></el-icon>工作台</el-menu-item>
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
        <div class="user-chip">
          <UserAvatar size="sm" />
          <span class="user-chip-name">{{ user.displayName }}</span>
          <span class="user-chip-role">{{ roleLabel }}</span>
        </div>
        <el-button class="logout-btn" @click="onLogout">退出</el-button>
      </header>
      <div class="body">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const menuIndexes = [
  '/admin/users',
  '/admin/students',
  '/admin/knowledge',
  '/admin/questions',
  '/admin/papers',
  '/admin/exams',
  '/admin/marking',
  '/admin/scores',
  '/admin/analysis',
  '/admin/logs'
]
const active = computed(() => {
  const p = route.path
  return menuIndexes.find((m) => p === m || p.startsWith(`${m}/`)) || '/admin'
})
const roleLabel = computed(() => user.isAdmin ? '管理员' : '教师')

onMounted(() => {
  if (user.token && !user.avatarLoaded) user.loadAvatar()
})

function onLogout() {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100%; }
.side { width: 220px; background: var(--sidebar); color: #fff; flex-shrink: 0; }
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
}
.brand-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 8px;
  background: #000;
  flex-shrink: 0;
}
.side-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 12px 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
}
.side-user .meta { display: flex; flex-direction: column; gap: 4px; line-height: 1.25; min-width: 0; }
.side-user strong { font-size: 13px; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.role-tag {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 1px 8px;
  border: 1px solid rgba(148, 163, 184, 0.7);
  border-radius: 4px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
  font-style: normal;
}
.role-tag.light {
  border-color: #cbd5e1;
  color: var(--muted);
  background: #f8fafc;
}
.side :deep(.el-menu) { border-right: 0; }
.side :deep(.el-menu-item.is-active) { background: #1e3a8a; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.top {
  height: 56px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; justify-content: flex-end; padding: 0 20px; gap: 8px;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 10px 0 6px;
  border-radius: 999px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
} 

.user-chip-name {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-chip-role {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #93c5fd;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 1;
  flex-shrink: 0;
}
.logout-btn {
  height: 36px !important;
  padding: 0 14px !important;
  border-radius: 999px !important;
  border: 1px solid #fecaca !important;
  background: #fef2f2 !important;
  color: #dc2626 !important;
  font-size: 13px !important;
}
.logout-btn:hover,
.logout-btn:focus {
  background: #fee2e2 !important;
  border-color: #fca5a5 !important;
  color: #b91c1c !important;
}
.body { padding: 20px; }
</style>
