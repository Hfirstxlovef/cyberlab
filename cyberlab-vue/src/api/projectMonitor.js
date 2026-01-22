import request from './axios'

// 项目监控相关API

// 获取项目资产统计数据
export const getProjectAssetStats = (projectId) => {
  return request.get(`/assets/project-stats/${projectId}`)
}
