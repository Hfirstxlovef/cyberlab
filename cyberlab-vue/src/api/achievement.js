import request from './axios'

// 成果管理相关API
export const submitAchievement = (data) => {
  return request.post('/achievements/submit', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 红队成果提交
export const submitRedTeamAchievement = (data) => {
  return request.post('/achievements/red-team/submit', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 蓝队成果提交
export const submitBlueTeamAchievement = (data) => {
  return request.post('/achievements/blue-team/submit', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getAchievementsList = (params) => {
  return request.get('/achievements/admin/list', { params })
}

export const getAchievementDetail = (id) => {
  return request.get(`/achievements/${id}`)
}

export const approveAchievement = (id, reviewerId) => {
  return request.put(`/achievements/admin/approve/${id}`, null, {
    params: { reviewerId }
  })
}

export const rejectAchievement = (id, reviewerId, reason) => {
  return request.put(`/achievements/admin/reject/${id}`, null, {
    params: { reviewerId, reason }
  })
}

export const getAchievementStatistics = () => {
  return request.get('/achievements/statistics')
}

/**
 * 获取最近审核通过的成果（用于实时展示）
 */
export const getRecentApprovedAchievements = (limit = 10, since = null) => {
  return request.get('/achievements/recent-approved', {
    params: { limit, since }
  })
}

/**
 * 轮询获取最新成果（用于实时更新）
 * @param {Function} callback - 接收新成果的回调函数
 * @param {number} interval - 轮询间隔（毫秒）
 * @returns {Function} 清理函数，用于停止轮询
 */
export const pollLatestAchievements = (callback, interval = 5000) => {
  let lastFetchTime = null // 首次为null，获取所有历史成果
  let errorCount = 0
  const MAX_ERRORS = 3 // 连续错误超过3次时停止轮询
  let isFirstLoad = true // 标记是否首次加载

  const poll = async () => {
    try {
      // 首次加载时不传since参数，获取所有已审核通过的成果
      const achievements = await getRecentApprovedAchievements(10, lastFetchTime)

      // 安全处理：确保返回的是数组
      const safeAchievements = Array.isArray(achievements) ? achievements : []

      // 首次加载时即使是空数组也要调用callback，后续只在有新数据时调用
      if (isFirstLoad || safeAchievements.length > 0) {
        callback(safeAchievements)
        errorCount = 0 // 重置错误计数

        // 更新lastFetchTime
        if (isFirstLoad && safeAchievements.length > 0) {
          // 首次加载：使用最新成果的审核时间作为起点
          const latestReviewTime = safeAchievements[0].reviewTime
          lastFetchTime = new Date(latestReviewTime).toISOString()
          isFirstLoad = false
        } else if (safeAchievements.length > 0) {
          // 后续轮询：使用当前时间
          lastFetchTime = new Date().toISOString()
        } else {
          // 首次加载但没有数据
          lastFetchTime = new Date().toISOString()
          isFirstLoad = false
        }
      }
      // 注意：后续轮询时空数组不是错误，不记录日志
    } catch (error) {
      errorCount++

      // 只在前3次错误时记录日志，避免控制台刷屏
      if (errorCount <= MAX_ERRORS) {
        console.warn(`轮询获取最新成果失败 (${errorCount}/${MAX_ERRORS}):`, error.message)

        // 如果是权限错误（403），立即停止轮询
        if (error.response && error.response.status === 403) {
          console.error('权限不足，停止轮询成果更新')
          return false // 通知停止轮询
        }
      }

      // 连续错误超过阈值时，增加轮询间隔或停止轮询
      if (errorCount >= MAX_ERRORS) {
        console.error(`连续${MAX_ERRORS}次获取成果失败，请检查网络连接或后端服务`)
      }
    }

    return true // 继续轮询
  }

  const intervalId = setInterval(async () => {
    const shouldContinue = await poll()
    if (shouldContinue === false) {
      clearInterval(intervalId) // 停止轮询
    }
  }, interval)

  poll() // 立即执行一次

  return () => clearInterval(intervalId) // 返回清理函数
}