-- V007: 为Asset表添加容器相关字段
-- 支持Docker容器资产管理和容器发现功能

ALTER TABLE asset 
ADD COLUMN asset_type VARCHAR(50) DEFAULT 'server' COMMENT '资产类型：server(服务器), container(容器), service(服务), network(网络设备)';

ALTER TABLE asset 
ADD COLUMN docker_image VARCHAR(255) COMMENT 'Docker镜像名称，如 nginx:alpine';

ALTER TABLE asset 
ADD COLUMN container_ports TEXT COMMENT '容器端口配置，JSON格式，如 {"80/tcp": "8080", "443/tcp": "8443"}';

ALTER TABLE asset 
ADD COLUMN volumes TEXT COMMENT '数据卷配置，JSON格式，如 ["/app/data:/data", "/app/logs:/logs"]';

ALTER TABLE asset 
ADD COLUMN environments TEXT COMMENT '环境变量，JSON格式，如 {"ENV": "production", "DEBUG": "false"}';

ALTER TABLE asset 
ADD COLUMN container_command VARCHAR(500) COMMENT '容器启动命令，如 "nginx -g daemon off;"';

ALTER TABLE asset 
ADD COLUMN health_check_url VARCHAR(255) COMMENT '健康检查URL，如 "http://localhost:8080/health"';

ALTER TABLE asset 
ADD COLUMN resource_limit_cpu INT COMMENT 'CPU限制，单位为millicores';

ALTER TABLE asset 
ADD COLUMN resource_limit_memory BIGINT COMMENT '内存限制，单位为MB';

-- 更新现有记录，将没有指定类型的资产默认设置为服务器类型
UPDATE asset SET asset_type = 'server' WHERE asset_type IS NULL;

-- 为名为 "docker容器测试" 的资产设置容器配置（如果存在）
UPDATE asset 
SET 
    asset_type = 'container',
    docker_image = 'nginx:alpine',
    container_ports = '{"80/tcp": "8080"}',
    health_check_url = 'http://localhost:8080'
WHERE name = 'docker容器测试';

-- 添加索引以提高查询性能
CREATE INDEX idx_asset_type ON asset(asset_type);
CREATE INDEX idx_docker_image ON asset(docker_image);