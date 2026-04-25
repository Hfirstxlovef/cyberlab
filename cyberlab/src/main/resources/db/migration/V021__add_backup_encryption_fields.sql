-- 添加备份加密相关字段
-- Author: Claude
-- Date: 2025-10-23
-- Purpose: 为备份系统添加加密功能，保护敏感配置数据

ALTER TABLE system_backup
    ADD COLUMN encrypted BOOLEAN DEFAULT FALSE COMMENT '是否加密（密码保护）' AFTER md5checksum,
    ADD COLUMN encryption_method VARCHAR(20) DEFAULT 'NONE' COMMENT '加密方法：NONE/AES256' AFTER encrypted;

-- 更新现有备份记录为未加密
UPDATE system_backup SET encrypted = FALSE, encryption_method = 'NONE' WHERE encrypted IS NULL;
