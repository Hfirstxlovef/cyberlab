package org.cyberlab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cyberlab.entity.SystemLog;
import org.cyberlab.enums.BusinessModule;
import org.cyberlab.enums.OperationStatus;
import org.cyberlab.enums.OperationType;
import org.cyberlab.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 增强日志服务
 *
 * 提供编程式的日志记录API，支持：
 * 1. 分类日志记录（系统/业务/安全/性能/审计/前端）
 * 2. 性能分析（慢查询、慢接口）
 * 3. 前端异常收集
 * 4. 日志统计与分析
 */
@Service
public class EnhancedLogService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedLogService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SystemLogRepository logRepository;

    // ========== 分类日志记录 ==========

    /**
     * 记录系统日志
     */
    public void logSystem(String operation, String description, String level) {
        SystemLog systemLog = createBaseLog(operation, description, level, "SYSTEM");
        saveLogSafely(systemLog);
    }

    /**
     * 记录业务日志
     */
    public void logBusiness(String operation, String description, String businessId) {
        SystemLog systemLog = createBaseLog(operation, description, "INFO", "BUSINESS");
        systemLog.setBusinessId(businessId);
        saveLogSafely(systemLog);
    }

    /**
     * 记录安全日志
     */
    public void logSecurity(String operation, String description, String username, String level) {
        SystemLog systemLog = createBaseLog(operation, description, level, "SECURITY");
        systemLog.setUsername(username);
        saveLogSafely(systemLog);
    }

    /**
     * 记录性能日志
     */
    public void logPerformance(String operation, String requestUrl, String requestMethod,
                               long executionTime, String description) {
        SystemLog systemLog = createBaseLog(operation, description, "INFO", "PERFORMANCE");
        systemLog.setRequestUrl(requestUrl);
        systemLog.setRequestMethod(requestMethod);
        systemLog.setExecutionTime(executionTime);

        // 执行时间超过阈值时提升日志级别
        if (executionTime > 5000) {
            systemLog.setLogLevel("ERROR");
        } else if (executionTime > 3000) {
            systemLog.setLogLevel("WARN");
        }

        saveLogSafely(systemLog);

        // 同时输出到应用日志
        if (executionTime > 3000) {
            log.warn("慢操作检测 [{}] 耗时: {}ms, URL: {}", operation, executionTime, requestUrl);
        }
    }

    /**
     * 记录审计日志（带操作前后数据）
     */
    public void logAudit(String operation, String username, String businessId,
                         Object beforeData, Object afterData, String[] tags) {
        SystemLog systemLog = createBaseLog(operation, "审计操作: " + operation, "WARN", "AUDIT");
        systemLog.setUsername(username);
        systemLog.setBusinessId(businessId);

        // 序列化前后数据
        systemLog.setBeforeData(serializeObject(beforeData));
        systemLog.setAfterData(serializeObject(afterData));

        // 设置标签
        if (tags != null && tags.length > 0) {
            try {
                systemLog.setTags(objectMapper.writeValueAsString(tags));
            } catch (Exception e) {
                log.warn("序列化标签失败: {}", e.getMessage());
            }
        }

        saveLogSafely(systemLog);
    }

    /**
     * 记录前端异常日志
     */
    public void logFrontendError(String errorMessage, String errorStack, String url,
                                 String browserInfo, String username) {
        SystemLog systemLog = createBaseLog("前端异常", errorMessage, "ERROR", "FRONTEND");
        systemLog.setUsername(username);
        systemLog.setRequestUrl(url);
        systemLog.setBrowserInfo(browserInfo);
        systemLog.setErrorStack(errorStack);
        systemLog.setExceptionType("FrontendError");

        saveLogSafely(systemLog);

        // 输出到应用日志便于监控
        log.error("前端异常 - 用户: {}, URL: {}, 错误: {}", username, url, errorMessage);
    }

    // ========== 日志查询与统计 ==========

    /**
     * 按级别统计日志数量
     */
    public Map<String, Long> countByLogLevel(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> stats = new HashMap<>();
        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};

        for (String level : levels) {
            long count = logRepository.countByLogLevelAndTimestampBetween(level, startTime, endTime);
            stats.put(level, count);
        }

        return stats;
    }

    /**
     * 按分类统计日志数量
     */
    public Map<String, Long> countByCategory(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> stats = new HashMap<>();
        String[] categories = {"SYSTEM", "BUSINESS", "SECURITY", "PERFORMANCE", "AUDIT", "FRONTEND"};

        for (String category : categories) {
            long count = logRepository.countByLogCategoryAndTimestampBetween(category, startTime, endTime);
            stats.put(category, count);
        }

        return stats;
    }

    /**
     * 获取慢操作列表
     */
    public Page<SystemLog> getSlowOperations(long thresholdMs, Pageable pageable) {
        return logRepository.findByExecutionTimeGreaterThan(thresholdMs, pageable);
    }

    /**
     * 获取慢操作列表（带时间范围）
     */
    public Page<SystemLog> getSlowOperations(long thresholdMs, LocalDateTime startTime,
                                             LocalDateTime endTime, Pageable pageable) {
        return logRepository.findByExecutionTimeGreaterThanAndTimestampBetween(
                thresholdMs, startTime, endTime, pageable);
    }

    /**
     * 获取异常日志列表
     */
    public Page<SystemLog> getErrorLogs(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return logRepository.findByLogLevelAndTimestampBetween("ERROR", startTime, endTime, pageable);
    }

    /**
     * 获取所有异常类型统计
     */
    public Map<String, Long> getExceptionTypeStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<SystemLog> errorLogs = logRepository.findByLogLevelAndTimestampBetween("ERROR", startTime, endTime);

        Map<String, Long> stats = new HashMap<>();
        for (SystemLog log : errorLogs) {
            String exceptionType = log.getExceptionType();
            if (exceptionType != null && !exceptionType.isEmpty()) {
                stats.put(exceptionType, stats.getOrDefault(exceptionType, 0L) + 1);
            }
        }

        return stats;
    }

    /**
     * 按TraceId查询完整请求链路
     */
    public List<SystemLog> getTraceChain(String traceId) {
        return logRepository.findByTraceIdOrderByTimestampAsc(traceId);
    }

    /**
     * 按SessionId查询用户行为轨迹
     */
    public Page<SystemLog> getUserBehaviorTrace(String sessionId, Pageable pageable) {
        return logRepository.findBySessionIdOrderByTimestampAsc(sessionId, pageable);
    }

    /**
     * 获取业务实体的审计追踪
     */
    public Page<SystemLog> getBusinessAuditTrail(String businessId, Pageable pageable) {
        return logRepository.findByBusinessIdOrderByTimestampAsc(businessId, pageable);
    }

    /**
     * 获取安全审计日志（SECURITY + ERROR/WARN）
     */
    public Page<SystemLog> getSecurityAuditLogs(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        List<String> levels = Arrays.asList("ERROR", "WARN");
        return logRepository.findByLogCategoryAndLogLevelInAndTimestampBetween(
                "SECURITY", levels, startTime, endTime, pageable);
    }

    /**
     * 获取前端异常日志
     */
    public Page<SystemLog> getFrontendErrorLogs(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return logRepository.findByLogCategoryAndTimestampBetween("FRONTEND", startTime, endTime, pageable);
    }

    // ========== 操作行为分类统计（新增） ==========

    /**
     * 按操作类型统计
     * @return Map<操作类型, 数量>
     */
    public Map<String, Long> countByOperationType(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> stats = new LinkedHashMap<>();

        for (OperationType type : OperationType.values()) {
            long count = logRepository.countByOperationTypeAndTimestampBetween(type, startTime, endTime);
            stats.put(type.getDescription(), count);
        }

        return stats;
    }

    /**
     * 按业务模块统计
     * @return Map<业务模块, 数量>
     */
    public Map<String, Long> countByBusinessModule(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> stats = new LinkedHashMap<>();

        for (BusinessModule module : BusinessModule.values()) {
            long count = logRepository.countByBusinessModuleAndTimestampBetween(module, startTime, endTime);
            stats.put(module.getDescription(), count);
        }

        return stats;
    }

    /**
     * 按操作状态统计
     * @return Map<操作状态, 数量>
     */
    public Map<String, Long> countByOperationStatus(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> stats = new LinkedHashMap<>();

        for (OperationStatus status : OperationStatus.values()) {
            long count = logRepository.countByOperationStatusAndTimestampBetween(status, startTime, endTime);
            stats.put(status.getDescription(), count);
        }

        return stats;
    }

    /**
     * 获取指定业务模块的操作成功率
     * @param businessModule 业务模块
     * @return Map包含：total（总数）、success（成功数）、failed（失败数）、successRate（成功率）
     */
    public Map<String, Object> getModuleSuccessRate(BusinessModule businessModule,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        long totalCount = logRepository.countByBusinessModuleAndTimestampBetween(businessModule, startTime, endTime);
        long successCount = logRepository.countByBusinessModuleAndOperationStatusAndTimestampBetween(
            businessModule, OperationStatus.SUCCESS, startTime, endTime);
        long failedCount = logRepository.countByBusinessModuleAndOperationStatusAndTimestampBetween(
            businessModule, OperationStatus.FAILED, startTime, endTime);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", businessModule.getDescription());
        result.put("total", totalCount);
        result.put("success", successCount);
        result.put("failed", failedCount);
        result.put("successRate", totalCount > 0 ? String.format("%.2f%%", (successCount * 100.0 / totalCount)) : "0.00%");

        return result;
    }

    /**
     * 获取所有业务模块的操作成功率统计
     * @return List<Map>，每个Map包含模块的统计数据
     */
    public List<Map<String, Object>> getAllModulesSuccessRate(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (BusinessModule module : BusinessModule.values()) {
            Map<String, Object> moduleStats = getModuleSuccessRate(module, startTime, endTime);
            results.add(moduleStats);
        }

        return results;
    }

    /**
     * 按操作对象类型统计（TOP N）
     * @param topN 返回前N个最活跃的对象类型
     * @return Map<对象类型, 数量>，按数量降序
     */
    public Map<String, Long> getTopObjectTypes(int topN, LocalDateTime startTime, LocalDateTime endTime) {
        List<SystemLog> logs = logRepository.findByTimestampBetween(startTime, endTime);

        // 统计各对象类型的数量
        Map<String, Long> typeCount = new HashMap<>();
        for (SystemLog log : logs) {
            String objectType = log.getObjectType();
            if (objectType != null && !objectType.isEmpty()) {
                typeCount.put(objectType, typeCount.getOrDefault(objectType, 0L) + 1);
            }
        }

        // 按数量降序排序并取前N个
        return typeCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .collect(LinkedHashMap::new,
                    (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                    LinkedHashMap::putAll);
    }

    /**
     * 获取指定用户在各业务模块的操作分布
     * @param username 用户名
     * @return Map<业务模块, 操作数量>
     */
    public Map<String, Long> getUserModuleDistribution(String username,
                                                        LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> distribution = new LinkedHashMap<>();

        List<SystemLog> userLogs = logRepository.findByUsernameAndTimestampBetween(username, startTime, endTime);

        for (SystemLog log : userLogs) {
            BusinessModule module = log.getBusinessModule();
            if (module != null) {
                String moduleName = module.getDescription();
                distribution.put(moduleName, distribution.getOrDefault(moduleName, 0L) + 1);
            }
        }

        return distribution;
    }

    /**
     * 获取指定业务模块的失败操作详情
     * @param businessModule 业务模块
     * @return 失败操作的分页列表
     */
    public Page<SystemLog> getModuleFailedOperations(BusinessModule businessModule,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      Pageable pageable) {
        return logRepository.findByBusinessModuleAndOperationStatusAndTimestampBetween(
            businessModule, OperationStatus.FAILED, startTime, endTime, pageable);
    }

    /**
     * 获取指定操作类型的日志
     * @param operationType 操作类型
     * @return 分页结果
     */
    public Page<SystemLog> getLogsByOperationType(OperationType operationType,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Pageable pageable) {
        return logRepository.findByOperationTypeAndTimestampBetween(operationType, startTime, endTime, pageable);
    }

    /**
     * 获取用户在指定模块的失败操作（安全审计用）
     * @param username 用户名
     * @param businessModule 业务模块
     * @return 失败操作列表
     */
    public Page<SystemLog> getUserModuleFailures(String username, BusinessModule businessModule, Pageable pageable) {
        return logRepository.findByUsernameAndBusinessModuleAndOperationStatus(
            username, businessModule, OperationStatus.FAILED, pageable);
    }

    /**
     * 获取综合操作统计（包含所有维度）
     * @return 综合统计报告
     */
    public Map<String, Object> getComprehensiveStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总体统计
        long totalLogs = logRepository.countByTimestampBetween(startTime, endTime);
        stats.put("totalLogs", totalLogs);
        stats.put("timeRange", Map.of("start", startTime, "end", endTime));

        // 按操作类型统计
        stats.put("byOperationType", countByOperationType(startTime, endTime));

        // 按业务模块统计
        stats.put("byBusinessModule", countByBusinessModule(startTime, endTime));

        // 按操作状态统计
        stats.put("byOperationStatus", countByOperationStatus(startTime, endTime));

        // 按日志级别统计
        stats.put("byLogLevel", countByLogLevel(startTime, endTime));

        // 按分类统计
        stats.put("byCategory", countByCategory(startTime, endTime));

        // 各模块成功率
        stats.put("moduleSuccessRates", getAllModulesSuccessRate(startTime, endTime));

        // TOP 10 操作对象类型
        stats.put("topObjectTypes", getTopObjectTypes(10, startTime, endTime));

        return stats;
    }

    /**
     * 获取操作类型和业务模块的交叉统计
     * @return Map<操作类型, Map<业务模块, 数量>>
     */
    public Map<String, Map<String, Long>> getOperationModuleCrossStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Map<String, Long>> crossStats = new LinkedHashMap<>();

        for (OperationType type : OperationType.values()) {
            Map<String, Long> moduleStats = new LinkedHashMap<>();

            for (BusinessModule module : BusinessModule.values()) {
                List<SystemLog> logs = logRepository.findByOperationTypeAndBusinessModule(type, module);
                long count = logs.stream()
                    .filter(log -> log.getTimestamp().isAfter(startTime) && log.getTimestamp().isBefore(endTime))
                    .count();

                if (count > 0) {
                    moduleStats.put(module.getDescription(), count);
                }
            }

            if (!moduleStats.isEmpty()) {
                crossStats.put(type.getDescription(), moduleStats);
            }
        }

        return crossStats;
    }

    // ========== 日志清理 ==========

    /**
     * 清理指定时间之前的日志
     */
    @Transactional
    public int cleanOldLogs(LocalDateTime beforeTime) {
        try {
            int deletedCount = logRepository.deleteByTimestampBefore(beforeTime);
            log.info("清理旧日志完成，删除 {} 条记录（时间早于 {}）", deletedCount, beforeTime);
            return deletedCount;
        } catch (Exception e) {
            log.error("清理旧日志失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 归档日志（标记为已归档，实际归档操作由定时任务完成）
     */
    public void archiveLogs(LocalDateTime startTime, LocalDateTime endTime) {
        // 归档逻辑由 LogArchiveTask 实现
        log.info("日志归档请求：{} ~ {}", startTime, endTime);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 创建基础日志对象
     */
    private SystemLog createBaseLog(String operation, String description, String level, String category) {
        SystemLog systemLog = new SystemLog();
        systemLog.setOperation(operation);
        systemLog.setDescription(description);
        systemLog.setLogLevel(level);
        systemLog.setLogCategory(category);
        systemLog.setTimestamp(LocalDateTime.now());

        // 尝试从 MDC 获取 traceId
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isEmpty()) {
            systemLog.setTraceId(traceId);
        }

        // 默认信息
        systemLog.setUsername("system");
        systemLog.setIp("127.0.0.1");

        return systemLog;
    }

    /**
     * 序列化对象为JSON
     */
    private String serializeObject(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(obj);
            // 限制长度
            if (json.length() > 4000) {
                return json.substring(0, 4000) + "...(已截断)";
            }
            return json;
        } catch (Exception e) {
            return "序列化失败: " + e.getMessage();
        }
    }

    /**
     * 安全地保存日志（失败不影响业务）
     */
    private void saveLogSafely(SystemLog systemLog) {
        try {
            logRepository.save(systemLog);
        } catch (Exception e) {
            // 日志保存失败不应影响业务逻辑
            log.error("保存日志失败: {}", e.getMessage());
        }
    }
}
