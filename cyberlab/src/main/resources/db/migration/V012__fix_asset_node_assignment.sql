-- V012 - 修复资产节点分配错误问题
-- 解决test等资产被错误地全部关联到同一个节点的问题

-- 1. 重置被错误关联的资产，只保留真正需要固定节点的资产
-- 识别哪些资产应该使用固定节点策略，哪些应该使用智能分配

-- 重置除了明确需要固定节点的资产外的所有资产关联
UPDATE asset 
SET preferred_host_node_id = NULL,
    preferred_host_node_name = NULL,
    deployment_strategy = 'any',
    ip = NULL,
    updated_at = NOW()
WHERE name NOT LIKE '%Web服务器%' 
  AND name NOT LIKE '%数据库服务器%'
  AND name NOT LIKE '%关键服务%'
  AND asset_type != 'server'
  AND (preferred_host_node_id IS NOT NULL OR deployment_strategy = 'fixed');

-- 2. 确保test资产使用智能分配策略
UPDATE asset 
SET preferred_host_node_id = NULL,
    preferred_host_node_name = NULL,
    deployment_strategy = 'any',
    ip = NULL,
    notes = CONCAT(COALESCE(notes, ''), ' - 使用智能节点分配'),
    updated_at = NOW()
WHERE name LIKE '%test%' 
   OR name LIKE '%测试%'
   OR name = 'test';

-- 3. 保留真正需要固定节点的资产（Web服务器、数据库服务器等）
-- 这些资产保持固定节点分配
UPDATE asset 
SET deployment_strategy = 'fixed',
    notes = CONCAT(COALESCE(notes, ''), ' - 固定节点部署'),
    updated_at = NOW()
WHERE (name LIKE '%Web服务器%' 
       OR name LIKE '%数据库服务器%'
       OR name LIKE '%关键服务%'
       OR asset_type = 'server')
  AND preferred_host_node_id IS NOT NULL;

-- 4. 为container类型的资产设置合适的默认策略
UPDATE asset 
SET deployment_strategy = CASE 
    WHEN name LIKE '%生产%' OR name LIKE '%prod%' THEN 'load_balanced'
    WHEN name LIKE '%测试%' OR name LIKE '%test%' OR name LIKE '%dev%' THEN 'any'
    ELSE 'any'
END,
ip = NULL,
updated_at = NOW()
WHERE asset_type = 'container' 
  AND deployment_strategy IS NULL;

-- 5. 添加一些多样化的测试节点（如果不存在的话）
-- 这样可以让系统有更多节点选择，避免所有资产都分配到同一个节点

-- 检查是否需要添加更多测试节点
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, ssh_port, cluster_name,
    node_type, environment, status, description,
    cpu_cores, memory_total, disk_total,
    docker_tls_enabled, docker_cert_path,
    monitoring_enabled, max_containers, priority,
    labels, metadata,
    created_at, updated_at
) 
SELECT 
    '测试节点02',
    '测试节点02 - Docker节点',
    '172.16.190.131',
    2375, 22, 'cyberlab-test-cluster',
    'docker', 'testing', 'active',
    '苏州科技大学测试环境Docker节点，用于容器管理和负载分担',
    2, 2048, 30,
    FALSE, NULL,
    TRUE, 20, 3,
    '{"environment": "testing", "location": "suzhou", "purpose": "load-balancing"}',
    '{"university": "苏州科技大学", "lab": "红岸网络空间安全仿真攻防实验室", "role": "secondary"}',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM host_nodes 
    WHERE name = '测试节点02' OR host_ip = '172.16.190.131'
);

INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, ssh_port, cluster_name,
    node_type, environment, status, description,
    cpu_cores, memory_total, disk_total,
    docker_tls_enabled, docker_cert_path,
    monitoring_enabled, max_containers, priority,
    labels, metadata,
    created_at, updated_at
) 
SELECT 
    '测试节点03',
    '测试节点03 - 轻量级节点',
    '172.16.190.132',
    2375, 22, 'cyberlab-test-cluster',
    'lightweight', 'development', 'active',
    '苏州科技大学开发环境轻量级节点，用于开发和测试',
    1, 1024, 20,
    FALSE, NULL,
    TRUE, 15, 2,
    '{"environment": "development", "location": "suzhou", "purpose": "development"}',
    '{"university": "苏州科技大学", "lab": "红岸网络空间安全仿真攻防实验室", "role": "development"}',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM host_nodes 
    WHERE name = '测试节点03' OR host_ip = '172.16.190.132'
);

-- 6. 记录修复操作
INSERT INTO system_log (level, source, action, message, details, created_at) VALUES 
('INFO', 'DATABASE_MIGRATION', 'V012_ASSET_NODE_FIX', '资产节点分配错误修复完成', 
CONCAT('重置资产数量: ', 
       (SELECT COUNT(*) FROM asset WHERE preferred_host_node_id IS NULL AND deployment_strategy = 'any'),
       ', 固定节点资产数量: ',
       (SELECT COUNT(*) FROM asset WHERE preferred_host_node_id IS NOT NULL AND deployment_strategy = 'fixed'),
       ', 可用节点数量: ',
       (SELECT COUNT(*) FROM host_nodes WHERE status = 'active')), 
NOW())
ON DUPLICATE KEY UPDATE id=id;

-- 7. 验证修复结果
SELECT 
    '修复验证' as section,
    '智能分配资产数量' as metric, 
    COUNT(*) as value,
    'test等资产应使用智能分配' as description
FROM asset 
WHERE deployment_strategy = 'any' AND preferred_host_node_id IS NULL
UNION ALL
SELECT 
    '修复验证' as section,
    '固定节点资产数量' as metric, 
    COUNT(*) as value,
    'Web服务器等关键资产使用固定节点' as description
FROM asset 
WHERE deployment_strategy = 'fixed' AND preferred_host_node_id IS NOT NULL
UNION ALL
SELECT 
    '修复验证' as section,
    '可用节点数量' as metric, 
    COUNT(*) as value,
    '用于负载分担的活跃节点' as description
FROM host_nodes 
WHERE status = 'active'
UNION ALL
SELECT 
    '修复验证' as section,
    'test资产配置' as metric, 
    COUNT(*) as value,
    'test资产应该使用any策略' as description
FROM asset 
WHERE (name LIKE '%test%' OR name = 'test') 
  AND deployment_strategy = 'any' 
  AND preferred_host_node_id IS NULL;