<template>
  <div>
    <div class="page-head">
      <div><h2>学生管理</h2><p>学号即登录名，默认密码 student123</p></div>
      <div class="page-actions">
        <el-upload class="import-btn" :show-file-list="false" accept=".xlsx,.xls" :http-request="onImport">
          <el-button>Excel 导入</el-button>
        </el-upload>
        <el-button type="primary" @click="open()">新增学生</el-button>
      </div>
    </div>
    <div class="card">
      <el-form inline>
        <el-input v-model="query.keyword" placeholder="姓名 / 学号" clearable style="width:200px" />
        <el-input v-model="query.className" placeholder="班级" clearable style="width:160px" />
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
      <el-table :data="table.records">
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="department" label="部门" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="phone" label="手机" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="table.total" v-model:current-page="query.page" @current-change="load" />
    </div>
    <el-dialog v-model="visible" :title="form.id ? '编辑学生' : '新增学生'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="班级"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" placeholder="留空使用默认 / 不修改" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageStudents, saveStudent, removeStudent, importStudents } from '../../api'

const route = useRoute()
const query = reactive({ page: 1, size: 10, keyword: '', className: route.query.className || '' })
const table = reactive({ records: [], total: 0 })
const visible = ref(false)
const empty = () => ({ id: null, studentNo: '', name: '', department: '', className: '', phone: '', email: '', password: '' })
const form = reactive(empty())

async function load() {
  const res = await pageStudents(query)
  table.records = res.data.records
  table.total = res.data.total
}
function open(row) {
  Object.assign(form, empty(), row || {}, { password: '' })
  visible.value = true
}
async function save() {
  await saveStudent({ ...form })
  ElMessage.success('已保存')
  visible.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`删除学生 ${row.name}？`)
  await removeStudent(row.id)
  load()
}
async function onImport({ file }) {
  const res = await importStudents(file)
  ElMessage.success(`导入 ${res.data} 人`)
  load()
}
onMounted(load)
</script>

<style scoped>
.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.import-btn :deep(.el-upload) {
  display: inline-flex;
}
</style>
