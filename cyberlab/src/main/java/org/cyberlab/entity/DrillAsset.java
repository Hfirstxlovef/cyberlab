package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drill_assets")
public class DrillAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "category", nullable = false)
    private String category; // web_app, server, database, network_device, vulnerability_tool
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "docker_image", nullable = false)
    private String dockerImage;
    
    @Column(name = "default_port")
    private Integer defaultPort;
    
    @Column(name = "exposed_ports", columnDefinition = "TEXT")
    private String exposedPorts; // JSON格式存储多个端口配置
    
    @Column(name = "environment_vars", columnDefinition = "TEXT")
    private String environmentVars; // JSON格式存储环境变量
    
    @Column(name = "volume_mounts", columnDefinition = "TEXT")
    private String volumeMounts; // JSON格式存储卷挂载配置
    
    @Column(name = "network_config", columnDefinition = "TEXT")
    private String networkConfig; // JSON格式存储网络配置
    
    @Column(name = "security_config", columnDefinition = "TEXT")
    private String securityConfig; // JSON格式存储安全配置
    
    @Column(name = "resource_limits", columnDefinition = "TEXT")
    private String resourceLimits; // JSON格式存储资源限制（CPU、内存等）
    
    @Column(name = "vulnerability_type")
    private String vulnerabilityType; // sql_injection, xss, command_injection, privilege_escalation
    
    @Column(name = "difficulty_level", nullable = false)
    private String difficultyLevel; // beginner, intermediate, advanced
    
    @Column(name = "attack_vector")
    private String attackVector; // network, local, adjacent_network, physical
    
    @Column(name = "team_visibility", nullable = false)
    private String teamVisibility; // red, blue, both
    
    @Column(name = "is_target", nullable = false)
    private Boolean isTarget; // 是否为攻击目标
    
    @Column(name = "setup_instructions", columnDefinition = "TEXT")
    private String setupInstructions; // 部署说明
    
    @Column(name = "exercise_instructions", columnDefinition = "TEXT")
    private String exerciseInstructions; // 演练指导
    
    @Column(name = "solution_hints", columnDefinition = "TEXT")
    private String solutionHints; // 解决方案提示
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON格式存储标签
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 构造函数
    public DrillAsset() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.isTarget = false;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDockerImage() { return dockerImage; }
    public void setDockerImage(String dockerImage) { this.dockerImage = dockerImage; }
    
    public Integer getDefaultPort() { return defaultPort; }
    public void setDefaultPort(Integer defaultPort) { this.defaultPort = defaultPort; }
    
    public String getExposedPorts() { return exposedPorts; }
    public void setExposedPorts(String exposedPorts) { this.exposedPorts = exposedPorts; }
    
    public String getEnvironmentVars() { return environmentVars; }
    public void setEnvironmentVars(String environmentVars) { this.environmentVars = environmentVars; }
    
    public String getVolumeMounts() { return volumeMounts; }
    public void setVolumeMounts(String volumeMounts) { this.volumeMounts = volumeMounts; }
    
    public String getNetworkConfig() { return networkConfig; }
    public void setNetworkConfig(String networkConfig) { this.networkConfig = networkConfig; }
    
    public String getSecurityConfig() { return securityConfig; }
    public void setSecurityConfig(String securityConfig) { this.securityConfig = securityConfig; }
    
    public String getResourceLimits() { return resourceLimits; }
    public void setResourceLimits(String resourceLimits) { this.resourceLimits = resourceLimits; }
    
    public String getVulnerabilityType() { return vulnerabilityType; }
    public void setVulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; }
    
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public String getAttackVector() { return attackVector; }
    public void setAttackVector(String attackVector) { this.attackVector = attackVector; }
    
    public String getTeamVisibility() { return teamVisibility; }
    public void setTeamVisibility(String teamVisibility) { this.teamVisibility = teamVisibility; }
    
    public Boolean getIsTarget() { return isTarget; }
    public void setIsTarget(Boolean isTarget) { this.isTarget = isTarget; }
    
    public String getSetupInstructions() { return setupInstructions; }
    public void setSetupInstructions(String setupInstructions) { this.setupInstructions = setupInstructions; }
    
    public String getExerciseInstructions() { return exerciseInstructions; }
    public void setExerciseInstructions(String exerciseInstructions) { this.exerciseInstructions = exerciseInstructions; }
    
    public String getSolutionHints() { return solutionHints; }
    public void setSolutionHints(String solutionHints) { this.solutionHints = solutionHints; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}