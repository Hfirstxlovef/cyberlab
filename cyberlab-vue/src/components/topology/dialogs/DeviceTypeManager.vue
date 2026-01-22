<template>
  <el-dialog 
    v-model="dialogVisible" 
    title="设备类型管理" 
    :width="800"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="device-type-manager">
      <!-- 操作栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button 
            type="primary" 
            :icon="Plus" 
            @click="showAddDialog"
          >
            添加自定义类型
          </el-button>
          <el-button 
            :icon="Refresh" 
            @click="resetToDefaults"
          >
            重置默认
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索设备类型..."
            :prefix-icon="Search"
            clearable
            style="width: 200px"
            @input="handleSearch"
          />
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-section">
          <span class="filter-label">分类筛选：</span>
          <el-checkbox-group v-model="categoryFilters" @change="handleCategoryFilter">
            <el-checkbox 
              v-for="(label, category) in CategoryLabels" 
              :key="category"
              :value="category"
              :style="{ color: CategoryColors[category] }"
            >
              {{ label }} ({{ typeStatistics.byCategory[category]?.total || 0 }})
            </el-checkbox>
          </el-checkbox-group>
        </div>
        
        <div class="filter-section">
          <span class="filter-label">标签筛选：</span>
          <el-select
            v-model="tagFilters"
            multiple
            placeholder="选择标签"
            style="width: 300px"
            @change="handleTagFilter"
          >
            <el-option
              v-for="tag in availableTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </div>

        <el-button 
          v-if="hasActiveFilters" 
          link 
          @click="clearFilters"
        >
          清空筛选
        </el-button>
      </div>

      <!-- 设备类型列表 -->
      <div class="device-type-list">
        <div 
          v-for="(types, category) in groupedDeviceTypes" 
          :key="category"
          class="category-group"
        >
          <div class="category-header">
            <div class="category-title" :style="{ color: CategoryColors[category] }">
              <el-icon><Grid /></el-icon>
              {{ CategoryLabels[category] }}
              <span class="category-count">({{ types.length }})</span>
            </div>
            <div class="category-actions">
              <el-switch
                :model-value="isCategoryVisible(category)"
                @change="(val) => toggleCategoryVisibility(category, val)"
                active-text="显示"
                inactive-text="隐藏"
              />
            </div>
          </div>
          
          <div class="device-type-cards">
            <div 
              v-for="type in types" 
              :key="type.id"
              class="device-type-card"
              :class="{ 'hidden-type': !type.visible }"
            >
              <div class="card-header">
                <div class="device-icon">
                  <img :src="getIconPath(type.iconName)" :alt="type.name" />
                </div>
                <div class="device-info">
                  <div class="device-name">{{ type.name }}</div>
                  <div class="device-description">{{ type.description }}</div>
                </div>
                <div class="card-actions">
                  <el-tooltip content="编辑">
                    <el-button 
                      size="small" 
                      :icon="Edit" 
                      circle 
                      @click="editDeviceType(type)"
                    />
                  </el-tooltip>
                  <el-tooltip content="显示/隐藏">
                    <el-button 
                      size="small" 
                      :icon="type.visible ? View : Hide" 
                      circle 
                      @click="toggleTypeVisibility(type)"
                    />
                  </el-tooltip>
                  <el-tooltip v-if="type.category === 'custom'" content="删除">
                    <el-button 
                      size="small" 
                      :icon="Delete" 
                      circle 
                      type="danger"
                      @click="deleteDeviceType(type)"
                    />
                  </el-tooltip>
                </div>
              </div>
              
              <div class="device-tags">
                <el-tag 
                  v-for="tag in type.tags" 
                  :key="tag"
                  size="small"
                  class="tag-item"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="statistics">
        <el-statistic title="总设备类型" :value="typeStatistics.total" />
        <el-statistic title="可见类型" :value="typeStatistics.visible" />
        <el-statistic title="自定义类型" :value="customDeviceTypes.length" />
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="saveAndClose">保存并关闭</el-button>
      </span>
    </template>

    <!-- 添加/编辑设备类型对话框 -->
    <DeviceTypeEditDialog
      v-model="editDialogVisible"
      :device-type="editingDeviceType"
      @save="handleSaveDeviceType"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  Refresh, 
  Search, 
  Edit, 
  Delete, 
  View, 
  Hide, 
  Grid 
} from '@element-plus/icons-vue'
import { useDeviceTypes } from '@/composables/useDeviceTypes'
import { CategoryLabels, CategoryColors, DeviceCategories } from '@/types/deviceTypes'
import { iconMap } from '@/assets/icons/iconMap'
import DeviceTypeEditDialog from './DeviceTypeEditDialog.vue'

// Props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'save'])

// 设备类型管理
const {
  allDeviceTypes,
  customDeviceTypes,
  deviceTypesByCategory,
  filteredDeviceTypes,
  availableTags,
  typeStatistics,
  activeFilters,
  setSearchKeyword,
  addCategoryFilter,
  removeCategoryFilter,
  addTagFilter,
  removeTagFilter,
  clearAllFilters,
  toggleCategoryFilter,
  toggleTagFilter,
  setDeviceTypeVisibility,
  setCategoryVisibility,
  addCustomDeviceType,
  updateDeviceType,
  deleteCustomDeviceType,
  resetToDefaults: resetDeviceTypes
} = useDeviceTypes()

// 对话框显示控制
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 搜索和筛选
const searchKeyword = ref('')
const categoryFilters = ref([])
const tagFilters = ref([])

// 编辑对话框
const editDialogVisible = ref(false)
const editingDeviceType = ref(null)

// 计算属性
const hasActiveFilters = computed(() => {
  return categoryFilters.value.length > 0 || 
         tagFilters.value.length > 0 || 
         searchKeyword.value.trim() !== ''
})

const groupedDeviceTypes = computed(() => {
  let types = filteredDeviceTypes.value
  
  // 按分类分组
  const grouped = {}
  types.forEach(type => {
    if (!grouped[type.category]) {
      grouped[type.category] = []
    }
    grouped[type.category].push(type)
  })
  
  return grouped
})

// 方法
const getIconPath = (iconName) => {
  return iconMap[iconName] || '/icons/pc.png'
}

const handleSearch = (keyword) => {
  setSearchKeyword(keyword)
}

const handleCategoryFilter = (categories) => {
  activeFilters.categories = [...categories]
}

const handleTagFilter = (tags) => {
  activeFilters.tags = [...tags]
}

const clearFilters = () => {
  searchKeyword.value = ''
  categoryFilters.value = []
  tagFilters.value = []
  clearAllFilters()
}

const isCategoryVisible = (category) => {
  const typesInCategory = allDeviceTypes.value.filter(t => t.category === category)
  return typesInCategory.every(t => t.visible)
}

const toggleCategoryVisibility = (category, visible) => {
  setCategoryVisibility(category, visible)
}

const toggleTypeVisibility = (type) => {
  setDeviceTypeVisibility(type.id, !type.visible)
}

const showAddDialog = () => {
  editingDeviceType.value = null
  editDialogVisible.value = true
}

const editDeviceType = (type) => {
  editingDeviceType.value = { ...type }
  editDialogVisible.value = true
}

const handleSaveDeviceType = (typeData) => {
  if (editingDeviceType.value && editingDeviceType.value.id) {
    // 更新现有类型
    updateDeviceType(editingDeviceType.value.id, typeData)
    ElMessage.success('设备类型更新成功')
  } else {
    // 添加新类型
    addCustomDeviceType(typeData)
    ElMessage.success('设备类型添加成功')
  }
  editDialogVisible.value = false
}

const deleteDeviceType = async (type) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除设备类型 "${type.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (deleteCustomDeviceType(type.id)) {
      ElMessage.success('设备类型删除成功')
    } else {
      ElMessage.error('删除失败：无法删除内置设备类型')
    }
  } catch {    // 用户取消删除
  }
}

const resetToDefaults = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置到默认配置吗？这将清除所有自定义设备类型和配置。',
      '确认重置',
      {
        confirmButtonText: '重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    resetDeviceTypes()
    clearFilters()
    ElMessage.success('已重置到默认配置')
  } catch {    // 用户取消重置
  }
}

const saveAndClose = () => {
  emit('save')
  dialogVisible.value = false
  ElMessage.success('设备类型配置已保存')
}

// 监听筛选器变化
watch(() => activeFilters.categories, (newVal) => {
  categoryFilters.value = [...newVal]
})

watch(() => activeFilters.tags, (newVal) => {
  tagFilters.value = [...newVal]
})

watch(() => activeFilters.search, (newVal) => {
  searchKeyword.value = newVal
})
</script>

<style scoped>
.device-type-manager {
  padding: 20px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.filter-bar {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.filter-section {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.filter-section:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-weight: 500;
  margin-right: 12px;
  min-width: 80px;
  color: #606266;
}

.device-type-list {
  max-height: 500px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.category-group {
  border-bottom: 1px solid #f0f0f0;
}

.category-group:last-child {
  border-bottom: none;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background-color: #f8f9fa;
  font-weight: 500;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.category-count {
  color: #909399;
  font-weight: normal;
}

.device-type-cards {
  padding: 16px 20px;
}

.device-type-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
}

.device-type-card:last-child {
  margin-bottom: 0;
}

.device-type-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.hidden-type {
  opacity: 0.5;
  background-color: #f5f5f5;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.device-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background-color: #fff;
}

.device-icon img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.device-info {
  flex: 1;
}

.device-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.device-description {
  font-size: 14px;
  color: #606266;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.device-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  font-size: 12px;
}

.statistics {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>