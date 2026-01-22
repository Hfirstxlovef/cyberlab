package org.cyberlab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.cyberlab.entity.SystemLog;
import org.cyberlab.entity.User;
import org.cyberlab.enums.BusinessModule;
import org.cyberlab.enums.OperationStatus;
import org.cyberlab.enums.OperationType;
import org.cyberlab.repository.SystemLogRepository;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.util.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider jwtProvider;

    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private SystemLogRepository systemLogRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();

        // 跳过这些路径的JWT验证，与SecurityConfig中的permitAll保持一致
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/api/range/") ||
               requestURI.startsWith("/api/drills/") ||
               requestURI.startsWith("/api/health") ||
               requestURI.startsWith("/api/assets/export") ||
               requestURI.startsWith("/api/admin/exceptions/") || // 允许前端异常报告(包括health接口)
               (requestURI.equals("/api/settings") && "GET".equals(request.getMethod())) || // 只跳过GET请求
               requestURI.equals("/") ||
               requestURI.startsWith("/assets/") ||
               requestURI.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain chain)
            throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);

            if (token != null && jwtProvider.validateToken(token)) {
                authenticateUser(token, request);
            } else if (token != null) {
                logger.warn("JWT验证失败，可能已过期或签名无效");
            }
        } catch (Exception e) {
            logger.error("JWT过滤器处理异常: {}", e.getMessage(), e);
            // 清除可能的错误认证状态
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    /**
     * 认证用户并设置安全上下文
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        try {
            String username = jwtProvider.getUsername(token);
            List<String> authorities = jwtProvider.getAuthorities(token);

            if (username == null || username.trim().isEmpty()) {
                logger.warn("JWT中用户名为空");
                return;
            }

            User user = userRepo.findByUsername(username).orElse(null);
            if (user != null && user.isEnabled()) {
                // 权限转换
                List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                        .filter(auth -> auth != null && !auth.trim().isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                grantedAuthorities
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 记录登录日志
                logUserAccess(username, request);
            } else {
                logger.warn("用户不存在或已被禁用：{}", username);
            }
        } catch (Exception e) {
            logger.error("用户认证过程异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录用户访问日志
     */
    private void logUserAccess(String username, HttpServletRequest request) {
        try {
            // 直接使用注入的Repository，避免使用SpringContext
            if (systemLogRepository != null) {
                String requestUri = request.getRequestURI();
                String requestMethod = request.getMethod();

                SystemLog log = new SystemLog();

                // ========== 基础字段 ==========
                log.setUsername(username);
                log.setOperation("系统访问");
                log.setDescription("JWT认证成功，访问路径: " + requestUri);
                log.setIp(getClientIpAddress(request));
                log.setTimestamp(LocalDateTime.now());

                // ========== HTTP请求字段 ==========
                log.setRequestUrl(requestUri);
                log.setRequestMethod(requestMethod);

                // ========== 日志分类字段 ==========
                log.setLogLevel("INFO");
                log.setLogCategory("SECURITY");

                // ========== 操作行为分类字段 ==========
                // 根据HTTP方法推断操作类型
                log.setOperationType(inferOperationTypeFromMethod(requestMethod));

                // 从URL推断业务模块
                log.setBusinessModule(inferBusinessModuleFromUrl(requestUri));

                // 认证成功，操作状态为成功
                log.setOperationStatus(OperationStatus.SUCCESS);

                systemLogRepository.save(log);
            } else {
                logger.warn("SystemLogRepository未注入，跳过日志记录");
            }
        } catch (Exception e) {
            // 日志记录失败不应该影响主要业务流程
            logger.warn("日志记录失败：{}", e.getMessage());
        }
    }

    /**
     * 根据HTTP方法推断操作类型
     */
    private OperationType inferOperationTypeFromMethod(String method) {
        if (method == null) {
            return OperationType.EXECUTE;
        }

        switch (method.toUpperCase()) {
            case "GET":
                return OperationType.READ;
            case "POST":
                return OperationType.CREATE;
            case "PUT":
            case "PATCH":
                return OperationType.UPDATE;
            case "DELETE":
                return OperationType.DELETE;
            default:
                return OperationType.EXECUTE;
        }
    }

    /**
     * 从请求URL推断业务模块
     */
    private BusinessModule inferBusinessModuleFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return BusinessModule.OTHER;
        }

        String lowerUrl = url.toLowerCase();

        // 按URL路径匹配业务模块
        if (lowerUrl.contains("/api/users") || lowerUrl.contains("/api/auth")) {
            return BusinessModule.USER;
        }
        if (lowerUrl.contains("/api/containers") || lowerUrl.contains("/api/drill-containers")) {
            return BusinessModule.CONTAINER;
        }
        if (lowerUrl.contains("/api/assets")) {
            return BusinessModule.ASSET;
        }
        if (lowerUrl.contains("/api/achievements")) {
            return BusinessModule.ACHIEVEMENT;
        }
        if (lowerUrl.contains("/api/scenarios") || lowerUrl.contains("/api/templates")) {
            return BusinessModule.SCENARIO;
        }
        if (lowerUrl.contains("/api/hosts") || lowerUrl.contains("/api/host-nodes")) {
            return BusinessModule.HOST_NODE;
        }
        if (lowerUrl.contains("/api/logs") || lowerUrl.contains("/api/audit")) {
            return BusinessModule.LOG;
        }
        if (lowerUrl.contains("/api/settings") || lowerUrl.contains("/api/system")) {
            return BusinessModule.SYSTEM_CONFIG;
        }
        if (lowerUrl.contains("/api/big-screen") || lowerUrl.contains("/api/dashboard")) {
            return BusinessModule.BIG_SCREEN;
        }
        if (lowerUrl.contains("/api/teams")) {
            return BusinessModule.TEAM;
        }
        if (lowerUrl.contains("/api/permissions")) {
            return BusinessModule.PERMISSION;
        }
        if (lowerUrl.contains("/api/roles")) {
            return BusinessModule.ROLE;
        }
        if (lowerUrl.contains("/api/files") || lowerUrl.contains("/api/uploads")) {
            return BusinessModule.FILE;
        }
        if (lowerUrl.contains("/api/monitor")) {
            return BusinessModule.MONITOR;
        }
        if (lowerUrl.contains("/api/drill")) {
            return BusinessModule.DRILL;
        }
        if (lowerUrl.contains("/api/recording") || lowerUrl.contains("/api/screen-recording")) {
            return BusinessModule.RECORDING;
        }
        if (lowerUrl.contains("/api/topology") || lowerUrl.contains("/api/network")) {
            return BusinessModule.TOPOLOGY;
        }
        if (lowerUrl.contains("/api/backup")) {
            return BusinessModule.BACKUP;
        }
        if (lowerUrl.contains("/api/notifications") || lowerUrl.contains("/api/messages")) {
            return BusinessModule.NOTIFICATION;
        }
        if (lowerUrl.contains("/api/range")) {
            return BusinessModule.DRILL;  // 靶场管理归类为演练管理
        }

        return BusinessModule.OTHER;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }

    private String getTokenFromRequest(@NonNull HttpServletRequest request) {
        // 首先尝试从Authorization头获取token
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ") && bearer.length() > 7) {
            String token = bearer.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        // 如果Authorization头中没有token，尝试从Cookie获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.trim().isEmpty()) {
                        return token.trim();
                    }
                }
            }
        }

        return null;
    }
}