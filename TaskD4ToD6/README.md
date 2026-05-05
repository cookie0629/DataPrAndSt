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
        ├── .gitignore
        ├── app.js                    # Express 应用入口
        ├── server.js                 # HTTP 服务启动
        ├── api.yaml                  # 与实现同步的 OpenAPI 规范
        ├── package.json
        ├── configs/
        │   ├── appConfig.js          # 端口 / 环境配置
        │   └── dbConfig.js           # PostgreSQL 连接池
        ├── controllers/              # 请求处理层（薄层，调用 model）
        │   ├── airport.js
        │   ├── bookings.js
        │   ├── cities.js
        │   └── flights.js
        ├── middlewares/
        │   ├── errorHandler.js       # 统一错误响应
        │   ├── loggers/
        │   │   ├── errorLogger.js    # Winston 错误日志 → logs/error.log
        │   │   └── requestLogger.js  # Winston 请求日志 → logs/request.log
        │   └── validators/           # Joi/Celebrate 入参校验
        │       ├── airportValidator.js
        │       ├── bookingsValidator.js
        │       ├── citiesValidator.js
        │       └── flightsValidator.js
        ├── models/                   # 数据库查询层（业务逻辑 + SQL）
        │   ├── Airport.js
        │   ├── Booking.js
        │   ├── City.js
        │   └── Flight.js
        ├── routes/                   # Express 路由注册
        │   ├── index.js
        │   ├── airports.js
        │   ├── bookings.js
        │   ├── cities.js
        │   └── flights.js
        └── test/                     # Jest + Supertest 集成测试
            ├── airports.test.js
            ├── bookings.test.js
            ├── cities.test.js
            └── flights.test.js
```

---

## 数据库说明

本项目使用 PostgresPro 提供的 **demo** 数据库（PostgreSQL >= 15），所有表位于 `bookings` schema 下。

下载地址：https://postgrespro.ru/education/demodb

### 2025-09-01 版本与旧版的关键差异

| 旧版 | 新版（本项目使用） |
|------|------|
| `ticket_flights` 表 | `segments` 表 |
| `aircrafts` 表 | `airplanes_data` 表（jsonb 字段） |
| `airports` 视图 | `airports_data` 表（`airport_name`、`city` 为 jsonb） |
| `amount` 字段 | `price` 字段（在 `segments` 中） |
| `aircraft_code` 字段 | `airplane_code` 字段 |
| `routes.flight_no` 关联 | Temporal join：`routes.validity @> flights.scheduled_departure` |
| `flights.aircraft_code` | `airplane_code` 在 `routes` 表，需 join 获取 |

> 注意：`airports_data` 和 `airplanes_data` 的名称、城市等字段为 jsonb 格式，查询时需用 `->>'en'` 提取英文值。

---

## Task D4 — 数据修复与定价规则

**文件：** `Task D4/Task_D4.sql`

### 目标

从历史 `segments` 表中还原每条航线（`route_no`）+ 舱位等级（`fare_conditions`）的标准定价，写入 `bookings.pricing_rules` 表，供未来航班定价使用。

### 执行方法

```bash
psql -U postgres -d demo -f "Task D4/Task_D4.sql"
```

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

### 实现思路

1. 从 `segments` + `boarding_passes` + `flights` 提取每个 `(route_no, fare_conditions, seat_no)` 的历史均价
2. 对于没有历史数据的座位，用同排同舱位的价格插值补全
3. 按 `(route_no, fare_conditions)` 聚合得到基准价格
4. 中间表在脚本末尾自动清理

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
| POST | `/flights/checkins` | 办理值机（自动分配座位，生成登机牌） |

---

## Task D6 — API 实现

**目录：** `Task D6/airline-service/`

使用 Node.js + Express 实现，连接 PostgreSQL，包含完整的日志、错误处理、入参校验和集成测试。

### 环境要求

- Node.js >= 18
- PostgreSQL >= 15，已导入 demo 数据库

### 快速开始

**1. 安装依赖**

```bash
cd "Task D6/airline-service"
npm install
```

**2. 配置环境变量**

复制并编辑 `.env`：

```env
NODE_ENV=development
PORT=3001

DB_HOST=localhost
DB_PORT=5432
DB_NAME=demo
DB_USER=postgres
DB_PASSWORD=your_password
```

**3. 启动服务**

```bash
# 生产模式
npm start

# 开发模式（nodemon 热重载）
npm run dev
```

服务启动后监听 `http://localhost:3001`。

**4. 运行测试**

```bash
npm test
```

测试结果（19 个用例全部通过）：

```
Test Suites: 4 passed, 4 total
Tests:       19 passed, 19 total
Time:        ~6s
```

---

## API 接口详细说明

所有接口返回 JSON，错误响应统一格式：

```json
{ "message": "错误描述" }
```

### GET /cities

分页返回所有城市名称（去重，按字母排序）。

```
GET /cities?limit=5&page=1
```

响应：
```json
["108 Mile Ranch", "A Coruña", "Aachen", "Aalborg", "Aarhus"]
```

---

### GET /cities/{cityName}/airports

返回指定城市的所有机场。

```
GET /cities/Moscow/airports
```

响应：
```json
[
  { "code": "BKA", "name": "Bykovo",       "city": "Moscow", "longitude": 38.06,   "latitude": 55.6172, "timezone": "Europe/Moscow" },
  { "code": "DME", "name": "Domodedovo",   "city": "Moscow", "longitude": 37.9063, "latitude": 55.4088, "timezone": "Europe/Moscow" },
  { "code": "SVO", "name": "Sheremetyevo", "city": "Moscow", "longitude": 37.4146, "latitude": 55.9726, "timezone": "Europe/Moscow" },
  { "code": "VKO", "name": "Vnukovo",      "city": "Moscow", "longitude": 37.2615, "latitude": 55.5915, "timezone": "Europe/Moscow" }
]
```

---

### GET /airports/{airportCode}/schedules

返回机场进港或出港时刻表。`type` 参数必填，值为 `inbound` 或 `outbound`。

```
GET /airports/SVO/schedules?type=outbound
```

响应：
```json
[
  { "flightNo": "PG0106", "departureTime": "02:40:00", "daysOfWeek": [1,2,3,4,5,6,7], "destination": "Saint Petersburg" },
  { "flightNo": "PG0177", "departureTime": "15:05:00", "daysOfWeek": [1,5],           "destination": "Sochi" }
]
```

---

### GET /flights

搜索两点间可用航班。`origin` 和 `destination` 可以是城市名或机场代码（IATA）。

```
GET /flights?origin=Moscow&destination=Saint+Petersburg&departureDate=2025-12-01&transfers=0
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `origin` | 是 | 出发城市名或机场代码 |
| `destination` | 是 | 目的城市名或机场代码 |
| `departureDate` | 是 | 出发日期，格式 `YYYY-MM-DD` |
| `fareConditions` | 否 | 舱位筛选：`Economy` / `Comfort` / `Business` |
| `transfers` | 否 | 最大中转次数（`0`=仅直飞，不传=最多 3 次中转） |

响应：
```json
[
  {
    "flightNo": "PG0063",
    "scheduledDeparture": "2025-12-01T22:30:00.000Z",
    "scheduledArrival":   "2025-12-01T23:45:00.000Z",
    "scheduledDuration":  { "hours": 1, "minutes": 15 },
    "departureAirportName": "Vnukovo",
    "departureCity":        "Moscow",
    "arrivalAirportName":   "Pulkovo",
    "arrivalCity":          "Saint Petersburg",
    "aircraftModel":        "Aerobus A330-900neo"
  }
]
```

---

### POST /bookings

为单名乘客创建预订，自动生成 `bookRef` 和 `ticketNo`。

请求体：
```json
{
  "flightNo":       "PG0191",
  "flightDate":     "2025-12-01",
  "fareConditions": "Economy",
  "passengerId":    "1234 567890",
  "passengerName":  "IVAN IVANOV"
}
```

成功响应（201）：
```json
{
  "bookRef":        "5EDA47",
  "ticketNo":       "4908b9b9fc684",
  "flightNo":       "PG0191",
  "flightDate":     "2025-12-01",
  "fareConditions": "Economy",
  "passengerId":    "1234 567890",
  "passengerName":  "IVAN IVANOV"
}
```

错误场景：
- `400` — 航班已到达/取消、该舱位无余座、无定价信息
- `404` — 航班不存在

---

### POST /flights/checkins

为已持票乘客办理值机，自动分配座位并生成登机牌。

请求体：
```json
{
  "ticketNo": "4908b9b9fc684",
  "flightId": 11052
}
```

成功响应（201）：
```json
{
  "boardingNo": 1,
  "ticketNo":   "4908b9b9fc684",
  "flightId":   11052,
  "seatNo":     "17A"
}
```

错误场景：
- `400` — 已办理过值机、该舱位无可用座位、票不存在
- `404` — 航班不存在或状态不允许值机

---

## 数据库索引

为保障查询性能，建议执行以下索引（尤其是航班搜索和值机场景）：

```sql
-- 航班时序 join 加速（temporal join 核心索引）
CREATE INDEX idx_flights_route_departure
  ON bookings.flights (route_no, scheduled_departure);

-- 航段查询加速
CREATE INDEX idx_segments_flight_fare
  ON bookings.segments (flight_id, fare_conditions);

-- 登机牌查询加速
CREATE INDEX idx_boarding_passes_flight
  ON bookings.boarding_passes (flight_id);
CREATE INDEX idx_boarding_passes_ticket
  ON bookings.boarding_passes (ticket_no);

-- 座位查询加速
CREATE INDEX idx_seats_airplane_fare
  ON bookings.seats (airplane_code, fare_conditions);

-- airports_data 城市名查询加速（jsonb 表达式索引）
CREATE INDEX idx_airports_city_en
  ON bookings.airports_data ((city->>'en'));

-- 定价规则（Task D4 脚本已创建）
CREATE INDEX idx_pricing_rules_route_no
  ON bookings.pricing_rules (route_no);
```

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 运行时 | Node.js 22 |
| Web 框架 | Express 4 |
| 数据库 | PostgreSQL 13（demo 数据库） |
| 数据库驱动 | node-postgres (pg 8) |
| 入参校验 | Celebrate 15 + Joi |
| 日志 | Winston 3 + express-winston |
| 错误处理 | http-errors 2 |
| ID 生成 | uuid 9 |
| 测试框架 | Jest 29 + Supertest |
| 开发工具 | Nodemon |

---

## 已知限制

- 航班搜索中转查询使用递归 CTE，最大深度为 3 次中转，超大数据集下可能较慢，建议配合索引使用。
- `pricing_rules` 表需先执行 Task D4 脚本生成；若未执行，预订接口会自动回退到从 `segments` 历史数据取价。
- 测试用例依赖数据库中特定航班的状态（`On Time` / `Delayed`），若数据库状态变化需相应更新测试中的 `flightId` 和日期。
