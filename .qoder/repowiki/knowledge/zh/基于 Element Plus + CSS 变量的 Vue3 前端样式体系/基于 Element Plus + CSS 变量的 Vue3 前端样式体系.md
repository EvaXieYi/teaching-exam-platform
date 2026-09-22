---
kind: frontend_style
name: 基于 Element Plus + CSS 变量的 Vue3 前端样式体系
category: frontend_style
scope:
    - '**'
source_files:
    - exam-web/package.json
    - exam-web/vite.config.js
    - exam-web/src/main.js
    - exam-web/src/styles.css
    - exam-web/src/App.vue
    - exam-web/src/layouts/AdminLayout.vue
    - exam-web/src/layouts/StudentLayout.vue
    - exam-web/src/views/admin/Dashboard.vue
---

## 1. 采用的样式方案

- **UI 组件库**：使用 Element Plus（`element-plus@2.9.1`）作为基础 UI 组件，并在 `main.js` 中全局注册并配置中文语言包 `zh-cn`，同时批量注册 `@element-plus/icons-vue` 全部图标。
- **构建工具**：Vite 6 + `@vitejs/plugin-vue`，无 Tailwind/SCSS/Less 等预处理或原子化框架，纯 CSS。
- **主题系统**：通过全局 `:root` CSS 自定义属性集中定义设计令牌（design tokens），包括侧边栏色 `--sidebar`、强调色 `--accent`、页面背景 `--page`、分割线 `--line`、主文本 `--text`、弱化文本 `--muted` 等，所有视图与布局均引用这些变量而非硬编码颜色。
- **响应式策略**：采用 CSS `@media (max-width: ...)` 媒体查询进行断点适配（如 `900px`、`1100px`），配合 CSS Grid 的 `repeat(4, 1fr)` / `repeat(5, 1fr)` 自动换行实现统计卡片网格的自适应。

## 2. 关键文件

- `exam-web/src/styles.css`：全局设计令牌（CSS 变量）、全局重置、通用 `.page-head`、`.card`、`.stat-grid`、`.stat-card` 等共享样式。
- `exam-web/src/main.js`：应用入口，挂载 Element Plus（含中文 locale）、Pinia、Vue Router，并引入全局样式。
- `exam-web/vite.config.js`：开发服务器端口 `5173`，`/api` 代理至后端 `http://127.0.0.1:8080`。
- `exam-web/src/layouts/AdminLayout.vue`、`StudentLayout.vue`：角色化布局，侧边栏深色主题（`background: var(--sidebar)`），顶部用户信息条。
- `exam-web/src/views/admin/Dashboard.vue`：工作台页面，展示统一的欢迎横幅渐变、统计卡片网格、ECharts 图表容器等。
- 各 `views/**/*.vue`：业务页面，统一使用 `<style scoped>` 编写局部样式。

## 3. 架构与约定

- **全局样式层**：`styles.css` 提供设计令牌与通用 UI 原语（页面头部、卡片、统计网格），被所有页面复用。
- **布局层**：通过 `AdminLayout` / `StudentLayout` 两个布局组件封装侧边导航、顶栏、内容区，子路由在 `<router-view />` 中渲染。
- **组件层**：Element Plus 组件（`el-menu`、`el-button`、`el-upload`、`el-message`、`el-message-box` 等）直接用于交互；自定义组件 `UserAvatar` 仅包含头像逻辑与自身 `scoped` 样式。
- **样式隔离**：每个 `.vue` 文件的 `<style>` 块均使用 `scoped`，避免样式泄漏；对第三方组件（如 `el-menu`）使用 `:deep()` 穿透修改激活态背景等。
- **主题定制**：通过覆盖 Element Plus 的 CSS 变量或直接设置 `background-color`、`text-color`、`active-text-color` 等 props 来匹配深色侧边栏风格；例如 `el-menu` 传入 `background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff"`，并通过 `:deep(.el-menu-item.is-active)` 将激活项高亮为 `#1e3a8a`。
- **图标体系**：统一使用 `@element-plus/icons-vue` 提供的 SVG 图标，通过 `<el-icon><Odometer /></el-icon>` 形式嵌入菜单和按钮。
- **图表**：使用 ECharts 5（`echarts@5.5.1`）在 Dashboard 等页面渲染学情分析图，未引入额外可视化库。

## 4. 约定与约束

- **颜色必须来自 CSS 变量**：全局色值集中在 `styles.css` 的 `:root`，组件内应引用 `var(--sidebar)`、`var(--page)`、`var(--line)`、`var(--muted)` 等，避免散落硬编码色值。
- **样式作用域**：所有组件样式写在 `<style scoped>` 中；需要修改 Element Plus 内部样式时统一使用 `:deep()` 选择器。
- **响应式断点**：以 `900px`、`1100px` 等作为主要断点，通过 Grid 列数变化实现卡片网格的自适应布局。
- **Element Plus 全局配置**：仅在 `main.js` 中一次性配置中文语言包，业务组件不重复导入 locale。
- **图标注册**：所有 `@element-plus/icons-vue` 图标在 `main.js` 中通过 `Object.entries(Icons)` 循环注册，组件内直接使用标签名即可。
- **布局一致性**：教师端与管理端共用同一套侧边栏结构（`AdminLayout`），学生端使用独立 `StudentLayout`，保证不同角色的视觉一致性与导航规范。