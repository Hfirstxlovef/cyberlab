/**
 * 设备类型和标签管理
 * 用于图例筛选和样式分类
 */

// 预定义设备类型分类
export const DeviceCategories = {
  TERMINAL: 'terminal',      // 终端设备
  SERVER: 'server',          // 服务器类
  NETWORK: 'network',        // 网络设备
  SECURITY: 'security',      // 安全设备
  SERVICE: 'service',        // 服务类
  CUSTOM: 'custom'           // 自定义类型
}

// 分类中文名称映射
export const CategoryLabels = {
  [DeviceCategories.TERMINAL]: '终端设备',
  [DeviceCategories.SERVER]: '服务器',
  [DeviceCategories.NETWORK]: '网络设备', 
  [DeviceCategories.SECURITY]: '安全设备',
  [DeviceCategories.SERVICE]: '服务',
  [DeviceCategories.CUSTOM]: '自定义'
}

// 分类颜色主题
export const CategoryColors = {
  [DeviceCategories.TERMINAL]: '#409EFF',    // 蓝色
  [DeviceCategories.SERVER]: '#67C23A',      // 绿色
  [DeviceCategories.NETWORK]: '#E6A23C',     // 橙色
  [DeviceCategories.SECURITY]: '#F56C6C',    // 红色
  [DeviceCategories.SERVICE]: '#909399',     // 灰色
  [DeviceCategories.CUSTOM]: '#6B73FF'       // 紫色
}

// 预定义设备类型配置
export const DefaultDeviceTypes = [
  // 终端设备
  {
    id: 'laptop',
    name: '笔记本',
    iconName: 'laptop',
    category: DeviceCategories.TERMINAL,
    color: CategoryColors[DeviceCategories.TERMINAL],
    tags: ['终端', '移动设备'],
    description: '便携式计算机设备'
  },
  {
    id: 'pc',
    name: 'PC',
    iconName: 'pc', 
    category: DeviceCategories.TERMINAL,
    color: CategoryColors[DeviceCategories.TERMINAL],
    tags: ['终端', '桌面设备'],
    description: '个人计算机'
  },
  // 服务器类
  {
    id: 'webserver',
    name: 'Web服务器',
    iconName: 'webserver',
    category: DeviceCategories.SERVER,
    color: CategoryColors[DeviceCategories.SERVER],
    tags: ['服务器', 'Web服务', 'HTTP'],
    description: 'Web应用服务器'
  },
  {
    id: 'storage_server',
    name: '存储服务器',
    iconName: 'storage_server',
    category: DeviceCategories.SERVER,
    color: CategoryColors[DeviceCategories.SERVER],
    tags: ['服务器', '存储', '数据'],
    description: '数据存储服务器'
  },
  {
    id: 'database',
    name: '数据库',
    iconName: 'database',
    category: DeviceCategories.SERVER,
    color: CategoryColors[DeviceCategories.SERVER],
    tags: ['服务器', '数据库', 'SQL'],
    description: '数据库服务器'
  },
  {
    id: 'mail_server',
    name: '邮件服务器',
    iconName: 'mail_server',
    category: DeviceCategories.SERVER,
    color: CategoryColors[DeviceCategories.SERVER],
    tags: ['服务器', '邮件', 'SMTP'],
    description: '邮件服务器'
  },
  {
    id: 'pcserver',
    name: 'PC服务器',
    iconName: 'pcserver',
    category: DeviceCategories.SERVER,
    color: CategoryColors[DeviceCategories.SERVER],
    tags: ['服务器', '通用服务器'],
    description: 'PC服务器'
  },
  // 网络设备
  {
    id: 'router',
    name: '路由器',
    iconName: 'router',
    category: DeviceCategories.NETWORK,
    color: CategoryColors[DeviceCategories.NETWORK],
    tags: ['网络设备', '路由', '三层'],
    description: '网络路由器'
  },
  {
    id: 'main_switch',
    name: '主交换机',
    iconName: 'main_switch',
    category: DeviceCategories.NETWORK,
    color: CategoryColors[DeviceCategories.NETWORK],
    tags: ['网络设备', '交换机', '核心'],
    description: '核心交换机'
  },
  {
    id: 'fiber_switch',
    name: '光纤交换机',
    iconName: 'fiber_switch',
    category: DeviceCategories.NETWORK,
    color: CategoryColors[DeviceCategories.NETWORK],
    tags: ['网络设备', '交换机', '光纤'],
    description: '光纤交换机'
  },
  {
    id: 'ethernet_switch',
    name: '以太网交换机',
    iconName: 'ethernet_switch',
    category: DeviceCategories.NETWORK,
    color: CategoryColors[DeviceCategories.NETWORK],
    tags: ['网络设备', '交换机', '以太网'],
    description: '以太网交换机'
  },
  // 安全设备
  {
    id: 'firewall',
    name: '防火墙',
    iconName: 'firewall',
    category: DeviceCategories.SECURITY,
    color: CategoryColors[DeviceCategories.SECURITY],
    tags: ['安全设备', '防火墙', '安全'],
    description: '网络防火墙'
  },
  // 服务类
  {
    id: 'dns',
    name: 'DNS',
    iconName: 'dns',
    category: DeviceCategories.SERVICE,
    color: CategoryColors[DeviceCategories.SERVICE],
    tags: ['服务', 'DNS', '域名解析'],
    description: 'DNS服务器'
  }
]

// 设备类型数据结构
export class DeviceType {
  constructor({
    id,
    name,
    iconName,
    category = DeviceCategories.CUSTOM,
    color = CategoryColors[DeviceCategories.CUSTOM],
    tags = [],
    description = '',
    visible = true,
    sortOrder = 0
  }) {
    this.id = id
    this.name = name
    this.iconName = iconName
    this.category = category
    this.color = color
    this.tags = Array.isArray(tags) ? tags : []
    this.description = description
    this.visible = visible
    this.sortOrder = sortOrder
    this.createdAt = new Date().toISOString()
    this.updatedAt = new Date().toISOString()
  }

  // 添加标签
  addTag(tag) {
    if (!this.tags.includes(tag)) {
      this.tags.push(tag)
      this.updatedAt = new Date().toISOString()
    }
  }

  // 移除标签
  removeTag(tag) {
    const index = this.tags.indexOf(tag)
    if (index > -1) {
      this.tags.splice(index, 1)
      this.updatedAt = new Date().toISOString()
    }
  }

  // 更新属性
  update(updates) {
    Object.assign(this, updates)
    this.updatedAt = new Date().toISOString()
  }
}

// 图例配置
export const LegendConfig = {
  position: 'top-right',    // 图例位置
  visible: true,            // 是否显示图例
  collapsible: true,        // 是否可折叠
  groupByCategory: true,    // 是否按分类分组
  showCount: true,          // 是否显示数量统计
  filterEnabled: true       // 是否启用筛选功能
}

// 筛选配置
export const FilterConfig = {
  enableCategoryFilter: true,   // 启用分类筛选
  enableTagFilter: true,        // 启用标签筛选
  enableCustomFilter: true,     // 启用自定义筛选
  multiSelect: true            // 支持多选
}