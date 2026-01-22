<template>
  <Teleport to="body">
    <div v-if="visible" class="rename-popup">
      <input 
        ref="inputRef"
        v-model="inputValue" 
        @keyup.enter="confirmRename"
        @keyup.escape="cancelRename"
        class="rename-input"
        placeholder="请输入新名称"
      />
      <button @click="confirmRename" class="rename-confirm-btn">确定</button>
      <button @click="cancelRename" class="rename-cancel-btn">取消</button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  visible: Boolean,
  initialValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const inputRef = ref(null)
const inputValue = ref('')

// 监听显示状态，自动聚焦输入框
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    inputValue.value = props.initialValue
    nextTick(() => {
      if (inputRef.value) {
        inputRef.value.focus()
        inputRef.value.select()
      }
    })
  }
})

// 确认重命名
const confirmRename = () => {
  if (inputValue.value.trim()) {
    emit('confirm', inputValue.value.trim())
  }
}

// 取消重命名
const cancelRename = () => {
  emit('cancel')
}
</script>

<style scoped>
.rename-popup {
  position: absolute; 
  top: 50%; 
  left: 50%;
  background-color: #f5f5f7;
  padding: 20px; 
  border-radius: 12px;
  transform: translate(-50%, -50%); 
  z-index: 2000;
  display: flex; 
  gap: 12px; 
  align-items: center;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.rename-input {
  padding: 12px 16px; 
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  font-size: 15px; 
  font-family: -apple-system, BlinkMacSystemFont, sans-serif;
  width: 220px; 
  transition: all 0.2s ease;
}

.rename-input:focus {
  outline: none; 
  border-color: #007aff;
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.2);
}

.rename-confirm-btn {
  padding: 12px 20px; 
  background-color: #007aff;
  color: white; 
  border: none; 
  border-radius: 8px;
  font-size: 15px; 
  font-weight: 500;
  cursor: pointer; 
  transition: background-color 0.2s;
}

.rename-confirm-btn:hover {
  background-color: #0066cc;
}

.rename-cancel-btn {
  padding: 12px 20px; 
  background-color: #f0f0f0;
  color: #333; 
  border: none; 
  border-radius: 8px;
  font-size: 15px; 
  font-weight: 500;
  cursor: pointer; 
  transition: background-color 0.2s;
}

.rename-cancel-btn:hover {
  background-color: #e0e0e0;
}
</style>