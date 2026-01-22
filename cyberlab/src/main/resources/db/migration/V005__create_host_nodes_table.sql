-- 创建主机节点管理表
CREATE TABLE host_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '节点名称',
    display_name VARCHAR(150) NOT NULL COMMENT '显示名称',
    host_ip VARCHAR(45) NOT NULL COMMENT '主机IP地址',
    docker_port INT DEFAULT 2376 COMMENT 'Docker API端口',
    ssh_port INT DEFAULT 22 COMMENT 'SSH端口',
    cluster_name VARCHAR(100) COMMENT '所属集群名称',
    node_type VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT '节点类型: local, remote, vm, physical',
    environment VARCHAR(20) NOT NULL DEFAULT 'development' COMMENT '环境类型: development, testing, staging, production',
    status VARCHAR(20) NOT NULL DEFAULT 'inactive' COMMENT '节点状态: active, inactive, maintenance, error',
    description TEXT COMMENT '节点描述信息',
    
    -- 资源信息
    cpu_cores INT COMMENT 'CPU核心数',
    memory_total BIGINT COMMENT '总内存(MB)',
    disk_total BIGINT COMMENT '总磁盘空间(GB)',
    
    -- 连接配置
    docker_tls_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用Docker TLS',
    docker_cert_path VARCHAR(255) COMMENT 'Docker证书路径',
    ssh_username VARCHAR(50) COMMENT 'SSH用户名',
    ssh_key_path VARCHAR(255) COMMENT 'SSH密钥路径',
    
    -- 监控配置
    monitoring_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用监控',
    max_containers INT DEFAULT 50 COMMENT '最大容器数量',
    priority INT DEFAULT 1 COMMENT '节点优先级，用于负载均衡',
    
    -- 标签和元数据
    labels TEXT COMMENT 'JSON格式存储节点标签',
    metadata TEXT COMMENT 'JSON格式存储其他元数据',
    
    -- 时间字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_health_check TIMESTAMP NULL COMMENT '最后健康检查时间',
    
    INDEX idx_host_nodes_name (name),
    INDEX idx_host_nodes_status (status),
    INDEX idx_host_nodes_environment (environment),
    INDEX idx_host_nodes_cluster_name (cluster_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主机节点管理表';

-- 插入默认的本地节点
INSERT INTO host_nodes (
    name, display_name, host_ip, node_type, environment, status, description,
    cpu_cores, memory_total, disk_total, monitoring_enabled, max_containers, priority
) VALUES (
    'localhost', '本地主机', '127.0.0.1', 'local', 'development', 'active', '默认本地Docker主机',
    4, 8192, 100, TRUE, 50, 1
);