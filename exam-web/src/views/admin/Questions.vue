<template>
  <div>
    <div class="page-head">
      <div><h2>题库</h2><p>单选 / 多选 / 判断 / 填空 / 简答，必须绑定知识点</p></div>
      <el-button type="primary" @click="$router.push('/admin/questions/edit')">新增题目</el-button>
    </div>
    <div class="card">
      <el-form inline>
        <el-select v-model="query.type" clearable placeholder="题型" style="width:140px">
          <el-option v-for="t in types" :key="t.v" :label="t.l" :value="t.v" />
        </el-select>
        <el-select v-model="query.categoryId" clearable placeholder="分类" style="width:160px">
          <el-option v-for="c in cats" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="题干关键词" clearable style="width:220px" />
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
      <el-table :data="table.records">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="90"><template #default="{ row }">{{ typeName(row.questionType) }}</template></el-table-column>
        <el-table-column prop="content" label="题干" show-overflow-tooltip />
        <el-table-column label="知识点"><template #default="{ row }">{{ (row.knowledgePointNames || []).join('、') }}</template></el-table-column>
        <el-table-column prop="defaultScore" label="默认分" width="90" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/admin/questions/edit/${row.id}`)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="table.total" v-model:current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { pageQuestions, listCategories, removeQuestion } from '../../api'

export const types = [
  { v: 'SINGLE', l: '单选' }, { v: 'MULTIPLE', l: '多选' }, { v: 'JUDGE', l: '判断' },
  { v: 'FILL', l: '填空' }, { v: 'ESSAY', l: '简答' }
]
const typeName = (v) => types.find(t => t.v === v)?.l || v
const query = reactive({ page: 1, size: 10, type: '', categoryId: null, keyword: '' })
const table = reactive({ records: [], total: 0 })
const cats = ref([])

async function load() {
  const res = await pageQuestions(query)
  table.records = res.data.records
  table.total = res.data.total
}
async function remove(row) {
  await ElMessageBox.confirm('逻辑删除该题？')
  await removeQuestion(row.id)
  load()
}
onMounted(async () => {
  cats.value = (await listCategories()).data
  load()
})
</script>
