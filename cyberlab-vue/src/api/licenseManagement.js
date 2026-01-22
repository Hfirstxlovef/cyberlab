/**
 * 授权管理 API 服务
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Management API for CyberLab Platform
 *
 * hongan 用户专用授权管理接口
 */

import axios from 'axios'

// 基础路径
const BASE_URL = '/api/licenses/manage'

/**
 * 生成新授权
 * @param {Object} data - 授权请求数据
 * @param {string} data.issuedTo - 授权给（客户名称）
 * @param {string} data.edition - 版本（PRO, ENTERPRISE, TRIAL）
 * @param {string} data.expiryDate - 过期日期（YYYY-MM-DD）
 * @param {string} data.notes - 备注
 * @returns {Promise}
 */
export function generateLicense(data) {
  return axios.post(`${BASE_URL}/generate`, data)
}

/**
 * 获取所有授权列表
 * @returns {Promise}
 */
export function getLicenseList() {
  return axios.get(`${BASE_URL}/list`)
}

/**
 * 搜索授权
 * @param {string} keyword - 搜索关键词
 * @returns {Promise}
 */
export function searchLicenses(keyword) {
  return axios.get(`${BASE_URL}/search`, {
    params: { keyword }
  })
}

/**
 * 获取当前激活的授权
 * @returns {Promise}
 */
export function getCurrentLicense() {
  return axios.get(`${BASE_URL}/current`)
}

/**
 * 设置为当前授权（触发双表同步）
 * @param {number} id - 授权 ID
 * @returns {Promise}
 */
export function setCurrentLicense(id) {
  return axios.put(`${BASE_URL}/${id}/set-current`)
}

/**
 * 延长授权有效期
 * @param {number} id - 授权 ID
 * @param {string} newExpiryDate - 新过期日期（YYYY-MM-DD）
 * @returns {Promise}
 */
export function extendLicense(id, newExpiryDate) {
  return axios.put(`${BASE_URL}/${id}/extend`, null, {
    params: { newExpiryDate }
  })
}

/**
 * 激活授权
 * @param {number} id - 授权 ID
 * @returns {Promise}
 */
export function activateLicense(id) {
  return axios.put(`${BASE_URL}/${id}/activate`)
}

/**
 * 停用授权
 * @param {number} id - 授权 ID
 * @returns {Promise}
 */
export function deactivateLicense(id) {
  return axios.put(`${BASE_URL}/${id}/deactivate`)
}

/**
 * 删除授权
 * @param {number} id - 授权 ID
 * @returns {Promise}
 */
export function deleteLicense(id) {
  return axios.delete(`${BASE_URL}/${id}`)
}

/**
 * 通过授权码激活系统（新增接口）
 * @param {Object} data - 激活请求数据
 * @param {string} data.serialNumber - 序列号
 * @param {string} data.licenseCode - 授权码
 * @returns {Promise}
 */
export function activateByCode(data) {
  return axios.post(`${BASE_URL}/activate-by-code`, data)
}
