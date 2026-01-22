package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rangeId; // 关联演练ID

    @Column(nullable = false)
    private String teamName; // 队伍名称（攻击队/防御队/个人）

    @Column(name = "team_type", length = 20)
    private String teamType; // 队伍类型：red(红队)/blue(蓝队)

    @Column(nullable = false)
    private String targetName; // 攻击目标

    @Lob
    @Column(nullable = false)
    private String description; // 攻击描述

    private String attackMethod; // 攻击工具/方法

    @Lob
    private String screenshots; // 漏洞截图路径(JSON格式存储多个文件)

    @Lob
    private String proofFiles; // 证明文件路径(JSON格式存储多个文件)

    @Column(nullable = false)
    private String status = "pending"; // pending/approved/rejected

    private String rejectReason; // 驳回理由

    @Column(nullable = false)
    private LocalDateTime submitTime = LocalDateTime.now();

    private LocalDateTime reviewTime; // 审批时间

    private String reviewerId; // 审批人ID

    @Lob
    @Column(name = "attack_report_json", columnDefinition = "TEXT")
    private String attackReportJson; // 攻击报告结构化数据（JSON格式）

    @Column(name = "recording_time_range")
    private String recordingTimeRange; // 录屏时间范围，格式："startTime,endTime"

    @Lob
    @Column(name = "related_recording_ids", columnDefinition = "TEXT")
    private String relatedRecordingIds; // 关联的录屏ID列表（JSON数组）

    // 打分系统相关字段
    @Column(name = "achievement_type", length = 50)
    private String achievementType; // 成果类型 (vulnerability_discovery/threat_detection/emergency_response/forensics)

    @Column(name = "base_score")
    private Integer baseScore; // 基础分值（根据类型预设）

    @Column(name = "final_score")
    private Integer finalScore; // 最终得分（裁判可在基础分上微调）

    @Lob
    @Column(name = "score_reason", columnDefinition = "TEXT")
    private String scoreReason; // 打分理由/说明

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRangeId() { return rangeId; }
    public void setRangeId(Long rangeId) { this.rangeId = rangeId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttackMethod() { return attackMethod; }
    public void setAttackMethod(String attackMethod) { this.attackMethod = attackMethod; }

    public String getScreenshots() { return screenshots; }
    public void setScreenshots(String screenshots) { this.screenshots = screenshots; }

    public String getProofFiles() { return proofFiles; }
    public void setProofFiles(String proofFiles) { this.proofFiles = proofFiles; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }

    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getAttackReportJson() { return attackReportJson; }
    public void setAttackReportJson(String attackReportJson) { this.attackReportJson = attackReportJson; }

    public String getRecordingTimeRange() { return recordingTimeRange; }
    public void setRecordingTimeRange(String recordingTimeRange) { this.recordingTimeRange = recordingTimeRange; }

    public String getRelatedRecordingIds() { return relatedRecordingIds; }
    public void setRelatedRecordingIds(String relatedRecordingIds) { this.relatedRecordingIds = relatedRecordingIds; }

    public String getAchievementType() { return achievementType; }
    public void setAchievementType(String achievementType) { this.achievementType = achievementType; }

    public Integer getBaseScore() { return baseScore; }
    public void setBaseScore(Integer baseScore) { this.baseScore = baseScore; }

    public Integer getFinalScore() { return finalScore; }
    public void setFinalScore(Integer finalScore) { this.finalScore = finalScore; }

    public String getScoreReason() { return scoreReason; }
    public void setScoreReason(String scoreReason) { this.scoreReason = scoreReason; }
}