<template>
  <div>
    <div class="page-head">
      <div><h2>知识点</h2><p>课程 → 章节 → 知识点，题目必须绑定叶子或任意节点</p></div>
      <el-button type="primary" @click="open()">新增根节点</el-button>
    </div>
    <div class="card">
      <el-tree :data="tree" node-key="id" default-expand-all :props="{ label: 'name' }">
        <template #default="{ data }">
          <span class="node">
            <span>{{ data.name }}</span>
            <span>
              <el-button link type="primary" @click.stop="open({ parentId: data.id })">加子级</el-button>
              <el-button link type="primary" @click.stop="open(data)">编辑</el-button>
              <el-button link type="danger" @click.stop="remove(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </div>
    <el-dialog v-model="visible" :title="form.id ? '编辑知识点' : '新增知识点'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeTree, saveKnowledge, removeKnowledge } from '../../api'

const tree = ref([])
const visible = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', code: '', sortNo: 0 })

async function load() {
  const res = await knowledgeTree()
  tree.value = res.data
}
function open(row = {}) {
  Object.assign(form, { id: null, parentId: 0, name: '', code: '', sortNo: 0 }, row)
  visible.value = true
}
async function save() {
  await saveKnowledge({ ...form })
  ElMessage.success('已保存')
  visible.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`删除「${row.name}」？`)
  await removeKnowledge(row.id)
  load()
}
onMounted(load)
</script>

<style scoped>
.node { flex: 1; display: flex; justify-content: space-between; padding-right: 8px; }
</style>
