<template>
  <el-dialog 
    v-model="dialogVisible" 
    :title="isEditing ? '编辑设备类型' : '添加设备类型'"
    :width="600"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form 
      ref="formRef"
      :model="formData" 
      :rules="formRules" 
      label-width="100px"
      @submit.prevent
    >
      <el-form-item label="类型名称" prop="name">
        <el-input 
          v-model="formData.name" 
          placeholder="请输入设备类型名称"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="图标选择" prop="iconName">
        <div class="icon-selector">
          <div class="selected-icon">
            <img 
              :src="getIconPath(formData.iconName)" 
              :alt="formData.name"
              class="icon-preview"
            />
            <span class="icon-name">{{ formData.iconName }}</span>
          </div>
          <el-button @click="showIconSelector">选择图标</el-button>
        </div>
      </el-form-item>

      <el-form-item label="设备分类" prop="category">
        <el-select 
          v-model="formData.category" 
          placeholder="选择设备分类"
          style="width: 100%"
        >
          <el-option
            v-for="(label, value) in CategoryLabels"
            :key="value"
            :label="label"
            :value="value"
            :style="{ color: CategoryColors[value] }"
          >
            <span :style="{ color: CategoryColors[value] }">
              {{ label }}
            </span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="显示颜色" prop="color">
        <el-color-picker 
          v-model="formData.color"
          :predefine="predefineColors"
          show-alpha
        />
        <span class="color-preview" :style="{ backgroundColor: formData.color }"></span>
      </el-form-item>

      <el-form-item label="设备描述" prop="description">
        <el-input 
          v-model="formData.description"
          type="textarea"
          placeholder="请输入设备描述"
          :rows="3"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="标签管理" prop="tags">
        <div class="tags-container">
          <div class="existing-tags">
            <el-tag
              v-for="tag in formData.tags"
              :key="tag"
              closable
              @close="removeTag(tag)"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>
          <div class="add-tag">
            <el-input
              v-if="inputVisible"
              ref="inputRef"
              v-model="inputValue"
              size="small"
              style="width: 120px"
              @keyup.enter="handleInputConfirm"
              @blur="handleInputConfirm"
            />
            <el-button v-else size="small" @click="showInput">
              <el-icon><Plus /></el-icon>
              添加标签
            </el-button>
          </div>
        </div>
        <div class="tag-suggestions">
          <span class="suggestion-label">常用标签：</span>
          <el-button
            v-for="tag in suggestedTags"
            :key="tag"
            size="small"
            plain
            @click="addSuggestedTag(tag)"
            class="suggestion-tag"
          >
            {{ tag }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="排序顺序" prop="sortOrder">
        <el-input-number 
          v-model="formData.sortOrder"
          :min="0"
          :max="999"
          controls-position="right"
          style="width: 150px"
        />
        <span class="sort-hint">数值越小排序越靠前</span>
      </el-form-item>

      <el-form-item label="默认可见">
        <el-switch 
          v-model="formData.visible"
          active-text="显示"
          inactive-text="隐藏"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEditing ? '更新' : '添加' }}
        </el-button>
      </span>
    </template>

    <!-- 图标选择对话框 -->
    <el-dialog 
      v-model="iconSelectorVisible" 
      title="选择设备图标"
      :width="500"
      append-to-body
    >
      <div class="icon-grid">
        <div 
          v-for="(iconPath, iconName) in iconMap"
          :key="iconName"
          class="icon-item"
          :class="{ active: formData.iconName === iconName }"
          @click="selectIcon(iconName)"
        >
          <img :src="iconPath" :alt="iconName" />
          <span class="icon-label">{{ iconName }}</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="iconSelectorVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmIconSelection">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { CategoryLabels, CategoryColors, DeviceCategories } from '@/types/deviceTypes'
import { iconMap } from '@/assets/icons/iconMap'

// Props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  deviceType: {
    type: Object,
    default: null
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'save'])

// 表单相关
const formRef = ref()
const saving = ref(false)

// 默认表单数据
const defaultFormData = {
  name: '',
  iconName: 'pc',
  category: DeviceCategories.CUSTOM,
  color: CategoryColors[DeviceCategories.CUSTOM],
  description: '',
  tags: [],
  sortOrder: 0,
  visible: true
}

const formData = ref({ ...defaultFormData })

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入设备类型名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  iconName: [
    { required: true, message: '请选择设备图标', trigger: 'change' }
  ],
  category: [
    { required: true, message: '请选择设备分类', trigger: 'change' }
  ],
  color: [
    { required: true, message: '请选择显示颜色', trigger: 'change' }
  ]
}

// 标签输入
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref()

// 图标选择
const iconSelectorVisible = ref(false)
const selectedIconName = ref('')

// 预定义颜色
const predefineColors = Object.values(CategoryColors)

// 建议标签
const suggestedTags = [
  '服务器', '网络设备', '终端', '安全设备', '存储', '数据库', 
  '虚拟化', '云服务', '监控', '备份', '高可用', '负载均衡'
]

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEditing = computed(() => {
  return props.deviceType && props.deviceType.id
})

// 方法
const getIconPath = (iconName) => {
  return iconMap[iconName] || '/icons/pc.png'
}

const showIconSelector = () => {
  selectedIconName.value = formData.value.iconName
  iconSelectorVisible.value = true
}

const selectIcon = (iconName) => {
  selectedIconName.value = iconName
}

const confirmIconSelection = () => {
  formData.value.iconName = selectedIconName.value
  iconSelectorVisible.value = false
}

const removeTag = (tag) => {
  const index = formData.value.tags.indexOf(tag)
  if (index > -1) {
    formData.value.tags.splice(index, 1)
  }
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleInputConfirm = () => {
  const value = inputValue.value.trim()
  if (value && !formData.value.tags.includes(value)) {
    formData.value.tags.push(value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

const addSuggestedTag = (tag) => {
  if (!formData.value.tags.includes(tag)) {
    formData.value.tags.push(tag)
  }
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    
    // 构造保存数据
    const saveData = {
      ...formData.value,
      id: isEditing.value ? props.deviceType.id : undefined
    }
    
    emit('save', saveData)
    
    // 重置表单
    formData.value = { ...defaultFormData }
    
  } catch {    ElMessage.error('请检查表单输入')
  } finally {
    saving.value = false
  }
}

// 监听设备类型变化，初始化表单
watch(() => props.deviceType, (newType) => {
  if (newType) {
    formData.value = {
      name: newType.name || '',
      iconName: newType.iconName || 'pc',
      category: newType.category || DeviceCategories.CUSTOM,
      color: newType.color || CategoryColors[DeviceCategories.CUSTOM],
      description: newType.description || '',
      tags: [...(newType.tags || [])],
      sortOrder: newType.sortOrder || 0,
      visible: newType.visible !== false
    }
  } else {
    formData.value = { ...defaultFormData }
  }
}, { immediate: true })

// 监听分类变化，自动更新颜色
watch(() => formData.value.category, (newCategory) => {
  if (newCategory && CategoryColors[newCategory]) {
    formData.value.color = CategoryColors[newCategory]
  }
})
</script>

<style scoped>
.icon-selector {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selected-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background-color: #f8f9fa;
}

.icon-preview {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.icon-name {
  font-size: 14px;
  color: #606266;
}

.color-preview {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  margin-left: 12px;
  border: 1px solid #e4e7ed;
}

.tags-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.existing-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
}

.tag-item {
  margin: 0;
}

.add-tag {
  display: flex;
  align-items: center;
}

.tag-suggestions {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.suggestion-label {
  font-size: 14px;
  color: #606266;
  margin-right: 8px;
}

.suggestion-tag {
  margin: 0 4px 4px 0;
  font-size: 12px;
}

.sort-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding: 12px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.icon-item:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.icon-item.active {
  border-color: #409eff;
  background-color: #e6f7ff;
}

.icon-item img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.icon-label {
  font-size: 12px;
  color: #606266;
  text-align: center;
  word-break: break-word;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>