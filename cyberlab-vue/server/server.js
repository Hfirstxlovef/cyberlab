const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');

const app = express();
const PORT = 8080;

// MySQL数据库配置（使用Spring Boot相同的配置）
const dbConfig = {
  host: 'localhost',
  port: 3306,
  user: 'root',
  password: 'sunbuyang123',
  database: 'cyberlab',
  charset: 'utf8mb4'
};

// 创建数据库连接池
const pool = mysql.createPool(dbConfig);

// 确保uploads目录存在
const uploadDir = 'uploads';
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// 中间件
app.use(cors({
  origin: ['http://localhost:5173', 'http://127.0.0.1:5173'], // Vite开发服务器地址
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With']
}));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// 文件上传配置
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, 'uploads/');
  },
  filename: (req, file, cb) => {
    cb(null, Date.now() + '-' + file.originalname);
  }
});
const upload = multer({ storage });

// JWT密钥
const JWT_SECRET = 'cyberlab_jwt_secret_key_2024';

// 模拟用户数据
const users = [
  {
    id: 1,
    username: 'admin',
    password: bcrypt.hashSync('admin123', 10), // 密码: admin123
    role: 'admin',
    email: 'admin@cyberlab.com',
    enabled: true
  },
  {
    id: 2,
    username: 'user',
    password: bcrypt.hashSync('user123', 10), // 密码: user123
    role: 'user',
    email: 'user@cyberlab.com',
    enabled: true
  },
  {
    id: 3,
    username: 'teacher',
    password: bcrypt.hashSync('teacher123', 10), // 密码: teacher123
    role: 'teacher',
    email: 'teacher@cyberlab.com',
    enabled: true
  },
  {
    id: 4,
    username: 'blue',
    password: bcrypt.hashSync('blue123', 10), // 密码: blue123
    role: 'blue',
    email: 'blue@cyberlab.com',
    enabled: true
  },
  {
    id: 5,
    username: 'red',
    password: bcrypt.hashSync('red123', 10), // 密码: red123
    role: 'red',
    email: 'red@cyberlab.com',
    enabled: true
  },
  {
    id: 6,
    username: 'judge',
    password: bcrypt.hashSync('judge123', 10), // 密码: judge123
    role: 'judge',
    email: 'judge@cyberlab.com',
    enabled: true
  }
];

// JWT验证中间件
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (!token) {
    return res.status(401).json({ 
      success: false, 
      message: '缺少访问令牌' 
    });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ 
        success: false, 
        message: '令牌无效或已过期' 
      });
    }
    req.user = user;
    next();
  });
};

// 角色权限验证中间件
const requireRole = (allowedRoles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        code: 'AUTHENTICATION_REQUIRED',
        message: '需要登录才能访问此资源'
      });
    }

    const userRole = req.user.role;
    
    // 检查用户角色是否在允许的角色列表中
    if (!allowedRoles.includes(userRole)) {
      console.log(`权限不足: 用户角色 ${userRole}, 需要角色: ${allowedRoles.join(', ')}`);
      
      return res.status(403).json({
        success: false,
        code: 'ACCESS_DENIED',
        message: `权限不足。您没有权限访问此功能，需要以下角色之一：${allowedRoles.join(', ')}`,
        requiredRoles: allowedRoles,
        currentRole: userRole
      });
    }

    next();
  };
};

// 模拟数据
let achievements = [
  {
    id: 1,
    type: 'red_team',
    teamName: '红队Alpha',
    drillName: '春季攻防演练',
    targetInfo: '目标系统：Web应用服务器',
    vulnerabilities: '发现SQL注入漏洞',
    cvssScore: 'high',
    status: 'pending',
    submittedAt: '2024-01-15T10:30:00Z',
    reviewedAt: null,
    reviewerId: null,
    reviewComments: null
  },
  {
    id: 2,
    type: 'blue_team',
    teamName: '蓝队Beta',
    drillName: '春季攻防演练',
    incidentType: 'malware',
    detectionMethod: '基于行为分析的检测',
    responseTime: 15,
    status: 'approved',
    submittedAt: '2024-01-14T14:20:00Z',
    reviewedAt: '2024-01-15T09:15:00Z',
    reviewerId: 'admin',
    reviewComments: '检测及时，响应迅速'
  }
];

let drills = [
  { id: 1, name: '春季攻防演练', status: 'active' },
  { id: 2, name: '夏季红蓝对抗', status: 'active' },
  { id: 3, name: '秋季实战演习', status: 'planned' },
  { id: 4, name: '冬季安全评估', status: 'planned' }
];

let topologyData = {
  projectId: 'default',
  nodes: [
    {
      id: 'node-1',
      name: '防火墙',
      iconName: 'firewall',
      x: 100,
      y: 100,
      type: 'security'
    },
    {
      id: 'node-2', 
      name: 'Web服务器',
      iconName: 'webserver',
      x: 300,
      y: 100,
      type: 'server'
    },
    {
      id: 'node-3',
      name: '数据库',
      iconName: 'database', 
      x: 500,
      y: 100,
      type: 'database'
    },
    {
      id: 'node-4',
      name: '用户终端',
      iconName: 'pc',
      x: 200,
      y: 250,
      type: 'endpoint'
    }
  ],
  links: [
    { source: 'node-1', target: 'node-2', label: '允许HTTP' },
    { source: 'node-2', target: 'node-3', label: '数据库连接' },
    { source: 'node-4', target: 'node-1', label: '网络访问' },
    { source: 'node-4', target: 'node-2', label: '直接访问' }
  ],
  customElements: []
};

// 数据库初始化 - 创建系统设置表
const initDatabase = async () => {
  try {
    const connection = await pool.getConnection();
    
    // 创建系统设置表（如果不存在）
    await connection.execute(`
      CREATE TABLE IF NOT EXISTS system_settings (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        setting_key VARCHAR(255) NOT NULL UNIQUE,
        setting_value LONGTEXT,
        setting_type VARCHAR(50),
        description VARCHAR(500),
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    `);
    
    console.log('✅ 数据库表初始化完成');
    connection.release();
  } catch (error) {
    console.error('❌ 数据库初始化失败:', error);
  }
};

// 系统设置数据库操作
const systemSettingsDB = {
  // 获取所有设置
  async getAllSettings() {
    try {
      const [rows] = await pool.execute('SELECT setting_key, setting_value FROM system_settings');
      const settings = {};
      rows.forEach(row => {
        settings[row.setting_key] = row.setting_value;
      });
      
      // 设置默认值
      const defaults = {
        system_logo: '',
        login_title: 'CyberLab网络安全实验平台',
        sidebar_title: 'CyberLab',
        serial_number: 'CYBERLAB-2024-PROD-001',
        license_code: 'CL2024PROD1234567890ABCDEF'
      };
      
      Object.entries(defaults).forEach(([key, defaultValue]) => {
        if (settings[key] === undefined) {
          settings[key] = defaultValue;
        }
      });
      
      return settings;
    } catch (error) {
      console.error('获取系统设置失败:', error);
      throw error;
    }
  },

  // 保存单个设置
  async saveSetting(key, value, type = 'text', description = '') {
    try {
      await pool.execute(`
        INSERT INTO system_settings (setting_key, setting_value, setting_type, description) 
        VALUES (?, ?, ?, ?) 
        ON DUPLICATE KEY UPDATE 
        setting_value = VALUES(setting_value), 
        updated_at = CURRENT_TIMESTAMP
      `, [key, value, type, description]);
      
      console.log(`✅ 设置已保存: ${key} = ${value}`);
    } catch (error) {
      console.error('保存设置失败:', error);
      throw error;
    }
  },

  // 批量保存设置
  async saveSettings(settings) {
    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();
      
      for (const [key, value] of Object.entries(settings)) {
        const type = this.getSettingType(key);
        const description = this.getSettingDescription(key);
        
        await connection.execute(`
          INSERT INTO system_settings (setting_key, setting_value, setting_type, description) 
          VALUES (?, ?, ?, ?) 
          ON DUPLICATE KEY UPDATE 
          setting_value = VALUES(setting_value), 
          updated_at = CURRENT_TIMESTAMP
        `, [key, value, type, description]);
      }
      
      await connection.commit();
      console.log('✅ 批量设置已保存:', Object.keys(settings));
    } catch (error) {
      await connection.rollback();
      console.error('批量保存设置失败:', error);
      throw error;
    } finally {
      connection.release();
    }
  },

  getSettingType(key) {
    switch (key) {
      case 'system_logo': return 'image';
      case 'serial_number':
      case 'license_code': return 'code';
      default: return 'text';
    }
  },

  getSettingDescription(key) {
    switch (key) {
      case 'system_logo': return '系统Logo图片';
      case 'login_title': return '登录页面标题';
      case 'sidebar_title': return '侧边栏标题';
      case 'serial_number': return '产品序列号';
      case 'license_code': return '产品授权码';
      default: return '';
    }
  }
};

// API路由

// 认证相关API
app.post('/api/auth/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({
        success: false,
        message: '用户名和密码不能为空'
      });
    }

    // 查找用户
    const user = users.find(u => u.username === username);
    if (!user) {
      return res.status(401).json({
        success: false,
        message: '用户名或密码错误'
      });
    }

    // 验证密码
    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: '用户名或密码错误'
      });
    }

    // 生成JWT令牌
    const token = jwt.sign(
      { 
        sub: user.username,
        authorities: [`ROLE_${user.role}`],
        id: user.id,
        username: user.username,
        role: user.role
      },
      JWT_SECRET,
      { expiresIn: '24h' }
    );

    console.log(`用户 ${username} 登录成功`);

    res.json({
      success: true,
      message: '登录成功',
      token,
      username: user.username,
      role: user.role
    });

  } catch (error) {
    console.error('登录失败:', error);
    res.status(500).json({
      success: false,
      message: '服务器内部错误'
    });
  }
});

app.post('/api/auth/logout', authenticateToken, (req, res) => {
  console.log(`用户 ${req.user.username} 退出登录`);
  res.json({
    success: true,
    message: '退出登录成功'
  });
});

app.get('/api/auth/me', authenticateToken, (req, res) => {
  const user = users.find(u => u.username === req.user.username);
  if (!user) {
    return res.status(404).json({
      success: false,
      message: '用户不存在'
    });
  }

  res.json({
    success: true,
    data: {
      id: user.id,
      username: user.username,
      role: user.role,
      email: user.email
    }
  });
});

// 用户管理相关API
// 获取用户列表（管理员专用）
app.get('/api/admin/users', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const { role } = req.query;
    console.log(`管理员 ${req.user.username} 获取用户列表，角色筛选: ${role || '全部'}`);

    let filteredUsers = users;
    if (role && role !== '') {
      filteredUsers = users.filter(u => u.role === role);
    }

    // 返回用户列表（不包含密码）
    const userList = filteredUsers.map(user => ({
      id: user.id,
      username: user.username,
      role: user.role,
      email: user.email,
      enabled: user.enabled
    }));

    console.log(`返回 ${userList.length} 个用户`);
    res.json(userList);
  } catch (error) {
    console.error('获取用户列表失败:', error);
    res.status(500).json({
      success: false,
      message: '获取用户列表失败'
    });
  }
});

// 创建用户（管理员专用）
app.post('/api/admin/users', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const { username, password, role } = req.body;
    console.log(`管理员 ${req.user.username} 创建用户: ${username} (${role})`);

    // 检查用户名是否已存在
    if (users.find(u => u.username === username)) {
      return res.status(400).json({
        success: false,
        message: '用户名已存在'
      });
    }

    // 生成新用户ID
    const newUserId = Math.max(...users.map(u => u.id), 0) + 1;

    // 创建新用户
    const newUser = {
      id: newUserId,
      username,
      password: bcrypt.hashSync(password, 10),
      role,
      email: `${username}@cyberlab.com`,
      enabled: true
    };

    users.push(newUser);

    console.log(`用户创建成功: ${username} (ID: ${newUserId})`);
    res.status(201).json({
      success: true,
      message: '用户创建成功',
      data: {
        id: newUser.id,
        username: newUser.username,
        role: newUser.role,
        email: newUser.email,
        enabled: newUser.enabled
      }
    });
  } catch (error) {
    console.error('创建用户失败:', error);
    res.status(500).json({
      success: false,
      message: '创建用户失败'
    });
  }
});

// 更新用户（管理员专用）
app.put('/api/admin/users/:id', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const userId = parseInt(req.params.id);
    const { username, role, enabled } = req.body;
    console.log(`管理员 ${req.user.username} 更新用户 ID: ${userId}`);

    const userIndex = users.findIndex(u => u.id === userId);
    if (userIndex === -1) {
      return res.status(404).json({
        success: false,
        message: '用户不存在'
      });
    }

    // 检查用户名是否与其他用户冲突
    if (username && users.find(u => u.username === username && u.id !== userId)) {
      return res.status(400).json({
        success: false,
        message: '用户名已存在'
      });
    }

    // 更新用户信息
    if (username) users[userIndex].username = username;
    if (role) users[userIndex].role = role;
    if (typeof enabled === 'boolean') users[userIndex].enabled = enabled;

    console.log(`用户更新成功: ${users[userIndex].username}`);
    res.json({
      success: true,
      message: '用户更新成功',
      data: {
        id: users[userIndex].id,
        username: users[userIndex].username,
        role: users[userIndex].role,
        email: users[userIndex].email,
        enabled: users[userIndex].enabled
      }
    });
  } catch (error) {
    console.error('更新用户失败:', error);
    res.status(500).json({
      success: false,
      message: '更新用户失败'
    });
  }
});

// 删除用户（管理员专用）
app.delete('/api/admin/users/:id', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const userId = parseInt(req.params.id);
    console.log(`管理员 ${req.user.username} 删除用户 ID: ${userId}`);

    const userIndex = users.findIndex(u => u.id === userId);
    if (userIndex === -1) {
      return res.status(404).json({
        success: false,
        message: '用户不存在'
      });
    }

    // 不能删除管理员自己
    if (users[userIndex].username === req.user.username) {
      return res.status(400).json({
        success: false,
        message: '不能删除当前登录用户'
      });
    }

    const deletedUser = users.splice(userIndex, 1)[0];
    console.log(`用户删除成功: ${deletedUser.username}`);
    res.json({
      success: true,
      message: '用户删除成功'
    });
  } catch (error) {
    console.error('删除用户失败:', error);
    res.status(500).json({
      success: false,
      message: '删除用户失败'
    });
  }
});

// 切换用户状态（管理员专用）
app.put('/api/admin/users/:id/toggle', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const userId = parseInt(req.params.id);
    console.log(`管理员 ${req.user.username} 切换用户状态 ID: ${userId}`);

    const userIndex = users.findIndex(u => u.id === userId);
    if (userIndex === -1) {
      return res.status(404).json({
        success: false,
        message: '用户不存在'
      });
    }

    // 不能禁用管理员自己
    if (users[userIndex].username === req.user.username) {
      return res.status(400).json({
        success: false,
        message: '不能禁用当前登录用户'
      });
    }

    // 切换启用状态
    users[userIndex].enabled = !users[userIndex].enabled;

    console.log(`用户状态切换成功: ${users[userIndex].username} -> ${users[userIndex].enabled ? '启用' : '禁用'}`);
    res.json({
      success: true,
      message: `用户${users[userIndex].enabled ? '启用' : '禁用'}成功`,
      data: {
        id: users[userIndex].id,
        username: users[userIndex].username,
        role: users[userIndex].role,
        email: users[userIndex].email,
        enabled: users[userIndex].enabled
      }
    });
  } catch (error) {
    console.error('切换用户状态失败:', error);
    res.status(500).json({
      success: false,
      message: '切换用户状态失败'
    });
  }
});

// 成果管理相关API
app.get('/api/achievements/admin/list', (req, res) => {
  const { page = 0, size = 10, status } = req.query;
  
  let filteredAchievements = achievements;
  if (status && status !== '') {
    filteredAchievements = achievements.filter(a => a.status === status);
  }
  
  const start = page * size;
  const end = start + parseInt(size);
  const content = filteredAchievements.slice(start, end);
  
  res.json({
    content,
    totalElements: filteredAchievements.length,
    totalPages: Math.ceil(filteredAchievements.length / size),
    number: parseInt(page),
    size: parseInt(size)
  });
});

app.get('/api/achievements/statistics', (req, res) => {
  const stats = {
    totalSubmissions: achievements.length,
    redTeamSubmissions: achievements.filter(a => a.type === 'red_team').length,
    blueTeamSubmissions: achievements.filter(a => a.type === 'blue_team').length,
    approvalRate: achievements.filter(a => a.status === 'approved').length / achievements.length
  };
  res.json(stats);
});

app.get('/api/achievements/:id', (req, res) => {
  const achievement = achievements.find(a => a.id === parseInt(req.params.id));
  if (!achievement) {
    return res.status(404).json({ error: 'Achievement not found' });
  }
  res.json(achievement);
});

app.put('/api/achievements/admin/approve/:id', (req, res) => {
  const { reviewerId } = req.query;
  const achievement = achievements.find(a => a.id === parseInt(req.params.id));
  
  if (!achievement) {
    return res.status(404).json({ error: 'Achievement not found' });
  }
  
  achievement.status = 'approved';
  achievement.reviewedAt = new Date().toISOString();
  achievement.reviewerId = reviewerId;
  achievement.reviewComments = '审批通过';
  
  res.json({ message: 'Achievement approved successfully' });
});

app.put('/api/achievements/admin/reject/:id', (req, res) => {
  const { reviewerId, reason } = req.query;
  const achievement = achievements.find(a => a.id === parseInt(req.params.id));
  
  if (!achievement) {
    return res.status(404).json({ error: 'Achievement not found' });
  }
  
  achievement.status = 'rejected';
  achievement.reviewedAt = new Date().toISOString();
  achievement.reviewerId = reviewerId;
  achievement.reviewComments = reason || '不符合要求';
  
  res.json({ message: 'Achievement rejected successfully' });
});

// 演练相关API
app.get('/api/drills/active', (req, res) => {
  const activeDrills = drills.filter(d => d.status === 'active');
  res.json(activeDrills);
});

app.get('/api/drills/containers', (req, res) => {
  res.json(drills);
});

// 成果提交API
app.post('/api/achievements/red-team/submit', upload.fields([
  { name: 'screenshots', maxCount: 10 },
  { name: 'pocVideos', maxCount: 5 },
  { name: 'logFiles', maxCount: 5 },
  { name: 'screenRecords', maxCount: 3 }
]), (req, res) => {
  try {
    const newAchievement = {
      id: achievements.length + 1,
      type: 'red_team',
      ...req.body,
      status: 'pending',
      submittedAt: new Date().toISOString(),
      reviewedAt: null,
      reviewerId: null,
      reviewComments: null,
      files: {
        screenshots: req.files?.screenshots?.map(f => f.filename) || [],
        pocVideos: req.files?.pocVideos?.map(f => f.filename) || [],
        logFiles: req.files?.logFiles?.map(f => f.filename) || [],
        screenRecords: req.files?.screenRecords?.map(f => f.filename) || []
      }
    };
    
    achievements.push(newAchievement);
    
    res.json({ 
      success: true,
      message: '红队成果提交成功！', 
      data: {
        id: newAchievement.id
      }
    });
  } catch (error) {
    console.error('红队成果提交错误:', error);
    res.status(500).json({ 
      success: false,
      message: '提交失败，请重试',
      error: error.message 
    });
  }
});

app.post('/api/achievements/blue-team/submit', upload.fields([
  { name: 'screenshots', maxCount: 10 },
  { name: 'logFiles', maxCount: 5 },
  { name: 'reportFiles', maxCount: 3 }
]), (req, res) => {
  try {
    const newAchievement = {
      id: achievements.length + 1,
      type: 'blue_team',
      ...req.body,
      status: 'pending',
      submittedAt: new Date().toISOString(),
      reviewedAt: null,
      reviewerId: null,
      reviewComments: null,
      files: {
        screenshots: req.files?.screenshots?.map(f => f.filename) || [],
        logFiles: req.files?.logFiles?.map(f => f.filename) || [],
        reportFiles: req.files?.reportFiles?.map(f => f.filename) || []
      }
    };
    
    achievements.push(newAchievement);
    
    res.json({ 
      success: true,
      message: '蓝队成果提交成功！', 
      data: {
        id: newAchievement.id
      }
    });
  } catch (error) {
    console.error('蓝队成果提交错误:', error);
    res.status(500).json({ 
      success: false,
      message: '提交失败，请重试',
      error: error.message 
    });
  }
});

// 拓扑图相关API
app.get('/api/topology/load', (req, res) => {
  const { projectId } = req.query;
  if (projectId === 'default') {
    res.json(topologyData);
  } else {
    res.json({
      projectId,
      nodes: [],
      links: [],
      customElements: []
    });
  }
});

app.post('/api/topology/save', (req, res) => {
  const { projectId, nodes, links, customElements } = req.body;
  
  if (projectId === 'default') {
    topologyData = {
      projectId,
      nodes: nodes || [],
      links: links || [],
      customElements: customElements || []
    };
  }
  
  console.log('拓扑图已保存:', {
    projectId,
    nodesCount: (nodes || []).length,
    linksCount: (links || []).length,
    customElementsCount: (customElements || []).length
  });
  
  res.json({ message: 'Topology saved successfully' });
});

// 系统设置相关API
app.get('/api/settings', authenticateToken, requireRole(['admin']), async (req, res) => {
  try {
    const settings = await systemSettingsDB.getAllSettings();
    res.json({
      success: true,
      data: settings
    });
  } catch (error) {
    console.error('获取系统设置失败:', error);
    res.status(500).json({
      success: false,
      message: '获取设置失败',
      error: error.message
    });
  }
});

app.post('/api/settings', authenticateToken, requireRole(['admin']), async (req, res) => {
  try {
    const settings = req.body;
    await systemSettingsDB.saveSettings(settings);
    
    const updatedSettings = await systemSettingsDB.getAllSettings();
    
    res.json({
      success: true,
      message: '设置保存成功',
      data: updatedSettings
    });
  } catch (error) {
    console.error('保存系统设置失败:', error);
    res.status(500).json({
      success: false,
      message: '保存设置失败',
      error: error.message
    });
  }
});

app.post('/api/settings/upload/logo', authenticateToken, requireRole(['admin']), upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: '没有上传文件'
      });
    }
    
    const logoUrl = `/uploads/${req.file.filename}`;
    await systemSettingsDB.saveSetting('system_logo', logoUrl, 'image', '系统Logo图片');
    
    console.log('Logo上传成功并保存到数据库:', logoUrl);
    
    res.json({
      success: true,
      message: 'Logo上传成功',
      url: logoUrl,
      data: {
        url: logoUrl
      }
    });
  } catch (error) {
    console.error('Logo上传失败:', error);
    res.status(500).json({
      success: false,
      message: 'Logo上传失败',
      error: error.message
    });
  }
});

app.get('/api/settings/:key', authenticateToken, requireRole(['admin']), async (req, res) => {
  try {
    const { key } = req.params;
    const [rows] = await pool.execute('SELECT setting_value FROM system_settings WHERE setting_key = ?', [key]);
    
    if (rows.length > 0) {
      res.json({
        success: true,
        data: {
          key,
          value: rows[0].setting_value
        }
      });
    } else {
      res.status(404).json({
        success: false,
        message: '设置项不存在'
      });
    }
  } catch (error) {
    console.error('获取设置项失败:', error);
    res.status(500).json({
      success: false,
      message: '获取设置项失败',
      error: error.message
    });
  }
});

// 主机节点管理相关API
let hostNodes = [
  {
    id: 1,
    name: 'node-01',
    displayName: '生产服务器01',
    hostIp: '192.168.1.100',
    dockerPort: 2376,
    status: 'active',
    environment: 'production',
    nodeType: 'physical',
    clusterName: 'prod-cluster',
    maxContainers: 50,
    cpuCores: 8,
    memoryTotal: 16384,
    diskTotal: 500,
    description: '主要生产环境服务器',
    lastHealthCheck: new Date().toISOString(),
    connectionStatus: 'connected',
    lastConnectionTest: new Date().toISOString()
  },
  {
    id: 2,
    name: 'node-02', 
    displayName: '测试服务器01',
    hostIp: '192.168.1.101',
    dockerPort: 2376,
    status: 'inactive',
    environment: 'testing',
    nodeType: 'vm',
    clusterName: 'test-cluster',
    maxContainers: 30,
    cpuCores: 4,
    memoryTotal: 8192,
    diskTotal: 200,
    description: '测试环境服务器',
    lastHealthCheck: new Date(Date.now() - 3600000).toISOString(),
    connectionStatus: 'disconnected',
    lastConnectionTest: new Date(Date.now() - 1800000).toISOString()
  }
];

// 获取主机节点列表
app.get('/api/host-nodes', authenticateToken, requireRole(['admin', 'blue']), (req, res) => {
  try {
    console.log(`用户 ${req.user.username}(${req.user.role}) 获取主机节点列表`);
    res.json(hostNodes);
  } catch (error) {
    console.error('获取主机节点列表失败:', error);
    res.status(500).json({
      success: false,
      message: '获取主机节点列表失败',
      error: error.message
    });
  }
});

// 获取主机节点统计信息
app.get('/api/host-nodes/statistics', authenticateToken, requireRole(['admin', 'blue']), (req, res) => {
  try {
    const statistics = {
      active: hostNodes.filter(n => n.status === 'active').length,
      inactive: hostNodes.filter(n => n.status === 'inactive').length, 
      maintenance: hostNodes.filter(n => n.status === 'maintenance').length,
      error: hostNodes.filter(n => n.status === 'error').length
    };
    
    console.log(`用户 ${req.user.username}(${req.user.role}) 获取主机节点统计`);
    res.json(statistics);
  } catch (error) {
    console.error('获取主机节点统计失败:', error);
    res.status(500).json({
      success: false,
      message: '获取统计信息失败',
      error: error.message
    });
  }
});

// 创建新主机节点
app.post('/api/host-nodes', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const newNode = {
      id: hostNodes.length + 1,
      ...req.body,
      status: 'inactive',
      lastHealthCheck: null,
      connectionStatus: 'unknown',
      lastConnectionTest: null
    };
    
    hostNodes.push(newNode);
    
    console.log(`管理员 ${req.user.username} 创建了新主机节点: ${newNode.name}`);
    res.json({
      success: true,
      message: '主机节点创建成功',
      data: newNode
    });
  } catch (error) {
    console.error('创建主机节点失败:', error);
    res.status(500).json({
      success: false,
      message: '创建主机节点失败',
      error: error.message
    });
  }
});

// 更新主机节点
app.put('/api/host-nodes/:id', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const nodeId = parseInt(req.params.id);
    const nodeIndex = hostNodes.findIndex(n => n.id === nodeId);
    
    if (nodeIndex === -1) {
      return res.status(404).json({
        success: false,
        message: '主机节点不存在'
      });
    }
    
    hostNodes[nodeIndex] = { ...hostNodes[nodeIndex], ...req.body };
    
    console.log(`管理员 ${req.user.username} 更新了主机节点: ${hostNodes[nodeIndex].name}`);
    res.json({
      success: true,
      message: '主机节点更新成功',
      data: hostNodes[nodeIndex]
    });
  } catch (error) {
    console.error('更新主机节点失败:', error);
    res.status(500).json({
      success: false,
      message: '更新主机节点失败',
      error: error.message
    });
  }
});

// 删除主机节点
app.delete('/api/host-nodes/:id', authenticateToken, requireRole(['admin']), (req, res) => {
  try {
    const nodeId = parseInt(req.params.id);
    const nodeIndex = hostNodes.findIndex(n => n.id === nodeId);
    
    if (nodeIndex === -1) {
      return res.status(404).json({
        success: false,
        message: '主机节点不存在'
      });
    }
    
    const deletedNode = hostNodes.splice(nodeIndex, 1)[0];
    
    console.log(`管理员 ${req.user.username} 删除了主机节点: ${deletedNode.name}`);
    res.json({
      success: true,
      message: '主机节点删除成功'
    });
  } catch (error) {
    console.error('删除主机节点失败:', error);
    res.status(500).json({
      success: false,
      message: '删除主机节点失败',
      error: error.message
    });
  }
});

// 测试主机节点连接
app.post('/api/host-nodes/:id/test-connection', authenticateToken, requireRole(['admin', 'blue']), (req, res) => {
  try {
    const nodeId = parseInt(req.params.id);
    const node = hostNodes.find(n => n.id === nodeId);
    
    if (!node) {
      return res.status(404).json({
        success: false,
        message: '主机节点不存在'
      });
    }
    
    // 模拟连接测试 - 实际应该连接到Docker API
    const isConnected = Math.random() > 0.3; // 70%几率连接成功
    
    node.connectionStatus = isConnected ? 'connected' : 'disconnected';
    node.lastConnectionTest = new Date().toISOString();
    
    if (!isConnected) {
      node.connectionError = '连接超时，请检查网络和Docker服务状态';
    } else {
      node.connectionError = null;
    }
    
    console.log(`用户 ${req.user.username}(${req.user.role}) 测试节点 ${node.name} 连接: ${isConnected ? '成功' : '失败'}`);
    
    res.json({
      success: true,
      connected: isConnected,
      message: isConnected ? '连接成功' : '连接失败',
      error: node.connectionError,
      node: node
    });
  } catch (error) {
    console.error('测试连接失败:', error);
    res.status(500).json({
      success: false,
      message: '测试连接失败',
      error: error.message
    });
  }
});

// 主机节点健康检查
app.post('/api/host-nodes/:id/health-check', authenticateToken, requireRole(['admin', 'blue']), (req, res) => {
  try {
    const nodeId = parseInt(req.params.id);
    const node = hostNodes.find(n => n.id === nodeId);
    
    if (!node) {
      return res.status(404).json({
        success: false,
        message: '主机节点不存在'
      });
    }
    
    // 模拟健康检查 - 实际应该检查系统资源和服务状态
    const healthStatus = Math.random() > 0.2 ? 'active' : 'error'; // 80%几率健康
    
    node.status = healthStatus;
    node.lastHealthCheck = new Date().toISOString();
    
    if (healthStatus === 'active') {
      node.connectionStatus = 'connected';
    }
    
    console.log(`用户 ${req.user.username}(${req.user.role}) 对节点 ${node.name} 进行健康检查: ${healthStatus}`);
    
    res.json({
      success: true,
      message: `节点 ${node.displayName} 健康检查完成`,
      status: healthStatus,
      node: node
    });
  } catch (error) {
    console.error('健康检查失败:', error);
    res.status(500).json({
      success: false,
      message: '健康检查失败',
      error: error.message
    });
  }
});

// 批量健康检查
app.post('/api/host-nodes/health-check/batch', authenticateToken, requireRole(['admin', 'blue']), (req, res) => {
  try {
    const nodeIds = req.body; // 应该是节点ID数组
    
    if (!Array.isArray(nodeIds) || nodeIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: '请提供有效的节点ID数组'
      });
    }
    
    const results = [];
    
    nodeIds.forEach(nodeId => {
      const node = hostNodes.find(n => n.id === parseInt(nodeId));
      if (node) {
        const healthStatus = Math.random() > 0.2 ? 'active' : 'error';
        node.status = healthStatus;
        node.lastHealthCheck = new Date().toISOString();
        
        results.push({
          nodeId: node.id,
          nodeName: node.name,
          status: healthStatus
        });
      }
    });
    
    console.log(`用户 ${req.user.username}(${req.user.role}) 进行批量健康检查，检查了 ${results.length} 个节点`);
    
    res.json({
      success: true,
      message: `批量健康检查完成，检查了 ${results.length} 个节点`,
      results: results
    });
  } catch (error) {
    console.error('批量健康检查失败:', error);
    res.status(500).json({
      success: false,
      message: '批量健康检查失败',
      error: error.message
    });
  }
});

// 静态文件服务 - 用于Logo图片访问 (添加CORS支持)
app.use('/uploads', (req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET');
  res.header('Access-Control-Allow-Headers', 'Content-Type');
  next();
}, express.static('uploads'));

// 系统日志相关API
// 模拟系统日志数据
const systemLogs = [
  {
    id: 1,
    username: 'admin',
    operation: '用户登录',
    description: '管理员登录系统',
    ip: '127.0.0.1',
    timestamp: '2024-01-20 10:30:15'
  },
  {
    id: 2,
    username: 'admin',
    operation: '创建用户',
    description: '创建了新用户：test',
    ip: '127.0.0.1',
    timestamp: '2024-01-20 10:35:22'
  },
  {
    id: 3,
    username: 'blue',
    operation: '用户登录',
    description: '蓝队用户登录',
    ip: '192.168.1.100',
    timestamp: '2024-01-20 11:00:45'
  },
  {
    id: 4,
    username: 'red',
    operation: '提交成果',
    description: '红队提交漏洞挖掘成果',
    ip: '192.168.1.101',
    timestamp: '2024-01-20 11:30:12'
  },
  {
    id: 5,
    username: 'admin',
    operation: '系统配置',
    description: '修改系统设置',
    ip: '127.0.0.1',
    timestamp: '2024-01-20 12:00:00'
  },
  {
    id: 6,
    username: 'judge',
    operation: '审核成果',
    description: '裁判审核红队成果',
    ip: '192.168.1.102',
    timestamp: '2024-01-20 14:15:30'
  },
  {
    id: 7,
    username: 'blue',
    operation: '防护操作',
    description: '蓝队加强系统防护',
    ip: '192.168.1.100',
    timestamp: '2024-01-20 15:20:18'
  },
  {
    id: 8,
    username: 'admin',
    operation: '用户管理',
    description: '删除了用户：testuser',
    ip: '127.0.0.1',
    timestamp: '2024-01-20 16:45:55'
  }
];

// 获取系统日志（分页）
app.get('/api/logs/page', authenticateToken, requireRole(['admin', 'judge']), (req, res) => {
  try {
    const { username, operation, page = 0, size = 10, sortField = 'timestamp', sortOrder = 'desc' } = req.query;

    console.log(`${req.user.role}用户 ${req.user.username} 查询系统日志`);

    // 过滤日志
    let filteredLogs = [...systemLogs];

    if (username && username.trim()) {
      filteredLogs = filteredLogs.filter(log =>
        log.username.toLowerCase().includes(username.toLowerCase())
      );
    }

    if (operation && operation.trim()) {
      filteredLogs = filteredLogs.filter(log =>
        log.operation.toLowerCase().includes(operation.toLowerCase())
      );
    }

    // 排序
    filteredLogs.sort((a, b) => {
      let aValue = a[sortField] || '';
      let bValue = b[sortField] || '';

      // 时间戳特殊处理
      if (sortField === 'timestamp') {
        aValue = new Date(aValue).getTime();
        bValue = new Date(bValue).getTime();
      }

      if (sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    // 分页
    const pageNum = parseInt(page);
    const pageSize = parseInt(size);
    const startIndex = pageNum * pageSize;
    const endIndex = startIndex + pageSize;
    const content = filteredLogs.slice(startIndex, endIndex);

    const result = {
      content: content,
      totalElements: filteredLogs.length,
      totalPages: Math.ceil(filteredLogs.length / pageSize),
      first: pageNum === 0,
      last: endIndex >= filteredLogs.length,
      size: pageSize,
      number: pageNum
    };

    console.log(`返回 ${content.length} 条日志记录，总计 ${filteredLogs.length} 条`);
    res.json(result);

  } catch (error) {
    console.error('获取系统日志失败:', error);
    res.status(500).json({
      success: false,
      message: '获取系统日志失败',
      error: error.message
    });
  }
});

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// 错误处理
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

// 404处理
app.use((req, res) => {
  res.status(404).json({ error: 'API endpoint not found' });
});

// 启动服务器
const startServer = async () => {
  try {
    // 初始化数据库
    await initDatabase();
    
    // 启动HTTP服务器
    app.listen(PORT, () => {
      console.log(`🚀 CyberLab API Server running on http://localhost:${PORT}`);
      console.log(`📊 Health check: http://localhost:${PORT}/api/health`);
      console.log(`🗄️ MySQL数据库连接成功 - cyberlab@localhost:3306`);
    });
  } catch (error) {
    console.error('❌ 服务器启动失败:', error);
    process.exit(1);
  }
};

// 启动服务器
startServer();