-- 扩展drill_containers表，添加资产关联和安全配置字段
-- V004__alter_drill_containers_add_asset_fields.sql

ALTER TABLE drill_containers 
ADD COLUMN asset_id BIGINT COMMENT '关联的资产ID' AFTER range_id,
ADD COLUMN scenario_template_id BIGINT COMMENT '关联的场景模板ID' AFTER asset_id,
ADD COLUMN security_config TEXT COMMENT '安全配置(JSON格式)' AFTER log_path,
ADD COLUMN network_config TEXT COMMENT '网络配置(JSON格式)' AFTER security_config,
ADD COLUMN resource_limits TEXT COMMENT '资源限制(JSON格式)' AFTER network_config,
ADD COLUMN environment_vars TEXT COMMENT '环境变量(JSON格式)' AFTER resource_limits,
ADD COLUMN volume_mounts TEXT COMMENT '卷挂载配置(JSON格式)' AFTER environment_vars,
ADD COLUMN exposed_ports TEXT COMMENT '端口映射配置(JSON格式)' AFTER volume_mounts,
ADD COLUMN auto_start BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否自动启动' AFTER exposed_ports,
ADD COLUMN restart_policy VARCHAR(50) DEFAULT 'no' COMMENT '重启策略：no, on-failure, always, unless-stopped' AFTER auto_start,
ADD COLUMN health_check_config TEXT COMMENT '健康检查配置(JSON格式)' AFTER restart_policy;

-- 添加索引
CREATE INDEX idx_drill_containers_asset_id ON drill_containers(asset_id);
CREATE INDEX idx_drill_containers_scenario_template_id ON drill_containers(scenario_template_id);
CREATE INDEX idx_drill_containers_auto_start ON drill_containers(auto_start);