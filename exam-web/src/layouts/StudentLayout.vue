<template>
  <div class="s-layout">
    <header class="s-top">
      <div class="logo">教学考试平台</div>
      <nav>
        <router-link to="/student">待考 / 考试</router-link>
        <router-link to="/student/records">我的成绩</router-link>
        <router-link to="/student/knowledge">知识点掌握</router-link>
      </nav>
      <div class="right">
        <span>{{ user.user?.realName }}</span>
        <el-button text @click="onLogout">退出</el-button>
      </div>
    </header>
    <main class="s-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
const router = useRouter()
const user = useUserStore()
function onLogout() {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.s-layout { min-height: 100%; }
.s-top {
  height: 60px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; padding: 0 28px; gap: 28px;
}
.logo { font-weight: 700; }
nav { display: flex; gap: 18px; }
nav a { color: var(--muted); text-decoration: none; font-size: 14px; }
nav a.router-link-exact-active { color: var(--accent); font-weight: 600; }
.right { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.s-body { max-width: 1080px; margin: 0 auto; padding: 24px 20px 48px; }
</style>
