-- 数据库迁移脚本：为Asset表添加k8s_port字段
-- 目的：支持Kubernetes默认端口6443配置，与Docker端口配置保持对称
-- 作者：CyberLab Team
-- 日期：2025-10-27

-- 添加Kubernetes端口字段
ALTER TABLE asset ADD COLUMN k8s_port INT DEFAULT 6443 COMMENT 'Kubernetes API端口，默认6443';

-- 为现有k8s资产设置默认端口
UPDATE asset SET k8s_port = 6443 WHERE asset_platform IN ('k8s', 'both') AND k8s_port IS NULL;

-- 为k8s_port字段添加索引（优化查询性能）
CREATE INDEX idx_asset_k8s_port ON asset(k8s_port);
