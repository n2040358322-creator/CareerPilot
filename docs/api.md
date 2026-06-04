# CareerPilot 接口文档

基础地址：

```text
http://localhost:8080/api
```

## 统一返回结构

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "timestamp": "2026-05-24T10:00:00"
}
```

登录后访问用户相关接口时，前端会携带：

```http
Authorization: Bearer <token>
```

## 1. 健康检查

### GET `/health`

用于确认后端服务是否启动。

## 2. 用户认证

### POST `/auth/send-code`

发送 4 位验证码。当前项目为演示模式，后端会把验证码返回给前端展示；验证码优先写入 Redis，Redis 不可用时使用本地缓存兜底。

请求：

```json
{
  "phone": "13800138000",
  "purpose": "REGISTER"
}
```

`purpose` 可选值：

- `REGISTER`：注册。
- `RESET_PASSWORD`：忘记密码/重置密码。

### POST `/auth/register`

手机号验证码注册，注册成功后返回登录 Token。

请求：

```json
{
  "username": "student01",
  "phone": "13800138000",
  "code": "A7K2",
  "password": "123456",
  "confirmPassword": "123456"
}
```

### POST `/auth/login`

支持用户名或手机号登录。

请求：

```json
{
  "identifier": "student01",
  "password": "123456"
}
```

### POST `/auth/reset-password`

通过手机号和验证码重置密码。

请求：

```json
{
  "phone": "13800138000",
  "code": "A7K2",
  "password": "new123456",
  "confirmPassword": "new123456"
}
```

### GET `/auth/me`

获取当前登录用户信息。未登录时返回未认证状态。

## 3. AI 配置

### GET `/ai/status`

查询后端默认 AI 配置是否可用，以及当前默认模型名称。

说明：

- 如果用户未在前端配置自己的 AI，中间分析功能会尝试使用后端默认配置。
- AI 对话功能要求用户前端配置自己的 `apiKey`、`baseUrl`、`model`。

## 4. 简历解析与生成

### POST `/resumes/parse`

上传并解析 PDF、DOCX、TXT 简历。

请求类型：

```text
multipart/form-data
```

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `file` | File | 简历文件，支持 PDF/DOCX/TXT |

### POST `/resumes/build`

根据结构化信息生成 Markdown 简历。AI 可用时会尝试优化内容；不可用时返回本地模板结果。

请求：

```json
{
  "name": "张三",
  "targetRole": "Java 后端开发实习生",
  "education": "上海电力大学｜计算机科学与技术｜本科｜2023.09 - 至今",
  "skills": "Java、Spring Boot、MySQL、Redis、RESTful API、Vue3",
  "projects": "CareerPilot AI 简历分析与模拟面试平台...",
  "experience": "校园经历、实习经历或竞赛经历",
  "aiConfig": null
}
```

## 5. 简历岗位匹配分析

### POST `/analysis`

根据简历文本和岗位 JD 生成匹配分析。

请求：

```json
{
  "resumeName": "张三-后端实习简历",
  "resumeText": "简历正文",
  "jobDescription": "Java 后端开发实习生岗位 JD",
  "aiConfig": null
}
```

`aiConfig` 说明：

```json
{
  "apiKey": "sk-xxx",
  "baseUrl": "https://api.example.com/v1",
  "model": "gpt-5.2"
}
```

- 前端传入 `aiConfig` 时，优先使用用户自己的中转站配置。
- 未传入 `aiConfig` 时，使用后端 `.env` 默认配置。
- AI 调用失败时，后端返回本地兜底分析结果。
- 登录用户生成的分析记录会绑定当前用户 ID。

## 6. 历史记录

### GET `/analysis/records`

查询最近分析记录。

说明：

- 登录用户只能看到自己的记录。
- 未登录用户只能看到匿名记录。
- 默认用于右上角“最近记录”抽屉。

### GET `/analysis/records/{id}`

查询某条分析记录详情，可用于恢复简历文本、岗位 JD 和分析结果。

### DELETE `/analysis/records/{id}`

删除某条历史记录。

说明：

- 只能删除当前用户有权限访问的记录。

## 7. 模拟面试

### POST `/interview/questions`

根据简历和岗位 JD 生成模拟面试题。

请求：

```json
{
  "resumeText": "简历正文",
  "jobDescription": "岗位 JD",
  "answer": "",
  "aiConfig": null
}
```

## 8. AI 对话

### POST `/chat`

基于当前简历和 JD 进行多轮 AI 对话。

请求：

```json
{
  "resumeText": "简历正文",
  "jobDescription": "岗位 JD",
  "messages": [
    {
      "role": "user",
      "content": "帮我优化 CareerPilot 项目描述"
    }
  ],
  "aiConfig": {
    "apiKey": "sk-xxx",
    "baseUrl": "https://api.okinto.com/v1",
    "model": "gpt-5.5"
  }
}
```

说明：

- AI 对话必须传入用户前端自己的 `aiConfig`。
- 未配置时后端会拒绝对话请求，避免消耗项目默认 Key。

### GET `/chat/sessions`

获取当前登录用户最近 20 条 AI 对话会话。

说明：

- 需要登录，后端通过 `Authorization: Bearer <token>` 获取当前用户。
- 按 `updatedAt` 倒序返回。

### GET `/chat/sessions/{id}`

获取某个 AI 对话会话详情，包括会话上下文和消息列表。

说明：

- 只能访问当前登录用户自己的会话。
- 返回的 `messages` 按创建时间正序排列，可直接恢复到前端对话框。

### DELETE `/chat/sessions/{id}`

删除某个 AI 对话会话及其全部消息。

说明：

- 只能删除当前登录用户自己的会话。
- 删除后前端会刷新历史列表；如果删除的是当前会话，会自动切换为新对话。
