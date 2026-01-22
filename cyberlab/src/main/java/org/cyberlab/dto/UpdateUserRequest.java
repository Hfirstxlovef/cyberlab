package org.cyberlab.dto;

public class UpdateUserRequest {
    private String username;
    private String role;
    private Boolean enabled;
    
    // 构造函数
    public UpdateUserRequest() {}
    
    // Getter和Setter
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
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}