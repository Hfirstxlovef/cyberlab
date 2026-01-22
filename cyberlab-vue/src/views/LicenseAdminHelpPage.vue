<template>
  <div class="license-help-page">
    <div class="page-header">
      <h2>💡 使用帮助</h2>
      <p class="page-description">红岸授权管理系统使用指南</p>
    </div>

    <el-card class="apple-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">📖</span>
          <span class="header-title">授权管理指南</span>
        </div>
      </template>

      <div class="help-content">
        <section class="help-section">
          <h3>👤 角色说明</h3>
          <p>您当前的角色是：<el-tag type="warning" size="large">红岸授权员 (license_admin)</el-tag></p>
          <p>作为授权管理员，您拥有以下权限：</p>
          <ul>
            <li>✅ 生成新的系统授权码</li>
            <li>✅ 管理现有授权（激活、停用、延期）</li>
            <li>✅ 设置系统当前使用的授权</li>
            <li>✅ 查看授权历史记录</li>
            <li>✅ 查看系统用户信息</li>
            <li>❌ 无法访问系统其他功能模块</li>
          </ul>
        </section>

        <section class="help-section">
          <h3>🎫 授权码管理</h3>
          <h4>生成新授权</h4>
          <ol>
            <li>在"授权码管理"页面找到"生成新授权"表单</li>
            <li>填写以下必填信息：
              <ul>
                <li><strong>授权给</strong>：客户名称或组织名称</li>
                <li><strong>版本</strong>：选择 PRO（专业版）、ENTERPRISE（企业版）或 TRIAL（试用版）</li>
                <li><strong>有效期至</strong>：授权过期日期</li>
                <li><strong>备注</strong>（可选）：其他说明信息</li>
              </ul>
            </li>
            <li>点击"生成授权码"按钮</li>
            <li>系统自动生成序列号和授权码</li>
          </ol>

          <h4>授权码格式</h4>
          <div class="code-block">
            <p><strong>序列号示例：</strong></p>
            <code>CYBERLAB-2025-12-31-00001</code>
            <p class="format-desc">格式：CYBERLAB-[过期日期]-[唯一标识]</p>
          </div>

          <div class="code-block">
            <p><strong>授权码示例：</strong></p>
            <code>CL-ZL3B4T34M-PRO2025-ACTIVE-A7F3D2E8</code>
            <p class="format-desc">格式：CL-ZL3B4T34M-[版本][年份]-ACTIVE-[校验码]</p>
          </div>
        </section>

        <section class="help-section">
          <h3>⚙️ 授权操作</h3>
          <h4>设为当前授权</h4>
          <p>将某个授权设置为系统当前使用的授权：</p>
          <ol>
            <li>在授权历史列表中找到目标授权（状态必须为 ACTIVE）</li>
            <li>点击"设为当前"按钮</li>
            <li>确认操作后，系统会自动同步到系统配置</li>
            <li>所有用户下次登录时将使用新授权验证</li>
          </ol>
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>注意：设为当前授权会影响所有用户的登录验证</template>
          </el-alert>

          <h4>延长授权有效期</h4>
          <ol>
            <li>找到带有 ⭐ 标记的当前授权</li>
            <li>点击"延期"按钮</li>
            <li>选择新的过期日期（必须晚于当前日期）</li>
            <li>确认延期操作</li>
          </ol>

          <h4>激活/停用授权</h4>
          <ul>
            <li><strong>停用</strong>：将不再使用的授权状态设为 INACTIVE</li>
            <li><strong>激活</strong>：将已停用的授权重新激活为 ACTIVE</li>
            <li>注意：无法停用当前正在使用的授权</li>
          </ul>

          <h4>删除授权</h4>
          <p>永久删除授权记录（不可恢复）：</p>
          <ol>
            <li>只能删除非当前授权</li>
            <li>点击"删除"按钮</li>
            <li>需要二次确认操作</li>
          </ol>
        </section>

        <section class="help-section">
          <h3>👥 用户管理</h3>
          <p>在"用户管理"页面，您可以：</p>
          <ul>
            <li>查看所有系统用户列表</li>
            <li>查看用户的角色和状态信息</li>
            <li>搜索特定用户</li>
            <li>查看用户详细信息</li>
          </ul>
          <el-alert type="info" :closable="false" show-icon>
            <template #title>说明：授权管理员只有查看权限，无法创建、编辑或删除用户</template>
          </el-alert>
        </section>

        <section class="help-section">
          <h3>⚠️ 常见问题</h3>
          <h4>Q：忘记 hongan 密码怎么办？</h4>
          <p>A：联系系统管理员通过数据库重置密码。</p>

          <h4>Q：授权过期后会怎样？</h4>
          <p>A：</p>
          <ul>
            <li>所有用户登录后会看到全屏过期提示</li>
            <li>系统功能全部禁用</li>
            <li>需要延长授权或设置新授权才能恢复</li>
          </ul>

          <h4>Q：可以同时有多个 ACTIVE 授权吗？</h4>
          <p>A：可以。但只有标记为"当前"的授权会被系统实际使用。</p>

          <h4>Q：如何备份授权信息？</h4>
          <p>A：授权信息存储在数据库 `licenses` 表中，建议定期备份数据库。</p>
        </section>

        <section class="help-section">
          <h3>📞 技术支持</h3>
          <div class="contact-info">
            <p><strong>蟑螂恶霸团队 (Cockroach Bully Team)</strong></p>
            <p>📧 Email: <a href="mailto:sun740883686@foxmail.com">sun740883686@foxmail.com</a></p>
            <p>🌐 项目：红岸网络空间安全对抗平台 (CyberLab)</p>
          </div>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// 纯静态帮助页面，无需响应式数据
</script>

<style scoped>
.license-help-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: var(--spacing-lg, 32px);
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: var(--font-3xl, 32px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--apple-text-primary, #1d1d1f);
}

.page-description {
  margin: 0;
  font-size: var(--font-md, 16px);
  color: var(--apple-text-secondary, #6e6e73);
}

/* Apple 风格卡片 */
.apple-card {
  border-radius: var(--radius-xl, 20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 8px 32px rgba(0, 0, 0, 0.03);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
}

.header-title {
  flex: 1;
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

/* 帮助内容 */
.help-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}

.help-section {
  margin-bottom: 40px;
  padding-bottom: 30px;
  border-bottom: 1px solid #eee;
}

.help-section:last-child {
  border-bottom: none;
}

.help-section h3 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #1d1d1f;
}

.help-section h4 {
  font-size: 18px;
  font-weight: 500;
  margin: 20px 0 12px 0;
  color: #333;
}

.help-section p {
  margin: 12px 0;
}

.help-section ul,
.help-section ol {
  margin: 12px 0;
  padding-left: 24px;
}

.help-section li {
  margin: 8px 0;
}

.help-section ul ul {
  margin-top: 8px;
}

.code-block {
  background: #f5f5f7;
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
}

.code-block code {
  display: block;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 14px;
  color: #0066cc;
  margin: 8px 0;
}

.format-desc {
  font-size: 13px;
  color: #86868b;
  margin-top: 8px;
}

.contact-info {
  background: #f5f5f7;
  border-radius: 12px;
  padding: 20px;
  margin-top: 16px;
}

.contact-info p {
  margin: 8px 0;
}

.contact-info a {
  color: #0066cc;
  text-decoration: none;
}

.contact-info a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 768px) {
  .license-help-page {
    padding: var(--spacing-sm, 16px);
  }

  .help-content {
    font-size: 14px;
  }

  .help-section h3 {
    font-size: 20px;
  }

  .help-section h4 {
    font-size: 16px;
  }
}
</style>
