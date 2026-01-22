package org.cyberlab.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private HttpServletRequest request;

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "认证失败");
        error.put("message", "您的登录状态已过期或无效，请重新登录");
        error.put("code", "AUTHENTICATION_FAILED");
        error.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "权限不足");
        
        // 根据请求路径提供更准确的错误信息
        String requestPath = request.getRequestURI();
        String message;
        String[] requiredRoles;
        
        if (requestPath.contains("/settings")) {
            message = "系统设置功能仅限管理员使用，请联系管理员获取权限";
            requiredRoles = new String[]{"admin"};
        } else if (requestPath.contains("/host-nodes") || requestPath.contains("/containers")) {
            message = "主机节点和容器管理功能仅限管理员和蓝队用户使用";
            requiredRoles = new String[]{"admin", "blue"};
        } else {
            message = "您没有权限访问此功能";
            requiredRoles = new String[]{"admin"}; // 默认需要管理员权限
        }
        
        error.put("message", message);
        error.put("code", "ACCESS_DENIED");
        error.put("timestamp", System.currentTimeMillis());
        error.put("requiredRoles", requiredRoles);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "服务器内部错误");
        error.put("message", "系统出现异常，请稍后重试或联系管理员");
        error.put("code", "INTERNAL_SERVER_ERROR");
        error.put("timestamp", System.currentTimeMillis());

        logger.error("服务器内部错误", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}