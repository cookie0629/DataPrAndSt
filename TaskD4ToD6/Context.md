# 航空客运预订系统 (Flights Database) - 后端与数据库开发项目文档

## 1. 项目背景-->https://postgrespro.ru/education/demodb
本项目基于 PostgresPro 提供的航空运输演示数据库（版本：**2025年9月1日版本**，最低支持 PostgreSQL 15）。该数据库模拟了真实的航空客运网络、航班排班、票务预订以及值机流程。

本项目旨在基于该数据库完成底层数据修复、RESTful API 设计以及高并发后端的实现（涵盖 Task D4, D5, D6）。

---

## 2. 数据库版本核心变更（非常重要，请避免使用旧版 Schema）
与旧版相比，当前（2025-09-01）版本的数据模式发生了以下关键变化，**在编写 SQL 查询和业务逻辑时必须严格遵守**：
1. **表名重命名**：
   - `ticket_flights` 已重命名为 `segments`。
   - `aircrafts` 已重命名为 `airplanes`。
2. **字段变更**：
   - 客票价格字段 `amount` 已重命名为 `price`（位于 `segments` 表中）。
   - 取消了 `tickets.contact_info` JSON 字段。
   - 往返票逻辑：新增 `tickets.outbound` (boolean) 区分直达 (`true`) 和返程 (`false`)。
3. **路由逻辑变更 (Temporal Join)**：
   - `routes` 表的路由会随时间变化，主键为时序键 (`route_no` + `validity`)。
   - `flights` 表通过以下条件与 `routes` 连接：`routes.route_no = flights.route_no AND routes.validity @> flights.scheduled_departure`。
4. **多语言支持**：
   - 机场和城市信息现在存储在 `airports_data` 的 `jsonb` 字段中，通过 `bookings.lang()` 视图解析。

---

## 3. 数据库核心模式 (Database Schema)

主要实体及关联关系如下。所有表默认位于 `bookings` schema 下。

### 3.1 核心业务表

* **bookings (预订)**
  * `book_ref` (char 6, PK): 预订编号（6位字母数字）。
  * `book_date` (timestamptz): 预订日期。
  * `total_amount` (numeric 10,2): 预订总金额。

* **tickets (客票)**
  * `ticket_no` (text, PK): 13位唯一客票号。
  * `book_ref` (char 6, FK): 关联预订。
  * `passenger_id` (text): 乘客证件号（含国家代码）。
  * `passenger_name` (text): 乘客姓名。
  * `outbound` (boolean): 是否为去程航班。

* **segments (航段/飞跃，原 ticket_flights)**
  * `ticket_no` (text, PK/FK): 客票号。
  * `flight_id` (integer, PK/FK): 航班 ID。
  * `fare_conditions` (text): 舱位等级 (Economy, Comfort, Business)。
  * `price` (numeric 10,2): 航段价格。

* **boarding_passes (登机牌)**
  * `ticket_no` (text, PK/FK) & `flight_id` (integer, PK/FK): 联合主键，关联 `segments`。
  * `seat_no` (text): 座位号。
  * `boarding_no` (integer): 登机序号（单个航班内唯一）。
  * `boarding_time` (timestamptz): 登机时间。

### 3.2 航班排班与地理表

* **flights (航班)**
  * `flight_id` (integer, PK): 航班代理主键。
  * `route_no` (text): 航线编号。
  * `scheduled_departure` (timestamptz): 计划起飞时间。
  * `scheduled_arrival` (timestamptz): 计划到达时间。
  * `actual_departure` / `actual_arrival` (timestamptz): 实际时间。
  * `status` (text): 状态 (Scheduled, On Time, Delayed, Boarding, Departed, Arrived, Cancelled)。

* **routes (航线)**
  * `route_no` (text): 航线编号。
  * `validity` (tstzrange): 有效期（时间段）。
  * `departure_airport` / `arrival_airport` (char 3, FK): 起降机场代码。
  * `airplane_code` (char 3, FK): 飞机型号。
  * `days_of_week` (integer[]): 执飞班期 (1-7)。

* **timetable (航班时刻表 - 视图)**
  * 隐藏了 `routes` 和 `flights` 的复杂时序 JOIN，建议用于简化时刻表查询。包含本地时间和跨时区计算（如 `scheduled_departure_local`）。

* **airports / airports_data (机场)**
  * `airport_code` (char 3, PK): 机场代码。
  * `airport_name`, `city`, `country`: 机场名、城市、国家（多语言支持）。
  * `coordinates` (point): 坐标。
  * `timezone` (text): 时区。

* **airplanes / airplanes_data (飞机)**
  * `airplane_code` (char 3, PK): 飞机代码。
  * `model` (text/jsonb): 飞机型号。

* **seats (座位布局)**
  * `airplane_code` (char 3, PK/FK).
  * `seat_no` (text, PK): 座位号（如 1A, 24C）。
  * `fare_conditions` (text): 舱位等级 (Economy, Comfort, Business)。

---

## 4. 开发任务描述 (Project Tasks)

Tasks in this block are built upon the Flights database described above.

### Task D4 (Data Engineering)
**Restore the price information for each flight based on the past bookings, and build the pricing rule table that determines the prices for all upcoming flights.**
*要求：使用 SQL 脚本基于历史 `segments` 表中的 `price` 数据，提取并还原每个航线（`route_no`）+ 舱位等级（`fare_conditions`）的标准定价，并建立一张正式的 `pricing_rules` 表，用于为未来尚未售出的航班定价。*

### Task D5 (API Design)
**Design the RESTful web service to handle the following requests:**
*   List all the available source and destination cities
*   List all the available source and destination airports
*   List the airports within a city
*   List the inbound schedule for an airport:
    *   Days of week
    *   Time of arrival
    *   Flight no
    *   Origin (Departure Airport)
*   List the outbound schedule for an airport:
    *   Days of week
    *   Time of departure
    *   Flight no
    *   Destination (Arrival Airport)
*   List the routes connecting two points:
    *   *Point* might be either an airport or a city. In the latter case, we should search for the flights connecting any airports within the city.
    *   The mandatory “departure date” parameter limits the flights by the ones departing between 0:00:00 of the specified date and 0:00:00 of the next date.
    *   The “booking class” parameter should be one of the 'Economy', 'Comfort', 'Business'.
    *   Additional parameter limits the number of connections: 0 (direct), 1, 2, 3, unbound.
*   Create a booking for a selected route for a single passenger
*   Online check-in for a flight (分配座位并生成 boarding_passes)

### Task D6 (API Implementation & Optimization)
**Implement the RESTful web service described above. Consider adding the appropriate indexes to make the requests reasonably fast.**
*要求：使用适当的后端框架（如 Python FastAPI, Java Spring Boot, Node.js 等）实现 D5 中设计的接口。特别需要注意复杂查询（如带中转次数限制的航线图搜索）的 SQL 实现逻辑，并为相关表建立合理的数据库索引（B-Tree, GIN, GiST 等）以保障性能。*