<template>
  <div class="icon-dialog-container">
    <Teleport to="body">
      <el-dialog
        v-model="localModelValue"
        title="选择图标"
        width="600px"
        @close="handleClose"
        :before-close="handleBeforeClose"
        :destroy-on-close="true"
      >
        <!-- 节点数据验证提示 -->
        <div v-if="!nodeId" class="error-message">
          错误：未找到有效的节点信息
        </div>
        <div class="icon-grid" v-else>
          <div
            v-for="icon in icons"
            :key="icon.name"
            class="icon-item"
            @click="handleIconSelect(icon.name)">
            <img 
              :src="`/icons/${icon.name}`"
              :alt="icon.label || icon.name"
              class="icon-image"
            />
            <span class="icon-label">{{ icon.label || icon.name }}</span>
          </div>
        </div>
      </el-dialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits, watch } from 'vue'

// Props定义
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  nodeId: { type: String, required: true }
})

// 事件定义
const emits = defineEmits(['update:modelValue', 'select'])

// 响应式变量
const localModelValue = ref(props.modelValue)
const icons = ref([
  { name: 'server.png', label: '服务器' },
  { name: 'router.png', label: '路由器' },
  { name: 'firewall.png', label: '防火墙' },
  // 添加更多图标定义
  { name: 'pc.png', label: 'PC' },
  { name: 'database.png', label: '数据库' },
  { name: 'switch.png', label: '交换机' },
  { name: 'webserver.png', label: 'Web服务器' },
  { name: 'storage.png', label: '存储服务器' }
])

// 方法定义
const handleClose = () => {
  emits('update:modelValue', false)
}

const handleBeforeClose = (done) => {
  // 可以添加关闭前的验证逻辑
  done()
}

const handleIconSelect = (iconName) => {
  if (!iconName) {
    return
  }
  emits('select', { nodeId: props.nodeId, iconName })
  handleClose()
}

// 监听modelValue变化
watch(
  () => props.modelValue,
  (val) => {
    localModelValue.value = val
  }
)
</script>

<style scoped>
/* 移除空的 .icon-dialog-container 规则集 */

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 16px;
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.icon-item:hover {
  background-color: #f0f0f0;
}

.icon-image {
  width: 48px;
  height: 48px;
  object-fit: contain;
  margin-bottom: 8px;
}

.icon-label {
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
}

.error-message {
  color: #f56c6c;
  padding: 16px;
  text-align: center;
}
</style>