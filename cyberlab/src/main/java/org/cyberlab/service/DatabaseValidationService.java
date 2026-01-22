package org.cyberlab.service;

import org.cyberlab.entity.HostNode;
import org.cyberlab.entity.Asset;
import org.cyberlab.repository.HostNodeRepository;
import org.cyberlab.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据库验证和修复服务
 * 用于检查和修复VM配置问题
 */
@Service
public class DatabaseValidationService {

    @Autowired
    private HostNodeRepository hostNodeRepository;
    
    @Autowired
    private AssetRepository assetRepository;

    private static final String VM_IP = "172.16.190.130";
    private static final int VM_DOCKER_PORT = 2375;
    
    /**
     * 验证VM配置是否正确
     */
    public Map<String, Object> validateVMConfiguration() {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 检查VM节点配置 - 包括重复检查
        List<HostNode> vmNodes = hostNodeRepository.findAllByHostIp(VM_IP);
        result.put("vmNodesCount", vmNodes.size());
        result.put("vmNodesCorrect", vmNodes.size() == 1);
        
        // 检查是否有重复节点
        boolean hasDuplicates = vmNodes.size() > 1;
        result.put("hasDuplicateNodes", hasDuplicates);
        
        if (hasDuplicates) {
            result.put("duplicateNodeIds", vmNodes.stream().map(HostNode::getId).toList());
            result.put("duplicateNodesInfo", vmNodes.stream().map(node -> Map.of(
                "id", node.getId(),
                "name", node.getName(),
                "displayName", node.getDisplayName(),
                "createdAt", node.getCreatedAt()
            )).toList());
        }
        
        Optional<HostNode> vmNodeOpt = vmNodes.isEmpty() ? Optional.empty() : Optional.of(vmNodes.get(0));
        
        if (vmNodeOpt.isPresent()) {
            HostNode vmNode = vmNodeOpt.get();
            result.put("vmNodeInfo", Map.of(
                "id", vmNode.getId(),
                "name", vmNode.getDisplayName(),
                "ip", vmNode.getHostIp(),
                "port", vmNode.getDockerPort(),
                "status", vmNode.getStatus(),
                "tlsEnabled", vmNode.getDockerTlsEnabled()
            ));
            
            // 检查端口配置
            boolean portCorrect = vmNode.getDockerPort() == VM_DOCKER_PORT;
            result.put("vmPortCorrect", portCorrect);
            
            // 检查关联的资产数量
            List<Asset> associatedAssets = assetRepository.findByPreferredHostNodeId(vmNode.getId());
            result.put("associatedAssetsCount", associatedAssets.size());
            result.put("associatedAssets", associatedAssets.stream().map(asset -> Map.of(
                "id", asset.getId(),
                "name", asset.getName(),
                "ip", asset.getIp(),
                "deploymentStrategy", asset.getDeploymentStrategy()
            )).toList());
        } else {
            result.put("vmNodeInfo", null);
            result.put("vmPortCorrect", false);
            result.put("associatedAssetsCount", 0);
            result.put("associatedAssets", List.of());
        }
        
        // 2. 检查是否存在错误配置的节点
        Optional<HostNode> wrongNodeOpt = hostNodeRepository.findByHostIp("192.168.1.100");
        result.put("wrongNodesCount", wrongNodeOpt.isPresent() ? 1 : 0);
        if (wrongNodeOpt.isPresent()) {
            HostNode wrongNode = wrongNodeOpt.get();
            result.put("wrongNodes", List.of(Map.of(
                "id", wrongNode.getId(),
                "name", wrongNode.getDisplayName(),
                "ip", wrongNode.getHostIp(),
                "port", wrongNode.getDockerPort()
            )));
        } else {
            result.put("wrongNodes", List.of());
        }
        
        // 3. 检查孤立资产（没有关联到正确节点的资产）
        List<Asset> allProjectAssets = assetRepository.findByCompany("红岸实验室");
        List<Asset> orphanedAssets = allProjectAssets.stream()
            .filter(asset -> asset.getPreferredHostNodeId() == null || 
                           (vmNodeOpt.isEmpty() || !vmNodeOpt.get().getId().equals(asset.getPreferredHostNodeId())))
            .toList();
        
        result.put("totalProjectAssets", allProjectAssets.size());
        result.put("orphanedAssetsCount", orphanedAssets.size());
        result.put("orphanedAssets", orphanedAssets.stream().map(asset -> Map.of(
            "id", asset.getId(),
            "name", asset.getName(),
            "ip", asset.getIp(),
            "preferredHostNodeId", asset.getPreferredHostNodeId()
        )).toList());
        
        // 4. 计算整体健康状态
        boolean isHealthy = vmNodeOpt.isPresent() && 
                          vmNodeOpt.get().getDockerPort() == VM_DOCKER_PORT &&
                          wrongNodeOpt.isEmpty() && 
                          orphanedAssets.isEmpty();
        
        result.put("overallHealthy", isHealthy);
        result.put("validationTime", LocalDateTime.now());
        
        return result;
    }
    
    /**
     * 修复VM配置问题
     */
    @Transactional
    public Map<String, Object> fixVMConfiguration() {
        Map<String, Object> result = new HashMap<>();
        int fixedCount = 0;
        
        try {
            // 1. 删除错误配置的节点
            Optional<HostNode> wrongNodeOpt = hostNodeRepository.findByHostIp("192.168.1.100");
            if (wrongNodeOpt.isPresent()) {
                HostNode wrongNode = wrongNodeOpt.get();
                // 先将引用此节点的资产解除关联
                List<Asset> referencingAssets = assetRepository.findByPreferredHostNodeId(wrongNode.getId());
                for (Asset asset : referencingAssets) {
                    asset.setPreferredHostNodeId(null);
                    asset.setPreferredHostNodeName(null);
                    assetRepository.save(asset);
                }
                
                hostNodeRepository.delete(wrongNode);
                fixedCount++;
            }
            
            // 2. 处理重复的VM节点
            List<HostNode> vmNodes = hostNodeRepository.findAllByHostIp(VM_IP);
            if (vmNodes.size() > 1) {
                // 找到最合适的节点保留（最完整的配置和最早创建的）
                HostNode bestNode = vmNodes.stream()
                    .sorted((a, b) -> {
                        // 优先选择描述信息完整的节点
                        int scoreA = (a.getDescription() != null && a.getDescription().contains("苏州科技大学")) ? 100 : 0;
                        int scoreB = (b.getDescription() != null && b.getDescription().contains("苏州科技大学")) ? 100 : 0;
                        
                        // 其次选择状态为active的节点
                        scoreA += "active".equals(a.getStatus()) ? 50 : 0;
                        scoreB += "active".equals(b.getStatus()) ? 50 : 0;
                        
                        // 最后按创建时间排序（较早的优先）
                        if (scoreA != scoreB) {
                            return Integer.compare(scoreB, scoreA); // 降序，分数高的排前面
                        }
                        return a.getCreatedAt().compareTo(b.getCreatedAt()); // 升序，早创建的排前面
                    })
                    .findFirst()
                    .orElse(vmNodes.get(0));
                
                // 删除其他重复节点
                for (HostNode node : vmNodes) {
                    if (!node.getId().equals(bestNode.getId())) {
                        // 将引用重复节点的资产和容器重新关联到最佳节点
                        List<Asset> referencingAssets = assetRepository.findByPreferredHostNodeId(node.getId());
                        for (Asset asset : referencingAssets) {
                            asset.setPreferredHostNodeId(bestNode.getId());
                            asset.setPreferredHostNodeName(bestNode.getDisplayName());
                            assetRepository.save(asset);
                        }
                        
                        // 删除重复节点
                        hostNodeRepository.delete(node);
                        fixedCount++;
                    }
                }
                
                result.put("removedDuplicates", vmNodes.size() - 1);
                result.put("retainedNodeId", bestNode.getId());
            }
            
            // 3. 确保VM节点存在且配置正确
            Optional<HostNode> vmNodeOpt = hostNodeRepository.findByHostIp(VM_IP);
            HostNode vmNode;
            
            if (vmNodeOpt.isEmpty()) {
                // 创建VM节点（添加重复检查）
                if (hostNodeRepository.existsByNameAndHostIp("测试节点01", VM_IP)) {
                    throw new RuntimeException("节点已存在，避免重复创建");
                }
                
                vmNode = new HostNode();
                vmNode.setName("测试节点01");
                vmNode.setDisplayName("测试节点01 - Ubuntu VM");
                vmNode.setHostIp(VM_IP);
                vmNode.setDockerPort(VM_DOCKER_PORT);
                vmNode.setSshPort(22);
                vmNode.setClusterName("cyberlab-test-cluster");
                vmNode.setNodeType("vm");
                vmNode.setEnvironment("testing");
                vmNode.setStatus("active");
                vmNode.setDescription("苏州科技大学测试环境Ubuntu虚拟机，Docker API端口2375，用于容器管理和安全演练");
                vmNode.setCpuCores(4);
                vmNode.setMemoryTotal(4096L);
                vmNode.setDiskTotal(50L);
                vmNode.setDockerTlsEnabled(false);
                vmNode.setMonitoringEnabled(true);
                vmNode.setMaxContainers(30);
                vmNode.setPriority(5);
                
                vmNode = hostNodeRepository.save(vmNode);
                fixedCount++;
            } else {
                // 更新现有VM节点
                vmNode = vmNodeOpt.get();
                boolean needsUpdate = false;
                
                if (vmNode.getDockerPort() != VM_DOCKER_PORT) {
                    vmNode.setDockerPort(VM_DOCKER_PORT);
                    needsUpdate = true;
                }
                
                if (vmNode.getDockerTlsEnabled() == null || vmNode.getDockerTlsEnabled()) {
                    vmNode.setDockerTlsEnabled(false);
                    needsUpdate = true;
                }
                
                if (!"active".equals(vmNode.getStatus())) {
                    vmNode.setStatus("active");
                    needsUpdate = true;
                }
                
                // 确保显示名称正确
                if (!"测试节点01 - Ubuntu VM".equals(vmNode.getDisplayName())) {
                    vmNode.setDisplayName("测试节点01 - Ubuntu VM");
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    vmNode = hostNodeRepository.save(vmNode);
                    fixedCount++;
                }
            }
            
            // 3. 修复孤立的资产
            List<Asset> allProjectAssets = assetRepository.findByCompany("红岸实验室");
            for (Asset asset : allProjectAssets) {
                boolean needsUpdate = false;
                
                // 更新IP地址
                if ("192.168.1.100".equals(asset.getIp()) || asset.getIp() == null || asset.getIp().isEmpty()) {
                    asset.setIp(VM_IP);
                    needsUpdate = true;
                }
                
                // 关联到VM节点
                if (asset.getPreferredHostNodeId() == null || 
                    !asset.getPreferredHostNodeId().equals(vmNode.getId())) {
                    asset.setPreferredHostNodeId(vmNode.getId());
                    asset.setPreferredHostNodeName(vmNode.getDisplayName());
                    
                    // 设置部署策略
                    if ("Web服务器".equals(asset.getName()) || "数据库服务器".equals(asset.getName())) {
                        asset.setDeploymentStrategy("fixed");
                    } else {
                        asset.setDeploymentStrategy("any");
                    }
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    assetRepository.save(asset);
                    fixedCount++;
                }
            }
            
            result.put("success", true);
            result.put("fixedCount", fixedCount);
            result.put("vmNodeId", vmNode.getId());
            result.put("message", "VM配置修复完成");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("fixedCount", fixedCount);
        }
        
        result.put("fixTime", LocalDateTime.now());
        return result;
    }
    
    /**
     * 获取系统健康报告
     */
    public Map<String, Object> getHealthReport() {
        Map<String, Object> validation = validateVMConfiguration();
        
        Map<String, Object> report = new HashMap<>();
        report.put("timestamp", LocalDateTime.now());
        report.put("validation", validation);
        
        // 生成建议
        if ((Boolean) validation.get("overallHealthy")) {
            report.put("status", "HEALTHY");
            report.put("recommendation", "系统配置正常，VM节点和资产关联正确");
        } else {
            report.put("status", "NEEDS_FIX");
            
            StringBuilder recommendations = new StringBuilder("需要修复以下问题：");
            
            if ((Integer) validation.get("vmNodesCount") != 1) {
                recommendations.append(" [VM节点配置]");
            }
            
            if (!(Boolean) validation.get("vmPortCorrect")) {
                recommendations.append(" [Docker端口配置]");
            }
            
            if ((Integer) validation.get("wrongNodesCount") > 0) {
                recommendations.append(" [删除错误节点]");
            }
            
            if ((Integer) validation.get("orphanedAssetsCount") > 0) {
                recommendations.append(" [修复孤立资产]");
            }
            
            report.put("recommendation", recommendations.toString());
        }
        
        return report;
    }
}