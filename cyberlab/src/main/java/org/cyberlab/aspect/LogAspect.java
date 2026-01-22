package org.cyberlab.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.cyberlab.annotation.LogOperation;
import org.cyberlab.entity.SystemLog;
import org.cyberlab.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * 增强版日志切面
 * 功能：
 * 1. 自动记录方法执行时间
 * 2. 支持分布式追踪（traceId）
 * 3. 捕获HTTP请求上下文（URL、方法、状态码）
 * 4. 记录异常堆栈
 * 5. 支持日志级别和分类
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SystemLogRepository logRepository;

    @Pointcut("@annotation(org.cyberlab.annotation.LogOperation)")
    public void logPointcut() {}

    /**
     * 环绕通知 - 替代 @AfterReturning 和 @AfterThrowing
     * 优势：可以捕获方法执行时间、返回值、异常信息
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 1. 生成或获取 traceId
        String traceId = getOrCreateTraceId();

        // 2. 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        LogOperation logAnnotation = signature.getMethod().getAnnotation(LogOperation.class);

        // 3. 获取请求上下文
        EnhancedRequestInfo requestInfo = getEnhancedRequestInfo();

        Object result = null;
        Throwable exception = null;
        Integer responseStatus = null;

        try {
            // 4. 执行目标方法
            result = joinPoint.proceed();
            responseStatus = 200; // 方法执行成功，默认200
            return result;
        } catch (Throwable ex) {
            exception = ex;
            responseStatus = 500; // 方法执行失败，默认500
            throw ex; // 重新抛出异常，不影响原有业务逻辑
        } finally {
            try {
                // 5. 计算执行时间
                long executionTime = System.currentTimeMillis() - startTime;

                // 6. 保存增强日志
                saveEnhancedLog(joinPoint, logAnnotation, requestInfo, traceId,
                               executionTime, responseStatus, exception);
            } catch (Exception e) {
                // 日志记录失败不应影响业务
                log.error("保存日志失败: {}", e.getMessage(), e);
            } finally {
                // 7. 清理 MDC（避免内存泄漏）
                MDC.remove(TRACE_ID_KEY);
            }
        }
    }

    /**
     * 保存增强日志
     */
    private void saveEnhancedLog(ProceedingJoinPoint joinPoint, LogOperation logAnnotation,
                                 EnhancedRequestInfo requestInfo, String traceId,
                                 long executionTime, Integer responseStatus, Throwable exception) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
            String params = formatMethodParams(joinPoint.getArgs());

            SystemLog systemLog = new SystemLog();

            // ========== 原有字段 ==========
            systemLog.setUsername(requestInfo.getUsername());
            systemLog.setIp(requestInfo.getIp());
            systemLog.setOperation(logAnnotation != null ? logAnnotation.value() : "未知操作");
            systemLog.setTimestamp(LocalDateTime.now());

            // Description 字段
            StringBuilder description = new StringBuilder();
            description.append("方法: ").append(methodName);
            if (params.length() > 0) {
                description.append("\n参数: ").append(params);
            }
            if (exception != null) {
                description.append("\n异常: ").append(exception.getMessage());
            }
            systemLog.setDescription(description.toString());

            // ========== 新增字段 ==========

            // 日志级别
            if (exception != null) {
                systemLog.setLogLevel("ERROR");
            } else if (executionTime > 3000) { // 执行时间超过3秒警告
                systemLog.setLogLevel("WARN");
            } else {
                systemLog.setLogLevel("INFO");
            }

            // 日志分类（默认SYSTEM，可以通过注解扩展）
            systemLog.setLogCategory("SYSTEM");

            // 分布式追踪
            systemLog.setTraceId(traceId);
            systemLog.setSessionId(requestInfo.getSessionId());

            // 用户角色
            systemLog.setUserRole(requestInfo.getUserRole());

            // HTTP请求上下文
            systemLog.setRequestUrl(requestInfo.getRequestUrl());
            systemLog.setRequestMethod(requestInfo.getRequestMethod());
            systemLog.setResponseStatus(responseStatus);

            // 性能指标
            systemLog.setExecutionTime(executionTime);

            // 异常信息
            if (exception != null) {
                systemLog.setExceptionType(exception.getClass().getName());
                systemLog.setErrorStack(getStackTrace(exception));
            }

            // 业务ID（从方法参数中提取，如果有的话）
            systemLog.setBusinessId(extractBusinessId(joinPoint.getArgs()));

            // 浏览器信息（前端请求）
            systemLog.setBrowserInfo(requestInfo.getBrowserInfo());

            // ========== 操作行为分类字段 ==========

            if (logAnnotation != null) {
                // 操作类型（从注解读取）
                systemLog.setOperationType(logAnnotation.operationType());

                // 业务模块（从注解读取）
                systemLog.setBusinessModule(logAnnotation.module());

                // 操作对象类型（从注解读取，如果为空则自动推断）
                String objectType = logAnnotation.objectType();
                if (objectType == null || objectType.isEmpty()) {
                    objectType = inferObjectType(joinPoint.getArgs());
                }
                systemLog.setObjectType(objectType);
            } else {
                // 没有注解时的降级处理：从operation字段和URL推断
                systemLog.setOperationType(inferOperationType(systemLog.getOperation()));
                systemLog.setBusinessModule(inferBusinessModule(systemLog.getOperation(), systemLog.getRequestUrl()));
            }

            // 操作状态（根据异常自动判断）
            systemLog.setOperationStatus(
                exception == null ?
                org.cyberlab.enums.OperationStatus.SUCCESS :
                org.cyberlab.enums.OperationStatus.FAILED
            );

            // 保存日志
            logRepository.save(systemLog);

            // 如果执行时间过长，输出警告日志
            if (executionTime > 3000) {
                log.warn("慢操作检测 [{}] 执行耗时: {}ms, TraceId: {}",
                        methodName, executionTime, traceId);
            }

        } catch (Exception e) {
            log.error("保存增强日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 优化的参数格式化方法，避免过度创建字符串对象
     */
    private String formatMethodParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                String str = (String) arg;
                // 限制字符串长度，避免日志过大
                if (str.length() > 200) {
                    sb.append(str.substring(0, 200)).append("...(截断)");
                } else {
                    sb.append("\"").append(str).append("\"");
                }
            } else {
                // 其他对象类型，限制toString长度
                String objStr = arg.toString();
                if (objStr.length() > 100) {
                    sb.append(objStr.substring(0, 100)).append("...(截断)");
                } else {
                    sb.append(objStr);
                }
            }
        }
        
        sb.append("]");
        return sb.toString();
    }

    /**
     * 生成或获取 TraceId（分布式追踪）
     */
    private String getOrCreateTraceId() {
        // 1. 优先从 MDC 获取（同一请求链路复用）
        String traceId = MDC.get(TRACE_ID_KEY);

        if (traceId == null || traceId.isEmpty()) {
            // 2. 尝试从 HTTP Header 获取（分布式系统传递）
            try {
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof ServletRequestAttributes) {
                    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                    traceId = request.getHeader("X-Trace-Id");
                }
            } catch (Exception e) {
                // Ignore
            }

            // 3. 都没有则生成新的 TraceId
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            // 4. 存入 MDC（便于日志框架自动打印）
            MDC.put(TRACE_ID_KEY, traceId);
        }

        return traceId;
    }

    /**
     * 获取增强的请求信息
     */
    private EnhancedRequestInfo getEnhancedRequestInfo() {
        EnhancedRequestInfo info = new EnhancedRequestInfo();

        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

            if (requestAttributes instanceof ServletRequestAttributes) {
                ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
                HttpServletRequest request = servletRequestAttributes.getRequest();

                if (request != null) {
                    // 基础信息
                    info.setUsername(extractUsername(request));
                    info.setIp(getClientIpAddress(request));

                    // HTTP上下文
                    info.setRequestUrl(request.getRequestURI());
                    info.setRequestMethod(request.getMethod());

                    // 会话ID
                    info.setSessionId(extractSessionId(request));

                    // 用户角色
                    info.setUserRole(extractUserRole(request));

                    // 浏览器信息
                    info.setBrowserInfo(request.getHeader("User-Agent"));
                }
            } else {
                // 非HTTP请求（如定时任务、MQ消息），使用默认值
                info.setUsername("system");
                info.setIp("127.0.0.1");
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败: {}", e.getMessage());
            // 使用默认值
            info.setUsername("system");
            info.setIp("unknown");
        }

        return info;
    }

    /**
     * 提取会话ID
     */
    private String extractSessionId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            return session != null ? session.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取用户角色
     */
    private String extractUserRole(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 1. 尝试从 Header 获取
        String role = request.getHeader("userRole");

        // 2. 尝试从 Session 获取
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

        return role;
    }

    /**
     * 获取异常堆栈信息
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

            // 限制长度（数据库 TEXT 类型限制）
            if (stackTrace.length() > 4000) {
                return stackTrace.substring(0, 4000) + "\n... (堆栈信息已截断)";
            }

            return stackTrace;
        } catch (Exception e) {
            return "Failed to get stack trace: " + e.getMessage();
        }
    }

    /**
     * 从方法参数中提取业务ID
     * 支持常见的业务对象（含id、containerId、userId等字段）
     */
    private String extractBusinessId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            for (Object arg : args) {
                if (arg == null) continue;

                // 1. 如果参数本身是 Long/String 类型的 ID
                if (arg instanceof Long || arg instanceof String) {
                    return arg.toString();
                }

                // 2. 如果参数是业务对象，尝试反射获取 id 字段
                try {
                    java.lang.reflect.Field idField = arg.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object id = idField.get(arg);
                    if (id != null) {
                        return id.toString();
                    }
                } catch (NoSuchFieldException e) {
                    // 尝试其他字段
                    try {
                        java.lang.reflect.Field containerIdField = arg.getClass().getDeclaredField("containerId");
                        containerIdField.setAccessible(true);
                        Object containerId = containerIdField.get(arg);
                        if (containerId != null) {
                            return containerId.toString();
                        }
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            // 提取失败不影响日志记录
        }

        return null;
    }
    
    /**
     * 从请求中提取用户名
     */
    private String extractUsername(HttpServletRequest request) {
        if (request == null) {
            return "anonymous";
        }
        
        String username = request.getHeader("username");
        if (username == null || username.isEmpty()) {
            try {
                // 安全地获取session，避免创建新session
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
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String ip = null;
        
        try {
            // 按优先级获取IP地址
            String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP", 
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
            };
            
            for (String header : headers) {
                ip = request.getHeader(header);
                if (isValidIp(ip)) {
                    break;
                }
            }
            
            // 如果代理头都没有有效IP，使用远程地址
            if (!isValidIp(ip)) {
                ip = request.getRemoteAddr();
            }
            
            // 处理多个IP的情况，取第一个有效IP
            if (ip != null && ip.contains(",")) {
                String[] ips = ip.split(",");
                for (String singleIp : ips) {
                    String trimmedIp = singleIp.trim();
                    if (isValidIp(trimmedIp)) {
                        ip = trimmedIp;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 获取客户端IP地址异常，使用默认值
        }
        
        return ip != null ? ip : "unknown";
    }
    
    /**
     * 验证IP地址是否有效
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * 从operation字段推断操作类型
     */
    private org.cyberlab.enums.OperationType inferOperationType(String operation) {
        if (operation == null || operation.isEmpty()) {
            return org.cyberlab.enums.OperationType.EXECUTE;
        }

        // 使用枚举的fromDescription方法
        org.cyberlab.enums.OperationType type = org.cyberlab.enums.OperationType.fromDescription(operation);
        return type != null ? type : org.cyberlab.enums.OperationType.EXECUTE;
    }

    /**
     * 从operation字段和URL推断业务模块
     */
    private org.cyberlab.enums.BusinessModule inferBusinessModule(String operation, String requestUrl) {
        // 优先从URL推断（更准确）
        if (requestUrl != null && !requestUrl.isEmpty()) {
            org.cyberlab.enums.BusinessModule moduleFromUrl = inferModuleFromUrl(requestUrl);
            if (moduleFromUrl != org.cyberlab.enums.BusinessModule.OTHER) {
                return moduleFromUrl;
            }
        }

        // 其次从operation推断
        if (operation != null && !operation.isEmpty()) {
            return org.cyberlab.enums.BusinessModule.fromDescription(operation);
        }

        return org.cyberlab.enums.BusinessModule.OTHER;
    }

    /**
     * 从请求URL推断业务模块
     */
    private org.cyberlab.enums.BusinessModule inferModuleFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return org.cyberlab.enums.BusinessModule.OTHER;
        }

        String lowerUrl = url.toLowerCase();

        // 按URL路径匹配业务模块
        if (lowerUrl.contains("/api/users") || lowerUrl.contains("/api/auth")) {
            return org.cyberlab.enums.BusinessModule.USER;
        }
        if (lowerUrl.contains("/api/containers") || lowerUrl.contains("/api/drill-containers")) {
            return org.cyberlab.enums.BusinessModule.CONTAINER;
        }
        if (lowerUrl.contains("/api/assets")) {
            return org.cyberlab.enums.BusinessModule.ASSET;
        }
        if (lowerUrl.contains("/api/achievements")) {
            return org.cyberlab.enums.BusinessModule.ACHIEVEMENT;
        }
        if (lowerUrl.contains("/api/scenarios") || lowerUrl.contains("/api/templates")) {
            return org.cyberlab.enums.BusinessModule.SCENARIO;
        }
        if (lowerUrl.contains("/api/hosts") || lowerUrl.contains("/api/host-nodes")) {
            return org.cyberlab.enums.BusinessModule.HOST_NODE;
        }
        if (lowerUrl.contains("/api/logs") || lowerUrl.contains("/api/audit")) {
            return org.cyberlab.enums.BusinessModule.LOG;
        }
        if (lowerUrl.contains("/api/settings") || lowerUrl.contains("/api/system")) {
            return org.cyberlab.enums.BusinessModule.SYSTEM_CONFIG;
        }
        if (lowerUrl.contains("/api/big-screen") || lowerUrl.contains("/api/dashboard")) {
            return org.cyberlab.enums.BusinessModule.BIG_SCREEN;
        }
        if (lowerUrl.contains("/api/teams")) {
            return org.cyberlab.enums.BusinessModule.TEAM;
        }
        if (lowerUrl.contains("/api/permissions")) {
            return org.cyberlab.enums.BusinessModule.PERMISSION;
        }
        if (lowerUrl.contains("/api/roles")) {
            return org.cyberlab.enums.BusinessModule.ROLE;
        }
        if (lowerUrl.contains("/api/files") || lowerUrl.contains("/api/uploads")) {
            return org.cyberlab.enums.BusinessModule.FILE;
        }
        if (lowerUrl.contains("/api/monitor")) {
            return org.cyberlab.enums.BusinessModule.MONITOR;
        }
        if (lowerUrl.contains("/api/drill")) {
            return org.cyberlab.enums.BusinessModule.DRILL;
        }
        if (lowerUrl.contains("/api/recording") || lowerUrl.contains("/api/screen-recording")) {
            return org.cyberlab.enums.BusinessModule.RECORDING;
        }
        if (lowerUrl.contains("/api/topology") || lowerUrl.contains("/api/network")) {
            return org.cyberlab.enums.BusinessModule.TOPOLOGY;
        }
        if (lowerUrl.contains("/api/backup")) {
            return org.cyberlab.enums.BusinessModule.BACKUP;
        }
        if (lowerUrl.contains("/api/notifications") || lowerUrl.contains("/api/messages")) {
            return org.cyberlab.enums.BusinessModule.NOTIFICATION;
        }

        return org.cyberlab.enums.BusinessModule.OTHER;
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
     * 增强版请求信息封装类
     */
    private static class EnhancedRequestInfo {
        private String username = "system";
        private String ip = "unknown";
        private String requestUrl;
        private String requestMethod;
        private String sessionId;
        private String userRole;
        private String browserInfo;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username != null ? username : "system";
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip != null ? ip : "unknown";
        }

        public String getRequestUrl() {
            return requestUrl;
        }

        public void setRequestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
        }

        public String getRequestMethod() {
            return requestMethod;
        }

        public void setRequestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getUserRole() {
            return userRole;
        }

        public void setUserRole(String userRole) {
            this.userRole = userRole;
        }

        public String getBrowserInfo() {
            return browserInfo;
        }

        public void setBrowserInfo(String browserInfo) {
            this.browserInfo = browserInfo;
        }
    }
}