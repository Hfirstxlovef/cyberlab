package org.cyberlab.repository;

import org.cyberlab.entity.SystemLog;
import org.cyberlab.enums.BusinessModule;
import org.cyberlab.enums.OperationStatus;
import org.cyberlab.enums.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    // ✅ 模糊匹配用户名和操作行为（适用于管理员查看）
    List<SystemLog> findByUsernameContainingAndOperationContaining(String username, String operation);

    // ✅ 支持分页的版本（管理员分页查看日志）
    Page<SystemLog> findByUsernameContainingAndOperationContaining(String username, String operation, Pageable pageable);

    // ✅ 普通用户查看自己的操作记录（用户名精确匹配 + 操作行为模糊匹配）
    List<SystemLog> findByUsernameAndOperationContaining(String username, String operation);

    // ✅ 支持分页的版本（普通用户分页查看）
    Page<SystemLog> findByUsernameAndOperationContaining(String username, String operation, Pageable pageable);

    // ✅ 清理三个月前的日志
    int deleteByTimestampBefore(LocalDateTime time);

    // 在现有的SystemLogRepository中添加以下方法
    List<SystemLog> findTop10ByOrderByTimestampDesc();

    /**
     * 查询最近10条日志（带时间范围限制，优化性能）
     * @param after 时间起点（只查询此时间之后的日志）
     */
    List<SystemLog> findTop10ByTimestampAfterOrderByTimestampDesc(LocalDateTime after);

    long countByTimestampAfter(LocalDateTime dateTime);
    
    /**
     * 根据时间范围统计日志数量
     */
    long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围查询日志
     */
    List<SystemLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户名和时间范围查询日志
     */
    List<SystemLog> findByUsernameAndTimestampBetween(String username, LocalDateTime startTime, LocalDateTime endTime);
    
    // 容器审计相关查询方法
    List<SystemLog> findByOperationContaining(String operation);
    Page<SystemLog> findByOperationContaining(String operation, Pageable pageable);
    
    List<SystemLog> findByOperationContainingAndTimestampBetween(String operation, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByOperationContainingAndTimestampBetween(String operation, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    List<SystemLog> findByUsernameAndOperationContainingAndTimestampBetween(String username, String operation, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByUsernameAndOperationContainingAndTimestampBetween(String username, String operation, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    List<SystemLog> findByOperationContainingAndTimestampAfter(String operation, LocalDateTime timestamp);
    List<SystemLog> findByOperationContainingAndTimestampBefore(String operation, LocalDateTime timestamp);

    // ========== 日志级别查询 ==========

    /**
     * 按日志级别查询
     * @param logLevel 日志级别（INFO/WARN/ERROR/DEBUG）
     */
    List<SystemLog> findByLogLevel(String logLevel);
    Page<SystemLog> findByLogLevel(String logLevel, Pageable pageable);

    /**
     * 按日志级别和时间范围查询
     */
    List<SystemLog> findByLogLevelAndTimestampBetween(String logLevel, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByLogLevelAndTimestampBetween(String logLevel, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计指定级别的日志数量
     */
    long countByLogLevel(String logLevel);
    long countByLogLevelAndTimestampBetween(String logLevel, LocalDateTime startTime, LocalDateTime endTime);

    // ========== 日志分类查询 ==========

    /**
     * 按日志分类查询
     * @param logCategory 日志分类（SYSTEM/BUSINESS/SECURITY/PERFORMANCE/AUDIT/FRONTEND）
     */
    List<SystemLog> findByLogCategory(String logCategory);
    Page<SystemLog> findByLogCategory(String logCategory, Pageable pageable);

    /**
     * 按日志分类和时间范围查询
     */
    List<SystemLog> findByLogCategoryAndTimestampBetween(String logCategory, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByLogCategoryAndTimestampBetween(String logCategory, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计指定分类的日志数量（带时间范围）
     */
    long countByLogCategoryAndTimestampBetween(String logCategory, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 组合查询：级别 + 分类
     */
    List<SystemLog> findByLogLevelAndLogCategory(String logLevel, String logCategory);
    Page<SystemLog> findByLogLevelAndLogCategory(String logLevel, String logCategory, Pageable pageable);

    /**
     * 组合查询：级别 + 分类 + 时间范围
     */
    List<SystemLog> findByLogLevelAndLogCategoryAndTimestampBetween(String logLevel, String logCategory, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByLogLevelAndLogCategoryAndTimestampBetween(String logLevel, String logCategory, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== 分布式追踪查询 ==========

    /**
     * 按追踪ID查询（获取完整请求链路）
     * @param traceId 请求追踪ID
     */
    List<SystemLog> findByTraceIdOrderByTimestampAsc(String traceId);

    /**
     * 按会话ID查询（用户行为分析）
     * @param sessionId 用户会话ID
     */
    List<SystemLog> findBySessionIdOrderByTimestampAsc(String sessionId);
    Page<SystemLog> findBySessionIdOrderByTimestampAsc(String sessionId, Pageable pageable);

    // ========== 用户角色查询 ==========

    /**
     * 按用户角色查询
     * @param userRole 用户角色（admin/red/blue/judge）
     */
    List<SystemLog> findByUserRole(String userRole);
    Page<SystemLog> findByUserRole(String userRole, Pageable pageable);

    /**
     * 按用户角色和时间范围查询
     */
    List<SystemLog> findByUserRoleAndTimestampBetween(String userRole, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByUserRoleAndTimestampBetween(String userRole, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== HTTP请求查询 ==========

    /**
     * 按请求URL模糊查询
     * @param requestUrl 请求URL（支持模糊匹配）
     */
    List<SystemLog> findByRequestUrlContaining(String requestUrl);
    Page<SystemLog> findByRequestUrlContaining(String requestUrl, Pageable pageable);

    /**
     * 按HTTP方法查询
     * @param requestMethod HTTP方法（GET/POST/PUT/DELETE）
     */
    List<SystemLog> findByRequestMethod(String requestMethod);
    Page<SystemLog> findByRequestMethod(String requestMethod, Pageable pageable);

    /**
     * 按响应状态码查询（如查找所有4xx/5xx错误）
     * @param responseStatus HTTP响应状态码
     */
    List<SystemLog> findByResponseStatus(Integer responseStatus);
    Page<SystemLog> findByResponseStatus(Integer responseStatus, Pageable pageable);

    /**
     * 按响应状态码范围查询（如查找所有错误响应 >= 400）
     */
    List<SystemLog> findByResponseStatusGreaterThanEqual(Integer responseStatus);
    Page<SystemLog> findByResponseStatusGreaterThanEqual(Integer responseStatus, Pageable pageable);

    // ========== 性能分析查询 ==========

    /**
     * 查询慢操作（执行时间超过阈值）
     * @param executionTime 执行时间阈值（毫秒）
     */
    List<SystemLog> findByExecutionTimeGreaterThan(Long executionTime);
    Page<SystemLog> findByExecutionTimeGreaterThan(Long executionTime, Pageable pageable);

    /**
     * 查询慢操作（带时间范围）
     */
    List<SystemLog> findByExecutionTimeGreaterThanAndTimestampBetween(Long executionTime, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByExecutionTimeGreaterThanAndTimestampBetween(Long executionTime, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计平均执行时间（需要配合 @Query 使用）
     * 注：这里只声明方法，实际需要在 Service 层通过聚合查询实现
     */

    // ========== 异常查询 ==========

    /**
     * 按异常类型查询
     * @param exceptionType 异常类型（完整类名）
     */
    List<SystemLog> findByExceptionTypeContaining(String exceptionType);
    Page<SystemLog> findByExceptionTypeContaining(String exceptionType, Pageable pageable);

    /**
     * 查询所有包含异常的日志
     */
    List<SystemLog> findByExceptionTypeIsNotNull();
    Page<SystemLog> findByExceptionTypeIsNotNull(Pageable pageable);

    /**
     * 按异常类型和时间范围查询
     */
    List<SystemLog> findByExceptionTypeContainingAndTimestampBetween(String exceptionType, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByExceptionTypeContainingAndTimestampBetween(String exceptionType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== 业务关联查询 ==========

    /**
     * 按业务ID查询（审计追踪）
     * @param businessId 业务ID（容器ID/用户ID/成果ID等）
     */
    List<SystemLog> findByBusinessIdOrderByTimestampAsc(String businessId);
    Page<SystemLog> findByBusinessIdOrderByTimestampAsc(String businessId, Pageable pageable);

    /**
     * 按业务ID和操作类型查询
     */
    List<SystemLog> findByBusinessIdAndOperationContaining(String businessId, String operation);

    /**
     * 查询有操作前后数据的日志（审计日志）
     */
    List<SystemLog> findByBeforeDataIsNotNullOrAfterDataIsNotNull();
    Page<SystemLog> findByBeforeDataIsNotNullOrAfterDataIsNotNull(Pageable pageable);

    // ========== 前端日志查询 ==========

    /**
     * 按浏览器信息模糊查询
     * @param browserInfo 浏览器信息（User-Agent）
     */
    List<SystemLog> findByBrowserInfoContaining(String browserInfo);
    Page<SystemLog> findByBrowserInfoContaining(String browserInfo, Pageable pageable);

    /**
     * 查询所有前端日志
     */
    List<SystemLog> findByLogCategoryAndBrowserInfoIsNotNull(String logCategory);
    Page<SystemLog> findByLogCategoryAndBrowserInfoIsNotNull(String logCategory, Pageable pageable);

    // ========== 复杂组合查询（高级搜索） ==========

    /**
     * 多条件组合查询（用户名 + 角色 + 级别 + 时间范围）
     */
    Page<SystemLog> findByUsernameContainingAndUserRoleAndLogLevelAndTimestampBetween(
        String username, String userRole, String logLevel, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 安全审计查询（安全类别 + 错误级别 + 时间范围）
     */
    Page<SystemLog> findByLogCategoryAndLogLevelInAndTimestampBetween(
        String logCategory, List<String> logLevels, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== 操作行为分类查询（新增字段） ==========

    /**
     * 按操作类型查询
     * @param operationType 操作类型（CREATE/UPDATE/DELETE/EXECUTE/QUERY/IMPORT/EXPORT/UPLOAD/DOWNLOAD/LOGIN）
     */
    List<SystemLog> findByOperationType(OperationType operationType);
    Page<SystemLog> findByOperationType(OperationType operationType, Pageable pageable);

    /**
     * 按操作类型和时间范围查询
     */
    List<SystemLog> findByOperationTypeAndTimestampBetween(OperationType operationType, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByOperationTypeAndTimestampBetween(OperationType operationType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计指定操作类型的数量
     */
    long countByOperationType(OperationType operationType);
    long countByOperationTypeAndTimestampBetween(OperationType operationType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按业务模块查询
     * @param businessModule 业务模块（USER/CONTAINER/ASSET/ACHIEVEMENT/SCENARIO/HOST/AUDIT/OTHER）
     */
    List<SystemLog> findByBusinessModule(BusinessModule businessModule);
    Page<SystemLog> findByBusinessModule(BusinessModule businessModule, Pageable pageable);

    /**
     * 按业务模块和时间范围查询
     */
    List<SystemLog> findByBusinessModuleAndTimestampBetween(BusinessModule businessModule, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByBusinessModuleAndTimestampBetween(BusinessModule businessModule, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计指定业务模块的操作数量
     */
    long countByBusinessModule(BusinessModule businessModule);
    long countByBusinessModuleAndTimestampBetween(BusinessModule businessModule, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按操作对象类型查询
     * @param objectType 操作对象类型（如：User, Container, Asset等）
     */
    List<SystemLog> findByObjectType(String objectType);
    Page<SystemLog> findByObjectType(String objectType, Pageable pageable);

    /**
     * 按操作对象类型和时间范围查询
     */
    List<SystemLog> findByObjectTypeAndTimestampBetween(String objectType, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByObjectTypeAndTimestampBetween(String objectType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 按操作状态查询
     * @param operationStatus 操作状态（SUCCESS/FAILED/PENDING）
     */
    List<SystemLog> findByOperationStatus(OperationStatus operationStatus);
    Page<SystemLog> findByOperationStatus(OperationStatus operationStatus, Pageable pageable);

    /**
     * 按操作状态和时间范围查询
     */
    List<SystemLog> findByOperationStatusAndTimestampBetween(OperationStatus operationStatus, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByOperationStatusAndTimestampBetween(OperationStatus operationStatus, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计失败操作数量
     */
    long countByOperationStatus(OperationStatus operationStatus);
    long countByOperationStatusAndTimestampBetween(OperationStatus operationStatus, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 组合查询：操作类型 + 业务模块
     */
    List<SystemLog> findByOperationTypeAndBusinessModule(OperationType operationType, BusinessModule businessModule);
    Page<SystemLog> findByOperationTypeAndBusinessModule(OperationType operationType, BusinessModule businessModule, Pageable pageable);

    /**
     * 组合查询：操作类型 + 业务模块 + 操作状态
     */
    List<SystemLog> findByOperationTypeAndBusinessModuleAndOperationStatus(
        OperationType operationType, BusinessModule businessModule, OperationStatus operationStatus);
    Page<SystemLog> findByOperationTypeAndBusinessModuleAndOperationStatus(
        OperationType operationType, BusinessModule businessModule, OperationStatus operationStatus, Pageable pageable);

    /**
     * 组合查询：业务模块 + 操作状态 + 时间范围（用于统计某模块的成功/失败操作）
     */
    List<SystemLog> findByBusinessModuleAndOperationStatusAndTimestampBetween(
        BusinessModule businessModule, OperationStatus operationStatus, LocalDateTime startTime, LocalDateTime endTime);
    Page<SystemLog> findByBusinessModuleAndOperationStatusAndTimestampBetween(
        BusinessModule businessModule, OperationStatus operationStatus, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计指定业务模块和操作状态的日志数量（带时间范围）
     */
    long countByBusinessModuleAndOperationStatusAndTimestampBetween(
        BusinessModule businessModule, OperationStatus operationStatus, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 高级组合查询：用户 + 业务模块 + 操作类型 + 时间范围
     */
    Page<SystemLog> findByUsernameAndBusinessModuleAndOperationTypeAndTimestampBetween(
        String username, BusinessModule businessModule, OperationType operationType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询用户在指定模块的失败操作（用于安全审计）
     */
    List<SystemLog> findByUsernameAndBusinessModuleAndOperationStatus(
        String username, BusinessModule businessModule, OperationStatus operationStatus);
    Page<SystemLog> findByUsernameAndBusinessModuleAndOperationStatus(
        String username, BusinessModule businessModule, OperationStatus operationStatus, Pageable pageable);
}