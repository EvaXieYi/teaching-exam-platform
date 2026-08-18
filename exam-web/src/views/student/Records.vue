<template>
  <div>
    <div class="page-head"><div><h2>我的成绩</h2><p>简答题未批完时不显示总分</p></div></div>
    <el-table :data="list" class="card">
      <el-table-column prop="examName" label="考试" />
      <el-table-column prop="recordStatus" label="状态">
        <template #default="{ row }">
          {{ { ANSWERING: '答题中', MARKING: '阅卷中', FINISHED: '已出分', SUBMITTED: '已交卷' }[row.recordStatus] || row.examStatus }}
        </template>
      </el-table-column>
      <el-table-column label="成绩">
        <template #default="{ row }">{{ row.recordStatus === 'FINISHED' ? row.totalScore : '—' }}</template>
      </el-table-column>
      <el-table-column width="120">
        <template #default="{ row }">
          <el-button v-if="row.recordId && row.recordStatus !== 'ANSWERING'" link type="primary" @click="$router.push(`/student/review/${row.recordId}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myExams } from '../../api'
const list = ref([])
onMounted(async () => { list.value = (await myExams()).data.filter(i => i.recordId) })
</script>
