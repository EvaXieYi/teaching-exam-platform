<template>
  <div>
    <div class="page-head">
      <div><h2>试卷</h2><p>手动组卷，未绑定知识点的题目不能加入</p></div>
      <el-button type="primary" @click="$router.push('/admin/papers/edit')">组卷</el-button>
    </div>
    <div class="card">
      <el-input v-model="query.keyword" placeholder="试卷名" clearable style="width:220px;margin-bottom:12px" @change="load" />
      <el-table :data="table.records">
        <el-table-column prop="paperName" label="名称" />
        <el-table-column prop="questionCount" label="题量" width="90" />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column prop="passScore" label="及格分" width="90" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/admin/papers/edit/${row.id}`)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="table.total" v-model:current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessageBox } from 'element-plus'
import { pagePapers, removePaper } from '../../api'
const query = reactive({ page: 1, size: 10, keyword: '' })
const table = reactive({ records: [], total: 0 })
async function load() {
  const res = await pagePapers(query)
  table.records = res.data.records
  table.total = res.data.total
}
async function remove(row) {
  await ElMessageBox.confirm('删除该试卷？')
  await removePaper(row.id)
  load()
}
onMounted(load)
</script>
