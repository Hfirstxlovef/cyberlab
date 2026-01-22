package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_applications")
public class TeamApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String status = "pending"; // pending, approved, rejected

    @Column(columnDefinition = "TEXT")
    private String message; // 申请留言

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason; // 拒绝理由

    @Column(name = "reviewed_by")
    private Long reviewedBy; // 审批人用户ID

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt; // 审批时间

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造函数
    public TeamApplication() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "pending";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 工具方法
    /**
     * 更新时间戳
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查申请是否待处理
     */
    public boolean isPending() {
        return "pending".equals(this.status);
    }

    /**
     * 检查申请是否已批准
     */
    public boolean isApproved() {
        return "approved".equals(this.status);
    }

    /**
     * 检查申请是否已拒绝
     */
    public boolean isRejected() {
        return "rejected".equals(this.status);
    }

    /**
     * 批准申请
     */
    public void approve(Long reviewerId) {
        this.status = "approved";
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = null;
    }

    /**
     * 拒绝申请
     */
    public void reject(Long reviewerId, String reason) {
        this.status = "rejected";
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = reason;
    }
}
