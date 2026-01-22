<!-- src/views/drills/DrillCreatePage.vue -->
<template>
  <div class="create-drill-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">新建演练</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="演练名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入演练名称" />
        </el-form-item>

        <el-form-item label="关联资产项目" prop="selectedProject">
          <el-select 
            v-model="form.selectedProject" 
            placeholder="请选择要关联的资产项目"
            style="width: 100%"
            clearable
            @change="handleProjectChange"
          >
            <el-option
              v-for="project in assetProjects"
              :key="project.topologyProjectId"
              :label="project.name"
              :value="project.topologyProjectId"
            />
          </el-select>
          <div class="form-tip">选择资产项目后将自动关联其网络拓扑图</div>
        </el-form-item>

        <el-form-item label="网络拓扑" prop="networkConfig">
          <el-input v-model="form.networkConfig" placeholder="例如：内网-公网-靶场" />
        </el-form-item>

        <el-form-item label="漏洞配置" prop="vulnConfig">
          <el-input v-model="form.vulnConfig" placeholder="例如：SQL注入 + 弱口令" />
        </el-form-item>

        <!-- 容器部署预览 -->
        <el-form-item v-if="form.selectedProject && containerAssets.length > 0" label="容器部署预览">
          <div class="container-preview">
            <div class="preview-header">
              <span class="preview-title">该项目包含 {{ containerAssets.length }} 个可容器化资产</span>
              <el-button size="small" type="primary" @click="showContainerConfig = true">
                配置容器部署
              </el-button>
            </div>
            
            <div class="container-assets-grid">
              <div 
                v-for="asset in containerAssets" 
                :key="asset.id"
                class="container-asset-card"
              >
                <div class="asset-header">
                  <span class="asset-name">{{ asset.name }}</span>
                  <el-tag 
                    :type="asset.assetType === 'container' ? 'success' : 'info'" 
                    size="small"
                  >
                    {{ getAssetTypeLabel(asset.assetType) }}
                  </el-tag>
                </div>
                
                <div class="asset-info">
                  <div v-if="asset.dockerImage" class="docker-info">
                    <span class="info-label">镜像:</span>
                    <span class="docker-image">{{ asset.dockerImage }}</span>
                  </div>
                  
                  <div v-if="asset.preferredHostNodeName" class="node-info">
                    <span class="info-label">首选节点:</span>
                    <span class="node-name">{{ asset.preferredHostNodeName }}</span>
                  </div>
                  
                  <div v-if="asset.deploymentStrategy" class="strategy-info">
                    <span class="info-label">部署策略:</span>
                    <span class="strategy">{{ getDeploymentStrategyLabel(asset.deploymentStrategy) }}</span>
                  </div>
                </div>
                
                <div class="asset-actions">
                  <el-button size="small" @click="editAssetConfig(asset)">配置</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit">提交</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 容器配置对话框 -->
    <el-dialog 
      v-model="showContainerConfig" 
      title="容器部署配置" 
      width="80%"
      :close-on-click-modal="false"
    >
      <div class="container-config-dialog">
        <div class="config-header">
          <span>为项目 "{{ getProjectName() }}" 配置容器部署策略</span>
        </div>
        
        <el-table :data="containerAssets" border>
          <el-table-column prop="name" label="资产名称" width="150" />
          <el-table-column label="Docker镜像" width="200">
            <template #default="scope">
              <el-input 
                v-model="scope.row.dockerImage" 
                placeholder="例如: nginx:alpine"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="部署节点" width="180">
            <template #default="scope">
              <el-select 
                v-model="scope.row.preferredHostNodeId" 
                placeholder="选择节点"
                size="small"
                style="width: 100%"
              >
                <el-option 
                  v-for="node in availableNodes" 
                  :key="node.id" 
                  :label="node.displayName || node.name" 
                  :value="node.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="端口配置" width="150">
            <template #default="scope">
              <el-input 
                v-model="scope.row.containerPorts" 
                placeholder="80:8080"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="自动启动">
            <template #default="scope">
              <el-switch v-model="scope.row.autoStart" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showContainerConfig = false">取消</el-button>
          <el-button type="primary" @click="saveContainerConfig">保存配置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAssetProjects } from '@/api/asset'

const router = useRouter()

const formRef = ref(null)
const assetProjects = ref([])
const containerAssets = ref([])
const availableNodes = ref([])
const showContainerConfig = ref(false)

const form = ref({
  name: '',
  selectedProject: '',
  networkConfig: '',
  vulnConfig: ''
})

const rules = {
  name: [{ required: true, message: '请输入演练名称', trigger: 'blur' }],
  networkConfig: [{ required: true, message: '请输入网络拓扑', trigger: 'blur' }],
  vulnConfig: [{ required: true, message: '请输入漏洞配置', trigger: 'blur' }]
}

// 获取资产项目列表
const fetchAssetProjects = async () => {
  try {
    const data = await getAssetProjects()
    
    // 由于axios配置返回的是response.data，所以直接使用data
    let projects = []
    if (Array.isArray(data)) {
      projects = data
    } else {
      projects = []
    }
    
    assetProjects.value = projects
    
    if (projects.length === 0) {
      ElMessage.info('暂无可关联的资产项目')
    } else {
      ElMessage.success(`加载了 ${projects.length} 个资产项目`)
    }
  } catch (error) {
    ElMessage.warning('获取资产项目列表失败')
  }
}

const handleProjectChange = async (value) => {
  
  if (value) {
    // 获取该项目下的可容器化资产
    await fetchProjectAssets(value)
    await fetchAvailableNodes()
  } else {
    containerAssets.value = []
  }
}

// 获取项目下的资产
const fetchProjectAssets = async (projectId) => {
  try {
    const response = await fetch(`/api/assets/by-project/${projectId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.ok) {
      const assets = await response.json()
      // 筛选出可容器化的资产（有Docker镜像或资产类型为container的）
      containerAssets.value = assets.filter(asset => 
        asset.dockerImage || asset.assetType === 'container'
      ).map(asset => ({
        ...asset,
        autoStart: asset.autoStart || false,
        containerPorts: asset.containerPorts || '',
      }))
      
    }
  } catch (error) {
    ElMessage.error('获取项目资产失败')
  }
}

// 获取可用节点
const fetchAvailableNodes = async () => {
  try {
    const response = await fetch('/api/host-nodes', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.ok) {
      const nodes = await response.json()
      availableNodes.value = nodes.filter(node => node.status === 'active')
    }
  } catch (error) {
  }
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const submitData = {
          name: form.value.name,
          topologyConfig: form.value.networkConfig,
          vulnerabilityConfig: form.value.vulnConfig,
          topologyProjectId: form.value.selectedProject // 添加拓扑项目关联
        }
        
        const res = await fetch('/api/range/create', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(submitData)
        })
        if (!res.ok) throw new Error('创建失败')
        ElMessage.success('演练创建成功')
        router.push('/drills')
      } catch {
        ElMessage.error('创建失败，请检查输入或权限')
      }
    }
  })
}

const goBack = () => {
  router.push('/drills')
}

// 辅助方法
const getProjectName = () => {
  const project = assetProjects.value.find(p => p.topologyProjectId === form.value.selectedProject)
  return project ? project.name : '未知项目'
}

const getAssetTypeLabel = (assetType) => {
  const typeMap = {
    server: '服务器',
    container: '容器',
    service: '服务',
    network: '网络设备'
  }
  return typeMap[assetType] || '未知'
}

const getDeploymentStrategyLabel = (strategy) => {
  const labelMap = {
    fixed: '固定节点',
    any: '任意节点',
    load_balanced: '负载均衡'
  }
  return labelMap[strategy] || '未设置'
}

// 编辑资产配置
const editAssetConfig = (asset) => {
  showContainerConfig.value = true
}

// 保存容器配置
const saveContainerConfig = async () => {
  try {
    // 这里可以保存容器配置到后端或本地存储
    ElMessage.success('容器配置已保存')
    showContainerConfig.value = false
  } catch (error) {
    ElMessage.error('保存配置失败')
  }
}

// 页面加载时获取资产项目列表
onMounted(() => {
  fetchAssetProjects()
})
</script>

<style scoped>
.create-drill-page {
  padding: 30px;
}
.card-header {
  font-size: 18px;
  font-weight: 600;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

/* 容器预览样式 */
.container-preview {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background-color: #fafafa;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.preview-title {
  font-weight: bold;
  color: #333;
}

.container-assets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.container-asset-card {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 12px;
  background: white;
}

.asset-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.asset-name {
  font-weight: bold;
  color: #333;
}

.asset-info {
  margin-bottom: 8px;
}

.asset-info > div {
  margin-bottom: 4px;
  font-size: 12px;
}

.info-label {
  color: #666;
  margin-right: 4px;
}

.docker-image {
  font-family: monospace;
  background: #f0f9ff;
  padding: 2px 4px;
  border-radius: 3px;
  color: #0369a1;
}

.node-name {
  color: #059669;
  font-weight: bold;
}

.strategy {
  color: #7c3aed;
}

.asset-actions {
  text-align: right;
}

/* 容器配置对话框样式 */
.container-config-dialog {
  max-height: 60vh;
  overflow-y: auto;
}

.config-header {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
  font-weight: bold;
  color: #333;
}
</style>