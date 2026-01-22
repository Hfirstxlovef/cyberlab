import request from './axios'

// 主机节点管理相关API
export const getAvailableHostNodes = () => {
  return request.get('/host-nodes/available-for-deployment')
}

export const getAllHostNodes = () => {
  return request.get('/host-nodes')
}

export const getHostNodeById = (id) => {
  return request.get(`/host-nodes/${id}`)
}

export const createHostNode = (data) => {
  return request.post('/host-nodes', data)
}

export const updateHostNode = (id, data) => {
  return request.put(`/host-nodes/${id}`, data)
}

export const deleteHostNode = (id) => {
  return request.delete(`/host-nodes/${id}`)
}

export const getHostNodeStatus = (id) => {
  return request.get(`/host-nodes/${id}/status`)
}

export const deployToHostNode = (nodeId, deploymentData) => {
  return request.post(`/host-nodes/${nodeId}/deploy`, deploymentData)
}

export const getHostNodeStatistics = () => {
  return request.get('/host-nodes/statistics')
}

export const performHealthCheck = (nodeId) => {
  return request.post(`/host-nodes/${nodeId}/health-check`)
}

export const testConnection = (nodeId) => {
  return request.post(`/host-nodes/${nodeId}/test-connection`)
}

export const performBatchHealthCheck = (nodeIds) => {
  return request.post('/host-nodes/health-check/batch', nodeIds)
}

// 批量健康检查并自动激活可用节点
export const batchHealthCheckAndActivate = () => {
  return request.post('/host-nodes/batch-health-check-activate')
}

export const getActiveHostNodes = () => {
  return request.get('/host-nodes/active')
}

// 获取所有主机节点（别名，兼容旧代码）
export const getHostNodes = () => {
  return getAllHostNodes()
}

// 获取主机节点负载信息
export const getHostNodesLoadInfo = () => {
  return request.get('/host-nodes/load-info')
}