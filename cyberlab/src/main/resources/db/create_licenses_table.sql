-- ===================================================================
-- 红岸网络空间安全对抗平台 - 授权码管理表
-- CyberLab License Management Table
-- ===================================================================
-- 蟑螂恶霸团队 (Cockroach Bully Team)
-- 该脚本用于创建授权码管理表，存储所有授权记录
-- ===================================================================

-- 创建授权码表
CREATE TABLE IF NOT EXISTS licenses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    serial_number VARCHAR(50) NOT NULL UNIQUE COMMENT '产品序列号 (CYBERLAB-YYYY-MM-DD-NNNNN)',
    license_code VARCHAR(200) NOT NULL UNIQUE COMMENT '授权码 (CL-TEAM-EDITION-STATUS-HASH)',
    edition VARCHAR(20) NOT NULL COMMENT '版本 (PRO, ENTERPRISE, TRIAL)',
    status VARCHAR(20) NOT NULL COMMENT '状态 (ACTIVE, INACTIVE, EXPIRED)',
    issued_to VARCHAR(100) COMMENT '授权给 (客户名称)',
    issued_date DATE NOT NULL COMMENT '颁发日期',
    expiry_date DATE NOT NULL COMMENT '过期日期',
    is_current BOOLEAN DEFAULT FALSE COMMENT '是否为当前激活授权',
    created_by VARCHAR(50) COMMENT '创建人用户名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    notes TEXT COMMENT '备注信息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授权码管理表';

-- 创建索引以优化查询性能
CREATE INDEX idx_serial_number ON licenses(serial_number);
CREATE INDEX idx_license_code ON licenses(license_code);
CREATE INDEX idx_status ON licenses(status);
CREATE INDEX idx_expiry_date ON licenses(expiry_date);
CREATE INDEX idx_is_current ON licenses(is_current);
CREATE INDEX idx_created_at ON licenses(created_at DESC);

-- ===================================================================
-- 验证表结构
-- ===================================================================
DESCRIBE licenses;

-- ===================================================================
-- 使用说明
-- ===================================================================
-- 1. 执行此脚本创建 licenses 表
-- 2. hongan 用户通过管理界面生成授权码，存储到此表
-- 3. 设置"当前激活授权"时，is_current 字段设为 true，并同步到 system_settings 表
-- 4. 所有用户登录后，从 system_settings 表读取当前激活授权进行验证
-- 5. 授权码格式：CL-ZL3B4T34M-PRO2025-ACTIVE-HASH
-- 6. 序列号格式：CYBERLAB-2025-12-31-00001
--
-- 联系方式：
--    Email: sun740883686@foxmail.com
--    团队：蟑螂恶霸团队
-- ===================================================================
