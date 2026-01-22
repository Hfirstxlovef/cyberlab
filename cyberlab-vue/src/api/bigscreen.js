import axios from './axios'

// 数据适配器：将后端数据格式转换为前端期望的格式
const adaptBackendData = (backendData) => {
  // 为缺失的trend数据生成基于recentOperations的图表数据
  const generateTrendData = (operations) => {
    if (!operations || !Array.isArray(operations)) return []

    // 按小时统计操作次数，生成6小时的趋势数据
    const hourlyData = {}
    const now = new Date()

    // 初始化最近6小时的数据
    for (let i = 5; i >= 0; i--) {
      const hour = new Date(now.getTime() - i * 60 * 60 * 1000)
      const hourKey = hour.getHours().toString().padStart(2, '0') + ':00'
      hourlyData[hourKey] = { attacks: 0, blocks: 0 }
    }

    // 统计操作数据
    operations.forEach(op => {
      const opTime = new Date(op.time)
      const hourKey = opTime.getHours().toString().padStart(2, '0') + ':00'
      if (hourlyData[hourKey]) {
        if (op.status === '成功') {
          hourlyData[hourKey].blocks += 1
        } else {
          hourlyData[hourKey].attacks += 1
        }
      }
    })

    return Object.entries(hourlyData).map(([time, data]) => ({
      time,
      attacks: data.attacks,
      blocks: data.blocks
    }))
  }

  return {
    // 总览数据 - 直接映射
    overview: {
      totalAttacks: backendData.overview?.totalAttacks || 0,
      blockedAttacks: backendData.overview?.runningDrills || 0,
      activeThreats: backendData.overview?.onlineUsers || 0,
      systemHealth: 98 // 固定值，可以后续从systemHealth计算
    },

    // 趋势数据 - 优先使用后端提供的trend，若无则从recentOperations生成
    trend: backendData.trend && Array.isArray(backendData.trend) && backendData.trend.length > 0
      ? backendData.trend
      : generateTrendData(backendData.recentOperations),

    // 漏洞数据 - 优先使用后端提供的vulnerability，若无则使用默认数据
    vulnerability: backendData.vulnerability && Array.isArray(backendData.vulnerability) && backendData.vulnerability.length > 0
      ? backendData.vulnerability
      : [
        { name: 'SQL注入', value: 25 },
        { name: 'XSS跨站脚本', value: 18 },
        { name: '权限提升', value: 15 },
        { name: '文件上传', value: 12 },
        { name: '命令执行', value: 10 },
        { name: '其他', value: 8 }
      ],

    // 容器数据 - 映射activeContainers
    containers: backendData.activeContainers?.map(container => ({
      id: container.name?.replace(/[^0-9]/g, '') || Math.random().toString(),
      name: container.name || 'Unknown',
      image: container.image || 'unknown:latest',
      status: container.status === 'not_deployed' ? 'stopped' : 'running',
      ports: 'N/A',
      cpu: Math.floor(Math.random() * 30) + 10,
      memory: Math.floor(Math.random() * 200) + 100
    })) || [],

    // 资源数据 - 映射resourceUsage
    resources: {
      cpu: Math.round(backendData.resourceUsage?.cpu || 0),
      memory: Math.round(backendData.resourceUsage?.memory || 0),
      disk: Math.round(backendData.resourceUsage?.disk || 0),
      network: Math.round(backendData.resourceUsage?.network || 0)
    },

    // 告警数据 - 映射securityAlerts
    alerts: backendData.securityAlerts?.map(alert => ({
      id: Math.random().toString(),
      level: alert.level === '高危' ? 'critical' : alert.level === '中危' ? 'warning' : 'info',
      title: alert.type || '系统告警',
      description: alert.message || '无详细信息',
      time: new Date(alert.time),
      source: 'CyberLab'
    })) || [],

    // 排行数据 - 优先使用后端提供的ranking，若无则使用默认数据
    ranking: backendData.ranking && Array.isArray(backendData.ranking) && backendData.ranking.length > 0
      ? backendData.ranking.map((team, index) => ({
          id: index + 1,
          name: team.name || '未知战队',
          type: team.type || 'red',
          score: team.score || 0,
          change: team.change || 0
        }))
      : [
        { id: 1, name: '红龙战队', type: 'red', score: 2850, change: 120 },
        { id: 2, name: '蓝盾防护', type: 'blue', score: 2720, change: 85 },
        { id: 3, name: '暗影突击', type: 'red', score: 2680, change: -45 },
        { id: 4, name: '守护者联盟', type: 'blue', score: 2590, change: 200 }
      ]
  }
}

// 获取大屏数据
export const getBigScreenData = async () => {
  try {
    // 注意：axios响应拦截器已经返回了response.data，所以这里直接是数据
    const backendData = await axios.get('/bigscreen/data')

    // 检查后端数据是否有效
    if (!backendData || typeof backendData !== 'object' || Object.keys(backendData).length === 0) {
      throw new Error('后端返回数据无效')
    }

    // 使用数据适配器转换数据格式
    const adaptedData = adaptBackendData(backendData)

    return adaptedData

  } catch (error) {
    // 记录错误信息，便于调试
    console.error('❌ 获取大屏数据失败:', error.message || error)
    console.warn('⚠️  正在使用默认示例数据，请检查后端API是否正常运行')

    // 返回最小化的默认数据（仅用于展示，避免页面空白）
    return {
        overview: {
          totalAttacks: 0,
          blockedAttacks: 0,
          activeThreats: 0,
          systemHealth: 0
        },
        trend: [],
        vulnerability: [],
        containers: [],
        resources: { cpu: 0, memory: 0, disk: 0, network: 0 },
        alerts: [{
          id: 1,
          level: 'warning',
          title: '无法连接到后端API',
          description: '请检查后端服务是否正常运行',
          time: new Date(),
          source: 'CyberLab前端'
        }],
        ranking: []
    }
  }
}

// 获取系统资源数据
export const getSystemResources = async () => {
  try {
    const response = await axios.get('/bigscreen/resources')
    return response.data
  } catch (error) {
    console.error('❌ 获取系统资源数据失败:', error.message || error)
    // 返回空数据
    return { cpu: 0, memory: 0, disk: 0, network: 0 }
  }
}

// 获取攻击趋势数据
export const getAttackTrend = async (hours = 24) => {
  try {
    const response = await axios.get('/bigscreen/trend', { params: { hours } })
    return response.data
  } catch (error) {
    console.error('❌ 获取攻击趋势数据失败:', error.message || error)
    // 返回空数组
    return []
  }
}