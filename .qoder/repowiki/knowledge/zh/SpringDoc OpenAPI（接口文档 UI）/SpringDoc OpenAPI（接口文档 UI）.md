---
kind: external_dependency
name: SpringDoc OpenAPI（接口文档 UI）
slug: springdoc-openapi
category: external_dependency
category_hints:
    - vendor_identity
scope:
    - '**'
source_files:
    - exam-server/pom.xml
    - README.md
---

### SpringDoc OpenAPI
- 角色：提供后端接口的在线文档页面，便于前后端联调。
- 使用方式：无需额外配置，Spring Boot 自动扫描 Controller 生成文档；JWT 鉴权接口需在文档中手动传入 token 测试。
- 注意：本项目未对接口做注解级文档描述，文档内容以方法签名和参数为准。