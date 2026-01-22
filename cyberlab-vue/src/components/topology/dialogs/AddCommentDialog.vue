<template>
  <div class="add-comment-dialog-container">
    <Teleport to="body">
      <el-dialog
        v-model="localModelValue"
        title="添加节点备注"
        width="500px"
        :close-on-click-modal="false"
        @close="handleClose"
        :destroy-on-close="true"
      >
        <el-form ref="commentFormRef" :model="commentForm" :rules="rules" label-width="80px">
          <el-form-item label="备注内容" prop="content">
            <el-input
              v-model="commentForm.content"
              type="textarea"
              rows="6"
              placeholder="请输入节点备注信息..."
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" @click="handleSave">保存</el-button>
          </span>
        </template>
      </el-dialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits, reactive, watch } from 'vue'

defineOptions({ inheritAttrs: false })

// 定义props和emits
const props = defineProps({
  modelValue: { type: Boolean, required: true },
  nodeId: { type: String, required: true }
})

const emits = defineEmits(['update:modelValue', 'save'])

// 响应式变量
const localModelValue = ref(props.modelValue)
const commentFormRef = ref(null)

// 表单数据和验证规则
const commentForm = reactive({
  content: '',
  priority: 'low'
})

const rules = {
  content: [
    { required: true, message: '请输入备注内容', trigger: 'blur' },
    { max: 500, message: '备注内容不能超过500个字符', trigger: 'blur' }
  ]
}

// 监听modelValue变化
watch(
  () => props.modelValue,
  (val) => {
    localModelValue.value = val
    // 当对话框打开时重置表单
    if (val && commentFormRef.value) {
      setTimeout(() => {
        commentFormRef.value.resetFields()
      }, 0)
    }
  }
)

// 处理保存
const handleSave = async () => {
  if (!commentFormRef.value) return

  try {
    // 表单验证
    await commentFormRef.value.validate()

    // 构造备注数据
    const commentData = {
      nodeId: props.nodeId,
      content: commentForm.content,
      priority: commentForm.priority,
      timestamp: new Date().toISOString()
    }

    // 触发保存事件
    emits('save', commentData)

    // 关闭对话框
    handleClose()
  } catch (error) {
    // 表单验证失败，不执行保存
  }
}

// 处理关闭
const handleClose = () => {
  // 重置表单
  if (commentFormRef.value) {
    commentFormRef.value.resetFields()
  }
  // 通知父组件关闭
  emits('update:modelValue', false)
}
</script>

<style scoped>
 移除空的规则集

.el-textarea__inner {
  resize: vertical;
}

.el-form-item {
  margin-bottom: 15px;
}
</style>