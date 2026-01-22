package org.cyberlab.service;

import org.cyberlab.entity.Asset;
import org.cyberlab.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;
    
    @Autowired
    private HostNodeService hostNodeService;

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    public Page<Asset> getAllAssets(Pageable pageable) {
        return assetRepository.findAll(pageable);
    }

    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }

    public void delete(Long id) {
        assetRepository.deleteById(id);
    }

    public Asset update(Long id, Asset updated) {
        return assetRepository.findById(id).map(asset -> {
            asset.setName(updated.getName());
            asset.setIp(updated.getIp());
            asset.setCompany(updated.getCompany());
            asset.setOwner(updated.getOwner());
            asset.setVisibility(updated.getVisibility());
            asset.setTarget(updated.isTarget());
            asset.setEnabled(updated.isEnabled());
            asset.setNotes(updated.getNotes());
            
            // 添加缺失的字段更新 - 修复资产类型等字段无法保存的问题
            asset.setProject(updated.getProject());
            asset.setTopologyProjectId(updated.getTopologyProjectId());
            asset.setAssetType(updated.getAssetType());
            asset.setDockerImage(updated.getDockerImage());
            asset.setContainerPorts(updated.getContainerPorts());
            asset.setVolumes(updated.getVolumes());
            asset.setEnvironments(updated.getEnvironments());
            asset.setContainerCommand(updated.getContainerCommand());
            asset.setHealthCheckUrl(updated.getHealthCheckUrl());
            asset.setResourceLimitCpu(updated.getResourceLimitCpu());
            asset.setResourceLimitMemory(updated.getResourceLimitMemory());
            asset.setPreferredHostNodeId(updated.getPreferredHostNodeId());
            asset.setPreferredHostNodeName(updated.getPreferredHostNodeName());
            asset.setDeploymentStrategy(updated.getDeploymentStrategy());
            
            return assetRepository.save(asset);
        }).orElse(null);
    }

    public List<Asset> getAssetsByCompany(String company) {
        return assetRepository.findByCompany(company);
    }

    public List<Asset> getAssetsByOwner(String owner) {
        return assetRepository.findByOwner(owner);
    }

    public List<Asset> getAssetsByVisibility(String visibility) {
        return assetRepository.findByVisibility(visibility);
    }

    public List<Asset> getEnabledAssets() {
        return assetRepository.findByEnabledTrue();
    }

    public List<Asset> getTargetAssets() {
        return assetRepository.findByIsTargetTrue();
    }
    
    // 获取指定角色可见的资产
    public List<Asset> getVisibleAssets(String role) {
        switch (role.toLowerCase()) {
            case "red":
                // 红队可以看到标记为 "red" 或 "both" 的资产
                return assetRepository.findByVisibilityInAndEnabled(List.of("red", "both"), true);
            case "blue":
                // 蓝队可以看到标记为 "blue" 或 "both" 的资产
                return assetRepository.findByVisibilityInAndEnabled(List.of("blue", "both"), true);
            case "judge":
            case "admin":
                // 裁判和管理员可以看到所有已启用的资产
                return assetRepository.findByEnabledTrue();
            default:
                return List.of();
        }
    }

    public List<Asset> searchAssetsByKeyword(String keyword) {
        return assetRepository.searchByKeyword(keyword);
    }

    public Page<Asset> searchAssetsByKeyword(String keyword, Pageable pageable) {
        return assetRepository.searchByKeyword(keyword, pageable);
    }

    public List<Asset> getAssetsByIpRange(String ipPattern) {
        return assetRepository.findByIpContaining(ipPattern);
    }

    public Map<String, Long> getCompanyStatistics() {
        List<Object[]> results = assetRepository.countByCompany();
        return results.stream().collect(
            Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            )
        );
    }

    public Map<String, Long> getOwnerStatistics() {
        List<Object[]> results = assetRepository.countByOwner();
        return results.stream().collect(
            Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            )
        );
    }

    public long getTotalAssetsCount() {
        return assetRepository.count();
    }

    public long getEnabledAssetsCount() {
        return assetRepository.countByEnabledTrue();
    }

    public long getTargetAssetsCount() {
        return assetRepository.countByIsTargetTrue();
    }

    public boolean isIpUnique(String ip) {
        return assetRepository.findByIp(ip).isEmpty();
    }

    public boolean validateAssetConfiguration(Asset asset) {
        if (asset.getName() == null || asset.getName().trim().isEmpty()) {
            return false;
        }
        if (asset.getIp() == null || asset.getIp().trim().isEmpty()) {
            return false;
        }
        if (asset.getCompany() == null || asset.getCompany().trim().isEmpty()) {
            return false;
        }
        return true;
    }

    public void enableAsset(Long id) {
        assetRepository.findById(id).ifPresent(asset -> {
            asset.setEnabled(true);
            assetRepository.save(asset);
        });
    }

    public void disableAsset(Long id) {
        assetRepository.findById(id).ifPresent(asset -> {
            asset.setEnabled(false);
            assetRepository.save(asset);
        });
    }

    public void setAsTarget(Long id, boolean isTarget) {
        assetRepository.findById(id).ifPresent(asset -> {
            asset.setTarget(isTarget);
            assetRepository.save(asset);
        });
    }

    public List<Asset> getAssetsByTopologyProjectId(Long topologyProjectId) {
        return assetRepository.findByTopologyProjectId(topologyProjectId.toString());
    }

    public List<Asset> getAssetsWithoutTopologyProject() {
        return assetRepository.findByTopologyProjectIdIsNull();
    }

    public void assignToTopologyProject(Long assetId, Long topologyProjectId) {
        assetRepository.findById(assetId).ifPresent(asset -> {
            asset.setTopologyProjectId(topologyProjectId.toString());
            assetRepository.save(asset);
        });
    }

    public void removeFromTopologyProject(Long assetId) {
        assetRepository.findById(assetId).ifPresent(asset -> {
            asset.setTopologyProjectId(null);
            assetRepository.save(asset);
        });
    }
    
    /**
     * 容器资产管理相关方法
     */
    
    public List<Asset> getAssetsByAssetType(String assetType) {
        return assetRepository.findByAssetType(assetType);
    }
    
    public List<Asset> getAllContainerAssets() {
        return assetRepository.findAllContainerAssets();
    }
    
    public Optional<Asset> getAssetByNameAndCompany(String name, String company) {
        return assetRepository.findByNameAndCompany(name, company);
    }
    
    public boolean isAssetNameUniqueInCompany(String name, String company) {
        return assetRepository.findByNameAndCompany(name, company).isEmpty();
    }
    
    public long getContainerAssetsCount() {
        return getAllContainerAssets().size();
    }
    
    public Map<String, Long> getAssetTypeStatistics() {
        return getAllAssets().stream()
            .filter(asset -> asset.getAssetType() != null)
            .collect(Collectors.groupingBy(
                Asset::getAssetType,
                Collectors.counting()
            ));
    }
    
    /**
     * 根据项目ID获取项目内的所有资产
     * 项目ID格式：{company}｜{project}
     */
    public List<Asset> getAssetsByProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return List.of();
        }
        
        String[] parts = projectId.split("｜");
        if (parts.length != 2) {
            return List.of();
        }
        
        String company = parts[0];
        String project = parts[1];
        
        return getAllAssets().stream()
            .filter(asset -> {
                String assetCompany = asset.getCompany() != null ? asset.getCompany() : "未知企业";
                String assetProject = asset.getProject() != null ? asset.getProject() : "未分组";
                return company.equals(assetCompany) && project.equals(assetProject);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目内关联了主机节点的资产
     * 包括指定了具体节点的资产和使用"any"部署策略的资产
     */
    public List<Asset> getProjectAssetsWithHostNodes(String projectId) {
        return getAssetsByProjectId(projectId).stream()
            .filter(asset -> {
                // 有明确指定的节点ID
                if (asset.getPreferredHostNodeId() != null) {
                    return true;
                }
                // 或者使用"any"部署策略（任意节点）
                String deploymentStrategy = asset.getDeploymentStrategy();
                return "any".equals(deploymentStrategy);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目内所有资产关联的主机节点ID列表（去重）
     * 对于使用"any"策略的资产，需要在容器发现时动态分配节点
     */
    public List<Long> getProjectAssociatedNodeIds(String projectId) {
        return getProjectAssetsWithHostNodes(projectId).stream()
            .map(Asset::getPreferredHostNodeId)
            .filter(nodeId -> nodeId != null)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目内使用"any"部署策略的资产
     */
    public List<Asset> getProjectAnyStrategyAssets(String projectId) {
        return getAssetsByProjectId(projectId).stream()
            .filter(asset -> "any".equals(asset.getDeploymentStrategy()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目内有明确节点指定的资产
     */
    public List<Asset> getProjectFixedStrategyAssets(String projectId) {
        return getAssetsByProjectId(projectId).stream()
            .filter(asset -> asset.getPreferredHostNodeId() != null)
            .collect(Collectors.toList());
    }

    /**
     * 统计关联指定节点的资产数量
     */
    public long countAssetsByNodeId(Long nodeId) {
        return assetRepository.findAll().stream()
            .filter(asset -> nodeId.equals(asset.getPreferredHostNodeId()))
            .count();
    }

    /**
     * 清空指定节点的所有资产引用
     * 用于删除节点前清理关联
     */
    public int clearNodeReferences(Long nodeId) {
        List<Asset> affectedAssets = assetRepository.findAll().stream()
            .filter(asset -> nodeId.equals(asset.getPreferredHostNodeId()))
            .collect(Collectors.toList());

        for (Asset asset : affectedAssets) {
            asset.setPreferredHostNodeId(null);
            asset.setPreferredHostNodeName(null);
            // 如果部署策略是 fixed，改为 any
            if ("fixed".equals(asset.getDeploymentStrategy())) {
                asset.setDeploymentStrategy("any");
            }
            assetRepository.save(asset);
        }

        return affectedAssets.size();
    }

    /**
     * 数据验证：检查项目内资产的部署策略和节点关联一致性
     */
    public Map<String, Object> validateProjectAssetDeploymentConsistency(String projectId) {
        List<Asset> projectAssets = getAssetsByProjectId(projectId);
        Map<String, Object> validation = new HashMap<>();
        List<Map<String, Object>> inconsistentAssets = new ArrayList<>();
        
        for (Asset asset : projectAssets) {
            String deploymentStrategy = asset.getDeploymentStrategy();
            Long preferredNodeId = asset.getPreferredHostNodeId();
            
            Map<String, Object> assetInfo = new HashMap<>();
            assetInfo.put("id", asset.getId());
            assetInfo.put("name", asset.getName());
            assetInfo.put("deploymentStrategy", deploymentStrategy);
            assetInfo.put("preferredHostNodeId", preferredNodeId);
            
            boolean isInconsistent = false;
            List<String> issues = new ArrayList<>();
            
            // 检查一致性问题
            if ("fixed".equals(deploymentStrategy) && preferredNodeId == null) {
                issues.add("固定部署策略但未指定节点");
                isInconsistent = true;
            }
            
            if ("any".equals(deploymentStrategy) && preferredNodeId != null) {
                issues.add("任意节点策略但指定了特定节点");
                isInconsistent = true;
            }
            
            if (deploymentStrategy == null || deploymentStrategy.trim().isEmpty()) {
                issues.add("未设置部署策略");
                isInconsistent = true;
            }
            
            if (isInconsistent) {
                assetInfo.put("issues", issues);
                inconsistentAssets.add(assetInfo);
            }
        }
        
        validation.put("projectId", projectId);
        validation.put("totalAssets", projectAssets.size());
        validation.put("inconsistentCount", inconsistentAssets.size());
        validation.put("inconsistentAssets", inconsistentAssets);
        validation.put("validationTime", System.currentTimeMillis());
        
        return validation;
    }
    
    /**
     * 修复资产部署策略一致性问题
     * 对于deploymentStrategy为"any"但有preferredHostNodeId的资产，清空preferredHostNodeId
     */
    public Map<String, Object> fixAssetDeploymentInconsistency(String projectId) {
        List<Asset> projectAssets = getAssetsByProjectId(projectId);
        List<Asset> fixedAssets = new ArrayList<>();
        
        for (Asset asset : projectAssets) {
            boolean needsUpdate = false;
            
            // 修复"any"策略但指定了节点的问题
            if ("any".equals(asset.getDeploymentStrategy()) && asset.getPreferredHostNodeId() != null) {
                asset.setPreferredHostNodeId(null);
                needsUpdate = true;
            }
            
            // 为没有部署策略的资产设置默认策略
            if (asset.getDeploymentStrategy() == null || asset.getDeploymentStrategy().trim().isEmpty()) {
                if (asset.getPreferredHostNodeId() != null) {
                    asset.setDeploymentStrategy("fixed");
                } else {
                    asset.setDeploymentStrategy("any");
                }
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                assetRepository.save(asset);
                fixedAssets.add(asset);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("fixedCount", fixedAssets.size());
        result.put("fixedAssets", fixedAssets.stream().map(asset -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", asset.getId());
            info.put("name", asset.getName());
            info.put("deploymentStrategy", asset.getDeploymentStrategy());
            info.put("preferredHostNodeId", asset.getPreferredHostNodeId());
            return info;
        }).collect(Collectors.toList()));
        result.put("fixTime", System.currentTimeMillis());
        
        return result;
    }
    
    // ========== 智能负载均衡分发算法 ==========
    
    /**
     * 为项目内的"any"策略资产智能分配节点
     * 基于负载均衡算法自动选择最优节点，考虑业务需求匹配度
     */
    public Map<String, Object> redistributeProjectAssets(String projectId, String environment) {
        List<Asset> anyStrategyAssets = getProjectAnyStrategyAssets(projectId);
        List<org.cyberlab.entity.HostNode> availableNodes = hostNodeService.getAvailableNodesForDeployment();
        
        // 过滤环境匹配的节点
        if (environment != null && !environment.trim().isEmpty()) {
            availableNodes = availableNodes.stream()
                .filter(node -> environment.equals(node.getEnvironment()))
                .collect(Collectors.toList());
        }
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> redistributions = new ArrayList<>();
        
        if (availableNodes.isEmpty()) {
            result.put("success", false);
            result.put("message", "没有可用的部署节点");
            return result;
        }
        
        // 获取每个节点的当前负载信息
        Map<Long, Map<String, Object>> nodeLoadInfos = new HashMap<>();
        for (org.cyberlab.entity.HostNode node : availableNodes) {
            Map<String, Object> loadInfo = hostNodeService.getNodeLoadInfo(node.getId());
            nodeLoadInfos.put(node.getId(), loadInfo);
        }
        
        // 为每个资产智能选择最优节点（考虑业务特性匹配）
        for (Asset asset : anyStrategyAssets) {
            org.cyberlab.entity.HostNode selectedNode = selectOptimalNodeForAsset(asset, availableNodes, nodeLoadInfos);
            
            if (selectedNode != null) {
                Map<String, Object> loadInfo = nodeLoadInfos.get(selectedNode.getId());
                
                // 创建分配记录
                Map<String, Object> redistribution = new HashMap<>();
                redistribution.put("assetId", asset.getId());
                redistribution.put("assetName", asset.getName());
                redistribution.put("assetIp", asset.getIp());
                redistribution.put("previousNodeId", asset.getPreferredHostNodeId());
                redistribution.put("newNodeId", selectedNode.getId());
                redistribution.put("newNodeName", selectedNode.getDisplayName());
                redistribution.put("newNodeIp", selectedNode.getHostIp());
                redistribution.put("newNodePort", selectedNode.getDockerPort());
                redistribution.put("deploymentStrategy", asset.getDeploymentStrategy());
                redistribution.put("currentLoad", loadInfo.get("totalContainers"));
                redistribution.put("loadRatio", loadInfo.get("loadRatio"));
                redistribution.put("nodeScore", loadInfo.get("nodeScore"));
                redistribution.put("selectionReason", determineSelectionReason(asset, selectedNode, loadInfo));
                
                redistributions.add(redistribution);
                
                // 更新节点负载计数（用于后续分配决策）
                int currentLoad = ((Number) loadInfo.get("totalContainers")).intValue();
                loadInfo.put("totalContainers", currentLoad + 1);
                double maxContainers = ((Number) loadInfo.get("maxContainers")).doubleValue();
                loadInfo.put("loadRatio", (currentLoad + 1) / maxContainers);
            }
        }
        
        result.put("success", true);
        result.put("projectId", projectId);
        result.put("environment", environment);
        result.put("totalAssets", anyStrategyAssets.size());
        result.put("redistributedAssets", redistributions.size());
        result.put("redistributions", redistributions);
        result.put("availableNodes", availableNodes.size());
        result.put("redistributionTime", System.currentTimeMillis());
        result.put("algorithmType", "智能负载均衡 + 业务匹配");
        
        return result;
    }
    
    /**
     * 为单个资产选择最优节点
     */
    private org.cyberlab.entity.HostNode selectOptimalNodeForAsset(
            Asset asset, 
            List<org.cyberlab.entity.HostNode> availableNodes,
            Map<Long, Map<String, Object>> nodeLoadInfos) {
        
        if (availableNodes.isEmpty()) return null;
        
        // 计算每个节点对该资产的适配度分数
        org.cyberlab.entity.HostNode bestNode = null;
        double bestScore = -1;
        
        for (org.cyberlab.entity.HostNode node : availableNodes) {
            double score = calculateAssetNodeMatchScore(asset, node, nodeLoadInfos.get(node.getId()));
            if (score > bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }
        
        return bestNode;
    }
    
    /**
     * 计算资产与节点的匹配度分数
     */
    private double calculateAssetNodeMatchScore(Asset asset, org.cyberlab.entity.HostNode node, Map<String, Object> loadInfo) {
        double score = 0;
        
        // 基础负载分数 (40分) - 负载越低分数越高
        double loadRatio = ((Number) loadInfo.get("loadRatio")).doubleValue();
        score += (1.0 - Math.min(loadRatio, 1.0)) * 40;
        
        // 节点优先级分数 (20分)
        int priority = node.getPriority() != null ? node.getPriority() : 1;
        score += Math.min(priority * 4, 20);
        
        // 环境匹配分数 (15分)
        if (node.getEnvironment() != null) {
            if ("production".equals(node.getEnvironment())) {
                score += 15;
            } else if ("testing".equals(node.getEnvironment()) || "development".equals(node.getEnvironment())) {
                score += 10;
            }
        }
        
        // 资产类型匹配分数 (15分)
        if (asset.getAssetType() != null) {
            if ("container".equals(asset.getAssetType())) {
                // 容器类型资产更适合轻量级节点
                if ("vm".equals(node.getNodeType()) || "lightweight".contains(node.getDisplayName().toLowerCase())) {
                    score += 15;
                } else {
                    score += 10;
                }
            } else if ("server".equals(asset.getAssetType())) {
                // 服务器类型资产更适合物理节点
                if ("physical".equals(node.getNodeType()) || "dedicated".equals(node.getNodeType())) {
                    score += 15;
                } else {
                    score += 8;
                }
            }
        }
        
        // IP地址段亲和性分数 (25分) - 增加权重，优先匹配同网段
        if (asset.getIp() != null && node.getHostIp() != null) {
            String assetSubnet = extractSubnet(asset.getIp());
            String nodeSubnet = extractSubnet(node.getHostIp());
            
            if (assetSubnet.equals(nodeSubnet)) {
                score += 25; // 同一子网，高优先级
            } else if (isCompatibleNetwork(asset.getIp(), node.getHostIp())) {
                score += 15; // 兼容网络（如192.168.x.x和172.16.x.x映射关系）
            } else {
                score += 5; // 不同子网但可达
            }
        }
        
        return Math.min(score, 100);
    }
    
    /**
     * 提取IP地址的子网段
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
     * 判断两个IP是否属于兼容的网络
     * 处理特殊映射关系，如192.168.1.x资产可以部署到172.16.190.x节点
     */
    private boolean isCompatibleNetwork(String assetIp, String nodeIp) {
        if (assetIp == null || nodeIp == null) return false;
        
        // 定义网络映射规则
        String assetSubnet = extractSubnet(assetIp);
        String nodeSubnet = extractSubnet(nodeIp);
        
        // 特殊映射规则：192.168.1网段的资产可以部署到172.16.190网段的节点
        if ("192.168.1".equals(assetSubnet) && "172.16.190".equals(nodeSubnet)) {
            return true;
        }
        
        // 特殊映射规则：192.168.2网段的资产可以部署到172.16.191网段的节点
        if ("192.168.2".equals(assetSubnet) && "172.16.191".equals(nodeSubnet)) {
            return true;
        }
        
        // 通用规则：如果资产IP和节点IP都是私有网络，认为兼容
        if (isPrivateNetwork(assetIp) && isPrivateNetwork(nodeIp)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断IP是否为私有网络地址
     */
    private boolean isPrivateNetwork(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        
        String[] parts = ip.split("\\.");
        if (parts.length < 2) return false;
        
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.0.0.0 - 10.255.255.255
            if (first == 10) return true;
            
            // 172.16.0.0 - 172.31.255.255
            if (first == 172 && second >= 16 && second <= 31) return true;
            
            // 192.168.0.0 - 192.168.255.255
            if (first == 192 && second == 168) return true;
            
        } catch (NumberFormatException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * 确定节点选择原因
     */
    private String determineSelectionReason(Asset asset, org.cyberlab.entity.HostNode node, Map<String, Object> loadInfo) {
        List<String> reasons = new ArrayList<>();
        
        double loadRatio = ((Number) loadInfo.get("loadRatio")).doubleValue();
        if (loadRatio < 0.3) {
            reasons.add("负载较低");
        } else if (loadRatio < 0.7) {
            reasons.add("负载适中");
        }
        
        if (node.getPriority() != null && node.getPriority() > 3) {
            reasons.add("高优先级节点");
        }
        
        if (asset.getIp() != null && node.getHostIp() != null) {
            String assetSubnet = extractSubnet(asset.getIp());
            String nodeSubnet = extractSubnet(node.getHostIp());
            if (assetSubnet.equals(nodeSubnet)) {
                reasons.add("同一网络段");
            } else if (isCompatibleNetwork(asset.getIp(), node.getHostIp())) {
                reasons.add("兼容网络映射");
            }
        }
        
        if ("轻量级".contains(node.getDisplayName()) && "container".equals(asset.getAssetType())) {
            reasons.add("容器类型匹配");
        }
        
        return reasons.isEmpty() ? "智能算法选择" : String.join(" + ", reasons);
    }
    
    /**
     * 智能负载均衡推荐
     * 为新资产推荐最优部署节点
     */
    public Map<String, Object> recommendOptimalNode(String projectId, String environment, String assetType) {
        List<org.cyberlab.entity.HostNode> candidateNodes = hostNodeService.recommendDeploymentNodes(environment, 5);
        
        Map<String, Object> recommendation = new HashMap<>();
        
        if (candidateNodes.isEmpty()) {
            recommendation.put("success", false);
            recommendation.put("message", "没有找到合适的候选节点");
            return recommendation;
        }
        
        org.cyberlab.entity.HostNode bestNode = candidateNodes.get(0);
        Map<String, Object> bestNodeLoadInfo = hostNodeService.getNodeLoadInfo(bestNode.getId());
        
        recommendation.put("success", true);
        recommendation.put("recommendedNode", bestNode);
        recommendation.put("nodeLoadInfo", bestNodeLoadInfo);
        recommendation.put("alternativeNodes", candidateNodes.subList(1, Math.min(candidateNodes.size(), 4)));
        recommendation.put("projectId", projectId);
        recommendation.put("environment", environment);
        recommendation.put("assetType", assetType);
        recommendation.put("recommendationTime", System.currentTimeMillis());
        
        return recommendation;
    }
    
    /**
     * 检查项目资产分布均衡度
     * 分析当前分布是否均衡，给出优化建议
     */
    public Map<String, Object> analyzeProjectDistributionBalance(String projectId) {
        List<Asset> projectAssets = getAssetsByProjectId(projectId);
        List<Asset> assetsWithNodes = getProjectAssetsWithHostNodes(projectId);
        
        // 统计各节点的资产数量
        Map<Long, Integer> nodeAssetCounts = new HashMap<>();
        for (Asset asset : assetsWithNodes) {
            Long nodeId = asset.getPreferredHostNodeId();
            if (nodeId != null) {
                nodeAssetCounts.put(nodeId, nodeAssetCounts.getOrDefault(nodeId, 0) + 1);
            }
        }
        
        // 计算分布均衡度
        double balanceScore = calculateDistributionBalance(nodeAssetCounts);
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("projectId", projectId);
        analysis.put("totalAssets", projectAssets.size());
        analysis.put("assetsWithNodes", assetsWithNodes.size());
        analysis.put("nodeCount", nodeAssetCounts.size());
        analysis.put("nodeAssetDistribution", nodeAssetCounts);
        analysis.put("balanceScore", Math.round(balanceScore * 100.0) / 100.0);
        analysis.put("balanceLevel", getBalanceLevel(balanceScore));
        
        // 生成优化建议
        List<String> recommendations = generateDistributionRecommendations(nodeAssetCounts, balanceScore);
        analysis.put("recommendations", recommendations);
        analysis.put("analysisTime", System.currentTimeMillis());
        
        return analysis;
    }
    
    /**
     * 计算分布均衡度分数 (0-100，100为完全均衡)
     */
    private double calculateDistributionBalance(Map<Long, Integer> nodeAssetCounts) {
        if (nodeAssetCounts.isEmpty()) return 100.0;
        
        List<Integer> counts = new ArrayList<>(nodeAssetCounts.values());
        
        if (counts.size() == 1) return 100.0; // 只有一个节点，认为是完全均衡
        
        double mean = counts.stream().mapToInt(Integer::intValue).average().orElse(0);
        if (mean == 0) return 100.0;
        
        double variance = counts.stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average()
            .orElse(0);
        
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = standardDeviation / mean;
        
        // 转换为均衡度分数 (CV越小越均衡)
        return Math.max(0, 100 - (coefficientOfVariation * 100));
    }
    
    /**
     * 获取均衡度等级描述
     */
    private String getBalanceLevel(double balanceScore) {
        if (balanceScore >= 90) return "excellent";
        if (balanceScore >= 75) return "good";
        if (balanceScore >= 60) return "fair";
        if (balanceScore >= 40) return "poor";
        return "critical";
    }
    
    /**
     * 生成分布优化建议
     */
    private List<String> generateDistributionRecommendations(Map<Long, Integer> nodeAssetCounts, double balanceScore) {
        List<String> recommendations = new ArrayList<>();
        
        if (balanceScore >= 90) {
            recommendations.add("资产分布非常均衡，无需调整");
            return recommendations;
        }
        
        if (nodeAssetCounts.isEmpty()) {
            recommendations.add("没有资产关联到节点，建议配置资产部署策略");
            return recommendations;
        }
        
        List<Integer> counts = new ArrayList<>(nodeAssetCounts.values());
        int maxCount = counts.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minCount = counts.stream().mapToInt(Integer::intValue).min().orElse(0);
        
        if (maxCount - minCount > 2) {
            recommendations.add("节点间资产数量差异较大，建议重新分配负载");
        }
        
        if (nodeAssetCounts.size() == 1) {
            recommendations.add("所有资产集中在单一节点，建议启用多节点部署");
        } else if (counts.stream().anyMatch(count -> count > 10)) {
            recommendations.add("某些节点负载过高，建议分散到其他节点");
        }
        
        if (balanceScore < 60) {
            recommendations.add("建议使用智能负载均衡重新分配资产");
        }
        
        return recommendations;
    }
    
    /**
     * 执行资产重新分配（实际修改数据库）
     * 根据负载均衡策略重新分配项目内的资产
     */
    @Transactional
    public Map<String, Object> executeAssetRedistribution(String projectId, String environment, boolean forceRebalance) {
        Map<String, Object> redistributionPlan = redistributeProjectAssets(projectId, environment);
        
        if (!(boolean) redistributionPlan.get("success")) {
            return redistributionPlan;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> redistributions = (List<Map<String, Object>>) redistributionPlan.get("redistributions");
        
        List<Asset> updatedAssets = new ArrayList<>();
        
        for (Map<String, Object> redistribution : redistributions) {
            Long assetId = ((Number) redistribution.get("assetId")).longValue();
            Long newNodeId = ((Number) redistribution.get("newNodeId")).longValue();
            
            Optional<Asset> assetOpt = assetRepository.findById(assetId);
            if (assetOpt.isPresent()) {
                Asset asset = assetOpt.get();
                
                // 只在强制重新平衡模式下修改"any"策略资产
                if (forceRebalance && "any".equals(asset.getDeploymentStrategy())) {
                    // 临时分配到特定节点进行重新平衡
                    String originalStrategy = asset.getDeploymentStrategy();
                    asset.setDeploymentStrategy("load_balanced");
                    asset.setPreferredHostNodeId(newNodeId);
                    
                    // 保存更新
                    assetRepository.save(asset);
                    updatedAssets.add(asset);
                    
                    // 更新redistribution信息
                    redistribution.put("updated", true);
                    redistribution.put("originalStrategy", originalStrategy);
                    redistribution.put("newStrategy", "load_balanced");
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("projectId", projectId);
        result.put("environment", environment);
        result.put("forceRebalance", forceRebalance);
        result.put("totalPlannedRedistributions", redistributions.size());
        result.put("actuallyUpdatedAssets", updatedAssets.size());
        result.put("updatedAssets", updatedAssets.stream().map(asset -> Map.of(
            "id", asset.getId(),
            "name", asset.getName(),
            "newNodeId", asset.getPreferredHostNodeId(),
            "newStrategy", asset.getDeploymentStrategy()
        )).collect(Collectors.toList()));
        result.put("redistributionPlan", redistributionPlan);
        result.put("executionTime", System.currentTimeMillis());
        
        return result;
    }
    
    // ========== 增强部署策略和故障转移机制 ==========
    
    /**
     * 智能选择最优部署节点（考虑亲和性、反亲和性和优先级）
     */
    public Map<String, Object> selectOptimalDeploymentNode(Asset asset, String environment) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取候选节点
        List<org.cyberlab.entity.HostNode> candidateNodes = hostNodeService.getAvailableNodesForDeployment();
        
        // 过滤环境匹配的节点
        if (environment != null && !environment.trim().isEmpty()) {
            candidateNodes = candidateNodes.stream()
                .filter(node -> environment.equals(node.getEnvironment()))
                .collect(Collectors.toList());
        }
        
        if (candidateNodes.isEmpty()) {
            result.put("success", false);
            result.put("message", "没有可用的部署节点");
            return result;
        }
        
        // 根据部署策略筛选节点
        org.cyberlab.entity.HostNode selectedNode = null;
        String selectionReason = "";
        
        switch (asset.getDeploymentStrategy() != null ? asset.getDeploymentStrategy() : "any") {
            case "fixed":
                if (asset.getPreferredHostNodeId() != null) {
                    selectedNode = candidateNodes.stream()
                        .filter(node -> node.getId().equals(asset.getPreferredHostNodeId()))
                        .findFirst()
                        .orElse(null);
                    if (selectedNode != null) {
                        selectionReason = "固定节点策略 - 使用指定节点";
                    } else if (asset.isFailoverEnabled() && asset.getFallbackHostNodeId() != null) {
                        selectedNode = candidateNodes.stream()
                            .filter(node -> node.getId().equals(asset.getFallbackHostNodeId()))
                            .findFirst()
                            .orElse(null);
                        selectionReason = "固定节点不可用 - 使用备用节点";
                    }
                }
                break;
                
            case "load_balanced":
                Optional<org.cyberlab.entity.HostNode> loadBalancedNode = hostNodeService.getLoadBalancedNode(environment);
                if (loadBalancedNode.isPresent() && candidateNodes.contains(loadBalancedNode.get())) {
                    selectedNode = loadBalancedNode.get();
                    selectionReason = "负载均衡策略 - 选择负载最低节点";
                }
                break;
                
            case "any":
            default:
                // 按优先级和负载选择最优节点
                selectedNode = selectNodeByPriorityAndLoad(candidateNodes, asset);
                selectionReason = "任意节点策略 - 按优先级和负载选择";
                break;
        }
        
        // 如果没有选中节点，使用默认选择逻辑
        if (selectedNode == null && !candidateNodes.isEmpty()) {
            selectedNode = candidateNodes.get(0);
            selectionReason = "默认选择 - 使用第一个可用节点";
        }
        
        if (selectedNode != null) {
            result.put("success", true);
            result.put("selectedNode", selectedNode);
            result.put("selectionReason", selectionReason);
            result.put("nodeLoadInfo", hostNodeService.getNodeLoadInfo(selectedNode.getId()));
        } else {
            result.put("success", false);
            result.put("message", "无法找到合适的部署节点");
        }
        
        return result;
    }
    
    /**
     * 按优先级和负载选择节点
     */
    private org.cyberlab.entity.HostNode selectNodeByPriorityAndLoad(
            List<org.cyberlab.entity.HostNode> candidateNodes, Asset asset) {
        
        return candidateNodes.stream()
            .sorted((node1, node2) -> {
                // 首先按节点优先级排序
                int priorityCompare = Integer.compare(
                    node2.getPriority() != null ? node2.getPriority() : 1,
                    node1.getPriority() != null ? node1.getPriority() : 1
                );
                if (priorityCompare != 0) return priorityCompare;
                
                // 然后按负载排序（负载低的优先）
                Map<String, Object> load1 = hostNodeService.getNodeLoadInfo(node1.getId());
                Map<String, Object> load2 = hostNodeService.getNodeLoadInfo(node2.getId());
                
                double loadRatio1 = ((Number) load1.getOrDefault("loadRatio", 0)).doubleValue();
                double loadRatio2 = ((Number) load2.getOrDefault("loadRatio", 0)).doubleValue();
                
                return Double.compare(loadRatio1, loadRatio2);
            })
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 检查并执行故障转移
     */
    public Map<String, Object> performFailoverIfNeeded(Long assetId, Long failedNodeId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Asset> assetOpt = assetRepository.findById(assetId);
        if (assetOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "资产不存在");
            return result;
        }
        
        Asset asset = assetOpt.get();
        
        if (!asset.isFailoverEnabled()) {
            result.put("success", false);
            result.put("message", "资产未启用故障转移");
            return result;
        }
        
        // 尝试使用备用节点
        if (asset.hasFallbackNode()) {
            Optional<org.cyberlab.entity.HostNode> fallbackNode = hostNodeService.getNodeById(asset.getFallbackHostNodeId());
            if (fallbackNode.isPresent() && fallbackNode.get().isActive()) {
                // 执行故障转移
                asset.setPreferredHostNodeId(asset.getFallbackHostNodeId());
                asset.setPreferredHostNodeName(fallbackNode.get().getDisplayName());
                assetRepository.save(asset);
                
                result.put("success", true);
                result.put("failoverType", "fallback_node");
                result.put("newNodeId", asset.getFallbackHostNodeId());
                result.put("newNodeName", fallbackNode.get().getDisplayName());
                result.put("message", "故障转移到备用节点成功");
                return result;
            }
        }
        
        // 如果没有备用节点或备用节点不可用，尝试自动选择新节点
        if (asset.isAutoFailover()) {
            Map<String, Object> selectionResult = selectOptimalDeploymentNode(asset, null);
            if ((boolean) selectionResult.get("success")) {
                @SuppressWarnings("unchecked")
                org.cyberlab.entity.HostNode newNode = (org.cyberlab.entity.HostNode) selectionResult.get("selectedNode");
                
                asset.setPreferredHostNodeId(newNode.getId());
                asset.setPreferredHostNodeName(newNode.getDisplayName());
                assetRepository.save(asset);
                
                result.put("success", true);
                result.put("failoverType", "auto_selection");
                result.put("newNodeId", newNode.getId());
                result.put("newNodeName", newNode.getDisplayName());
                result.put("selectionReason", selectionResult.get("selectionReason"));
                result.put("message", "自动故障转移成功");
                return result;
            }
        }
        
        result.put("success", false);
        result.put("message", "故障转移失败：无可用节点");
        return result;
    }
    
    /**
     * 批量检查并执行故障转移
     */
    public Map<String, Object> batchFailoverCheck(Long failedNodeId) {
        List<Asset> affectedAssets = assetRepository.findAll().stream()
            .filter(asset -> failedNodeId.equals(asset.getPreferredHostNodeId()))
            .filter(Asset::isFailoverEnabled)
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> failoverResults = new ArrayList<>();
        
        int successCount = 0;
        int failedCount = 0;
        
        for (Asset asset : affectedAssets) {
            Map<String, Object> failoverResult = performFailoverIfNeeded(asset.getId(), failedNodeId);
            failoverResults.add(Map.of(
                "assetId", asset.getId(),
                "assetName", asset.getName(),
                "failoverResult", failoverResult
            ));
            
            if ((boolean) failoverResult.get("success")) {
                successCount++;
            } else {
                failedCount++;
            }
        }
        
        result.put("totalAffectedAssets", affectedAssets.size());
        result.put("successfulFailovers", successCount);
        result.put("failedFailovers", failedCount);
        result.put("failoverResults", failoverResults);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 验证和修复资产部署配置
     */
    public Map<String, Object> validateAndFixDeploymentConfiguration(String projectId) {
        List<Asset> projectAssets = getAssetsByProjectId(projectId);
        Map<String, Object> result = new HashMap<>();
        List<Asset> fixedAssets = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        
        for (Asset asset : projectAssets) {
            boolean needsUpdate = false;
            
            // 验证部署策略
            if (asset.getDeploymentStrategy() == null || asset.getDeploymentStrategy().trim().isEmpty()) {
                asset.setDeploymentStrategy("any");
                needsUpdate = true;
                validationErrors.add("资产 " + asset.getName() + " 缺少部署策略，已设置为'any'");
            }
            
            // 验证固定策略的一致性
            if ("fixed".equals(asset.getDeploymentStrategy()) && asset.getPreferredHostNodeId() == null) {
                asset.setDeploymentStrategy("any");
                needsUpdate = true;
                validationErrors.add("资产 " + asset.getName() + " 使用固定策略但未指定节点，已改为'any'");
            }
            
            // 验证备用节点配置
            if (asset.isFailoverEnabled() && asset.getFallbackHostNodeId() != null) {
                if (asset.getFallbackHostNodeId().equals(asset.getPreferredHostNodeId())) {
                    asset.setFallbackHostNodeId(null);
                    needsUpdate = true;
                    validationErrors.add("资产 " + asset.getName() + " 的备用节点与主节点相同，已清除备用节点");
                }
            }
            
            // 设置默认部署优先级
            if (asset.getDeploymentPriority() == null) {
                asset.setDeploymentPriority(5);
                needsUpdate = true;
            }
            
            // 设置默认故障转移策略
            if (asset.isFailoverEnabled() && asset.getFailoverStrategy() == null) {
                asset.setFailoverStrategy("manual");
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                assetRepository.save(asset);
                fixedAssets.add(asset);
            }
        }
        
        result.put("projectId", projectId);
        result.put("totalAssets", projectAssets.size());
        result.put("fixedAssets", fixedAssets.size());
        result.put("validationErrors", validationErrors);
        result.put("fixedAssetsList", fixedAssets.stream().map(asset -> Map.of(
            "id", asset.getId(),
            "name", asset.getName(),
            "deploymentStrategy", asset.getDeploymentStrategy(),
            "deploymentPriority", asset.getDeploymentPriority(),
            "enableFailover", asset.getEnableFailover() != null ? asset.getEnableFailover() : false
        )).collect(Collectors.toList()));
        result.put("validationTime", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 获取项目内的所有容器资产
     */
    public List<Asset> getContainerAssetsByProjectId(String projectId) {
        return getAssetsByProjectId(projectId).stream()
            .filter(asset -> "container".equalsIgnoreCase(asset.getAssetType()) || 
                           (asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目内可用于演练的容器资产
     * 只返回已启用且配置完整的容器资产
     */
    public List<Asset> getActiveContainerAssetsByProjectId(String projectId) {
        return getContainerAssetsByProjectId(projectId).stream()
            .filter(asset -> asset.isEnabled() && 
                           asset.getDockerImage() != null && 
                           !asset.getDockerImage().trim().isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据topologyProjectId获取关联的容器资产
     * 专为演练系统设计，支持基于拓扑项目ID的资产查询
     */
    public List<Asset> getContainerAssetsByTopologyProjectId(String topologyProjectId) {
        if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
            return List.of();
        }
        
        return assetRepository.findByTopologyProjectId(topologyProjectId).stream()
            .filter(asset -> "container".equalsIgnoreCase(asset.getAssetType()) || 
                           (asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目资产的拓扑感知部署配置
     * 返回考虑网络拓扑关系的部署建议
     */
    public Map<String, Object> getProjectAssetTopologyAwareConfig(String projectId) {
        List<Asset> projectAssets = getAssetsByProjectId(projectId);
        Map<String, Object> config = new HashMap<>();
        
        // 按部署策略分组
        Map<String, List<Asset>> assetsByStrategy = projectAssets.stream()
            .filter(asset -> "container".equalsIgnoreCase(asset.getAssetType()))
            .collect(Collectors.groupingBy(
                asset -> asset.getDeploymentStrategy() != null ? asset.getDeploymentStrategy() : "default"
            ));
        
        config.put("assetsByStrategy", assetsByStrategy);
        
        // 统计节点分布
        Map<Long, Long> nodeDistribution = projectAssets.stream()
            .filter(asset -> asset.getPreferredHostNodeId() != null)
            .collect(Collectors.groupingBy(
                Asset::getPreferredHostNodeId,
                Collectors.counting()
            ));
        
        config.put("nodeDistribution", nodeDistribution);
        
        // 计算依赖关系（基于亲和性规则）
        List<Map<String, Object>> dependencyChains = projectAssets.stream()
            .filter(asset -> asset.getAffinityRules() != null && !asset.getAffinityRules().trim().isEmpty())
            .map(asset -> {
                Map<String, Object> dependency = new HashMap<>();
                dependency.put("assetId", asset.getId());
                dependency.put("assetName", asset.getName());
                dependency.put("affinityRules", asset.getAffinityRules());
                dependency.put("antiAffinityRules", asset.getAntiAffinityRules());
                return dependency;
            })
            .collect(Collectors.toList());
        
        config.put("dependencyChains", dependencyChains);
        
        return config;
    }
    
    /**
     * 智能推荐项目资产的部署节点
     * 基于负载均衡、网络亲和性和资源约束进行推荐
     */
    public Map<Long, List<Asset>> recommendAssetDeploymentNodes(String projectId) {
        List<Asset> containerAssets = getContainerAssetsByProjectId(projectId);
        Map<Long, List<Asset>> recommendations = new HashMap<>();
        
        // 获取所有可用节点
        List<Long> availableNodes = containerAssets.stream()
            .map(Asset::getPreferredHostNodeId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        
        // 按优先级分配资产
        for (Asset asset : containerAssets) {
            Long recommendedNodeId = selectOptimalNodeForAssetDeployment(asset, availableNodes, recommendations);
            
            if (recommendedNodeId != null) {
                recommendations.computeIfAbsent(recommendedNodeId, k -> new ArrayList<>()).add(asset);
            }
        }
        
        return recommendations;
    }
    
    /**
     * 为单个资产选择最优节点 (用于推荐部署)
     */
    private Long selectOptimalNodeForAssetDeployment(Asset asset, List<Long> availableNodes, Map<Long, List<Asset>> currentAllocations) {
        // 1. 优先使用指定的首选节点
        if (asset.getPreferredHostNodeId() != null && availableNodes.contains(asset.getPreferredHostNodeId())) {
            return asset.getPreferredHostNodeId();
        }
        
        // 2. 根据部署策略选择
        String strategy = asset.getDeploymentStrategy();
        if ("fixed".equals(strategy) && asset.getPreferredHostNodeId() != null) {
            return asset.getPreferredHostNodeId();
        }
        
        if ("load_balanced".equals(strategy)) {
            // 选择当前分配资产最少的节点
            return availableNodes.stream()
                .min(Comparator.comparing(nodeId -> currentAllocations.getOrDefault(nodeId, new ArrayList<>()).size()))
                .orElse(null);
        }
        
        // 3. 默认选择第一个可用节点
        return availableNodes.isEmpty() ? null : availableNodes.get(0);
    }
}