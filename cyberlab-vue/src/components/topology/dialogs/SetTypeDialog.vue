<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="handleClose"
    title="设置节点类型"
    width="400px"
    :destroy-on-close="true"
  >
    <el-form :model="localNode" label-width="100px">
      <el-form-item label="当前节点">
        <el-input :value="localNode.name || '未命名节点'" disabled></el-input>
      </el-form-item>
      <el-form-item label="节点类型">
        <el-select v-model="localNode.type" placeholder="请选择节点类型" style="width: 100%">
          <el-option label="服务器" value="server">
            <span>🖥️ 服务器</span>
          </el-option>
          <el-option label="终端" value="terminal">
            <span>💻 终端</span>
          </el-option>
          <el-option label="交换机" value="switch">
            <span>🔀 交换机</span>
          </el-option>
          <el-option label="路由器" value="router">
            <span>📡 路由器</span>
          </el-option>
          <el-option label="防火墙" value="firewall">
            <span>🛡️ 防火墙</span>
          </el-option>
          <el-option label="数据库" value="database">
            <span>🗄️ 数据库</span>
          </el-option>
          <el-option label="负载均衡器" value="loadbalancer">
            <span>⚖️ 负载均衡器</span>
          </el-option>
          <el-option label="自定义" value="custom">
            <span>🏷️ 自定义</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item v-if="localNode.type === 'custom'" label="自定义标签">
        <el-input 
          v-model="localNode.customLabel" 
          placeholder="请输入自定义标签"
          maxlength="20"
          show-word-limit
        ></el-input>
      </el-form-item>
      <el-form-item label="颜色标识">
        <el-color-picker 
          v-model="localNode.typeColor" 
          :predefine="predefineColors"
          show-alpha
        ></el-color-picker>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null }
});

const emits = defineEmits(['update:visible', 'save']);

// 预定义颜色
const predefineColors = [
  '#409EFF', // 蓝色
  '#67C23A', // 绿色
  '#E6A23C', // 黄色
  '#F56C6C', // 红色
  '#909399', // 灰色
  '#8B5CF6', // 紫色
  '#06B6D4', // 青色
  '#F59E0B', // 橙色
];

// 本地节点数据
const localNode = ref({
  id: '',
  name: '',
  type: '',
  customLabel: '',
  typeColor: '#409EFF'
});

// 监听节点数据变化
watch(() => props.node, (newNode) => {
  if (newNode) {
    localNode.value = {
      id: newNode.id || '',
      name: newNode.name || '',
      type: newNode.type || '',
      customLabel: newNode.customLabel || '',
      typeColor: newNode.typeColor || '#409EFF'
    };
  }
}, { immediate: true, deep: true });

const handleClose = () => {
  emits('update:visible', false);
};

const handleSave = () => {
  // 构建更新后的节点对象
  const updatedNode = {
    ...localNode.value,
    // 如果是自定义类型，使用customLabel作为显示标签
    displayType: localNode.value.type === 'custom' 
      ? localNode.value.customLabel || '自定义' 
      : getTypeLabel(localNode.value.type)
  };
  
  emits('save', updatedNode);
  handleClose();
};

// 获取类型显示标签
const getTypeLabel = (type) => {
  const typeMap = {
    server: '服务器',
    terminal: '终端',
    switch: '交换机',
    router: '路由器',
    firewall: '防火墙',
    database: '数据库',
    loadbalancer: '负载均衡器'
  };
  return typeMap[type] || type;
};
</script>

<style scoped>
.el-form-item {
  margin-bottom: 18px;
}

.el-select-dropdown__item {
  display: flex;
  align-items: center;
}
</style>