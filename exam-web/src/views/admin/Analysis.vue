<template>
  <div>
    <div class="page-head"><div><h2>学情分析</h2><p>掌握度 = 实得分 / 满分。≥80 掌握，60–79 一般，&lt;60 薄弱</p></div></div>
    <div class="card">
      <el-form inline>
        <el-select v-model="examId" placeholder="选择考试" style="width:280px" @change="loadExam">
          <el-option v-for="e in exams" :key="e.exam.id" :label="e.exam.examName" :value="e.exam.id" />
        </el-select>
        <el-select v-model="className" clearable placeholder="班级薄弱点" style="width:200px" @change="loadClass">
          <el-option v-for="c in classes" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form>
      <div v-if="stat" class="stat-grid" style="margin-top:12px">
        <div class="stat-card"><div class="label">应考</div><div class="value">{{ stat.assigned }}</div></div>
        <div class="stat-card"><div class="label">已交卷</div><div class="value">{{ stat.submitted }}</div></div>
        <div class="stat-card"><div class="label">平均分</div><div class="value">{{ stat.avgScore }}</div></div>
        <div class="stat-card"><div class="label">及格率</div><div class="value">{{ stat.passRate }}%</div></div>
      </div>
      <h3>本场知识点掌握度（从低到高）</h3>
      <div ref="chartEl" style="height:320px"></div>
      <el-table :data="knowledge" style="margin-top:12px">
        <el-table-column prop="name" label="知识点" />
        <el-table-column prop="masteryRate" label="掌握度 %" />
        <el-table-column prop="level" label="分档">
          <template #default="{ row }">
            <el-tag :type="row.level==='薄弱' ? 'danger' : row.level==='一般' ? 'warning' : 'success'" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <h3 style="margin-top:24px">考生明细</h3>
      <el-table :data="stat?.students || []">
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="studentName" label="姓名" />
        <el-table-column prop="recordStatus" label="状态" />
        <el-table-column prop="totalScore" label="总分" />
        <el-table-column prop="passed" label="及格">
          <template #default="{ row }">{{ row.passed === 1 ? '是' : row.passed === 0 ? '否' : '' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { pageExams, examStatistics, classKnowledge, dashboard } from '../../api'

const route = useRoute()
const exams = ref([])
const classes = ref([])
const examId = ref()
const className = ref()
const stat = ref(null)
const knowledge = ref([])
const chartEl = ref()
let chart

function renderChart(rows) {
  nextTick(() => {
    if (!chartEl.value) return
    if (!chart) chart = echarts.init(chartEl.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 80, right: 24, top: 24, bottom: 40 },
      xAxis: { type: 'value', name: '掌握度 %', max: 100 },
      yAxis: { type: 'category', data: rows.map(r => r.name) },
      series: [{ type: 'bar', data: rows.map(r => Number(r.masteryRate)), itemStyle: { color: '#2563eb' } }]
    })
  })
}
async function loadExam() {
  if (!examId.value) return
  const res = await examStatistics(examId.value)
  stat.value = res.data
  knowledge.value = res.data.knowledge || []
  renderChart([...knowledge.value].reverse())
}
async function loadClass() {
  if (!className.value) return
  knowledge.value = (await classKnowledge(className.value)).data
  renderChart([...knowledge.value].reverse())
}
onMounted(async () => {
  exams.value = (await pageExams({ page: 1, size: 100 })).data.records
  classes.value = (await dashboard()).data.classNames || []
  examId.value = route.query.examId ? Number(route.query.examId) : exams.value[0]?.exam.id
  if (examId.value) loadExam()
})
</script>
