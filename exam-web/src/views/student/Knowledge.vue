<template>
  <div>
    <div class="page-head"><div><h2>我的知识点掌握</h2><p>来自已完成阅卷的考试，简答题按实际得分计入</p></div></div>
    <div class="card">
      <div ref="chartEl" style="height:360px"></div>
      <el-table :data="rows" style="margin-top:12px">
        <el-table-column prop="name" label="知识点" />
        <el-table-column prop="questionCount" label="题量" width="90" />
        <el-table-column prop="masteryRate" label="掌握度 %" width="120" />
        <el-table-column prop="level" label="分档" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level==='薄弱' ? 'danger' : row.level==='一般' ? 'warning' : 'success'" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { myKnowledge } from '../../api'

const rows = ref([])
const chartEl = ref()
onMounted(async () => {
  rows.value = (await myKnowledge()).data
  await nextTick()
  if (!chartEl.value) return
  const chart = echarts.init(chartEl.value)
  chart.setOption({
    tooltip: {},
    radar: { indicator: rows.value.map(r => ({ name: r.name, max: 100 })) },
    series: [{ type: 'radar', data: [{ value: rows.value.map(r => Number(r.masteryRate)), name: '掌握度' }] }]
  })
})
</script>
