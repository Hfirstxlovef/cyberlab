package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "member_role", length = 50)
    private String memberRole = "member"; // leader, member

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    // 构造函数
    public TeamMember() {
        this.joinedAt = LocalDateTime.now();
        this.memberRole = "member";
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

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    // 工具方法
    /**
     * 检查是否为队长
     */
    public boolean isLeader() {
        return "leader".equals(this.memberRole);
    }

    /**
     * 检查是否为普通成员
     */
    public boolean isMember() {
        return "member".equals(this.memberRole);
    }
}
