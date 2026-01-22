package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

@Entity
@Table(name = "drill_containers")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DrillContainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "range_id", nullable = false)
    private Long rangeId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "image_name", nullable = false)
    private String imageName;
    
    @Column(name = "container_id")
    private String containerId;
    
    @Column(name = "status", nullable = false)
    private String status; // not_deployed, deploying, running, stopped, failed
    
    @Column(name = "ip")
    private String ip;
    
    @Column(name = "port")
    private Integer port;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "deploy_log", columnDefinition = "TEXT")
    private String deployLog;
    
    @Column(name = "log_path")
    private String logPath;
    
    // 新增：资产关联字段
    @Column(name = "asset_id")
    private Long assetId;
    
    @Column(name = "scenario_template_id")
    private Long scenarioTemplateId;
    
    // 新增：主机节点关联字段
    @Column(name = "host_node_id")
    private Long hostNodeId; // 关联的主机节点ID
    
    @Column(name = "host_node_name")
    private String hostNodeName; // 主机节点名称（冗余字段，提高查询性能）
    
    @Column(name = "host_node_ip")
    private String hostNodeIp; // 主机节点IP（冗余字段）
    
    @Column(name = "container_full_name")
    private String containerFullName; // 完整容器名称：{公司}-{项目}-{资产名}-{节点标识}-{时间戳}
    
    // 新增：安全配置字段
    @Column(name = "security_config", columnDefinition = "TEXT")
    private String securityConfig; // JSON格式存储安全配置
    
    @Column(name = "network_config", columnDefinition = "TEXT")
    private String networkConfig; // JSON格式存储网络配置
    
    @Column(name = "resource_limits", columnDefinition = "TEXT")
    private String resourceLimits; // JSON格式存储资源限制
    
    @Column(name = "environment_vars", columnDefinition = "TEXT")
    private String environmentVars; // JSON格式存储环境变量
    
    @Column(name = "volume_mounts", columnDefinition = "TEXT")
    private String volumeMounts; // JSON格式存储卷挂载配置
    
    @Column(name = "exposed_ports", columnDefinition = "TEXT")
    private String exposedPorts; // JSON格式存储端口映射配置
    
    // 新增：部署配置字段
    @Column(name = "auto_start", nullable = false)
    private Boolean autoStart; // 是否自动启动
    
    @Column(name = "restart_policy")
    private String restartPolicy; // 重启策略：no, on-failure, always, unless-stopped
    
    @Column(name = "health_check_config", columnDefinition = "TEXT")
    private String healthCheckConfig; // JSON格式存储健康检查配置
    
    // 时间字段
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "deploy_time")
    private LocalDateTime deployTime;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "stop_time")
    private LocalDateTime stopTime;
    
    // 构造函数
    public DrillContainer() {
        this.createTime = LocalDateTime.now();
        this.status = "not_deployed";
        this.autoStart = false;
        this.restartPolicy = "no";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getRangeId() { return rangeId; }
    public void setRangeId(Long rangeId) { this.rangeId = rangeId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }
    
    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDeployLog() { return deployLog; }
    public void setDeployLog(String deployLog) { this.deployLog = deployLog; }
    
    public String getLogPath() { return logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getDeployTime() { return deployTime; }
    public void setDeployTime(LocalDateTime deployTime) { this.deployTime = deployTime; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getStopTime() { return stopTime; }
    public void setStopTime(LocalDateTime stopTime) { this.stopTime = stopTime; }
    
    // 新增字段的 Getters and Setters
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    
    public Long getScenarioTemplateId() { return scenarioTemplateId; }
    public void setScenarioTemplateId(Long scenarioTemplateId) { this.scenarioTemplateId = scenarioTemplateId; }
    
    // 主机节点相关字段的 Getters and Setters
    public Long getHostNodeId() { return hostNodeId; }
    public void setHostNodeId(Long hostNodeId) { this.hostNodeId = hostNodeId; }
    
    public String getHostNodeName() { return hostNodeName; }
    public void setHostNodeName(String hostNodeName) { this.hostNodeName = hostNodeName; }
    
    public String getHostNodeIp() { return hostNodeIp; }
    public void setHostNodeIp(String hostNodeIp) { this.hostNodeIp = hostNodeIp; }
    
    public String getContainerFullName() { return containerFullName; }
    public void setContainerFullName(String containerFullName) { this.containerFullName = containerFullName; }
    
    public String getSecurityConfig() { return securityConfig; }
    public void setSecurityConfig(String securityConfig) { this.securityConfig = securityConfig; }
    
    public String getNetworkConfig() { return networkConfig; }
    public void setNetworkConfig(String networkConfig) { this.networkConfig = networkConfig; }
    
    public String getResourceLimits() { return resourceLimits; }
    public void setResourceLimits(String resourceLimits) { this.resourceLimits = resourceLimits; }
    
    public String getEnvironmentVars() { return environmentVars; }
    public void setEnvironmentVars(String environmentVars) { this.environmentVars = environmentVars; }
    
    public String getVolumeMounts() { return volumeMounts; }
    public void setVolumeMounts(String volumeMounts) { this.volumeMounts = volumeMounts; }
    
    public String getExposedPorts() { return exposedPorts; }
    public void setExposedPorts(String exposedPorts) { this.exposedPorts = exposedPorts; }
    
    public Boolean getAutoStart() { return autoStart; }
    public void setAutoStart(Boolean autoStart) { this.autoStart = autoStart; }
    
    public String getRestartPolicy() { return restartPolicy; }
    public void setRestartPolicy(String restartPolicy) { this.restartPolicy = restartPolicy; }
    
    public String getHealthCheckConfig() { return healthCheckConfig; }
    public void setHealthCheckConfig(String healthCheckConfig) { this.healthCheckConfig = healthCheckConfig; }
    
    // 工具方法
    /**
     * 生成完整的容器名称
     * 格式：{公司}-{项目}-{资产名}-{节点标识}-{时间戳}
     */
    public String generateContainerFullName(String company, String project, String assetName, String nodeIdentifier) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fullName = String.format("%s-%s-%s-%s-%s", 
            company != null ? company.replaceAll("[^a-zA-Z0-9]", "") : "unknown",
            project != null ? project.replaceAll("[^a-zA-Z0-9]", "") : "default", 
            assetName != null ? assetName.replaceAll("[^a-zA-Z0-9]", "") : "asset",
            nodeIdentifier != null ? nodeIdentifier.replaceAll("[^a-zA-Z0-9]", "") : "local",
            timestamp
        ).toLowerCase();
        this.containerFullName = fullName;
        return fullName;
    }
    
    /**
     * 获取容器的显示名称，包含部署位置信息
     */
    public String getDisplayName() {
        if (hostNodeName != null && !hostNodeName.isEmpty()) {
            return String.format("%s@%s", name, hostNodeName);
        }
        return name;
    }
    
    /**
     * 检查容器是否部署在远程节点
     */
    public boolean isRemoteDeployment() {
        return hostNodeId != null && hostNodeIp != null && !hostNodeIp.equals("localhost") && !hostNodeIp.equals("127.0.0.1");
    }
}