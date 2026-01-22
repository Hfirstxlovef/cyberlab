import request from './axios'

// 认证相关API
export const login = (data) => {
  return request.post('/auth/login', data)
}

export const register = (data) => {
  return request.post('/auth/register', data)
}

export const logout = () => {
  return request.post('/auth/logout')
}

// 刷新令牌
export const refreshToken = (refreshToken) => {
  return request.post('/auth/refresh', null, {
    headers: {
      'Authorization': `Bearer ${refreshToken}`
    }
  })
}