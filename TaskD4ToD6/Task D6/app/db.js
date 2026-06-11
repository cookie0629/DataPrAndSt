// 数据库连接配置（相当于以前 configs/dbConfig.js 的精简版）
// 使用 pg 库的「连接池」：复用连接，避免每次请求都重新连数据库

const { Pool } = require('pg');

// 创建连接池；环境变量由 docker-compose 注入，本地运行时可手动设置
const pool = new Pool({
  host: process.env.DB_HOST || 'db',           // Docker 里数据库服务名叫 db
  database: process.env.DB_NAME || 'demo',     // PostgresPro 演示库名
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'postgres',
  port: process.env.DB_PORT || 5432,
});

// 导出 pool，各路由文件通过 pool.query() 执行 SQL
module.exports = { pool };
