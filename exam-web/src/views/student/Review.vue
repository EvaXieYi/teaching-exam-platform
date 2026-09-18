<template>
  <div v-if="data">
    <div class="page-head">
      <div>
        <h2>{{ data.examName }}</h2>
        <p v-if="data.marking">教师阅卷中，暂不公布总分</p>
        <p v-else>总分 {{ data.record.totalScore }} · {{ data.record.passed === 1 ? '及格' : '不及格' }}</p>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <div v-for="(q, i) in data.questions" :key="q.questionId" class="card q">
      <div class="meta">第 {{ i + 1 }} 题 · {{ typeLabel(q.questionType) }} · {{ q.questionScore }} 分
        <span v-if="q.score != null"> · 得分 {{ q.score }}</span>
      </div>
      <div class="stem">{{ q.content }}</div>
      <div v-if="q.options?.length" class="opts">
        <div v-for="o in q.options" :key="o.optionKey">{{ o.optionKey }}. {{ o.optionContent }}</div>
      </div>
      <p><b>我的答案：</b>{{ q.studentAnswer || '未作答' }}</p>
      <p v-if="data.answerVisible"><b>正确答案：</b>{{ q.correctAnswer }}</p>
      <p v-if="data.answerVisible && q.analysis"><b>解析：</b>{{ q.analysis }}</p>
      <p v-if="q.comment"><b>评语：</b>{{ q.comment }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { reviewExam } from '../../api'
import { typeLabel } from '../../utils/questionTypes'
const route = useRoute()
const data = ref(null)
onMounted(async () => {
  data.value = (await reviewExam(route.params.recordId)).data
})
</script>

<style scoped>
.q { margin-bottom: 12px; }
.meta { color: var(--muted); margin-bottom: 8px; }
.stem { font-size: 16px; line-height: 1.7; }
.opts { margin: 8px 0; color: #334155; }
</style>
