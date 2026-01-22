-- 数据库迁移脚本：为Asset表添加自动探测字段
-- 目的：支持基于资产IP自动探测Docker容器和Kubernetes集群
-- 作者：CyberLab Team
-- 日期：2025-01-15

-- 添加平台类型字段
ALTER TABLE asset ADD COLUMN asset_platform VARCHAR(20) DEFAULT 'docker' COMMENT '资产平台类型：docker/k8s/both';

-- 添加Docker API配置字段
ALTER TABLE asset ADD COLUMN docker_port INT DEFAULT 2375 COMMENT 'Docker API端口';
ALTER TABLE asset ADD COLUMN docker_api_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用Docker API探测';

-- 添加Kubernetes API配置字段
ALTER TABLE asset ADD COLUMN k8s_api_server VARCHAR(255) COMMENT 'Kubernetes API Server地址';
ALTER TABLE asset ADD COLUMN k8s_token VARCHAR(512) COMMENT 'Kubernetes访问令牌';

-- 添加探测状态字段
ALTER TABLE asset ADD COLUMN probe_status VARCHAR(50) DEFAULT 'not_configured' COMMENT '探测状态：success/failed/not_configured/probing';
ALTER TABLE asset ADD COLUMN last_probe_time VARCHAR(50) COMMENT '最后探测时间';
ALTER TABLE asset ADD COLUMN probe_error_message VARCHAR(500) COMMENT '探测失败的错误信息';

-- 为常用查询添加索引
CREATE INDEX idx_asset_probe_status ON asset(probe_status);
CREATE INDEX idx_asset_platform ON asset(asset_platform);
CREATE INDEX idx_asset_project_platform ON asset(project, asset_platform);

-- 更新现有资产的默认值
UPDATE asset
SET
    asset_platform = 'docker',
    docker_port = 2375,
    docker_api_enabled = TRUE,
    probe_status = 'not_configured'
WHERE asset_platform IS NULL;

-- 为已有IP地址的资产自动设置Docker API探测状态
UPDATE asset
SET probe_status = 'success'
WHERE ip IS NOT NULL
  AND ip != ''
  AND docker_api_enabled = TRUE
  AND probe_status = 'not_configured';
