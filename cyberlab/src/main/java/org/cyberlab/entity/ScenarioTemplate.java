package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

@Entity
@Table(name = "scenario_templates")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ScenarioTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "scenario_type", nullable = false)
    private String scenarioType; // attack_defense, blue_team, red_team, mixed
    
    @Column(name = "difficulty_level", nullable = false)
    private String difficultyLevel; // beginner, intermediate, advanced
    
    @Column(name = "estimated_duration")
    private Integer estimatedDuration; // 预计演练时间（分钟）
    
    @Column(name = "max_participants")
    private Integer maxParticipants; // 最大参与人数
    
    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    private String learningObjectives; // 学习目标
    
    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites; // 前置要求
    
    @Column(name = "asset_config", columnDefinition = "TEXT")
    private String assetConfig; // JSON格式：包含资产ID和配置的数组
    
    @Column(name = "network_topology", columnDefinition = "TEXT")
    private String networkTopology; // JSON格式：网络拓扑配置
    
    @Column(name = "deployment_order", columnDefinition = "TEXT")
    private String deploymentOrder; // JSON格式：部署顺序配置
    
    @Column(name = "exercise_script", columnDefinition = "TEXT")
    private String exerciseScript; // 演练脚本和步骤
    
    @Column(name = "evaluation_criteria", columnDefinition = "TEXT")
    private String evaluationCriteria; // 评估标准
    
    @Column(name = "success_metrics", columnDefinition = "TEXT")
    private String successMetrics; // 成功指标
    
    @Column(name = "instructor_notes", columnDefinition = "TEXT")
    private String instructorNotes; // 教师备注
    
    @Column(name = "student_guidelines", columnDefinition = "TEXT")
    private String studentGuidelines; // 学生指导
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON格式：标签数组
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic; // 是否公开模板
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount; // 使用次数统计
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    // 构造函数
    public ScenarioTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.isPublic = true;
        this.usageCount = 0;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }
    
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    
    public String getLearningObjectives() { return learningObjectives; }
    public void setLearningObjectives(String learningObjectives) { this.learningObjectives = learningObjectives; }
    
    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }
    
    public String getAssetConfig() { return assetConfig; }
    public void setAssetConfig(String assetConfig) { this.assetConfig = assetConfig; }
    
    public String getNetworkTopology() { return networkTopology; }
    public void setNetworkTopology(String networkTopology) { this.networkTopology = networkTopology; }
    
    public String getDeploymentOrder() { return deploymentOrder; }
    public void setDeploymentOrder(String deploymentOrder) { this.deploymentOrder = deploymentOrder; }
    
    public String getExerciseScript() { return exerciseScript; }
    public void setExerciseScript(String exerciseScript) { this.exerciseScript = exerciseScript; }
    
    public String getEvaluationCriteria() { return evaluationCriteria; }
    public void setEvaluationCriteria(String evaluationCriteria) { this.evaluationCriteria = evaluationCriteria; }
    
    public String getSuccessMetrics() { return successMetrics; }
    public void setSuccessMetrics(String successMetrics) { this.successMetrics = successMetrics; }
    
    public String getInstructorNotes() { return instructorNotes; }
    public void setInstructorNotes(String instructorNotes) { this.instructorNotes = instructorNotes; }
    
    public String getStudentGuidelines() { return studentGuidelines; }
    public void setStudentGuidelines(String studentGuidelines) { this.studentGuidelines = studentGuidelines; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // 增加使用次数
    public void incrementUsageCount() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }
}