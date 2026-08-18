<template>
  <div>
    <div class="page-head"><div><h2>{{ form.id ? '编辑考试' : '新建考试' }}</h2></div><el-button @click="$router.back()">返回</el-button></div>
    <div class="card">
      <el-form label-width="110px" style="max-width:720px">
        <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="试卷">
          <el-select v-model="form.paperId" style="width:100%">
            <el-option v-for="p in papers" :key="p.id" :label="`${p.paperName}（${p.totalScore}分）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="答题时长"><el-input-number v-model="form.durationMinutes" :min="1" /> 分钟</el-form-item>
        <el-form-item label="最早交卷"><el-input-number v-model="form.allowSubmitMinutes" :min="0" /> 分钟后</el-form-item>
        <el-form-item label="公布成绩"><el-switch v-model="form.resultVisible" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="公布答案"><el-switch v-model="form.answerVisible" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="指定考生">
          <el-select v-model="form.studentIds" multiple filterable style="width:100%">
            <el-option v-for="s in students" :key="s.id" :label="`${s.studentNo} ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="save">保存草稿</el-button></el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { paperOptions, studentOptions, getExam, saveExam } from '../../api'

const route = useRoute()
const router = useRouter()
const papers = ref([])
const students = ref([])
const form = reactive({
  id: null, examName: '', paperId: null, startTime: '', endTime: '',
  durationMinutes: 60, allowSubmitMinutes: 0, resultVisible: 1, answerVisible: 0, studentIds: []
})
async function save() {
  const id = await saveExam({ ...form })
  ElMessage.success('已保存')
  router.push('/admin/exams')
  return id
}
onMounted(async () => {
  papers.value = (await paperOptions()).data
  students.value = (await studentOptions()).data
  if (route.params.id) {
    const res = await getExam(route.params.id)
    const e = res.data.exam
    Object.assign(form, {
      id: e.id, examName: e.examName, paperId: e.paperId, startTime: e.startTime, endTime: e.endTime,
      durationMinutes: e.durationMinutes, allowSubmitMinutes: e.allowSubmitMinutes,
      resultVisible: e.resultVisible, answerVisible: e.answerVisible, studentIds: res.data.studentIds
    })
  }
})
</script>
