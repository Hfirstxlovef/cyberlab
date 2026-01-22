package org.cyberlab.dto;

public class ReviewApplicationRequest {
    private String reason; // 拒绝理由（批准时可为空）

    // Constructors
    public ReviewApplicationRequest() {
    }

    public ReviewApplicationRequest(String reason) {
        this.reason = reason;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
