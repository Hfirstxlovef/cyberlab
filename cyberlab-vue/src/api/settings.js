import axios from './axios'

// 获取所有系统设置
export const getSystemSettings = () => {
  return axios.get('/settings')
}

// 保存系统设置
export const saveSystemSettings = (settings) => {
  return axios.post('/settings', settings)
}

// 上传Logo
export const uploadLogo = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return axios.post('/settings/upload/logo', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取单个设置
export const getSetting = (key) => {
  return axios.get(`/settings/${key}`)
}