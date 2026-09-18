<template>
  <div>
    <div class="page-head"><div><h2>阅卷中心</h2><p>只处理主观题（简答 / 关键词解释），全部批完后才出总分</p></div></div>
    <div class="card">
      <el-radio-group v-model="status" @change="load" style="margin-bottom:12px">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="MARKING">待阅卷</el-radio-button>
        <el-radio-button label="FINISHED">已完成</el-radio-button>
      </el-radio-group>
      <el-table :data="list">
        <el-table-column prop="examName" label="考试" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ row.record.recordStatus === 'MARKING' ? '待阅卷' : '已完成' }}</template>
        </el-table-column>
        <el-table-column prop="pendingEssay" label="未批主观题" width="120" />
        <el-table-column label="客观题" width="90"><template #default="{ row }">{{ row.record.objectiveScore }}</template></el-table-column>
        <el-table-column width="100">
          <template #default="{ row }"><el-button link type="primary" @click="$router.push(`/admin/marking/${row.record.id}`)">阅卷</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { markingList } from '../../api'
const status = ref('MARKING')
const list = ref([])
async function load() {
  list.value = (await markingList({ status: status.value })).data
}
onMounted(load)
</script>
