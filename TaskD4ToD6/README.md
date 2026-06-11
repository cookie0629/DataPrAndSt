# 航空客运预订系统 (Airline Booking System)

基于 [PostgresPro 航空运输演示数据库](https://postgrespro.ru/education/demodb)（2025-09-01 版本），完成数据修复（Task D4）、RESTful API 设计（Task D5）与 API 实现（Task D6）。项目结构对齐参考模板 `HOI_D1-D6_tasks`，采用极简扁平风格。

---

## 项目结构

```
.
├── README.md
├── 汇报.md
├── Task D4/
│   ├── price.sql          # pricing_rules 建表与历史数据填充
│   └── d4.sql             # 定价验证查询
├── Task D5/
│   └── api.yaml           # OpenAPI 3.0 接口规范
└── Task D6/
    ├── package.json       # Node.js 依赖清单
    ├── docker-compose.yaml
    ├── requests.txt       # 示例请求
    ├── postgres-init/
    │   └── indexes.sql    # 数据库索引初始化
    └── app/
        ├── Dockerfile
        ├── main.js        # 单一入口（Express 启动 + 路由挂载）
        ├── db.js          # PostgreSQL 连接池
        └── routes/        # 路由 + 业务逻辑（SQL 内聚于此）
            ├── index.js
            ├── cities.js
            ├── airports.js
            ├── routes.js
            ├── bookings.js
            └── checkin.js
```

---

## 数据库说明

使用 PostgresPro **demo** 数据库（PostgreSQL ≥ 15），表位于 `bookings` schema。

下载地址：https://postgrespro.ru/education/demodb

| 旧版 | 新版（本项目） |
|------|----------------|
| `ticket_flights` | `segments` |
| `aircrafts` | `airplanes_data`（jsonb） |
| `airports` 视图 | `airports_data` 表（jsonb） |
| `amount` | `price` |
| `aircraft_code` on `flights` | `airplane_code` on `routes` |
| 无时效路由 | `routes.validity @> flights.scheduled_departure` |

查询城市/机场名时使用 `city->>'en'`、`airport_name->>'en'`。

---

## Task D4 — 定价规则

| 文件 | 作用 |
|------|------|
| `price.sql` | 创建 `bookings.pricing_rules` 并从历史 `segments` 填充 |
| `d4.sql` | 对比计算价与实际价（验证脚本） |

### 执行顺序

```bash
psql -U postgres -d demo -f "Task D4/price.sql"
psql -U postgres -d demo -f "Task D4/d4.sql"
```

定价模型：Economy = 50 / Comfort = 65 / Business = 100（每分钟单价），实际票价 = 单价 × 飞行时长（分钟）。

---

## Task D5 — API 设计

**文件：** `Task D5/api.yaml`

可用 [Swagger Editor](https://editor.swagger.io/) 预览。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/cities` | 所有城市 |
| GET | `/cities/{city}/airports` | 城市下的机场 |
| GET | `/airports` | 所有机场 |
| GET | `/airports/{code}/inbound` | 进港航线 |
| GET | `/airports/{code}/outbound` | 出港航线 |
| GET | `/routes` | 航线搜索（支持中转、舱位） |
| POST | `/bookings` | 创建预订 |
| POST | `/check-in` | 在线值机 |

---

## Task D6 — API 实现

### 环境要求

- Node.js ≥ 18
- PostgreSQL ≥ 15（已导入 demo 数据库）
- Docker Desktop（仅在使用 `docker compose` 时需要）

### 方式一：Docker 启动

**前置条件：**

1. 启动 **Docker Desktop**（托盘显示 *Engine running*）
2. **必须已导入 PostgresPro demo 数据**（Docker 自带的 Postgres 是空库！）

详细步骤见 **`Task D6/SETUP.md`**。

```bash
cd "Task D6"
docker compose up --build
```

若本机 PostgreSQL 已导入 demo，可只跑 Node 容器、连本机库：

```bash
docker compose -f docker-compose.local.yaml up --build
```

服务地址：`http://localhost:3000`（先访问 `/` 应看到 `Hello world`，再访问 `/cities`）

#### 常见报错

| 报错 | 原因 | 处理 |
|------|------|------|
| `dockerDesktopLinuxEngine ... cannot find the file` | Docker Desktop 未启动 | 打开 Docker Desktop 后重试 |
| `relation "bookings.airports_data" does not exist` | **demo 数据未导入** | 见 `Task D6/SETUP.md` |
| `hoi_backend exited with code 1` | 访问接口时数据库缺表导致崩溃（已修复，请重新 build） | 导入 demo 数据 |
| `ERR_CONNECTION_REFUSED` | 后端容器未在运行 | `docker compose ps` 检查，看 logs |

验证 Docker 是否可用：

```bash
docker version
docker info
```

两条命令均能正常输出后再执行 `docker compose up --build`。

### 方式二：本地直接运行（无需 Docker）

适用于已有本地 PostgreSQL 且已导入 demo 库的情况：

```bash
cd "Task D6"
npm install

# 设置数据库连接（PowerShell 示例）
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="demo"
$env:DB_USER="postgres"
$env:DB_PASSWORD="你的密码"

cd app
node main.js
```

可选：在本地 demo 库执行索引脚本以提升查询性能：

```bash
psql -U postgres -d demo -f "Task D6/postgres-init/indexes.sql"
```

### 示例请求

见 `Task D6/requests.txt`，或快速验证：

```bash
curl http://localhost:3000/cities
curl "http://localhost:3000/routes?from=Moscow&to=LED&date=2025-12-31&connections=0&bookingClass=economy"
```

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 运行时 | Node.js 20 |
| Web 框架 | Express 4 |
| 数据库 | PostgreSQL ≥ 15（demo） |
| 数据库驱动 | pg 8 |
| 容器化 | Docker Compose + postgres:17 |

---
