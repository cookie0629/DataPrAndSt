// ============================================================
// 机场相关接口
// ============================================================

const express = require('express');
const { pool } = require('../db');
const { asyncHandler } = require('../asyncHandler');

const router = express.Router();

router.get('/airports', asyncHandler(async (req, res) => {
  const { rows } = await pool.query(`
    SELECT airport_code, airport_name->>'en' AS airport_name, city->>'en' AS city
    FROM bookings.airports_data
    ORDER BY city, airport_code
  `);
  res.json(rows);
}));

router.get('/airports/:code/inbound', asyncHandler(async (req, res) => {
  const { rows } = await pool.query(`
    WITH now_ts AS (
      SELECT MAX(actual_departure) AS ts FROM bookings.flights
    )
    SELECT DISTINCT ON (r.route_no)
      r.route_no,
      r.days_of_week,
      r.scheduled_time::text AS scheduled_time,
      dep.airport_code AS origin_airport,
      dep.airport_name->>'en' AS origin_name,
      dep.city->>'en' AS origin_city
    FROM bookings.routes AS r
    JOIN bookings.airports_data AS dep ON dep.airport_code = r.departure_airport
    WHERE r.arrival_airport = $1
      AND (SELECT ts FROM now_ts) <@ r.validity
    ORDER BY r.route_no
  `, [req.params.code]);
  res.json(rows);
}));

router.get('/airports/:code/outbound', asyncHandler(async (req, res) => {
  const { rows } = await pool.query(`
    WITH now_ts AS (
      SELECT MAX(actual_departure) AS ts FROM bookings.flights
    )
    SELECT DISTINCT ON (r.route_no)
      r.route_no,
      r.days_of_week,
      r.scheduled_time::text AS scheduled_time,
      arr.airport_code AS destination_airport,
      arr.airport_name->>'en' AS destination_name,
      arr.city->>'en' AS destination_city
    FROM bookings.routes AS r
    JOIN bookings.airports_data AS arr ON arr.airport_code = r.arrival_airport
    WHERE r.departure_airport = $1
      AND (SELECT ts FROM now_ts) <@ r.validity
    ORDER BY r.route_no
  `, [req.params.code]);
  res.json(rows);
}));

module.exports = router;
