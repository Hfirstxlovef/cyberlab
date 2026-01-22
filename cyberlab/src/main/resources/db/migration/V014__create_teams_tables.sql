-- 创建战队管理表
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '战队名称',
    leader_id BIGINT NOT NULL COMMENT '队长用户ID',
    description TEXT COMMENT '战队描述',
    team_type VARCHAR(20) NOT NULL COMMENT '战队类型: red, blue',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '战队状态: active, disbanded',

    -- 时间字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    CONSTRAINT fk_teams_leader FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE RESTRICT,

    INDEX idx_teams_name (name),
    INDEX idx_teams_type (team_type),
    INDEX idx_teams_status (status),
    INDEX idx_teams_leader (leader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='战队管理表';

-- 创建战队成员表
CREATE TABLE team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL COMMENT '战队ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    member_role VARCHAR(50) DEFAULT 'member' COMMENT '成员角色: leader, member',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',

    -- 外键约束
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 唯一约束：一个用户只能加入一个战队
    UNIQUE KEY uk_team_members_user (user_id),

    INDEX idx_team_members_team (team_id),
    INDEX idx_team_members_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='战队成员表';

-- 为users表添加team_id字段（便捷查询用户所属战队）
ALTER TABLE users
    ADD COLUMN team_id BIGINT NULL COMMENT '所属战队ID',
    ADD CONSTRAINT fk_users_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    ADD INDEX idx_users_team (team_id);
