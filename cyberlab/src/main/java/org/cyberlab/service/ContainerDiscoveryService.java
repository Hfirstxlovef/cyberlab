package org.cyberlab.service;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.ImageInfo;
import org.cyberlab.entity.HostNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 容器发现服务
 * 负责从Docker节点发现容器并转换为资产对象
 */
@Service
public class ContainerDiscoveryService {
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private HostNodeService hostNodeService;
    
    /**
     * 将容器信息转换为资产对象
     */
    public Asset convertContainerToAsset(ContainerInfo container, HostNode hostNode, String company, String project) {
        Asset asset = new Asset();
        
        // 基本信息
        asset.setName(generateAssetName(container));
        asset.setAssetType("container");
        asset.setCompany(company != null ? company : "发现的容器");
        asset.setProject(project != null ? project : "未分组");
        asset.setOwner("系统发现");
        asset.setEnabled(true);
        
        // 容器相关信息
        asset.setDockerImage(container.getImage());
        asset.setNotes("从节点 " + hostNode.getDisplayName() + " 发现的容器");
        
        // 节点关联信息
        asset.setPreferredHostNodeId(hostNode.getId());
        asset.setPreferredHostNodeName(hostNode.getDisplayName());
        asset.setDeploymentStrategy("fixed");
        
        // 根据容器状态设置可见性
        if (container.getStatus().toLowerCase().contains("running")) {
            asset.setVisibility("both");
        } else {
            asset.setVisibility("blue");
        }
        
        // 尝试解析容器端口配置
        asset.setContainerPorts(extractPortConfiguration(container));
        
        // 根据镜像类型设置健康检查URL
        asset.setHealthCheckUrl(generateHealthCheckUrl(container.getImage()));
        
        return asset;
    }
    
    /**
     * 批量发现所有节点上的容器
     */
    public Map<String, Object> discoverAllContainers() {
        Map<String, Object> result = new HashMap<>();
        List<HostNode> activeNodes = hostNodeService.getActiveNodes();
        List<ContainerInfo> allContainers = new ArrayList<>();
        Map<String, Object> nodeDetails = new HashMap<>();
        
        for (HostNode node : activeNodes) {
            try {
                List<ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());
                allContainers.addAll(containers);
                
                nodeDetails.put("node_" + node.getId(), Map.of(
                    "nodeName", node.getDisplayName(),
                    "nodeIp", node.getHostIp(),
                    "containerCount", containers.size(),
                    "containers", containers
                ));
            } catch (Exception e) {
                nodeDetails.put("node_" + node.getId(), Map.of(
                    "nodeName", node.getDisplayName(),
                    "nodeIp", node.getHostIp(),
                    "containerCount", 0,
                    "error", "无法连接到节点: " + e.getMessage()
                ));
            }
        }
        
        result.put("nodes", nodeDetails);
        result.put("summary", Map.of(
            "totalNodes", activeNodes.size(),
            "totalContainers", allContainers.size()
        ));
        result.put("allContainers", allContainers);
        
        return result;
    }
    
    /**
     * 生成资产名称
     */
    private String generateAssetName(ContainerInfo container) {
        String containerName = container.getName();
        if (containerName != null && !containerName.isEmpty()) {
            // 移除 Docker 自动添加的前缀斜杠
            if (containerName.startsWith("/")) {
                containerName = containerName.substring(1);
            }
            return containerName;
        }
        
        // 如果没有名称，基于镜像名称生成
        String image = container.getImage();
        if (image != null && !image.isEmpty()) {
            // 提取镜像名称部分，去除标签
            String imageName = image.split(":")[0];
            if (imageName.contains("/")) {
                imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
            }
            return imageName + "-container";
        }
        
        return "unknown-container";
    }
    
    /**
     * 提取端口配置
     */
    private String extractPortConfiguration(ContainerInfo container) {
        // 这里可以扩展以解析更复杂的端口配置
        // 目前返回基础的JSON格式
        if (container.getStatus().toLowerCase().contains("running")) {
            // 根据常见镜像类型提供默认端口配置
            String image = container.getImage().toLowerCase();
            if (image.contains("nginx")) {
                return "{\"80/tcp\": \"8080\", \"443/tcp\": \"8443\"}";
            } else if (image.contains("mysql")) {
                return "{\"3306/tcp\": \"3306\"}";
            } else if (image.contains("redis")) {
                return "{\"6379/tcp\": \"6379\"}";
            } else if (image.contains("postgres")) {
                return "{\"5432/tcp\": \"5432\"}";
            } else if (image.contains("apache") || image.contains("httpd")) {
                return "{\"80/tcp\": \"8080\"}";
            }
        }
        return "{}";
    }
    
    /**
     * 生成健康检查URL
     */
    private String generateHealthCheckUrl(String imageName) {
        if (imageName == null) return null;
        
        String image = imageName.toLowerCase();
        if (image.contains("nginx") || image.contains("apache") || image.contains("httpd")) {
            return "http://localhost:8080";
        } else if (image.contains("mysql")) {
            return "tcp://localhost:3306";
        } else if (image.contains("redis")) {
            return "tcp://localhost:6379";
        }
        
        return null;
    }
    
    /**
     * 验证容器是否适合转换为资产
     * 放宽了过滤条件，只排除明确的系统容器
     */
    public boolean isContainerSuitableForAsset(ContainerInfo container) {
        // 只排除明确的系统容器和临时容器，放宽过滤条件
        String name = container.getName();
        String image = container.getImage();
        
        if (name != null) {
            name = name.toLowerCase();
            // 只排除明确的Docker系统容器和Kubernetes系统容器
            if (name.startsWith("k8s_pause") || name.startsWith("k8s_coredns")) {
                return false;
            }
            // 排除Docker Desktop的系统容器
            if (name.contains("docker-desktop") || name.contains("com.docker.")) {
                return false;
            }
        }
        
        if (image != null) {
            image = image.toLowerCase();
            // 只排除明确的系统镜像，如pause容器
            if (image.startsWith("k8s.gcr.io/pause") || image.startsWith("registry.k8s.io/pause")) {
                return false;
            }
        }
        
        // 默认允许所有其他容器，包括用户的应用容器
        return true;
    }
    
    /**
     * 获取指定节点上的容器列表
     */
    public List<ContainerInfo> getContainersFromNode(Long nodeId) {
        try {
            return dockerService.getContainersOnNode(nodeId);
        } catch (Exception e) {
            // 获取节点容器列表失败
            return new ArrayList<>();
        }
    }
    
    /**
     * 强制刷新指定节点的容器列表
     */
    public List<ContainerInfo> forceRefreshNodeContainers(Long nodeId) {
        try {
            // 首先清理可能的缓存，然后重新获取容器信息
            List<ContainerInfo> containers = dockerService.getContainersOnNode(nodeId);
            
            // 对每个容器进行状态更新
            for (ContainerInfo container : containers) {
                try {
                    // 获取最新的容器详细信息
                    ContainerInfo detailedInfo = dockerService.getContainerInfo(container.getContainerId());
                    if (detailedInfo != null) {
                        // 更新状态信息
                        container.setStatus(detailedInfo.getStatus());
                        container.setCpuUsage(detailedInfo.getCpuUsage());
                        container.setMemoryUsage(detailedInfo.getMemoryUsage());
                        container.setNetworkInfo(detailedInfo.getNetworkInfo());
                    }
                } catch (Exception e) {
                    // 刷新容器详细信息失败
                }
            }
            
            // 成功刷新节点的容器列表
            return containers;
        } catch (Exception e) {
            // 强制刷新节点容器列表失败
            return new ArrayList<>();
        }
    }

    /**
     * 获取指定节点上的Docker镜像列表
     */
    public List<ImageInfo> getImagesFromNode(Long nodeId) {
        try {
            return dockerService.getImagesOnNode(nodeId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}