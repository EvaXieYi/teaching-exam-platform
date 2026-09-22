---
kind: build_system
name: 构建与部署体系（Maven + Vite + Nginx/systemd）
category: build_system
scope:
    - '**'
source_files:
    - exam-server/pom.xml
    - exam-web/package.json
    - exam-web/vite.config.js
    - docker-compose.yml
    - deploy/remote-setup.sh
    - start-server.bat
    - start-web.bat
    - docs/部署文档.md
---

## 1. 使用的构建系统

仓库采用**前后端分离、各自独立构建**的单体仓库模式：
- 后端 `exam-server/`：基于 Spring Boot，使用 **Maven**（`spring-boot-starter-parent` 2.7.18，Java 11）管理依赖与打包，通过 `spring-boot-maven-plugin` 生成可执行 fat JAR。
- 前端 `exam-web/`：基于 Vue3 + Vite，使用 **npm** 管理依赖，通过 `vite build` 输出静态产物到 `dist/`。
- 本地开发：根目录提供 `start-server.bat`（调用内置 `.tools/apache-maven-3.9.9`）和 `start-web.bat` 一键启动；Windows 下 Maven 通过 `.tools/settings.xml` 指向阿里云镜像。
- 服务器部署：在 Rocky Linux 上源码编译后以 systemd 托管 JAR，Nginx 对外暴露 80 端口并反向代理 `/api` 到 127.0.0.1:8080。

## 2. 关键文件

| 文件 | 作用 |
|---|---|
| `exam-server/pom.xml` | 后端依赖、版本属性、Maven 仓库（aliyun）、`spring-boot-maven-plugin` 配置 |
| `exam-web/package.json` | 前端依赖、`dev/build/preview` 脚本 |
| `exam-web/vite.config.js` | 开发服务器端口 5173，`/api` 代理到 `127.0.0.1:8080` |
| `docker-compose.yml` | 仅编排 MySQL 8.0（含 schema 初始化卷挂载），不包含应用服务 |
| `deploy/remote-setup.sh` | 远程主机一次性初始化脚本：写 Maven settings、npm registry、Nginx 站点、systemd 单元 |
| `start-server.bat` / `start-web.bat` | Windows 开发快捷启动 |
| `docs/部署文档.md` | 完整的源码部署流程、profile 切换、防火墙与备份说明 |
| `sql/schema.sql` | MySQL/H2 共享的建表 DDL，被 `application-mysql.yml` 的 `spring.sql.init` 自动加载 |

## 3. 架构与约定

- **构建产物固定路径**：后端 JAR 固定为 `target/exam-server-1.0.0.jar`（由 `pom.xml` 中 `artifactId=exam-server`、`version=1.0.0` 决定），systemd 与部署文档均硬编码该路径。
- **Profile 驱动环境切换**：`application.yml` 默认激活 `h2` profile；切换 MySQL 时设置环境变量 `SPRING_PROFILES_ACTIVE=mysql`，MySQL profile 通过 `application-mysql.yml` 注入 JDBC URL、用户名密码（均以 `${MYSQL_*}` 环境变量覆盖）。
- **数据库初始化策略**：MySQL profile 启用 `spring.sql.init.mode=always` 且 `continue-on-error=true`，配合 `DataInitializer` 在空库时写入演示账号；H2 profile 使用内嵌文件库 `./data/exam.mv.db`。
- **Nginx 反代约定**：所有 `/api/*` 请求转发到后端 8080；Swagger/H2 Console 等调试路径也透传；前端 SPA 使用 `try_files $uri $uri/ /index.html` 支持 History 路由。
- **字体与 PDF**：后端 OpenPDF 导出题库 PDF 依赖系统中文字体，生产需安装 `google-noto-sans-cjk-fonts`，或通过 `EXAM_PDF_FONT_PATH` 环境变量指定字体路径。
- **Kafka 预留**：当前 JAR 未引入 `spring-kafka`，`application-kafka.yml` 作为二期 profile 预置，文档明确“不要在没有依赖的情况下把 kafka 写进 `SPRING_PROFILES_ACTIVE`”。

## 4. 约束与规则

- **JDK 版本锁定**：`pom.xml` 声明 `java.version=11`，部署文档强制要求运行与编译统一使用 JDK 11（`JAVA_HOME=/usr/lib/jvm/java-11-openjdk`），禁止用系统自带的 JDK 17 运行进程。
- **网络端口约束**：对外仅开放 80（HTTP）与 SSH；3306（MySQL）、9092/9093（Kafka）、8080（后端直连）**不得对公网暴露**，MySQL 必须 `bind-address=127.0.0.1`。
- **构建产物不入库**：`.gitignore` 排除 `node_modules/`、`exam-server/target/`、`exam-server/data/`，部署文档要求上传源码包时同样排除这些目录。
- **配置文件不可提交敏感信息**：`MYSQL_PASSWORD` 等凭据通过环境变量注入，文档明确“不要提交 Git”。
- **Vite 构建限制**：`<script setup>` 中不能使用 `export const`，否则 `npm run build` 失败（已在文档故障排查中记录）。
- **systemd 工作目录约束**：`WorkingDirectory` 必须为 `exam-server`，否则 H2 无法写入 `./data/exam` 下的数据文件。
- **Maven/NPM 镜像**：服务端通过 `/root/.m2/settings.xml` 全局镜像到阿里云，npm registry 设置为 `https://registry.npmmirror.com`，确保国内构建速度。

## 5. CI/流水线

仓库中未发现 GitHub Actions、GitLab CI 等 CI 配置文件。发布流程完全依赖人工在 Rocky Linux 服务器上执行 `mvn -B -DskipTests package` 与 `npm run build`，再通过 `systemctl restart exam-server` 完成更新。