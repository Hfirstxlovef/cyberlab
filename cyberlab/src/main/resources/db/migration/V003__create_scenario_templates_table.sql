-- 创建场景模板表
-- V003__create_scenario_templates_table.sql

CREATE TABLE scenario_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL COMMENT '场景名称',
    description TEXT COMMENT '场景描述',
    scenario_type VARCHAR(50) NOT NULL COMMENT '场景类型：attack_defense, blue_team, red_team, mixed',
    difficulty_level VARCHAR(50) NOT NULL COMMENT '难度等级：beginner, intermediate, advanced',
    estimated_duration INT COMMENT '预计演练时间（分钟）',
    max_participants INT COMMENT '最大参与人数',
    learning_objectives TEXT COMMENT '学习目标',
    prerequisites TEXT COMMENT '前置要求',
    asset_config TEXT COMMENT '资产配置(JSON格式：资产ID和配置数组)',
    network_topology TEXT COMMENT '网络拓扑配置(JSON格式)',
    deployment_order TEXT COMMENT '部署顺序配置(JSON格式)',
    exercise_script TEXT COMMENT '演练脚本和步骤',
    evaluation_criteria TEXT COMMENT '评估标准',
    success_metrics TEXT COMMENT '成功指标',
    instructor_notes TEXT COMMENT '教师备注',
    student_guidelines TEXT COMMENT '学生指导',
    tags TEXT COMMENT '标签(JSON格式)',
    is_public BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否公开模板',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    usage_count INT NOT NULL DEFAULT 0 COMMENT '使用次数统计',
    created_by BIGINT COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    
    INDEX idx_scenario_type (scenario_type),
    INDEX idx_difficulty_level (difficulty_level),
    INDEX idx_is_public (is_public),
    INDEX idx_is_active (is_active),
    INDEX idx_usage_count (usage_count),
    INDEX idx_created_at (created_at),
    INDEX idx_last_used_at (last_used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景模板表';