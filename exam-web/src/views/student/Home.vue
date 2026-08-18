<template>
  <div>
    <div class="page-head"><div><h2>我的考试</h2><p>开始后倒计时由服务器控制，请勿刷新过频以外的异常退出，答案会自动保存</p></div></div>
    <el-empty v-if="!list.length" description="暂无考试" />
    <div v-for="item in list" :key="item.examId" class="exam-card">
      <div>
        <h3>{{ item.examName }}</h3>
        <p>{{ item.startTime }} 至 {{ item.endTime }} · {{ item.durationMinutes }} 分钟 · 满分 {{ item.totalPaperScore }}</p>
      </div>
      <div class="actions">
        <el-tag>{{ statusText(item) }}</el-tag>
        <el-button v-if="canStart(item)" type="primary" @click="go(item)">开始考试</el-button>
        <el-button v-else-if="item.recordStatus === 'ANSWERING'" type="primary" @click="go(item)">继续答题</el-button>
        <el-button v-else-if="item.recordId" @click="$router.push(`/student/review/${item.recordId}`)">查看</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myExams } from '../../api'

const router = useRouter()
const list = ref([])
function statusText(item) {
  if (item.recordStatus === 'MARKING') return '阅卷中'
  if (item.recordStatus === 'FINISHED') return item.passed === 1 ? `已完成 ${item.totalScore}分` : `已完成 ${item.totalScore ?? ''}分`
  if (item.recordStatus === 'ANSWERING') return '答题中'
  return { PUBLISHED: '未开始', ONGOING: '进行中', FINISHED: '已结束' }[item.runtimeStatus] || item.runtimeStatus
}
function canStart(item) {
  return item.runtimeStatus === 'ONGOING' && !item.recordStatus
}
function go(item) {
  router.push(`/exam/${item.examId}`)
}
onMounted(async () => {
  list.value = (await myExams()).data
})
</script>

<style scoped>
.exam-card {
  background: #fff; border: 1px solid var(--line); border-radius: 12px;
  padding: 18px 20px; margin-bottom: 12px; display: flex; justify-content: space-between; gap: 16px; align-items: center;
}
h3 { margin: 0 0 6px; }
p { margin: 0; color: var(--muted); }
.actions { display: flex; align-items: center; gap: 10px; }
</style>
