package org.cyberlab.controller;

import org.cyberlab.entity.HostNode;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.ImageInfo;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.DrillContainer;
import org.cyberlab.entity.ContainerDiscoveryRecord;
import org.cyberlab.service.HostNodeService;
import org.cyberlab.service.DockerService;
import org.cyberlab.service.AssetService;
import org.cyberlab.repository.DrillContainerRepository;
import org.cyberlab.repository.ContainerDiscoveryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin(origins = "*")
public class ContainerController {

    private static final Logger logger = LoggerFactory.getLogger(ContainerController.class);

    @Autowired
    private HostNodeService hostNodeService;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DrillContainerRepository drillContainerRepository;

    @Autowired
    private ContainerDiscoveryRecordRepository containerDiscoveryRecordRepository;

    /**
     * 通用容器发现接口 - 发现所有活跃节点上的容器
     */
    @PostMapping("/discover")
    public ResponseEntity<?> discoverAllContainers() {
        try {
            // 获取所有活跃的主机节点
            List<HostNode> activeNodes = hostNodeService.getActiveNodes();
            
            Map<String, Object> result = new HashMap<>();
            List<ContainerInfo> allContainers = new ArrayList<>();
            List<Map<String, Object>> nodesList = new ArrayList<>();
            
            // 遍历所有活跃节点，获取容器信息
            for (HostNode node : activeNodes) {
                try {
                    List<ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());
                    
                    // 为每个容器添加节点信息，并过滤掉无效的容器
                    for (ContainerInfo container : containers) {
                        if (container != null && container.getContainerId() != null && !container.getContainerId().isEmpty()) {
                            container.setNodeId(node.getId());
                            container.setNodeName(node.getDisplayName());
                            allContainers.add(container);
                        } else {
                            // Debug statement removed
                        }
                    }
                    
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("id", node.getId());
                    nodeInfo.put("displayName", node.getDisplayName());
                    nodeInfo.put("hostIp", node.getHostIp());
                    nodeInfo.put("status", node.getStatus());
                    nodeInfo.put("containerCount", containers.size());
                    nodesList.add(nodeInfo);
                    
                } catch (Exception e) {
                    // Debug statement removed
                    
                    // 即使节点连接失败也添加到列表中，但容器数为0
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("id", node.getId());
                    nodeInfo.put("displayName", node.getDisplayName());
                    nodeInfo.put("hostIp", node.getHostIp());
                    nodeInfo.put("status", "unreachable");
                    nodeInfo.put("containerCount", 0);
                    nodeInfo.put("error", e.getMessage());
                    nodesList.add(nodeInfo);
                }
            }
            
            result.put("containers", allContainers);
            result.put("nodes", nodesList);
            result.put("totalContainers", allContainers.size());
            result.put("totalNodes", activeNodes.size());
            result.put("discoveryTime", System.currentTimeMillis());
            
            if (allContainers.isEmpty()) {
                result.put("message", activeNodes.isEmpty() ? 
                    "没有找到活跃的主机节点" : 
                    "在 " + activeNodes.size() + " 个活跃节点上未发现任何容器，请检查节点连接状态和Docker服务");
            } else {
                result.put("message", "成功从 " + activeNodes.size() + " 个节点发现 " + allContainers.size() + " 个容器");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "通用容器发现失败: " + e.getMessage()));
        }
    }
    
    /**
     * 简化版：直接基于资产IP探测容器 - 不再依赖主机节点配置
     * 自动根据资产的IP地址和平台配置探测Docker容器和K8s资源
     * 注意：不使用事务，探测操作不需要事务保护，持久化部分有独立的异常处理
     */
    @PostMapping("/discover/project/{projectId}/direct")
    public ResponseEntity<?> discoverProjectContainersDirect(@PathVariable String projectId) {
        try {
            // URL解码项目ID，处理中文字符
            String decodedProjectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8);
            logger.info("========== 开始容器探测 ==========");
            logger.info("项目ID: {}", decodedProjectId);

            // 获取项目内的所有资产
            List<Asset> projectAssets = assetService.getAssetsByProjectId(decodedProjectId);
            logger.info("获取到 {} 个资产", projectAssets.size());

            Map<String, Object> result = new HashMap<>();
            List<ContainerInfo> allContainers = new ArrayList<>();
            List<Map<String, Object>> assetProbeResults = new ArrayList<>();

            int successCount = 0;
            int failedCount = 0;
            int notConfiguredCount = 0;

            // 遍历每个资产，直接使用其IP探测容器
            for (Asset asset : projectAssets) {
                logger.info("--- 处理资产: ID={}, Name={}, IP={}, Platform={}",
                    asset.getId(), asset.getName(), asset.getIp(), asset.getAssetPlatform());

                Map<String, Object> probeResult = new HashMap<>();
                probeResult.put("assetId", asset.getId());
                probeResult.put("assetName", asset.getName());
                probeResult.put("assetIp", asset.getIp());
                probeResult.put("platform", asset.getAssetPlatform() != null ? asset.getAssetPlatform() : "docker");
                probeResult.put("dockerPort", asset.getDockerPort() != null ? asset.getDockerPort() : 2375);

                // 检查资产是否配置了探测
                boolean dockerConfigured = asset.isDockerApiConfigured();
                boolean k8sConfigured = asset.isK8sApiConfigured();
                logger.info("配置检查: isDockerApiConfigured={}, isK8sApiConfigured={}",
                    dockerConfigured, k8sConfigured);

                if (!dockerConfigured && !k8sConfigured) {
                    probeResult.put("status", "not_configured");
                    probeResult.put("message", "未配置Docker或K8s探测");
                    notConfiguredCount++;
                    assetProbeResults.add(probeResult);
                    continue;
                }

                // Docker 探测
                if (asset.isDockerApiConfigured()) {
                    try {
                        String dockerUrl = asset.getDockerApiUrl();
                        logger.info("开始Docker探测: dockerUrl={}", dockerUrl);

                        List<ContainerInfo> containers = dockerService.getContainersFromUrl(dockerUrl);
                        logger.info("Docker探测完成: 发现 {} 个容器", containers.size());

                        // 为容器添加资产关联信息
                        for (ContainerInfo container : containers) {
                            if (container != null && container.getContainerId() != null) {
                                container.setNodeName(asset.getName());
                                container.setAssetId(asset.getId());
                                // 设置nodeId为资产ID,保持前端兼容性
                                container.setNodeId(asset.getId());
                                allContainers.add(container);
                            }
                        }

                        probeResult.put("dockerStatus", "success");
                        probeResult.put("dockerContainerCount", containers.size());
                        probeResult.put("dockerUrl", dockerUrl);
                        successCount++;

                        // 更新资产探测状态
                        asset.setProbeStatus("success");
                        asset.setLastProbeTime(LocalDateTime.now().toString());
                        asset.setProbeErrorMessage(null);
                        assetService.save(asset);

                    } catch (Exception e) {
                        String errorMessage = e.getMessage();
                        String userFriendlyMessage = errorMessage;

                        // 提供更友好的错误提示
                        if (e instanceof IllegalStateException && errorMessage != null && errorMessage.contains("IP格式无效")) {
                            // IP格式验证失败
                            userFriendlyMessage = errorMessage;
                        } else if (errorMessage != null && errorMessage.contains("no such host")) {
                            // DNS解析失败
                            userFriendlyMessage = "主机名无法解析，请检查IP地址格式是否正确（不能包含中文字符）";
                        } else if (errorMessage != null && (errorMessage.contains("Connection refused") || errorMessage.contains("Connection timed out"))) {
                            // 连接被拒绝或超时
                            userFriendlyMessage = "无法连接到Docker API，请检查：1) Docker服务是否运行 2) 端口是否正确 3) 防火墙设置";
                        } else if (errorMessage != null && errorMessage.contains("connect:")) {
                            // 通用连接错误
                            userFriendlyMessage = "网络连接失败: " + errorMessage;
                        }

                        logger.error("Docker探测失败 [资产: {}]: {}", asset.getName(), userFriendlyMessage, e);
                        probeResult.put("dockerStatus", "failed");
                        probeResult.put("dockerError", userFriendlyMessage);
                        failedCount++;

                        // 更新资产探测状态
                        asset.setProbeStatus("failed");
                        asset.setLastProbeTime(LocalDateTime.now().toString());
                        asset.setProbeErrorMessage("Docker探测失败: " + userFriendlyMessage);
                        assetService.save(asset);
                    }
                }

                // K8s 探测 (如果配置了)
                if (asset.isK8sApiConfigured()) {
                    try {
                        // TODO: 实现K8s探测逻辑
                        probeResult.put("k8sStatus", "not_implemented");
                        probeResult.put("k8sMessage", "Kubernetes探测功能开发中");
                    } catch (Exception e) {
                        probeResult.put("k8sStatus", "failed");
                        probeResult.put("k8sError", e.getMessage());
                    }
                }

                probeResult.put("status", probeResult.containsKey("dockerStatus") && "success".equals(probeResult.get("dockerStatus")) ? "success" : "failed");
                assetProbeResults.add(probeResult);
            }

            // 🔧 构建虚拟nodes数组 - 基于成功探测的资产，保持前端兼容性
            List<Map<String, Object>> nodesList = new ArrayList<>();
            for (Map<String, Object> probeResult : assetProbeResults) {
                if ("success".equals(probeResult.get("status"))) {
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("id", probeResult.get("assetId"));
                    nodeInfo.put("displayName", probeResult.get("assetName"));
                    nodeInfo.put("hostIp", probeResult.get("assetIp"));
                    nodeInfo.put("dockerPort", probeResult.get("dockerPort"));
                    nodeInfo.put("status", "active");
                    nodeInfo.put("containerCount", probeResult.get("dockerContainerCount"));
                    nodeInfo.put("nodeType", "direct_probe"); // 标识这是直接探测模式的虚拟节点
                    nodeInfo.put("environment", probeResult.get("platform"));
                    nodesList.add(nodeInfo);
                }
            }

            result.put("containers", allContainers);
            result.put("nodes", nodesList);  // 添加虚拟节点列表，保持前端兼容性
            result.put("totalNodes", nodesList.size());
            result.put("assetProbeResults", assetProbeResults);
            result.put("totalContainers", allContainers.size());
            result.put("totalAssets", projectAssets.size());
            result.put("successfulProbes", successCount);
            result.put("failedProbes", failedCount);
            result.put("notConfiguredAssets", notConfiguredCount);
            result.put("discoveryTime", System.currentTimeMillis());
            result.put("discoveryMode", "direct_ip_based");

            String message;
            if (projectAssets.isEmpty()) {
                message = "项目中没有资产";
            } else if (notConfiguredCount == projectAssets.size()) {
                message = "项目中有 " + projectAssets.size() + " 个资产，但都未配置容器探测。请在资产编辑中配置Docker或K8s平台";
            } else if (successCount == 0) {
                message = "探测了 " + (successCount + failedCount) + " 个资产，但全部失败。请检查资产IP和Docker API配置";
            } else {
                message = String.format("成功探测 %d/%d 个资产，发现 %d 个容器",
                    successCount, projectAssets.size(), allContainers.size());
            }
            result.put("message", message);

            // 🔄 增量同步逻辑：以探测结果为准，持久化到数据库
            try {
                // 1. 查询数据库中该项目的现有容器记录
                List<ContainerDiscoveryRecord> existingRecords = containerDiscoveryRecordRepository.findByProjectId(decodedProjectId);

                // 2. 构建现有容器ID集合（数据库中的）
                Set<String> existingContainerIds = existingRecords.stream()
                    .map(ContainerDiscoveryRecord::getContainerId)
                    .collect(Collectors.toSet());

                // 3. 构建探测到的容器ID集合（实时探测的）
                Set<String> discoveredContainerIds = allContainers.stream()
                    .map(ContainerInfo::getContainerId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toSet());

                // 4. ➖ 删除：数据库有但探测不到的容器（已销毁）
                List<String> toDelete = existingContainerIds.stream()
                    .filter(id -> !discoveredContainerIds.contains(id))
                    .collect(Collectors.toList());

                int deletedCount = 0;
                if (!toDelete.isEmpty()) {
                    containerDiscoveryRecordRepository.deleteByProjectIdAndContainerIdIn(decodedProjectId, toDelete);
                    deletedCount = toDelete.size();
                    logger.info("项目 {} 删除了 {} 个已销毁的容器记录", decodedProjectId, deletedCount);
                }

                // 5. ➕ 新增/更新：探测到的容器
                int addedCount = 0;
                int updatedCount = 0;

                for (ContainerInfo container : allContainers) {
                    if (container.getContainerId() == null || container.getContainerId().isEmpty()) {
                        continue;
                    }

                    Optional<ContainerDiscoveryRecord> existingOpt =
                        containerDiscoveryRecordRepository.findByProjectIdAndContainerId(decodedProjectId, container.getContainerId());

                    ContainerDiscoveryRecord record;
                    if (existingOpt.isPresent()) {
                        // 更新现有记录
                        record = existingOpt.get();
                        record.setStatus(container.getStatus());
                        record.setContainerName(container.getName());
                        record.setImage(container.getImage());
                        record.setLastSeenAt(LocalDateTime.now());
                        updatedCount++;
                    } else {
                        // 新增记录
                        record = new ContainerDiscoveryRecord();
                        record.setProjectId(decodedProjectId);
                        record.setAssetId(container.getAssetId());
                        record.setAssetName(container.getNodeName());

                        // 从assetProbeResults中找到对应的资产IP
                        for (Map<String, Object> probeResult : assetProbeResults) {
                            if (container.getAssetId() != null &&
                                container.getAssetId().equals(probeResult.get("assetId"))) {
                                record.setAssetIp((String) probeResult.get("assetIp"));
                                break;
                            }
                        }

                        record.setContainerId(container.getContainerId());
                        record.setContainerName(container.getName());
                        record.setImage(container.getImage());
                        record.setStatus(container.getStatus());

                        // 保存端口信息（ContainerInfo中的portMappings字段已经是JSON格式）
                        if (container.getPortMappings() != null && !container.getPortMappings().isEmpty()) {
                            record.setPorts(container.getPortMappings());
                        }

                        // 保存标签信息（ContainerInfo中的tags字段已经是JSON格式）
                        if (container.getTags() != null && !container.getTags().isEmpty()) {
                            record.setLabels(container.getTags());
                        }

                        record.setDiscoveredAt(LocalDateTime.now());
                        record.setLastSeenAt(LocalDateTime.now());
                        addedCount++;
                    }

                    containerDiscoveryRecordRepository.save(record);
                }

                // 6. 统计同步结果
                Map<String, Object> syncStats = new HashMap<>();
                syncStats.put("added", addedCount);
                syncStats.put("updated", updatedCount);
                syncStats.put("deleted", deletedCount);
                syncStats.put("totalInDatabase", addedCount + updatedCount);
                result.put("syncStats", syncStats);

                logger.info("项目 {} 增量同步完成：新增={}, 更新={}, 删除={}",
                    decodedProjectId, addedCount, updatedCount, deletedCount);

            } catch (Exception syncError) {
                logger.error("增量同步失败，但探测结果仍然返回", syncError);
                result.put("syncError", "数据持久化失败: " + syncError.getMessage());
            }

            logger.info("========== 容器探测完成 ==========");
            logger.info("总结: 资产总数={}, 成功={}, 失败={}, 未配置={}, 发现容器数={}",
                projectAssets.size(), successCount, failedCount, notConfiguredCount, allContainers.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("容器探测失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "容器探测失败: " + e.getMessage(),
                "details", e.toString()
            ));
        }
    }

    /**
     * 发现项目镜像列表 - 基于资产IP探测Docker镜像
     * 自动根据资产的IP地址和平台配置探测Docker镜像
     */
    @PostMapping("/discover/project/{projectId}/images")
    public ResponseEntity<?> discoverProjectImages(@PathVariable String projectId) {
        try {
            // URL解码项目ID，处理中文字符
            String decodedProjectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8);
            logger.info("========== 开始镜像探测 ==========");
            logger.info("项目ID: {}", decodedProjectId);

            // 获取项目内的所有资产
            List<Asset> projectAssets = assetService.getAssetsByProjectId(decodedProjectId);
            logger.info("获取到 {} 个资产", projectAssets.size());

            Map<String, Object> result = new HashMap<>();
            List<ImageInfo> allImages = new ArrayList<>();
            List<Map<String, Object>> assetProbeResults = new ArrayList<>();

            int successCount = 0;
            int failedCount = 0;
            int notConfiguredCount = 0;

            // 遍历每个资产，直接使用其IP探测镜像
            for (Asset asset : projectAssets) {
                logger.info("--- 处理资产: ID={}, Name={}, IP={}, Platform={}",
                    asset.getId(), asset.getName(), asset.getIp(), asset.getAssetPlatform());

                Map<String, Object> probeResult = new HashMap<>();
                probeResult.put("assetId", asset.getId());
                probeResult.put("assetName", asset.getName());
                probeResult.put("assetIp", asset.getIp());
                probeResult.put("platform", asset.getAssetPlatform() != null ? asset.getAssetPlatform() : "docker");
                probeResult.put("dockerPort", asset.getDockerPort() != null ? asset.getDockerPort() : 2375);

                // 检查资产是否配置了Docker探测
                boolean dockerConfigured = asset.isDockerApiConfigured();
                logger.info("配置检查: isDockerApiConfigured={}", dockerConfigured);

                if (!dockerConfigured) {
                    probeResult.put("status", "not_configured");
                    probeResult.put("message", "未配置Docker探测");
                    notConfiguredCount++;
                    assetProbeResults.add(probeResult);
                    continue;
                }

                // Docker 镜像探测
                if (asset.isDockerApiConfigured()) {
                    try {
                        String dockerUrl = asset.getDockerApiUrl();
                        logger.info("开始Docker镜像探测: dockerUrl={}", dockerUrl);

                        List<ImageInfo> images = dockerService.getImagesFromUrl(dockerUrl);
                        logger.info("Docker镜像探测完成: 发现 {} 个镜像", images.size());

                        // 为镜像添加资产关联信息
                        for (ImageInfo image : images) {
                            if (image != null && image.getImageId() != null) {
                                image.setHostNodeName(asset.getName());
                                image.setHostNodeIp(asset.getIp());
                                image.setHostNodeId(asset.getId());
                                allImages.add(image);
                            }
                        }

                        probeResult.put("dockerStatus", "success");
                        probeResult.put("dockerImageCount", images.size());
                        probeResult.put("dockerUrl", dockerUrl);
                        successCount++;

                        // 更新资产探测状态
                        asset.setProbeStatus("success");
                        asset.setLastProbeTime(LocalDateTime.now().toString());
                        asset.setProbeErrorMessage(null);
                        assetService.save(asset);

                    } catch (Exception e) {
                        String errorMessage = e.getMessage();
                        String userFriendlyMessage = errorMessage;

                        // 提供更友好的错误提示
                        if (e instanceof IllegalStateException && errorMessage != null && errorMessage.contains("IP格式无效")) {
                            userFriendlyMessage = errorMessage;
                        } else if (errorMessage != null && errorMessage.contains("no such host")) {
                            userFriendlyMessage = "主机名无法解析，请检查IP地址格式是否正确（不能包含中文字符）";
                        } else if (errorMessage != null && (errorMessage.contains("Connection refused") || errorMessage.contains("Connection timed out"))) {
                            userFriendlyMessage = "无法连接到Docker API，请检查：1) Docker服务是否运行 2) 端口是否正确 3) 防火墙设置";
                        } else if (errorMessage != null && errorMessage.contains("connect:")) {
                            userFriendlyMessage = "网络连接失败: " + errorMessage;
                        }

                        logger.error("Docker镜像探测失败 [资产: {}]: {}", asset.getName(), userFriendlyMessage, e);
                        probeResult.put("dockerStatus", "failed");
                        probeResult.put("dockerError", userFriendlyMessage);
                        failedCount++;

                        // 更新资产探测状态
                        asset.setProbeStatus("failed");
                        asset.setLastProbeTime(LocalDateTime.now().toString());
                        asset.setProbeErrorMessage("Docker镜像探测失败: " + userFriendlyMessage);
                        assetService.save(asset);
                    }
                }

                probeResult.put("status", probeResult.containsKey("dockerStatus") && "success".equals(probeResult.get("dockerStatus")) ? "success" : "failed");
                assetProbeResults.add(probeResult);
            }

            // 构建虚拟nodes数组 - 基于成功探测的资产，保持前端兼容性
            List<Map<String, Object>> nodesList = new ArrayList<>();
            for (Map<String, Object> probeResult : assetProbeResults) {
                if ("success".equals(probeResult.get("status"))) {
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("id", probeResult.get("assetId"));
                    nodeInfo.put("displayName", probeResult.get("assetName"));
                    nodeInfo.put("hostIp", probeResult.get("assetIp"));
                    nodeInfo.put("dockerPort", probeResult.get("dockerPort"));
                    nodeInfo.put("status", "active");
                    nodeInfo.put("imageCount", probeResult.get("dockerImageCount"));
                    nodeInfo.put("nodeType", "direct_probe");
                    nodeInfo.put("environment", probeResult.get("platform"));
                    nodesList.add(nodeInfo);
                }
            }

            result.put("images", allImages);
            result.put("nodes", nodesList);
            result.put("totalNodes", nodesList.size());
            result.put("assetProbeResults", assetProbeResults);
            result.put("totalImages", allImages.size());
            result.put("totalAssets", projectAssets.size());
            result.put("successfulProbes", successCount);
            result.put("failedProbes", failedCount);
            result.put("notConfiguredAssets", notConfiguredCount);
            result.put("discoveryTime", System.currentTimeMillis());
            result.put("discoveryMode", "direct_ip_based");

            String message;
            if (projectAssets.isEmpty()) {
                message = "项目中没有资产";
            } else if (notConfiguredCount == projectAssets.size()) {
                message = "项目中有 " + projectAssets.size() + " 个资产，但都未配置镜像探测。请在资产编辑中配置Docker平台";
            } else if (successCount == 0) {
                message = "探测了 " + (successCount + failedCount) + " 个资产，但全部失败。请检查资产IP和Docker API配置";
            } else {
                message = String.format("成功探测 %d/%d 个资产，发现 %d 个镜像",
                    successCount, projectAssets.size(), allImages.size());
            }
            result.put("message", message);

            logger.info("========== 镜像探测完成 ==========");
            logger.info("总结: 资产总数={}, 成功={}, 失败={}, 未配置={}, 发现镜像数={}",
                projectAssets.size(), successCount, failedCount, notConfiguredCount, allImages.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("镜像探测失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "镜像探测失败: " + e.getMessage(),
                "details", e.toString()
            ));
        }
    }

    /**
     * 发现单个资产的Docker镜像
     * @param assetId 资产ID
     * @return 镜像列表
     */
    @PostMapping("/discover/asset/{assetId}/images")
    public ResponseEntity<?> discoverAssetImages(@PathVariable Long assetId) {
        try {
            logger.info("========== 开始单资产镜像探测 ==========");
            logger.info("资产ID: {}", assetId);

            // 1. 获取资产信息
            Asset asset = assetService.getAssetById(assetId)
                .orElseThrow(() -> new RuntimeException("资产不存在: " + assetId));

            logger.info("资产名称: {}, IP: {}, 平台: {}",
                asset.getName(), asset.getIp(), asset.getAssetPlatform());

            List<ImageInfo> images = new ArrayList<>();
            boolean hasDockerPlatform = false;

            // 2. 检查Docker平台配置
            if (asset.isDockerApiConfigured()) {
                hasDockerPlatform = true;
                String dockerUrl = asset.getDockerApiUrl();
                logger.info("使用Docker配置: {}", dockerUrl);

                try {
                    images = dockerService.getImagesFromUrl(dockerUrl);
                    logger.info("从资产 {} 发现 {} 个镜像", asset.getName(), images.size());

                    // 为镜像添加资产关联信息
                    for (ImageInfo image : images) {
                        if (image != null && image.getImageId() != null) {
                            image.setHostNodeName(asset.getName());
                            image.setHostNodeIp(asset.getIp());
                            image.setHostNodeId(asset.getId());
                        }
                    }
                } catch (Exception e) {
                    logger.error("从资产 {} 获取镜像失败: {}", asset.getName(), e.getMessage());
                }
            } else {
                logger.info("资产 {} 未配置Docker API探测", asset.getName());
            }

            // 3. 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("assetId", assetId);
            result.put("assetName", asset.getName());
            result.put("assetIp", asset.getIp());
            result.put("hasDockerPlatform", hasDockerPlatform);
            result.put("images", images);
            result.put("totalImages", images.size());
            result.put("discoveryTime", System.currentTimeMillis());

            logger.info("========== 资产镜像探测完成 ==========");
            logger.info("资产ID: {}, 发现镜像: {}", assetId, images.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("资产镜像探测失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "资产镜像探测失败: " + e.getMessage(),
                "details", e.toString()
            ));
        }
    }

    /**
     * 发现项目关联节点上的容器（旧版本 - 基于主机节点）
     */
    @PostMapping("/discover/project/{projectId}")
    public ResponseEntity<?> discoverProjectContainers(@PathVariable String projectId) {
        try {
            // URL解码项目ID，处理中文字符
            String decodedProjectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8);

            // 获取项目内的所有资产
            List<Asset> projectAssets = assetService.getAssetsByProjectId(decodedProjectId);

            // 获取项目内关联了主机节点的资产（包括any策略的资产）
            List<Asset> assetsWithNodes = assetService.getProjectAssetsWithHostNodes(decodedProjectId);

            // 获取项目内有明确节点指定的资产
            List<Asset> fixedStrategyAssets = assetService.getProjectFixedStrategyAssets(decodedProjectId);

            // 获取项目内使用"any"部署策略的资产
            List<Asset> anyStrategyAssets = assetService.getProjectAnyStrategyAssets(decodedProjectId);

            Map<String, Object> result = new HashMap<>();
            List<ContainerInfo> allContainers = new ArrayList<>();
            List<Map<String, Object>> nodesList = new ArrayList<>();

            // 🔧 新增：验证节点有效性，识别无效节点
            List<Long> invalidNodeIds = new ArrayList<>();
            int assetsWithInvalidNodes = 0;

            // 检查固定策略资产的节点是否存在
            for (Asset asset : fixedStrategyAssets) {
                Long nodeId = asset.getPreferredHostNodeId();
                if (nodeId != null) {
                    Optional<HostNode> nodeOpt = hostNodeService.getNodeById(nodeId);
                    if (nodeOpt.isEmpty()) {
                        // 节点已被删除
                        if (!invalidNodeIds.contains(nodeId)) {
                            invalidNodeIds.add(nodeId);
                        }
                        assetsWithInvalidNodes++;
                    }
                }
            }

            // 如果存在无效节点，提前返回错误信息
            if (!invalidNodeIds.isEmpty()) {
                result.put("containers", allContainers);
                result.put("nodes", nodesList);
                result.put("totalContainers", 0);
                result.put("totalNodes", 0);
                result.put("totalAssets", projectAssets.size());
                result.put("assetsWithNodes", assetsWithNodes.size());
                result.put("assetsWithInvalidNodes", assetsWithInvalidNodes);
                result.put("invalidNodeIds", invalidNodeIds);
                result.put("discoveryTime", System.currentTimeMillis());
                result.put("error", "invalid_nodes");
                result.put("message", String.format(
                    "项目中有 %d 个资产配置的节点已被删除，请重新为这些资产配置部署节点",
                    assetsWithInvalidNodes
                ));

                return ResponseEntity.ok(result);
            }

            // 如果没有任何资产关联节点，返回详细信息
            if (assetsWithNodes.isEmpty()) {
                result.put("containers", allContainers);
                result.put("nodes", nodesList);
                result.put("totalContainers", 0);
                result.put("totalNodes", 0);
                result.put("totalAssets", projectAssets.size());
                result.put("assetsWithNodes", 0);
                result.put("assetsWithInvalidNodes", 0);
                result.put("discoveryTime", System.currentTimeMillis());
                result.put("message", projectAssets.isEmpty() ?
                    "项目中没有任何资产" :
                    "项目中有 " + projectAssets.size() + " 个资产，但没有资产关联到主机节点。请在资产管理中为资产选择部署节点");

                return ResponseEntity.ok(result);
            }

            // 使用智能负载均衡算法为any策略的资产分配节点
            Map<String, Object> redistributionResult = null;
            if (!anyStrategyAssets.isEmpty()) {
                redistributionResult = assetService.redistributeProjectAssets(decodedProjectId, null);
            }

            // 获取明确指定的节点ID列表
            List<Long> explicitNodeIds = assetService.getProjectAssociatedNodeIds(decodedProjectId);

            // 获取可用节点列表（用于any策略资产）
            List<HostNode> availableNodes = new ArrayList<>();
            if (!anyStrategyAssets.isEmpty()) {
                availableNodes = hostNodeService.getAvailableNodesForDeployment();
            }

            // 合并所有需要发现容器的节点
            Set<Long> allNodeIds = new HashSet<>(explicitNodeIds);
            List<Long> availableNodeIds = availableNodes.stream()
                .map(HostNode::getId)
                .collect(Collectors.toList());
            allNodeIds.addAll(availableNodeIds);

            // 从重新分配结果中获取实际使用的节点（如果有）
            Map<Long, Map<String, Object>> nodeAssignments = new HashMap<>();
            if (redistributionResult != null && (boolean) redistributionResult.get("success")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> redistributions =
                    (List<Map<String, Object>>) redistributionResult.get("redistributions");

                for (Map<String, Object> redistribution : redistributions) {
                    Long nodeId = ((Number) redistribution.get("newNodeId")).longValue();
                    nodeAssignments.put(nodeId, redistribution);
                }
            }

            // 获取节点详细信息并发现容器
            for (Long nodeId : allNodeIds) {
                try {
                    Optional<HostNode> nodeOpt = hostNodeService.getNodeById(nodeId);
                    if (nodeOpt.isEmpty()) {
                        continue;
                    }

                    HostNode node = nodeOpt.get();

                    // 获取该节点上的容器
                    List<ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());

                    // 为每个容器添加节点信息
                    for (ContainerInfo container : containers) {
                        if (container != null && container.getContainerId() != null && !container.getContainerId().isEmpty()) {
                            container.setNodeId(node.getId());
                            container.setNodeName(node.getDisplayName());
                            allContainers.add(container);
                        }
                    }

                    // 计算关联的资产数量
                    long explicitAssetCount = fixedStrategyAssets.stream()
                        .filter(asset -> nodeId.equals(asset.getPreferredHostNodeId()))
                        .count();

                    long anyAssetCount = 0;
                    if (nodeAssignments.containsKey(nodeId)) {
                        anyAssetCount = anyStrategyAssets.stream()
                            .filter(asset -> nodeAssignments.get(nodeId).get("assetId").equals(asset.getId()))
                            .count();
                    }

                    // 创建节点信息
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("id", node.getId());
                    nodeInfo.put("displayName", node.getDisplayName());
                    nodeInfo.put("hostIp", node.getHostIp());
                    nodeInfo.put("dockerPort", node.getDockerPort());
                    nodeInfo.put("status", node.getStatus());
                    nodeInfo.put("associatedAssets", explicitAssetCount + anyAssetCount);
                    nodeInfo.put("explicitAssets", explicitAssetCount);
                    nodeInfo.put("anyStrategyAssets", anyAssetCount);
                    nodeInfo.put("nodeType", explicitAssetCount > 0 ? "explicit" : "anyStrategy");
                    nodeInfo.put("containerCount", containers.size());
                    nodeInfo.put("environment", node.getEnvironment());
                    nodeInfo.put("priority", node.getPriority());

                    // 如果是智能分配的节点，添加分配详情
                    if (nodeAssignments.containsKey(nodeId)) {
                        Map<String, Object> assignment = nodeAssignments.get(nodeId);
                        nodeInfo.put("assignmentReason", assignment.get("selectionReason"));
                        nodeInfo.put("nodeScore", assignment.get("nodeScore"));
                        nodeInfo.put("loadRatio", assignment.get("loadRatio"));
                        nodeInfo.put("algorithmType", "智能负载均衡");
                    }

                    nodesList.add(nodeInfo);

                } catch (Exception e) {
                    // Debug statement removed
                }
            }

            result.put("containers", allContainers);
            result.put("nodes", nodesList);
            result.put("totalContainers", allContainers.size());
            result.put("totalNodes", allNodeIds.size());
            result.put("totalAssets", projectAssets.size());
            result.put("assetsWithNodes", assetsWithNodes.size());
            result.put("assetsWithInvalidNodes", assetsWithInvalidNodes);
            result.put("invalidNodeIds", invalidNodeIds);
            result.put("explicitNodeAssets", fixedStrategyAssets.size());
            result.put("anyStrategyAssets", anyStrategyAssets.size());
            result.put("discoveryTime", System.currentTimeMillis());

            // 添加智能分配结果信息
            if (redistributionResult != null) {
                result.put("redistributionResult", redistributionResult);
                if ((boolean) redistributionResult.get("success")) {
                    result.put("message", String.format(
                        "智能容器发现：%d个固定节点资产 + %d个智能分配资产，共使用%d个节点",
                        fixedStrategyAssets.size(),
                        anyStrategyAssets.size(),
                        allNodeIds.size()
                    ));
                }
            } else if (!anyStrategyAssets.isEmpty()) {
                result.put("message", String.format(
                    "容器发现：%d个明确节点资产，共使用%d个节点",
                    fixedStrategyAssets.size(),
                    allNodeIds.size()
                ));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("容器发现失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "容器发现失败: " + e.getMessage(),
                "details", e.toString()
            ));
        }
    }
    
    /**
     * 获取项目的容器状态
     */
    @GetMapping("/project-status")
    public ResponseEntity<?> getProjectContainerStatus(@RequestParam String projectId) {
        try {
            // URL解码项目ID，处理中文字符
            String decodedProjectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8);

            // 获取项目相关的资产
            List<Asset> projectAssets = assetService.getAssetsByProjectId(decodedProjectId);

            // 统计容器状态信息
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("projectId", decodedProjectId);
            statusInfo.put("totalAssets", projectAssets.size());
            
            // 统计不同类型的资产
            long containerAssets = projectAssets.stream()
                .filter(asset -> "容器".equals(asset.getAssetType()))
                .count();
            long vmAssets = projectAssets.stream()
                .filter(asset -> "虚拟机".equals(asset.getAssetType()))
                .count();
            long physicalAssets = projectAssets.stream()
                .filter(asset -> "物理机".equals(asset.getAssetType()))
                .count();
            
            statusInfo.put("containerAssets", containerAssets);
            statusInfo.put("vmAssets", vmAssets);
            statusInfo.put("physicalAssets", physicalAssets);
            
            // 统计运行状态
            List<Map<String, Object>> assetList = new ArrayList<>();
            for (Asset asset : projectAssets) {
                Map<String, Object> assetInfo = new HashMap<>();
                assetInfo.put("id", asset.getId());
                assetInfo.put("name", asset.getName());
                assetInfo.put("type", asset.getAssetType());
                assetInfo.put("ip", asset.getIp());
                assetInfo.put("status", "active");
                assetInfo.put("nodeStrategy", "fixed");
                assetList.add(assetInfo);
            }
            statusInfo.put("assets", assetList);

            // 从数据库统计运行中的容器数量（不调用Docker API）
            List<HostNode> activeNodes = hostNodeService.getActiveNodes();
            long totalContainers = 0;
            for (HostNode node : activeNodes) {
                // 查询数据库中该节点上status='running'的容器数量
                totalContainers += drillContainerRepository.countByHostNodeIdAndStatus(
                    node.getId(), "running"
                );
            }
            statusInfo.put("runningContainers", totalContainers);
            statusInfo.put("activeNodes", activeNodes.size());
            
            return ResponseEntity.ok(statusInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取容器项目状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取容器详情
     */
    @GetMapping("/{containerId}/details")
    public ResponseEntity<?> getContainerDetails(@PathVariable String containerId, @RequestParam Long nodeId) {
        try {
            // 从数据库查询容器记录（不调用Docker API）
            Optional<DrillContainer> containerOpt = drillContainerRepository.findByContainerId(containerId);

            if (containerOpt.isPresent()) {
                DrillContainer container = containerOpt.get();

                // 构造返回的ContainerInfo对象
                ContainerInfo info = new ContainerInfo();
                info.setContainerId(container.getContainerId());
                info.setName(container.getName());
                info.setImage(container.getImageName());
                info.setStatus(container.getStatus());
                info.setHostNodeId(container.getHostNodeId());
                info.setHostNodeName(container.getHostNodeName());
                info.setHostNodeIp(container.getHostNodeIp());
                info.setCpuUsage("N/A");  // 不查询实时资源
                info.setMemoryUsage("N/A");

                // 设置端口映射信息（如果有）
                if (container.getPort() != null) {
                    info.setPortMappings(container.getIp() + ":" + container.getPort());
                }

                return ResponseEntity.ok(info);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取容器详情失败: " + e.getMessage()));
        }
    }
    private int generateRandomUsage(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }
    
    private String formatUptime(String createdAtStr) {
        if (createdAtStr == null || createdAtStr.isEmpty()) {
            return "未知";
        }
        
        try {
            // 尝试解析时间字符串为LocalDateTime
            LocalDateTime createdAt = LocalDateTime.parse(createdAtStr.replace(" ", "T"));
            LocalDateTime now = LocalDateTime.now();
            long hours = java.time.Duration.between(createdAt, now).toHours();
            long minutes = java.time.Duration.between(createdAt, now).toMinutes() % 60;
            
            if (hours > 0) {
                return hours + "小时" + minutes + "分钟";
            } else {
                return minutes + "分钟";
            }
        } catch (Exception e) {
            // 如果解析失败，返回原始字符串或默认值
            return createdAtStr;
        }
    }
}