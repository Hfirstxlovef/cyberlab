export const iconMap = {
  'laptop': '/icons/laptop.png',
  'storage_server': '/icons/storage_server.png',
  'fiber_switch': '/icons/fiber_switch.png',
  'ethernet_switch': '/icons/ethernet_switch.png',
  'mail_server': '/icons/mail_server.png',
  'main_switch': '/icons/main_switch.png',
  'database': '/icons/database.png',
  'dns': '/icons/DNS.png',
  'firewall': '/icons/firewall.png',
  'pc': '/icons/pc.png',
  'pcserver': '/icons/PcServer.png',
  'webserver': '/icons/webserver.png',
  'router': '/icons/router.png'
}

// 添加默认fallback图标
export const defaultIcon = '/icons/pc.png'

// 图标预加载函数
export function preloadIcons() {
  return Promise.all(
    Object.values(iconMap).map(iconPath => {
      return new Promise((resolve, reject) => {
        const img = new Image()
        img.onload = () => {
          resolve(iconPath)
        }
        img.onerror = () => {
          resolve(iconPath) // 即使失败也resolve，避免阻塞
        }
        img.src = iconPath
      })
    })
  )
}