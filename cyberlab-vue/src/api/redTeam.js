import request from './axios'

/**
 * 获取红队大屏展示数据
 * @returns {Promise}
 */
export function getRedTeamBigScreenData() {
  return request({
    url: '/api/bigscreen/red',
    method: 'get'
  })
}

/**
 * 获取攻击成功率统计
 * @returns {Promise}
 */
export function getAttackSuccessRate() {
  return request({
    url: '/api/red/attack-success',
    method: 'get'
  })
}

/**
 * 获取漏洞利用统计
 * @returns {Promise}
 */
export function getExploitStatistics() {
  return request({
    url: '/api/red/exploits',
    method: 'get'
  })
}

/**
 * 获取目标系统状态
 * @returns {Promise}
 */
export function getTargetSystemStatus() {
  return request({
    url: '/api/red/targets',
    method: 'get'
  })
}

/**
 * 获取攻击路径分析
 * @returns {Promise}
 */
export function getAttackPaths() {
  return request({
    url: '/api/red/attack-paths',
    method: 'get'
  })
}

/**
 * 获取红队战果统计
 * @returns {Promise}
 */
export function getAttackAchievements() {
  return request({
    url: '/api/red/achievements',
    method: 'get'
  })
}

/**
 * 获取任务进度
 * @returns {Promise}
 */
export function getMissionProgress() {
  return request({
    url: '/api/red/missions',
    method: 'get'
  })
}

/**
 * 模拟红队大屏数据（用于开发测试）
 * @returns {Object}
 */
export function getMockRedTeamData() {
  return {
    attackSuccess: [
      { name: 'Web应用', success: 85, attempts: 100 },
      { name: '数据库', success: 60, attempts: 80 },
      { name: '主机系统', success: 45, attempts: 60 },
      { name: '网络设备', success: 70, attempts: 90 }
    ],
    exploits: [
      { type: 'SQL注入', count: 45, success: 38 },
      { type: 'XSS', count: 32, success: 28 },
      { type: '文件上传', count: 18, success: 15 },
      { type: '命令执行', count: 12, success: 10 },
      { type: '权限提升', count: 8, success: 7 }
    ],
    targets: [
      { name: 'Web-01', status: 'compromised', services: 5, score: 150 },
      { name: 'DB-01', status: 'partial', services: 3, score: 80 },
      { name: 'App-01', status: 'scanning', services: 8, score: 0 },
      { name: 'File-01', status: 'clean', services: 4, score: 0 }
    ],
    paths: [
      { from: 'Internet', to: 'DMZ', status: 'active' },
      { from: 'DMZ', to: 'Web-01', status: 'compromised' },
      { from: 'Web-01', to: 'DB-01', status: 'attempting' }
    ],
    achievements: [
      { name: '首次突破', time: '00:12:34', points: 100 },
      { name: '获取敏感数据', time: '00:25:18', points: 200 },
      { name: '权限提升成功', time: '00:38:42', points: 150 }
    ],
    missions: [
      { name: '渗透Web服务', progress: 100, status: 'completed' },
      { name: '获取数据库权限', progress: 65, status: 'in_progress' },
      { name: '横向移动', progress: 30, status: 'in_progress' },
      { name: '数据外泄', progress: 0, status: 'pending' }
    ]
  }
}

// 如果后端API还未实现，使用模拟数据
export async function getRedTeamBigScreenDataWithFallback() {
  try {
    const response = await getRedTeamBigScreenData()
    return response
  } catch (error) {
    return getMockRedTeamData()
  }
}