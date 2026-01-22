/**
 * 设备类型管理 Composable
 * 提供设备类型的状态管理、CRUD操作、筛选和图例功能
 */

import { ref, computed, reactive } from 'vue'
import { 
  DefaultDeviceTypes, 
  DeviceType, 
  DeviceCategories,
  CategoryLabels,
  CategoryColors,
  LegendConfig,
  FilterConfig 
} from '@/types/deviceTypes'

// 全局状态
let deviceTypesState = null

export function useDeviceTypes() {
  // 单例模式，确保全局状态一致
  if (!deviceTypesState) {
    deviceTypesState = createDeviceTypesState()
  }

  return deviceTypesState
}

function createDeviceTypesState() {
  // 设备类型列表
  const deviceTypes = ref([...DefaultDeviceTypes])
  
  // 自定义设备类型
  const customDeviceTypes = ref([])
  
  // 当前激活的筛选器
  const activeFilters = reactive({
    categories: [],     // 激活的分类筛选
    tags: [],          // 激活的标签筛选
    search: '',        // 搜索关键词
    showHidden: false  // 是否显示隐藏的类型
  })
  
  // 图例配置
  const legendConfig = reactive({ ...LegendConfig })
  
  // 筛选配置
  const filterConfig = reactive({ ...FilterConfig })

  // 计算属性：所有设备类型（包含自定义）
  const allDeviceTypes = computed(() => {
    return [...deviceTypes.value, ...customDeviceTypes.value]
  })

  // 计算属性：按分类分组的设备类型
  const deviceTypesByCategory = computed(() => {
    const grouped = {}
    
    allDeviceTypes.value.forEach(type => {
      if (!grouped[type.category]) {
        grouped[type.category] = []
      }
      grouped[type.category].push(type)
    })

    // 按排序顺序排列每个分类内的设备
    Object.keys(grouped).forEach(category => {
      grouped[category].sort((a, b) => a.sortOrder - b.sortOrder)
    })

    return grouped
  })

  // 计算属性：筛选后的设备类型
  const filteredDeviceTypes = computed(() => {
    let filtered = allDeviceTypes.value

    // 分类筛选
    if (activeFilters.categories.length > 0) {
      filtered = filtered.filter(type => 
        activeFilters.categories.includes(type.category)
      )
    }

    // 标签筛选
    if (activeFilters.tags.length > 0) {
      filtered = filtered.filter(type =>
        activeFilters.tags.some(tag => type.tags.includes(tag))
      )
    }

    // 搜索筛选
    if (activeFilters.search) {
      const searchLower = activeFilters.search.toLowerCase()
      filtered = filtered.filter(type =>
        type.name.toLowerCase().includes(searchLower) ||
        type.description.toLowerCase().includes(searchLower) ||
        type.tags.some(tag => tag.toLowerCase().includes(searchLower))
      )
    }

    // 可见性筛选
    if (!activeFilters.showHidden) {
      filtered = filtered.filter(type => type.visible)
    }

    return filtered
  })

  // 计算属性：所有可用标签
  const availableTags = computed(() => {
    const tags = new Set()
    allDeviceTypes.value.forEach(type => {
      type.tags.forEach(tag => tags.add(tag))
    })
    return Array.from(tags).sort()
  })

  // 计算属性：设备类型统计
  const typeStatistics = computed(() => {
    const stats = {
      total: allDeviceTypes.value.length,
      visible: allDeviceTypes.value.filter(t => t.visible).length,
      byCategory: {}
    }

    Object.values(DeviceCategories).forEach(category => {
      const typesInCategory = allDeviceTypes.value.filter(t => t.category === category)
      stats.byCategory[category] = {
        total: typesInCategory.length,
        visible: typesInCategory.filter(t => t.visible).length,
        label: CategoryLabels[category],
        color: CategoryColors[category]
      }
    })

    return stats
  })

  // 方法：根据ID获取设备类型
  const getDeviceTypeById = (id) => {
    return allDeviceTypes.value.find(type => type.id === id)
  }

  // 方法：根据iconName获取设备类型
  const getDeviceTypeByIconName = (iconName) => {
    return allDeviceTypes.value.find(type => type.iconName === iconName)
  }

  // 方法：添加自定义设备类型
  const addCustomDeviceType = (typeData) => {
    const newType = new DeviceType({
      id: typeData.id || `custom_${Date.now()}`,
      ...typeData,
      category: typeData.category || DeviceCategories.CUSTOM
    })

    customDeviceTypes.value.push(newType)
    return newType
  }

  // 方法：更新设备类型
  const updateDeviceType = (id, updates) => {
    const type = getDeviceTypeById(id)
    if (type) {
      type.update(updates)
      return true
    }
    return false
  }

  // 方法：删除自定义设备类型
  const deleteCustomDeviceType = (id) => {
    const index = customDeviceTypes.value.findIndex(type => type.id === id)
    if (index > -1) {
      customDeviceTypes.value.splice(index, 1)
      return true
    }
    return false
  }

  // 方法：设置设备类型可见性
  const setDeviceTypeVisibility = (id, visible) => {
    return updateDeviceType(id, { visible })
  }

  // 方法：批量设置分类可见性
  const setCategoryVisibility = (category, visible) => {
    allDeviceTypes.value
      .filter(type => type.category === category)
      .forEach(type => type.update({ visible }))
  }

  // 方法：添加筛选器
  const addCategoryFilter = (category) => {
    if (!activeFilters.categories.includes(category)) {
      activeFilters.categories.push(category)
    }
  }

  const addTagFilter = (tag) => {
    if (!activeFilters.tags.includes(tag)) {
      activeFilters.tags.push(tag)
    }
  }

  // 方法：移除筛选器
  const removeCategoryFilter = (category) => {
    const index = activeFilters.categories.indexOf(category)
    if (index > -1) {
      activeFilters.categories.splice(index, 1)
    }
  }

  const removeTagFilter = (tag) => {
    const index = activeFilters.tags.indexOf(tag)
    if (index > -1) {
      activeFilters.tags.splice(index, 1)
    }
  }

  // 方法：清空所有筛选器
  const clearAllFilters = () => {
    activeFilters.categories = []
    activeFilters.tags = []
    activeFilters.search = ''
  }

  // 方法：设置搜索关键词
  const setSearchKeyword = (keyword) => {
    activeFilters.search = keyword
  }

  // 方法：切换筛选器状态
  const toggleCategoryFilter = (category) => {
    if (activeFilters.categories.includes(category)) {
      removeCategoryFilter(category)
    } else {
      addCategoryFilter(category)
    }
  }

  const toggleTagFilter = (tag) => {
    if (activeFilters.tags.includes(tag)) {
      removeTagFilter(tag)
    } else {
      addTagFilter(tag)
    }
  }

  // 方法：更新图例配置
  const updateLegendConfig = (config) => {
    Object.assign(legendConfig, config)
  }

  // 方法：更新筛选配置
  const updateFilterConfig = (config) => {
    Object.assign(filterConfig, config)
  }

  // 方法：导出设备类型配置
  const exportDeviceTypes = () => {
    return {
      deviceTypes: deviceTypes.value,
      customDeviceTypes: customDeviceTypes.value,
      legendConfig: { ...legendConfig },
      filterConfig: { ...filterConfig }
    }
  }

  // 方法：导入设备类型配置
  const importDeviceTypes = (data) => {
    if (data.deviceTypes) {
      deviceTypes.value = data.deviceTypes
    }
    if (data.customDeviceTypes) {
      customDeviceTypes.value = data.customDeviceTypes
    }
    if (data.legendConfig) {
      Object.assign(legendConfig, data.legendConfig)
    }
    if (data.filterConfig) {
      Object.assign(filterConfig, data.filterConfig)
    }
  }

  // 方法：重置为默认配置
  const resetToDefaults = () => {
    deviceTypes.value = [...DefaultDeviceTypes]
    customDeviceTypes.value = []
    clearAllFilters()
    Object.assign(legendConfig, LegendConfig)
    Object.assign(filterConfig, FilterConfig)
  }

  return {
    // 状态
    deviceTypes,
    customDeviceTypes,
    activeFilters,
    legendConfig,
    filterConfig,

    // 计算属性
    allDeviceTypes,
    deviceTypesByCategory,
    filteredDeviceTypes,
    availableTags,
    typeStatistics,

    // 查询方法
    getDeviceTypeById,
    getDeviceTypeByIconName,

    // CRUD方法
    addCustomDeviceType,
    updateDeviceType,
    deleteCustomDeviceType,
    setDeviceTypeVisibility,
    setCategoryVisibility,

    // 筛选方法
    addCategoryFilter,
    addTagFilter,
    removeCategoryFilter,
    removeTagFilter,
    clearAllFilters,
    setSearchKeyword,
    toggleCategoryFilter,
    toggleTagFilter,

    // 配置方法
    updateLegendConfig,
    updateFilterConfig,

    // 数据管理方法
    exportDeviceTypes,
    importDeviceTypes,
    resetToDefaults
  }
}