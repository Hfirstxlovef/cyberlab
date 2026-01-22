package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "asset")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String ip;
    private String company;
    private String owner;
    private String visibility; // red / blue / both
    private boolean isTarget;
    private boolean enabled;
    private String notes;
    private String project; // ✅ 新增字段：功能区或项目名
    private String topologyProjectId; // 关联的拓扑项目ID

    // 新增：自动探测容器的字段
    private String assetPlatform; // 资产平台类型：docker / k8s / both，默认 docker
    private Integer dockerPort; // Docker API端口，默认 2375
    private Boolean dockerApiEnabled; // 是否启用Docker API探测，默认 true
    private String k8sApiServer; // Kubernetes API Server地址，如 https://192.168.1.100:6443
    private Integer k8sPort; // Kubernetes API端口，默认 6443
    private String k8sToken; // Kubernetes访问令牌
    private String probeStatus; // 探测状态：success / failed / not_configured / probing
    private String lastProbeTime; // 最后探测时间
    private String probeErrorMessage; // 探测失败的错误信息

    // 新增：主机节点关联字段
    private Long preferredHostNodeId; // 首选部署节点ID
    private String preferredHostNodeName; // 首选部署节点名称（冗余字段）
    private String deploymentStrategy; // 部署策略：fixed(固定节点), any(任意节点), load_balanced(负载均衡)
    
    // 新增：高级部署配置
    private String affinityRules; // 亲和性规则，JSON格式存储节点亲和性配置
    private String antiAffinityRules; // 反亲和性规则，JSON格式存储避免与哪些资产部署在同一节点
    private Integer deploymentPriority; // 部署优先级，数值越高优先级越高
    private Boolean enableFailover; // 是否启用故障转移
    private String failoverStrategy; // 故障转移策略：manual(手动), auto(自动)
    private Long fallbackHostNodeId; // 备用节点ID
    
    // 新增：容器相关字段
    private String assetType; // 资产类型：server(服务器), container(容器), service(服务), network(网络设备)
    private String dockerImage; // Docker镜像名称，如 nginx:alpine
    private String containerPorts; // 容器端口配置，JSON格式，如 {"80/tcp": "8080", "443/tcp": "8443"}
    private String volumes; // 数据卷配置，JSON格式，如 ["/app/data:/data", "/app/logs:/logs"]
    private String environments; // 环境变量，JSON格式，如 {"ENV": "production", "DEBUG": "false"}
    private String containerCommand; // 容器启动命令，如 "nginx -g daemon off;"
    private String healthCheckUrl; // 健康检查URL，如 "http://localhost:8080/health"
    private Integer resourceLimitCpu; // CPU限制，单位为millicores
    private Long resourceLimitMemory; // 内存限制，单位为MB

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public void setTarget(boolean target) {
        isTarget = target;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getTopologyProjectId() {
        return topologyProjectId;
    }

    public void setTopologyProjectId(String topologyProjectId) {
        this.topologyProjectId = topologyProjectId;
    }
    
    // 新增字段的 Getters and Setters
    public Long getPreferredHostNodeId() { return preferredHostNodeId; }
    public void setPreferredHostNodeId(Long preferredHostNodeId) { this.preferredHostNodeId = preferredHostNodeId; }
    
    public String getPreferredHostNodeName() { return preferredHostNodeName; }
    public void setPreferredHostNodeName(String preferredHostNodeName) { this.preferredHostNodeName = preferredHostNodeName; }
    
    public String getDeploymentStrategy() { return deploymentStrategy; }
    public void setDeploymentStrategy(String deploymentStrategy) { this.deploymentStrategy = deploymentStrategy; }
    
    // 容器相关字段的 Getters and Setters
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    
    public String getDockerImage() { return dockerImage; }
    public void setDockerImage(String dockerImage) { this.dockerImage = dockerImage; }
    
    public String getContainerPorts() { return containerPorts; }
    public void setContainerPorts(String containerPorts) { this.containerPorts = containerPorts; }
    
    public String getVolumes() { return volumes; }
    public void setVolumes(String volumes) { this.volumes = volumes; }
    
    public String getEnvironments() { return environments; }
    public void setEnvironments(String environments) { this.environments = environments; }
    
    public String getContainerCommand() { return containerCommand; }
    public void setContainerCommand(String containerCommand) { this.containerCommand = containerCommand; }
    
    public String getHealthCheckUrl() { return healthCheckUrl; }
    public void setHealthCheckUrl(String healthCheckUrl) { this.healthCheckUrl = healthCheckUrl; }
    
    public Integer getResourceLimitCpu() { return resourceLimitCpu; }
    public void setResourceLimitCpu(Integer resourceLimitCpu) { this.resourceLimitCpu = resourceLimitCpu; }
    
    public Long getResourceLimitMemory() { return resourceLimitMemory; }
    public void setResourceLimitMemory(Long resourceLimitMemory) { this.resourceLimitMemory = resourceLimitMemory; }
    
    // 高级部署配置的 Getters and Setters
    public String getAffinityRules() { return affinityRules; }
    public void setAffinityRules(String affinityRules) { this.affinityRules = affinityRules; }
    
    public String getAntiAffinityRules() { return antiAffinityRules; }
    public void setAntiAffinityRules(String antiAffinityRules) { this.antiAffinityRules = antiAffinityRules; }
    
    public Integer getDeploymentPriority() { return deploymentPriority; }
    public void setDeploymentPriority(Integer deploymentPriority) { this.deploymentPriority = deploymentPriority; }
    
    public Boolean getEnableFailover() { return enableFailover; }
    public void setEnableFailover(Boolean enableFailover) { this.enableFailover = enableFailover; }
    
    public String getFailoverStrategy() { return failoverStrategy; }
    public void setFailoverStrategy(String failoverStrategy) { this.failoverStrategy = failoverStrategy; }
    
    public Long getFallbackHostNodeId() { return fallbackHostNodeId; }
    public void setFallbackHostNodeId(Long fallbackHostNodeId) { this.fallbackHostNodeId = fallbackHostNodeId; }

    // 自动探测字段的 Getters and Setters
    public String getAssetPlatform() { return assetPlatform; }
    public void setAssetPlatform(String assetPlatform) { this.assetPlatform = assetPlatform; }

    public Integer getDockerPort() { return dockerPort; }
    public void setDockerPort(Integer dockerPort) { this.dockerPort = dockerPort; }

    public Boolean getDockerApiEnabled() { return dockerApiEnabled; }
    public void setDockerApiEnabled(Boolean dockerApiEnabled) { this.dockerApiEnabled = dockerApiEnabled; }

    public String getK8sApiServer() { return k8sApiServer; }
    public void setK8sApiServer(String k8sApiServer) { this.k8sApiServer = k8sApiServer; }

    public Integer getK8sPort() { return k8sPort; }
    public void setK8sPort(Integer k8sPort) { this.k8sPort = k8sPort; }

    public String getK8sToken() { return k8sToken; }
    public void setK8sToken(String k8sToken) { this.k8sToken = k8sToken; }

    public String getProbeStatus() { return probeStatus; }
    public void setProbeStatus(String probeStatus) { this.probeStatus = probeStatus; }

    public String getLastProbeTime() { return lastProbeTime; }
    public void setLastProbeTime(String lastProbeTime) { this.lastProbeTime = lastProbeTime; }

    public String getProbeErrorMessage() { return probeErrorMessage; }
    public void setProbeErrorMessage(String probeErrorMessage) { this.probeErrorMessage = probeErrorMessage; }

    // 工具方法
    /**
     * 获取资产的完整标识符
     * 格式：{公司}|{项目}|{资产名称}
     */
    public String getFullIdentifier() {
        return String.format("%s|%s|%s", 
            company != null ? company : "未知企业",
            project != null ? project : "未分组", 
            name != null ? name : "未命名资产"
        );
    }
    
    /**
     * 检查是否有首选部署节点
     */
    public boolean hasPreferredNode() {
        return preferredHostNodeId != null;
    }
    
    /**
     * 检查是否使用固定节点部署策略
     */
    public boolean isFixedDeployment() {
        return "fixed".equals(deploymentStrategy);
    }
    
    /**
     * 检查是否为容器类型资产
     */
    public boolean isContainerAsset() {
        return "container".equals(assetType);
    }
    
    /**
     * 检查是否配置了Docker镜像
     */
    public boolean hasDockerImage() {
        return dockerImage != null && !dockerImage.trim().isEmpty();
    }
    
    /**
     * 获取默认的容器名称（基于资产名称）
     */
    public String getDefaultContainerName() {
        if (name == null) return "unknown-container";
        return name.toLowerCase().replaceAll("[^a-zA-Z0-9-]", "-");
    }
    
    /**
     * 检查是否配置了健康检查
     */
    public boolean hasHealthCheck() {
        return healthCheckUrl != null && !healthCheckUrl.trim().isEmpty();
    }
    
    /**
     * 检查是否启用了故障转移
     */
    public boolean isFailoverEnabled() {
        return enableFailover != null && enableFailover;
    }
    
    /**
     * 检查是否有备用节点
     */
    public boolean hasFallbackNode() {
        return fallbackHostNodeId != null;
    }
    
    /**
     * 检查是否配置了亲和性规则
     */
    public boolean hasAffinityRules() {
        return affinityRules != null && !affinityRules.trim().isEmpty();
    }
    
    /**
     * 检查是否配置了反亲和性规则
     */
    public boolean hasAntiAffinityRules() {
        return antiAffinityRules != null && !antiAffinityRules.trim().isEmpty();
    }
    
    /**
     * 获取部署优先级，如果未设置则返回默认优先级5
     */
    public int getEffectiveDeploymentPriority() {
        return deploymentPriority != null ? deploymentPriority : 5;
    }
    
    /**
     * 检查是否使用自动故障转移策略
     */
    public boolean isAutoFailover() {
        return "auto".equals(failoverStrategy);
    }
    
    /**
     * 检查是否为高优先级资产（优先级大于7）
     */
    public boolean isHighPriority() {
        return getEffectiveDeploymentPriority() > 7;
    }
    
    /**
     * 获取部署策略显示名称
     */
    public String getDeploymentStrategyDisplayName() {
        if (deploymentStrategy == null) return "未设置";
        switch (deploymentStrategy.toLowerCase()) {
            case "fixed": return "固定节点";
            case "any": return "任意节点";
            case "load_balanced": return "负载均衡";
            default: return deploymentStrategy;
        }
    }
    
    /**
     * 获取完整的部署配置摘要
     */
    public String getDeploymentConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("策略: ").append(getDeploymentStrategyDisplayName());
        if (hasPreferredNode()) {
            summary.append(" | 首选节点: ").append(preferredHostNodeName != null ? preferredHostNodeName : "ID-" + preferredHostNodeId);
        }
        if (hasFallbackNode()) {
            summary.append(" | 备用节点: ID-").append(fallbackHostNodeId);
        }
        if (isFailoverEnabled()) {
            summary.append(" | 故障转移: ").append(isAutoFailover() ? "自动" : "手动");
        }
        summary.append(" | 优先级: ").append(getEffectiveDeploymentPriority());
        return summary.toString();
    }

    /**
     * 检查是否配置了Docker API
     * 只有容器类型的资产才会被容器探测功能使用
     */
    public boolean isDockerApiConfigured() {
        // 只有容器类型资产才进行Docker探测
        if (!"container".equals(assetType)) {
            return false;
        }

        return ("docker".equalsIgnoreCase(assetPlatform) || "both".equalsIgnoreCase(assetPlatform))
            && ip != null && !ip.trim().isEmpty();
    }

    /**
     * 检查是否配置了Kubernetes API
     * 根据asset_platform字段判断，支持默认端口6443
     * 只有容器类型的资产才会被容器探测功能使用
     */
    public boolean isK8sApiConfigured() {
        // 只有容器类型资产才进行K8s探测
        if (!"container".equals(assetType)) {
            return false;
        }

        return ("k8s".equalsIgnoreCase(assetPlatform) || "both".equalsIgnoreCase(assetPlatform))
            && ip != null && !ip.trim().isEmpty();
    }

    /**
     * 获取有效的Docker端口（如果未设置则返回默认值2375）
     */
    public int getEffectiveDockerPort() {
        return dockerPort != null ? dockerPort : 2375;
    }

    /**
     * 获取有效的K8s端口（如果未设置则返回默认值6443）
     */
    public int getEffectiveK8sPort() {
        return k8sPort != null ? k8sPort : 6443;
    }

    /**
     * 获取Docker API连接地址
     */
    public String getDockerApiUrl() {
        if (!isDockerApiConfigured()) {
            return null;
        }

        // 验证IP格式，防止中文字符等无效输入
        if (!isValidIpOrHostname(ip)) {
            throw new IllegalStateException("资产IP格式无效: " + ip + " (不能包含中文字符，只能包含字母、数字、点和连字符)");
        }

        return String.format("tcp://%s:%d", ip, getEffectiveDockerPort());
    }

    /**
     * 获取K8s API连接地址
     */
    public String getK8sApiUrl() {
        if (!isK8sApiConfigured()) {
            return null;
        }
        // 如果已配置完整的apiServer，直接使用
        if (k8sApiServer != null && !k8sApiServer.trim().isEmpty()) {
            return k8sApiServer;
        }
        // 否则根据IP和端口自动构建
        return String.format("https://%s:%d", ip, getEffectiveK8sPort());
    }

    /**
     * 检查探测状态是否成功
     */
    public boolean isProbeSuccessful() {
        return "success".equals(probeStatus);
    }

    /**
     * 检查探测状态是否失败
     */
    public boolean isProbeFailed() {
        return "failed".equals(probeStatus);
    }

    /**
     * 检查是否正在探测中
     */
    public boolean isProbing() {
        return "probing".equals(probeStatus);
    }

    /**
     * 检查是否支持Docker平台
     */
    public boolean supportsDockerPlatform() {
        return assetPlatform == null || "docker".equals(assetPlatform) || "both".equals(assetPlatform);
    }

    /**
     * 检查是否支持Kubernetes平台
     */
    public boolean supportsK8sPlatform() {
        return "k8s".equals(assetPlatform) || "both".equals(assetPlatform);
    }

    /**
     * 获取平台显示名称
     */
    public String getPlatformDisplayName() {
        if (assetPlatform == null) return "Docker";
        switch (assetPlatform.toLowerCase()) {
            case "docker": return "Docker";
            case "k8s": return "Kubernetes";
            case "both": return "Docker & Kubernetes";
            default: return assetPlatform;
        }
    }

    /**
     * 验证IP地址或主机名格式是否有效
     * 拒绝包含中文字符的主机名
     */
    private boolean isValidIpOrHostname(String host) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        // 检查是否包含中文字符
        if (host.matches(".*[\\u4e00-\\u9fa5]+.*")) {
            return false;
        }

        // 检查是否为有效的IP地址或主机名（只允许字母、数字、点、连字符）
        return host.matches("^[a-zA-Z0-9.-]+$");
    }
}
