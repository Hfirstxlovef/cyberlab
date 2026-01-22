-- V011 - 修复重复节点问题并添加唯一性约束
-- 解决测试节点01重复显示的问题

-- 1. 首先添加唯一性约束到host_nodes表
-- 在添加约束前先清理重复数据

-- 删除可能的重复配置记录（保留一个最佳的记录）
-- 使用窗口函数来识别重复记录，保留质量最好的记录
CREATE TEMPORARY TABLE temp_duplicates AS
SELECT 
    id,
    host_ip,
    name,
    ROW_NUMBER() OVER (
        PARTITION BY host_ip, name 
        ORDER BY 
            CASE WHEN description LIKE '%苏州科技大学%' THEN 1 ELSE 2 END,
            CASE WHEN status = 'active' THEN 1 ELSE 2 END,
            created_at ASC
    ) as rn
FROM host_nodes 
WHERE host_ip = '172.16.190.130' OR name LIKE '%测试节点01%';

-- 获取要删除的重复记录ID
SET @duplicate_ids = (
    SELECT GROUP_CONCAT(id SEPARATOR ',') 
    FROM temp_duplicates 
    WHERE rn > 1
);

-- 在删除前，将引用重复节点的资产重新关联到保留的节点
SET @keep_node_id = (
    SELECT id 
    FROM temp_duplicates 
    WHERE rn = 1 AND host_ip = '172.16.190.130'
    LIMIT 1
);

-- 更新资产关联
UPDATE asset 
SET preferred_host_node_id = @keep_node_id,
    preferred_host_node_name = '测试节点01 - Ubuntu VM',
    updated_at = NOW()
WHERE preferred_host_node_id IN (
    SELECT id FROM temp_duplicates WHERE rn > 1
);

-- 更新容器关联
UPDATE drill_containers 
SET host_node_id = @keep_node_id,
    host_node_name = '测试节点01 - Ubuntu VM',
    host_node_ip = '172.16.190.130',
    updated_at = NOW()
WHERE host_node_id IN (
    SELECT id FROM temp_duplicates WHERE rn > 1
);

-- 删除重复的节点
DELETE FROM host_nodes 
WHERE id IN (
    SELECT id FROM temp_duplicates WHERE rn > 1
);

-- 清理临时表
DROP TEMPORARY TABLE temp_duplicates;

-- 删除配置验证记录和其他无用记录
DELETE FROM host_nodes 
WHERE name = 'config-verification' 
   OR (host_ip = '0.0.0.1' AND node_type = 'system')
   OR host_ip = '192.168.1.100';

-- 2. 确保保留的VM节点配置正确
UPDATE host_nodes 
SET name = '测试节点01',
    display_name = '测试节点01 - Ubuntu VM',
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
    updated_at = NOW()
WHERE host_ip = '172.16.190.130' AND name = '测试节点01';

-- 3. 如果没有VM节点存在，创建一个标准的节点
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
    '测试节点01',
    '测试节点01 - Ubuntu VM',
    '172.16.190.130',
    2375, 22, 'cyberlab-test-cluster',
    'vm', 'testing', 'active',
    '苏州科技大学测试环境Ubuntu虚拟机，Docker API端口2375，用于容器管理和安全演练',
    4, 4096, 50,
    FALSE, NULL,
    TRUE, 30, 5,
    '{"environment": "testing", "location": "suzhou", "purpose": "container-discovery"}',
    '{"university": "苏州科技大学", "lab": "红岸网络空间安全仿真攻防实验室"}',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM host_nodes 
    WHERE host_ip = '172.16.190.130' AND name = '测试节点01'
);

-- 4. 添加唯一性约束以防止将来重复
-- 首先检查是否已存在该约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'host_nodes' 
      AND CONSTRAINT_NAME = 'uk_host_nodes_name_ip'
);

-- 只有当约束不存在时才添加
SET @sql = IF(@constraint_exists = 0, 
    'ALTER TABLE host_nodes ADD CONSTRAINT uk_host_nodes_name_ip UNIQUE (name, host_ip)',
    'SELECT "Unique constraint already exists" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 更新相关资产的关联
SET @vm_node_id = (SELECT id FROM host_nodes WHERE host_ip = '172.16.190.130' AND name = '测试节点01' LIMIT 1);

UPDATE asset 
SET preferred_host_node_id = @vm_node_id,
    preferred_host_node_name = '测试节点01 - Ubuntu VM',
    ip = '172.16.190.130',
    deployment_strategy = CASE 
        WHEN name LIKE '%Web服务器%' OR name LIKE '%数据库服务器%' THEN 'fixed'
        ELSE 'any'
    END,
    updated_at = NOW()
WHERE (company = '红岸实验室' OR company LIKE '%苏州科技大学%')
  AND (preferred_host_node_id IS NULL OR preferred_host_node_id != @vm_node_id);

-- 6. 记录修复操作
INSERT INTO system_log (level, source, action, message, details, created_at) VALUES 
('INFO', 'DATABASE_MIGRATION', 'V011_DUPLICATE_FIX', '重复节点问题修复完成', 
CONCAT('VM节点ID: ', @vm_node_id, 
       ', 当前172.16.190.130节点数量: ', (SELECT COUNT(*) FROM host_nodes WHERE host_ip = '172.16.190.130'),
       ', 关联资产数量: ', (SELECT COUNT(*) FROM asset WHERE preferred_host_node_id = @vm_node_id)), 
NOW())
ON DUPLICATE KEY UPDATE id=id;

-- 7. 验证修复结果
SELECT 
    '修复验证' as section,
    'VM节点数量' as metric, 
    COUNT(*) as value,
    '期望值: 1' as expected
FROM host_nodes WHERE host_ip = '172.16.190.130'
UNION ALL
SELECT 
    '修复验证' as section,
    '测试节点01数量' as metric, 
    COUNT(*) as value,
    '期望值: 1' as expected
FROM host_nodes WHERE name = '测试节点01'
UNION ALL
SELECT 
    '修复验证' as section,
    '关联资产数量' as metric, 
    COUNT(*) as value,
    '期望值: >0' as expected
FROM asset WHERE preferred_host_node_id = @vm_node_id;