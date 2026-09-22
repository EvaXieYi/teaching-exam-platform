---
kind: configuration_system
name: Spring Boot 多环境配置与运行时参数管理
category: configuration_system
scope:
    - '**'
source_files:
    - exam-server/src/main/resources/application.yml
    - exam-server/src/main/resources/application-h2.yml
    - exam-server/src/main/resources/application-mysql.yml
    - exam-server/src/main/resources/application-kafka.yml
    - exam-server/src/main/java/com/exam/config/SecurityConfig.java
    - exam-server/src/main/java/com/exam/config/DataInitializer.java
    - docker-compose.yml
    - exam-web/vite.config.js
---

## 1. 使用的系统与框架
本仓库采用 Spring Boot 原生的 `application.yml` + `application-{profile}.yml` 多环境配置机制，配合环境变量注入（`${ENV_VAR:default}`）完成运行期参数管理。前端基于 Vite 开发服务器代理到后端，生产部署通过 docker-compose 编排 MySQL。

## 2. 关键配置文件
- `exam-server/src/main/resources/application.yml`：全局默认配置，包含 server、spring、mybatis-plus、自定义 `exam.*` 命名空间、springdoc 等。
- `application-h2.yml`：H2 内存/文件数据库 profile，启用 H2 Console。
- `application-mysql.yml`：MySQL profile，连接串、用户名密码均通过环境变量占位符注入。
- `application-kafka.yml`：可选的 Kafka profile，仅在 `SPRING_PROFILES_ACTIVE` 包含 `kafka` 且引入 spring-kafka 依赖时生效。
- `docker-compose.yml`：定义 MySQL 服务及初始化 SQL 挂载。
- `exam-web/vite.config.js`：开发时 `/api` 请求代理至 `http://127.0.0.1:8080`。

## 3. 架构与约定
### 3.1 Profile 切换
- 通过 `spring.profiles.active=${SPRING_PROFILES_ACTIVE:h2}` 指定激活的 profile，默认使用 H2 以便本地快速启动；生产或集成测试切换为 `mysql`。
- 每个 profile 仅覆盖自身差异部分（如 datasource），其余配置从 `application.yml` 继承。

### 3.2 环境变量注入
- 所有外部化参数统一通过 `${VAR:default}` 语法注入，包括 MySQL 连接信息（`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`）、Kafka 地址（`KAFKA_BOOTSTRAP_SERVERS`）、PDF 字体路径（`EXAM_PDF_FONT_PATH`）以及 Spring profile 本身（`SPRING_PROFILES_ACTIVE`）。
- 应用自定义配置集中在 `exam.*` 命名空间下：`exam.jwt-secret`、`exam.jwt-expire-hours`、`exam.pdf-font-path`、`exam.kafka.topics.*`，由业务代码通过 Spring 配置绑定读取。

### 3.3 数据初始化策略
- `spring.sql.init.mode=always` 配合 `classpath:db/schema.sql` 保证每次启动都执行建表脚本，`continue-on-error=true` 避免重复执行失败。
- `DataInitializer` 实现 `ApplicationRunner`，在库为空时写入演示用户（admin/teacher/student）、知识点树、样题与一场进行中的考试；一旦存在用户即跳过，防止覆盖线上数据。

### 3.4 安全与跨域配置
- `SecurityConfig` 以 Java Config 方式声明 SecurityFilterChain：关闭 CSRF、无状态 Session、按 URL 前缀划分角色权限（`/api/auth/login` 匿名、`/api/student/**` 学生、`/api/users/**` 管理员、其余教师/管理员）。
- CORS 允许所有来源、所有方法并携带凭证，适配前后端分离场景。
- 鉴权异常统一返回 JSON `{code,message,data}` 格式。

### 3.5 前端 API 访问
- 开发阶段通过 Vite `server.proxy` 将 `/api` 转发到 `http://127.0.0.1:8080`，前端无需关心后端端口。
- 构建产物静态部署时，前端通过相对路径调用后端 REST API，不嵌入后端地址。

## 4. 约定与约束
- **Profile 命名**：数据库 profile 以 `h2`、`mysql` 区分；可选功能（如 Kafka）通过追加 profile 名（如 `kafka`）组合激活。
- **敏感信息外置**：JWT 密钥、数据库密码、Kafka 地址等不得硬编码，必须通过环境变量注入；`application.yml` 中仅保留示例值。
- **默认值兜底**：所有 `${VAR}` 均提供冒号后的默认值，确保未设置环境变量时仍可启动（如 `MYSQL_HOST:localhost`、`KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092`）。
- **演示数据保护**：`DataInitializer` 仅在 `selectCount(null) == 0` 时写入演示数据，禁止在生产环境误删已有用户。
- **Kafka 可选性**：`application-kafka.yml` 顶部注释明确“现网不要把 kafka 加进 systemd，否则缺少依赖会启动失败”，因此该 profile 需显式加入 `SPRING_PROFILES_ACTIVE` 才加载。
- **时区与字符集**：全局统一 `Asia/Shanghai` 时区与 UTF-8 编码，Jackson 日期格式固定为 `yyyy-MM-dd HH:mm:ss`。
- **Swagger 开放**：`/swagger-ui/**`、`/v3/api-docs/**`、`/h2-console/**` 在 Security 中放行，便于调试但应限制生产访问。