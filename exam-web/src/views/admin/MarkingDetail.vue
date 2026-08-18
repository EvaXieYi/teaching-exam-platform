<template>
  <div v-if="detail">
    <div class="page-head">
      <div>
        <h2>阅卷 · {{ detail.studentName }}（{{ detail.studentNo }}）</h2>
        <p>{{ detail.examName }} · 客观题 {{ detail.record.objectiveScore }} 分</p>
      </div>
      <el-button @click="$router.push('/admin/marking')">返回列表</el-button>
    </div>
    <div class="mark">
      <aside>
        <div v-for="(a, i) in detail.answers" :key="a.id" class="qno" :class="{ on: i===idx, essay: a.questionTypeSnapshot==='ESSAY', done: a.markedAt }" @click="idx=i">
          {{ i + 1 }}
        </div>
      </aside>
      <section class="card" v-if="cur">
        <div class="meta">{{ typeName(cur.questionTypeSnapshot) }} · {{ cur.questionScore }} 分</div>
        <div class="stem">{{ cur.questionContentSnapshot }}</div>
        <h4>学生作答</h4>
        <pre>{{ cur.studentAnswer || '（未作答）' }}</pre>
        <h4>参考答案</h4>
        <pre>{{ cur.correctAnswerSnapshot }}</pre>
        <template v-if="cur.questionTypeSnapshot === 'ESSAY'">
          <el-form-item label="给分">
            <el-input-number v-model="score" :min="0" :max="Number(cur.questionScore)" />
          </el-form-item>
          <el-input v-model="comment" type="textarea" :rows="3" placeholder="评语（可选）" />
          <el-button type="primary" style="margin-top:12px" @click="save">保存本题分数</el-button>
        </template>
        <template v-else>
          <p>客观题已自动评分：{{ cur.score }} 分</p>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { markingDetail, markAnswer } from '../../api'

const map = { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }
const typeName = (v) => map[v] || v
const route = useRoute()
const detail = ref(null)
const idx = ref(0)
const score = ref(0)
const comment = ref('')
const cur = computed(() => detail.value?.answers?.[idx.value])

watch(cur, (a) => {
  if (!a) return
  score.value = Number(a.score || 0)
  comment.value = a.comment || ''
})

async function load() {
  detail.value = (await markingDetail(route.params.recordId)).data
  const first = detail.value.answers.findIndex(a => a.questionTypeSnapshot === 'ESSAY' && !a.markedAt)
  idx.value = first >= 0 ? first : 0
}
async function save() {
  await markAnswer(cur.value.id, { score: score.value, comment: comment.value })
  ElMessage.success('已保存')
  await load()
}
onMounted(load)
</script>

<style scoped>
.mark { display: grid; grid-template-columns: 88px 1fr; gap: 16px; }
aside { display: flex; flex-wrap: wrap; align-content: flex-start; gap: 8px; }
.qno {
  width: 36px; height: 36px; border: 1px solid var(--line); border-radius: 8px;
  display: grid; place-items: center; cursor: pointer; background: #fff;
}
.qno.essay { border-color: #f59e0b; }
.qno.done { background: #dcfce7; }
.qno.on { background: #1d4ed8; color: #fff; border-color: #1d4ed8; }
.stem { font-size: 16px; line-height: 1.7; margin: 8px 0 16px; }
pre { white-space: pre-wrap; background: #f8fafc; padding: 12px; border-radius: 8px; }
.meta { color: var(--muted); }
</style>
