import axios from './axios'

/**
 * 获取所有备份列表
 */
export const getBackupList = () => {
  return axios.get('/backup/list')
}

/**
 * 获取备份详情
 * @param {number} id - 备份ID
 */
export const getBackupById = (id) => {
  return axios.get(`/backup/${id}`)
}

/**
 * 创建新备份
 * @param {object} data - 备份数据
 * @param {string} data.name - 备份名称
 * @param {string} data.type - 备份类型：full/database/files
 * @param {string} data.description - 备份描述
 * @param {string} data.createdBy - 创建者
 * @param {string} data.password - 可选，加密密码
 */
export const createBackup = (data) => {
  const params = new URLSearchParams()
  params.append('name', data.name)
  params.append('type', data.type)
  params.append('description', data.description || '')
  params.append('createdBy', data.createdBy)

  // ✅ 修复：密码为必需参数（安全加固），总是传递
  params.append('password', data.password || '')

  // ✅ 修复：显式转换为字符串，确保 axios 不会将其序列化为 JSON
  // ✅ 修复：设置1分钟超时，创建备份是异步操作但需要等待后端响应
  return axios.post('/backup/create', params.toString(), {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    timeout: 60 * 1000  // 1分钟 = 60秒
  })
}

/**
 * 恢复备份
 * @param {number} id - 备份ID
 * @param {string} password - 可选，解密密码（用于加密备份）
 */
export const restoreBackup = (id, password) => {
  const params = new URLSearchParams()

  // ✅ 修复：密码为必需参数（安全加固），总是传递
  params.append('password', password || '')

  // ✅ 修复：显式转换为字符串，确保 axios 不会将其序列化为 JSON
  return axios.post(`/backup/restore/${id}`, params.toString(), {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    // ✅ 修复：设置5分钟超时，恢复操作是异步长时间运行的后台任务
    timeout: 5 * 60 * 1000  // 5分钟 = 300秒
  })
}

/**
 * 删除备份
 * @param {number} id - 备份ID
 */
export const deleteBackup = (id) => {
  return axios.delete(`/backup/delete/${id}`)
}

/**
 * 下载备份文件
 * @param {number} id - 备份ID
 * @param {string} password - 备份密码（必需，用于验证）
 * @returns {Promise<Blob>} 文件Blob
 */
export const downloadBackup = (id, password) => {
  const params = new URLSearchParams()
  // ✅ 修复：密码为必需参数（安全加固），总是传递
  params.append('password', password || '')

  // ✅ 修复：显式转换为字符串，确保 axios 不会将其序列化为 JSON
  return axios.post(`/backup/download/${id}`, params.toString(), {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    responseType: 'blob',
    // ✅ 修复：设置30分钟超时，支持大文件下载
    timeout: 30 * 60 * 1000  // 30分钟 = 1800秒
  })
}

/**
 * 上传外部备份文件
 * @param {FormData} formData - 包含文件和元数据的FormData对象
 */
export const uploadBackup = (formData) => {
  return axios.post('/backup/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 查询备份状态
 * @param {number} id - 备份ID
 */
export const getBackupStatus = (id) => {
  return axios.get(`/backup/status/${id}`)
}

/**
 * 获取备份统计信息
 */
export const getBackupStatistics = () => {
  return axios.get('/backup/statistics')
}

/**
 * 验证备份文件完整性
 * @param {number} id - 备份ID
 */
export const validateBackup = (id) => {
  return axios.post(`/backup/validate/${id}`)
}

/**
 * 扫描物理备份文件并同步到数据库
 * ✅ 修复：用于恢复数据库后，重新导入物理文件的备份记录
 * @returns {Promise} 导入结果
 */
export const scanBackups = () => {
  return axios.post('/backup/scan')
}
