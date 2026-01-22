import axios from './axios'

// 获取管理员统计数据
export const getAdminStats = async () => {
  try {
    const response = await axios.get('/dashboard/admin/stats')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      totalUsers: 156,
      activeRanges: 8,
      totalAchievements: 342,
      systemHealth: 98,
      runningDrills: 0,
      redTeamCount: 0,
      blueTeamCount: 0,
      systemAlerts: 0
    }
  }
}

// 获取裁判统计数据
export const getJudgeStats = async () => {
  try {
    const response = await axios.get('/dashboard/judge/stats')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      drillCount: 5,
      redSuccessRate: 72,
      blueDefenseRate: 85,
      pendingCount: 12
    }
  }
}

// 获取红队统计数据
export const getRedTeamStats = async () => {
  try {
    const response = await axios.get('/dashboard/red/stats')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      successfulAttacks: 28,
      vulnerabilitiesFound: 15,
      currentScore: 850,
      ranking: 2
    }
  }
}

// 获取蓝队统计数据
export const getBlueTeamStats = async () => {
  try {
    const response = await axios.get('/dashboard/blue/stats')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      attacksBlocked: 45,
      vulnerabilitiesFixed: 23,
      currentScore: 920,
      ranking: 1
    }
  }
}

// 获取待审核提交列表
export const getPendingSubmissions = async () => {
  try {
    const response = await axios.get('/dashboard/pending-submissions')
    return response
  } catch (error) {
    // 返回模拟数据
    return [
      { 
        id: 1, 
        time: '14:30', 
        team: 'red', 
        type: 'SQL注入', 
        description: '成功获取用户数据', 
        status: '待审核' 
      },
      { 
        id: 2, 
        time: '14:25', 
        team: 'blue', 
        type: '拦截记录', 
        description: '成功拦截XSS攻击', 
        status: '待审核' 
      },
      { 
        id: 3, 
        time: '14:20', 
        team: 'red', 
        type: '权限提升', 
        description: '获取管理员权限', 
        status: '可疑' 
      }
    ]
  }
}

// 审核通过成果
export const approveAchievement = async (achievementId, judgeId) => {
  try {
    const response = await axios.post(`/achievements/${achievementId}/approve`, {
      judgeId,
      action: 'approve'
    })
    return response
  } catch (error) {
    // 模拟成功响应
    return { success: true, message: '审核通过' }
  }
}

// 驳回成果
export const rejectAchievement = async (achievementId, judgeId, reason) => {
  try {
    const response = await axios.post(`/achievements/${achievementId}/reject`, {
      judgeId,
      reason,
      action: 'reject'
    })
    return response
  } catch (error) {
    // 模拟成功响应
    return { success: true, message: '已驳回' }
  }
}

// 获取系统活动日志
export const getSystemLogs = async (limit = 50) => {
  try {
    const response = await axios.get(`/dashboard/system-logs?limit=${limit}`)
    return response
  } catch (error) {
    // 返回模拟数据
    return [
      { time: '14:35', type: 'info', message: '用户登录成功', user: 'admin' },
      { time: '14:30', type: 'warning', message: '检测到异常流量', user: 'system' },
      { time: '14:25', type: 'success', message: '成果提交成功', user: 'red_team_1' },
      { time: '14:20', type: 'error', message: '数据库连接超时', user: 'system' }
    ]
  }
}

// 获取实时攻防数据
export const getRealTimeData = async () => {
  try {
    const response = await axios.get('/dashboard/realtime')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      activeAttacks: Math.floor(Math.random() * 10) + 1,
      blockedAttacks: Math.floor(Math.random() * 20) + 5,
      systemLoad: Math.floor(Math.random() * 30) + 40,
      networkTraffic: Math.floor(Math.random() * 100) + 200
    }
  }
}

// 获取演练历史数据
export const getDrillHistory = async () => {
  try {
    const response = await axios.get('/dashboard/drill-history')
    return response
  } catch (error) {
    // 返回模拟数据
    return [
      { date: '2024-01-15', redScore: 850, blueScore: 920, winner: 'blue' },
      { date: '2024-01-10', redScore: 780, blueScore: 760, winner: 'red' },
      { date: '2024-01-05', redScore: 920, blueScore: 880, winner: 'red' },
      { date: '2023-12-28', redScore: 650, blueScore: 720, winner: 'blue' },
      { date: '2023-12-20', redScore: 800, blueScore: 790, winner: 'red' }
    ]
  }
}

// 获取用户活动统计
export const getUserActivity = async () => {
  try {
    const response = await axios.get('/dashboard/user-activity')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      onlineUsers: 45,
      todayLogins: 128,
      activeTeams: 8,
      totalSessions: 256
    }
  }
}

// 获取管理员趋势图数据
// ✅ 修复：添加 days 参数支持（1天/3天/7天）
export const getAdminTrendData = async (days = 7) => {
  try {
    const response = await axios.get(`/dashboard/admin/trend-data?days=${days}`)
    return response
  } catch (error) {
    // 返回模拟数据，根据天数调整
    if (days === 1) {
      // 1天的数据（24小时）
      const labels = []
      const red = []
      const blue = []
      for (let i = 0; i < 24; i++) {
        labels.push(`${String(i).padStart(2, '0')}:00`)
        red.push(Math.floor(Math.random() * 10))
        blue.push(Math.floor(Math.random() * 10))
      }
      return { timeLabels: labels, redTeamData: red, blueTeamData: blue, success: false }
    } else {
      // 多天的数据
      const labels = []
      const red = []
      const blue = []
      const now = new Date()
      for (let i = days - 1; i >= 0; i--) {
        const day = new Date(now)
        day.setDate(now.getDate() - i)
        labels.push(`${String(day.getMonth() + 1).padStart(2, '0')}/${String(day.getDate()).padStart(2, '0')}`)
        red.push(Math.floor(Math.random() * 20 + 5))
        blue.push(Math.floor(Math.random() * 20 + 5))
      }
      return { timeLabels: labels, redTeamData: red, blueTeamData: blue, success: false }
    }
  }
}

// 获取管理员资源分配数据
export const getAdminResourceData = async () => {
  try {
    const response = await axios.get('/dashboard/admin/resource-data')
    return response
  } catch (error) {
    // 返回模拟数据
    return {
      resourceList: [
        { value: 35, name: '靶机资源' },
        { value: 25, name: '容器资源' },
        { value: 20, name: '网络带宽' },
        { value: 20, name: '存储空间' }
      ],
      success: false
    }
  }
}