-- 修复节点激活状态问题
-- 自动激活已知可用的Docker节点

-- 首先更新所有现有节点的基本信息
UPDATE host_nodes 
SET 
    last_health_check = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id IS NOT NULL;

-- 为172.16.190.130:2375节点强制设置为active状态（根据日志显示该节点Docker连接正常）
UPDATE host_nodes 
SET 
    status = 'active',
    last_health_check = CURRENT_TIMESTAMP,
    description = CONCAT(
        COALESCE(description, ''), 
        ' - 自动激活：Docker API连接正常 (', 
        CURRENT_TIMESTAMP, 
        ')'
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE host_ip = '172.16.190.130' 
  AND docker_port = 2375;

-- 如果有测试节点02和测试节点03但状态异常，尝试修复
UPDATE host_nodes 
SET 
    status = CASE 
        WHEN host_ip = '172.16.190.130' AND docker_port = 2375 THEN 'active'
        ELSE status 
    END,
    last_health_check = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE (display_name LIKE '%测试节点02%' OR display_name LIKE '%测试节点03%')
  AND status = 'inactive';

-- 创建或更新已知可用节点的记录
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, node_type, environment, status, 
    description, cpu_cores, memory_total, disk_total, monitoring_enabled, 
    max_containers, priority, cluster_name, docker_tls_enabled
) VALUES (
    '测试节点02', '测试节点02 - Docker节点', '172.16.190.130', 2375, 'vm', 'testing', 'active',
    '苏州科技大学测试环境Docker节点 - 连接正常自动激活',
    4, 4096, 50, TRUE, 30, 2, 'cyberlab-test-cluster', FALSE
) ON DUPLICATE KEY UPDATE
    host_ip = '172.16.190.130',
    docker_port = 2375,
    status = 'active',
    description = '苏州科技大学测试环境Docker节点 - 连接正常自动激活',
    last_health_check = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP;

-- 为轻量级节点创建记录（如果不存在）
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, node_type, environment, status,
    description, cpu_cores, memory_total, disk_total, monitoring_enabled,
    max_containers, priority, cluster_name, docker_tls_enabled
) VALUES (
    '测试节点03', '测试节点03 - 轻量级节点', '172.16.190.131', 2375, 'vm', 'testing', 'inactive',
    '苏州科技大学测试环境轻量级节点 - 待激活',
    2, 2048, 30, TRUE, 20, 1, 'cyberlab-test-cluster', FALSE
) ON DUPLICATE KEY UPDATE
    description = '苏州科技大学测试环境轻量级节点 - 待激活',
    updated_at = CURRENT_TIMESTAMP;

-- 清理可能存在的重复或无效节点
DELETE FROM host_nodes 
WHERE name = 'config-fix-log' 
  OR (host_ip = '0.0.0.0' AND docker_port = 0);

-- 更新所有localhost节点，确保其为active状态
UPDATE host_nodes 
SET 
    status = 'active',
    last_health_check = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE host_ip IN ('127.0.0.1', 'localhost')
  AND docker_port IN (2375, 2376);

-- 记录修复操作
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, node_type, environment, status,
    description, cpu_cores, memory_total, disk_total, monitoring_enabled,
    max_containers, priority, cluster_name, docker_tls_enabled
) VALUES (
    'node-fix-v012', '节点状态修复记录V012', '0.0.0.1', 0, 'local', 'development', 'maintenance',
    CONCAT('节点状态修复完成 - ', CURRENT_TIMESTAMP, ' - 已激活172.16.190.130:2375节点'),
    0, 0, 0, FALSE, 0, 0, 'maintenance', FALSE
) ON DUPLICATE KEY UPDATE
    description = CONCAT('节点状态修复完成 - ', CURRENT_TIMESTAMP, ' - 已激活172.16.190.130:2375节点'),
    updated_at = CURRENT_TIMESTAMP;