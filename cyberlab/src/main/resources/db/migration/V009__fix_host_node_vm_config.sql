-- 修复主机节点配置，添加真实虚拟机节点
-- 更新或添加测试环境的VM节点配置

-- 首先检查是否已有测试节点01，如果有则更新，否则插入
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, node_type, environment, status, description,
    cpu_cores, memory_total, disk_total, monitoring_enabled, max_containers, priority,
    cluster_name, docker_tls_enabled
) VALUES (
    '测试节点01', '测试节点01 - Ubuntu VM', '172.16.190.130', 2375, 'vm', 'testing', 'active', 
    '苏州科技大学测试环境Ubuntu虚拟机，用于容器管理演练',
    4, 4096, 50, TRUE, 30, 2,
    'cyberlab-test-cluster', FALSE
) ON DUPLICATE KEY UPDATE
    host_ip = '172.16.190.130',
    docker_port = 2375,
    node_type = 'vm',
    environment = 'testing',
    status = 'active',
    description = '苏州科技大学测试环境Ubuntu虚拟机，用于容器管理演练',
    cluster_name = 'cyberlab-test-cluster',
    docker_tls_enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 如果之前有192.168.1.100的测试数据，将其更新为新的VM地址
UPDATE host_nodes 
SET 
    host_ip = '172.16.190.130',
    docker_port = 2375,
    display_name = '测试节点01 - Ubuntu VM',
    description = '苏州科技大学测试环境Ubuntu虚拟机，用于容器管理演练',
    node_type = 'vm',
    environment = 'testing',
    cluster_name = 'cyberlab-test-cluster',
    docker_tls_enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE host_ip = '192.168.1.100' OR name LIKE '%测试节点%' OR name LIKE '%test%';

-- 更新资产表中可能关联了错误IP的资产
-- 将所有原来可能关联到192.168.1.100主机的资产重新关联到正确的VM节点
UPDATE asset a
INNER JOIN host_nodes hn ON hn.host_ip = '172.16.190.130'
SET a.preferred_host_node_id = hn.id,
    a.preferred_host_node_name = hn.display_name,
    a.deployment_strategy = 'fixed'
WHERE a.preferred_host_node_id IS NULL 
   OR a.preferred_host_node_id IN (
       SELECT id FROM host_nodes WHERE host_ip = '192.168.1.100'
   );

-- 为测试项目资产设置合理的部署策略
UPDATE asset 
SET deployment_strategy = 'any',
    preferred_host_node_id = NULL,
    preferred_host_node_name = NULL
WHERE company = '红岸实验室' 
  AND project = '网络安全演练项目'
  AND deployment_strategy IS NULL;

-- 插入一条日志记录，标记这次配置修复
INSERT INTO host_nodes (
    name, display_name, host_ip, docker_port, node_type, environment, status, description,
    cpu_cores, memory_total, disk_total, monitoring_enabled, max_containers, priority,
    cluster_name, docker_tls_enabled
) VALUES (
    'config-fix-log', '配置修复日志记录', '0.0.0.0', 0, 'local', 'development', 'inactive', 
    CONCAT('主机节点配置修复完成 - ', NOW(), ' - 已将测试节点IP从192.168.1.100更新为172.16.190.130'),
    0, 0, 0, FALSE, 0, 0,
    'maintenance', FALSE
) ON DUPLICATE KEY UPDATE
    description = CONCAT('主机节点配置修复完成 - ', NOW(), ' - 已将测试节点IP从192.168.1.100更新为172.16.190.130'),
    updated_at = CURRENT_TIMESTAMP;