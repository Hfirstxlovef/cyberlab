import request from './axios'

// 演练管理相关API
export const getAllRanges = () => {
  return request.get('/range')
}

// 获取正在运行的演练（用于成果提交页面）
export const getActiveRanges = () => {
  return request.get('/range/active')
}

export const getRangeById = (id) => {
  return request.get(`/range/${id}`)
}

export const createRange = (data) => {
  return request.post('/range/create', data)
}

export const pauseRange = (id) => {
  return request.put(`/range/pause/${id}`)
}

export const startRange = (id) => {
  return request.put(`/range/start/${id}`)
}

export const stopRange = (id) => {
  return request.put(`/range/stop/${id}`)
}

export const deleteRange = (id) => {
  return request.delete(`/range/delete/${id}`)
}