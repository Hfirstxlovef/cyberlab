package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 团队得分统计实体
 * 记录团队在指定演练中的总分、排名等信息
 */
@Entity
@Table(name = "team_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"team_name", "range_id"}))
public class TeamScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id")
    private Long teamId; // 团队ID（关联teams表）

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName; // 团队名称

    @Column(name = "team_type", nullable = false, length = 20)
    private String teamType; // red/blue

    @Column(name = "range_id", nullable = false)
    private Long rangeId; // 演练ID（不同演练分开统计）

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0; // 总分

    @Column(name = "approved_count", nullable = false)
    private Integer approvedCount = 0; // 通过成果数量

    @Column(name = "ranking")
    private Integer ranking; // 排名（同类型队伍中）

    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime; // 最后更新时间

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 构造函数
    public TeamScore() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
        this.totalScore = 0;
        this.approvedCount = 0;
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

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public Long getRangeId() {
        return rangeId;
    }

    public void setRangeId(Long rangeId) {
        this.rangeId = rangeId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Integer approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 工具方法
    /**
     * 添加分数
     */
    public void addScore(int score) {
        this.totalScore += score;
        this.approvedCount++;
        this.lastUpdateTime = LocalDateTime.now();
    }

    /**
     * 更新时间戳
     */
    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTime = LocalDateTime.now();
    }
}
