-- 数据库迁移脚本：为system_log表添加日志增强字段
-- 目的：支持结构化日志、审计追踪、性能监控、前端异常收集
-- 作者：CyberLab Team
-- 日期：2025-01-22

-- ========== 日志分类与级别 ==========

ALTER TABLE system_log ADD COLUMN log_level VARCHAR(10) COMMENT '日志级别：INFO/WARN/ERROR/DEBUG';
ALTER TABLE system_log ADD COLUMN log_category VARCHAR(20) COMMENT '日志分类：SYSTEM/BUSINESS/SECURITY/PERFORMANCE/AUDIT/FRONTEND';

-- ========== 请求追踪与会话管理 ==========

ALTER TABLE system_log ADD COLUMN trace_id VARCHAR(64) COMMENT '请求追踪ID（分布式追踪）';
ALTER TABLE system_log ADD COLUMN session_id VARCHAR(64) COMMENT '用户会话ID';
ALTER TABLE system_log ADD COLUMN user_role VARCHAR(20) COMMENT '用户角色：admin/red/blue/judge';

-- ========== HTTP请求上下文 ==========

ALTER TABLE system_log ADD COLUMN request_url VARCHAR(500) COMMENT '请求URL';
ALTER TABLE system_log ADD COLUMN request_method VARCHAR(10) COMMENT 'HTTP方法：GET/POST/PUT/DELETE';
ALTER TABLE system_log ADD COLUMN response_status INT COMMENT 'HTTP响应状态码';

-- ========== 性能监控 ==========

ALTER TABLE system_log ADD COLUMN execution_time BIGINT COMMENT '执行耗时（毫秒）';

-- ========== 异常追踪 ==========

ALTER TABLE system_log ADD COLUMN exception_type VARCHAR(255) COMMENT '异常类型（完整类名）';
ALTER TABLE system_log ADD COLUMN error_stack TEXT COMMENT '错误堆栈信息';

-- ========== 业务关联与审计 ==========

ALTER TABLE system_log ADD COLUMN business_id VARCHAR(100) COMMENT '关联业务ID（容器ID/用户ID/成果ID等）';
ALTER TABLE system_log ADD COLUMN before_data TEXT COMMENT '操作前数据（JSON格式）';
ALTER TABLE system_log ADD COLUMN after_data TEXT COMMENT '操作后数据（JSON格式）';
ALTER TABLE system_log ADD COLUMN tags JSON COMMENT '标签（JSON数组）如：["security", "critical", "录屏"]';

-- ========== 前端日志专用 ==========

ALTER TABLE system_log ADD COLUMN browser_info VARCHAR(500) COMMENT '浏览器信息（User-Agent）';

-- ========== 索引优化（提升查询性能） ==========

-- 按日志级别查询（查看所有ERROR日志）
CREATE INDEX idx_log_level ON system_log(log_level);

-- 按日志分类查询（安全日志、性能日志）
CREATE INDEX idx_log_category ON system_log(log_category);

-- 按追踪ID查询（分布式追踪全链路）
CREATE INDEX idx_trace_id ON system_log(trace_id);

-- 按会话ID查询（用户行为分析）
CREATE INDEX idx_session_id ON system_log(session_id);

-- 按时间范围查询（时间序列分析）
CREATE INDEX idx_timestamp ON system_log(timestamp);

-- 按业务ID查询（审计追踪）
CREATE INDEX idx_business_id ON system_log(business_id);

-- 组合索引：时间+级别（常用查询优化）
CREATE INDEX idx_timestamp_level ON system_log(timestamp, log_level);

-- 组合索引：时间+分类（按时间查询特定类型日志）
CREATE INDEX idx_timestamp_category ON system_log(timestamp, log_category);

-- ========== 数据填充（为历史日志设置默认值） ==========

-- 为现有日志设置默认值（避免NULL）
UPDATE system_log
SET
    log_level = 'INFO',
    log_category = 'SYSTEM'
WHERE log_level IS NULL;

-- ========== 注释说明 ==========

-- 字段使用说明：
-- 1. log_level: 用于过滤日志严重程度（ERROR > WARN > INFO > DEBUG）
-- 2. log_category: 用于日志分类聚合（安全审计、性能分析、业务监控）
-- 3. trace_id: 分布式系统中请求全链路追踪（可结合Sleuth/Zipkin）
-- 4. session_id: 用户会话级别分析（用户行为轨迹）
-- 5. execution_time: 性能瓶颈分析（慢SQL、慢接口）
-- 6. before_data/after_data: 审计追踪（敏感操作前后对比）
-- 7. tags: 灵活标签系统（支持多维度检索）
-- 8. browser_info: 前端日志分析（兼容性问题定位）
-- 9. error_stack: 异常定位（快速还原错误现场）
