<template>
  <div class="node-detail-dialog-container">
    <Teleport to="body">
      <el-dialog
        :model-value="props.visible"
        @update:model-value="val => emits('update:visible', val)"
        @close="handleClose"
        title="节点详情"
        width="500px"
        :destroy-on-close="true"
      >
        <el-form v-if="localNode" :model="localNode" label-width="100px">
          <el-form-item label="节点ID">
            <el-input v-model="localNode.id" disabled></el-input>
          </el-form-item>
          <el-form-item label="节点名称">
            <el-input v-model="localNode.name"></el-input>
          </el-form-item>
          <el-form-item label="节点类型">
            <el-input :value="localNode.type || '未设置'" disabled></el-input>
          </el-form-item>
          <el-form-item label="IP地址">
            <el-input v-model="localNode.ip"></el-input>
          </el-form-item>
          <el-form-item label="所属部门">
            <el-input v-model="localNode.department"></el-input>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="localNode.status">
              <el-option label="正常" value="normal"></el-option>
              <el-option label="警告" value="warning"></el-option>
              <el-option label="异常" value="error"></el-option>
              <el-option label="离线" value="offline"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input type="textarea" v-model="localNode.comment"></el-input>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

defineOptions({ inheritAttrs: false })

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null }
})

const emits = defineEmits(['update:visible', 'save']);
const localNode = ref({
  id: '',
  name: '',
  type: '',
  ip: '',
  department: '',
  status: 'normal',
  comment: ''
});

// 监听节点数据变化并深拷贝
watch(() => props.node,
  (newNode) => {
    if (newNode) {
      // 使用深拷贝避免引用关系，并确保所有属性都有默认值
      localNode.value = {
        id: newNode.id || '',
        name: newNode.name || '',
        type: newNode.type || '',
        ip: newNode.ip || '',
        department: newNode.department || '',
        status: newNode.status || 'normal',
        comment: newNode.comment || ''
      };
    }
  },
  { immediate: true, deep: true }
);

const handleClose = () => {
  emits('update:visible', false);
};

const handleSave = () => {
  emits('save', localNode.value);
  handleClose();
};
</script>

<style scoped>
.el-form-item__content {
  margin-left: 100px !important;
}
</style>