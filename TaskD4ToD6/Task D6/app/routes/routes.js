// 航线搜索（本项目最复杂的接口）
// GET /routes?from=...&to=...&date=...&connections=...&bookingClass=...
//
// 功能：在两点之间找航班，支持直飞 + 中转，支持按城市名或机场三字码查询
// 核心技术：PostgreSQL 递归 CTE（WITH RECURSIVE），像走迷宫一样一层层接下一程

const express = require('express');
const { pool } = require('../db');
const { asyncHandler } = require('../asyncHandler');
const router = express.Router();

const MIN_CONNECTION_MINUTES = 40;  // 两程之间至少间隔 40 分钟（转机来得及）
const MAX_CONNECTION_HOURS = 24;    // 两程之间最多等 24 小时

// 把前端传的 economy/comfort/business 转成数据库里的 Economy/Comfort/Business
function normalizeClass(bc) {
  if (!bc) return null;
  const map = { economy: 'Economy', comfort: 'Comfort', business: 'Business' };
  return map[bc.toLowerCase()] ?? bc;
}

// 判断用户输入的是机场三字码（如 SVO）还是城市名（如 Moscow）
function isAirportCode(s) {
  return /^[A-Z]{3}$/i.test(s) && s.length === 3;
}

router.get('/routes', asyncHandler(async (req, res) => {
  const { from, to, date, connections, bookingClass } = req.query;

  // 必填参数校验
  if (!from || !to || !date) {
    return res.status(400).json({ error: 'Missing params' });
  }

  const fareClass = normalizeClass(bookingClass);
  // connections=unbound 表示最多允许 10 次中转，否则按数字来（0=只要直飞）
  const maxConn = connections === 'unbound' ? 10 : parseInt(connections ?? '0', 10);

  // 把用户选的日期扩成「当天 00:00 ~ 23:59」UTC 时间范围
  const dayStart = `${date} 00:00:00+00`;
  const dayEnd = `${date} 23:59:59+00`;

  // 动态拼 SQL 的 WHERE 条件（起点侧）
  const startConditions = [
    'f.scheduled_departure >= $1',       // 不早于当天开始
    'f.scheduled_departure <= $2',       // 不晚于当天结束
    'f.scheduled_departure <@ r.validity', // 时效关联：航班落在航线有效期内
    "f.status = 'Scheduled'",            // 只查计划中的航班
  ];
  const params = [dayStart, dayEnd];

  // 起点：三字码则比机场，否则比城市名
  if (isAirportCode(from)) {
    params.push(from);
    startConditions.push(`r.departure_airport = $${params.length}`);
  } else {
    params.push(from);
    startConditions.push(`dep_a.city->>'en' = $${params.length}`);
  }

  // 如果指定了舱位，要求该航线机型上确实有这种舱位的座位
  if (fareClass) {
    params.push(fareClass);
    startConditions.push(
      `EXISTS (SELECT 1 FROM bookings.seats s WHERE s.airplane_code = r.airplane_code AND s.fare_conditions = $${params.length})`
    );
  }

  // 终点条件（在最外层 FROM itins 上过滤，这里没有别名 i，直接用列名）
  const finishConditions = [];
  if (isAirportCode(to)) {
    params.push(to);
    finishConditions.push(`arrival_airport = $${params.length}`);
  } else {
    params.push(to);
    finishConditions.push(`arrival_city = $${params.length}`);
  }

  params.push(maxConn);
  const maxConnParam = params.length;

  // ---------- 递归 CTE：从第一程出发，不断「接下一程」，直到到达终点或超过中转次数 ----------
  const sql = `
    WITH RECURSIVE itins AS (
      -- 【锚点】第一程：当天从起点出发的所有符合条件的航班
      SELECT
        f.flight_id,
        r.departure_airport,
        dep_a.city->>'en' AS departure_city,
        f.scheduled_departure,
        r.arrival_airport,
        arr_a.city->>'en' AS arrival_city,
        f.scheduled_arrival,
        0 AS connections,  -- 中转次数，第一程为 0
        ARRAY[dep_a.city->>'en', arr_a.city->>'en']::text[] AS path_cities,     -- 经过的城市，防绕圈
        ARRAY[r.departure_airport::text, r.arrival_airport::text]::text[] AS path_airports,
        ARRAY[f.flight_id::bigint]::bigint[] AS path_flights  -- 本行程包含的航班 ID 列表
      FROM bookings.flights f
      JOIN bookings.routes r ON r.route_no = f.route_no
      JOIN bookings.airports_data dep_a ON dep_a.airport_code = r.departure_airport
      JOIN bookings.airports_data arr_a ON arr_a.airport_code = r.arrival_airport
      WHERE ${startConditions.join(' AND ')}

      UNION ALL

      -- 【递归】在上一程落地机场，找「下一程」能接上的航班
      SELECT
        f2.flight_id,
        i.departure_airport,      -- 整段行程的最初出发机场不变
        i.departure_city,
        i.scheduled_departure,    -- 整段行程的最初出发时间不变
        r2.arrival_airport,
        arr_a2.city->>'en' AS arrival_city,
        f2.scheduled_arrival,
        i.connections + 1,
        (i.path_cities || (arr_a2.city->>'en'))::text[],
        (i.path_airports || r2.arrival_airport::text)::text[],
        (i.path_flights || f2.flight_id::bigint)::bigint[]
      FROM itins i
      JOIN bookings.routes r2 ON r2.departure_airport = i.arrival_airport  -- 从上一程落地机场出发
      JOIN bookings.airports_data arr_a2 ON arr_a2.airport_code = r2.arrival_airport  -- 必须先 JOIN，后面才能用 arr_a2
      JOIN bookings.flights f2
        ON f2.route_no = r2.route_no
        AND f2.scheduled_departure <@ r2.validity
        AND f2.scheduled_departure >= i.scheduled_arrival + INTERVAL '${MIN_CONNECTION_MINUTES} minutes'
        AND f2.scheduled_departure <= i.scheduled_arrival + INTERVAL '${MAX_CONNECTION_HOURS} hours'
        AND f2.status = 'Scheduled'
        AND NOT (r2.arrival_airport::text = ANY(i.path_airports))  -- 不能重复经过同一机场
      WHERE i.connections + 1 <= $${maxConnParam}  -- 中转次数不能超过用户设定的上限
    )
    SELECT connections, path_flights
    FROM itins
    WHERE ${finishConditions.join(' AND ')}  -- 只保留「最终到达用户目的地」的行程
    ORDER BY connections, scheduled_departure   -- 优先直飞，同中转数按出发时间排
  `;

  const { rows } = await pool.query(sql, params);
  if (rows.length === 0) return res.json([]);

  // 递归只返回 flight_id 数组，再查一次拿到每段的详细时间、机场等信息
  const allFlightIds = [...new Set(rows.flatMap((r) => r.path_flights))];
  const { rows: flightDetails } = await pool.query(`
    SELECT DISTINCT ON (f.flight_id)
      f.flight_id, f.route_no, r.departure_airport, r.arrival_airport,
      f.scheduled_departure, f.scheduled_arrival
    FROM bookings.flights f
    JOIN bookings.routes r
      ON r.route_no = f.route_no AND f.scheduled_departure <@ r.validity
    WHERE f.flight_id = ANY($1)
    ORDER BY f.flight_id
  `, [allFlightIds]);

  const flightMap = Object.fromEntries(flightDetails.map((f) => [f.flight_id, f]));

  // 组装成 api.yaml 里定义的 JSON 格式返回给前端
  const result = rows.map((row) => ({
    connectionsCount: row.connections,
    segments: row.path_flights.map((id) => {
      const f = flightMap[id];
      return {
        routeNo: f.route_no,
        departureAirportCode: f.departure_airport,
        arrivalAirportCode: f.arrival_airport,
        departureTime: f.scheduled_departure.toISOString(),
        arrivalTime: f.scheduled_arrival.toISOString(),
      };
    }),
  }));

  res.json(result);
}));

module.exports = router;
