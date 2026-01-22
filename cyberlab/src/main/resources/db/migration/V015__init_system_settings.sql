-- 系统设置表初始化数据
-- 如果不存在这些设置，则插入默认值

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, updated_at) 
VALUES 
    ('system_logo', '/images/default-logo.png', 'image', '系统Logo图片', NOW()),
    ('login_title', '欢迎使用CyberLab网络空间安全攻防演练平台', 'text', '登录页面标题', NOW()),
    ('sidebar_title', 'CyberLab平台', 'text', '侧边栏标题', NOW()),
    ('serial_number', 'CYBERLAB-2024-001', 'code', '产品序列号', NOW()),
    ('license_code', '未设置授权码', 'code', '产品授权码', NOW())
ON DUPLICATE KEY UPDATE 
    updated_at = NOW();