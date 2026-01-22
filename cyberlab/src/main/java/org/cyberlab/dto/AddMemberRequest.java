package org.cyberlab.dto;

public class AddMemberRequest {
    private Long userId;

    // 构造函数
    public AddMemberRequest() {}

    public AddMemberRequest(Long userId) {
        this.userId = userId;
    }

    // Getter和Setter
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
