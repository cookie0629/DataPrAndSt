# 航空客运预订系统 (Airline Booking System)

基于 [PostgresPro 航空运输演示数据库](https://postgrespro.ru/education/demodb)（2025-09-01 版本）构建的后端服务，涵盖数据修复（Task D4）、RESTful API 设计（Task D5）与 API 实现（Task D6）三个模块。

---

## 项目结构

```
.
├── README.md
├── Context.md                        # 项目需求文档
├── Task D4/
│   └── Task_D4.sql                   # 数据修复 & pricing_rules 表构建脚本
├── Task D5/
│   └── airlines.yml                  # OpenAPI 3.0 接口设计规范
└── Task D6/
    └── airline-service/              # Node.js RESTful API 实现
        ├── .env                      # 环境变量（不提交到 git）
        ├── app.js                    # Express 应用入口
        ├── server.js                 # HTTP 服务启动
        ├── api.yaml                  # 与实现同步的 OpenAPI 规范
        ├── package.json
        ├── configs/
        │   ├── appConfig.js          # 端口 / 环境配置
        │   └── dbConfig.js           # PostgreSQL 连接池
        ├── controllers/              # 请求处理层（薄层，调用 model）
        ├── middlewares/
        │   ├── errorHandler.js       # 统一错误响应
        │   ├── loggers/              # Winston 请求/错误日志
        │   └── validators/           # Celebrate + Joi 入参校验
        ├── models/                   # 数据库查询层（业务逻辑 + SQL）
        ├── routes/                   # Express 路由注册
        └── test/                     # Jest + Supertest 集成测试（19 个用例）
```

---

## 数据库说明

本项目使用 PostgresPro 提供的 **demo** 数据库（PostgreSQL ≥ 15），所有表位于 `bookings` schema 下。

下载地址：https://postgrespro.ru/education/demodb

### 2025-09-01 版本与旧版的关键差异

| 旧版 | 新版（本项目使用） |
|------|------|
| `ticket_flights` 表 | `segments` 表 |
| `aircrafts` 表 | `airplanes_data` 表（jsonb 字段） |
| `airports` 视图 | `airports_data` 表（`airport_name`、`city` 为 jsonb） |
| `amount` 字段 | `price` 字段（在 `segments` 中） |
| `aircraft_code` on `flights` | `airplane_code` on `routes` |
| 无时效路由 | `routes.validity @> flights.scheduled_departure`（temporal join） |
| 无 `outbound` 字段 | `tickets.outbound boolean`（区分去程/返程票） |

> `airports_data` 和 `airplanes_data` 的名称、城市等字段为 jsonb 格式，查询时需用 `->>'en'` 提取英文值。

---

## Task D4 — 数据修复与定价规则

**文件：** `Task D4/Task_D4.sql`

从历史 `segments` 数据还原每条航线 × 每种舱位的基准价格，写入 `bookings.pricing_rules`。

### 执行方法

```bash
psql -U postgres -d demo -f "Task D4/Task_D4.sql"
```

脚本已做幂等处理，可安全重复执行。

### 产出表结构

```sql
bookings.pricing_rules (
  route_no        text,
  fare_conditions text,
  base_price      numeric(10,2),   -- 该航线该舱位的平均历史价格
  seat_count      bigint,          -- 参与统计的座位数
  PRIMARY KEY (route_no, fare_conditions)
)
```

执行结果：1410 行，覆盖所有航线的所有舱位组合。

### 实现思路（6步流水线）

1. 从 `segments` + `boarding_passes` + `flights` 提取每个 `(route_no, fare_conditions, seat_no)` 的历史均价
2. 从 `routes` + `seats` 枚举出所有航线应有的座位组合（直接用 `routes.airplane_code`，无需 JOIN `airplanes_data`）
3. LEFT JOIN 得到含空缺的价格表，价格为 0 表示该座位无历史销售记录
4. 对价格为 0 的座位，用同排同舱位有价格的座位插值补全
5. 合并得到每个座位的完整价格
6. 聚合为 `(route_no, fare_conditions)` → 均价，写入 `pricing_rules`，清理中间表

---

## Task D5 — API 设计规范

**文件：** `Task D5/airlines.yml`

OpenAPI 3.0 规范，可用 [Swagger Editor](https://editor.swagger.io/) 预览。

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/cities` | 分页获取所有城市（去重） |
| GET | `/cities/{cityName}/airports` | 获取指定城市的机场列表 |
| GET | `/airports` | 分页获取所有机场 |
| GET | `/airports/{airportCode}/schedules` | 获取机场进港/出港时刻表 |
| GET | `/flights` | 搜索两点间航班（支持城市/机场代码、中转次数限制） |
| POST | `/bookings` | 为单名乘客创建预订 |
| POST | `/checkins` | 办理值机（自动分配座位，生成登机牌） |

---

## Task D6 — API 实现

**目录：** `Task D6/airline-service/`

### 环境要求

- Node.js ≥ 18
- PostgreSQL ≥ 15，已导入 demo 数据库

### 快速开始

```bash
# 1. 安装依赖
cd "Task D6/airline-service"
npm install

# 2. 配置环境变量（复制后填入数据库密码）
# 创建 .env 文件：
#   NODE_ENV=development
#   PORT=3001
#   DB_HOST=localhost
#   DB_PORT=5432
#   DB_NAME=demo
#   DB_USER=postgres
#   DB_PASSWORD=your_password

# 3. 启动服务
npm start          # 生产模式
npm run dev        # 开发模式（nodemon 热重载）

# 4. 运行测试
npm test
```

服务启动后监听 `http://localhost:3001`。

### 测试结果

```
Test Suites: 4 passed, 4 total
Tests:       19 passed, 19 total
```

---

## API 接口详细说明

所有接口返回 JSON，错误响应统一格式：`{ "message": "错误描述" }`

### GET /cities

分页返回所有城市名称（去重，按字母排序）。

```
GET /cities?limit=5&page=1
```

响应：`["108 Mile Ranch", "A Coruña", "Aachen", "Aalborg", "Aarhus"]`

---

### GET /cities/{cityName}/airports

```
GET /cities/Moscow/airports
```

响应：
```json
[
  { "code": "SVO", "name": "Sheremetyevo", "city": "Moscow", "longitude": 37.4146, "latitude": 55.9726, "timezone": "Europe/Moscow" }
]
```

---

### GET /airports/{airportCode}/schedules

`type` 参数必填，值为 `inbound` 或 `outbound`。

```
GET /airports/SVO/schedules?type=outbound
```

响应：
```json
[
  { "flightNo": "PG0106", "departureTime": "02:40:00", "daysOfWeek": [1,2,3,4,5,6,7], "destination": "Saint Petersburg" }
]
```

---

### GET /flights

| 参数 | 必填 | 说明 |
|------|------|------|
| `origin` | 是 | 出发城市名或机场代码 |
| `destination` | 是 | 目的城市名或机场代码 |
| `departureDate` | 是 | 出发日期，格式 `YYYY-MM-DD` |
| `fareConditions` | 否 | `Economy` / `Comfort` / `Business` |
| `transfers` | 否 | 最大中转次数（`0`=仅直飞，不传=最多 3 次，最大值 3） |

---

### POST /bookings

请求体：
```json
{
  "flightNo": "PG0191",
  "flightDate": "2025-12-01",
  "fareConditions": "Economy",
  "passengerId": "1234 567890",
  "passengerName": "IVAN IVANOV",
  "outbound": true
}
```

成功响应（201）：返回含 `bookRef` 和 `ticketNo` 的预订信息。

错误场景：`400` 航班已到达/取消、无余座、无定价 | `404` 航班不存在

---

### POST /checkins

只需传 `ticketNo`，后端自动从 `segments` 查出关联航班：

请求体：`{ "ticketNo": "4908b9b9fc684" }`

成功响应（201）：
```json
{ "boardingNo": 1, "ticketNo": "4908b9b9fc684", "flightId": 11052, "seatNo": "17A" }
```

错误场景：`400` 已值机、无可用座位 | `404` 票不存在、航班状态不允许值机

---

## 数据库索引

```sql
-- Temporal join 核心索引
CREATE INDEX idx_flights_route_departure ON bookings.flights (route_no, scheduled_departure);

-- 航段查询
CREATE INDEX idx_segments_flight_fare ON bookings.segments (flight_id, fare_conditions);

-- 登机牌查询
CREATE INDEX idx_boarding_passes_flight ON bookings.boarding_passes (flight_id);
CREATE INDEX idx_boarding_passes_ticket ON bookings.boarding_passes (ticket_no);

-- 座位查询
CREATE INDEX idx_seats_airplane_fare ON bookings.seats (airplane_code, fare_conditions);

-- airports_data 城市名查询（jsonb 表达式索引）
CREATE INDEX idx_airports_city_en ON bookings.airports_data ((city->>'en'));
```

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 运行时 | Node.js 22 |
| Web 框架 | Express 4 |
| 数据库 | PostgreSQL ≥ 15（demo 数据库） |
| 数据库驱动 | node-postgres (pg 8) |
| 入参校验 | Celebrate 15 + Joi |
| 日志 | Winston 3 + express-winston |
| 错误处理 | http-errors 2 |
| ID 生成 | uuid 9 |
| 测试框架 | Jest 29 + Supertest |
| 开发工具 | Nodemon |
