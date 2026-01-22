package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cyber_range")
public class CyberRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String status; // running / paused / deleted

    @Lob
    private String topologyConfig;

    @Lob
    private String vulnerabilityConfig;

    // 演练基本信息字段
    @Column(columnDefinition = "TEXT")
    private String description; // 演练描述
    
    private String drillType; // 演练类型：attack_defense, blue_team, red_team, mixed
    
    private String difficultyLevel; // 难度等级：beginner, intermediate, advanced
    
    private LocalDateTime startTime; // 计划开始时间
    
    private LocalDateTime endTime; // 计划结束时间
    
    private Integer maxParticipants; // 最大参与人数
    
    private Long creatorId; // 创建者ID
    
    private String topologyProjectId; // 关联的拓扑项目ID
    
    private LocalDateTime updatedAt; // 更新时间

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTopologyConfig() { return topologyConfig; }
    public void setTopologyConfig(String topologyConfig) { this.topologyConfig = topologyConfig; }

    public String getVulnerabilityConfig() { return vulnerabilityConfig; }
    public void setVulnerabilityConfig(String vulnerabilityConfig) { this.vulnerabilityConfig = vulnerabilityConfig; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // 新增字段的 Getter 和 Setter 方法
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDrillType() { return drillType; }
    public void setDrillType(String drillType) { this.drillType = drillType; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getTopologyProjectId() { return topologyProjectId; }
    public void setTopologyProjectId(String topologyProjectId) { this.topologyProjectId = topologyProjectId; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}