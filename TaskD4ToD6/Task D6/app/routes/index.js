// 根路径健康检查：确认服务已启动（浏览器访问 http://localhost:3000/ 应看到 Hello world）

const express = require('express');
const router = express.Router();

router.get('/', (req, res) => {
  res.send('你看看是不是这里');
});

module.exports = router;
