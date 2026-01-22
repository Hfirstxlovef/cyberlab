-- 性能优化：添加数据库索引以提升查询效率
-- 创建时间：2025-01-XX

-- 1. system_log表索引（高频查询日志列表）
-- CREATE INDEX idx_system_log_username ON system_log(username); -- Already exists
-- CREATE INDEX idx_system_log_timestamp ON system_log(timestamp DESC); -- Already exists
-- CREATE INDEX idx_system_log_log_level ON system_log(log_level); -- Already exists
-- CREATE INDEX idx_system_log_log_category ON system_log(log_category); -- Already exists
-- CREATE INDEX idx_system_log_composite ON system_log(username, timestamp DESC, log_level); -- Already exists

-- 2. asset表索引（资产管理和查询优化）
-- CREATE INDEX idx_asset_topology_project_id ON asset(topology_project_id); -- Already exists
-- CREATE INDEX idx_asset_enabled ON asset(enabled); -- Already exists
-- CREATE INDEX idx_asset_asset_type ON asset(asset_type); -- Already exists
-- CREATE INDEX idx_asset_preferred_host_node_id ON asset(preferred_host_node_id); -- Already exists
-- CREATE INDEX idx_asset_composite ON asset(topology_project_id, enabled, asset_type); -- Already exists

-- 3. container_states表索引（容器状态同步核心表）
-- CREATE INDEX idx_container_states_asset_id ON container_states(asset_id); -- Already exists
-- CREATE INDEX idx_container_states_host_node_id ON container_states(host_node_id); -- Already exists
-- CREATE INDEX idx_container_states_sync_status ON container_states(sync_status); -- Already exists
-- CREATE INDEX idx_container_states_current_status ON container_states(current_status); -- Already exists
-- CREATE INDEX idx_container_states_composite ON container_states(sync_status, current_status, host_node_id); -- Already exists

-- 4. drill_containers表索引（演练容器查询优化）
-- CREATE INDEX idx_drill_containers_range_id ON drill_containers(range_id); -- Already exists
-- CREATE INDEX idx_drill_containers_status ON drill_containers(status); -- Already exists
-- CREATE INDEX idx_drill_containers_host_node_id ON drill_containers(host_node_id); -- Already exists
-- CREATE INDEX idx_drill_containers_composite ON drill_containers(range_id, status); -- Already exists

-- 5. achievements表索引（成就提交和评审优化）
-- CREATE INDEX idx_achievements_range_id ON achievements(range_id); -- Already exists
-- CREATE INDEX idx_achievements_team_name ON achievements(team_name); -- Already exists
-- CREATE INDEX idx_achievements_team_type ON achievements(team_type); -- Already exists
-- CREATE INDEX idx_achievements_status ON achievements(status); -- Already exists
-- CREATE INDEX idx_achievements_composite ON achievements(range_id, team_type, status); -- Already exists

-- 6. host_nodes表索引（主机节点查询优化）
-- CREATE INDEX idx_host_nodes_status ON host_nodes(status); -- Already exists
CREATE INDEX idx_host_nodes_last_health_check ON host_nodes(last_health_check);
CREATE INDEX idx_host_nodes_composite ON host_nodes(status, last_health_check);

-- 7. users表索引（用户认证和查询优化）
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_team_id ON users(team_id);
CREATE INDEX idx_users_enabled ON users(enabled);

-- 8. scenario_templates表索引（场景模板查询优化）
CREATE INDEX idx_scenario_templates_scenario_type ON scenario_templates(scenario_type);
CREATE INDEX idx_scenario_templates_is_active ON scenario_templates(is_active);
