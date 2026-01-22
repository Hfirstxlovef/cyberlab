import request from './axios'

/**
 * 获取裁判大屏展示数据
 * @returns {Promise}
 */
export function getJudgeBigScreenData() {
  return request({
    url: '/api/bigscreen/judge',
    method: 'get'
  })
}

/**
 * 获取比赛实时得分
 * @returns {Promise}
 */
export function getMatchScores() {
  return request({
    url: '/api/judge/scores',
    method: 'get'
  })
}

/**
 * 获取对抗态势数据
 * @returns {Promise}
 */
export function getBattleSituation() {
  return request({
    url: '/api/judge/battle',
    method: 'get'
  })
}

/**
 * 获取得分趋势
 * @returns {Promise}
 */
export function getScoreTrends() {
  return request({
    url: '/api/judge/score-trends',
    method: 'get'
  })
}

/**
 * 获取攻防事件时间线
 * @returns {Promise}
 */
export function getEventTimeline() {
  return request({
    url: '/api/judge/events',
    method: 'get'
  })
}

/**
 * 获取系统监控数据
 * @returns {Promise}
 */
export function getSystemMonitoring() {
  return request({
    url: '/api/judge/systems',
    method: 'get'
  })
}

/**
 * 获取队伍表现统计
 * @returns {Promise}
 */
export function getTeamPerformance() {
  return request({
    url: '/api/judge/performance',
    method: 'get'
  })
}

/**
 * 获取规则违反记录
 * @returns {Promise}
 */
export function getViolationRecords() {
  return request({
    url: '/api/judge/violations',
    method: 'get'
  })
}

/**
 * 更新比赛状态
 * @param {String} status - 比赛状态 (ongoing, paused, finished)
 * @returns {Promise}
 */
export function updateMatchStatus(status) {
  return request({
    url: '/api/judge/match-status',
    method: 'put',
    data: { status }
  })
}

/**
 * 记录违规行为
 * @param {Object} violation - 违规信息
 * @returns {Promise}
 */
export function recordViolation(violation) {
  return request({
    url: '/api/judge/violations',
    method: 'post',
    data: violation
  })
}

/**
 * 模拟裁判大屏数据（用于开发测试）
 * @returns {Object}
 */
export function getMockJudgeData() {
  return {
    scores: {
      red: 1250,
      blue: 1180,
      redTrend: '↑ +120',
      blueTrend: '↑ +85'
    },
    battle: [
      { time: '00:10', red: 100, blue: 100 },
      { time: '00:20', red: 250, blue: 180 },
      { time: '00:30', red: 450, blue: 380 },
      { time: '00:40', red: 680, blue: 620 },
      { time: '00:50', red: 920, blue: 850 },
      { time: '01:00', red: 1100, blue: 1050 },
      { time: '01:10', red: 1250, blue: 1180 }
    ],
    redTrend: [800, 900, 1000, 1100, 1150, 1250],
    blueTrend: [850, 920, 980, 1050, 1120, 1180],
    events: [
      { 
        time: '00:15:23', 
        team: 'red', 
        event: '成功获取Web服务权限', 
        score: '+50',
        severity: 'high'
      },
      { 
        time: '00:18:45', 
        team: 'blue', 
        event: '检测并阻止SQL注入攻击', 
        score: '+30',
        severity: 'medium'
      },
      { 
        time: '00:22:10', 
        team: 'red', 
        event: '发现并利用文件上传漏洞', 
        score: '+80',
        severity: 'critical'
      },
      { 
        time: '00:25:30', 
        team: 'blue', 
        event: '修复关键系统漏洞', 
        score: '+60',
        severity: 'high'
      },
      { 
        time: '00:32:15', 
        team: 'red', 
        event: '成功提升系统权限', 
        score: '+100',
        severity: 'critical'
      }
    ],
    systems: [
      { 
        name: 'Web服务器', 
        status: 'compromised', 
        owner: 'red',
        health: 45,
        services: ['HTTP', 'HTTPS', 'SSH']
      },
      { 
        name: '数据库服务器', 
        status: 'defended', 
        owner: 'blue',
        health: 85,
        services: ['MySQL', 'Redis']
      },
      { 
        name: '应用服务器', 
        status: 'contested', 
        owner: 'neutral',
        health: 60,
        services: ['Tomcat', 'Node.js']
      },
      { 
        name: '文件服务器', 
        status: 'normal', 
        owner: 'blue',
        health: 95,
        services: ['FTP', 'SMB']
      }
    ],
    redPerformance: {
      attacks: 45,
      successes: 28,
      rate: 62,
      avgTime: '12m',
      points: 1250
    },
    bluePerformance: {
      defenses: 38,
      blocks: 30,
      rate: 79,
      avgResponse: '2.3m',
      points: 1180
    },
    violations: [
      {
        time: '00:45:12',
        team: 'red',
        type: '超出攻击范围',
        description: '尝试攻击非目标系统',
        penalty: '-50'
      }
    ]
  }
}

// 如果后端API还未实现，使用模拟数据
export async function getJudgeBigScreenDataWithFallback() {
  try {
    const response = await getJudgeBigScreenData()
    return response
  } catch (error) {
    return getMockJudgeData()
  }
}