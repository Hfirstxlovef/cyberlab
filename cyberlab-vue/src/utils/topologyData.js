import { iconMap } from '@/assets/icons/iconMap'

/**
 * 标准化节点数据结构
 */
export const withDefaultPosition = (nodes) => {
  if (!Array.isArray(nodes)) {
    return []
  }

  const result = nodes.map((node, index) => {
    // 确保节点数据结构统一
    const normalizedNode = {
      id: node.id || `${Date.now()}_${index}`,
      name: node.name || '未命名节点',
      iconName: node.iconName || 'pc',
      x: node.x != null ? node.x : 100 + (index % 5) * 150,
      y: node.y != null ? node.y : 100 + Math.floor(index / 5) * 120,
      comment: node.comment || '',
      ...node
    }

    const iconPath = getIconByName(normalizedNode.iconName)
    const fullIconUrl = `${window.location.origin}${iconPath}`

    // 使用 ECharts 标准的 image:// 前缀格式
    const symbolUrl = `image://${fullIconUrl}`

    const processedNode = {
      ...normalizedNode,
      symbol: symbolUrl,
      symbolKeepAspect: true,
      symbolSize: normalizedNode.symbolSize || [60, 60],
      draggable: true,
      // 添加备选的内置 symbol，以防图标加载失败
      itemStyle: {
        color: getNodeColor(normalizedNode.iconName),
        borderColor: '#333',
        borderWidth: 2
      },
      // 添加标签样式，让设备类型更清晰
      label: {
        show: true,
        position: 'bottom',
        fontSize: 12,
        color: '#333',
        fontWeight: 'bold'
      }
    }

    return processedNode
  })

  return result
}

/**
 * 根据图标名称获取图标路径
 */
export const getIconByName = (name) => {
  if (!name || typeof name !== 'string') return '/icons/default.png'
  const filename = iconMap[name] || `${name}.png`
  return `/icons/${filename.replace(/^.*[/]/, '')}`
}

/**
 * 根据设备类型获取节点颜色
 */
const getNodeColor = (iconName) => {
  const colorMap = {
    'laptop': '#4CAF50',
    'pc': '#2196F3',
    'webserver': '#FF9800',
    'database': '#9C27B0',
    'router': '#F44336',
    'switch': '#607D8B',
    'firewall': '#E91E63',
    'dns': '#00BCD4'
  }
  return colorMap[iconName] || '#666666'
}



/**
 * 获取所有可用图标列表
 */
export const getAvailableIcons = () => {
  return [
    { label: '笔记本', type: 'laptop', iconName: 'laptop' },
    { label: 'PC', type: 'pc', iconName: 'pc' },
    { label: 'Web服务器', type: 'webserver', iconName: 'webserver' },
    { label: '存储服务器', type: 'storage_server', iconName: 'storage_server' },
    { label: '数据库', type: 'database', iconName: 'database' },
    { label: 'DNS', type: 'dns', iconName: 'dns' },
    { label: '防火墙', type: 'firewall', iconName: 'firewall' },
    { label: '邮件服务器', type: 'mail_server', iconName: 'mail_server' },
    { label: '主交换机', type: 'main_switch', iconName: 'main_switch' },
    { label: '光纤交换机', type: 'fiber_switch', iconName: 'fiber_switch' },
    { label: '以太网交换机', type: 'ethernet_switch', iconName: 'ethernet_switch' },
    { label: '路由器', type: 'router', iconName: 'router' }
  ]
}