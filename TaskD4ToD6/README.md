# 航空客运预订系统

基于 [PostgresPro 航空运输演示数据库](https://postgrespro.ru/education/demodb)（2025-09-01 版本），完成数据修复（Task D4）、RESTful API 设计（Task D5）与 API 实现（Task D6）。

讲稿与演示命令见 **[汇报.md](汇报.md)**。

---

## 项目结构

```
.
├── README.md
├── 汇报.md                          # 答辩讲稿 + 演示命令 + Q&A
├── Task D4/
│   ├── price.sql                    # 建 bookings.pricing_rules 并填充
│   └── d4.sql                       # 验证定价模型
├── Task D5/
│   └── api.yaml                     # OpenAPI 3.0 接口契约
└── Task D6/
    ├── api.yaml                     # 与 Task D5 同步，D6 实现依据
    ├── package.json
    ├── docker-compose.yaml          # Docker 全栈（web + postgres）
    ├── docker-compose.local.yaml    # 仅 web，连本机 Postgres（推荐）
    ├── SETUP.md                     # 环境配置与排错
    ├── requests.txt                 # GET/POST 示例（含 outbound 筛选）
    ├── postgres-init/indexes.sql    # 查询索引（demo 导入后执行）
    └── app/
        ├── main.js                  # 唯一入口
        ├── db.js                    # 连接池
        ├── asyncHandler.js          # async 路由错误捕获
        ├── Dockerfile
        └── routes/                  # 路由 + SQL 业务逻辑
            ├── index.js
            ├── cities.js
            ├── airports.js
            ├── routes.js            # 递归 CTE 航线搜索
            ├── bookings.js
            └── checkin.js
```

---

## 快速开始（已验证流程）

### 前提

1. 已安装并启动 **Docker Desktop**
2. 本机 PostgreSQL 已导入 **PostgresPro demo** 数据（库名 `demo`）
3. 已执行 D4 脚本（创建 `bookings.pricing_rules`）

### 1. 执行 D4（首次或重置后）

```powershell
# 本机 psql
psql -U postgres -d demo -f "Task D4/price.sql"
psql -U postgres -d demo -f "Task D4/d4.sql"

# 或在 Docker 容器内
Get-Content "Task D4\price.sql" -Raw | docker exec -i postgres_db_demo psql -U postgres -d demo
```

### 2. 启动 API

```powershell
cd "Task D6"
docker compose -f docker-compose.local.yaml up --build
```

> 若 demo 跑在 Docker 容器内而非本机，见 `Task D6/SETUP.md`。

### 3. 验证接口

| 步骤 | URL | 预期 |
|------|-----|------|
| 健康检查 | http://localhost:3000/ | `你看看是不是这里` |
| 城市列表 | http://localhost:3000/cities | JSON 数组 |
| 城市机场 | http://localhost:3000/cities/Moscow/airports | 机场列表 |
| 航线搜索 | http://localhost:3000/routes?from=Moscow&to=LED&date=2025-12-31&connections=1&bookingClass=economy | 航线 JSON |
| 出港（带筛选） | http://localhost:3000/airports/SVO/outbound?destination=LED&time_from=06:00:00&time_to=23:59:59&limit=5 | 最多 5 条，目的地 LED |
| 订票 / 值机 | 见 `Task D6/requests.txt` | 201 + JSON |

---

## Task D4 — 定价规则

| 文件 | 作用 |
|------|------|
| `price.sql` | 创建 `bookings.pricing_rules` 并从历史 `segments` 聚合填充 |
| `d4.sql` | 对比「计算价」与「实际价」，验证模型 |

**三者关系（易混淆）：**

| 层次 | 来源 |
|------|------|
| 数据库 `demo` | Docker 或本机创建 |
| `bookings.*` 航班表 | 导入 PostgresPro demo |
| `bookings.pricing_rules` | **执行 `price.sql` 后才有**（D4 交付物，不在原始 demo 中） |

定价公式：`票价 = 飞行分钟数 × 舱位每分钟单价`

| 舱位 | 每分钟单价 | 依据 |
|------|-----------|------|
| Economy | 50 | 从历史 `segments` 反推的均价（见 `Task D4/verify_rates.sql`） |
| Comfort | 65 | 同上，约为经济舱的 1.3 倍 |
| Business | 100 | 同上，为经济舱的 2 倍 |

这三个数不是随意选取：demo 库中全部历史票价均满足 `price = 单价 × 飞行分钟数`，回代误差为 0。

---

## Task D5 — API 设计

**文件：** `Task D5/api.yaml`（OpenAPI 3.0，可用 [Swagger Editor](https://editor.swagger.io/) 预览）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/cities` | 所有城市 |
| GET | `/cities/{city}/airports` | 城市下的机场 |
| GET | `/airports` | 所有机场 |
| GET | `/airports/{code}/inbound` | 进港航线（可选 `origin`、`time_from`、`time_to`、`limit`） |
| GET | `/airports/{code}/outbound` | 出港航线（可选 `destination`、`time_from`、`time_to`、`limit`） |
| GET | `/routes` | 航线搜索（直飞 + 中转） |
| POST | `/bookings` | 创建预订（支持多段 `segments`） |
| POST | `/check-in` | 在线值机（仅需 `ticket_no`） |

D6 的 `app/routes/*.js` 与上述接口一一对应。  
出港/进港筛选在 `app/routes/airports.js` 实现：`destination`/`origin`、`time_from`/`time_to`、`limit`。

---

## Task D6 — 后端实现

### 架构说明

- `main.js` — 启动 + 挂载路由
- `db.js` — 数据库连接池
- `routes/*.js` — 接收请求、执行 SQL、返回 JSON（逻辑内聚）

依赖仅 `express` + `pg`。

### 启动方式

| 场景 | 命令 |
|------|------|
| 本机已有 demo（**推荐**） | `docker compose -f docker-compose.local.yaml up --build` |
| Docker 内置 Postgres | `docker compose up --build`（需先导入 demo，见 SETUP.md） |
| 不用 Docker | `cd app && node main.js`（需设置 `DB_*` 环境变量） |

### 本地运行（无 Docker）

```powershell
cd "Task D6"
npm install
$env:DB_HOST="localhost"
$env:DB_NAME="demo"
$env:DB_USER="postgres"
$env:DB_PASSWORD="你的密码"
cd app
node main.js
```

### 常见报错

| 报错 | 原因 | 处理 |
|------|------|------|
| `dockerDesktopLinuxEngine ... cannot find the file` | Docker Desktop 未启动 | 打开 Docker Desktop |
| `relation "bookings.airports_data" does not exist` | demo 航班数据未导入 | 见 `SETUP.md` |
| `missing FROM-clause entry for table` | SQL 别名 / JOIN 顺序错误 | 已修复于 `routes.js`，`docker compose restart web` |
| `ERR_CONNECTION_REFUSED` | 后端容器未运行 | `docker compose ps` / `logs web` |

---

## 数据库说明（2025 新版 demo）

下载：https://postgrespro.ru/education/demodb

| 旧版 | 新版 |
|------|------|
| `ticket_flights` | `segments` |
| `airports` 视图 | `airports_data`（jsonb，用 `->>'en'` 取英文） |
| `amount` | `price` |
| 无时效路由 | `routes.validity @> flights.scheduled_departure` |

索引脚本（demo 导入后执行）：

```powershell
psql -U postgres -d demo -f "Task D6/postgres-init/indexes.sql"
```

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 运行时 | Node.js 20 |
| Web 框架 | Express 4 |
| 数据库 | PostgreSQL ≥ 15 |
| 驱动 | pg 8 |
| 容器 | Docker Compose + postgres:17 |

---