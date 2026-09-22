---
kind: external_dependency
name: OpenPDF（题库勾选题目导出 PDF）
slug: openpdf
category: external_dependency
category_hints:
    - vendor_identity
    - framework_behavior
scope:
    - '**'
source_files:
    - exam-server/pom.xml
    - exam-server/src/main/java/com/exam/service/QuestionPdfService.java
    - README.md
---

### OpenPDF
- 角色：将勾选的题目导出为 A4 格式 PDF 试卷，由 `QuestionPdfService` 实现。
- 集成点：`POST /api/questions/export-pdf` 接收题目 ID 列表、标题、是否附带答案与解析参数，生成并返回 PDF 流。
- 字体策略：优先读取配置 `exam.pdf-font-path`（环境变量 `EXAM_PDF_FONT_PATH`），其次 classpath `fonts/` 目录，再回退 Windows `simhei.ttf`/`msyh.ttc` 或 Linux Noto CJK/文泉驿；Linux 部署必须安装中文字体，否则导出会提示缺少字体。
- 输出行为：按题型分组编号，选择题列出选项，主观题留作答空白，可附加每题【答案】【解析】段落。
- 注意：PDF 标题文件名需过滤非法字符，避免跨平台命名问题。