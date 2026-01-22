-- 数据库迁移脚本：为system_log表添加操作行为分类字段
-- 目的：支持多维度操作分类（操作类型+业务模块+对象类型+操作状态）
-- 作者：CyberLab Team
-- 日期：2025-01-22

-- ========== 添加操作分类字段 ==========

-- 操作类型（CRUD标准操作）
ALTER TABLE system_log ADD COLUMN operation_type VARCHAR(20) COMMENT '操作类型：CREATE/READ/UPDATE/DELETE/EXECUTE/BATCH/EXPORT/IMPORT/AUDIT/APPROVE/REJECT/CONFIGURE/LOGIN/LOGOUT';

-- 业务模块（业务领域分类）
ALTER TABLE system_log ADD COLUMN business_module VARCHAR(50) COMMENT '业务模块：CONTAINER/USER/ACHIEVEMENT/DRILL/SCENARIO/PERMISSION/ROLE/TEAM/HOST_NODE/ASSET/SYSTEM_CONFIG/LOG/AUTH/MONITOR/BIG_SCREEN/FILE/RECORDING/TOPOLOGY/BACKUP/NOTIFICATION/OTHER';

-- 操作对象类型（实体类型）
ALTER TABLE system_log ADD COLUMN object_type VARCHAR(50) COMMENT '操作对象类型：Container/User/Achievement/Role/DrillScenario等';

-- 操作状态（执行结果）
ALTER TABLE system_log ADD COLUMN operation_status VARCHAR(20) COMMENT '操作状态：SUCCESS/FAILED/PARTIAL/PENDING/TIMEOUT/CANCELLED/SKIPPED';

-- ========== 创建索引（优化查询性能） ==========

-- 单字段索引
CREATE INDEX idx_operation_type ON system_log(operation_type);
CREATE INDEX idx_business_module ON system_log(business_module);
CREATE INDEX idx_object_type ON system_log(object_type);
CREATE INDEX idx_operation_status ON system_log(operation_status);

-- 组合索引（高频查询优化）
CREATE INDEX idx_module_type ON system_log(business_module, operation_type);
CREATE INDEX idx_module_status ON system_log(business_module, operation_status);
CREATE INDEX idx_type_status ON system_log(operation_type, operation_status);
CREATE INDEX idx_module_type_status ON system_log(business_module, operation_type, operation_status);

-- 时间范围查询优化（已有timestamp索引，增加组合索引）
CREATE INDEX idx_module_timestamp ON system_log(business_module, timestamp);
CREATE INDEX idx_status_timestamp ON system_log(operation_status, timestamp);

-- ========== 历史数据自动分类（智能推断） ==========

-- 1. 根据operation字段内容推断operation_type
UPDATE system_log
SET operation_type = CASE
    WHEN operation LIKE '%创建%' OR operation LIKE '%新增%' OR operation LIKE '%添加%' THEN 'CREATE'
    WHEN operation LIKE '%查询%' OR operation LIKE '%查看%' OR operation LIKE '%导出%' THEN 'READ'
    WHEN operation LIKE '%修改%' OR operation LIKE '%更新%' OR operation LIKE '%编辑%' THEN 'UPDATE'
    WHEN operation LIKE '%删除%' OR operation LIKE '%移除%' THEN 'DELETE'
    WHEN operation LIKE '%启动%' OR operation LIKE '%停止%' OR operation LIKE '%重启%' OR operation LIKE '%执行%' THEN 'EXECUTE'
    WHEN operation LIKE '%批量%' THEN 'BATCH'
    WHEN operation LIKE '%审核通过%' OR operation LIKE '%批准%' THEN 'APPROVE'
    WHEN operation LIKE '%驳回%' OR operation LIKE '%拒绝%' THEN 'REJECT'
    WHEN operation LIKE '%登录%' THEN 'LOGIN'
    WHEN operation LIKE '%登出%' OR operation LIKE '%退出%' THEN 'LOGOUT'
    WHEN operation LIKE '%配置%' OR operation LIKE '%设置%' THEN 'CONFIGURE'
    WHEN operation LIKE '%导入%' THEN 'IMPORT'
    ELSE 'EXECUTE'
END
WHERE operation_type IS NULL;

-- 2. 根据operation字段内容推断business_module
UPDATE system_log
SET business_module = CASE
    WHEN operation LIKE '%容器%' OR description LIKE '%容器%' THEN 'CONTAINER'
    WHEN operation LIKE '%用户%' OR description LIKE '%用户%' THEN 'USER'
    WHEN operation LIKE '%成果%' OR description LIKE '%成果%' THEN 'ACHIEVEMENT'
    WHEN operation LIKE '%演练%' OR description LIKE '%演练%' THEN 'DRILL'
    WHEN operation LIKE '%场景%' OR description LIKE '%场景%' THEN 'SCENARIO'
    WHEN operation LIKE '%权限%' OR description LIKE '%授权%' THEN 'PERMISSION'
    WHEN operation LIKE '%角色%' OR description LIKE '%角色%' THEN 'ROLE'
    WHEN operation LIKE '%队伍%' OR operation LIKE '%团队%' THEN 'TEAM'
    WHEN operation LIKE '%主机%' OR operation LIKE '%节点%' THEN 'HOST_NODE'
    WHEN operation LIKE '%资产%' OR description LIKE '%资产%' THEN 'ASSET'
    WHEN operation LIKE '%配置%' OR operation LIKE '%设置%' THEN 'SYSTEM_CONFIG'
    WHEN operation LIKE '%日志%' OR description LIKE '%日志%' THEN 'LOG'
    WHEN operation LIKE '%登录%' OR operation LIKE '%认证%' THEN 'AUTH'
    WHEN operation LIKE '%监控%' OR operation LIKE '%告警%' THEN 'MONITOR'
    WHEN operation LIKE '%大屏%' OR description LIKE '%大屏%' THEN 'BIG_SCREEN'
    WHEN operation LIKE '%文件%' OR operation LIKE '%上传%' OR operation LIKE '%下载%' THEN 'FILE'
    WHEN operation LIKE '%录屏%' OR operation LIKE '%录像%' THEN 'RECORDING'
    WHEN operation LIKE '%拓扑%' OR description LIKE '%拓扑%' THEN 'TOPOLOGY'
    WHEN operation LIKE '%备份%' OR operation LIKE '%恢复%' THEN 'BACKUP'
    WHEN operation LIKE '%通知%' OR operation LIKE '%消息%' THEN 'NOTIFICATION'
    ELSE 'OTHER'
END
WHERE business_module IS NULL;

-- 3. 根据exception_type字段推断operation_status
UPDATE system_log
SET operation_status = CASE
    WHEN exception_type IS NOT NULL AND exception_type != '' THEN 'FAILED'
    WHEN log_level = 'ERROR' THEN 'FAILED'
    WHEN log_level = 'WARN' AND operation LIKE '%超时%' THEN 'TIMEOUT'
    WHEN operation LIKE '%部分成功%' OR description LIKE '%部分成功%' THEN 'PARTIAL'
    WHEN operation LIKE '%取消%' OR description LIKE '%取消%' THEN 'CANCELLED'
    WHEN operation LIKE '%跳过%' OR description LIKE '%跳过%' THEN 'SKIPPED'
    ELSE 'SUCCESS'
END
WHERE operation_status IS NULL;

-- 4. 尝试从description字段提取object_type
UPDATE system_log
SET object_type = CASE
    WHEN description LIKE '%Container%' THEN 'Container'
    WHEN description LIKE '%User%' THEN 'User'
    WHEN description LIKE '%Achievement%' THEN 'Achievement'
    WHEN description LIKE '%DrillScenario%' THEN 'DrillScenario'
    WHEN description LIKE '%Role%' THEN 'Role'
    WHEN description LIKE '%HostNode%' THEN 'HostNode'
    WHEN description LIKE '%Asset%' THEN 'Asset'
    WHEN description LIKE '%Team%' THEN 'Team'
    ELSE NULL
END
WHERE object_type IS NULL;

-- ========== 数据统计（验证迁移结果） ==========

-- 输出迁移后的统计信息（可选，注释掉以避免执行时输出）
-- SELECT
--     business_module AS '业务模块',
--     COUNT(*) AS '日志数量',
--     ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM system_log), 2) AS '占比%'
-- FROM system_log
-- WHERE business_module IS NOT NULL
-- GROUP BY business_module
-- ORDER BY COUNT(*) DESC;

-- SELECT
--     operation_type AS '操作类型',
--     COUNT(*) AS '日志数量'
-- FROM system_log
-- WHERE operation_type IS NOT NULL
-- GROUP BY operation_type
-- ORDER BY COUNT(*) DESC;

-- SELECT
--     operation_status AS '操作状态',
--     COUNT(*) AS '日志数量'
-- FROM system_log
-- WHERE operation_status IS NOT NULL
-- GROUP BY operation_status;

-- ========== 注释说明 ==========

-- 字段使用说明：
-- 1. operation_type：标准CRUD操作类型，用于统计分析（如统计所有DELETE操作）
-- 2. business_module：业务模块分类，用于模块级监控（如容器模块健康度）
-- 3. object_type：操作的实体类型，用于精确追踪（如某个Container的所有操作历史）
-- 4. operation_status：操作执行结果，用于计算成功率和失败告警
--
-- 索引策略：
-- - 单字段索引：支持单维度查询（如所有DELETE操作）
-- - 组合索引：优化高频组合查询（如容器模块的删除操作）
-- - 时间组合索引：支持时间范围+分类的联合查询
--
-- 历史数据处理：
-- - 使用LIKE模糊匹配自动推断分类
-- - 推断规则可根据实际operation内容调整
-- - 无法推断的记录会被标记为OTHER或NULL
