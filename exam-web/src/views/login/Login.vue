<template>
  <div class="login-page">
    <div class="panel">
      <div class="intro">
        <h1>教学考试平台</h1>
        <p>教师出题组卷、学生在线作答、按知识点分析掌握情况。</p>
        <ul>
          <li>客观题自动评分，简答题人工阅卷</li>
          <li>考试倒计时以后端时间为准</li>
          <li>交卷后统计班级与个人知识点掌握度</li>
        </ul>
      </div>
      <div class="form">
        <h2>登录</h2>
        <el-form :model="form" @submit.prevent="onSubmit">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名 / 学号" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onSubmit">进入系统</el-button>
        </el-form>
        <div class="hints">
          <div>管理员 admin / admin123</div>
          <div>教师 teacher / teacher123</div>
          <div>学生 s2024001 / student123</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'teacher', password: 'teacher123' })

async function onSubmit() {
  loading.value = true
  try {
    const user = await store.login(form)
    router.push(user.role === 'STUDENT' ? '/student' : '/admin')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(1200px 400px at 10% -10%, #dbeafe 0%, transparent 50%),
    #f8fafc;
}
.panel {
  width: 860px;
  max-width: calc(100% - 32px);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  overflow: hidden;
}
.intro { padding: 40px 36px; background: #0f172a; color: #e2e8f0; }
.intro h1 { margin: 0 0 12px; color: #fff; font-size: 28px; }
.intro p { line-height: 1.7; }
.intro ul { padding-left: 18px; line-height: 1.9; color: #cbd5e1; }
.form { padding: 48px 36px; }
.form h2 { margin: 0 0 24px; }
.hints { margin-top: 24px; color: var(--muted); font-size: 12px; line-height: 1.8; }
@media (max-width: 800px) {
  .panel { grid-template-columns: 1fr; }
  .intro { display: none; }
}
</style>
