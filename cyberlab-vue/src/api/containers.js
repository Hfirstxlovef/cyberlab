import request from './axios'

// 容器管理相关API

/**
 * 发现项目容器 - 基于节点探测
 * @param {string} projectId - 项目ID
 * @returns {Promise} 容器发现结果
 */
export const discoverProjectContainers = (projectId) => {
  return request.post(`/containers/discover/project/${encodeURIComponent(projectId)}`)
}

/**
 * 发现项目容器 - 直接IP探测（推荐）
 * 直接通过资产配置的Docker/K8s平台IP进行探测，无需依赖主机节点
 * @param {string} projectId - 项目ID
 * @returns {Promise} 容器发现结果
 */
export const discoverProjectContainersDirect = (projectId) => {
  return request.post(`/containers/discover/project/${encodeURIComponent(projectId)}/direct`)
}

/**
 * 发现项目镜像 - 直接IP探测
 * 直接通过资产配置的Docker平台IP探测Docker镜像列表
 * @param {string} projectId - 项目ID
 * @returns {Promise} 镜像发现结果
 */
export const discoverProjectImages = (projectId) => {
  return request.post(`/containers/discover/project/${encodeURIComponent(projectId)}/images`)
}

/**
 * 发现单个资产的Docker镜像 - 直接IP探测
 * 直接通过资产配置的Docker平台IP探测该资产的Docker镜像列表
 * @param {number} assetId - 资产ID
 * @returns {Promise} 镜像发现结果
 */
export const discoverAssetImages = (assetId) => {
  return request.post(`/containers/discover/asset/${assetId}/images`)
}

/**
 * 发现所有容器
 * @returns {Promise} 所有容器发现结果
 */
export const discoverAllContainers = () => {
  return request.post('/containers/discover')
}

/**
 * 获取项目容器状态
 * @param {string} projectId - 项目ID
 * @returns {Promise} 项目容器状态信息
 */
export const getProjectContainerStatus = (projectId) => {
  return request.get('/containers/project-status', {
    params: { projectId }
  })
}

/**
 * 获取容器详情
 * @param {string} containerId - 容器ID
 * @param {number} nodeId - 节点ID
 * @returns {Promise} 容器详细信息
 */
export const getContainerDetails = (containerId, nodeId) => {
  return request.get(`/containers/${containerId}/details`, {
    params: { nodeId }
  })
}
