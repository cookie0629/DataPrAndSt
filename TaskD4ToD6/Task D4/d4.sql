-- Task D4 - d4.sql
-- 作用：验证定价模型是否合理——对比「按规则算出来的价」和「数据库里实际存的价」
-- 前提：先执行 price.sql 建好 pricing_rules 表
-- 执行：psql -U postgres -d demo -f "Task D4/d4.sql"

SELECT DISTINCT
    r.route_no,              -- 航线号
    s.fare_conditions,       -- 舱位
    r.duration,              -- 航线计划飞行时长

    -- 计算价 = 每分钟单价 × 飞行分钟数（EPOCH 是秒，除以60变分钟）
    (pr.base_price * EXTRACT(EPOCH FROM r.duration) / 60)::numeric(10,2) as calculated_price,
    s.price as actual_price  -- 数据库里 segments 表记录的实际票价

FROM bookings.routes r
-- 同样使用时效关联：航班起飞时间必须在航线 validity 范围内
JOIN bookings.flights f ON r.route_no = f.route_no
              AND r.validity @> f.scheduled_departure
JOIN bookings.segments s ON f.flight_id = s.flight_id
-- 关联定价规则表，按航线号+舱位匹配
JOIN bookings.pricing_rules pr ON s.fare_conditions = pr.fare_conditions
        AND r.route_no = pr.route_no
WHERE f.status = 'Scheduled'  -- 看尚未起飞、还在计划中的航班
ORDER BY r.route_no, s.fare_conditions, r.duration
LIMIT 100;
