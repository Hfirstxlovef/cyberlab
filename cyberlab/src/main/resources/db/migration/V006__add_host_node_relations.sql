-- 为 drill_containers 表添加主机节点相关字段
ALTER TABLE drill_containers 
ADD COLUMN host_node_id BIGINT COMMENT '关联的主机节点ID',
ADD COLUMN host_node_name VARCHAR(100) COMMENT '主机节点名称（冗余字段）',
ADD COLUMN host_node_ip VARCHAR(45) COMMENT '主机节点IP（冗余字段）',
ADD COLUMN container_full_name VARCHAR(255) COMMENT '完整容器名称';

-- 添加外键约束
ALTER TABLE drill_containers 
ADD CONSTRAINT fk_drill_containers_host_node 
FOREIGN KEY (host_node_id) REFERENCES host_nodes(id) ON DELETE SET NULL;

-- 添加索引
ALTER TABLE drill_containers 
ADD INDEX idx_drill_containers_host_node_id (host_node_id),
ADD INDEX idx_drill_containers_container_full_name (container_full_name);

-- 为 asset 表添加主机节点关联字段
ALTER TABLE asset 
ADD COLUMN preferred_host_node_id BIGINT COMMENT '首选部署节点ID',
ADD COLUMN preferred_host_node_name VARCHAR(100) COMMENT '首选部署节点名称（冗余字段）',
ADD COLUMN deployment_strategy VARCHAR(20) DEFAULT 'any' COMMENT '部署策略：fixed(固定节点), any(任意节点), load_balanced(负载均衡)';

-- 添加外键约束
ALTER TABLE asset 
ADD CONSTRAINT fk_asset_preferred_host_node 
FOREIGN KEY (preferred_host_node_id) REFERENCES host_nodes(id) ON DELETE SET NULL;

-- 添加索引
ALTER TABLE asset 
ADD INDEX idx_asset_preferred_host_node_id (preferred_host_node_id),
ADD INDEX idx_asset_deployment_strategy (deployment_strategy);