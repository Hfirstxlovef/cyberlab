import request from './axios'

// 场景模板管理相关API
export const getScenarioTemplateList = (params) => {
  return request.get('/scenario-templates', { params })
}

export const getScenarioTemplate = (id) => {
  return request.get(`/scenario-templates/${id}`)
}

export const createScenarioTemplate = (data) => {
  return request.post('/scenario-templates', data)
}

export const updateScenarioTemplate = (id, data) => {
  return request.put(`/scenario-templates/${id}`, data)
}

export const deleteScenarioTemplate = (id) => {
  return request.delete(`/scenario-templates/${id}`)
}

export const activateScenarioTemplate = (id) => {
  return request.post(`/scenario-templates/${id}/activate`)
}

export const deactivateScenarioTemplate = (id) => {
  return request.post(`/scenario-templates/${id}/deactivate`)
}

export const getTemplateAssets = (templateId) => {
  return request.get(`/scenario-templates/${templateId}/assets`)
}