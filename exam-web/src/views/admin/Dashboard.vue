<template>
  <div>
    <div class="welcome">
      <div class="welcome-copy">
        <h2>{{ hello }}，{{ teacherName }} 👋</h2>
        <p><span class="role-tag">{{ roleLabel }}</span> 欢迎回到工作台，可查看班级学员的考试进度、待阅卷和学情情况</p>
      </div>
      <img class="welcome-hero" src="../../assets/welcome-hero.png" alt="" />
      <el-button round class="welcome-btn" @click="$router.push('/admin/analysis')">查看学情分析</el-button>
    </div>

    <div class="stat-grid five">
      <div class="stat-card">
        <div class="icon blue"><el-icon><UserFilled /></el-icon></div>
        <div>
          <div class="value">{{ data.studentCount || 0 }}</div>
          <div class="label">学员总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon orange"><el-icon><OfficeBuilding /></el-icon></div>
        <div>
          <div class="value">{{ data.classCount || 0 }}</div>
          <div class="label">班级数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon green"><el-icon><CircleCheck /></el-icon></div>
        <div>
          <div class="value">{{ data.submittedCount || 0 }}</div>
          <div class="label">已交卷</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon grey"><el-icon><EditPen /></el-icon></div>
        <div>
          <div class="value">{{ data.pendingMarking || 0 }}</div>
          <div class="label">待阅卷</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon dark"><el-icon><Trophy /></el-icon></div>
        <div>
          <div class="value">{{ data.avgScore || 0 }}</div>
          <div class="label">平均分 · 及格率 {{ data.passRate || 0 }}%</div>
        </div>
      </div>
    </div>

    <div class="dash-grid">
      <div class="card">
        <div class="card-head">
          <h3>最近考试</h3>
          <el-button link type="primary" @click="$router.push('/admin/exams')">全部考试</el-button>
        </div>
        <el-table :data="data.recentExams || []" empty-text="暂无考试">
          <el-table-column label="名称" min-width="160" :formatter="(_, __, row) => row.exam?.examName" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag size="small">{{ statusText(row.runtimeStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="考生" width="80" prop="studentCount" />
          <el-table-column label="已交卷" width="80" prop="submittedCount" />
          <el-table-column label="" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/admin/analysis?examId=${row.exam.id}`)">分析</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-head"><h3>班级学员</h3></div>
        <el-table :data="data.classStats || []" empty-text="暂无班级数据">
          <el-table-column prop="className" label="班级" />
          <el-table-column prop="studentCount" label="学员数" width="90" />
          <el-table-column label="" width="90">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/admin/students?className=${encodeURIComponent(row.className)}`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p class="hint">进行中考试 {{ data.ongoingExamCount || 0 }} 场 · 答题中 {{ data.answeringCount || 0 }} 人</p>
      </div>
    </div>

    <div class="card notice-card" style="margin-top:16px">
      <div class="card-head"><h3>最近动态</h3></div>
      <el-empty v-if="!(data.recentLogs || []).length" description="暂无操作记录" :image-size="64" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in data.recentLogs"
          :key="item.id"
          :timestamp="item.createdAt"
          placement="top"
        >
          {{ item.operation }}{{ item.detail ? ` · ${item.detail}` : '' }}
        </el-timeline-item>
      </el-timeline>
      <img class="notice-mascot" src="../../assets/notice-mascot.png" alt="" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { dashboard } from '../../api'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const data = reactive({})
const map = { DRAFT: '草稿', PUBLISHED: '未开始', ONGOING: '进行中', FINISHED: '已结束' }
const statusText = (s) => map[s] || s
const teacherName = computed(() => store.displayName)
const roleLabel = computed(() => store.isAdmin ? '管理员' : '教师')
const hello = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  // 进工作台时再拉一次当前用户，确保显示最新配置的姓名
  try { await store.loadMe() } catch { /* 忽略，用本地缓存 */ }
  const res = await dashboard()
  Object.assign(data, res.data)
})
</script>

<style scoped>
.welcome {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  margin-bottom: 16px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(120deg, #1d4ed8 0%, #2563eb 55%, #38bdf8 100%);
  overflow: hidden;
}
.welcome-copy { min-width: 0; flex: 1; }
.welcome h2 { margin: 0 0 6px; font-size: 22px; }
.welcome p { margin: 0; opacity: 0.92; font-size: 13px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.role-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  background: rgba(255, 255, 255, 0.12);
}
.welcome-hero {
  width: 220px; height: 88px; object-fit: cover; object-position: center;
  border-radius: 10px; flex-shrink: 0; opacity: 0.95;
}
.welcome-btn { background: #fff !important; color: #1d4ed8 !important; border: 0 !important; flex-shrink: 0; }
.notice-card { position: relative; padding-bottom: 28px; }
.notice-mascot {
  position: absolute; right: 8px; bottom: 4px;
  width: 88px; height: 88px; object-fit: contain; pointer-events: none;
}
.stat-grid.five { grid-template-columns: repeat(5, 1fr); }
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-card .value { font-size: 24px; margin-top: 0; }
.icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: grid; place-items: center; font-size: 18px; color: #fff; flex-shrink: 0;
}
.icon.blue { background: #3b82f6; }
.icon.orange { background: #f59e0b; }
.icon.green { background: #22c55e; }
.icon.grey { background: #94a3b8; }
.icon.dark { background: #334155; }
.dash-grid {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.card-head h3 { margin: 0; font-size: 16px; }
.hint { margin: 12px 0 0; color: var(--muted); font-size: 13px; }
@media (max-width: 1100px) {
  .stat-grid.five { grid-template-columns: 1fr 1fr; }
  .dash-grid { grid-template-columns: 1fr; }
  .welcome-hero { display: none; }
}
</style>
