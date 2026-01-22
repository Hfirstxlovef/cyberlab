import request from './axios'

// 战队管理相关API

// ==================== Team CRUD ====================

// 获取所有战队（可按类型筛选）
export const getTeamList = (params) => {
  return request.get('/teams', { params })
}

// 获取活跃战队（按类型）
export const getActiveTeams = (teamType) => {
  return request.get('/teams/active', { params: { teamType } })
}

// 根据ID获取战队详情
export const getTeamById = (id) => {
  return request.get(`/teams/${id}`)
}

// 创建战队
export const createTeam = (data) => {
  return request.post('/teams', data)
}

// 更新战队信息
export const updateTeam = (id, data) => {
  return request.put(`/teams/${id}`, data)
}

// 删除战队
export const deleteTeam = (id) => {
  return request.delete(`/teams/${id}`)
}

// 切换战队状态
export const toggleTeamStatus = (id) => {
  return request.put(`/teams/${id}/toggle`)
}

// ==================== Team Member Management ====================

// 获取战队成员列表
export const getTeamMembers = (teamId) => {
  return request.get(`/teams/${teamId}/members`)
}

// 添加战队成员
export const addTeamMember = (teamId, data) => {
  return request.post(`/teams/${teamId}/members`, data)
}

// 移除战队成员
export const removeTeamMember = (teamId, userId) => {
  return request.delete(`/teams/${teamId}/members/${userId}`)
}

// 获取战队成员数量
export const getTeamMemberCount = (teamId) => {
  return request.get(`/teams/${teamId}/members/count`)
}

// 根据用户ID获取所在战队
export const getTeamByUserId = (userId) => {
  return request.get(`/teams/user/${userId}`)
}

// ==================== Statistics ====================

// 统计战队数量
export const countTeams = (teamType) => {
  return request.get('/teams/count', { params: { teamType } })
}

// ==================== Team Applications ====================

// 提交加入战队申请
export const submitApplication = (data) => {
  return request.post('/team-applications', data)
}

// 批准申请
export const approveApplication = (id) => {
  return request.post(`/team-applications/${id}/approve`)
}

// 拒绝申请
export const rejectApplication = (id, data) => {
  return request.post(`/team-applications/${id}/reject`, data)
}

// 撤回申请
export const withdrawApplication = (id) => {
  return request.delete(`/team-applications/${id}`)
}

// 获取战队的所有申请
export const getTeamApplications = (teamId) => {
  return request.get(`/team-applications/team/${teamId}`)
}

// 获取战队的待处理申请
export const getPendingApplicationsByTeam = (teamId) => {
  return request.get(`/team-applications/team/${teamId}/pending`)
}

// 获取当前用户的所有申请
export const getMyApplications = () => {
  return request.get('/team-applications/my')
}

// 获取当前用户的待处理申请
export const getMyPendingApplications = () => {
  return request.get('/team-applications/my/pending')
}

// 根据ID获取申请详情
export const getApplicationById = (id) => {
  return request.get(`/team-applications/${id}`)
}

// 统计战队的待处理申请数量
export const countPendingApplications = (teamId) => {
  return request.get(`/team-applications/team/${teamId}/pending/count`)
}

// 检查当前用户是否有待处理的申请
export const hasPendingApplication = () => {
  return request.get('/team-applications/my/pending/exists')
}
