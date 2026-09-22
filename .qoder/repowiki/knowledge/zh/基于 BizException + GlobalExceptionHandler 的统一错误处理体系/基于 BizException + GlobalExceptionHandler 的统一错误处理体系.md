---
kind: error_handling
name: 基于 BizException + GlobalExceptionHandler 的统一错误处理体系
category: error_handling
scope:
    - '**'
source_files:
    - exam-server/src/main/java/com/exam/common/BizException.java
    - exam-server/src/main/java/com/exam/common/GlobalExceptionHandler.java
    - exam-server/src/main/java/com/exam/common/Result.java
    - exam-web/src/api/http.js
---

## 1. 采用的系统/方案

后端采用 Spring Boot 的 `@RestControllerAdvice` 全局异常处理器，配合自定义业务异常 `BizException` 与统一响应体 `Result<T>`，形成「业务层抛异常 → 全局处理器捕获 → 统一 JSON 返回」的错误链路；前端使用 axios 拦截器对后端 `Result.code !== 0` 或 HTTP 401 进行集中处理（提示 + 跳转登录）。

## 2. 关键文件与位置

- `exam-server/src/main/java/com/exam/common/BizException.java`：业务异常基类，携带 `code`（默认 400）和 `message`，用于表达「考试已结束」「请勿重复交卷」等可预期业务失败。
- `exam-server/src/main/java/com/exam/common/GlobalExceptionHandler.java`：全局异常处理器，按异常类型分别映射到 HTTP 状态码与 `Result.fail(...)`。
- `exam-server/src/main/java/com/exam/common/Result.java`：统一响应结构 `{ code, message, data }`，约定 `code=0` 表示成功，非 0 表示失败。
- `exam-web/src/api/http.js`：axios 实例，请求拦截自动附加 JWT，响应拦截根据 `body.code !== 0` 弹出 `ElMessage.error` 并 reject Promise；HTTP 401 时清除本地 token/user 并重定向到 `/login`。

## 3. 架构与约定

### 后端异常分类与映射
| 异常类型 | 触发场景 | HTTP 状态 | 行为 |
|---|---|---|---|
| `BizException` | 业务校验失败、权限不足等 | 由 `code` 决定（400/401/403），其余走 500 | `handleBiz` 中根据 `e.getCode()` 设置 401/403，否则返回 `Result.fail(code, message)` |
| `MethodArgumentNotValidException` / `BindException` | JSR-303 / `@Valid` 参数校验失败 | 200（通过 Result 承载） | 取第一个字段错误消息，返回 `Result.fail("参数错误" | 字段信息)` |
| `BadCredentialsException` | Spring Security 用户名密码错误 | 401 | 固定返回 `Result.fail(401, "用户名或密码错误")` |
| `AccessDeniedException` | 鉴权通过但无资源权限 | 403 | 固定返回 `Result.fail(403, "没有权限")` |
| `Exception`（兜底） | 未捕获的其他异常 | 500 | 打印堆栈，返回 `Result.fail(500, ...)` |

### 前后端协作约定
- 后端所有接口统一返回 `Result<T>`，`code=0` 为成功，非 0 为失败。
- 前端 axios 响应拦截器在 `res.data.code !== 0` 时调用 `ElMessage.error(body.message || '请求失败')` 并 `Promise.reject(new Error(body.message))`，使上层组件无需重复判断错误。
- 当 HTTP 状态为 401（token 过期/缺失）时，前端清除 `localStorage` 中的 `token` 与 `user`，并跳转到 `/login`。

### 设计决策
- 业务异常不向上抛出原始堆栈，而是通过 `BizException` 将业务语义编码进 `code`+`message`，由 `GlobalExceptionHandler` 统一收敛，避免 Controller 中散落 try-catch。
- 参数校验错误不走 HTTP 4xx，而是以 `Result.fail` 形式返回，保持 REST 响应体一致。
- 安全相关异常（认证失败、权限拒绝）单独处理并映射到标准 HTTP 状态码，便于前端区分网络错误与鉴权错误。

## 4. 约定与约束

- **统一响应体**：所有接口必须返回 `Result<T>`，`code=0` 表示成功（见 `Result.ok` 实现及注释）。前端据此判断是否展示错误。
- **业务失败走异常**：业务层应通过 `throw new BizException(code, message)` 表达失败，而不是在 Controller 中构造 `Result.fail`（`BizException` 注释明确说明“不用在每个接口里 try-catch”）。
- **401/403 的特殊处理**：`GlobalExceptionHandler.handleBiz` 中对 `BizException` 的 401/403 会显式设置 `HttpServletResponse` 状态码，前端 axios 拦截器据此执行登出重定向。
- **参数校验错误**：使用 JSR-303 / MyBatis-Plus 校验注解，由 Spring 的 `MethodArgumentNotValidException`/`BindException` 捕获，返回第一条字段错误消息。
- **未知异常兜底**：`Exception` 处理器作为最后防线，记录堆栈并返回 500，确保任何未捕获异常都不会泄露内部细节给前端。
- **前端统一错误提示**：所有通过 `http.js` 发起的请求共享同一错误提示逻辑，组件层不应自行 `alert` 或 `console.error` 业务错误。