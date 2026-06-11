# Task D6 环境配置指南

## 你遇到的报错是什么意思？

```
relation "bookings.airports_data" does not exist
hoi_backend exited with code 1
```

**原因：** Docker 里的 PostgreSQL 是一个**空数据库**，只有库名 `demo`，没有 PostgresPro 的航班数据。  
本项目的代码要查 `bookings.airports_data` 等表，表不存在就会报错。

**Docker 已启动 ≠ 数据已导入。** 还需要单独导入 demo 数据。

---

## 方案 A：用本机 PostgreSQL（推荐，如果你已经导入过 demo）

如果你之前在本地 `psql` 里跑过 D4 脚本，说明本机很可能已有 demo 库。

### 1. 只启动 Node 服务，不用 Docker 里的数据库

```powershell
cd "F:\XOU\DataPrAndSt\TaskD4ToD6\Task D6"
docker compose -f docker-compose.local.yaml up --build
```

这会连接你**电脑上**的 PostgreSQL（`host.docker.internal:5432`）。

### 2. 或者完全不使用 Docker，直接本地跑

```powershell
cd "F:\XOU\DataPrAndSt\TaskD4ToD6\Task D6"
npm install

$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="demo"
$env:DB_USER="postgres"
$env:DB_PASSWORD="你的密码"

cd app
node main.js
```

### 3. 验证

浏览器打开：

- http://localhost:3000/          → 应显示 `Hello world`
- http://localhost:3000/cities    → 应返回城市 JSON 数组

---

## 方案 B：把 demo 数据导入 Docker 里的 PostgreSQL

### 1. 下载 demo 数据

从 PostgresPro 下载（选最小的 3 个月版即可，约 133MB）：

https://postgrespro.ru/education/demodb

文件名类似：`demo-20250901-3m.sql.gz`

### 2. 清空旧 Docker 数据卷（重要！）

你之前的 Docker 数据库是空的，需要删掉重来：

```powershell
cd "F:\XOU\DataPrAndSt\TaskD4ToD6\Task D6"
docker compose down -v
```

`-v` 会删除 `pgdata` 卷，否则 Postgres 会跳过初始化。

### 3. 先只启动数据库容器

```powershell
docker compose up -d db
```

等几秒，确认数据库就绪：

```powershell
docker exec postgres_db_demo pg_isready -U postgres
```

### 4. 导入 demo 数据

**如果有 Git Bash：**

```bash
cd /f/你的下载目录
gunzip -c demo-20250901-3m.sql.gz | docker exec -i postgres_db_demo psql -U postgres
```

**如果只有 PowerShell（先解压 .gz 得到 .sql 文件）：**

```powershell
# 假设 demo.sql 在 D:\Downloads\demo.sql
Get-Content "D:\Downloads\demo.sql" -Raw | docker exec -i postgres_db_demo psql -U postgres
```

导入需要几分钟，完成后应能看到 `CREATE DATABASE` 等输出。

### 5. 创建索引 + 执行 D4 脚本（可选但推荐）

```powershell
docker exec -i postgres_db_demo psql -U postgres -d demo < postgres-init/indexes.sql
docker exec -i postgres_db_demo psql -U postgres -d demo < "../Task D4/price.sql"
```

### 6. 启动完整服务

```powershell
docker compose up --build
```

### 7. 验证

http://localhost:3000/cities

---

## 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| `ERR_CONNECTION_REFUSED` | Node 容器已崩溃退出 | 看 `docker compose logs web`，多半是数据库没数据 |
| `airports_data does not exist` | demo 未导入 | 按上面方案 A 或 B 操作 |
| 端口 5432 冲突 | 本机 Postgres 和 Docker 都在占 5432 | 改 `docker-compose.yaml` 里 db 的端口为 `5433:5432`，或停掉本机 Postgres |
| `Skipping initialization` | 用了旧的空数据卷 | `docker compose down -v` 后重新导入 |

---

## 启动成功的标志

终端里应看到：

```
数据库检查通过：bookings.airports_data 已存在
服务已启动: http://localhost:3000
```

如果看到**警告**说表不存在，服务能起来但 `/cities` 会返回 500 JSON 错误提示，按本文导入数据即可。
