<template>
  <div>
    <div class="page-head">
      <div>
        <h2>知识点</h2>
        <p>先新增分类（树的根），再在分类下加章节 / 知识点。分类会同步出现在题库的分类下拉中</p>
      </div>
      <el-button type="primary" @click="open()">新增分类</el-button>
    </div>
    <div class="card">
      <el-tree :data="tree" node-key="id" default-expand-all :props="{ label: 'name' }">
        <template #default="{ data }">
          <span class="node">
            <span class="name">
              <el-tag v-if="isRoot(data)" size="small" type="primary" effect="plain">分类</el-tag>
              {{ data.name }}
            </span>
            <span>
              <el-button link type="primary" @click.stop="open({ parentId: data.id })">
                {{ isRoot(data) ? '加知识点' : '加子级' }}
              </el-button>
              <el-button link type="primary" @click.stop="open(data)">编辑</el-button>
              <el-button link type="danger" @click.stop="remove(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </div>
    <el-dialog v-model="visible" :title="dialogTitle" width="420px">
      <el-form label-width="80px">
        <el-form-item :label="isRootForm ? '分类名称' : '名称'">
          <el-input v-model="form.name" :placeholder="isRootForm ? '例如：Linux相关' : '例如：基础命令'" />
        </el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" placeholder="可选，如 linux" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeTree, saveKnowledge, removeKnowledge } from '../../api'

const tree = ref([])
const visible = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', code: '', sortNo: 0 })

const isRoot = (data) => !data.parentId
const isRootForm = computed(() => !form.parentId)
const dialogTitle = computed(() => {
  if (isRootForm.value) return form.id ? '编辑分类' : '新增分类'
  return form.id ? '编辑知识点' : '新增知识点'
})

async function load() {
  const res = await knowledgeTree()
  tree.value = res.data
}
function open(row = {}) {
  Object.assign(form, { id: null, parentId: 0, name: '', code: '', sortNo: 0 }, row)
  visible.value = true
}
async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning(isRootForm.value ? '请填写分类名称' : '请填写知识点名称')
    return
  }
  await saveKnowledge({
    id: form.id,
    parentId: form.parentId || 0,
    name: form.name.trim(),
    code: form.code,
    sortNo: form.sortNo
  })
  ElMessage.success('已保存')
  visible.value = false
  load()
}
async function remove(row) {
  const kind = isRoot(row) ? '分类' : '知识点'
  await ElMessageBox.confirm(`删除${kind}「${row.name}」？${isRoot(row) ? '题库分类下拉中也会不再显示该项。' : ''}`)
  await removeKnowledge(row.id)
  load()
}
onMounted(load)
</script>

<style scoped>
.node { flex: 1; display: flex; justify-content: space-between; padding-right: 8px; }
.name { display: inline-flex; align-items: center; gap: 8px; }
</style>
