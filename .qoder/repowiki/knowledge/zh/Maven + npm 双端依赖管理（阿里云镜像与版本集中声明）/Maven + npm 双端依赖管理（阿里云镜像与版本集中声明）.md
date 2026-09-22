---
kind: dependency_management
name: Maven + npm 双端依赖管理（阿里云镜像与版本集中声明）
category: dependency_management
scope:
    - '**'
source_files:
    - exam-server/pom.xml
    - exam-server/.mvn/wrapper/maven-wrapper.properties
    - .tools/apache-maven-3.9.9/
    - .tools/settings.xml
    - exam-web/package.json
    - exam-web/package-lock.json
    - docker-compose.yml
---

## 1. 使用的系统/工具

仓库采用前后端分离的双包管理器策略：
- **后端（exam-server）**：使用 **Maven**，基于 `spring-boot-starter-parent:2.7.18` 作为父 POM，通过 `<properties>` 集中声明第三方库版本号，并使用 Maven Wrapper（`.mvn/wrapper`）锁定 Maven 版本为 3.9.9。
- **前端（exam-web）**：使用 **npm**（由 `package-lock.json` 可知），基于 Vite 6，依赖通过 `package.json` 的 `dependencies` / `devDependencies` 声明，并生成 `package-lock.json` 锁定精确版本。

## 2. 关键文件

| 模块 | 关键文件 | 作用 |
|---|---|---|
| 后端 | `exam-server/pom.xml` | 依赖声明、版本集中管理、Maven 仓库配置、构建插件 |
| 后端 | `exam-server/.mvn/wrapper/maven-wrapper.properties` | 锁定 Maven 发行版与 wrapper jar 下载地址 |
| 后端 | `.tools/apache-maven-3.9.9/`、`.tools/settings.xml` | 本地预置 Maven 安装及可选 settings 配置 |
| 前端 | `exam-web/package.json` | 依赖与脚本声明 |
| 前端 | `exam-web/package-lock.json` | 精确依赖树锁定 |
| 根级 | `docker-compose.yml` | 运行时依赖（MySQL/Kafka）以容器形式声明 |

## 3. 架构与约定

### 后端（Maven）
- **版本集中化**：所有第三方库版本统一放在 `<properties>` 中（如 `mybatis-plus.version=3.5.5`、`jjwt.version=0.11.5`、`easyexcel.version=3.3.4`、`openpdf.version=1.3.43`、`springdoc.version=1.7.0`），依赖引用处通过 `${...}` 引用，避免散落的硬编码版本。
- **父 POM 继承**：通过 `spring-boot-starter-parent` 统一管理 Spring Boot 生态依赖版本，减少显式版本声明。
- **私有/加速镜像**：在 `<repositories>` 和 `<pluginRepositories>` 中仅配置阿里云公共镜像 `https://maven.aliyun.com/repository/public`，未配置私有仓库或 `~/.m2/settings.xml` 中的认证信息；Maven Wrapper 从官方 `repo.maven.apache.org` 下载 Maven 本身。
- **依赖范围控制**：数据库驱动（H2、mysql-connector-j）、JWT 实现（jjwt-impl/jjwt-jackson）、测试依赖均使用 `<scope>runtime</scope>` 或 `<scope>test</scope>`，Lombok 标记为 `<optional>true</optional>` 并在打包时排除。
- **构建产物**：通过 `spring-boot-maven-plugin` 打包可执行 JAR，并显式 exclude Lombok。

### 前端（npm）
- **依赖分类**：运行期依赖（vue、pinia、axios、element-plus、echarts 等）放入 `dependencies`，开发期依赖（vite、@vitejs/plugin-vue）放入 `devDependencies`。
- **版本策略**：全部使用 `^` 语义化版本前缀，允许小版本自动升级；精确锁定由 `package-lock.json` 保证。
- **无自定义 registry**：未配置 `.npmrc` 或私有源，默认使用 npm 官方源。

### 运行时依赖
- 通过根级 `docker-compose.yml` 声明 MySQL、Kafka 等外部服务，作为应用启动时的运行时依赖。

## 4. 约定与约束

- **禁止散乱版本**：后端新增依赖时应优先在 `<properties>` 中声明版本号，再在 `<dependencies>` 中以变量形式引用（现有 MyBatis-Plus、JJWT、EasyExcel、OpenPDF、SpringDoc 均遵循此模式）。
- **统一镜像源**：后端构建必须走阿里云 Maven 镜像，不直接访问中央仓库（pom.xml 中已覆盖默认仓库）。
- **Maven 版本固定**：团队通过 `.mvn/wrapper` 与 `.tools/apache-maven-3.9.9/` 双重保障，确保构建环境一致。
- **前端锁文件纳入版本控制**：`package-lock.json` 随仓库提交，保证不同机器安装结果一致。
- **运行时依赖容器化**：MySQL、Kafka 等通过 docker-compose 管理，不在本机安装。
- **无私有仓库/认证**：当前仓库未配置任何需要鉴权的私有 Maven/NPM 源，所有依赖均来自公开源。