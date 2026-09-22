---
kind: external_dependency
name: MySQL 8（生产/可选持久化存储）
slug: mysql
category: external_dependency
category_hints:
    - vendor_identity
scope:
    - '**'
source_files:
    - docker-compose.yml
    - exam-server/src/main/resources/application-mysql.yml
    - README.md
---

### MySQL 8
- 角色：生产环境推荐的关系型数据库，替代默认 H2 文件库。
- 集成点：`docker-compose.yml` 提供 MySQL 8 容器；通过 `SPRING_PROFILES_ACTIVE=mysql` 激活 `application-mysql.yml`，默认连接 `localhost:3306`、库名 `exam`、账号 `root/root`；可通过 `MYSQL_HOST/MYSQL_PORT/MYSQL_USER/MYSQL_PASSWORD` 环境变量覆盖。
- 迁移方式：执行 `sql/schema.sql` 初始化结构后启动后端即可。
- 注意：docker-compose 中 root/root 密码暴露到所有网卡，生产应修改密码并限制端口绑定。