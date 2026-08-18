<template>
  <div>
    <div class="page-head">
      <div><h2>考试</h2><p>试卷是内容，考试是一次真实发布</p></div>
      <el-button type="primary" @click="$router.push('/admin/exams/edit')">新建考试</el-button>
    </div>
    <div class="card">
      <el-table :data="table.records">
        <el-table-column label="名称" :formatter="(_,__,row) => row.exam.examName" />
        <el-table-column prop="paperName" label="试卷" />
        <el-table-column label="时间" width="220">
          <template #default="{ row }">{{ row.exam.startTime }} ~ {{ row.exam.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag size="small">{{ map[row.runtimeStatus] || row.runtimeStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="studentCount" label="考生" width="80" />
        <el-table-column prop="submittedCount" label="已交" width="80" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-if="row.exam.status==='DRAFT'" link type="primary" @click="$router.push(`/admin/exams/edit/${row.exam.id}`)">编辑</el-button>
            <el-button v-if="row.exam.status==='DRAFT'" link type="success" @click="pub(row)">发布</el-button>
            <el-button v-if="row.exam.status==='PUBLISHED'" link type="warning" @click="stop(row)">停止</el-button>
            <el-button link @click="$router.push(`/admin/analysis?examId=${row.exam.id}`)">分析</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="table.total" v-model:current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageExams, publishExam, stopExam } from '../../api'
const map = { DRAFT: '草稿', PUBLISHED: '未开始', ONGOING: '进行中', FINISHED: '已结束' }
const query = reactive({ page: 1, size: 10 })
const table = reactive({ records: [], total: 0 })
async function load() {
  const res = await pageExams(query)
  table.records = res.data.records
  table.total = res.data.total
}
async function pub(row) {
  await ElMessageBox.confirm('发布后学生可见该考试')
  await publishExam(row.exam.id)
  ElMessage.success('已发布')
  load()
}
async function stop(row) {
  await ElMessageBox.confirm('提前结束本场考试？')
  await stopExam(row.exam.id)
  load()
}
onMounted(load)
</script>
