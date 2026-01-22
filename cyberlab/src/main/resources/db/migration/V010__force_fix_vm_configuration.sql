-- 强制修复VM配置 - 确保所有数据正确指向172.16.190.130:2375
-- 这个脚本将彻底解决主机节点和资产关联问题

-- 1. 删除所有可能的错误配置节点
DELETE FROM host_nodes WHERE host_ip IN ('192.168.1.100', '127.0.0.1') AND name != 'localhost';

-- 2. 强制插入或更新正确的VM节点配置
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, ssh_port, cluster_name,
    node_type, environment, status, description,
    cpu_cores, memory_total, disk_total,
    docker_tls_enabled, docker_cert_path,
    monitoring_enabled, max_containers, priority,
    labels, metadata,
    created_at, updated_at
) VALUES (
    '测试节点01', 
    '测试节点01 - Ubuntu VM', 
    '172.16.190.130', 
    2375,
    22, 
    'cyberlab-test-cluster',
    'vm', 
    'testing', 
    'active', 
    '苏州科技大学测试环境Ubuntu虚拟机，Docker API端口2375，用于容器管理和安全演练',
    4, 
    4096, 
    50,
    FALSE,
    NULL,
    TRUE, 
    30, 
    5,
    '{"environment": "testing", "location": "suzhou", "purpose": "container-discovery"}',
    '{"university": "苏州科技大学", "lab": "红岸网络空间安全仿真攻防实验室"}',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    host_ip = '172.16.190.130',
    docker_port = 2375,
    ssh_port = 22,
    cluster_name = 'cyberlab-test-cluster',
    node_type = 'vm',
    environment = 'testing',
    status = 'active',
    description = '苏州科技大学测试环境Ubuntu虚拟机，Docker API端口2375，用于容器管理和安全演练',
    cpu_cores = 4,
    memory_total = 4096,
    disk_total = 50,
    docker_tls_enabled = FALSE,
    docker_cert_path = NULL,
    monitoring_enabled = TRUE,
    max_containers = 30,
    priority = 5,
    labels = '{"environment": "testing", "location": "suzhou", "purpose": "container-discovery"}',
    metadata = '{"university": "苏州科技大学", "lab": "红岸网络空间安全仿真攻防实验室"}',
    updated_at = NOW();

-- 3. 更新所有资产的IP地址和主机节点关联
-- 首先更新资产IP地址
UPDATE asset 
SET ip = '172.16.190.130', 
    updated_at = NOW()
WHERE ip = '192.168.1.100' OR ip IS NULL OR ip = '';

-- 4. 获取VM节点ID并更新资产关联
SET @vm_node_id = (SELECT id FROM host_nodes WHERE host_ip = '172.16.190.130' LIMIT 1);

-- 5. 为所有测试项目的资产设置正确的节点关联
UPDATE asset 
SET preferred_host_node_id = @vm_node_id,
    preferred_host_node_name = '测试节点01 - Ubuntu VM',
    deployment_strategy = CASE 
        WHEN name LIKE '%Web服务器%' OR name LIKE '%数据库服务器%' THEN 'fixed'
        ELSE 'any'
    END,
    updated_at = NOW()
WHERE (company = '红岸实验室' OR company LIKE '%苏州科技大学%')
  AND (preferred_host_node_id IS NULL OR preferred_host_node_id NOT IN (
      SELECT id FROM host_nodes WHERE host_ip = '172.16.190.130'
  ));

-- 6. 清理可能存在的孤立容器记录
UPDATE drill_containers 
SET host_node_id = @vm_node_id,
    host_node_name = '测试节点01 - Ubuntu VM',
    host_node_ip = '172.16.190.130',
    updated_at = NOW()
WHERE (host_node_ip = '192.168.1.100' OR host_node_ip IS NULL)
  AND host_node_id != @vm_node_id;

-- 7. 插入配置验证记录
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, node_type, environment, status, description,
    cpu_cores, memory_total, disk_total, monitoring_enabled, max_containers, priority,
    created_at, updated_at
) VALUES (
    'config-verification', 
    '配置验证记录', 
    '0.0.0.1', 
    0, 
    'system', 
    'maintenance', 
    'inactive',
    CONCAT('VM配置强制修复完成 - ', NOW(), ' - 节点ID: ', @vm_node_id, ' - 已将所有配置统一指向172.16.190.130:2375'),
    0, 0, 0, FALSE, 0, 0,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    description = CONCAT('VM配置强制修复完成 - ', NOW(), ' - 节点ID: ', @vm_node_id, ' - 已将所有配置统一指向172.16.190.130:2375'),
    updated_at = NOW();

-- 8. 验证修复结果的查询（插入日志记录）
-- 创建一个临时表来存储验证结果
CREATE TEMPORARY TABLE IF NOT EXISTS fix_verification AS
SELECT 
    'host_nodes' as table_name,
    COUNT(*) as vm_nodes_count,
    GROUP_CONCAT(CONCAT(name, '(', host_ip, ':', docker_port, ')') SEPARATOR ', ') as vm_nodes_info
FROM host_nodes 
WHERE host_ip = '172.16.190.130';

INSERT INTO fix_verification
SELECT 
    'assets_with_vm_node' as table_name,
    COUNT(*) as count,
    GROUP_CONCAT(CONCAT(name, '(', ip, ')') SEPARATOR ', ') as info
FROM asset 
WHERE preferred_host_node_id = @vm_node_id;

-- 记录验证结果到系统日志表（如果存在）
-- 由于我们没有专门的日志表，使用host_nodes表的description字段记录
UPDATE host_nodes 
SET description = CONCAT(
    description, 
    ' | 验证结果: VM节点数=', 
    (SELECT vm_nodes_count FROM fix_verification WHERE table_name = 'host_nodes'),
    ', 关联资产数=',
    (SELECT count FROM fix_verification WHERE table_name = 'assets_with_vm_node')
)
WHERE name = 'config-verification';

DROP TEMPORARY TABLE fix_verification;