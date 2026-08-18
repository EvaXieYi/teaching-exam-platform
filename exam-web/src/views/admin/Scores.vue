<template>
  <div>
    <div class="page-head">
      <div><h2>成绩</h2><p>仅展示已完成阅卷的答卷</p></div>
      <el-button @click="exp">导出 Excel</el-button>
    </div>
    <div class="card">
      <el-form inline>
        <el-select v-model="examId" clearable placeholder="选择考试" style="width:260px" @change="load">
          <el-option v-for="e in exams" :key="e.exam.id" :label="e.exam.examName" :value="e.exam.id" />
        </el-select>
      </el-form>
      <el-table :data="list">
        <el-table-column prop="examName" label="考试" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="studentName" label="姓名" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="objectiveScore" label="客观题" />
        <el-table-column prop="subjectiveScore" label="主观题" />
        <el-table-column prop="totalScore" label="总分" />
        <el-table-column prop="passed" label="是否及格" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { pageExams, scores } from '../../api'
import http from '../../api/http'

const examId = ref()
const exams = ref([])
const list = ref([])
async function load() {
  list.value = (await scores({ examId: examId.value })).data
}
async function exp() {
  const res = await http.get('/api/scores/export', { params: { examId: examId.value }, responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = '成绩导出.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}
onMounted(async () => {
  exams.value = (await pageExams({ page: 1, size: 100 })).data.records
  load()
})
</script>
