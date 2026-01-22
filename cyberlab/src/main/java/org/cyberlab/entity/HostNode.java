package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

@Entity
@Table(name = "host_nodes")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class HostNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true)
    private String name; // 节点名称，如 "生产服务器01", "实验环境主机A"
    
    @Column(name = "display_name", nullable = false)
    private String displayName; // 显示名称，用于前端展示
    
    @Column(name = "host_ip", nullable = false)
    private String hostIp; // 主机IP地址
    
    @Column(name = "docker_port")
    private Integer dockerPort; // Docker API端口，默认2376
    
    @Column(name = "ssh_port")
    private Integer sshPort; // SSH端口，默认22
    
    @Column(name = "cluster_name")
    private String clusterName; // 所属集群名称
    
    @Column(name = "node_type", nullable = false)
    private String nodeType; // 节点类型: local, remote, vm, physical
    
    @Column(name = "environment", nullable = false)
    private String environment; // 环境类型: development, testing, staging, production
    
    @Column(name = "status", nullable = false)
    private String status; // 节点状态: active, inactive, maintenance, error
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 节点描述信息
    
    // 资源信息
    @Column(name = "cpu_cores")
    private Integer cpuCores; // CPU核心数
    
    @Column(name = "memory_total")
    private Long memoryTotal; // 总内存(MB)
    
    @Column(name = "disk_total")
    private Long diskTotal; // 总磁盘空间(GB)
    
    // 连接配置
    @Column(name = "docker_tls_enabled")
    private Boolean dockerTlsEnabled; // 是否启用Docker TLS
    
    @Column(name = "docker_cert_path")
    private String dockerCertPath; // Docker证书路径
    
    @Column(name = "ssh_username")
    private String sshUsername; // SSH用户名
    
    @Column(name = "ssh_key_path")
    private String sshKeyPath; // SSH密钥路径
    
    // 监控配置
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled; // 是否启用监控
    
    @Column(name = "max_containers")
    private Integer maxContainers; // 最大容器数量
    
    @Column(name = "priority")
    private Integer priority; // 节点优先级，用于负载均衡
    
    // 标签和元数据
    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels; // JSON格式存储节点标签
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON格式存储其他元数据
    
    // 时间字段
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck; // 最后健康检查时间
    
    // 构造函数
    public HostNode() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "inactive";
        this.nodeType = "local";
        this.environment = "development";
        this.dockerPort = 2376;
        this.sshPort = 22;
        this.dockerTlsEnabled = false;
        this.monitoringEnabled = true;
        this.maxContainers = 50;
        this.priority = 1;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getHostIp() { return hostIp; }
    public void setHostIp(String hostIp) { this.hostIp = hostIp; }
    
    public Integer getDockerPort() { return dockerPort; }
    public void setDockerPort(Integer dockerPort) { this.dockerPort = dockerPort; }
    
    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }
    
    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }
    
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    
    public Long getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Long memoryTotal) { this.memoryTotal = memoryTotal; }
    
    public Long getDiskTotal() { return diskTotal; }
    public void setDiskTotal(Long diskTotal) { this.diskTotal = diskTotal; }
    
    public Boolean getDockerTlsEnabled() { return dockerTlsEnabled; }
    public void setDockerTlsEnabled(Boolean dockerTlsEnabled) { this.dockerTlsEnabled = dockerTlsEnabled; }
    
    public String getDockerCertPath() { return dockerCertPath; }
    public void setDockerCertPath(String dockerCertPath) { this.dockerCertPath = dockerCertPath; }
    
    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }
    
    public String getSshKeyPath() { return sshKeyPath; }
    public void setSshKeyPath(String sshKeyPath) { this.sshKeyPath = sshKeyPath; }
    
    public Boolean getMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(Boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }
    
    public Integer getMaxContainers() { return maxContainers; }
    public void setMaxContainers(Integer maxContainers) { this.maxContainers = maxContainers; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // 工具方法
    public String getFullAddress() {
        return hostIp + ":" + dockerPort;
    }
    
    public boolean isActive() {
        return "active".equals(status);
    }
    
    public boolean isLocal() {
        return "local".equals(nodeType);
    }
}