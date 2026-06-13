// 机场相关接口：进港 / 出港时刻表支持按地点、时间段、条数限制筛选

const express = require('express');
const { pool } = require('../db');
const { asyncHandler } = require('../asyncHandler');

const router = express.Router();

const TIME_RE = /^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$/;

function isAirportCode(s) {
  return typeof s === 'string' && /^[A-Z]{3}$/i.test(s) && s.length === 3;
}

function parseLimit(raw) {
  const n = parseInt(raw ?? '10', 10);
  if (Number.isNaN(n) || n < 1) return 10;
  return Math.min(n, 100);
}

// 拼装可选筛选条件（地点 / 时间段 / LIMIT）
function buildScheduleQuery({ side, airportCode, placeFilter, time_from, time_to, limit }) {
  const params = [airportCode];
  const filters = [];

  if (placeFilter) {
    if (isAirportCode(placeFilter)) {
      params.push(placeFilter.toUpperCase());
      if (side === 'outbound') {
        filters.push(`r.arrival_airport = $${params.length}`);
      } else {
        filters.push(`r.departure_airport = $${params.length}`);
      }
    } else {
      params.push(placeFilter);
      if (side === 'outbound') {
        filters.push(`arr.city->>'en' = $${params.length}`);
      } else {
        filters.push(`dep.city->>'en' = $${params.length}`);
      }
    }
  }

  if (time_from) {
    if (!TIME_RE.test(time_from)) {
      const err = new Error('time_from must be HH:MM:SS');
      err.status = 400;
      throw err;
    }
    params.push(time_from);
    filters.push(`r.scheduled_time >= $${params.length}::time`);
  }

  if (time_to) {
    if (!TIME_RE.test(time_to)) {
      const err = new Error('time_to must be HH:MM:SS');
      err.status = 400;
      throw err;
    }
    params.push(time_to);
    filters.push(`r.scheduled_time <= $${params.length}::time`);
  }

  params.push(limit);
  const limitParam = params.length;

  const airportCond = side === 'outbound'
    ? 'r.departure_airport = $1'
    : 'r.arrival_airport = $1';

  const joinPeer = side === 'outbound'
    ? 'JOIN bookings.airports_data AS arr ON arr.airport_code = r.arrival_airport'
    : 'JOIN bookings.airports_data AS dep ON dep.airport_code = r.departure_airport';

  const selectCols = side === 'outbound'
    ? `r.route_no,
       r.days_of_week,
       r.scheduled_time::text AS scheduled_time,
       arr.airport_code AS destination_airport,
       arr.airport_name->>'en' AS destination_name,
       arr.city->>'en' AS destination_city`
    : `r.route_no,
       r.days_of_week,
       r.scheduled_time::text AS scheduled_time,
       dep.airport_code AS origin_airport,
       dep.airport_name->>'en' AS origin_name,
       dep.city->>'en' AS origin_city`;

  const filterSql = filters.length ? `AND ${filters.join(' AND ')}` : '';

  const sql = `
    WITH now_ts AS (
      SELECT MAX(actual_departure) AS ts FROM bookings.flights
    )
    SELECT * FROM (
      SELECT DISTINCT ON (r.route_no)
        ${selectCols}
      FROM bookings.routes AS r
      ${joinPeer}
      WHERE ${airportCond}
        AND (SELECT ts FROM now_ts) <@ r.validity
        ${filterSql}
      ORDER BY r.route_no
    ) AS routes
    ORDER BY scheduled_time, route_no
    LIMIT $${limitParam}
  `;

  return { sql, params };
}

router.get('/airports', asyncHandler(async (req, res) => {
  const { rows } = await pool.query(`
    SELECT airport_code, airport_name->>'en' AS airport_name, city->>'en' AS city
    FROM bookings.airports_data
    ORDER BY city, airport_code
  `);
  res.json(rows);
}));

router.get('/airports/:code/inbound', asyncHandler(async (req, res) => {
  const { origin, time_from, time_to, limit: limitRaw } = req.query;
  const { sql, params } = buildScheduleQuery({
    side: 'inbound',
    airportCode: req.params.code,
    placeFilter: origin,
    time_from,
    time_to,
    limit: parseLimit(limitRaw),
  });
  const { rows } = await pool.query(sql, params);
  res.json(rows);
}));

router.get('/airports/:code/outbound', asyncHandler(async (req, res) => {
  const { destination, time_from, time_to, limit: limitRaw } = req.query;
  const { sql, params } = buildScheduleQuery({
    side: 'outbound',
    airportCode: req.params.code,
    placeFilter: destination,
    time_from,
    time_to,
    limit: parseLimit(limitRaw),
  });
  const { rows } = await pool.query(sql, params);
  res.json(rows);
}));

module.exports = router;
