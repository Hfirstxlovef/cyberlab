import request from './axios'

// 资产管理相关API
export const getAssetList = (params) => {
  return request.get('/assets', { params })
}

export const createAsset = (data) => {
  return request.post('/assets', data)
}

export const updateAsset = (id, data) => {
  return request.put(`/assets/${id}`, data)
}

export const deleteAsset = (id) => {
  return request.delete(`/assets/${id}`)
}

export const exportAssets = () => {
  return request.get('/assets/export', { responseType: 'blob' })
}

export const getAssetProjects = () => {
  return request.get('/assets/projects')
}

// 根据角色获取可见的资产
export const getRoleAssets = (role) => {
  return request.get(`/${role}/assets`)
}

// 获取所有资产（别名，兼容旧代码）
export const getAssets = (params) => {
  return getAssetList(params)
}