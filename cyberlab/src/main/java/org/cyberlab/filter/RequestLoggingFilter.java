package org.cyberlab.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

/**
 * 请求日志过滤器 - 记录所有进入后端的HTTP请求
 * 用于诊断请求是否到达后端、是否被Spring Security拦截等问题
 */
@Component
@Order(1) // 最高优先级，确保最先执行
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            long startTime = System.currentTimeMillis();

            // 记录请求详情
            logger.info("========== 收到HTTP请求 ==========");
            logger.info("请求方法: {}", httpRequest.getMethod());
            logger.info("请求URI: {}", httpRequest.getRequestURI());
            logger.info("请求URL: {}", httpRequest.getRequestURL());
            logger.info("查询参数: {}", httpRequest.getQueryString());
            logger.info("远程地址: {}", httpRequest.getRemoteAddr());
            logger.info("Content-Type: {}", httpRequest.getContentType());

            // 记录所有请求头（特别关注Authorization）
            logger.info("--- 请求头 ---");
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = httpRequest.getHeader(headerName);

                // 脱敏Authorization头（只显示前20个字符）
                if ("authorization".equalsIgnoreCase(headerName) && headerValue != null && headerValue.length() > 20) {
                    headerValue = headerValue.substring(0, 20) + "... (已截断)";
                }

                logger.info("{}: {}", headerName, headerValue);
            }

            try {
                // 继续过滤器链
                chain.doFilter(request, response);

                long duration = System.currentTimeMillis() - startTime;

                // 记录响应信息
                logger.info("========== 请求处理完成 ==========");
                logger.info("响应状态码: {}", httpResponse.getStatus());
                logger.info("处理耗时: {} ms", duration);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("========== 请求处理异常 ==========");
                logger.error("异常类型: {}", e.getClass().getName());
                logger.error("异常信息: {}", e.getMessage());
                logger.error("处理耗时: {} ms", duration);
                logger.error("异常堆栈:", e);
                throw e;
            }

        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RequestLoggingFilter 初始化完成");
    }

    @Override
    public void destroy() {
        logger.info("RequestLoggingFilter 销毁");
    }
}
