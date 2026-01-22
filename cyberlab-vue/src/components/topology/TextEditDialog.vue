<template>
  <div v-if="visible" class="dialog-overlay" @click="handleOverlayClick">
    <div class="text-edit-dialog" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">✏️ 编辑文字</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="dialog-content">
        <!-- 文字内容编辑 -->
        <div class="form-group">
          <label class="form-label">📝 文字内容</label>
          <textarea v-model="editForm.text" class="form-textarea" placeholder="请输入文字内容..." rows="3"></textarea>
        </div>

        <!-- 字体大小 -->
        <div class="form-group">
          <label class="form-label">🔠 字体大小</label>
          <div class="size-controls">
            <input v-model.number="editForm.fontSize" type="range" min="12" max="48" class="size-slider" />
            <input v-model.number="editForm.fontSize" type="number" min="12" max="48" class="size-input" />
            <span class="size-unit">px</span>
          </div>
        </div>

        <!-- 文字颜色 -->
        <div class="form-group">
          <label class="form-label">🎨 文字颜色</label>
          <div class="color-controls">
            <input v-model="editForm.color" type="color" class="color-picker" />
            <input v-model="editForm.color" type="text" class="color-input" placeholder="#333333" />
            <div class="color-presets">
              <div v-for="color in colorPresets" :key="color" class="color-preset" :style="{ backgroundColor: color }"
                @click="editForm.color = color" :class="{ active: editForm.color === color }"></div>
            </div>
          </div>
        </div>

        <!-- 字体粗细 -->
        <div class="form-group">
          <label class="form-label">💪 字体粗细</label>
          <select v-model="editForm.fontWeight" class="form-select">
            <option value="normal">正常</option>
            <option value="bold">粗体</option>
            <option value="lighter">细体</option>
          </select>
        </div>

        <!-- 固定位置 -->
        <div class="form-group">
          <label class="form-checkbox">
            <input v-model="editForm.pinned" type="checkbox" />
            <span class="checkbox-text">📌 固定位置（防止拖拽移动）</span>
          </label>
        </div>
      </div>

      <div class="dialog-footer">
        <button class="btn btn-secondary" @click="close">取消</button>
        <button class="btn btn-primary" @click="save">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  textElement: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'save'])

const editForm = ref({
  text: '',
  fontSize: 16,
  color: '#333333',
  fontWeight: 'normal',
  pinned: false
})

const colorPresets = [
  '#333333', '#666666', '#999999', '#000000',
  '#ff0000', '#00ff00', '#0000ff', '#ffff00',
  '#ff00ff', '#00ffff', '#ffa500', '#800080'
]

// 监听 textElement 变化，更新表单
watch(() => props.textElement, (newElement) => {
  if (newElement) {
    editForm.value = {
      text: newElement.style?.text || '',
      fontSize: newElement.style?.fontSize || 16,
      color: newElement.style?.fill || '#333333',
      fontWeight: newElement.style?.fontWeight || 'normal',
      pinned: newElement.pinned || false
    }
  }
}, { immediate: true })

const handleOverlayClick = () => {
  close()
}

const close = () => {
  emit('close')
}

const save = () => {
  if (!editForm.value.text.trim()) {
    alert('请输入文字内容')
    return
  }

  emit('save', {
    ...props.textElement,
    style: {
      ...props.textElement.style,
      text: editForm.value.text,
      fontSize: editForm.value.fontSize,
      fill: editForm.value.color,
      fontWeight: editForm.value.fontWeight
    },
    pinned: editForm.value.pinned,
    draggable: !editForm.value.pinned
  })

  close()
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  backdrop-filter: blur(4px);
}

.text-edit-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e1e4e8;
  background: linear-gradient(135deg, #f6f8fa 0%, #e1e4e8 100%);
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #24292f;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #656d76;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #24292f;
}

.dialog-content {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #24292f;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: border-color 0.2s ease;
}

.form-textarea:focus {
  outline: none;
  border-color: #0969da;
  box-shadow: 0 0 0 2px rgba(9, 105, 218, 0.2);
}

.size-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.size-slider {
  flex: 1;
  height: 6px;
  background: #e1e4e8;
  border-radius: 3px;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
}

.size-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  background: #0969da;
  border-radius: 50%;
  cursor: pointer;
}

.size-input {
  width: 60px;
  padding: 6px 8px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  text-align: center;
  font-size: 14px;
}

.size-unit {
  font-size: 14px;
  color: #656d76;
}

.color-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.color-picker {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
}

.color-input {
  width: 100px;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  font-size: 14px;
  font-family: monospace;
}

.color-presets {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.color-preset {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.color-preset:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.color-preset.active {
  border-color: #0969da;
  transform: scale(1.1);
}

.form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.form-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
}

.checkbox-text {
  color: #24292f;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e1e4e8;
  background: #f6f8fa;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
}

.btn-secondary:hover {
  background: #f3f4f6;
  border-color: #afb8c1;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover {
  background: #0550ae;
}
</style>