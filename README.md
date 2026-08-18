# 教学考试平台

教师出题组卷、学生在线考试、简答题阅卷、按知识点分析掌握情况。

## 环境

- JDK 11（本仓库后端使用 Spring Boot 2.7，兼容当前 Java 11）
- Node.js 18+
- 可选：MySQL 8、Docker

默认使用 **H2 文件库**，无需安装 MySQL，首次启动会自动建表并写入演示数据。

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | admin123 |
| 教师 | teacher | teacher123 |
| 学生 | s2024001 | student123 |
| 学生 | s2024002 | student123 |

已预置一场进行中的「Java 基础入学测验」，学生登录后可直接作答。简答题需教师在阅卷中心给分后才会出总分并写入学情。

## 启动

首次会自动写入演示数据，无需装 MySQL。

后端（已编译后）：

```bat
cd exam-server
java -jar target\exam-server-1.0.0.jar
```

或双击根目录 `start-server.bat`。接口：http://127.0.0.1:8080 ，Swagger：http://127.0.0.1:8080/swagger-ui.html

前端：

```bat
cd exam-web
npm install
npm run dev
```

或双击 `start-web.bat`。浏览器打开 http://127.0.0.1:5173

## 使用 MySQL（可选）

1. 执行 `sql/schema.sql` 或 `docker compose up -d mysql`
2. 启动后端时加上：

```bat
set SPRING_PROFILES_ACTIVE=mysql
mvnw.cmd spring-boot:run
```

默认连接 `localhost:3306`，库名 `exam`，账号 `root/root`，可用环境变量 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD` 覆盖。

## 目录

```
exam-server/   Spring Boot 后端
exam-web/      Vue 3 教师端 + 学生端
sql/           建表脚本
考试系统-方案.md
```

## 一期能力

- 三角色登录分流
- 知识点树、题库（含简答）、手动组卷
- 发布考试、指定考生
- 学生答题、自动保存、到时交卷、客观题自动评分
- 简答题人工阅卷
- 成绩查询 / Excel 导出
- 单场知识点掌握度、学生个人雷达图、班级薄弱点
