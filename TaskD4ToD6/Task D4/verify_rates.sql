-- ============================================================
-- 验证：50 / 65 / 100 从何而来？
-- 从历史 segments 反推「每分钟单价」，应与 CASE 中的三个数一致
-- 执行：psql -U postgres -d demo -f "Task D4/verify_rates.sql"
-- ============================================================

-- 1. 按舱位统计历史「每分钟单价」的均值（应约为 50 / 65 / 100）
SELECT
    s.fare_conditions,
    COUNT(*) AS sample_count,
    ROUND(AVG(
        s.price / NULLIF(EXTRACT(EPOCH FROM (f.scheduled_arrival - f.scheduled_departure)) / 60, 0)
    )::numeric, 2) AS avg_price_per_minute
FROM bookings.segments s
JOIN bookings.flights f ON f.flight_id = s.flight_id
WHERE f.status = 'Arrived'
  AND f.scheduled_arrival > f.scheduled_departure
GROUP BY s.fare_conditions
ORDER BY s.fare_conditions;

-- 2. 用线性模型回代，看与历史票价的最大误差（应全为 0）
SELECT
    s.fare_conditions,
    COUNT(*) AS sample_count,
    ROUND(MAX(ABS(
        s.price - CASE s.fare_conditions
            WHEN 'Economy'  THEN 50
            WHEN 'Comfort'  THEN 65
            WHEN 'Business' THEN 100
        END * EXTRACT(EPOCH FROM (f.scheduled_arrival - f.scheduled_departure)) / 60
    ))::numeric, 4) AS max_abs_error
FROM bookings.segments s
JOIN bookings.flights f ON f.flight_id = s.flight_id
WHERE f.status = 'Arrived'
  AND f.scheduled_arrival > f.scheduled_departure
GROUP BY s.fare_conditions
ORDER BY s.fare_conditions;
