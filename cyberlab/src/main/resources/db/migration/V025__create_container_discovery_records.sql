-- 容器探测记录表（持久化存储）
-- 用于保存直接IP探测的容器发现结果
-- 实现增量同步：以探测结果为准，自动增删容器记录

CREATE TABLE container_discovery_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_id VARCHAR(255) NOT NULL COMMENT '项目ID（格式：企业｜项目）',
    asset_id BIGINT COMMENT '关联的资产ID',
    asset_name VARCHAR(255) COMMENT '资产名称',
    asset_ip VARCHAR(100) COMMENT '资产IP地址',

    container_id VARCHAR(255) NOT NULL COMMENT 'Docker容器ID（完整ID）',
    container_name VARCHAR(255) COMMENT '容器名称',
    image VARCHAR(500) COMMENT '容器镜像名称',
    status VARCHAR(50) COMMENT '容器状态：running/exited/paused/dead',
    ports TEXT COMMENT '端口映射（JSON格式）',
    labels TEXT COMMENT '容器标签（JSON格式）',

    created_at TIMESTAMP NULL COMMENT '容器创建时间',
    discovered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首次发现时间',
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后探测时间',

    INDEX idx_project_id (project_id),
    INDEX idx_asset_id (asset_id),
    INDEX idx_container_id (container_id),
    INDEX idx_last_seen (last_seen_at DESC),
    INDEX idx_project_container (project_id, container_id),
    UNIQUE KEY uk_project_container (project_id, container_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='容器探测记录表-增量同步';
