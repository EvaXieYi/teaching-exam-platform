<template>
  <div class="take" v-if="paper">
    <header>
      <strong>{{ paper.examName }}</strong>
      <span :class="{ danger: remain < 300 }">剩余 {{ clock }}</span>
      <el-button type="primary" @click="submit">交卷</el-button>
    </header>
    <div class="wrap">
      <aside>
        <button
          v-for="q in paper.questions" :key="q.questionId"
          :class="{ on: q.questionId === current.questionId, done: !!q.studentAnswer, flag: q.flagged === 1 }"
          @click="current = q"
        >{{ q.seq }}</button>
      </aside>
      <section v-if="current">
        <div class="meta">第 {{ current.seq }} 题 / {{ paper.questions.length }} · {{ typeName(current.questionType) }} · {{ current.questionScore }} 分</div>
        <div class="stem">{{ current.content }}</div>
        <div v-if="current.questionType === 'SINGLE' || current.questionType === 'JUDGE'">
          <el-radio-group v-model="current.studentAnswer" @change="persist">
            <el-radio v-for="o in current.options" :key="o.optionKey" :label="o.optionKey" style="display:block;margin:10px 0">
              {{ o.optionKey }}. {{ o.optionContent }}
            </el-radio>
          </el-radio-group>
        </div>
        <div v-else-if="current.questionType === 'MULTIPLE'">
          <el-checkbox-group :model-value="multi" @change="onMulti">
            <el-checkbox v-for="o in current.options" :key="o.optionKey" :label="o.optionKey" style="display:block;margin:10px 0">
              {{ o.optionKey }}. {{ o.optionContent }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <div v-else>
          <el-input v-model="current.studentAnswer" type="textarea" :rows="current.questionType === 'ESSAY' ? 10 : 3" @change="persist" />
          <div v-if="current.questionType === 'ESSAY'" class="count">{{ (current.studentAnswer || '').length }} 字</div>
        </div>
        <el-button style="margin-top:16px" @click="toggleFlag">{{ current.flagged === 1 ? '取消标记' : '标记未确定' }}</el-button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { startExam, saveAnswers, submitExam } from '../../api'

const map = { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }
const typeName = (v) => map[v] || v
const route = useRoute()
const router = useRouter()
const paper = ref(null)
const current = ref(null)
const remain = ref(0)
let timer
let saver

const clock = computed(() => {
  const s = Math.max(0, remain.value)
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const sec = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${sec}`
})
const multi = computed(() => (current.value?.studentAnswer || '').split(',').filter(Boolean))

function payload() {
  return paper.value.questions.map(q => ({
    questionId: q.questionId,
    studentAnswer: q.studentAnswer,
    flagged: q.flagged || 0
  }))
}
async function persist() {
  if (!paper.value) return
  await saveAnswers(paper.value.recordId, payload())
}
function onMulti(val) {
  current.value.studentAnswer = val.join(',')
  persist()
}
function toggleFlag() {
  current.value.flagged = current.value.flagged === 1 ? 0 : 1
  persist()
}
async function submit() {
  await ElMessageBox.confirm('确认交卷？交卷后不能修改。')
  await submitExam(paper.value.recordId, payload())
  ElMessage.success('已交卷')
  router.push('/student/records')
}

onMounted(async () => {
  const res = await startExam(route.params.examId)
  paper.value = res.data
  current.value = paper.value.questions[0]
  remain.value = paper.value.remainingSeconds
  timer = setInterval(() => {
    remain.value -= 1
    if (remain.value <= 0) {
      clearInterval(timer)
      submitExam(paper.value.recordId, payload()).finally(() => {
        ElMessage.warning('时间到，已自动交卷')
        router.push('/student/records')
      })
    }
  }, 1000)
  saver = setInterval(persist, 20000)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(saver)
  persist().catch(() => {})
})
</script>

<style scoped>
.take { height: 100%; display: flex; flex-direction: column; background: #eef2f6; }
header {
  height: 56px; background: #0f172a; color: #fff;
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px;
}
.danger { color: #fca5a5; font-variant-numeric: tabular-nums; font-size: 20px; font-weight: 700; }
header span { font-variant-numeric: tabular-nums; font-size: 20px; font-weight: 700; }
.wrap { flex: 1; display: grid; grid-template-columns: 220px 1fr; min-height: 0; }
aside {
  background: #fff; border-right: 1px solid var(--line); padding: 16px;
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; align-content: start;
}
aside button {
  height: 36px; border: 1px solid var(--line); background: #fff; border-radius: 8px; cursor: pointer;
}
aside button.done { background: #dbeafe; }
aside button.flag { outline: 2px solid #f59e0b; }
aside button.on { background: #1d4ed8; color: #fff; border-color: #1d4ed8; }
section { margin: 16px; background: #fff; border-radius: 12px; padding: 24px 28px; overflow: auto; }
.stem { font-size: 18px; line-height: 1.8; margin: 12px 0 20px; }
.meta { color: var(--muted); }
.count { text-align: right; color: var(--muted); margin-top: 6px; }
</style>
