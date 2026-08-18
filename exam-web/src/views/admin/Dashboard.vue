<template>
  <div>
    <div class="page-head">
      <div>
        <h2>工作台</h2>
        <p>待阅卷、考试和学生规模一览</p>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><div class="label">学生人数</div><div class="value">{{ data.studentCount || 0 }}</div></div>
      <div class="stat-card"><div class="label">考试场次</div><div class="value">{{ data.examCount || 0 }}</div></div>
      <div class="stat-card"><div class="label">待阅卷</div><div class="value">{{ data.pendingMarking || 0 }}</div></div>
      <div class="stat-card"><div class="label">近期考试</div><div class="value">{{ (data.recentExams || []).length }}</div></div>
    </div>
    <div class="card">
      <h3 style="margin:0 0 12px">最近考试</h3>
      <el-table :data="data.recentExams || []">
        <el-table-column label="名称" :formatter="(_, __, row) => row.exam?.examName" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><el-tag size="small">{{ statusText(row.runtimeStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="考生" width="100" prop="studentCount" />
        <el-table-column label="已交卷" width="100" prop="submittedCount" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { dashboard } from '../../api'

const data = reactive({})
const map = { DRAFT: '草稿', PUBLISHED: '未开始', ONGOING: '进行中', FINISHED: '已结束' }
const statusText = (s) => map[s] || s

onMounted(async () => {
  const res = await dashboard()
  Object.assign(data, res.data)
})
</script>
