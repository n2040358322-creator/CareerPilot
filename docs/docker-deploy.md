# Docker 部署说明

CareerPilot 提供 Docker Compose 一键部署，包含：

- `mysql`：MySQL 8.0，容器内端口 `3306`，本机映射 `3307`。
- `redis`：Redis 7，容器内端口 `6379`，本机映射 `6380`。
- `backend`：Spring Boot 后端，容器内端口 `8080`，本机映射 `8081`。
- `frontend`：Nginx + Vue 静态页面，本机访问 `http://localhost:3000`。

## 1. 准备环境变量

如果要让后端默认 AI 配置可用，可以复制示例文件：

```powershell
cd C:\Users\Administrator\CareerPilot
Copy-Item .env.docker.example .env
```

然后修改 `.env`：

```env
AI_API_KEY=你的Key
AI_BASE_URL=https://api.okinto.com/v1
AI_MODEL=gpt-5.5
JWT_SECRET=请改成更长的随机字符串
```

如果不配置 `AI_API_KEY`，系统仍可启动；用户可以在前端右上角填写自己的中转站配置。

## 2. 启动

```powershell
cd C:\Users\Administrator\CareerPilot
docker compose up -d --build
```

第一次启动会拉取基础镜像并构建前后端镜像，耗时会比较久。

## 3. 访问

- 前端页面：`http://localhost:3000`
- 后端健康检查：`http://localhost:8081/api/health`
- MySQL：
  - Host：`localhost`
  - Port：`3307`
  - User：`careerpilot`
  - Password：`careerpilot123`
  - Database：`careerpilot`

## 4. 常用命令

查看容器状态：

```powershell
docker compose ps
```

查看后端日志：

```powershell
docker compose logs -f backend
```

停止服务：

```powershell
docker compose down
```

停止并删除数据库卷：

```powershell
docker compose down -v
```

## 5. 注意事项

- Docker MySQL 映射到本机 `3307`，避免和本机 MySQL `3306` 冲突。
- Docker Redis 映射到本机 `6380`，避免和本机 Redis `6379` 冲突。
- 前端容器通过 Nginx 把 `/api` 代理到后端容器，不需要再运行 Vite 开发服务。
- `.env` 已被 `.gitignore` 排除，不要把真实 API Key 上传到 GitHub。
