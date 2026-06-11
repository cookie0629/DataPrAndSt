// ============================================================
// 程序主入口
// ============================================================

const express = require('express');
const { pool } = require('./db');

const app = express();
app.use(express.json());

app.use(require('./routes/index'));
app.use(require('./routes/cities'));
app.use(require('./routes/airports'));
app.use(require('./routes/routes'));
app.use(require('./routes/bookings'));
app.use(require('./routes/checkin'));

// 统一错误处理：返回 JSON，不因单次请求失败而退出进程
app.use((err, req, res, next) => {
  const isMissingTable = err.code === '42P01';
  res.status(err.status || 500).json({
    error: err.message,
    hint: isMissingTable
      ? 'demo 数据库未导入。请先按 Task D6/SETUP.md 导入 PostgresPro demo 数据。'
      : undefined,
  });
});

const port = process.env.PORT || 3000;

async function start() {
  try {
    await pool.query('SELECT 1');
    const { rows } = await pool.query(`
      SELECT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'bookings' AND table_name = 'airports_data'
      ) AS ok
    `);
    if (!rows[0].ok) {
      console.warn('');
      console.warn('【警告】数据库里没有 bookings.airports_data 表！');
      console.warn('  服务可以启动，但接口会报错。请先导入 PostgresPro demo 数据。');
      console.warn('  操作步骤见：Task D6/SETUP.md');
      console.warn('');
    } else {
      console.log('数据库检查通过：bookings.airports_data 已存在');
    }
  } catch (e) {
    console.error('【错误】无法连接数据库:', e.message);
    console.error('  请检查 DB_HOST / DB_PASSWORD 等环境变量，见 Task D6/SETUP.md');
  }

  app.listen(port, '0.0.0.0', () => {
    console.log(`服务已启动: http://localhost:${port}`);
    console.log(`健康检查:   http://localhost:${port}/`);
    console.log(`城市列表:   http://localhost:${port}/cities`);
  });
}

start();
