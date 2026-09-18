<template>
  <div>
    <div class="page-head">
      <div>
        <h2>题库</h2>
        <p>单选 / 多选 / 判断 / 填空 / 简答 / 关键词解释；分类来自知识点树的根节点</p>
      </div>
      <div class="actions">
        <el-button :loading="tplLoading" @click="downloadTemplate">下载模板</el-button>
        <el-button @click="openImport">Excel 导入</el-button>
        <el-button type="success" :disabled="!selected.length" @click="openExport">
          导出 PDF（已选 {{ selected.length }} 题）
        </el-button>
        <el-button type="primary" @click="$router.push('/admin/questions/edit')">新增题目</el-button>
      </div>
    </div>
    <div class="card">
      <el-form inline>
        <el-select v-model="query.type" clearable placeholder="题型" style="width:140px">
          <el-option v-for="t in QUESTION_TYPES" :key="t.v" :label="t.l" :value="t.v" />
        </el-select>
        <el-select v-model="query.categoryId" clearable placeholder="分类（知识点根）" style="width:180px">
          <el-option v-for="c in cats" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="题干关键词" clearable style="width:220px" />
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
      <div v-if="selected.length" class="sel-bar">
        已选 {{ selected.length }} 题 ·
        <el-button link type="primary" @click="clearSelection">清空选择</el-button>
      </div>
      <el-table ref="tableRef" :data="table.records" row-key="id" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" reserve-selection />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="100"><template #default="{ row }">{{ typeLabel(row.questionType) }}</template></el-table-column>
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

    <!-- Excel 导入 -->
    <el-dialog v-model="importVisible" title="Excel 导入题库" width="520px" :close-on-click-modal="false">
      <p class="hint">请先「下载模板」，按模板格式填写后上传 .xlsx 文件；每行一题，题型支持单选 / 多选 / 判断 / 填空 / 简答 / 关键词解释。</p>
      <el-form label-position="top">
        <el-form-item label="默认知识点（Excel 未填知识点的行使用）">
          <el-tree-select
            v-model="importForm.defaultKnowledgePointId"
            :data="tree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            filterable
            placeholder="可不选"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item>
          <el-upload :show-file-list="false" accept=".xlsx,.xls" :http-request="onImport">
            <el-button type="primary" :loading="importing">{{ importing ? '导入中…' : '选择文件并导入' }}</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 导入结果 -->
    <el-dialog v-model="resultVisible" title="导入结果" width="620px" @closed="load">
      <el-result
        v-if="importResult"
        :icon="importResult.failed > 0 ? 'warning' : 'success'"
        :title="`共 ${importResult.total} 行，成功 ${importResult.success}，失败 ${importResult.failed}`"
      />
      <el-table v-if="importResult && importResult.failed > 0" :data="importResult.errors" max-height="320" size="small">
        <el-table-column prop="rowNo" label="Excel 行号" width="110" />
        <el-table-column prop="message" label="失败原因" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 导出 PDF -->
    <el-dialog v-model="exportVisible" title="导出 PDF" width="460px">
      <p class="hint">将导出已选的 {{ selected.length }} 道题目。</p>
      <el-form label-width="120px">
        <el-form-item label="标题"><el-input v-model="exportForm.title" /></el-form-item>
        <el-form-item label="附带答案与解析"><el-switch v-model="exportForm.withAnswer" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="doExport">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageQuestions, listCategories, removeQuestion, knowledgeTree,
  downloadQuestionTemplate, importQuestions, exportQuestionsPdf
} from '../../api'
import { QUESTION_TYPES, typeLabel } from '../../utils/questionTypes'
import { downloadBlob, blobErrorMessage, safeFileName } from '../../utils/download'

const query = reactive({ page: 1, size: 10, type: '', categoryId: null, keyword: '' })
const table = reactive({ records: [], total: 0 })
const cats = ref([])
const tree = ref([])
const tableRef = ref()
const selected = ref([])

const tplLoading = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const importForm = reactive({ defaultKnowledgePointId: null })
const resultVisible = ref(false)
const importResult = ref(null)
const exportVisible = ref(false)
const exporting = ref(false)
const exportForm = reactive({ title: '云计算题库练习', withAnswer: false })

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
function onSelectionChange(rows) {
  selected.value = rows
}
function clearSelection() {
  tableRef.value?.clearSelection()
}

async function downloadTemplate() {
  tplLoading.value = true
  try {
    const res = await downloadQuestionTemplate()
    const err = await blobErrorMessage(res)
    if (err) return ElMessage.error(err)
    downloadBlob(res, '题库导入模板.xlsx')
  } finally {
    tplLoading.value = false
  }
}

async function openImport() {
  importForm.defaultKnowledgePointId = null
  importVisible.value = true
  if (!tree.value.length) tree.value = (await knowledgeTree()).data
}
// el-upload 的 http-request：拿到原始 File 后交给后端解析
async function onImport({ file }) {
  importing.value = true
  try {
    const res = await importQuestions(file, importForm.defaultKnowledgePointId)
    importResult.value = { total: 0, success: 0, failed: 0, errors: [], ...(res.data || {}) }
    importVisible.value = false
    resultVisible.value = true
  } finally {
    importing.value = false
  }
}

function openExport() {
  if (!selected.value.length) return
  exportVisible.value = true
}
async function doExport() {
  const title = (exportForm.title || '').trim() || '云计算题库练习'
  exporting.value = true
  try {
    const res = await exportQuestionsPdf({
      ids: selected.value.map(s => s.id),
      title,
      withAnswer: exportForm.withAnswer
    })
    const err = await blobErrorMessage(res)
    if (err) return ElMessage.error(err)
    downloadBlob(res, `${safeFileName(title, '云计算题库练习')}.pdf`)
    ElMessage.success('已导出')
    exportVisible.value = false
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  cats.value = (await listCategories()).data
  load()
})
</script>

<style scoped>
.actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.sel-bar { margin: 4px 0 8px; color: var(--muted); font-size: 13px; display: flex; align-items: center; gap: 4px; }
.hint { margin: 0 0 12px; color: var(--muted); font-size: 13px; line-height: 1.6; }
</style>
