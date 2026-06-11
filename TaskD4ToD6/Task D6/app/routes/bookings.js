// 订票接口
// POST /bookings
//
// 流程：解析每段行程 → 算总价 → 写 bookings / tickets / segments 三张表
// 全程在一个数据库事务里，要么全成功要么全回滚

const express = require('express');
const { pool } = require('../db');
const { asyncHandler } = require('../asyncHandler');
const router = express.Router();

// 与 D4 pricing_rules 一致的「每分钟单价」
const RATE = { Economy: 50, Comfort: 65, Business: 100 };

// 生成 6 位预订号（字母+数字）
function genBookRef() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  return Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

// 生成 13 位票号：固定前缀 000543 + 7 位随机数字
function genTicketNo() {
  const digits = Array.from({ length: 7 }, () => Math.floor(Math.random() * 10)).join('');
  return '000543' + digits;
}

// 根据前端传来的一段行程信息，在数据库里找到对应的 flight_id
async function resolveFlightId(client, seg) {
  const { rows } = await client.query(`
    SELECT f.flight_id
    FROM bookings.flights f
    JOIN bookings.routes r
      ON r.route_no = f.route_no
      AND r.departure_airport = $2   -- 出发机场要对上
      AND r.arrival_airport = $3     -- 到达机场要对上
    WHERE f.route_no = $1
      AND f.scheduled_departure = $4  -- 出发时间要精确匹配
    LIMIT 1
  `, [seg.routeNo, seg.departureAirportCode, seg.arrivalAirportCode, seg.departureTime]);

  if (!rows[0]) {
    const err = new Error(`Flight not found for segment: ${seg.routeNo} at ${seg.departureTime}`);
    err.status = 400;
    throw err;
  }
  return rows[0].flight_id;
}

router.post('/bookings', asyncHandler(async (req, res) => {
  const { passenger_id, passenger_name, fare_condition = 'Economy', outbound = true, segments } = req.body;

  if (!segments || segments.length === 0) {
    return res.status(400).json({ error: 'segments must be non-empty' });
  }

  // 从连接池拿一条专用连接，用于事务（BEGIN/COMMIT 必须在同一连接上）
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 1. 把每一段行程都解析成 flight_id
    const flightIds = [];
    for (const seg of segments) {
      flightIds.push(await resolveFlightId(client, seg));
    }

    // 2. 按「飞行分钟数 × 舱位单价」计算订单总价
    const rate = RATE[fare_condition] ?? 50;
    const { rows: priceRows } = await client.query(`
      SELECT COALESCE(SUM(
        EXTRACT(EPOCH FROM (f.scheduled_arrival - f.scheduled_departure)) / 60.0 * $1
      ), 0)::numeric(10,2) AS total
      FROM bookings.flights f
      WHERE f.flight_id = ANY($2)
    `, [rate, flightIds]);
    const totalAmount = priceRows[0].total;

    // 3. 生成不重复的预订号（最多试 50 次）
    let bookRef;
    for (let i = 0; i < 50; i++) {
      bookRef = genBookRef();
      const { rows } = await client.query(
        'SELECT 1 FROM bookings.bookings WHERE book_ref = $1 LIMIT 1', [bookRef]
      );
      if (!rows[0]) break;
    }

    // 4. 生成不重复的票号
    let ticketNo;
    for (let i = 0; i < 50; i++) {
      ticketNo = genTicketNo();
      const { rows } = await client.query(
        'SELECT 1 FROM bookings.tickets WHERE ticket_no = $1 LIMIT 1', [ticketNo]
      );
      if (!rows[0]) break;
    }

    // 5. 写入订单主表
    await client.query(`
      INSERT INTO bookings.bookings (book_ref, book_date, total_amount)
      VALUES ($1, now(), $2)
    `, [bookRef, totalAmount]);

    // 6. 写入机票表（乘客信息、去程/返程标记）
    await client.query(`
      INSERT INTO bookings.tickets (ticket_no, book_ref, passenger_id, passenger_name, outbound)
      VALUES ($1, $2, $3, $4, $5)
    `, [ticketNo, bookRef, passenger_id, passenger_name, outbound]);

    // 7. 每一段航班写一条 segments 记录（票价 = 该段飞行分钟数 × 单价）
    for (const flightId of flightIds) {
      const { rows: dur } = await client.query(`
        SELECT (EXTRACT(EPOCH FROM (scheduled_arrival - scheduled_departure)) / 60.0 * $1)::numeric(10,2) AS price
        FROM bookings.flights WHERE flight_id = $2
      `, [rate, flightId]);
      await client.query(`
        INSERT INTO bookings.segments (ticket_no, flight_id, fare_conditions, price)
        VALUES ($1, $2, $3, $4)
      `, [ticketNo, flightId, fare_condition, dur[0].price]);
    }

    await client.query('COMMIT');
    res.status(201).json({ book_ref: bookRef, ticket_no: ticketNo });
  } catch (err) {
    await client.query('ROLLBACK');
    err.status = err.status || 500;
    throw err;
  } finally {
    client.release();
  }
}));

module.exports = router;
