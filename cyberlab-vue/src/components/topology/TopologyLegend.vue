<template>
  <div 
    class="topology-legend" 
    :class="{ 
      collapsed: isCollapsed, 
      [`position-${position}`]: true 
    }"
  >
    <!-- 图例头部 -->
    <div class="legend-header">
      <div class="legend-title">
        <el-icon><Grid /></el-icon>
        <span>设备图例</span>
        <el-badge :value="visibleTypeCount" class="type-count-badge" />
      </div>
      <div class="legend-actions">
        <el-tooltip content="管理设备类型">
          <el-button 
            size="small" 
            :icon="Setting" 
            circle 
            @click="showTypeManager"
          />
        </el-tooltip>
        <el-tooltip :content="isCollapsed ? '展开图例' : '折叠图例'">
          <el-button 
            size="small" 
            :icon="isCollapsed ? ArrowDown : ArrowUp" 
            circle 
            @click="toggleCollapse"
          />
        </el-tooltip>
      </div>
    </div>

    <!-- 图例内容 -->
    <div v-show="!isCollapsed" class="legend-content">
      <!-- 筛选控制 -->
      <div v-if="showFilters" class="legend-filters">
        <div class="filter-section">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索设备类型..."
            size="small"
            :prefix-icon="Search"
            clearable
            @input="handleSearch"
          />
        </div>
        <div class="filter-section">
          <el-button 
            size="small" 
            @click="showAllTypes"
            :disabled="!hasActiveFilters"
          >
            显示全部
          </el-button>
          <el-button 
            size="small" 
            @click="hideAllTypes"
          >
            隐藏全部
          </el-button>
        </div>
      </div>

      <!-- 设备类型列表 -->
      <div class="legend-types">
        <div 
          v-for="(types, category) in groupedVisibleTypes" 
          :key="category"
          class="category-section"
        >
          <!-- 分类标题 -->
          <div class="category-header" @click="toggleCategory(category)">
            <div class="category-info">
              <el-icon class="category-icon" :style="{ color: CategoryColors[category] }">
                <CaretRight v-if="collapsedCategories.includes(category)" />
                <CaretBottom v-else />
              </el-icon>
              <span class="category-name" :style="{ color: CategoryColors[category] }">
                {{ CategoryLabels[category] }}
              </span>
              <el-badge :value="types.length" class="category-count" />
            </div>
            <div class="category-actions">
              <el-switch
                :model-value="isCategoryVisible(category)"
                @change="(val) => toggleCategoryFilter(category, val)"
                size="small"
                @click.stop
              />
            </div>
          </div>

          <!-- 设备类型项 -->
          <div 
            v-show="!collapsedCategories.includes(category)"
            class="type-items"
          >
            <div
              v-for="type in types"
              :key="type.id"
              class="type-item"
              :class="{ 
                'filtered': isTypeFiltered(type),
                'highlighted': highlightedTypes.includes(type.id)
              }"
              @click="toggleTypeFilter(type)"
              @mouseenter="highlightType(type)"
              @mouseleave="unhighlightType(type)"
            >
              <div class="type-icon">
                <img :src="getIconPath(type.iconName)" :alt="type.name" />
              </div>
              <div class="type-info">
                <div class="type-name">{{ type.name }}</div>
                <div class="type-count">{{ getTypeNodeCount(type) }}</div>
              </div>
              <div class="type-status">
                <el-icon v-if="isTypeFiltered(type)" class="filter-icon">
                  <View />
                </el-icon>
                <el-icon v-else class="filter-icon inactive">
                  <Hide />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div v-if="showStatistics" class="legend-statistics">
        <div class="stat-item">
          <span class="stat-label">总节点:</span>
          <span class="stat-value">{{ totalNodeCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">可见:</span>
          <span class="stat-value">{{ visibleNodeCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">类型:</span>
          <span class="stat-value">{{ visibleTypeCount }}</span>
        </div>
      </div>
    </div>

    <!-- 设备类型管理对话框 -->
    <DeviceTypeManager
      v-model="typeManagerVisible"
      @save="handleTypesUpdated"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { 
  Grid, 
  Setting, 
  ArrowDown, 
  ArrowUp, 
  Search,
  CaretRight,
  CaretBottom,
  View,
  Hide
} from '@element-plus/icons-vue'
import { useDeviceTypes } from '@/composables/useDeviceTypes'
import { CategoryLabels, CategoryColors } from '@/types/deviceTypes'
import { iconMap } from '@/assets/icons/iconMap'
import DeviceTypeManager from './dialogs/DeviceTypeManager.vue'

// Props
const props = defineProps({
  // 图例位置: 'top-right', 'top-left', 'bottom-right', 'bottom-left'
  position: {
    type: String,
    default: 'top-right'
  },
  // 是否显示筛选器
  showFilters: {
    type: Boolean,
    default: true
  },
  // 是否显示统计信息
  showStatistics: {
    type: Boolean,
    default: true
  },
  // 拓扑图节点数据（用于统计）
  nodes: {
    type: Array,
    default: () => []
  },
  // 当前筛选的节点类型
  filteredTypes: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits(['type-filter-change', 'type-highlight', 'type-unhighlight'])

// 设备类型管理
const {
  // allDeviceTypes,
  deviceTypesByCategory,
  // filteredDeviceTypes,
  activeFilters,
  // typeStatistics,
  setSearchKeyword,
  toggleCategoryFilter: toggleCategoryFilterState,
  clearAllFilters,
  getDeviceTypeByIconName
} = useDeviceTypes()

// 本地状态
const isCollapsed = ref(false)
const collapsedCategories = ref([])
const searchKeyword = ref('')
const typeManagerVisible = ref(false)
const highlightedTypes = ref([])

// 计算属性
const groupedVisibleTypes = computed(() => {
  const grouped = {}
  
  Object.entries(deviceTypesByCategory.value).forEach(([category, types]) => {
    const visibleTypes = types.filter(type => type.visible)
    if (visibleTypes.length > 0) {
      grouped[category] = visibleTypes
    }
  })
  
  return grouped
})

const visibleTypeCount = computed(() => {
  return Object.values(groupedVisibleTypes.value)
    .reduce((count, types) => count + types.length, 0)
})

const totalNodeCount = computed(() => {
  return props.nodes.length
})

const visibleNodeCount = computed(() => {
  if (!hasActiveFilters.value) return totalNodeCount.value
  
  return props.nodes.filter(node => {
    const nodeType = getDeviceTypeByIconName(node.iconName)
    return nodeType && isTypeFiltered(nodeType)
  }).length
})

const hasActiveFilters = computed(() => {
  return activeFilters.categories.length > 0 ||
         activeFilters.tags.length > 0 ||
         activeFilters.search.trim() !== ''
})

// 方法
const getIconPath = (iconName) => {
  return iconMap[iconName] || '/icons/pc.png'
}

const getTypeNodeCount = (type) => {
  return props.nodes.filter(node => node.iconName === type.iconName).length
}

const isTypeFiltered = (type) => {
  if (!hasActiveFilters.value) return true
  
  // 检查分类筛选
  if (activeFilters.categories.length > 0) {
    return activeFilters.categories.includes(type.category)
  }
  
  // 检查标签筛选
  if (activeFilters.tags.length > 0) {
    return activeFilters.tags.some(tag => type.tags.includes(tag))
  }
  
  return true
}

const isCategoryVisible = (category) => {
  return activeFilters.categories.length === 0 || 
         activeFilters.categories.includes(category)
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const toggleCategory = (category) => {
  const index = collapsedCategories.value.indexOf(category)
  if (index > -1) {
    collapsedCategories.value.splice(index, 1)
  } else {
    collapsedCategories.value.push(category)
  }
}

const toggleTypeFilter = (type) => {
  const isCurrentlyFiltered = isTypeFiltered(type)
  
  if (isCurrentlyFiltered) {
    // 当前已显示，点击隐藏该类型
    if (activeFilters.categories.includes(type.category)) {
      toggleCategoryFilterState(type.category)
    }
  } else {
    // 当前已隐藏，点击显示该类型
    if (!activeFilters.categories.includes(type.category)) {
      toggleCategoryFilterState(type.category)
    }
  }
  
  // 触发筛选变化事件
  emit('type-filter-change', {
    type,
    visible: !isCurrentlyFiltered,
    activeFilters: { ...activeFilters }
  })
}

const toggleCategoryFilter = (category, visible) => {
  toggleCategoryFilterState(category)
  
  // 触发筛选变化事件
  emit('type-filter-change', {
    category,
    visible,
    activeFilters: { ...activeFilters }
  })
}

const showAllTypes = () => {
  clearAllFilters()
  searchKeyword.value = ''
  
  emit('type-filter-change', {
    action: 'show-all',
    activeFilters: { ...activeFilters }
  })
}

const hideAllTypes = () => {
  // 设置筛选为空数组，这样不会显示任何类型
  activeFilters.categories = []
  
  emit('type-filter-change', {
    action: 'hide-all',
    activeFilters: { ...activeFilters }
  })
}

const handleSearch = (keyword) => {
  setSearchKeyword(keyword)
  
  emit('type-filter-change', {
    search: keyword,
    activeFilters: { ...activeFilters }
  })
}

const highlightType = (type) => {
  if (!highlightedTypes.value.includes(type.id)) {
    highlightedTypes.value.push(type.id)
  }
  
  emit('type-highlight', type)
}

const unhighlightType = (type) => {
  const index = highlightedTypes.value.indexOf(type.id)
  if (index > -1) {
    highlightedTypes.value.splice(index, 1)
  }
  
  emit('type-unhighlight', type)
}

const showTypeManager = () => {
  typeManagerVisible.value = true
}

const handleTypesUpdated = () => {
  // 设备类型更新后的处理
  emit('type-filter-change', {
    action: 'types-updated',
    activeFilters: { ...activeFilters }
  })
}

// 监听搜索关键词变化
watch(() => activeFilters.search, (newVal) => {
  searchKeyword.value = newVal
})
</script>

<style scoped>
.topology-legend {
  position: absolute;
  width: 280px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(8px);
  z-index: 1000;
  transition: all 0.3s ease;
}

.position-top-right {
  top: 20px;
  right: 20px;
}

.position-top-left {
  top: 20px;
  left: 20px;
}

.position-bottom-right {
  bottom: 20px;
  right: 20px;
}

.position-bottom-left {
  bottom: 20px;
  left: 20px;
}

.collapsed {
  width: auto;
  min-width: 160px;
}

.legend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #f8f9fa;
  border-radius: 8px 8px 0 0;
}

.legend-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.type-count-badge {
  margin-left: 8px;
}

.legend-actions {
  display: flex;
  gap: 4px;
}

.legend-content {
  max-height: 400px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.legend-filters {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.filter-section {
  margin-bottom: 8px;
}

.filter-section:last-child {
  margin-bottom: 0;
}

.legend-types {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.category-section {
  margin-bottom: 8px;
}

.category-section:last-child {
  margin-bottom: 0;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.category-header:hover {
  background-color: #f5f7fa;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-icon {
  font-size: 14px;
  transition: transform 0.3s ease;
}

.category-name {
  font-weight: 500;
  font-size: 14px;
}

.category-count {
  margin-left: 4px;
}

.type-items {
  padding-left: 8px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 4px;
  margin: 2px 8px;
}

.type-item:hover {
  background-color: #f0f9ff;
}

.type-item.filtered {
  background-color: #e6f7ff;
  border: 1px solid #409eff;
}

.type-item.highlighted {
  background-color: #fff7e6;
  border: 1px solid #e6a23c;
}

.type-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-icon img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.type-info {
  flex: 1;
  min-width: 0;
}

.type-name {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-count {
  font-size: 11px;
  color: #909399;
}

.type-status {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.filter-icon {
  font-size: 14px;
  color: #409eff;
  transition: color 0.3s ease;
}

.filter-icon.inactive {
  color: #c0c4cc;
}

.legend-statistics {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;
  border-radius: 0 0 8px 8px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
}

.stat-item:last-child {
  margin-bottom: 0;
}

.stat-label {
  color: #606266;
}

.stat-value {
  color: #303133;
  font-weight: 500;
}

/* 滚动条样式 */
.legend-types::-webkit-scrollbar {
  width: 4px;
}

.legend-types::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.legend-types::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.legend-types::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>