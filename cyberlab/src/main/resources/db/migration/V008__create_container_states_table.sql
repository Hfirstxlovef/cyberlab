-- 创建容器状态表
CREATE TABLE container_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT,
    host_node_id BIGINT,
    container_id VARCHAR(255),
    container_name VARCHAR(255),
    image_name VARCHAR(255),
    desired_status VARCHAR(50) NOT NULL COMMENT '目标状态: RUNNING, STOPPED, PAUSED, RESTARTED',
    current_status VARCHAR(50) COMMENT '当前状态: RUNNING, STOPPED, PAUSED, UNKNOWN',
    health_status VARCHAR(50) COMMENT '健康状态: healthy, unhealthy, unknown',
    sync_status VARCHAR(50) NOT NULL DEFAULT 'OUT_OF_SYNC' COMMENT '同步状态: SYNCED, OUT_OF_SYNC, SYNCING, FAILED',
    last_sync_at TIMESTAMP NULL COMMENT '最后同步时间',
    sync_attempts INT DEFAULT 0 COMMENT '同步尝试次数',
    max_sync_attempts INT DEFAULT 3 COMMENT '最大同步尝试次数',
    sync_error TEXT COMMENT '同步错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(100) COMMENT '创建者',
    
    INDEX idx_asset_id (asset_id),
    INDEX idx_host_node_id (host_node_id),
    INDEX idx_container_id (container_id),
    INDEX idx_sync_status (sync_status),
    INDEX idx_desired_status (desired_status),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_asset_container (asset_id, container_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='容器状态管理表';