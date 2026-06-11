-- Task D4 - price.sql
-- 作用：创建「定价规则表」pricing_rules，并用历史票价数据填充
-- 执行：psql -U postgres -d demo -f "Task D4/price.sql"

DROP TABLE IF EXISTS bookings.pricing_rules CASCADE;

-- 创建定价规则表：每一行 = 某条航线 + 某种舱位 的定价信息
CREATE TABLE bookings.pricing_rules (
    route_no text NOT NULL,           -- 航线编号，如 PG0063
    fare_conditions text NOT NULL,    -- 舱位等级：Economy / Comfort / Business
    base_price numeric(10,2) NOT NULL, -- 每分钟单价（订票时用来算总价）
    min_price numeric(10,2) NOT NULL,  -- 历史上该航线该舱位出现过的最低票价
    max_price numeric(10,2) NOT NULL,  -- 历史上该航线该舱位出现过的最高票价
    sample_count integer NOT NULL,     -- 参与统计的历史航段条数
    last_updated timestamp with time zone DEFAULT now(), -- 记录更新时间
    PRIMARY KEY (route_no, fare_conditions)  -- 主键：同一条航线+同一舱位只能有一行
);

-- 从历史已到达（Arrived）的航班里，统计各航线各舱位的票价范围，并写入上表
INSERT INTO bookings.pricing_rules (route_no, fare_conditions, base_price, min_price, max_price, sample_count)
SELECT
    r.route_no,
    s.fare_conditions,
    -- 按舱位设定「每分钟单价」：经济舱50、舒适舱65、商务舱100（与 D6 订票逻辑一致）
    CASE s.fare_conditions
        WHEN 'Economy' THEN 50
        WHEN 'Comfort' THEN 65
        WHEN 'Business' THEN 100
    END AS base_price,
    MIN(s.price) AS min_price,   -- 该组合下所有历史票价的最小值
    MAX(s.price) AS max_price,   -- 该组合下所有历史票价的最大值
    COUNT(*) AS sample_count     -- 一共统计了多少条航段记录
FROM bookings.routes r
-- 关联航班：航线号相同，且航班起飞时间落在该航线版本的有效期内（时效关联，新版库的关键写法）
JOIN bookings.flights f ON r.route_no = f.route_no
    AND r.validity @> f.scheduled_departure
-- 关联航段：拿到每张票在该航班上的实际售价
JOIN bookings.segments s ON f.flight_id = s.flight_id
WHERE f.status = 'Arrived'  -- 只统计已经飞完的航班，数据更可靠
GROUP BY r.route_no, s.fare_conditions;

-- 给 route_no 建索引：以后按航线查定价会更快
CREATE INDEX idx_pricing_rules_route_no ON bookings.pricing_rules (route_no);
