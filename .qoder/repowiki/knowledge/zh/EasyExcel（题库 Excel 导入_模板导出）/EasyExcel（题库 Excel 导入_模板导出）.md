---
kind: external_dependency
name: EasyExcel（题库 Excel 导入/模板导出）
slug: easyexcel
category: external_dependency
category_hints:
    - vendor_identity
    - sdk_real_api
scope:
    - '**'
source_files:
    - exam-server/pom.xml
    - exam-server/src/main/java/com/exam/service/QuestionImportService.java
---

### EasyExcel
- 角色：题库批量导入与模板下载的核心依赖，由 `QuestionImportService` 调用。
- 集成点：`GET /api/questions/import-template` 返回包含表头与示例题的 Excel 模板（Sheet1 为题目数据、Sheet2 为填写说明）；`POST /api/questions/import` 接收上传的 Excel 文件，逐行校验并支持部分成功。
- 稳定用法：导入时知识点按名称或「父/子」路径匹配，未填时使用前端弹窗选中的默认知识点；错误行返回行号+原因，不中断整批导入。
- 注意：当前实现不做重复题干去重，重复导入会产生重复题目；建议后续增加文件大小与行数上限校验。