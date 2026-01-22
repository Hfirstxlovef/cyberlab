package org.cyberlab.entity;

import jakarta.persistence.*;
import org.cyberlab.enums.BusinessModule;
import org.cyberlab.enums.OperationStatus;
import org.cyberlab.enums.OperationType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_log", indexes = {
    @Index(name = "idx_system_log_timestamp_desc", columnList = "timestamp DESC"),
    @Index(name = "idx_system_log_username", columnList = "username"),
    @Index(name = "idx_system_log_level", columnList = "log_level"),
    @Index(name = "idx_system_log_category", columnList = "log_category"),
    @Index(name = "idx_system_log_operation_type", columnList = "operation_type"),
    @Index(name = "idx_system_log_business_module", columnList = "business_module"),
    @Index(name = "idx_system_log_operation_status", columnList = "operation_status"),
    @Index(name = "idx_system_log_timestamp_username", columnList = "timestamp DESC, username"),
    @Index(name = "idx_system_log_level_timestamp", columnList = "log_level, timestamp DESC"),
    @Index(name = "idx_system_log_trace_id", columnList = "trace_id"),
    @Index(name = "idx_system_log_session_id", columnList = "session_id"),
    @Index(name = "idx_system_log_business_id", columnList = "business_id")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) // ✅ 明确指定表名，避免默认映射错误
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String operation;

    @Column(length = 4000)
    private String description;

    private String ip;
    private LocalDateTime timestamp;

    // ========== 新增字段（日志增强） ==========

    @Column(name = "log_level")
    private String logLevel;  // INFO, WARN, ERROR, DEBUG

    @Column(name = "log_category")
    private String logCategory;  // SYSTEM, BUSINESS, SECURITY, PERFORMANCE, AUDIT, FRONTEND

    @Column(name = "trace_id")
    private String traceId;  // 请求追踪ID

    @Column(name = "session_id")
    private String sessionId;  // 用户会话ID

    @Column(name = "user_role")
    private String userRole;  // 用户角色

    @Column(name = "request_url", length = 1000)
    private String requestUrl;  // 请求URL（支持长URL及中文参数编码）

    @Column(name = "request_method")
    private String requestMethod;  // HTTP方法 (GET, POST, PUT, DELETE)

    @Column(name = "response_status")
    private Integer responseStatus;  // 响应状态码

    @Column(name = "execution_time")
    private Long executionTime;  // 执行耗时(毫秒)

    @Column(name = "exception_type")
    private String exceptionType;  // 异常类型

    @Column(name = "business_id")
    private String businessId;  // 关联业务ID（如容器ID、用户ID、成果ID）

    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;  // 操作前数据（JSON格式）

    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;  // 操作后数据（JSON格式）

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;  // 标签（JSON数组）如：["security", "critical", "录屏"]

    @Column(name = "browser_info")
    private String browserInfo;  // 浏览器信息（前端日志）

    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;  // 错误堆栈（异常日志）

    // ========== 操作行为分类字段 ==========

    @Column(name = "operation_type")
    @Enumerated(EnumType.STRING)
    private OperationType operationType;  // 操作类型：CREATE/UPDATE/DELETE/EXECUTE等

    @Column(name = "business_module")
    @Enumerated(EnumType.STRING)
    private BusinessModule businessModule;  // 业务模块：CONTAINER/USER/ACHIEVEMENT等

    @Column(name = "object_type")
    private String objectType;  // 操作对象类型：Container/User/Achievement等

    @Column(name = "operation_status")
    @Enumerated(EnumType.STRING)
    private OperationStatus operationStatus;  // 操作状态：SUCCESS/FAILED/PARTIAL等

    // ========== Getter and Setter ==========

    // 原有字段
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // 新增字段
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getLogCategory() { return logCategory; }
    public void setLogCategory(String logCategory) { this.logCategory = logCategory; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }

    public Long getExecutionTime() { return executionTime; }
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }

    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }

    // 操作分类字段
    public OperationType getOperationType() { return operationType; }
    public void setOperationType(OperationType operationType) { this.operationType = operationType; }

    public BusinessModule getBusinessModule() { return businessModule; }
    public void setBusinessModule(BusinessModule businessModule) { this.businessModule = businessModule; }

    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }

    public OperationStatus getOperationStatus() { return operationStatus; }
    public void setOperationStatus(OperationStatus operationStatus) { this.operationStatus = operationStatus; }
}