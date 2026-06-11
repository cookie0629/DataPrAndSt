// 包装 async 路由：把错误交给 Express 统一处理，避免一次查询失败就整个服务崩溃

function asyncHandler(fn) {
  return (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

module.exports = { asyncHandler };
