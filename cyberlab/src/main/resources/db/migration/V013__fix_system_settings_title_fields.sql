-- 修复系统设置中的标题字段命名和用途
-- 将混乱的字段命名改为更清晰的命名

-- 添加新的字段
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, updated_at) 
SELECT 'website_title', setting_value, setting_type, '网站标题（浏览器标题栏显示）', NOW()
FROM system_settings 
WHERE setting_key = 'login_title'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, updated_at) 
SELECT 'login_page_title', '红岸网络空间安全对抗平台', 'text', '登录页面显示的标题', NOW()
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 更新现有字段的描述，使其含义更清晰
UPDATE system_settings SET description = '侧边栏显示的标题' WHERE setting_key = 'sidebar_title';

-- 保留原有的 login_title 字段但更新其描述，以保持向后兼容性
-- 在代码更新完成后，可以考虑删除这个字段
UPDATE system_settings SET description = '网站标题（已废弃，请使用website_title）' WHERE setting_key = 'login_title';