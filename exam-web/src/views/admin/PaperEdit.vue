<template>
  <div>
    <div class="page-head">
      <div><h2>{{ form.id ? '编辑试卷' : '组卷' }}</h2></div>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <div class="card">
      <el-form inline>
        <el-form-item label="试卷名"><el-input v-model="form.paperName" style="width:280px" /></el-form-item>
        <el-form-item label="及格分"><el-input-number v-model="form.passScore" /></el-form-item>
      </el-form>
      <p>已选题 {{ form.questions.length }} 道，总分 {{ total }}</p>
      <el-table :data="form.questions">
        <el-table-column type="index" width="50" />
        <el-table-column prop="content" label="题干" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="90" />
        <el-table-column label="分值" width="140">
          <template #default="{ row }"><el-input-number v-model="row.questionScore" :min="0" size="small" /></template>
        </el-table-column>
        <el-table-column width="80"><template #default="{ $index }"><el-button link @click="form.questions.splice($index,1)">移除</el-button></template></el-table-column>
      </el-table>
      <el-button type="primary" style="margin:16px 0" @click="save">保存试卷</el-button>
      <el-divider />
      <h3>从题库添加</h3>
      <el-form inline>
        <el-select v-model="q.type" clearable placeholder="题型" style="width:140px">
          <el-option label="单选" value="SINGLE" /><el-option label="多选" value="MULTIPLE" />
          <el-option label="判断" value="JUDGE" /><el-option label="填空" value="FILL" /><el-option label="简答" value="ESSAY" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="关键词" clearable style="width:200px" />
        <el-button @click="search">查询</el-button>
      </el-form>
      <el-table :data="bank">
        <el-table-column prop="content" label="题干" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="90" />
        <el-table-column width="100">
          <template #default="{ row }"><el-button link type="primary" @click="add(row)">加入</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPaper, savePaper, pageQuestions } from '../../api'

const route = useRoute()
const router = useRouter()
const form = reactive({ id: null, paperName: '', passScore: 60, questions: [] })
const q = reactive({ type: '', keyword: '' })
const bank = ref([])
const total = computed(() => form.questions.reduce((s, i) => s + Number(i.questionScore || 0), 0))

function add(row) {
  if (form.questions.some(x => x.questionId === row.id)) return
  form.questions.push({
    questionId: row.id,
    content: row.content,
    questionType: row.questionType,
    questionScore: Number(row.defaultScore || 5),
    sortNo: form.questions.length + 1
  })
}
async function search() {
  const res = await pageQuestions({ page: 1, size: 50, ...q })
  bank.value = res.data.records
}
async function save() {
  await savePaper({
    id: form.id,
    paperName: form.paperName,
    passScore: form.passScore,
    questions: form.questions.map((x, i) => ({ questionId: x.questionId, questionScore: x.questionScore, sortNo: i + 1 }))
  })
  ElMessage.success('已保存')
  router.push('/admin/papers')
}
onMounted(async () => {
  await search()
  if (route.params.id) {
    const res = await getPaper(route.params.id)
    form.id = res.data.paper.id
    form.paperName = res.data.paper.paperName
    form.passScore = res.data.paper.passScore
    form.questions = res.data.questions.map(x => ({
      questionId: x.questionId,
      content: x.content,
      questionType: x.questionType,
      questionScore: x.questionScore,
      sortNo: x.sortNo
    }))
  }
})
</script>
