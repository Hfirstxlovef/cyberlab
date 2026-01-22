import { getSystemSettings } from '@/api/system'

// 系统设置缓存
let systemSettings = null
let settingsPromise = null

// 获取系统设置（带缓存）
const getSettings = async () => {
  if (systemSettings) {
    return systemSettings
  }
  
  if (settingsPromise) {
    return settingsPromise
  }
  
  settingsPromise = getSystemSettings()
    .then(response => {
      systemSettings = response.data || response
      return systemSettings
    })
    .catch(error => {
      // 返回默认设置
      systemSettings = {
        website_title: 'CyberLab网络空间安全攻防实验室',
        login_page_title: '红岸网络空间安全对抗平台',
        sidebar_title: 'CyberLab',
        system_logo: '/logo.png'
      }
      return systemSettings
    })
    .finally(() => {
      settingsPromise = null
    })
  
  return settingsPromise
}

// 设置页面标题
export const setPageTitle = async (roleTitle = '') => {
  try {
    const settings = await getSettings()
    // 优先使用新的website_title字段，向后兼容login_title
    const baseTitle = settings.website_title || settings.login_title || 'CyberLab网络空间安全攻防实验室'
    
    let finalTitle = baseTitle
    if (roleTitle) {
      finalTitle = `${baseTitle} - ${roleTitle}`
    }
    
    document.title = finalTitle
    
    return finalTitle
  } catch (error) {
    const fallbackTitle = roleTitle ? `CyberLab - ${roleTitle}` : 'CyberLab网络空间安全攻防实验室'
    document.title = fallbackTitle
    return fallbackTitle
  }
}

// 设置页面图标
export const setPageIcon = async () => {
  try {
    const settings = await getSettings()
    const iconUrl = settings.system_logo || '/logo.png'
    
    // 查找现有的favicon链接
    let iconLink = document.querySelector('link[rel="icon"]')
    
    if (!iconLink) {
      // 如果不存在，创建新的favicon链接
      iconLink = document.createElement('link')
      iconLink.rel = 'icon'
      iconLink.type = 'image/png'
      document.head.appendChild(iconLink)
    }
    
    // 设置图标URL
    iconLink.href = iconUrl
    
    return iconUrl
  } catch (error) {
    return '/logo.png'
  }
}

// 根据角色设置大屏幕标题和图标
export const setBigScreenPageMeta = async (role) => {
  const roleMap = {
    admin: '管理员大屏',
    red: '红队大屏', 
    blue: '蓝队大屏',
    judge: '裁判大屏'
  }
  
  const roleTitle = roleMap[role] || '大屏模式'
  
  try {
    // 同时设置标题和图标
    await Promise.all([
      setPageTitle(roleTitle),
      setPageIcon()
    ])
    
  } catch (error) {
  }
}

// 初始化页面设置（在应用启动时调用）
export const initPageSettings = async () => {
  try {
    // 预加载系统设置
    await getSettings()
    
    // 设置默认标题和图标
    await Promise.all([
      setPageTitle(),
      setPageIcon()
    ])
    
  } catch (error) {
  }
}

// 清除缓存（在系统设置更新时调用）
export const clearSettingsCache = () => {
  systemSettings = null
  settingsPromise = null
}