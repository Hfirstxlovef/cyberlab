package org.cyberlab.dto;

/**
 * 登录成功后的响应数据结构，包含 token、refreshToken、用户名、角色 和 用户ID
 */
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String role;
    private Long userId;

    public AuthResponse() {
        // 无参构造函数
    }

    public AuthResponse(String token, String refreshToken, String username, String role, Long userId) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }

    // 向后兼容的构造函数（不包含 userId）
    public AuthResponse(String token, String refreshToken, String username, String role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.role = role;
    }

    // 向后兼容的构造函数
    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}