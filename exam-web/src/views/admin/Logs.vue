<template>
  <div>
    <div class="page-head"><div><h2>操作日志</h2></div></div>
    <div class="card">
      <el-table :data="table.records">
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="operation" label="操作" width="160" />
        <el-table-column prop="detail" label="详情" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="table.total" v-model:current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { logs } from '../../api'
const query = reactive({ page: 1, size: 10 })
const table = reactive({ records: [], total: 0 })
async function load() {
  const res = await logs(query)
  table.records = res.data.records
  table.total = res.data.total
}
onMounted(load)
</script>
