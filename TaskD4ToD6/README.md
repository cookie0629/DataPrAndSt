# 航空客运预订系统

基于 [PostgresPro 航空运输演示数据库](https://postgrespro.ru/education/demodb)（2025-09-01 版），完成 **D4 定价规则补全 → D5 OpenAPI 契约 → D6 Node.js 实现** 三步交付。

**答辩讲稿与代码指读**见 **[汇报.md](汇报.md)**（重点讲解 `Task D5/api.yaml` 与 Task D6 整体架构）。

---

## 项目结构

```
.
├── README.md
├── 汇报.md                          # 答辩主文档（D5 契约 + D6 实现）
├── Task D4/
│   ├── price.sql                    # 建 bookings.pricing_rules 并填充
│   ├── d4.sql                       # 验证定价模型
│   └── verify_rates.sql             # 反推 50/65/100 的数据依据
├── Task D5/
│   └── api.yaml                     # OpenAPI 3.0.3 接口契约（设计稿）
└── Task D6/
    ├── api.yaml                     # 与 D5 同步（实现依据）
    ├── package.json
    ├── docker-compose.yaml          # Docker 全栈（web + postgres）
    ├── docker-compose.local.yaml    # 仅 web，连本机 Postgres（推荐）
    ├── SETUP.md                     # 环境配置与排错
    ├── requests.txt                 # GET/POST 示例
    ├── postgres-init/indexes.sql    # 查询索引
    └── app/
        ├── main.js                  # 入口：挂载路由 + 启动检查
        ├── db.js                    # pg 连接池
        ├── asyncHandler.js          # async 路由错误包装
        ├── Dockerfile
        └── routes/                  # 一文件一接口组，SQL 内联
            ├── index.js             # GET /
            ├── cities.js            # GET /cities, /cities/{city}/airports
            ├── airports.js          # GET /airports, inbound, outbound（含筛选）
            ├── routes.js            # GET /routes（递归 CTE）
            ├── bookings.js          # POST /bookings
            └── checkin.js           # POST /check-in
```

---

## 快速开始

### 前提

1. **Docker Desktop** 已启动
2. 本机 PostgreSQL 已导入 PostgresPro **demo** 库
3. 已执行 D4 的 `price.sql`（创建 `bookings.pricing_rules`）

### 1. D4（首次或重置后）

```powershell
psql -U postgres -d demo -f "Task D4/price.sql"
psql -U postgres -d demo -f "Task D4/d4.sql"
```

### 2. 启动 API

```powershell
cd "Task D6"
docker compose -f docker-compose.local.yaml up --build
```

成功标志：

```
数据库检查通过：bookings.airports_data 已存在
服务已启动: http://localhost:3000
```

### 3. 快速验证

| URL | 说明 |
|-----|------|
| http://localhost:3000/ | 健康检查 |
| http://localhost:3000/cities | 城市列表 |
| http://localhost:3000/airports/SVO/outbound?destination=LED&limit=5 | 出港筛选 |
| http://localhost:3000/routes?from=Moscow&to=LED&date=2025-12-31&connections=1&bookingClass=economy | 航线搜索 |

POST 订票 / 值机见 `Task D6/requests.txt`。更多命令见 `汇报.md` 第二节。

---

## 三个 Task 的关系

```
D4 price.sql          D5 api.yaml              D6 app/routes/*.js
定价规则表     →      接口契约（先设计）   →    按契约实现（后编码）
     │                      │                         │
     └──── bookings.js 用同一组单价 50/65/100 ────────┘
```

| Task | 交付物 | 一句话 |
|------|--------|--------|
| D4 | `price.sql` + `d4.sql` | 从历史票价提炼 `pricing_rules`，验证「分钟数 × 单价」模型 |
| D5 | `api.yaml` | OpenAPI 3.0.3 定义 8 个 REST 接口，含出港/进港筛选参数 |
| D6 | `app/` + Docker | Express 实现全部接口，扁平 `routes/` 架构 |

---

## Task D4 概要

定价公式：**票价 = 飞行分钟数 × 舱位每分钟单价**

| 舱位 | 单价 | 依据 |
|------|------|------|
| Economy | 50 | 历史 `segments` 反推均价（见 `verify_rates.sql`） |
| Comfort | 65 | 同上 |
| Business | 100 | 同上 |

新版 demo 要点：`routes.validity @> flights.scheduled_departure`（航线有时效版本）；机场/城市名在 jsonb 字段，用 `->>'en'` 取值。

---

## Task D5 / D6 详细说明

**D5** 与 **D6** 的完整讲解（接口分组、新增筛选功能、`components` 复用、各路由文件职责、SQL 策略）均在 **[汇报.md](汇报.md)**：

- **第二节** — `Task D5/api.yaml` 逐段讲解，强调新增功能
- **第三节** — Task D6 目录结构、基础设施、六个路由模块
- **第四节** — 现场演示 URL 与命令
- **第五节** — Q&A

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 运行时 | Node.js 20 |
| Web | Express 4 |
| 数据库 | PostgreSQL ≥ 15（PostgresPro demo） |
| 驱动 | pg 8 |
| 容器 | Docker Compose |

依赖仅 `express` + `pg`，无 ORM。

---

## 常见问题

| 现象 | 处理 |
|------|------|
| `relation "bookings.airports_data" does not exist` | demo 未导入，见 `Task D6/SETUP.md` |
| `ERR_CONNECTION_REFUSED` | 容器未运行：`docker compose ps` / `logs web` |
| 改代码不生效 | `docker compose restart web` |
| 端口 5432 冲突 | 用 `docker-compose.local.yaml` 连本机库，或改映射端口 |

---

## 参考

- Demo 下载：https://postgrespro.ru/education/demodb
- Swagger 预览：https://editor.swagger.io/（粘贴 `Task D5/api.yaml`）
