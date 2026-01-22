package org.cyberlab.entity;

import lombok.Data;

@Data
public class ContainerInfo {
    private String containerId;
    private String name;
    private String image;
    private String status;
    private String cpuUsage;
    private String memoryUsage;
    
    // 新增：主机节点相关字段
    private Long hostNodeId;
    private String hostNodeName;
    private String hostNodeIp;
    
    // 新增：健康检查和网络相关字段
    private String healthStatus; // healthy, unhealthy, unknown
    private String networkInfo; // 网络信息，JSON格式
    private String portMappings; // 端口映射，JSON格式
    private String createdAt; // 创建时间
    private String updatedAt; // 最后更新时间
    private String tags; // 标签，JSON格式

    // 手动添加getter和setter方法以确保兼容性
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(String cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public String getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(String memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
    
    // 新增字段的 getter 和 setter 方法
    public Long getHostNodeId() {
        return hostNodeId;
    }
    
    public void setHostNodeId(Long hostNodeId) {
        this.hostNodeId = hostNodeId;
    }
    
    /**
     * 为前端兼容性提供 getId() 方法，返回容器ID
     * 解决前端 container.id 访问问题
     */
    public String getId() {
        return containerId;
    }
    
    /**
     * 为前端兼容性提供 setId() 方法
     */
    public void setId(String id) {
        this.containerId = id;
    }
    
    public String getHostNodeName() {
        return hostNodeName;
    }
    
    public void setHostNodeName(String hostNodeName) {
        this.hostNodeName = hostNodeName;
    }
    
    public String getHostNodeIp() {
        return hostNodeIp;
    }
    
    public void setHostNodeIp(String hostNodeIp) {
        this.hostNodeIp = hostNodeIp;
    }
    
    // 新增字段的 getter 和 setter 方法
    public String getHealthStatus() {
        return healthStatus;
    }
    
    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }
    
    public String getNetworkInfo() {
        return networkInfo;
    }
    
    public void setNetworkInfo(String networkInfo) {
        this.networkInfo = networkInfo;
    }
    
    public String getPortMappings() {
        return portMappings;
    }
    
    public void setPortMappings(String portMappings) {
        this.portMappings = portMappings;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    // 新增：项目化功能需要的字段
    private Long nodeId;
    private String nodeName;

    // 新增：资产直连相关字段
    private Long assetId; // 关联的资产ID
    private String dockerUrl; // Docker API连接URL

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getDockerUrl() {
        return dockerUrl;
    }

    public void setDockerUrl(String dockerUrl) {
        this.dockerUrl = dockerUrl;
    }
    
    /**
     * 判断容器是否健康
     */
    public boolean isHealthy() {
        return "healthy".equals(healthStatus) && 
               status != null && 
               (status.toLowerCase().contains("running") || status.toLowerCase().contains("up"));
    }
    
    /**
     * 获取简化的状态描述
     */
    public String getSimpleStatus() {
        if (status == null) return "未知";
        
        String lowerStatus = status.toLowerCase();
        if (lowerStatus.contains("running") || lowerStatus.contains("up")) {
            return "运行中";
        } else if (lowerStatus.contains("exited")) {
            return "已停止";
        } else if (lowerStatus.contains("paused")) {
            return "已暂停";
        } else if (lowerStatus.contains("restarting")) {
            return "重启中";
        } else {
            return "未知";
        }
    }
    
    /**
     * 获取优先级状态（用于排序和过滤）
     */
    public int getStatusPriority() {
        String simple = getSimpleStatus();
        switch (simple) {
            case "运行中": return 1;
            case "重启中": return 2;
            case "已暂停": return 3;
            case "已停止": return 4;
            default: return 5;
        }
    }
}