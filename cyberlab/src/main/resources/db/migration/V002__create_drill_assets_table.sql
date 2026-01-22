-- 创建攻防演练资产表
-- V002__create_drill_assets_table.sql

CREATE TABLE drill_assets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL COMMENT '资产名称',
    category VARCHAR(100) NOT NULL COMMENT '资产分类：web_app, server, database, network_device, vulnerability_tool',
    description TEXT COMMENT '资产描述',
    docker_image VARCHAR(500) NOT NULL COMMENT 'Docker镜像名称',
    default_port INT COMMENT '默认端口',
    exposed_ports TEXT COMMENT '暴露端口配置(JSON格式)',
    environment_vars TEXT COMMENT '环境变量配置(JSON格式)',
    volume_mounts TEXT COMMENT '卷挂载配置(JSON格式)',
    network_config TEXT COMMENT '网络配置(JSON格式)',
    security_config TEXT COMMENT '安全配置(JSON格式)',
    resource_limits TEXT COMMENT '资源限制配置(JSON格式)',
    vulnerability_type VARCHAR(100) COMMENT '漏洞类型：sql_injection, xss, command_injection, privilege_escalation',
    difficulty_level VARCHAR(50) NOT NULL COMMENT '难度等级：beginner, intermediate, advanced',
    attack_vector VARCHAR(50) COMMENT '攻击向量：network, local, adjacent_network, physical',
    team_visibility VARCHAR(20) NOT NULL DEFAULT 'both' COMMENT '团队可见性：red, blue, both',
    is_target BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为攻击目标',
    setup_instructions TEXT COMMENT '部署说明',
    exercise_instructions TEXT COMMENT '演练指导',
    solution_hints TEXT COMMENT '解决方案提示',
    tags TEXT COMMENT '标签(JSON格式)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_category (category),
    INDEX idx_difficulty_level (difficulty_level),
    INDEX idx_vulnerability_type (vulnerability_type),
    INDEX idx_team_visibility (team_visibility),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻防演练资产表';