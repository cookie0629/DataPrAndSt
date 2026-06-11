// 在线值机接口
// POST /check-in  请求体：{ "ticket_no": "0005431234567" }
//
// 流程：查是否已值机 → 未值机则按票上每段航班自动分配座位 → 写 boarding_passes

const express = require('express');
const { pool } = require('../db');
const { asyncHandler } = require('../asyncHandler');
const router = express.Router();

router.post('/check-in', asyncHandler(async (req, res) => {
  const { ticket_no } = req.body;

  if (!ticket_no) {
    return res.status(400).json({ error: 'ticket_no is required' });
  }

  const client = await pool.connect();
  try {
    // 1. 先看这张票是不是已经值机过（boarding_passes 里已有记录）
    const { rows: existing } = await client.query(`
      SELECT bp.flight_id, bp.seat_no, bp.boarding_no, bp.boarding_time
      FROM bookings.boarding_passes bp
      WHERE bp.ticket_no = $1
      ORDER BY bp.flight_id
    `, [ticket_no]);

    if (existing.length > 0) {
      // 已值机：返回 200 和已有登机牌信息（幂等：重复请求不会重复占座）
      return res.status(200).json(existing.map((r) => ({
        flightId: r.flight_id,
        seatNo: r.seat_no,
        boardingNo: r.boarding_no,
        boardingTime: r.boarding_time?.toISOString() ?? null,
      })));
    }

    // 2. 从 segments 表查出这张票包含哪些航班、什么舱位
    const { rows: segRows } = await client.query(`
      SELECT s.flight_id, s.fare_conditions
      FROM bookings.segments s
      WHERE s.ticket_no = $1
      ORDER BY s.flight_id
    `, [ticket_no]);

    if (segRows.length === 0) {
      return res.status(200).json([]);  // 票号不存在或没有航段
    }

    await client.query('BEGIN');
    const results = [];

    // 3. 对每一段航班分别分配座位、写登机牌
    for (const seg of segRows) {
      // 3a. 查航班起飞时间和机型（机型在 routes 上，需时效关联）
      const { rows: flightRows } = await client.query(`
        SELECT f.scheduled_departure, r.airplane_code
        FROM bookings.flights f
        JOIN bookings.routes r ON r.route_no = f.route_no
        WHERE f.flight_id = $1
          AND f.scheduled_departure <@ r.validity
        LIMIT 1
      `, [seg.flight_id]);

      if (!flightRows[0]) continue;

      const { scheduled_departure, airplane_code } = flightRows[0];

      // 3b. 在该机型、该舱位下，找第一个「还没被人占」的座位
      const { rows: seatRows } = await client.query(`
        SELECT st.seat_no
        FROM bookings.seats st
        WHERE st.airplane_code = $1
          AND st.fare_conditions = $2
          AND NOT EXISTS (
            SELECT 1
            FROM bookings.boarding_passes bp
            WHERE bp.flight_id = $3
              AND bp.seat_no = st.seat_no
          )
        ORDER BY st.seat_no   -- 按座位号排序，取最小的可用座位
        LIMIT 1
      `, [airplane_code, seg.fare_conditions, seg.flight_id]);

      if (!seatRows[0]) continue;  // 该航班该舱位已满，跳过这一段

      const seatNo = seatRows[0].seat_no;
      // 登机时间 = 起飞前 30 分钟
      const boardingTime = new Date(scheduled_departure.getTime() - 30 * 60 * 1000);

      // 3c. 登机序号 = 该航班当前最大序号 + 1
      const { rows: boardingRows } = await client.query(`
        SELECT coalesce(max(bp.boarding_no), 0) + 1 AS boarding_no
        FROM bookings.boarding_passes bp
        WHERE bp.flight_id = $1
      `, [seg.flight_id]);
      const boardingNo = boardingRows[0].boarding_no;

      // 3d. 写入登机牌表
      await client.query(`
        INSERT INTO bookings.boarding_passes (ticket_no, flight_id, seat_no, boarding_no, boarding_time)
        VALUES ($1, $2, $3, $4, $5)
      `, [ticket_no, seg.flight_id, seatNo, boardingNo, boardingTime]);

      results.push({
        flightId: seg.flight_id,
        seatNo,
        boardingNo,
        boardingTime: boardingTime.toISOString(),
      });
    }

    await client.query('COMMIT');
    res.status(201).json(results);
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

module.exports = router;
