package org.cyberlab.service;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.DrillContainer;
import org.cyberlab.entity.HostNode;
import org.cyberlab.repository.DrillContainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目感知容器传输服务
 * 提供基于项目拓扑的智能容器部署、迁移和优化功能
 */
@Service
@Transactional
public class ProjectAwareContainerTransferService {

    @Autowired
    private AssetService assetService;
    
    @Autowired
    private DrillContainerService drillContainerService;
    
    @Autowired
    private HostNodeService hostNodeService;
    
    @Autowired
    private DrillContainerRepository drillContainerRepository;
    
    /**
     * 基于项目拓扑智能部署容器组
     * 考虑网络亲和性、负载均衡和资源约束
     */
    public Map<String, Object> deployContainerGroupForProject(String projectId, Long rangeId, String environment) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取项目内的容器资产
        List<Asset> containerAssets = assetService.getActiveContainerAssetsByProjectId(projectId);
        
        if (containerAssets.isEmpty()) {
            result.put("success", false);
            result.put("message", "项目内没有可部署的容器资产");
            return result;
        }
        
        // 分析项目拓扑
        Map<String, Object> topologyAnalysis = analyzeProjectTopology(projectId, containerAssets);
        result.put("topologyAnalysis", topologyAnalysis);
        
        // 生成部署计划
        Map<String, Object> deploymentPlan = generateSmartDeploymentPlan(containerAssets, environment);
        result.put("deploymentPlan", deploymentPlan);
        
        // 执行部署
        List<Map<String, Object>> deploymentResults = executeDeploymentPlan(rangeId, deploymentPlan);
        result.put("deploymentResults", deploymentResults);
        
        // 统计结果
        long successCount = deploymentResults.stream()
            .mapToLong(r -> (Boolean) r.get("success") ? 1L : 0L)
            .sum();
        
        result.put("success", true);
        result.put("projectId", projectId);
        result.put("environment", environment);
        result.put("totalAssets", containerAssets.size());
        result.put("successfulDeployments", successCount);
        result.put("failedDeployments", containerAssets.size() - successCount);
        result.put("deploymentTime", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 分析项目拓扑结构
     */
    private Map<String, Object> analyzeProjectTopology(String projectId, List<Asset> assets) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 网络分组分析
        Map<String, List<Asset>> networkGroups = assets.stream()
            .collect(Collectors.groupingBy(asset -> {
                if (asset.getIp() != null) {
                    String[] parts = asset.getIp().split("\\.");
                    if (parts.length >= 3) {
                        return parts[0] + "." + parts[1] + "." + parts[2];
                    }
                }
                return "unknown";
            }));
        
        analysis.put("networkGroups", networkGroups);
        analysis.put("networkSegments", networkGroups.size());
        
        // 部署策略分析
        Map<String, Long> strategyDistribution = assets.stream()
            .collect(Collectors.groupingBy(
                asset -> asset.getDeploymentStrategy() != null ? asset.getDeploymentStrategy() : "default",
                Collectors.counting()
            ));
        
        analysis.put("strategyDistribution", strategyDistribution);
        
        // 节点亲和性分析
        Map<Long, Long> nodeAffinity = assets.stream()
            .filter(asset -> asset.getPreferredHostNodeId() != null)
            .collect(Collectors.groupingBy(
                Asset::getPreferredHostNodeId,
                Collectors.counting()
            ));
        
        analysis.put("nodeAffinity", nodeAffinity);
        
        // 依赖关系分析
        List<Map<String, Object>> dependencies = assets.stream()
            .filter(asset -> asset.getAffinityRules() != null && !asset.getAffinityRules().trim().isEmpty())
            .map(asset -> {
                Map<String, Object> dep = new HashMap<>();
                dep.put("assetId", asset.getId());
                dep.put("assetName", asset.getName());
                dep.put("affinityRules", asset.getAffinityRules());
                dep.put("antiAffinityRules", asset.getAntiAffinityRules());
                return dep;
            })
            .collect(Collectors.toList());
        
        analysis.put("dependencies", dependencies);
        analysis.put("hasDependencies", !dependencies.isEmpty());
        
        return analysis;
    }
    
    /**
     * 生成智能部署计划
     */
    private Map<String, Object> generateSmartDeploymentPlan(List<Asset> assets, String environment) {
        Map<String, Object> plan = new HashMap<>();
        
        // 获取可用节点
        List<HostNode> availableNodes = hostNodeService.getAvailableNodesForDeployment();
        if (environment != null && !environment.trim().isEmpty()) {
            availableNodes = availableNodes.stream()
                .filter(node -> environment.equals(node.getEnvironment()))
                .collect(Collectors.toList());
        }
        
        plan.put("availableNodes", availableNodes.size());
        
        // 为每个资产分配最优节点
        Map<String, Object> assetNodeMapping = new HashMap<>();
        Map<Long, Integer> nodeLoadCounter = new HashMap<>();
        
        // 初始化节点负载计数器
        for (HostNode node : availableNodes) {
            Long currentLoad = drillContainerRepository.countByHostNodeId(node.getId());
            nodeLoadCounter.put(node.getId(), currentLoad.intValue());
        }
        
        for (Asset asset : assets) {
            HostNode selectedNode = selectOptimalNodeForAsset(asset, availableNodes, nodeLoadCounter);
            
            if (selectedNode != null) {
                Map<String, Object> mapping = new HashMap<>();
                mapping.put("asset", asset);
                mapping.put("node", selectedNode);
                mapping.put("deploymentStrategy", asset.getDeploymentStrategy());
                mapping.put("selectionReason", getNodeSelectionReason(asset, selectedNode));
                
                assetNodeMapping.put(asset.getId().toString(), mapping);
                
                // 更新节点负载计数
                nodeLoadCounter.put(selectedNode.getId(), nodeLoadCounter.get(selectedNode.getId()) + 1);
            }
        }
        
        plan.put("assetNodeMapping", assetNodeMapping);
        plan.put("planGenerated", true);
        plan.put("planTime", System.currentTimeMillis());
        
        return plan;
    }
    
    /**
     * 为资产选择最优节点
     */
    private HostNode selectOptimalNodeForAsset(Asset asset, List<HostNode> availableNodes, Map<Long, Integer> nodeLoadCounter) {
        if (availableNodes.isEmpty()) return null;
        
        // 固定策略：使用指定节点
        if ("fixed".equals(asset.getDeploymentStrategy()) && asset.getPreferredHostNodeId() != null) {
            return availableNodes.stream()
                .filter(node -> node.getId().equals(asset.getPreferredHostNodeId()))
                .findFirst()
                .orElse(null);
        }
        
        // 负载均衡策略：选择负载最低的节点
        if ("load_balanced".equals(asset.getDeploymentStrategy())) {
            return availableNodes.stream()
                .min(Comparator.comparing(node -> {
                    int currentLoad = nodeLoadCounter.getOrDefault(node.getId(), 0);
                    int maxCapacity = node.getMaxContainers() != null ? node.getMaxContainers() : 50;
                    return (double) currentLoad / maxCapacity;
                }))
                .orElse(null);
        }
        
        // 智能选择：综合考虑多个因素
        return availableNodes.stream()
            .max(Comparator.comparing(node -> calculateNodeScore(asset, node, nodeLoadCounter)))
            .orElse(null);
    }
    
    /**
     * 计算节点适配度分数
     */
    private double calculateNodeScore(Asset asset, HostNode node, Map<Long, Integer> nodeLoadCounter) {
        double score = 0;
        
        // 负载评分 (40%)
        int currentLoad = nodeLoadCounter.getOrDefault(node.getId(), 0);
        int maxCapacity = node.getMaxContainers() != null ? node.getMaxContainers() : 50;
        double loadRatio = (double) currentLoad / maxCapacity;
        score += (1.0 - Math.min(loadRatio, 1.0)) * 40;
        
        // 优先级评分 (20%)
        int priority = node.getPriority() != null ? node.getPriority() : 1;
        score += Math.min(priority * 4, 20);
        
        // 网络亲和性评分 (25%)
        if (asset.getIp() != null && node.getHostIp() != null) {
            String assetSubnet = extractSubnet(asset.getIp());
            String nodeSubnet = extractSubnet(node.getHostIp());
            if (assetSubnet.equals(nodeSubnet)) {
                score += 25;
            } else if (isCompatibleNetwork(asset.getIp(), node.getHostIp())) {
                score += 15;
            } else {
                score += 5;
            }
        }
        
        // 环境匹配评分 (15%)
        if (node.getEnvironment() != null) {
            if ("production".equals(node.getEnvironment())) {
                score += 15;
            } else if ("testing".equals(node.getEnvironment())) {
                score += 10;
            }
        }
        
        return score;
    }
    
    /**
     * 提取IP子网
     */
    private String extractSubnet(String ip) {
        if (ip == null || ip.isEmpty()) return "";
        String[] parts = ip.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return ip;
    }
    
    /**
     * 判断网络兼容性
     */
    private boolean isCompatibleNetwork(String assetIp, String nodeIp) {
        String assetSubnet = extractSubnet(assetIp);
        String nodeSubnet = extractSubnet(nodeIp);
        
        // 特殊映射规则
        if ("192.168.1".equals(assetSubnet) && "172.16.190".equals(nodeSubnet)) return true;
        if ("192.168.2".equals(assetSubnet) && "172.16.191".equals(nodeSubnet)) return true;
        
        // 私有网络兼容
        return isPrivateNetwork(assetIp) && isPrivateNetwork(nodeIp);
    }
    
    /**
     * 判断是否为私有网络
     */
    private boolean isPrivateNetwork(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        String[] parts = ip.split("\\.");
        if (parts.length < 2) return false;
        
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            if (first == 10) return true;
            if (first == 172 && second >= 16 && second <= 31) return true;
            if (first == 192 && second == 168) return true;
        } catch (NumberFormatException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * 获取节点选择原因
     */
    private String getNodeSelectionReason(Asset asset, HostNode node) {
        List<String> reasons = new ArrayList<>();
        
        if ("fixed".equals(asset.getDeploymentStrategy())) {
            reasons.add("固定策略");
        } else if ("load_balanced".equals(asset.getDeploymentStrategy())) {
            reasons.add("负载均衡");
        }
        
        if (asset.getIp() != null && node.getHostIp() != null) {
            String assetSubnet = extractSubnet(asset.getIp());
            String nodeSubnet = extractSubnet(node.getHostIp());
            if (assetSubnet.equals(nodeSubnet)) {
                reasons.add("同网段");
            }
        }
        
        if (node.getPriority() != null && node.getPriority() > 3) {
            reasons.add("高优先级");
        }
        
        return reasons.isEmpty() ? "智能选择" : String.join(" + ", reasons);
    }
    
    /**
     * 执行部署计划
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executeDeploymentPlan(Long rangeId, Map<String, Object> plan) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        Map<String, Object> assetNodeMapping = (Map<String, Object>) plan.get("assetNodeMapping");
        
        for (Map.Entry<String, Object> entry : assetNodeMapping.entrySet()) {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> mapping = (Map<String, Object>) entry.getValue();
            Asset asset = (Asset) mapping.get("asset");
            HostNode node = (HostNode) mapping.get("node");
            
            try {
                // 创建容器
                DrillContainer container = drillContainerService.createContainerFromAsset(rangeId, asset.getId());
                
                // 设置部署节点
                container.setHostNodeId(node.getId());
                container.setHostNodeName(node.getDisplayName());
                container.setHostNodeIp(node.getHostIp());
                
                // 保存容器配置
                drillContainerRepository.save(container);
                
                result.put("success", true);
                result.put("assetId", asset.getId());
                result.put("assetName", asset.getName());
                result.put("containerId", container.getId());
                result.put("containerName", container.getName());
                result.put("nodeId", node.getId());
                result.put("nodeName", node.getDisplayName());
                result.put("selectionReason", mapping.get("selectionReason"));
                
            } catch (Exception e) {
                result.put("success", false);
                result.put("assetId", asset.getId());
                result.put("assetName", asset.getName());
                result.put("error", e.getMessage());
            }
            
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 智能容器迁移
     * 将容器从一个节点迁移到另一个节点
     */
    public Map<String, Object> migrateContainer(Long containerId, Long targetNodeId, boolean forceRebalance) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "容器不存在");
            return result;
        }
        
        DrillContainer container = containerOpt.get();
        Optional<HostNode> targetNodeOpt = hostNodeService.getNodeById(targetNodeId);
        if (targetNodeOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "目标节点不存在");
            return result;
        }
        
        HostNode targetNode = targetNodeOpt.get();
        Long originalNodeId = container.getHostNodeId();
        
        try {
            // 检查目标节点是否可用
            if (!targetNode.isActive()) {
                throw new RuntimeException("目标节点不可用");
            }
            
            // 检查目标节点负载
            long currentLoad = drillContainerRepository.countByHostNodeId(targetNodeId);
            int maxCapacity = targetNode.getMaxContainers() != null ? targetNode.getMaxContainers() : 50;
            if (currentLoad >= maxCapacity && !forceRebalance) {
                throw new RuntimeException("目标节点负载已满");
            }
            
            // 停止原容器
            if ("running".equals(container.getStatus()) && container.getContainerId() != null) {
                drillContainerService.stopContainer(containerId);
            }
            
            // 更新容器节点信息
            container.setHostNodeId(targetNodeId);
            container.setHostNodeName(targetNode.getDisplayName());
            container.setHostNodeIp(targetNode.getHostIp());
            container.setStatus("migrating");
            // 记录迁移时间可以通过更新updatedAt字段或添加日志
            drillContainerRepository.save(container);
            
            // 在目标节点重新部署
            String deployResult = drillContainerService.deployContainerToNode(container, targetNode);
            
            if (deployResult.contains("成功")) {
                container.setStatus("running");
                result.put("success", true);
                result.put("message", "容器迁移成功");
            } else {
                container.setStatus("failed");
                result.put("success", false);
                result.put("message", "迁移部署失败: " + deployResult);
            }
            
            drillContainerRepository.save(container);
            
            result.put("containerId", containerId);
            result.put("containerName", container.getName());
            result.put("originalNodeId", originalNodeId);
            result.put("targetNodeId", targetNodeId);
            result.put("targetNodeName", targetNode.getDisplayName());
            result.put("migrationTime", System.currentTimeMillis());
            
        } catch (Exception e) {
            container.setStatus("migration_failed");
            drillContainerRepository.save(container);
            
            result.put("success", false);
            result.put("message", "迁移失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 项目级容器负载重平衡
     */
    public Map<String, Object> rebalanceProjectContainers(String projectId, String environment) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取项目内的所有容器
        List<Asset> projectAssets = assetService.getAssetsByProjectId(projectId);
        List<Long> assetIds = projectAssets.stream().map(Asset::getId).collect(Collectors.toList());
        
        List<DrillContainer> projectContainers = new ArrayList<>();
        for (Long assetId : assetIds) {
            projectContainers.addAll(drillContainerService.getContainersByAsset(assetId));
        }
        
        if (projectContainers.isEmpty()) {
            result.put("success", false);
            result.put("message", "项目内没有容器");
            return result;
        }
        
        // 分析当前分布
        Map<String, Object> currentDistribution = analyzeContainerDistribution(projectContainers);
        result.put("currentDistribution", currentDistribution);
        
        // 生成重平衡计划
        Map<String, Object> rebalancePlan = generateRebalancePlan(projectContainers, environment);
        result.put("rebalancePlan", rebalancePlan);
        
        // 执行重平衡
        List<Map<String, Object>> migrationResults = executeMigrationPlan(rebalancePlan);
        result.put("migrationResults", migrationResults);
        
        long successfulMigrations = migrationResults.stream()
            .mapToLong(r -> (Boolean) r.get("success") ? 1L : 0L)
            .sum();
        
        result.put("success", true);
        result.put("projectId", projectId);
        result.put("totalContainers", projectContainers.size());
        result.put("successfulMigrations", successfulMigrations);
        result.put("rebalanceTime", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 分析容器分布情况
     */
    private Map<String, Object> analyzeContainerDistribution(List<DrillContainer> containers) {
        Map<String, Object> analysis = new HashMap<>();
        
        Map<Long, Long> nodeDistribution = containers.stream()
            .filter(container -> container.getHostNodeId() != null)
            .collect(Collectors.groupingBy(
                DrillContainer::getHostNodeId,
                Collectors.counting()
            ));
        
        analysis.put("nodeDistribution", nodeDistribution);
        analysis.put("totalNodes", nodeDistribution.size());
        
        if (!nodeDistribution.isEmpty()) {
            double mean = nodeDistribution.values().stream().mapToLong(Long::longValue).average().orElse(0);
            double maxLoad = nodeDistribution.values().stream().mapToLong(Long::longValue).max().orElse(0);
            double minLoad = nodeDistribution.values().stream().mapToLong(Long::longValue).min().orElse(0);
            
            analysis.put("avgLoad", mean);
            analysis.put("maxLoad", maxLoad);
            analysis.put("minLoad", minLoad);
            analysis.put("loadVariance", maxLoad - minLoad);
            analysis.put("isBalanced", (maxLoad - minLoad) <= 2);
        }
        
        return analysis;
    }
    
    /**
     * 生成重平衡计划
     */
    private Map<String, Object> generateRebalancePlan(List<DrillContainer> containers, String environment) {
        Map<String, Object> plan = new HashMap<>();
        
        // 获取可用节点
        List<HostNode> availableNodes = hostNodeService.getAvailableNodesForDeployment();
        if (environment != null && !environment.trim().isEmpty()) {
            availableNodes = availableNodes.stream()
                .filter(node -> environment.equals(node.getEnvironment()))
                .collect(Collectors.toList());
        }
        
        // 计算每个节点的当前负载
        Map<Long, Integer> currentLoads = new HashMap<>();
        for (HostNode node : availableNodes) {
            Long load = drillContainerRepository.countByHostNodeId(node.getId());
            currentLoads.put(node.getId(), load.intValue());
        }
        
        // 计算理想负载分布
        int totalContainers = containers.size();
        int nodeCount = availableNodes.size();
        int idealLoad = totalContainers / nodeCount;
        int remainder = totalContainers % nodeCount;
        
        Map<Long, Integer> targetLoads = new HashMap<>();
        for (int i = 0; i < availableNodes.size(); i++) {
            HostNode node = availableNodes.get(i);
            int target = idealLoad + (i < remainder ? 1 : 0);
            targetLoads.put(node.getId(), target);
        }
        
        // 生成迁移计划
        List<Map<String, Object>> migrations = new ArrayList<>();
        
        for (DrillContainer container : containers) {
            Long currentNodeId = container.getHostNodeId();
            if (currentNodeId == null) continue;
            
            int currentLoad = currentLoads.getOrDefault(currentNodeId, 0);
            int targetLoad = targetLoads.getOrDefault(currentNodeId, 0);
            
            // 如果当前节点负载过高，寻找负载较低的目标节点
            if (currentLoad > targetLoad) {
                Long bestTargetNodeId = findBestTargetNode(currentLoads, targetLoads);
                if (bestTargetNodeId != null && !bestTargetNodeId.equals(currentNodeId)) {
                    Map<String, Object> migration = new HashMap<>();
                    migration.put("containerId", container.getId());
                    migration.put("containerName", container.getName());
                    migration.put("sourceNodeId", currentNodeId);
                    migration.put("targetNodeId", bestTargetNodeId);
                    migration.put("reason", "负载重平衡");
                    
                    migrations.add(migration);
                    
                    // 更新负载计数
                    currentLoads.put(currentNodeId, currentLoads.get(currentNodeId) - 1);
                    currentLoads.put(bestTargetNodeId, currentLoads.getOrDefault(bestTargetNodeId, 0) + 1);
                }
            }
        }
        
        plan.put("migrations", migrations);
        plan.put("totalMigrations", migrations.size());
        plan.put("targetLoads", targetLoads);
        plan.put("planGenerated", true);
        
        return plan;
    }
    
    /**
     * 找到最佳目标节点
     */
    private Long findBestTargetNode(Map<Long, Integer> currentLoads, Map<Long, Integer> targetLoads) {
        return currentLoads.entrySet().stream()
            .filter(entry -> {
                Long nodeId = entry.getKey();
                int currentLoad = entry.getValue();
                int targetLoad = targetLoads.getOrDefault(nodeId, 0);
                return currentLoad < targetLoad;
            })
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * 执行迁移计划
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executeMigrationPlan(Map<String, Object> plan) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        List<Map<String, Object>> migrations = (List<Map<String, Object>>) plan.get("migrations");
        
        for (Map<String, Object> migration : migrations) {
            Long containerId = ((Number) migration.get("containerId")).longValue();
            Long targetNodeId = ((Number) migration.get("targetNodeId")).longValue();
            
            Map<String, Object> migrationResult = migrateContainer(containerId, targetNodeId, true);
            migrationResult.put("planReason", migration.get("reason"));
            
            results.add(migrationResult);
        }
        
        return results;
    }
    
    /**
     * 获取项目容器传输统计信息
     */
    public Map<String, Object> getProjectTransferStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取项目资产
        List<Asset> projectAssets = assetService.getAssetsByProjectId(projectId);
        List<Long> assetIds = projectAssets.stream().map(Asset::getId).collect(Collectors.toList());
        
        // 获取项目容器
        List<DrillContainer> projectContainers = new ArrayList<>();
        for (Long assetId : assetIds) {
            projectContainers.addAll(drillContainerService.getContainersByAsset(assetId));
        }
        
        // 统计信息
        stats.put("projectId", projectId);
        stats.put("totalAssets", projectAssets.size());
        stats.put("totalContainers", projectContainers.size());
        
        Map<String, Long> statusDistribution = projectContainers.stream()
            .collect(Collectors.groupingBy(
                container -> container.getStatus() != null ? container.getStatus() : "unknown",
                Collectors.counting()
            ));
        stats.put("statusDistribution", statusDistribution);
        
        Map<Long, Long> nodeDistribution = projectContainers.stream()
            .filter(container -> container.getHostNodeId() != null)
            .collect(Collectors.groupingBy(
                DrillContainer::getHostNodeId,
                Collectors.counting()
            ));
        stats.put("nodeDistribution", nodeDistribution);
        
        // 计算分布均衡度
        if (!nodeDistribution.isEmpty()) {
            double mean = nodeDistribution.values().stream().mapToLong(Long::longValue).average().orElse(0);
            double variance = nodeDistribution.values().stream()
                .mapToDouble(count -> Math.pow(count - mean, 2))
                .average()
                .orElse(0);
            double balanceScore = mean > 0 ? Math.max(0, 100 - (Math.sqrt(variance) / mean * 100)) : 100;
            stats.put("balanceScore", Math.round(balanceScore * 100.0) / 100.0);
        }
        
        stats.put("generatedAt", System.currentTimeMillis());
        
        return stats;
    }
}