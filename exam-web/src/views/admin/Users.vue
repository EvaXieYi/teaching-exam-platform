<template>
  <div>
    <div class="page-head">
      <div><h2>账号管理</h2><p>仅管理员可创建教师和管理员</p></div>
      <el-button type="primary" @click="open()">新建账号</el-button>
    </div>
    <div class="card">
      <el-form inline>
        <el-input v-model="query.keyword" placeholder="用户名 / 姓名" clearable style="width:200px" />
        <el-select v-model="query.role" clearable placeholder="角色" style="width:140px">
          <el-option label="管理员" value="ADMIN" /><el-option label="教师" value="TEACHER" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
      <el-table :data="table.records">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="role" label="角色" />
        <el-table-column label="状态"><template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template></el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="table.total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />
    </div>
    <el-dialog v-model="visible" :title="form.id ? '编辑账号' : '新建账号'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" /><el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" placeholder="留空则不修改 / 新建默认 123456" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageUsers, saveUser } from '../../api'

const query = reactive({ page: 1, size: 10, keyword: '', role: '' })
const table = reactive({ records: [], total: 0 })
const visible = ref(false)
const form = reactive({ id: null, username: '', realName: '', role: 'TEACHER', password: '', status: 1 })

async function load() {
  const res = await pageUsers(query)
  table.records = res.data.records
  table.total = res.data.total
}
function open(row) {
  Object.assign(form, { id: null, username: '', realName: '', role: 'TEACHER', password: '', status: 1, ...row, password: '' })
  visible.value = true
}
async function save() {
  await saveUser({ ...form })
  ElMessage.success('已保存')
  visible.value = false
  load()
}
onMounted(load)
</script>
