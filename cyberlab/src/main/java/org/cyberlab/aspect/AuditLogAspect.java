package org.cyberlab.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.cyberlab.annotation.AuditLog;
import org.cyberlab.entity.SystemLog;
import org.cyberlab.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * 审计日志切面
 * 功能：
 * 1. 捕获操作前后数据（beforeData/afterData）
 * 2. 强制使用 AUDIT/SECURITY 分类
 * 3. 支持标签系统
 * 4. 自动提取业务ID
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SystemLogRepository logRepository;

    @Pointcut("@annotation(org.cyberlab.annotation.AuditLog)")
    public void auditLogPointcut() {}

    @Around("auditLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 1. 获取审计注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog auditAnnotation = signature.getMethod().getAnnotation(AuditLog.class);

        // 2. 获取或生成 traceId
        String traceId = getOrCreateTraceId();

        // 3. 捕获操作前数据
        String beforeData = null;
        if (auditAnnotation.captureArgs()) {
            beforeData = serializeArgs(joinPoint.getArgs());
        }

        // 4. 获取请求上下文
        RequestContext context = getRequestContext();

        Object result = null;
        Throwable exception = null;
        String afterData = null;

        try {
            // 5. 执行目标方法
            result = joinPoint.proceed();

            // 6. 捕获操作后数据
            if (auditAnnotation.captureResult() && result != null) {
                afterData = serializeObject(result);
            }

            return result;
        } catch (Throwable ex) {
            exception = ex;
            throw ex;
        } finally {
            try {
                // 7. 计算执行时间
                long executionTime = System.currentTimeMillis() - startTime;

                // 8. 保存审计日志
                saveAuditLog(joinPoint, auditAnnotation, context, traceId,
                            beforeData, afterData, executionTime, exception);
            } catch (Exception e) {
                log.error("保存审计日志失败: {}", e.getMessage(), e);
            } finally {
                MDC.remove(TRACE_ID_KEY);
            }
        }
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditAnnotation,
                              RequestContext context, String traceId, String beforeData,
                              String afterData, long executionTime, Throwable exception) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

            SystemLog systemLog = new SystemLog();

            // ========== 基础字段 ==========
            systemLog.setUsername(context.getUsername());
            systemLog.setIp(context.getIp());
            systemLog.setOperation(auditAnnotation.operation());
            systemLog.setTimestamp(LocalDateTime.now());

            // Description
            StringBuilder description = new StringBuilder();
            description.append("审计操作: ").append(auditAnnotation.operation());
            if (!auditAnnotation.description().isEmpty()) {
                description.append("\n说明: ").append(auditAnnotation.description());
            }
            description.append("\n方法: ").append(methodName);
            if (exception != null) {
                description.append("\n异常: ").append(exception.getMessage());
            }
            systemLog.setDescription(description.toString());

            // ========== 审计专用字段 ==========

            // 日志级别（优先使用注解配置，异常时强制ERROR）
            systemLog.setLogLevel(exception != null ? "ERROR" : auditAnnotation.level());

            // 日志分类（强制使用 AUDIT/SECURITY）
            systemLog.setLogCategory(auditAnnotation.category());

            // 分布式追踪
            systemLog.setTraceId(traceId);
            systemLog.setSessionId(context.getSessionId());

            // 用户角色
            systemLog.setUserRole(context.getUserRole());

            // HTTP上下文
            systemLog.setRequestUrl(context.getRequestUrl());
            systemLog.setRequestMethod(context.getRequestMethod());
            systemLog.setResponseStatus(exception != null ? 500 : 200);

            // 性能指标
            systemLog.setExecutionTime(executionTime);

            // 业务ID（按优先级获取）
            String businessId = extractBusinessId(joinPoint, auditAnnotation);
            systemLog.setBusinessId(businessId);

            // 操作前后数据
            systemLog.setBeforeData(beforeData);
            systemLog.setAfterData(afterData);

            // 标签
            if (auditAnnotation.tags().length > 0) {
                systemLog.setTags(objectMapper.writeValueAsString(auditAnnotation.tags()));
            }

            // 异常信息
            if (exception != null) {
                systemLog.setExceptionType(exception.getClass().getName());
                systemLog.setErrorStack(getStackTrace(exception));
            }

            // 浏览器信息
            systemLog.setBrowserInfo(context.getBrowserInfo());

            // ========== 操作行为分类字段 ==========

            // 操作类型（从注解读取，审计日志必填）
            systemLog.setOperationType(auditAnnotation.operationType());

            // 业务模块（从注解读取，审计日志必填）
            systemLog.setBusinessModule(auditAnnotation.module());

            // 操作对象类型（从注解读取，如果为空则自动推断）
            String objectType = auditAnnotation.objectType();
            if (objectType == null || objectType.isEmpty()) {
                objectType = inferObjectType(joinPoint.getArgs());
            }
            systemLog.setObjectType(objectType);

            // 操作状态（根据异常自动判断）
            systemLog.setOperationStatus(
                exception == null ?
                org.cyberlab.enums.OperationStatus.SUCCESS :
                org.cyberlab.enums.OperationStatus.FAILED
            );

            // 保存日志
            logRepository.save(systemLog);

            // 审计日志输出（便于实时监控）
            if (exception != null) {
                log.error("审计日志 [{}] 失败 - 用户: {}, 业务ID: {}, TraceId: {}",
                        auditAnnotation.operation(), context.getUsername(), businessId, traceId);
            } else {
                log.warn("🟡 审计日志 [{}] 成功 - 用户: {}, 业务ID: {}, 耗时: {}ms, TraceId: {}",
                        auditAnnotation.operation(), context.getUsername(), businessId, executionTime, traceId);
            }

        } catch (Exception e) {
            log.error("保存审计日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 提取业务ID
     */
    private String extractBusinessId(ProceedingJoinPoint joinPoint, AuditLog auditAnnotation) {
        Object[] args = joinPoint.getArgs();

        // 1. 如果指定了参数索引
        if (auditAnnotation.businessIdParam() >= 0 && args.length > auditAnnotation.businessIdParam()) {
            Object idArg = args[auditAnnotation.businessIdParam()];
            if (idArg != null) {
                return idArg.toString();
            }
        }

        // 2. 如果指定了字段名
        if (!auditAnnotation.businessIdField().isEmpty()) {
            for (Object arg : args) {
                if (arg == null) continue;
                try {
                    java.lang.reflect.Field field = arg.getClass().getDeclaredField(auditAnnotation.businessIdField());
                    field.setAccessible(true);
                    Object fieldValue = field.get(arg);
                    if (fieldValue != null) {
                        return fieldValue.toString();
                    }
                } catch (Exception e) {
                    // Ignore and try next arg
                }
            }
        }

        // 3. 自动推断（查找常见字段名）
        String[] commonFields = {"id", "containerId", "userId", "achievementId", "drillId"};
        for (Object arg : args) {
            if (arg == null) continue;

            // 基础类型直接返回
            if (arg instanceof Long || arg instanceof Integer || arg instanceof String) {
                return arg.toString();
            }

            // 尝试从对象中提取ID字段
            for (String fieldName : commonFields) {
                try {
                    java.lang.reflect.Field field = arg.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object fieldValue = field.get(arg);
                    if (fieldValue != null) {
                        return fieldValue.toString();
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Try next field
                }
            }
        }

        return null;
    }

    /**
     * 序列化方法参数
     */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(args);
            // 限制长度
            if (json.length() > 4000) {
                return json.substring(0, 4000) + "...(已截断)";
            }
            return json;
        } catch (Exception e) {
            return "序列化参数失败: " + e.getMessage();
        }
    }

    /**
     * 序列化对象
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
            return "序列化结果失败: " + e.getMessage();
        }
    }

    /**
     * 获取或创建 TraceId
     */
    private String getOrCreateTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);

        if (traceId == null || traceId.isEmpty()) {
            try {
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof ServletRequestAttributes) {
                    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                    traceId = request.getHeader("X-Trace-Id");
                }
            } catch (Exception e) {
                // Ignore
            }

            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            MDC.put(TRACE_ID_KEY, traceId);
        }

        return traceId;
    }

    /**
     * 获取请求上下文
     */
    private RequestContext getRequestContext() {
        RequestContext context = new RequestContext();

        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

            if (requestAttributes instanceof ServletRequestAttributes) {
                ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
                HttpServletRequest request = servletRequestAttributes.getRequest();

                if (request != null) {
                    context.setUsername(extractUsername(request));
                    context.setIp(getClientIpAddress(request));
                    context.setRequestUrl(request.getRequestURI());
                    context.setRequestMethod(request.getMethod());
                    context.setBrowserInfo(request.getHeader("User-Agent"));

                    // 会话ID
                    try {
                        jakarta.servlet.http.HttpSession session = request.getSession(false);
                        if (session != null) {
                            context.setSessionId(session.getId());
                        }
                    } catch (Exception e) {
                        // Ignore
                    }

                    // 用户角色
                    String role = request.getHeader("userRole");
                    if (role == null || role.isEmpty()) {
                        try {
                            jakarta.servlet.http.HttpSession session = request.getSession(false);
                            if (session != null) {
                                Object sessionRole = session.getAttribute("userRole");
                                role = sessionRole != null ? sessionRole.toString() : null;
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    context.setUserRole(role);
                }
            } else {
                context.setUsername("system");
                context.setIp("127.0.0.1");
            }
        } catch (Exception e) {
            log.warn("获取审计请求上下文失败: {}", e.getMessage());
            context.setUsername("system");
            context.setIp("unknown");
        }

        return context;
    }

    /**
     * 从请求中提取用户名
     */
    private String extractUsername(HttpServletRequest request) {
        String username = request.getHeader("username");
        if (username == null || username.isEmpty()) {
            try {
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                if (session != null) {
                    Object sessionUser = session.getAttribute("username");
                    username = sessionUser != null ? sessionUser.toString() : "anonymous";
                } else {
                    username = "anonymous";
                }
            } catch (Exception e) {
                username = "anonymous";
            }
        }
        return username != null ? username : "anonymous";
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        String ip = null;
        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            if (stackTrace.length() > 4000) {
                return stackTrace.substring(0, 4000) + "\n... (堆栈已截断)";
            }

            return stackTrace;
        } catch (Exception e) {
            return "Failed to get stack trace: " + e.getMessage();
        }
    }

    /**
     * 从方法参数推断对象类型
     */
    private String inferObjectType(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            for (Object arg : args) {
                if (arg == null) continue;

                // 跳过基础类型
                if (arg instanceof String || arg instanceof Long || arg instanceof Integer ||
                    arg instanceof Boolean || arg instanceof Double) {
                    continue;
                }

                // 获取类名（去掉包名）
                String className = arg.getClass().getSimpleName();

                // 过滤掉一些不需要的类型
                if (!className.equals("Object") && !className.startsWith("Array") &&
                    !className.equals("ArrayList") && !className.equals("HashMap")) {
                    return className;
                }
            }
        } catch (Exception e) {
            // 推断失败不影响日志记录
        }

        return null;
    }

    /**
     * 请求上下文封装类
     */
    private static class RequestContext {
        private String username = "system";
        private String ip = "unknown";
        private String requestUrl;
        private String requestMethod;
        private String sessionId;
        private String userRole;
        private String browserInfo;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public String getRequestUrl() { return requestUrl; }
        public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

        public String getRequestMethod() { return requestMethod; }
        public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }

        public String getBrowserInfo() { return browserInfo; }
        public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }
    }
}
