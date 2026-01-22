package org.cyberlab.dto;

import org.cyberlab.entity.User;

/**
 * 用户基本信息DTO - 不包含敏感数据（如密码）
 * 用于公共接口返回用户信息，供红队/蓝队用户查看其他用户基本信息
 */
public class UserBasicDTO {
    private Long id;
    private String username;
    private String role;
    private Long teamId;
    private boolean enabled;

    // 默认构造函数
    public UserBasicDTO() {
    }

    // 从 User 实体转换的构造函数
    public UserBasicDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.teamId = user.getTeamId();
        this.enabled = user.isEnabled();
    }

    // 完整构造函数
    public UserBasicDTO(Long id, String username, String role, Long teamId, boolean enabled) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.teamId = teamId;
        this.enabled = enabled;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
