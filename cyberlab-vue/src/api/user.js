import request from './axios'

// 用户管理相关API

// ==================== 公共用户信息接口（所有认证用户可访问） ====================

/**
 * 获取用户列表（基本信息，不含密码）
 * 所有认证用户都可以访问，用于显示用户名等基本信息
 */
export const getUsers = (params) => {
  return request.get('/users', { params })
}

// ==================== 管理员专用接口 ====================

/**
 * 获取用户列表（完整信息，仅管理员可访问）
 */
export const getUserList = (params) => {
  return request.get('/admin/users', { params })
}

export const createUser = (data) => {
  return request.post('/admin/users', data)
}

export const updateUser = (id, data) => {
  return request.put(`/admin/users/${id}`, data)
}

export const deleteUser = (id) => {
  return request.delete(`/admin/users/${id}`)
}

export const toggleUserStatus = (id) => {
  return request.put(`/admin/users/${id}/toggle`)
}

// 获取当前用户信息
export const getCurrentUser = () => {
  return request.get('/auth/me')
}