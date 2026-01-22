# CyberLab 后端API服务

## 🚀 快速启动

### 方法1：使用启动脚本（推荐）
```bash
cd server
./start.sh
```

### 方法2：手动启动
```bash
cd server
npm install
npm run dev
```

## 📋 API接口列表

### 成果管理
- `GET /api/achievements/admin/list` - 获取成果列表
- `GET /api/achievements/statistics` - 获取统计数据
- `GET /api/achievements/:id` - 获取成果详情
- `PUT /api/achievements/admin/approve/:id` - 审批通过
- `PUT /api/achievements/admin/reject/:id` - 审批驳回

### 演练管理
- `GET /api/drills/active` - 获取活跃演练
- `GET /api/drills/containers` - 获取所有演练

### 成果提交
- `POST /api/achievements/red-team/submit` - 红队成果提交
- `POST /api/achievements/blue-team/submit` - 蓝队成果提交

### 拓扑图管理
- `GET /api/topology/load` - 加载拓扑图
- `POST /api/topology/save` - 保存拓扑图

### 系统
- `GET /api/health` - 健康检查

## 🔧 配置说明

- 服务端口：3000
- 前端API地址已更新为：`http://localhost:3000/api`
- 文件上传目录：`uploads/`

## 📝 注意事项

1. 确保端口3000没有被其他服务占用
2. 前端开发服务器运行在5173端口
3. 所有API都支持CORS跨域请求
4. 文件上传功能已配置multer中间件