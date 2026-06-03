# 数据库设计与 MySQL 切换说明

CareerPilot 当前支持两种数据库运行方式：

- **默认 H2 文件数据库**：适合本地快速演示，不需要额外安装数据库。
- **MySQL 正式配置**：适合写入简历、后续部署和企业项目展示。

## 1. 默认 H2 配置

默认配置位于：

```text
backend/src/main/resources/application.yml
```

默认连接：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/careerpilot;MODE=MySQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

说明：

- `MODE=MySQL`：让 H2 尽量兼容 MySQL 语法。
- `backend/data/`：保存 H2 文件数据库，已加入 `.gitignore`。
- 本地演示时不需要安装 MySQL，直接 `mvn spring-boot:run` 即可。

## 2. MySQL Profile 配置

MySQL 独立配置位于：

```text
backend/src/main/resources/application-mysql.yml
```

启用 MySQL 时设置：

```env
SPRING_PROFILES_ACTIVE=mysql
MYSQL_URL=jdbc:mysql://localhost:3306/careerpilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=123456
JPA_DDL_AUTO=update
```

启动命令：

```powershell
cd C:\Users\Administrator\CareerPilot\backend
mvn spring-boot:run
```

## 3. 创建 MySQL 数据库

先在 MySQL 中执行：

```sql
CREATE DATABASE careerpilot
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

如果使用 JPA 自动建表，创建数据库后直接启动后端即可。

如果希望手动建表，可以参考：

```text
docs/schema-mysql.sql
```

## 4. 核心表设计

### app_user

用户表，用于注册登录和历史记录隔离。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键，自增 |
| `username` | VARCHAR(40) | 用户名，唯一 |
| `phone` | VARCHAR(20) | 手机号，唯一 |
| `password_hash` | VARCHAR(255) | 加盐哈希后的密码 |
| `salt` | VARCHAR(255) | 密码盐值 |
| `created_at` | DATETIME | 创建时间 |

### analysis_record

分析记录表，用于保存 AI 简历分析结果。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键，自增 |
| `resume_name` | VARCHAR(255) | 简历名称 |
| `user_id` | BIGINT | 用户 ID，未登录时可为空 |
| `match_score` | INT | 岗位匹配分数 |
| `summary` | VARCHAR(1000) | 分析摘要 |
| `resume_text` | TEXT | 简历正文 |
| `job_description` | TEXT | 岗位 JD |
| `result_json` | TEXT | 完整分析结果 JSON |
| `created_at` | DATETIME | 创建时间 |

## 5. 推荐索引

```sql
CREATE INDEX idx_analysis_user_created
  ON analysis_record(user_id, created_at DESC);

CREATE INDEX idx_analysis_created
  ON analysis_record(created_at DESC);
```

说明：

- `idx_analysis_user_created`：用于登录用户查看自己的最近分析记录。
- `idx_analysis_created`：用于匿名记录或全局最近记录倒序查询。

## 6. 面试讲解说法

可以这样讲：

> 项目默认使用 H2 文件数据库，方便本地演示；同时我已经补充了 MySQL 驱动和 `mysql` profile，正式环境只需要设置 `SPRING_PROFILES_ACTIVE=mysql` 和 MySQL 连接信息即可切换。历史记录表按 `user_id + created_at` 建索引，支持按用户查询最近分析记录。

