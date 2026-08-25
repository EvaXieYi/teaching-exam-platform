<template>
  <div>
    <div class="page-head">
      <div><h2>{{ form.id ? '编辑题目' : '新增题目' }}</h2><p>简答题请填写参考答案，仅阅卷可见</p></div>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <div class="card">
      <el-form label-width="100px">
        <el-form-item label="题型">
          <el-radio-group v-model="form.questionType" @change="onType">
            <el-radio-button v-for="t in types" :key="t.v" :label="t.v">{{ t.l }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类（来自知识点根节点）" style="width:240px">
            <el-option v-for="c in cats" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
          <div class="hint">分类即知识点树的根节点。在「知识点」里新增「Linux相关」后，这里会出现该项</div>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item v-if="isChoice" label="选项">
          <div v-for="(o, i) in form.options" :key="i" class="opt">
            <el-input v-model="o.optionKey" style="width:70px" />
            <el-input v-model="o.optionContent" />
            <el-checkbox :model-value="o.isCorrect === 1" @change="(v) => toggleCorrect(i, v)">正确答案</el-checkbox>
            <el-button link @click="form.options.splice(i,1)">删</el-button>
          </div>
          <el-button @click="addOpt">加选项</el-button>
        </el-form-item>
        <el-form-item v-if="form.questionType === 'FILL' || form.questionType === 'ESSAY'" :label="form.questionType === 'ESSAY' ? '参考答案' : '标准答案'">
          <el-input v-model="form.correctAnswer" type="textarea" :rows="3" :placeholder="form.questionType === 'FILL' ? '多个空用 | 分隔' : '阅卷参考，不会发给学生'" />
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="默认分"><el-input-number v-model="form.defaultScore" :min="0" :step="1" /></el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty" :max="3" />
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="form.visibility">
            <el-radio label="PUBLIC">公共题库</el-radio>
            <el-radio label="PRIVATE">仅自己</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="知识点">
          <el-tree
            ref="treeRef"
            :data="tree"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ label: 'name' }"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getQuestion, saveQuestion, listCategories, knowledgeTree } from '../../api'

const types = [
  { v: 'SINGLE', l: '单选' }, { v: 'MULTIPLE', l: '多选' }, { v: 'JUDGE', l: '判断' },
  { v: 'FILL', l: '填空' }, { v: 'ESSAY', l: '简答' }
]
const route = useRoute()
const router = useRouter()
const cats = ref([])
const tree = ref([])
const treeRef = ref()
const form = reactive({
  id: null, categoryId: null, questionType: 'SINGLE', content: '', correctAnswer: '', analysis: '',
  difficulty: 1, defaultScore: 5, visibility: 'PUBLIC', options: [], knowledgePointIds: []
})
const isChoice = computed(() => ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(form.questionType))

function defaultOptions() {
  if (form.questionType === 'JUDGE') return [{ optionKey: '对', optionContent: '对', isCorrect: 1 }, { optionKey: '错', optionContent: '错', isCorrect: 0 }]
  return [
    { optionKey: 'A', optionContent: '', isCorrect: 0 },
    { optionKey: 'B', optionContent: '', isCorrect: 0 },
    { optionKey: 'C', optionContent: '', isCorrect: 0 },
    { optionKey: 'D', optionContent: '', isCorrect: 0 }
  ]
}
function onType() {
  if (isChoice.value && (!form.options || !form.options.length)) form.options = defaultOptions()
}
function addOpt() {
  const keys = 'ABCDEFGH'
  form.options.push({ optionKey: keys[form.options.length] || 'X', optionContent: '', isCorrect: 0 })
}
function toggleCorrect(i, v) {
  if (form.questionType === 'SINGLE' || form.questionType === 'JUDGE') {
    form.options.forEach((o, idx) => { o.isCorrect = idx === i && v ? 1 : 0 })
  } else {
    form.options[i].isCorrect = v ? 1 : 0
  }
}
async function save() {
  form.knowledgePointIds = treeRef.value.getCheckedKeys(false)
  await saveQuestion({ ...form })
  ElMessage.success('已保存')
  router.push('/admin/questions')
}
onMounted(async () => {
  cats.value = (await listCategories()).data
  tree.value = (await knowledgeTree()).data
  if (!form.categoryId && cats.value[0]) form.categoryId = cats.value[0].id
  if (route.params.id) {
    const res = await getQuestion(route.params.id)
    Object.assign(form, res.data)
    await nextTick()
    treeRef.value.setCheckedKeys(form.knowledgePointIds || [])
  } else {
    form.options = defaultOptions()
  }
})
</script>

<style scoped>
.opt { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.hint { margin-top: 6px; color: var(--muted); font-size: 12px; line-height: 1.5; }
</style>
