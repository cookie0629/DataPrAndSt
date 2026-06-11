// ============================================================
// 城市相关接口
// GET /cities              → 返回所有城市名列表
// GET /cities/:city/airports → 返回某城市下的机场
// ============================================================

const express = require('express');
const { pool } = require('../db');
const { asyncHandler } = require('../asyncHandler');

const router = express.Router();

router.get('/cities', asyncHandler(async (req, res) => {
  const { rows } = await pool.query(`
    SELECT DISTINCT city->>'en' AS city
    FROM bookings.airports_data
    WHERE city->>'en' IS NOT NULL
    ORDER BY city
  `);
  res.json(rows.map((r) => r.city));
}));

router.get('/cities/:city/airports', asyncHandler(async (req, res) => {
  const { rows } = await pool.query(`
    SELECT airport_code, airport_name->>'en' AS airport_name
    FROM bookings.airports_data
    WHERE city->>'en' = $1
    ORDER BY airport_code
  `, [req.params.city]);
  res.json(rows);
}));

module.exports = router;
