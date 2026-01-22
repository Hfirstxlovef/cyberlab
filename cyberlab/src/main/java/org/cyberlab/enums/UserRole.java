package org.cyberlab.enums;

public enum UserRole {
    RED("red", "红队用户"),
    BLUE("blue", "蓝队用户"), 
    JUDGE("judge", "裁判用户"),
    ADMIN("admin", "管理员用户");
    
    private final String code;
    private final String description;
    
    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role code: " + code);
    }
}