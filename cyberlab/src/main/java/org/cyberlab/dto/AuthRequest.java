package org.cyberlab.dto;

/**
 * 登录请求对象，封装前端传来的用户名和密码
 */
public class AuthRequest {

    private String username;
    private String password;

    public AuthRequest() {
        // 无参构造函数，确保 Spring Boot 可以正常反序列化 JSON 请求体
    }

    /**
     * 获取用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     */
    public void setPassword(String password) {
        this.password = password;
    }
}