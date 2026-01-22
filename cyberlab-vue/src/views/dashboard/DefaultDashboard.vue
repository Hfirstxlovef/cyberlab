<template>
  <div class="default-dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>🎯 欢迎使用网络安全攻防演练平台</span>
        </div>
      </template>
      
      <div class="welcome-content">
        <div class="welcome-message">
          <h2>欢迎来到网络安全攻防演练平台</h2>
          <p>请联系管理员为您分配相应的角色权限，以便使用平台的完整功能。</p>
        </div>
        
        <div class="feature-grid">
          <div class="feature-item">
            <el-icon class="feature-icon" color="#409eff"><Lock /></el-icon>
            <h3>安全防护</h3>
            <p>全面的网络安全防护体系</p>
          </div>
          
          <div class="feature-item">
            <el-icon class="feature-icon" color="#67c23a"><Medal /></el-icon>
            <h3>攻防演练</h3>
            <p>真实的攻防对抗演练环境</p>
          </div>
          
          <div class="feature-item">
            <el-icon class="feature-icon" color="#e6a23c"><TrendCharts /></el-icon>
            <h3>数据分析</h3>
            <p>详细的演练数据统计分析</p>
          </div>
          
          <div class="feature-item">
            <el-icon class="feature-icon" color="#f56c6c"><Files /></el-icon>
            <h3>成果管理</h3>
            <p>完整的攻防成果记录管理</p>
          </div>
        </div>
        
        <div class="contact-info">
          <el-alert
            title="需要帮助？"
            type="info"
            description="如需获取系统权限或遇到技术问题，请联系系统管理员。"
            show-icon
            :closable="false">
          </el-alert>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Lock, Medal, TrendCharts, Files } from '@element-plus/icons-vue'
</script>

<style scoped>
.default-dashboard {
  padding: 20px;
  height: 100%;
}

.welcome-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.welcome-content {
  text-align: center;
}

.welcome-message {
  margin-bottom: 40px;
}

.welcome-message h2 {
  color: #303133;
  margin-bottom: 16px;
  font-size: 28px;
}

.welcome-message p {
  color: #606266;
  font-size: 16px;
  line-height: 1.6;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
  margin-bottom: 40px;
}

.feature-item {
  padding: 30px 20px;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: #f0f9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-item h3 {
  color: #303133;
  margin-bottom: 12px;
  font-size: 18px;
}

.feature-item p {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.contact-info {
  max-width: 600px;
  margin: 0 auto;
}
</style>