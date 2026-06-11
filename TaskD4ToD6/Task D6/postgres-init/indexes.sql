-- ============================================================
-- 数据库索引初始化脚本
-- 作用：给经常用来「查条件」的字段建索引，让 SQL 查询更快
-- 时机：Docker 首次启动 Postgres 时会自动执行（挂载在 docker-entrypoint-initdb.d）
--       本地库也可手动：psql -U postgres -d demo -f postgres-init/indexes.sql
-- ============================================================

-- 按城市查机场时用（city 是 jsonb，但整列索引仍能加速部分查询）
CREATE INDEX IF NOT EXISTS idx_airports_city ON bookings.airports_data(city);

-- 查「从某机场出发」的航线时用
CREATE INDEX IF NOT EXISTS idx_routes_departure_airport ON bookings.routes(departure_airport);

-- 查「到达某机场」的航线时用
CREATE INDEX IF NOT EXISTS idx_routes_arrival_airport ON bookings.routes(arrival_airport);

-- 按出发日期/时间筛航班时用（航线搜索的核心条件之一）
CREATE INDEX IF NOT EXISTS idx_flights_departure_time ON bookings.flights(scheduled_departure);

-- 值机时按航班查已占座位、分配登机序号时用
CREATE INDEX IF NOT EXISTS idx_boarding_passes_flight_id ON bookings.boarding_passes(flight_id);

-- 值机时按票号查是否已值机、是否已有登机牌时用
CREATE INDEX IF NOT EXISTS idx_boarding_passes_ticket ON bookings.boarding_passes(ticket_no);
