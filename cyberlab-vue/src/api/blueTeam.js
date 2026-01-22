import request from './axios'

/**
 * 获取蓝队大屏展示数据
 * @returns {Promise}
 */
export function getBlueTeamBigScreenData() {
  return request.get('/bigscreen/blue')
}

/**
 * 获取蓝队防护统计数据
 * @returns {Promise}
 */
export function getDefenseStatistics() {
  return request.get('/blue/defense-stats')
}

/**
 * 获取威胁检测数据
 * @returns {Promise}
 */
export function getThreatDetections() {
  return request.get('/blue/threats')
}

/**
 * 获取系统健康状态
 * @returns {Promise}
 */
export function getSystemHealth() {
  return request.get('/blue/system-health')
}

/**
 * 获取安全告警列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getSecurityAlerts(params) {
  return request.get('/blue/alerts', { params })
}

/**
 * 处理安全告警
 * @param {String} alertId - 告警ID
 * @param {Object} data - 处理信息
 * @returns {Promise}
 */
export function handleSecurityAlert(alertId, data) {
  return request.post(`/blue/alerts/${alertId}/handle`, data)
}

/**
 * 获取防护趋势数据
 * @param {Object} params - 时间范围参数
 * @returns {Promise}
 */
export function getDefenseTrend(params) {
  return request.get('/blue/defense-trend', { params })
}

/**
 * 获取防护效果评估
 * @returns {Promise}
 */
export function getDefenseEffectiveness() {
  return request.get('/blue/effectiveness')
}

/**
 * 获取实时资源监控数据
 * @returns {Promise}
 */
export function getResourceMonitoring() {
  return request.get('/blue/resources')
}

/**
 * 获取蓝队活动日志
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getBlueTeamLogs(params) {
  return request.get('/blue/logs', { params })
}

/**
 * 更新防护策略
 * @param {Object} strategy - 防护策略
 * @returns {Promise}
 */
export function updateDefenseStrategy(strategy) {
  return request.put('/blue/strategy', strategy)
}

/**
 * 获取防护策略列表
 * @returns {Promise}
 */
export function getDefenseStrategies() {
  return request.get('/blue/strategies')
}

/**
 * 模拟蓝队大屏数据（用于开发测试）
 * @returns {Object}
 */
export function getMockBlueTeamData() {
  return {
    overview: {
      runningDrills: 3,
      totalDefenses: 856,
      onlineUsers: 12,
      activeServices: 8,
      successRate: 92,
      activeThreats: 5
    },
    trend: [
      { time: '00:00', value: 92 },
      { time: '04:00', value: 94 },
      { time: '08:00', value: 93 },
      { time: '12:00', value: 95 },
      { time: '16:00', value: 91 },
      { time: '20:00', value: 96 },
      { time: '24:00', value: 94 }
    ],
    threats: [
      { type: 'SQL注入', count: 45, severity: 'high' },
      { type: 'XSS攻击', count: 32, severity: 'medium' },
      { type: 'DDoS', count: 12, severity: 'critical' },
      { type: '端口扫描', count: 78, severity: 'low' },
      { type: '暴力破解', count: 23, severity: 'high' }
    ],
    health: [
      { service: 'Web服务', status: 'normal', uptime: 99.9 },
      { service: '数据库', status: 'normal', uptime: 99.8 },
      { service: '防火墙', status: 'normal', uptime: 100 },
      { service: 'IDS/IPS', status: 'warning', uptime: 98.5 },
      { service: '日志系统', status: 'normal', uptime: 99.7 }
    ],
    resources: {
      cpu: 35,
      memory: 58,
      disk: 42,
      network: 18,
      bandwidth: {
        in: 1250,
        out: 890
      }
    },
    alerts: [
      {
        id: 1,
        severity: 'critical',
        time: '10:23:15',
        title: 'DDoS攻击检测',
        source: 'IDS',
        target: '192.168.1.100',
        description: '检测到大规模分布式拒绝服务攻击',
        handled: false
      },
      {
        id: 2,
        severity: 'high',
        time: '10:25:42',
        title: 'SQL注入尝试',
        source: 'WAF',
        target: '192.168.1.101',
        description: '检测到SQL注入攻击尝试，已自动拦截',
        handled: true
      }
    ],
    defense: [
      { label: '攻击拦截', value: 1247, percentage: 95 },
      { label: '流量阻断', value: 856, percentage: 88 },
      { label: '漏洞修复', value: 42, percentage: 100 },
      { label: '响应速度', value: 1.2, percentage: 96 }
    ]
  }
}

// 如果后端API还未实现，使用模拟数据
export async function getBlueTeamBigScreenDataWithFallback() {
  try {
    const response = await getBlueTeamBigScreenData()
    return response
  } catch (error) {
    return getMockBlueTeamData()
  }
}