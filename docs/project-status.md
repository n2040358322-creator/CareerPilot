# CareerPilot 项目状态

更新时间：2026-06-03

## 当前定位

CareerPilot 是一个面向大学生求职场景的 AI 简历分析与模拟面试平台，主打 Java 后端 / AI 应用后端方向。项目已经形成“简历制作、简历上传解析、AI 岗位匹配分析、模拟面试、AI 对话、历史记录、用户登录注册、数据库持久化”的完整闭环。

## 已完成功能

### 1. 主工作台

- NotebookLM 风格三栏布局：左侧简历与岗位，中间分析结果，右侧模拟面试与 AI 对话。
- 页面整体固定高度，各区域独立滚动，适合大量 AI 分析文本展示。
- 支持最近记录入口，方便快速查看历史分析。

### 2. 简历上传与解析

- 支持 PDF、DOCX、TXT 简历上传。
- 使用 Apache Tika 提取简历文本。
- 支持直接粘贴简历文本。
- 支持文件类型、文件大小和文本长度校验。

### 3. AI 简历分析

- 根据简历和目标岗位 JD 输出岗位匹配分数。
- 输出分析摘要、优势、短板、优化建议、项目改写建议。
- 支持后端默认 AI 配置，也支持用户在前端填写自己的中转站配置。
- AI 调用失败时提供本地兜底结果，保证演示稳定。

### 4. 模拟面试与 AI 对话

- 根据当前简历和 JD 生成模拟面试题。
- 支持围绕 Java、Spring Boot、MySQL、Redis、项目经验和 AI 应用进行追问。
- AI 对话支持携带当前简历与岗位上下文。
- 未配置用户自定义 AI 时，限制 AI 对话，避免消耗项目默认 Key。
- 登录用户的 AI 对话会自动保存为历史会话，支持恢复和删除。

### 5. 用户注册登录

- 支持手机号验证码注册。
- 支持用户名/手机号登录。
- 支持忘记密码和验证码重置密码。
- 验证码优先使用 Redis 缓存，Redis 不可用时本地缓存兜底。
- 支持轻量 JWT 鉴权。
- 密码使用加盐哈希保存。

### 6. 历史记录

- 简历分析完成后自动保存记录。
- 支持最近记录列表、详情查看、恢复历史现场和删除记录。
- 登录用户按 user_id 隔离历史记录。
- 已支持 MySQL 持久化，`analysis_record` 表已验证能写入分析记录。
- 已支持 `chat_session` 和 `chat_message_record` 保存 AI 对话历史。

### 7. 简历编辑器

- 支持多套 A4 简历模板。
- 支持直接在简历预览上编辑内容，接近可画/Canva 的编辑体验。
- 支持模块标题编辑、照片上传、照片显示/隐藏。
- 支持主题色、字体、自定义主色和预览缩放。
- 支持导出 PDF、HTML、JSON、Markdown 和 WPS 可编辑 Word。

### 8. 数据库

- 默认支持 H2 文件数据库，适合本地快速演示。
- 已完成 MySQL 切换能力：
  - 增加 MySQL 驱动。
  - 增加 `application-mysql.yml`。
  - 支持通过 `.env` 设置 `SPRING_PROFILES_ACTIVE=mysql`。
  - 已验证 `app_user`、`analysis_record` 表自动创建。
  - 已验证简历分析记录写入 MySQL。
- MySQL 表结构和索引说明见 `docs/database.md`、`docs/schema-mysql.sql`。

### 9. 后端工程规范

- Controller / Service / Repository / DTO 分层。
- 统一返回结构。
- 全局异常处理。
- 参数校验和文件校验。
- AI 调用超时控制和异常兜底。
- SLF4J 日志记录关键流程。
- `.env` 管理敏感配置，避免密钥进入 GitHub。

### 10. Docker 部署

- 已提供 `docker-compose.yml` 一键启动 MySQL、Redis、后端和前端。
- 后端容器使用 `mysql,docker` profile，连接 Compose 内部 MySQL 和 Redis。
- 前端容器使用 Nginx 托管 Vue 静态资源，并将 `/api` 代理到后端容器。
- 为避免和本机服务冲突，Docker MySQL 映射到 `3307`，Redis 映射到 `6380`，后端映射到 `8081`，前端映射到 `3000`。

## 已完成文档

- `README.md`：项目首页说明。
- `docs/api.md`：接口文档。
- `docs/database.md`：数据库设计与 MySQL 切换说明。
- `docs/schema-mysql.sql`：MySQL 建表脚本。
- `docs/docker-deploy.md`：Docker Compose 部署说明。
- `docs/demo-script.md`：项目演示脚本。
- `docs/interview-guide.md`：面试讲解提纲。
- `docs/github-checklist.md`：GitHub 上传检查清单。
- `docs/demo-jd.txt`：演示岗位 JD。
- `docs/demo-resume.txt`：演示简历文本。
- `docs/roadmap.md`：后续优化规划。

## 仍可继续优化

### 高优先级

- 将最新 MySQL 切换代码和文档提交 GitHub。
- 准备项目讲解稿：重点解释 H2 与 MySQL 切换、表结构设计、AI 调用流程。
- 补充 README 截图，展示首页、分析结果、简历编辑器和 MySQL 数据写入。

### 中优先级

- AI 对话会话搜索、导出和重命名。
- 增加用户每日 AI 调用额度。
- 增加 Docker 镜像体积优化和生产环境 Nginx HTTPS 配置。

### 低优先级

- 服务端原生 DOCX/PDF 导出。
- 简历编辑器拖拽式模块排序。
- 云服务器部署和在线演示地址。

## 当前结论

项目已经具备写进简历、上传 GitHub 和现场演示的完整度。当前最值得投入的是文档、截图、GitHub 同步和项目讲解准备；继续加功能时，优先选择“用户调用额度限制”和“Docker Compose 一键启动”，这两个更贴近真实企业项目。
