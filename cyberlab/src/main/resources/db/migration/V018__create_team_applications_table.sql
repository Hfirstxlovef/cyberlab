-- 创建战队申请表
CREATE TABLE team_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL COMMENT '申请加入的战队ID',
    user_id BIGINT NOT NULL COMMENT '申请用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '申请状态: pending, approved, rejected',
    message TEXT COMMENT '申请留言',
    reject_reason TEXT COMMENT '拒绝理由',

    -- 审批信息
    reviewed_by BIGINT NULL COMMENT '审批人用户ID',
    reviewed_at TIMESTAMP NULL COMMENT '审批时间',

    -- 时间字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    CONSTRAINT fk_applications_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,

    -- 防止重复申请：同一用户对同一战队只能有一个待处理申请
    UNIQUE KEY uk_applications_pending (team_id, user_id, status),

    INDEX idx_applications_team (team_id),
    INDEX idx_applications_user (user_id),
    INDEX idx_applications_status (status),
    INDEX idx_applications_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='战队申请表';
