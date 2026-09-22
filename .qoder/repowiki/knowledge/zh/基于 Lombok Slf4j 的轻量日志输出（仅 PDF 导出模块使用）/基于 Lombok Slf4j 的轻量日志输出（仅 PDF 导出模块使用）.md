---
kind: logging_system
name: 基于 Lombok Slf4j 的轻量日志输出（仅 PDF 导出模块使用）
category: logging_system
scope:
    - '**'
source_files:
    - exam-server/src/main/java/com/exam/service/QuestionPdfService.java
    - exam-server/src/main/java/com/exam/common/GlobalExceptionHandler.java
    - exam-server/src/main/resources/application.yml
    - exam-server/pom.xml
---

## 1. 使用的系统/方案

后端采用 Spring Boot 2.7.18 默认集成的 SLF4J + Logback 作为日志门面与实现，通过 Lombok 的 `@Slf4j` 注解在类中注入 `log` 字段。仓库未引入独立的 log4j2、logback 自定义配置或第三方 APM 组件，属于最轻量的内置日志方案。

前端（exam-web）为 Vue3 + Vite 项目，未发现任何浏览器端日志框架或统一日志收集逻辑，代码中也没有 `console.log` 等调试输出的集中管理。

## 2. 关键文件

- `exam-server/src/main/java/com/exam/service/QuestionPdfService.java`：唯一显式使用 `@Slf4j` 的业务类，通过 `log.warn(...)` 记录字体查找失败和 classpath 扫描异常。
- `exam-server/src/main/java/com/exam/common/GlobalExceptionHandler.java`：全局异常处理器，但使用的是 `e.printStackTrace()` 而非日志框架，将堆栈直接打印到标准错误流。
- `exam-server/pom.xml`：依赖 `spring-boot-starter-web`（间接引入 SLF4J/Logback）、`lombok`；未声明 `logback-spring.xml` 或 `log4j2.xml`。
- `exam-server/src/main/resources/application.yml`：未包含任何 `logging.level`、`logging.file`、`logging.pattern` 等日志相关配置项，全部使用 Spring Boot 默认值。

## 3. 架构与约定

- **日志级别**：仅在资源定位失败的兜底路径中使用 `warn` 级别（如配置的 PDF 字体不存在、扫描 classpath 字体目录失败），业务正常流程不输出日志。
- **结构化字段**：未定义统一的日志结构（无 MDC、无 JSON 格式、无固定字段如 traceId、userId、requestId 等），日志消息为纯文本模板字符串。
- **输出目标**：依赖 Spring Boot 默认行为，即输出到控制台；没有文件滚动、没有外部 sink（如 ELK、Kafka）接入。
- **异常处理中的日志**：`GlobalExceptionHandler` 捕获异常后调用 `e.printStackTrace()` 输出堆栈，而不是通过 `log.error` 输出，这意味着异常信息走的是 JVM stderr 而非应用日志流。
- **前端侧**：未发现统一的请求/响应拦截器日志、错误上报或前端日志聚合逻辑。

## 4. 约定与约束

- **实际约束**：当前代码库并未建立强制性的日志规范；仅有 `QuestionPdfService` 一处示范性地使用 `log.warn` 记录可恢复的降级场景。
- **缺失的机制**：
  - 没有统一的日志门面抽象（除 SLF4J 外），也没有禁止直接使用 `System.out` / `System.err` 的 lint 规则。
  - 没有按模块/包设置不同日志级别的配置文件。
  - 没有操作审计日志（尽管存在 `SysOperLog` 实体和 `OperLogController`/`OperLogService`，但那是数据库持久化的操作记录，不是运行期日志输出）。
- **可推断的实践**：由于所有配置均走 Spring Boot 默认值，生产部署时日志行为完全取决于运行环境（容器 stdout/stderr 或宿主机的 logback 默认输出），因此日志采集应交由容器编排层（如 Docker/K8s 日志驱动）完成。

总体而言，该仓库的“日志系统”处于极轻量状态：仅依赖 Spring Boot 内置 SLF4J/Logback，且只在 PDF 导出模块中零星使用 `log.warn`，其余业务代码未主动输出日志，异常则通过 `printStackTrace` 输出到 stderr。