// Token 管理工具函数

// 存储策略枚举
const StorageType = {
  LOCAL: 'localStorage',
  SESSION: 'sessionStorage'
}

// 获取当前使用的存储类型
function getCurrentStorageType() {
  // 如果两个存储中都有 token，优先使用 localStorage
  if (localStorage.getItem('token')) {
    return StorageType.LOCAL
  }
  if (sessionStorage.getItem('token')) {
    return StorageType.SESSION
  }
  return StorageType.LOCAL // 默认使用 localStorage
}

// 获取存储对象
function getStorage(storageType = null) {
  const type = storageType || getCurrentStorageType()
  return type === StorageType.LOCAL ? localStorage : sessionStorage
}

// 获取存储的 token
export function getToken() {
  // 检查两个存储位置
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  return token
}

// 设置 token（支持存储策略选择）
export function setToken(token, rememberMe = true) {
  // 清除其他存储位置的旧令牌
  clearUserInfo()
  
  const storage = rememberMe ? localStorage : sessionStorage
  storage.setItem('token', token)
  storage.setItem('storage_type', rememberMe ? StorageType.LOCAL : StorageType.SESSION)
}

// 获取刷新令牌
export function getRefreshToken() {
  return localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken')
}

// 设置刷新令牌
export function setRefreshToken(refreshToken, rememberMe = true) {
  const storage = getStorage(rememberMe ? StorageType.LOCAL : StorageType.SESSION)
  storage.setItem('refreshToken', refreshToken)
}

// 移除刷新令牌
export function removeRefreshToken() {
  localStorage.removeItem('refreshToken')
  sessionStorage.removeItem('refreshToken')
}

// 移除 token
export function removeToken() {
  localStorage.removeItem('token')
  sessionStorage.removeItem('token')
}

// 检查是否已登录
export function isLoggedIn() {
  return !!getToken()
}

// 获取用户角色
export function getUserRole() {
  return localStorage.getItem('role') || sessionStorage.getItem('role')
}

// 设置用户角色
export function setUserRole(role, rememberMe = true) {
  const storage = getStorage(rememberMe ? StorageType.LOCAL : StorageType.SESSION)
  storage.setItem('role', role)
}

// 获取用户名
export function getUsername() {
  return localStorage.getItem('username') || sessionStorage.getItem('username')
}

// 设置用户名
export function setUsername(username, rememberMe = true) {
  const storage = getStorage(rememberMe ? StorageType.LOCAL : StorageType.SESSION)
  storage.setItem('username', username)
}

// 获取用户ID
export function getUserId() {
  const userId = localStorage.getItem('userId') || sessionStorage.getItem('userId')
  return userId ? parseInt(userId, 10) : null
}

// 设置用户ID
export function setUserId(userId, rememberMe = true) {
  const storage = getStorage(rememberMe ? StorageType.LOCAL : StorageType.SESSION)
  storage.setItem('userId', userId.toString())
}

// 获取权限信息
export function getAuthorities() {
  const authorities = localStorage.getItem('authorities') || sessionStorage.getItem('authorities')
  return authorities ? JSON.parse(authorities) : []
}

// 设置权限信息
export function setAuthorities(authorities, rememberMe = true) {
  const storage = getStorage(rememberMe ? StorageType.LOCAL : StorageType.SESSION)
  storage.setItem('authorities', JSON.stringify(authorities))
}

// 获取当前存储类型
export function getStorageType() {
  return localStorage.getItem('storage_type') || sessionStorage.getItem('storage_type') || StorageType.LOCAL
}

// 清除所有用户信息
export function clearUserInfo() {
  // 清除 localStorage 中的用户信息
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('role')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  localStorage.removeItem('authorities')
  localStorage.removeItem('storage_type')

  // 清除 sessionStorage 中的用户信息
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('refreshToken')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('username')
  sessionStorage.removeItem('userId')
  sessionStorage.removeItem('authorities')
  sessionStorage.removeItem('storage_type')
}

// 检查令牌是否过期（简单检查）
export function isTokenExpired() {
  const token = getToken()
  if (!token) return true
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const currentTime = Date.now() / 1000
    return payload.exp < currentTime
  } catch (error) {
    return true
  }
}

// 获取令牌过期时间
export function getTokenExpirationTime() {
  const token = getToken()
  if (!token) return null
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 // 转换为毫秒
  } catch (error) {
    return null
  }
}