-- ========================================
-- 数据库索引优化脚本
-- 用途: 解决资产查询慢查询问题
-- 创建时间: 2025-10-21
-- ========================================

-- 执行方式:
-- mysql -u root -p cyberlab < database_indexes.sql

USE cyberlab;

-- 1. 为 company 字段添加索引 (用于按公司筛选)
CREATE INDEX IF NOT EXISTS idx_assets_company ON assets(company);

-- 2. 为 project 字段添加索引 (用于按项目筛选)
CREATE INDEX IF NOT EXISTS idx_assets_project ON assets(project);

-- 3. 为 company + project 组合添加复合索引 (最重要 - 用于组合筛选)
CREATE INDEX IF NOT EXISTS idx_assets_company_project ON assets(company, project);

-- 4. 为 asset_type 添加索引 (用于按资产类型筛选)
CREATE INDEX IF NOT EXISTS idx_assets_asset_type ON assets(asset_type);

-- 5. 为 enabled 添加索引 (用于筛选启用/禁用资产)
CREATE INDEX IF NOT EXISTS idx_assets_enabled ON assets(enabled);

-- 6. 为 visibility 添加索引 (用于筛选可见性)
CREATE INDEX IF NOT EXISTS idx_assets_visibility ON assets(visibility);

-- 7. 为 is_target 添加索引 (用于筛选目标资产)
CREATE INDEX IF NOT EXISTS idx_assets_is_target ON assets(is_target);

-- 8. 为 topology_project_id 添加索引 (用于拓扑关联查询)
CREATE INDEX IF NOT EXISTS idx_assets_topology_project_id ON assets(topology_project_id);

-- ========================================
-- 验证索引是否创建成功
-- ========================================
SHOW INDEX FROM assets;

-- ========================================
-- 分析查询性能 (执行EXPLAIN查看索引使用情况)
-- ========================================
-- 示例:
-- EXPLAIN SELECT * FROM assets WHERE company='苏州科技大学' AND project='网络安全演练项目';
-- 应该看到 type=ref, key=idx_assets_company_project
