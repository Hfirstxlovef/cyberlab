-- 扩展 request_url 字段长度以支持长URL（含中文参数）
-- 解决问题：Data too long for column 'request_url' at row 1
-- 原因：带中文项目名称的URL经过编码后超过默认VARCHAR(255)限制

ALTER TABLE system_log MODIFY COLUMN request_url VARCHAR(1000);
