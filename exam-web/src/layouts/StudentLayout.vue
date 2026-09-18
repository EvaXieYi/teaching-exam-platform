<template>
  <div class="s-layout">
    <header class="s-top">
      <div class="logo">
        <img src="../assets/logo.png" alt="教学考试平台" />
        <span>教学考试平台</span>
      </div>
      <nav>
        <router-link to="/student">待考 / 考试</router-link>
        <router-link to="/student/records">我的成绩</router-link>
        <router-link to="/student/knowledge">知识点掌握</router-link>
      </nav>
      <div class="right">
        <UserAvatar size="sm" />
        <span>{{ user.displayName }}</span>
        <el-button text @click="onLogout">退出</el-button>
      </div>
    </header>
    <main class="s-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()
const user = useUserStore()

onMounted(() => {
  if (user.token && !user.avatarLoaded) user.loadAvatar()
})

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
.logo { font-weight: 700; display: flex; align-items: center; gap: 8px; }
.logo img { width: 28px; height: 28px; object-fit: contain; border-radius: 6px; background: #000; }
nav { display: flex; gap: 18px; }
nav a { color: var(--muted); text-decoration: none; font-size: 14px; }
nav a.router-link-exact-active { color: var(--accent); font-weight: 600; }
.right { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.s-body { max-width: 1080px; margin: 0 auto; padding: 24px 20px 48px; }
</style>
