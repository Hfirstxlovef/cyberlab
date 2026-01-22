<template>
  <div class="link-mode-overlay" v-if="linking">
    <p>正在连线：已选中 <strong>{{ sourceNode?.name || '未知节点' }}</strong>，点击另一个节点完成连线</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  nodes: Array,
  links: Array
})

const emit = defineEmits(['update'])

const linking = ref(false)
const sourceNode = ref(null)

const handleNodeClick = async (node) => {
  if (!linking.value) {
    // 第一次点击，选中起始节点
    sourceNode.value = node
    linking.value = true
  } else {
    if (node.id === sourceNode.value.id) {
      ElMessageBox.alert('不能连接自己', '非法连接')
      return
    }

    try {
      const { value: label } = await ElMessageBox.prompt(
        `请输入 ${sourceNode.value.name} → ${node.name} 的连接标签`,
        '设置连接标签',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '如 HTTP、SSH、数据库等',
          inputValidator: (val) => val.trim() !== '' || '标签不能为空'
        }
      )

      const newLink = {
        source: sourceNode.value.id,
        target: node.id,
        label
      }

      emit('update', {
        nodes: props.nodes,
        links: [...props.links, newLink]
      })

    } catch {
      // 用户取消了连接标签输入，不需要处理
    }

    // 重置状态
    linking.value = false
    sourceNode.value = null
  }
}

onMounted(() => {
  window.addEventListener('node-click', (e) => handleNodeClick(e.detail))
})
</script>

<style scoped>
/* 确保样式标签正确闭合且无语法错误 */
.link-mode-overlay {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
}
</style>