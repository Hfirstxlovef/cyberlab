-- 为资产表添加拓扑项目ID字段
-- 用于将资产与拓扑图数据关联，解决资产中心拓扑图显示问题

ALTER TABLE asset 
ADD COLUMN topology_project_id VARCHAR(255) COMMENT '关联的拓扑项目ID';

-- 为苏州科技大学项目的资产建立拓扑关联（假设已有拓扑数据）
-- 这里使用示例数据，实际部署时需要根据具体情况调整
UPDATE asset 
SET topology_project_id = 'suzhou-university-cyberlab' 
WHERE company = '苏州科技大学' 
  AND project = 'CyberLab Inc.｜苏州科技大学';

-- 可选：为表添加索引以提升查询性能
CREATE INDEX idx_asset_topology_project_id ON asset(topology_project_id);