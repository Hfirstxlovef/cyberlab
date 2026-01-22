-- ===================================================================
-- 红岸网络空间安全对抗平台 - 授权系统初始化脚本
-- CyberLab License Management System Initialization
-- ===================================================================
-- 蟑螂恶霸团队 (Cockroach Bully Team)
-- 该脚本用于初始化产品授权信息，包含有效期管理
-- ===================================================================

-- 清理旧的授权数据（如果存在）
DELETE FROM system_settings WHERE setting_key IN ('serial_number', 'license_code', 'license_owner', 'license_issued_date');

-- 插入产品序列号（格式：CYBERLAB-YYYY-MM-DD-NNNNN）
-- 当前示例：有效期至 2025-12-31
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, created_at, updated_at)
VALUES
(
    'serial_number',
    'CYBERLAB-2025-12-31-00001',
    'code',
    '产品序列号（有效期至2025年12月31日）',
    NOW(),
    NOW()
);

-- 插入授权码（蟑螂恶霸团队专用格式）
-- 格式：CL-TEAMNAME-EDITION-STATUS-HASH
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, created_at, updated_at)
VALUES
(
    'license_code',
    'CL-ZL3B4T34M-PRO2025-ACTIVE-ABCD1234',
    'code',
    '蟑螂恶霸团队授权码（专业版）',
    NOW(),
    NOW()
);

-- 插入授权持有者信息
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, created_at, updated_at)
VALUES
(
    'license_owner',
    '红岸网络空间安全对抗平台',
    'text',
    '授权持有者名称',
    NOW(),
    NOW()
);

-- 插入授权颁发日期
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, created_at, updated_at)
VALUES
(
    'license_issued_date',
    '2024-01-01',
    'date',
    '授权颁发日期',
    NOW(),
    NOW()
);

-- ===================================================================
-- 验证插入的数据
-- ===================================================================
SELECT
    setting_key AS '设置键',
    setting_value AS '设置值',
    setting_type AS '类型',
    description AS '描述',
    created_at AS '创建时间'
FROM system_settings
WHERE setting_key IN ('serial_number', 'license_code', 'license_owner', 'license_issued_date')
ORDER BY setting_key;

-- ===================================================================
-- 使用说明
-- ===================================================================
-- 1. 序列号格式说明：CYBERLAB-YYYY-MM-DD-NNNNN
--    - CYBERLAB: 产品前缀
--    - YYYY-MM-DD: 授权过期日期（年-月-日）
--    - NNNNN: 唯一标识符（5位数字）
--
-- 2. 授权码格式说明：CL-ZL3B4T34M-PRO2025-ACTIVE-ABCD1234
--    - CL: CyberLab 缩写
--    - ZL3B4T34M: 蟑螂恶霸团队拼音缩写 (Zhanglang Eba Team)
--    - PRO2025: 版本（专业版2025）
--    - ACTIVE: 状态（激活/停用）
--    - ABCD1234: 校验哈希值
--
-- 3. 修改授权有效期：
--    UPDATE system_settings
--    SET setting_value = 'CYBERLAB-2026-12-31-00001',
--        description = '产品序列号（有效期至2026年12月31日）',
--        updated_at = NOW()
--    WHERE setting_key = 'serial_number';
--
-- 4. 联系方式：
--    授权续期、技术支持请联系：蟑螂恶霸团队
--    Email: sun740883686@foxmail.com
-- ===================================================================
