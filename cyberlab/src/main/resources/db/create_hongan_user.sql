-- ===================================================================
-- 红岸网络空间安全对抗平台 - 创建 hongan 授权管理员用户
-- CyberLab Create License Admin User
-- ===================================================================
-- 蟑螂恶霸团队 (Cockroach Bully Team)
-- 该脚本用于创建 hongan 用户，专门负责授权码的生成和管理
-- ===================================================================

-- 创建 hongan 用户（密码：hongan123）
-- 密码已使用 BCrypt 加密（强度 10）
INSERT INTO users (username, password, role, enabled)
VALUES (
    'hongan',
    '$2a$10$aOcRAW0.9hiZdL2pj6jlN.KXad5qKrHmOXHbrSPw8CXddEHP4FJNi',  -- hongan123 的 BCrypt 加密
    'license_admin',
    true
)
ON DUPLICATE KEY UPDATE
    password = '$2a$10$aOcRAW0.9hiZdL2pj6jlN.KXad5qKrHmOXHbrSPw8CXddEHP4FJNi',
    role = 'license_admin',
    enabled = true;

-- ===================================================================
-- 验证用户创建
-- ===================================================================
SELECT id, username, role, enabled
FROM users
WHERE username = 'hongan';

-- ===================================================================
-- 使用说明
-- ===================================================================
-- 1. hongan 用户专门负责授权码的生成和管理
-- 2. 登录信息：
--    用户名：hongan
--    密码：hongan123
-- 3. 权限范围：
--    - ✅ 生成新授权码
--    - ✅ 激活/停用授权
--    - ✅ 设置当前激活授权
--    - ✅ 延长授权有效期
--    - ✅ 查看授权历史
--    - ❌ 无法访问系统其他功能
-- 4. 角色：license_admin（授权管理员）
--
-- 注意：
--    - 请妥善保管 hongan 用户密码
--    - 如需修改密码，请使用系统的密码修改功能
--    - 该用户专门用于授权管理，请勿用于其他用途
--
-- 联系方式：
--    Email: sun740883686@foxmail.com
--    团队：蟑螂恶霸团队
-- ===================================================================
