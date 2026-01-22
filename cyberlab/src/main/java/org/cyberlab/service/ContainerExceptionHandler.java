package org.cyberlab.service;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.HostNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 容器异常处理服务
 * 处理各种容器相关的异常情况和保护机制
 */
@Service
public class ContainerExceptionHandler {

    @Autowired
    private HostNodeService hostNodeService;
    
    @Autowired
    private ContainerDiscoveryService containerDiscoveryService;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private ContainerAuditService containerAuditService;

    // 端口范围定义
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;
    
    // 常用端口冲突检查
    private static final Set<Integer> RESERVED_PORTS = Set.of(
        22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 3306, 5432, 6379, 27017
    );

    /**
     * 检查主机是否离线
     */
    public Map<String, Object> checkHostStatus(Long hostNodeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hostNodeId", hostNodeId);
        result.put("timestamp", LocalDateTime.now());
        
        try {
            Optional<HostNode> hostOpt = hostNodeService.getNodeById(hostNodeId);
            if (hostOpt.isEmpty()) {
                result.put("status", "NOT_FOUND");
                result.put("message", "主机节点不存在");
                result.put("actionRequired", "检查主机节点配置");
                return result;
            }
            
            HostNode host = hostOpt.get();
            result.put("hostName", host.getDisplayName());
            result.put("hostIp", host.getHostIp());
            
            // 测试连接
            boolean isOnline = hostNodeService.testNodeConnection(hostNodeId);
            
            if (isOnline) {
                result.put("status", "ONLINE");
                result.put("message", "主机在线");
                result.put("actionRequired", "无");
                
                // 测试Docker服务
                try {
                    List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(hostNodeId);
                    result.put("dockerStatus", "AVAILABLE");
                    result.put("containerCount", containers.size());
                } catch (Exception e) {
                    result.put("dockerStatus", "UNAVAILABLE");
                    result.put("dockerError", e.getMessage());
                    result.put("actionRequired", "检查Docker服务状态");
                }
            } else {
                result.put("status", "OFFLINE");
                result.put("message", "主机离线或无法连接");
                result.put("actionRequired", "检查网络连接和主机状态");
                result.put("suggestedActions", Arrays.asList(
                    "ping " + host.getHostIp(),
                    "检查主机是否启动",
                    "检查网络防火墙设置",
                    "验证SSH/Docker端口是否开放"
                ));
            }
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "检查主机状态时发生异常");
            result.put("error", e.getMessage());
            result.put("actionRequired", "查看错误详情并联系管理员");
        }
        
        return result;
    }

    /**
     * 检测失联的容器
     */
    public List<Map<String, Object>> detectLostContainers() {
        List<Map<String, Object>> lostContainers = new ArrayList<>();
        
        try {
            List<Asset> containerAssets = assetService.getAllAssets().stream()
                .filter(asset -> "container".equals(asset.getAssetType()) && 
                               asset.getPreferredHostNodeId() != null)
                .collect(Collectors.toList());
            
            for (Asset asset : containerAssets) {
                Map<String, Object> lostInfo = checkContainerLost(asset);
                if ((Boolean) lostInfo.get("isLost")) {
                    lostContainers.add(lostInfo);
                }
            }
            
        } catch (Exception e) {
            // Debug statement removed
        }
        
        return lostContainers;
    }

    /**
     * 检查单个容器是否失联
     */
    public Map<String, Object> checkContainerLost(Asset asset) {
        Map<String, Object> result = new HashMap<>();
        result.put("assetId", asset.getId());
        result.put("assetName", asset.getName());
        result.put("isLost", false);
        
        try {
            if (asset.getPreferredHostNodeId() == null) {
                result.put("isLost", false);
                result.put("reason", "资产未关联主机节点");
                return result;
            }
            
            // 检查主机状态
            Map<String, Object> hostStatus = checkHostStatus(asset.getPreferredHostNodeId());
            if (!"ONLINE".equals(hostStatus.get("status"))) {
                result.put("isLost", true);
                result.put("reason", "主机离线");
                result.put("hostStatus", hostStatus);
                return result;
            }
            
            // 检查容器是否存在
            List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(asset.getPreferredHostNodeId());
            
            boolean containerExists = containers.stream()
                .anyMatch(container -> isContainerMatchAsset(container, asset));
            
            if (!containerExists) {
                result.put("isLost", true);
                result.put("reason", "容器不存在或已被删除");
                result.put("availableContainers", containers.size());
                result.put("rematchSuggestions", suggestContainerRematch(asset, containers));
            }
            
        } catch (Exception e) {
            result.put("isLost", true);
            result.put("reason", "检查容器状态异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 端口冲突预检查
     */
    public Map<String, Object> preCheckPortConflicts(String portMapping, Long hostNodeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasConflict", false);
        result.put("conflicts", new ArrayList<>());
        result.put("warnings", new ArrayList<>());
        result.put("suggestions", new ArrayList<>());
        
        try {
            if (portMapping == null || portMapping.trim().isEmpty()) {
                result.put("hasConflict", false);
                result.put("message", "未指定端口映射");
                return result;
            }
            
            List<Integer> requestedPorts = parsePortMapping(portMapping);
            List<String> conflicts = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();
            
            // 检查端口范围
            for (Integer port : requestedPorts) {
                if (port < MIN_PORT || port > MAX_PORT) {
                    conflicts.add(String.format("端口 %d 超出有效范围 (%d-%d)", port, MIN_PORT, MAX_PORT));
                }
                
                if (RESERVED_PORTS.contains(port)) {
                    warnings.add(String.format("端口 %d 是系统保留端口，可能导致冲突", port));
                }
            }
            
            // 检查主机上的端口占用情况
            if (hostNodeId != null) {
                Map<String, Object> hostPortUsage = checkHostPortUsage(hostNodeId, requestedPorts);
                List<Integer> occupiedPorts = (List<Integer>) hostPortUsage.get("occupiedPorts");
                
                for (Integer occupiedPort : occupiedPorts) {
                    conflicts.add(String.format("端口 %d 在主机上已被占用", occupiedPort));
                }
                
                // 建议可用端口
                List<Integer> availablePorts = findAvailablePorts(hostNodeId, requestedPorts.size());
                if (!availablePorts.isEmpty()) {
                    suggestions.add("建议使用以下可用端口: " + availablePorts.toString());
                }
            }
            
            result.put("hasConflict", !conflicts.isEmpty());
            result.put("conflicts", conflicts);
            result.put("warnings", warnings);
            result.put("suggestions", suggestions);
            result.put("requestedPorts", requestedPorts);
            
        } catch (Exception e) {
            result.put("hasConflict", true);
            result.put("error", "端口冲突检查异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 解析端口映射
     */
    private List<Integer> parsePortMapping(String portMapping) {
        List<Integer> ports = new ArrayList<>();
        
        try {
            // 支持多种格式：8080:80, 8080-8082:80, [8080,8081]:80
            Pattern portPattern = Pattern.compile("(\\d+)(?:-(\\d+))?");
            java.util.regex.Matcher matcher = portPattern.matcher(portMapping);
            
            while (matcher.find()) {
                int startPort = Integer.parseInt(matcher.group(1));
                String endPortStr = matcher.group(2);
                
                if (endPortStr != null) {
                    int endPort = Integer.parseInt(endPortStr);
                    for (int port = startPort; port <= endPort; port++) {
                        ports.add(port);
                    }
                } else {
                    ports.add(startPort);
                }
            }
        } catch (Exception e) {
            // Debug statement removed
        }
        
        return ports;
    }

    /**
     * 检查主机端口使用情况
     */
    private Map<String, Object> checkHostPortUsage(Long hostNodeId, List<Integer> requestedPorts) {
        Map<String, Object> result = new HashMap<>();
        List<Integer> occupiedPorts = new ArrayList<>();
        
        try {
            List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(hostNodeId);
            
            for (ContainerInfo container : containers) {
                if (container.getPortMappings() != null) {
                    List<Integer> containerPorts = parsePortMapping(container.getPortMappings());
                    for (Integer port : requestedPorts) {
                        if (containerPorts.contains(port)) {
                            occupiedPorts.add(port);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Debug statement removed
        }
        
        result.put("occupiedPorts", occupiedPorts);
        result.put("checkedPorts", requestedPorts);
        return result;
    }

    /**
     * 查找可用端口
     */
    private List<Integer> findAvailablePorts(Long hostNodeId, int count) {
        List<Integer> availablePorts = new ArrayList<>();
        Set<Integer> usedPorts = new HashSet<>();
        
        try {
            List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(hostNodeId);
            for (ContainerInfo container : containers) {
                if (container.getPortMappings() != null) {
                    usedPorts.addAll(parsePortMapping(container.getPortMappings()));
                }
            }
            
            // 添加保留端口
            usedPorts.addAll(RESERVED_PORTS);
            
            // 从8000开始查找可用端口
            int currentPort = 8000;
            while (availablePorts.size() < count && currentPort < MAX_PORT) {
                if (!usedPorts.contains(currentPort)) {
                    availablePorts.add(currentPort);
                }
                currentPort++;
            }
            
        } catch (Exception e) {
            // Debug statement removed
        }
        
        return availablePorts;
    }

    /**
     * 建议容器重新匹配
     */
    public List<Map<String, Object>> suggestContainerRematch(Asset asset, List<ContainerInfo> availableContainers) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        try {
            for (ContainerInfo container : availableContainers) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("containerId", container.getContainerId());
                suggestion.put("containerName", container.getName());
                suggestion.put("image", container.getImage());
                suggestion.put("status", container.getStatus());
                
                // 计算匹配度
                double matchScore = calculateMatchScore(asset, container);
                suggestion.put("matchScore", matchScore);
                suggestion.put("confidence", getConfidenceLevel(matchScore));
                
                // 匹配原因
                List<String> matchReasons = getMatchReasons(asset, container);
                suggestion.put("matchReasons", matchReasons);
                
                if (matchScore > 0.3) { // 只返回匹配度较高的建议
                    suggestions.add(suggestion);
                }
            }
            
            // 按匹配度排序
            suggestions.sort((a, b) -> Double.compare(
                (Double) b.get("matchScore"), 
                (Double) a.get("matchScore")
            ));
            
        } catch (Exception e) {
            // Debug statement removed
        }
        
        return suggestions;
    }

    /**
     * 计算容器与资产的匹配度
     */
    private double calculateMatchScore(Asset asset, ContainerInfo container) {
        double score = 0.0;
        int factors = 0;
        
        // 镜像名称匹配
        if (asset.getDockerImage() != null && container.getImage() != null) {
            factors++;
            if (asset.getDockerImage().equals(container.getImage())) {
                score += 0.5;
            } else if (isSimilarImage(asset.getDockerImage(), container.getImage())) {
                score += 0.3;
            }
        }
        
        // 容器名称匹配
        if (asset.getName() != null && container.getName() != null) {
            factors++;
            if (asset.getName().toLowerCase().contains(container.getName().toLowerCase()) ||
                container.getName().toLowerCase().contains(asset.getName().toLowerCase())) {
                score += 0.3;
            }
        }
        
        // 端口匹配
        if (asset.getContainerPorts() != null && container.getPortMappings() != null) {
            factors++;
            if (hasPortOverlap(asset.getContainerPorts(), container.getPortMappings())) {
                score += 0.2;
            }
        }
        
        return factors > 0 ? score / factors : 0.0;
    }

    /**
     * 判断是否为相似镜像
     */
    private boolean isSimilarImage(String image1, String image2) {
        String base1 = image1.split(":")[0];
        String base2 = image2.split(":")[0];
        return base1.equals(base2);
    }

    /**
     * 检查端口是否有重叠
     */
    private boolean hasPortOverlap(String ports1, String ports2) {
        try {
            List<Integer> portList1 = parsePortMapping(ports1);
            List<Integer> portList2 = parsePortMapping(ports2);
            return portList1.stream().anyMatch(portList2::contains);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取置信度级别
     */
    private String getConfidenceLevel(double score) {
        if (score >= 0.8) return "高";
        if (score >= 0.5) return "中";
        if (score >= 0.3) return "低";
        return "很低";
    }

    /**
     * 获取匹配原因
     */
    private List<String> getMatchReasons(Asset asset, ContainerInfo container) {
        List<String> reasons = new ArrayList<>();
        
        if (asset.getDockerImage() != null && container.getImage() != null) {
            if (asset.getDockerImage().equals(container.getImage())) {
                reasons.add("镜像完全匹配");
            } else if (isSimilarImage(asset.getDockerImage(), container.getImage())) {
                reasons.add("镜像基础名称匹配");
            }
        }
        
        if (asset.getName() != null && container.getName() != null) {
            if (asset.getName().toLowerCase().contains(container.getName().toLowerCase()) ||
                container.getName().toLowerCase().contains(asset.getName().toLowerCase())) {
                reasons.add("名称部分匹配");
            }
        }
        
        if (hasPortOverlap(asset.getContainerPorts(), container.getPortMappings())) {
            reasons.add("端口配置匹配");
        }
        
        return reasons;
    }

    /**
     * 检查容器是否匹配资产
     */
    private boolean isContainerMatchAsset(ContainerInfo container, Asset asset) {
        return calculateMatchScore(asset, container) > 0.5;
    }

    /**
     * 禁用离线主机操作
     */
    public Map<String, Object> validateHostOperation(Long hostNodeId, String operation) {
        Map<String, Object> result = new HashMap<>();
        result.put("allowed", false);
        result.put("operation", operation);
        result.put("hostNodeId", hostNodeId);
        
        try {
            Map<String, Object> hostStatus = checkHostStatus(hostNodeId);
            String status = (String) hostStatus.get("status");
            
            if ("ONLINE".equals(status)) {
                result.put("allowed", true);
                result.put("message", "操作允许执行");
            } else {
                result.put("allowed", false);
                result.put("message", "主机离线，操作被禁止");
                result.put("hostStatus", hostStatus);
                result.put("suggestedActions", Arrays.asList(
                    "检查主机连接状态",
                    "尝试重新连接主机",
                    "联系系统管理员"
                ));
            }
            
        } catch (Exception e) {
            result.put("allowed", false);
            result.put("message", "验证主机操作时发生异常");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}