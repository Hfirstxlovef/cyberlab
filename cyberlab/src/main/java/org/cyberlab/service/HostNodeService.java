package org.cyberlab.service;

import org.cyberlab.entity.HostNode;
import org.cyberlab.repository.HostNodeRepository;
import org.cyberlab.repository.DrillContainerRepository;
import org.cyberlab.util.NetworkTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HostNodeService {
    
    @Autowired
    private HostNodeRepository hostNodeRepository;
    
    @Autowired
    private DrillContainerRepository drillContainerRepository;
    
    @Autowired
    private DockerService dockerService;
    
    /**
     * 获取所有主机节点
     */
    public List<HostNode> getAllNodes() {
        return hostNodeRepository.findAll();
    }
    
    /**
     * 根据ID获取节点
     */
    public Optional<HostNode> getNodeById(Long id) {
        return hostNodeRepository.findById(id);
    }
    
    /**
     * 根据名称获取节点
     */
    public Optional<HostNode> getNodeByName(String name) {
        return hostNodeRepository.findByName(name);
    }
    
    /**
     * 创建新节点
     */
    @Transactional
    public HostNode createNode(HostNode node) {
        // 检查名称是否已存在
        if (hostNodeRepository.findByName(node.getName()).isPresent()) {
            throw new RuntimeException("节点名称已存在: " + node.getName());
        }
        
        // 检查IP是否已存在
        if (hostNodeRepository.findByHostIp(node.getHostIp()).isPresent()) {
            throw new RuntimeException("主机IP已存在: " + node.getHostIp());
        }
        
        return hostNodeRepository.save(node);
    }
    
    /**
     * 更新节点信息
     */
    @Transactional
    public HostNode updateNode(Long id, HostNode nodeUpdate) {
        HostNode existingNode = hostNodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        // 检查名称冲突（除了当前节点）
        Optional<HostNode> nodeWithSameName = hostNodeRepository.findByName(nodeUpdate.getName());
        if (nodeWithSameName.isPresent() && !nodeWithSameName.get().getId().equals(id)) {
            throw new RuntimeException("节点名称已存在: " + nodeUpdate.getName());
        }
        
        // 更新字段
        existingNode.setName(nodeUpdate.getName());
        existingNode.setDisplayName(nodeUpdate.getDisplayName());
        existingNode.setHostIp(nodeUpdate.getHostIp());
        existingNode.setDockerPort(nodeUpdate.getDockerPort());
        existingNode.setSshPort(nodeUpdate.getSshPort());
        existingNode.setClusterName(nodeUpdate.getClusterName());
        existingNode.setNodeType(nodeUpdate.getNodeType());
        existingNode.setEnvironment(nodeUpdate.getEnvironment());
        existingNode.setDescription(nodeUpdate.getDescription());
        existingNode.setCpuCores(nodeUpdate.getCpuCores());
        existingNode.setMemoryTotal(nodeUpdate.getMemoryTotal());
        existingNode.setDiskTotal(nodeUpdate.getDiskTotal());
        existingNode.setDockerTlsEnabled(nodeUpdate.getDockerTlsEnabled());
        existingNode.setDockerCertPath(nodeUpdate.getDockerCertPath());
        existingNode.setSshUsername(nodeUpdate.getSshUsername());
        existingNode.setSshKeyPath(nodeUpdate.getSshKeyPath());
        existingNode.setMonitoringEnabled(nodeUpdate.getMonitoringEnabled());
        existingNode.setMaxContainers(nodeUpdate.getMaxContainers());
        existingNode.setPriority(nodeUpdate.getPriority());
        existingNode.setLabels(nodeUpdate.getLabels());
        existingNode.setMetadata(nodeUpdate.getMetadata());
        
        return hostNodeRepository.save(existingNode);
    }
    
    /**
     * 删除节点
     */
    @Transactional
    public void deleteNode(Long id) {
        HostNode node = hostNodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        // 检查是否有关联的容器
        // 这里可以添加检查逻辑，确保删除前没有运行中的容器
        
        hostNodeRepository.delete(node);
    }
    
    /**
     * 更新节点状态
     */
    @Transactional
    public HostNode updateNodeStatus(Long id, String status) {
        HostNode node = hostNodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        node.setStatus(status);
        node.setLastHealthCheck(LocalDateTime.now());
        
        return hostNodeRepository.save(node);
    }
    
    /**
     * 获取活跃节点列表
     */
    public List<HostNode> getActiveNodes() {
        return hostNodeRepository.findByStatusOrderByPriorityDesc("active");
    }
    
    /**
     * 获取可用于部署的节点
     */
    public List<HostNode> getAvailableNodesForDeployment() {
        return hostNodeRepository.findAvailableNodesForDeployment();
    }
    
    /**
     * 根据环境类型获取节点
     */
    public List<HostNode> getNodesByEnvironment(String environment) {
        return hostNodeRepository.findByEnvironment(environment);
    }
    
    /**
     * 根据集群获取节点
     */
    public List<HostNode> getNodesByCluster(String clusterName) {
        return hostNodeRepository.findByClusterName(clusterName);
    }
    
    /**
     * 选择最佳部署节点
     */
    public Optional<HostNode> selectBestNodeForDeployment(String environment, String preferredNodeName) {
        // 如果指定了首选节点，优先使用
        if (preferredNodeName != null && !preferredNodeName.isEmpty()) {
            Optional<HostNode> preferredNode = hostNodeRepository.findByName(preferredNodeName);
            if (preferredNode.isPresent() && preferredNode.get().isActive()) {
                return preferredNode;
            }
        }
        
        // 获取环境匹配且可用的节点
        List<HostNode> availableNodes = hostNodeRepository.findAvailableNodesForDeployment()
            .stream()
            .filter(node -> environment == null || environment.equals(node.getEnvironment()))
            .collect(Collectors.toList());
        
        // 返回优先级最高的节点
        return availableNodes.isEmpty() ? Optional.empty() : Optional.of(availableNodes.get(0));
    }
    
    /**
     * 获取节点状态统计
     */
    public Map<String, Long> getNodeStatusStatistics() {
        List<Object[]> stats = hostNodeRepository.countNodesByStatus();
        return stats.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
    }
    
    /**
     * 测试节点连接
     */
    public boolean testNodeConnection(Long nodeId) {
        HostNode node = hostNodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        
        try {
            // 使用NetworkTestUtil进行连接测试
            NetworkTestUtil.NetworkDiagnosticResult result = NetworkTestUtil.diagnoseConnection(
                node.getHostIp(), node.getDockerPort()
            );
            
            // 如果Docker API可用或TCP连接成功，则认为连接正常
            return result.isDockerApiSuccess() || result.isTcpConnectionSuccess();
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行节点健康检查
     */
    @Transactional
    public boolean performHealthCheck(Long nodeId) {
        HostNode node = hostNodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        
        try {
            // 使用NetworkTestUtil进行综合诊断
            NetworkTestUtil.NetworkDiagnosticResult result = NetworkTestUtil.diagnoseConnection(
                node.getHostIp(), node.getDockerPort()
            );
            
            // 根据诊断结果更新节点状态
            String newStatus;
            boolean isHealthy = false;
            
            // 确保result有正确的overallStatus值
            if (result.isDockerApiSuccess()) {
                newStatus = "active";
                isHealthy = true;
                result.setOverallStatus("SUCCESS");
            } else if (result.isTcpConnectionSuccess()) {
                newStatus = "docker_unavailable";
                isHealthy = false;
                result.setOverallStatus("PARTIAL");
            } else if (result.isPingSuccess()) {
                newStatus = "ping_ok_docker_fail";
                isHealthy = false;
                result.setOverallStatus("NETWORK_ISSUE");
            } else {
                newStatus = "error";
                isHealthy = false;
                result.setOverallStatus("FAILED");
            }
            
            node.setStatus(newStatus);
            node.setLastHealthCheck(LocalDateTime.now());
            hostNodeRepository.save(node);
            
            // 记录健康检查结果
            // Debug statement removed            
            return isHealthy;
            
        } catch (Exception e) {
            // 健康检查失败，设置为错误状态
            node.setStatus("error");
            node.setLastHealthCheck(LocalDateTime.now());
            hostNodeRepository.save(node);
            
            // Debug statement removed
            return false;
        }
    }
    /**
     * 批量执行所有节点健康检查并自动激活可用节点
     */
    @Transactional
    public Map<String, Object> performBatchHealthCheckAndAutoActivate() {
        List<HostNode> allNodes = getAllNodes();
        Map<String, Object> result = new HashMap<>();
        
        int totalNodes = allNodes.size();
        int activatedNodes = 0;
        int failedNodes = 0;
        List<String> activatedNodeNames = new ArrayList<>();
        List<String> failedNodeNames = new ArrayList<>();
        
        for (HostNode node : allNodes) {
            boolean isHealthy = false;
            int retryCount = 3;
            int attempts = 0;
            
            while (attempts < retryCount && !isHealthy) {
                try {
                    attempts++;
                    isHealthy = performHealthCheck(node.getId());
                    if (isHealthy) {
                        activatedNodes++;
                        activatedNodeNames.add(node.getDisplayName());
                        break;
                    } else if (attempts < retryCount) {
                        Thread.sleep(1000 * attempts); // Exponential backoff
                    }
                } catch (Exception e) {
                    if (attempts >= retryCount) {
                        failedNodes++;
                        failedNodeNames.add(node.getDisplayName() + " (异常: " + e.getMessage() + ")");
                    } else {
                        try {
                            Thread.sleep(1000 * attempts); // Exponential backoff on exception
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (!isHealthy && attempts >= retryCount) {
                failedNodes++;
                if (!failedNodeNames.stream().anyMatch(name -> name.startsWith(node.getDisplayName()))) {
                    failedNodeNames.add(node.getDisplayName() + " (重试" + retryCount + "次后失败)");
                }
            }
        }
        
        result.put("totalNodes", totalNodes);
        result.put("activatedNodes", activatedNodes);
        result.put("failedNodes", failedNodes);
        result.put("activatedNodeNames", activatedNodeNames);
        result.put("failedNodeNames", failedNodeNames);
        result.put("message", String.format("批量健康检查完成：%d个节点中有%d个成功激活，%d个失败", 
                                           totalNodes, activatedNodes, failedNodes));
        
        return result;
    }

    // ========== 新增：节点负载查询和推荐算法 ==========

    /**
     * 获取节点负载信息
     */
    public Map<String, Object> getNodeLoadInfo(Long nodeId) {
        HostNode node = hostNodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        
        // 实时从Docker API获取容器统计信息
        long totalContainers = 0;
        long runningContainers = 0;
        long stoppedContainers = 0;
        long failedContainers = 0;
        
        try {
            // 使用DockerService获取实时容器信息
            List<org.cyberlab.entity.ContainerInfo> containers = dockerService.getContainersOnNode(nodeId);
            if (containers != null) {
                totalContainers = containers.size();
                runningContainers = containers.stream()
                    .filter(c -> "running".equalsIgnoreCase(c.getStatus()))
                    .count();
                stoppedContainers = containers.stream()
                    .filter(c -> "exited".equalsIgnoreCase(c.getStatus()) || "stopped".equalsIgnoreCase(c.getStatus()))
                    .count();
                failedContainers = containers.stream()
                    .filter(c -> "dead".equalsIgnoreCase(c.getStatus()) || "error".equalsIgnoreCase(c.getStatus()))
                    .count();
            }
        } catch (Exception e) {
            // Debug statement removed
            // 降级到数据库统计
            totalContainers = drillContainerRepository.countByHostNodeId(nodeId);
            runningContainers = drillContainerRepository.countByHostNodeIdAndStatus(nodeId, "running");
            stoppedContainers = drillContainerRepository.countByHostNodeIdAndStatus(nodeId, "stopped");
            failedContainers = drillContainerRepository.countByHostNodeIdAndStatus(nodeId, "failed");
        }
        
        // 计算负载率
        int maxContainers = node.getMaxContainers() != null ? node.getMaxContainers() : 50;
        double loadRatio = (double) totalContainers / maxContainers;
        
        // 计算可用容量
        int availableSlots = Math.max(0, maxContainers - (int) totalContainers);
        
        // 计算节点评分
        double nodeScore = calculateNodeLoadScore(node, loadRatio);
        
        Map<String, Object> loadInfo = new HashMap<>();
        loadInfo.put("nodeId", nodeId);
        loadInfo.put("nodeName", node.getDisplayName());
        loadInfo.put("status", node.getStatus());
        loadInfo.put("totalContainers", totalContainers);
        loadInfo.put("runningContainers", runningContainers);
        loadInfo.put("stoppedContainers", stoppedContainers);
        loadInfo.put("failedContainers", failedContainers);
        loadInfo.put("maxContainers", maxContainers);
        loadInfo.put("loadRatio", Math.round(loadRatio * 100.0) / 100.0);
        loadInfo.put("loadPercentage", Math.round(loadRatio * 100));
        loadInfo.put("availableSlots", availableSlots);
        loadInfo.put("nodeScore", Math.round(nodeScore * 100.0) / 100.0);
        loadInfo.put("priority", node.getPriority());
        loadInfo.put("environment", node.getEnvironment());
        loadInfo.put("nodeType", node.getNodeType());
        loadInfo.put("lastHealthCheck", node.getLastHealthCheck());
        
        // 资源信息
        if (node.getCpuCores() != null) {
            loadInfo.put("cpuCores", node.getCpuCores());
        }
        if (node.getMemoryTotal() != null) {
            loadInfo.put("memoryTotal", node.getMemoryTotal());
        }
        if (node.getDiskTotal() != null) {
            loadInfo.put("diskTotal", node.getDiskTotal());
        }
        
        return loadInfo;
    }
    
    /**
     * 获取所有节点的负载信息
     */
    public List<Map<String, Object>> getAllNodesLoadInfo() {
        List<HostNode> allNodes = hostNodeRepository.findAll();
        return allNodes.stream()
            .map(node -> getNodeLoadInfo(node.getId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取活跃节点的负载信息（按负载排序）
     */
    public List<Map<String, Object>> getActiveNodesLoadInfoSorted() {
        List<HostNode> activeNodes = getActiveNodes();
        return activeNodes.stream()
            .map(node -> getNodeLoadInfo(node.getId()))
            .sorted((a, b) -> {
                Double scoreA = (Double) a.get("nodeScore");
                Double scoreB = (Double) b.get("nodeScore");
                return Double.compare(scoreB, scoreA); // 高分在前
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 智能推荐最佳部署节点
     */
    public List<HostNode> recommendDeploymentNodes(String environment, int count) {
        List<HostNode> availableNodes = getAvailableNodesForDeployment();
        
        // 过滤环境匹配的节点
        if (environment != null && !environment.trim().isEmpty()) {
            availableNodes = availableNodes.stream()
                .filter(node -> environment.equals(node.getEnvironment()))
                .collect(Collectors.toList());
        }
        
        // 计算每个节点的综合评分
        List<NodeWithScore> scoredNodes = availableNodes.stream()
            .map(node -> {
                long totalContainers = 0;
                try {
                    // 实时从Docker API获取容器统计信息
                    List<org.cyberlab.entity.ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());
                    if (containers != null) {
                        totalContainers = containers.size();
                    }
                } catch (Exception e) {
                    // 降级到数据库统计
                    totalContainers = drillContainerRepository.countByHostNodeId(node.getId());
                }
                
                int maxContainers = node.getMaxContainers() != null ? node.getMaxContainers() : 50;
                double loadRatio = (double) totalContainers / maxContainers;
                double score = calculateNodeLoadScore(node, loadRatio);
                return new NodeWithScore(node, score);
            })
            .sorted((a, b) -> Double.compare(b.score, a.score)) // 按分数降序排列
            .collect(Collectors.toList());
        
        // 返回前N个推荐节点
        return scoredNodes.stream()
            .limit(count)
            .map(ns -> ns.node)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取负载均衡推荐节点
     */
    public Optional<HostNode> getLoadBalancedNode(String environment) {
        List<HostNode> candidates = recommendDeploymentNodes(environment, 5);
        
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        
        // 从前5个候选节点中选择负载最低的
        HostNode bestNode = candidates.get(0);
        long minLoad = 0;
        
        try {
            // 实时从Docker API获取容器统计信息
            List<org.cyberlab.entity.ContainerInfo> containers = dockerService.getContainersOnNode(bestNode.getId());
            if (containers != null) {
                minLoad = containers.size();
            }
        } catch (Exception e) {
            // 降级到数据库统计
            minLoad = drillContainerRepository.countByHostNodeId(bestNode.getId());
        }
        
        for (int i = 1; i < candidates.size(); i++) {
            HostNode candidate = candidates.get(i);
            long candidateLoad = 0;
            
            try {
                // 实时从Docker API获取容器统计信息
                List<org.cyberlab.entity.ContainerInfo> containers = dockerService.getContainersOnNode(candidate.getId());
                if (containers != null) {
                    candidateLoad = containers.size();
                }
            } catch (Exception e) {
                // 降级到数据库统计
                candidateLoad = drillContainerRepository.countByHostNodeId(candidate.getId());
            }
            
            if (candidateLoad < minLoad) {
                minLoad = candidateLoad;
                bestNode = candidate;
            }
        }
        
        return Optional.of(bestNode);
    }
    
    /**
     * 计算节点负载评分
     */
    private double calculateNodeLoadScore(HostNode node, double loadRatio) {
        double score = 0;
        
        // 基础优先级分数 (0-50分)
        int priority = node.getPriority() != null ? node.getPriority() : 1;
        score += Math.min(priority * 10, 50);
        
        // 负载分数 (0-30分，负载越低分数越高)
        score += (1 - Math.min(loadRatio, 1.0)) * 30;
        
        // 节点类型分数 (0-10分)
        if ("local".equals(node.getNodeType())) {
            score += 10;
        } else if ("dedicated".equals(node.getNodeType())) {
            score += 8;
        } else if ("shared".equals(node.getNodeType())) {
            score += 5;
        }
        
        // 环境分数 (0-10分)
        if ("production".equals(node.getEnvironment())) {
            score += 10;
        } else if ("staging".equals(node.getEnvironment())) {
            score += 8;
        } else if ("development".equals(node.getEnvironment())) {
            score += 6;
        }
        
        // 健康状态分数 (0-20分)
        if ("active".equals(node.getStatus()) || "healthy".equals(node.getStatus())) {
            score += 20;
        } else if ("warning".equals(node.getStatus())) {
            score += 10;
        } else {
            score += 0; // error, offline等状态不加分
        }
        
        // 最近健康检查时间分数 (0-5分)
        if (node.getLastHealthCheck() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastCheck = node.getLastHealthCheck();
            long minutesSinceCheck = java.time.Duration.between(lastCheck, now).toMinutes();
            
            if (minutesSinceCheck <= 5) {
                score += 5; // 5分钟内检查过
            } else if (minutesSinceCheck <= 30) {
                score += 3; // 30分钟内检查过
            } else if (minutesSinceCheck <= 60) {
                score += 1; // 1小时内检查过
            }
        }
        
        return Math.min(score, 100); // 最高100分
    }
    
    /**
     * 获取节点容量预警信息
     */
    public List<Map<String, Object>> getNodeCapacityAlerts() {
        List<HostNode> allNodes = getAllNodes();
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        for (HostNode node : allNodes) {
            long totalContainers = 0;
            
            try {
                // 实时从Docker API获取容器统计信息
                List<org.cyberlab.entity.ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());
                if (containers != null) {
                    totalContainers = containers.size();
                }
            } catch (Exception e) {
                // 降级到数据库统计
                totalContainers = drillContainerRepository.countByHostNodeId(node.getId());
            }
            
            int maxContainers = node.getMaxContainers() != null ? node.getMaxContainers() : 50;
            double loadRatio = (double) totalContainers / maxContainers;
            
            Map<String, Object> alert = new HashMap<>();
            alert.put("nodeId", node.getId());
            alert.put("nodeName", node.getDisplayName());
            alert.put("loadRatio", Math.round(loadRatio * 100.0) / 100.0);
            alert.put("totalContainers", totalContainers);
            alert.put("maxContainers", maxContainers);
            
            // 根据负载率确定警告级别
            if (loadRatio >= 0.9) {
                alert.put("alertLevel", "critical");
                alert.put("message", "节点容量严重不足");
                alerts.add(alert);
            } else if (loadRatio >= 0.8) {
                alert.put("alertLevel", "warning");
                alert.put("message", "节点容量紧张");
                alerts.add(alert);
            } else if (loadRatio >= 0.7) {
                alert.put("alertLevel", "info");
                alert.put("message", "节点容量较高");
                alerts.add(alert);
            }
        }
        
        return alerts;
    }
    
    /**
     * 获取集群负载统计
     */
    public Map<String, Object> getClusterLoadStatistics() {
        List<HostNode> allNodes = getAllNodes();
        Map<String, Object> stats = new HashMap<>();
        
        // 总体统计
        long totalNodes = allNodes.size();
        long activeNodes = allNodes.stream().filter(node -> "active".equals(node.getStatus())).count();
        
        // 实时从Docker API获取容器统计信息
        long totalContainers = 0;
        long runningContainers = 0;
        
        try {
            for (HostNode node : allNodes) {
                if ("active".equals(node.getStatus())) {
                    List<org.cyberlab.entity.ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());
                    if (containers != null) {
                        totalContainers += containers.size();
                        runningContainers += containers.stream()
                            .filter(c -> "running".equalsIgnoreCase(c.getStatus()))
                            .count();
                    }
                }
            }
        } catch (Exception e) {
            // Debug statement removed
            // 降级到数据库统计
            totalContainers = drillContainerRepository.count();
            runningContainers = drillContainerRepository.countByStatus("running");
        }
        
        // 计算平均负载
        double totalCapacity = allNodes.stream()
            .mapToInt(node -> node.getMaxContainers() != null ? node.getMaxContainers() : 50)
            .sum();
        double overallLoadRatio = totalCapacity > 0 ? totalContainers / totalCapacity : 0;
        
        stats.put("totalNodes", totalNodes);
        stats.put("activeNodes", activeNodes);
        stats.put("inactiveNodes", totalNodes - activeNodes);
        stats.put("totalContainers", totalContainers);
        stats.put("runningContainers", runningContainers);
        stats.put("totalCapacity", (int) totalCapacity);
        stats.put("overallLoadRatio", Math.round(overallLoadRatio * 100.0) / 100.0);
        stats.put("overallLoadPercentage", Math.round(overallLoadRatio * 100));
        stats.put("availableCapacity", (int) (totalCapacity - totalContainers));
        
        // 按环境分组统计
        Map<String, Long> environmentStats = allNodes.stream()
            .filter(node -> node.getEnvironment() != null)
            .collect(Collectors.groupingBy(
                HostNode::getEnvironment,
                Collectors.counting()
            ));
        stats.put("environmentDistribution", environmentStats);
        
        // 获取容量预警
        List<Map<String, Object>> alerts = getNodeCapacityAlerts();
        stats.put("capacityAlerts", alerts);
        stats.put("alertCount", alerts.size());
        
        return stats;
    }
    
    /**
     * 系统启动时初始化节点
     */
    public void initializeNodesOnStartup() {
        try {
            // 执行批量健康检查并自动激活可用节点
            performBatchHealthCheckAndAutoActivate();
        } catch (Exception e) {
            // 初始化失败不应阻止系统启动
            // Debug statement removed
        }
    }
    
    /**
     * 强制激活指定节点（如果健康）
     */
    public boolean forceActivateNodeIfHealthy(String hostIp, int dockerPort) {
        try {
            Optional<HostNode> nodeOpt = hostNodeRepository.findByHostIp(hostIp);
            if (nodeOpt.isPresent()) {
                HostNode node = nodeOpt.get();
                if (node.getDockerPort() == dockerPort) {
                    boolean isHealthy = performHealthCheck(node.getId());
                    if (isHealthy) {
                        updateNodeStatus(node.getId(), "active");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Debug statement removed
        }
        return false;
    }
    
    /**
     * 强制激活指定节点（如果健康）- 重载方法
     */
    public boolean forceActivateNodeIfHealthy(String hostIp, Integer dockerPort) {
        return forceActivateNodeIfHealthy(hostIp, dockerPort != null ? dockerPort : 2375);
    }
    
    /**
     * 节点评分辅助类
     */
    private static class NodeWithScore {
        final HostNode node;
        final double score;
        
        NodeWithScore(HostNode node, double score) {
            this.node = node;
            this.score = score;
        }
    }
}