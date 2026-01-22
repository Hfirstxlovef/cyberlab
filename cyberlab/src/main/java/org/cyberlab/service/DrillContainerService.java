package org.cyberlab.service;

import org.cyberlab.entity.CyberRange;
import org.cyberlab.entity.DrillContainer;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.HostNode;
import org.cyberlab.repository.CyberRangeRepository;
import org.cyberlab.repository.DrillContainerRepository;
import org.cyberlab.repository.AssetRepository;
import org.cyberlab.service.HostNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class DrillContainerService {
    
    @Autowired
    private DrillContainerRepository drillContainerRepository;
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private CyberRangeRepository cyberRangeRepository;
    
    @Autowired
    private AssetRepository assetRepository;
    
    @Autowired
    private HostNodeService hostNodeService;
    
    /**
     * 获取活跃的演练列表
     */
    public List<Map<String, Object>> getActiveDrills() {
        List<Map<String, Object>> activeDrills = new ArrayList<>();
        
        // 从数据库获取状态为running的演练
        List<CyberRange> runningRanges = cyberRangeRepository.findByStatus("running");
        
        for (CyberRange range : runningRanges) {
            Map<String, Object> drill = new HashMap<>();
            drill.put("id", range.getId());
            drill.put("name", range.getName());
            drill.put("status", range.getStatus());
            drill.put("createdAt", range.getCreatedAt());
            activeDrills.add(drill);
        }
        
        // 如果没有运行中的演练，返回模拟数据作为后备
        if (activeDrills.isEmpty()) {
            Map<String, Object> drill1 = new HashMap<>();
            drill1.put("id", 1);
            drill1.put("name", "春季攻防演练");
            drill1.put("status", "running");
            activeDrills.add(drill1);
            
            Map<String, Object> drill2 = new HashMap<>();
            drill2.put("id", 2);
            drill2.put("name", "夏季红蓝对抗");
            drill2.put("status", "running");
            activeDrills.add(drill2);
            
            Map<String, Object> drill3 = new HashMap<>();
            drill3.put("id", 3);
            drill3.put("name", "秋季实战演习");
            drill3.put("status", "running");
            activeDrills.add(drill3);
        }
        
        return activeDrills;
    }
    
    /**
     * 获取演练下的所有容器
     */
    public List<DrillContainer> getContainersByRangeId(Long rangeId) {
        return drillContainerRepository.findByRangeId(rangeId);
    }
    
    /**
     * 创建容器配置
     */
    public DrillContainer createContainer(DrillContainer container) {
        // 检查名称是否重复
        Optional<DrillContainer> existing = drillContainerRepository
            .findByRangeIdAndName(container.getRangeId(), container.getName());
        if (existing.isPresent()) {
            throw new RuntimeException("容器名称已存在");
        }
        
        container.setCreateTime(LocalDateTime.now());
        container.setStatus("not_deployed");
        return drillContainerRepository.save(container);
    }
    
    /**
     * 异步部署容器
     */
    @Async
    public CompletableFuture<Void> deployContainerAsync(Long containerId) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        DrillContainer container = containerOpt.get();
        
        try {
            // 更新状态为部署中
            container.setStatus("deploying");
            container.setDeployTime(LocalDateTime.now());
            drillContainerRepository.save(container);
            
            // 执行Docker部署
            String deployResult = deployContainer(container);
            
            if (deployResult.contains("成功")) {
                container.setStatus("running");
                container.setStartTime(LocalDateTime.now());
                // 获取容器IP等信息
                updateContainerInfo(container);
            } else {
                container.setStatus("failed");
                container.setDeployLog(deployResult);
            }
            
        } catch (Exception e) {
            container.setStatus("failed");
            container.setDeployLog("部署失败: " + e.getMessage());
        } finally {
            drillContainerRepository.save(container);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 启动容器
     */
    public void startContainer(Long containerId) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            throw new RuntimeException("容器不存在");
        }
        
        DrillContainer container = containerOpt.get();
        
        if (container.getContainerId() == null) {
            throw new RuntimeException("容器尚未部署");
        }
        
        String result = dockerService.startContainer(container.getContainerId());
        if (result.contains("启动成功")) {
            container.setStatus("running");
            container.setStartTime(LocalDateTime.now());
            updateContainerInfo(container);
            drillContainerRepository.save(container);
        } else {
            throw new RuntimeException("启动失败: " + result);
        }
    }
    
    /**
     * 停止容器
     */
    public void stopContainer(Long containerId) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            throw new RuntimeException("容器不存在");
        }
        
        DrillContainer container = containerOpt.get();
        
        if (container.getContainerId() == null) {
            throw new RuntimeException("容器尚未部署");
        }
        
        String result = dockerService.stopContainer(container.getContainerId());
        if (result.contains("已停止")) {
            container.setStatus("stopped");
            container.setStopTime(LocalDateTime.now());
            drillContainerRepository.save(container);
        } else {
            throw new RuntimeException("停止失败: " + result);
        }
    }
    
    /**
     * 获取容器日志
     */
    public String getContainerLogs(Long containerId) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            return "容器不存在";
        }
        
        DrillContainer container = containerOpt.get();
        
        if (container.getContainerId() == null) {
            return container.getDeployLog() != null ? container.getDeployLog() : "容器尚未部署";
        }
        
        // 使用DockerService获取容器日志
        return dockerService.getContainerLogs(container.getContainerId());
    }
    
    /**
     * 删除容器
     */
    public void deleteContainer(Long containerId) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            throw new RuntimeException("容器不存在");
        }
        
        DrillContainer container = containerOpt.get();
        
        // 如果容器正在运行，先停止
        if ("running".equals(container.getStatus()) && container.getContainerId() != null) {
            dockerService.stopContainer(container.getContainerId());
        }
        
        // 删除Docker容器
        if (container.getContainerId() != null) {
            String result = dockerService.removeContainer(container.getContainerId(), true); // 强制删除
            if (!result.contains("删除成功")) {
                // Debug statement removed
            }
        }
        
        // 删除数据库记录
        drillContainerRepository.delete(container);
    }
    
    /**
     * 同步容器状态 - 与Docker实际状态同步
     */
    public void syncContainerStatus(Long containerId) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isEmpty()) {
            return;
        }
        
        DrillContainer container = containerOpt.get();
        if (container.getContainerId() == null) {
            return;
        }
        
        // 获取Docker中的实际状态
        var containerInfo = dockerService.getContainerInfo(container.getContainerId());
        if (containerInfo != null) {
            String dockerStatus = containerInfo.getStatus().toLowerCase();
            String currentStatus = container.getStatus();
            
            // 根据Docker状态更新数据库状态
            if (dockerStatus.contains("running") && !"running".equals(currentStatus)) {
                container.setStatus("running");
                if (container.getStartTime() == null) {
                    container.setStartTime(LocalDateTime.now());
                }
            } else if (dockerStatus.contains("exited") && !"stopped".equals(currentStatus)) {
                container.setStatus("stopped");
                container.setStopTime(LocalDateTime.now());
            }
            
            // 更新IP信息
            updateContainerInfo(container);
            drillContainerRepository.save(container);
        }
    }
    
    /**
     * 批量同步所有容器状态
     */
    public void syncAllContainerStatus() {
        List<DrillContainer> containers = drillContainerRepository.findAll();
        for (DrillContainer container : containers) {
            if (container.getContainerId() != null) {
                try {
                    syncContainerStatus(container.getId());
                } catch (Exception e) {
                    // Debug statement removed
                }
            }
        }
    }
    
    /**
     * 更新容器状态
     */
    public void updateContainerStatus(Long containerId, String status) {
        Optional<DrillContainer> containerOpt = drillContainerRepository.findById(containerId);
        if (containerOpt.isPresent()) {
            DrillContainer container = containerOpt.get();
            container.setStatus(status);
            drillContainerRepository.save(container);
        }
    }
    
    /**
     * 部署容器的具体实现
     */
    private String deployContainer(DrillContainer container) {
        try {
            // 使用DockerService的runContainer方法部署容器
            String portMapping = container.getPort() + ":" + container.getPort();
            String result = dockerService.runContainer(
                container.getImageName(),
                container.getName(),
                portMapping,
                null // 环境变量暂时为空，可根据需要扩展
            );
            
            if (result.contains("容器创建成功")) {
                // 从结果中解析容器ID
                String[] parts = result.split("容器ID: ");
                if (parts.length > 1) {
                    String containerId = parts[1].trim();
                    container.setContainerId(containerId);
                }
                return "容器部署成功";
            } else {
                return result;
            }
            
        } catch (Exception e) {
            return "容器部署异常: " + e.getMessage();
        }
    }
    
    /**
     * 更新容器信息（IP等）
     */
    private void updateContainerInfo(DrillContainer container) {
        try {
            // 获取容器IP
            String[] command = {
                "docker", "inspect", "-f", "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
                container.getContainerId()
            };
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            if (process.waitFor() == 0) {
                String ip = new String(process.getInputStream().readAllBytes()).trim();
                if (!ip.isEmpty()) {
                    container.setIp(ip);
                }
            }
        } catch (Exception e) {
            // 忽略IP获取失败
        }
    }

    // ========== 新增：基于Asset配置的智能部署逻辑 ==========

    /**
     * 基于资产配置创建并部署容器
     */
    public DrillContainer createContainerFromAsset(Long rangeId, Long assetId) {
        Optional<Asset> assetOpt = assetRepository.findById(assetId);
        if (assetOpt.isEmpty()) {
            throw new RuntimeException("资产不存在: " + assetId);
        }
        
        Asset asset = assetOpt.get();
        
        // 验证资产是否支持容器化
        if (!asset.isContainerAsset() || !asset.hasDockerImage()) {
            throw new RuntimeException("资产不支持容器化部署: " + asset.getName());
        }
        
        // 选择最佳部署节点
        HostNode targetNode = selectOptimalNode(asset);
        if (targetNode == null) {
            throw new RuntimeException("没有可用的部署节点");
        }
        
        // 创建容器配置
        DrillContainer container = new DrillContainer();
        container.setRangeId(rangeId);
        container.setAssetId(assetId);
        container.setName(generateContainerName(asset));
        container.setImageName(asset.getDockerImage());
        container.setDescription("基于资产 " + asset.getName() + " 自动创建");
        
        // 设置节点信息
        container.setHostNodeId(targetNode.getId());
        container.setHostNodeName(targetNode.getDisplayName());
        container.setHostNodeIp(targetNode.getHostIp());
        
        // 生成完整容器名称
        container.generateContainerFullName(
            asset.getCompany(), 
            asset.getProject(), 
            asset.getName(), 
            targetNode.getName()
        );
        
        // 设置容器配置
        setContainerConfigFromAsset(container, asset);
        
        // 保存容器配置
        DrillContainer savedContainer = drillContainerRepository.save(container);
        
        // 如果设置了自动启动，则立即部署
        if (asset.getAssetType() != null && "container".equals(asset.getAssetType())) {
            deployContainerAsync(savedContainer.getId());
        }
        
        return savedContainer;
    }
    
    /**
     * 基于资产配置批量创建容器
     */
    public List<DrillContainer> createContainersFromAssets(Long rangeId, List<Long> assetIds) {
        List<DrillContainer> containers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (Long assetId : assetIds) {
            try {
                DrillContainer container = createContainerFromAsset(rangeId, assetId);
                containers.add(container);
            } catch (Exception e) {
                errors.add("资产 " + assetId + " 部署失败: " + e.getMessage());
                // Debug statement removed
            }
        }
        
        if (!errors.isEmpty()) {
            // Debug statement removed
        }
        
        return containers;
    }
    
    /**
     * 智能选择最佳部署节点
     */
    private HostNode selectOptimalNode(Asset asset) {
        List<HostNode> availableNodes = hostNodeService.getActiveNodes();
        
        if (availableNodes.isEmpty()) {
            return null;
        }
        
        // 如果资产有首选节点且可用，使用首选节点
        if (asset.getPreferredHostNodeId() != null) {
            Optional<HostNode> preferredNode = availableNodes.stream()
                .filter(node -> node.getId().equals(asset.getPreferredHostNodeId()))
                .findFirst();
            
            if (preferredNode.isPresent() && "fixed".equals(asset.getDeploymentStrategy())) {
                return preferredNode.get();
            }
        }
        
        // 根据部署策略选择节点
        String strategy = asset.getDeploymentStrategy();
        if ("load_balanced".equals(strategy)) {
            return selectNodeByLoadBalance(availableNodes);
        } else if ("any".equals(strategy)) {
            return availableNodes.get(0); // 返回第一个可用节点
        } else {
            // 默认使用首选节点或第一个可用节点
            if (asset.getPreferredHostNodeId() != null) {
                return availableNodes.stream()
                    .filter(node -> node.getId().equals(asset.getPreferredHostNodeId()))
                    .findFirst()
                    .orElse(availableNodes.get(0));
            }
            return availableNodes.get(0);
        }
    }
    
    /**
     * 基于负载均衡选择节点
     */
    private HostNode selectNodeByLoadBalance(List<HostNode> nodes) {
        HostNode bestNode = null;
        double bestScore = Double.MIN_VALUE;
        
        for (HostNode node : nodes) {
            double score = calculateNodeScore(node);
            if (score > bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }
        
        return bestNode;
    }
    
    /**
     * 计算节点评分（用于负载均衡）
     */
    private double calculateNodeScore(HostNode node) {
        double score = 0;
        
        // 基础优先级分数
        score += (node.getPriority() != null ? node.getPriority() : 1) * 10;
        
        // 资源可用性分数
        int maxContainers = node.getMaxContainers() != null ? node.getMaxContainers() : 50;
        long currentContainers = drillContainerRepository.countByHostNodeId(node.getId());
        double loadRatio = (double) currentContainers / maxContainers;
        score += (1 - loadRatio) * 50; // 负载越低分数越高
        
        // 本地节点优先
        if ("local".equals(node.getNodeType())) {
            score += 20;
        }
        
        // 环境匹配加分
        if ("production".equals(node.getEnvironment())) {
            score += 15;
        }
        
        return score;
    }
    
    /**
     * 生成容器名称
     */
    private String generateContainerName(Asset asset) {
        String baseName = asset.getName().toLowerCase()
            .replaceAll("[^a-zA-Z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        
        long timestamp = System.currentTimeMillis();
        return String.format("%s-%d", baseName, timestamp);
    }
    
    /**
     * 根据资产配置设置容器参数
     */
    private void setContainerConfigFromAsset(DrillContainer container, Asset asset) {
        // 设置端口配置
        if (asset.getContainerPorts() != null && !asset.getContainerPorts().trim().isEmpty()) {
            container.setExposedPorts(asset.getContainerPorts());
            // 尝试解析第一个端口作为主端口
            try {
                String[] ports = asset.getContainerPorts().split(",");
                if (ports.length > 0) {
                    String firstPort = ports[0].trim();
                    if (firstPort.contains(":")) {
                        String hostPort = firstPort.split(":")[0];
                        container.setPort(Integer.parseInt(hostPort));
                    } else {
                        container.setPort(Integer.parseInt(firstPort));
                    }
                }
            } catch (Exception e) {
                container.setPort(8080); // 默认端口
            }
        } else {
            container.setPort(8080); // 默认端口
        }
        
        // 设置环境变量
        if (asset.getEnvironments() != null && !asset.getEnvironments().trim().isEmpty()) {
            container.setEnvironmentVars(asset.getEnvironments());
        }
        
        // 设置数据卷
        if (asset.getVolumes() != null && !asset.getVolumes().trim().isEmpty()) {
            container.setVolumeMounts(asset.getVolumes());
        }
        
        // 设置资源限制
        Map<String, Object> resourceLimits = new HashMap<>();
        if (asset.getResourceLimitCpu() != null) {
            resourceLimits.put("cpu", asset.getResourceLimitCpu() + "m");
        }
        if (asset.getResourceLimitMemory() != null) {
            resourceLimits.put("memory", asset.getResourceLimitMemory() + "M");
        }
        if (!resourceLimits.isEmpty()) {
            // 这里可以转换为JSON字符串存储
            container.setResourceLimits(resourceLimits.toString());
        }
        
        // 设置健康检查
        if (asset.getHealthCheckUrl() != null && !asset.getHealthCheckUrl().trim().isEmpty()) {
            Map<String, Object> healthCheck = new HashMap<>();
            healthCheck.put("url", asset.getHealthCheckUrl());
            healthCheck.put("interval", "30s");
            healthCheck.put("timeout", "10s");
            healthCheck.put("retries", 3);
            container.setHealthCheckConfig(healthCheck.toString());
        }
        
        // 设置自动启动
        container.setAutoStart(true);
        
        // 设置重启策略
        container.setRestartPolicy("unless-stopped");
    }
    
    /**
     * 基于资产配置部署容器到指定节点
     */
    public String deployContainerToNode(DrillContainer container, HostNode node) {
        try {
            if (node.isLocal()) {
                // 本地节点部署
                return deployContainerLocally(container);
            } else {
                // 远程节点部署
                return deployContainerOnRemoteNode(container, node);
            }
        } catch (Exception e) {
            container.setStatus("failed");
            container.setDeployLog("部署失败: " + e.getMessage());
            drillContainerRepository.save(container);
            return "部署失败: " + e.getMessage();
        }
    }
    
    /**
     * 在本地节点部署容器
     */
    private String deployContainerLocally(DrillContainer container) {
        // 使用现有的部署逻辑
        return deployContainer(container);
    }
    
    /**
     * 在远程节点部署容器
     */
    private String deployContainerOnRemoteNode(DrillContainer container, HostNode node) {
        try {
            // 构建环境变量列表
            List<String> envVars = new ArrayList<>();
            if (container.getEnvironmentVars() != null) {
                // 解析环境变量JSON（简化版本）
                String[] vars = container.getEnvironmentVars().replace("{", "").replace("}", "").split(",");
                for (String var : vars) {
                    if (var.contains(":")) {
                        envVars.add(var.replace("\"", "").replace(" ", ""));
                    }
                }
            }
            
            // 构建端口映射
            String portMapping = null;
            if (container.getPort() != null) {
                portMapping = container.getPort() + ":" + container.getPort();
            }
            
            // 使用DockerService在指定节点创建容器
            String result = dockerService.createContainerOnNode(
                container.getImageName(),
                container.getContainerFullName(),
                portMapping,
                envVars,
                node.getId()
            );
            
            if (result.contains("创建成功")) {
                // 解析容器ID
                String[] parts = result.split("容器ID: ");
                if (parts.length > 1) {
                    container.setContainerId(parts[1].trim());
                }
                container.setStatus("running");
                container.setDeployTime(LocalDateTime.now());
                container.setStartTime(LocalDateTime.now());
            } else {
                container.setStatus("failed");
            }
            
            container.setDeployLog(result);
            drillContainerRepository.save(container);
            
            return result;
        } catch (Exception e) {
            return "远程部署失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取资产相关的所有容器
     */
    public List<DrillContainer> getContainersByAsset(Long assetId) {
        return drillContainerRepository.findByAssetId(assetId);
    }
    
    /**
     * 同步资产容器状态到资产管理
     */
    public void syncAssetContainerStatus(Long assetId) {
        List<DrillContainer> containers = getContainersByAsset(assetId);
        
        // 统计运行中的容器数量
        long runningCount = containers.stream()
            .filter(container -> "running".equals(container.getStatus()))
            .count();
        
        // 这里可以更新Asset的容器状态字段
        // 由于Asset实体可能没有这些字段，这里只做日志记录
        // Debug statement removed
    }
}