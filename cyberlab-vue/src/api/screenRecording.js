import axios from './axios'

/**
 * 录屏相关 API
 */

/**
 * 获取所有已完成的录屏列表
 */
export const getAllRecordings = () => {
  return axios.get('/screen-recording/search')
}

/**
 * 按用户名搜索录屏
 */
export const getRecordingsByUsername = (username) => {
  return axios.get('/screen-recording/search', {
    params: { username }
  })
}

/**
 * 按时间范围和用户名搜索录屏
 */
export const searchRecordings = (params) => {
  return axios.get('/screen-recording/search', {
    params: {
      username: params.username,
      startTime: params.startTime,
      endTime: params.endTime
    }
  })
}

/**
 * 获取录屏详情
 */
export const getRecordingDetail = (id) => {
  return axios.get(`/screen-recording/${id}`)
}

/**
 * 获取视频流式播放 URL
 */
export const getVideoStreamUrl = (id) => {
  return `/api/screen-recording/${id}/stream`
}

/**
 * 获取视频下载 URL
 */
export const getVideoDownloadUrl = (id) => {
  return `/api/screen-recording/${id}/download`
}

/**
 * 下载录屏文件
 */
export const downloadRecording = (id, fileName) => {
  const url = getVideoDownloadUrl(id)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || `recording_${id}.webm`
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/**
 * 清理旧录屏（仅管理员）
 */
export const cleanupOldRecordings = (daysToKeep = 30) => {
  return axios.delete('/screen-recording/cleanup', {
    params: { daysToKeep }
  })
}
