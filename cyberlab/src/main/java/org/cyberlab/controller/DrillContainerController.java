package org.cyberlab.controller;

import org.cyberlab.entity.DrillContainer;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.DrillAsset;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.ImageInfo;
import org.cyberlab.entity.CyberRange;
import org.cyberlab.entity.DrillAssetImageMapping;
import org.cyberlab.service.DrillContainerService;
import org.cyberlab.service.DockerService;
import org.cyberlab.service.AssetService;
import org.cyberlab.service.DrillAssetService;
import org.cyberlab.service.ProjectAwareContainerTransferService;
import org.cyberlab.service.ContainerDiscoveryService;
import org.cyberlab.repository.CyberRangeRepository;
import org.cyberlab.repository.DrillContainerRepository;
import org.cyberlab.repository.DrillAssetImageMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drills")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class DrillContainerController {

    private static final Logger logger = LoggerFactory.getLogger(DrillContainerController.class);

    @Autowired
    private DrillContainerService drillContainerService;
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private DrillAssetService drillAssetService;
    
    @Autowired
    private CyberRangeRepository cyberRangeRepository;
    
    @Autowired
    private DrillContainerRepository drillContainerRepository;
    
    @Autowired
    private ProjectAwareContainerTransferService projectAwareTransferService;

    @Autowired
    private ContainerDiscoveryService containerDiscoveryService;

    @Autowired
    private ContainerController containerController;

    @Autowired
    private DrillAssetImageMappingRepository mappingRepository;

    // 🔧 内存存储：演练ID -> 资产部署状态映射（仅存储部署状态，不存储资产数据）
    private static final Map<String, Map<String, Object>> assetDeploymentStatusMap = new ConcurrentHashMap<>();

    // 🆕 内存存储：演练-资产关联关系 (演练ID -> Set<资产ID>)
    private static final Map<Long, Set<Long>> drillAssetMap = new ConcurrentHashMap<>();

    // 🆕 内存存储：演练-资产-镜像条目 (key: rangeId:assetId:imageId -> ImageInfo)
    // 用于支持"选择1个资产有3个镜像 -> 显示3个部署卡片"的需求
    private static final Map<String, ImageInfo> drillAssetImageMap = new ConcurrentHashMap<>();

    /**
     * 服务启动时从数据库恢复持久化数据到内存Map
     * 支持服务重启后数据恢复，解决刷新页面后镜像信息丢失的问题
     */
    @PostConstruct
    public void loadPersistentData() {
        logger.info("========== 开始从数据库恢复演练镜像映射数据 ==========");

        try {
            // 1. 从数据库加载所有映射记录
            List<DrillAssetImageMapping> allMappings = mappingRepository.findAll();
            logger.info("从数据库读取到 {} 条映射记录", allMappings.size());

            if (allMappings.isEmpty()) {
                logger.info("数据库中没有映射记录，跳过恢复");
                return;
            }

            int restoredCount = 0;

            // 2. 遍历每个映射，恢复到内存Map
            for (DrillAssetImageMapping mapping : allMappings) {
                try {
                    Long rangeId = mapping.getRangeId();
                    Long assetId = mapping.getAssetId();
                    String entryKey = mapping.generateKey();

                    // 3. 恢复到内存Map（映射关系表示"可部署的镜像"，不需要验证容器是否存在）
                    ImageInfo imageInfo = new ImageInfo();
                    imageInfo.setRepository(mapping.getImageName());
                    imageInfo.setTag(mapping.getImageTag());
                    imageInfo.setImageId(mapping.getImageId());
                    imageInfo.setCreated(""); // 创建时间不重要，可为空
                    imageInfo.setSize(""); // 大小不重要，可为空

                    drillAssetImageMap.put(entryKey, imageInfo);

                    // 4. 同时恢复到 drillAssetMap
                    Set<Long> assetIds = drillAssetMap.computeIfAbsent(rangeId, k -> new HashSet<>());
                    assetIds.add(assetId);

                    restoredCount++;
                    logger.debug("  ✓ 恢复映射: rangeId={}, assetId={}, image={}",
                        rangeId, assetId, mapping.getImageFullName());

                } catch (Exception e) {
                    logger.error("恢复映射失败: {}", mapping.generateKey(), e);
                }
            }

            logger.info("========== 数据恢复完成 ==========");
            logger.info("成功恢复: {} 条映射", restoredCount);
            logger.info("当前内存中共有 {} 个演练-资产-镜像条目", drillAssetImageMap.size());
            logger.info("当前内存中共有 {} 个演练包含资产", drillAssetMap.size());

        } catch (Exception e) {
            logger.error("从数据库恢复映射数据时发生错误", e);
        }
    }

    /**
     * 获取活跃的演练列表
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveDrills() {
        try {
            List<Map<String, Object>> activeDrills = drillContainerService.getActiveDrills();
            return ResponseEntity.ok(activeDrills);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取演练的所有容器
     */
    @GetMapping("/{rangeId}/containers")
    public ResponseEntity<List<DrillContainer>> getContainersByRangeId(@PathVariable Long rangeId) {
        try {
            List<DrillContainer> containers = drillContainerService.getContainersByRangeId(rangeId);
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建容器配置
     */
    @PostMapping("/{rangeId}/containers")
    public ResponseEntity<Map<String, Object>> createContainer(
            @PathVariable Long rangeId,
            @RequestBody DrillContainer container) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            container.setRangeId(rangeId);
            DrillContainer created = drillContainerService.createContainer(container);
            
            response.put("success", true);
            response.put("message", "容器配置创建成功");
            response.put("container", created);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 部署容器
     */
    @PostMapping("/containers/{containerId}/deploy")
    public ResponseEntity<Map<String, Object>> deployContainer(@PathVariable Long containerId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 异步部署
            drillContainerService.deployContainerAsync(containerId);
            
            response.put("success", true);
            response.put("message", "容器开始部署");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "部署失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 启动容器
     */
    @PostMapping("/containers/{containerId}/start")
    public ResponseEntity<Map<String, Object>> startContainer(@PathVariable Long containerId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            drillContainerService.startContainer(containerId);
            
            response.put("success", true);
            response.put("message", "容器启动成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 停止容器
     */
    @PostMapping("/containers/{containerId}/stop")
    public ResponseEntity<Map<String, Object>> stopContainer(@PathVariable Long containerId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            drillContainerService.stopContainer(containerId);
            
            response.put("success", true);
            response.put("message", "容器停止成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取容器日志
     */
    @GetMapping("/containers/{containerId}/logs")
    public ResponseEntity<String> getContainerLogs(@PathVariable Long containerId) {
        try {
            String logs = drillContainerService.getContainerLogs(containerId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.ok("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取容器状态
     */
    @GetMapping("/containers/{containerId}/status")
    public ResponseEntity<Map<String, Object>> getContainerStatus(@PathVariable Long containerId) {
        // 实现容器状态查询逻辑
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "running");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除容器
     */
    @DeleteMapping("/containers/{containerId}")
    public ResponseEntity<Map<String, Object>> deleteContainer(@PathVariable Long containerId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            drillContainerService.deleteContainer(containerId);
            
            response.put("success", true);
            response.put("message", "容器删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取演练相关的资产（基于关联项目）
     */
    @GetMapping("/{rangeId}/assets")
    public ResponseEntity<List<Map<String, Object>>> getDrillAssets(@PathVariable Long rangeId) {
        try {
            logger.warn("========== 获取演练镜像部署条目（镜像模式）==========");
            logger.warn("请求参数 - rangeId: {}", rangeId);

            // 🆕 从镜像map中获取该演练的所有镜像条目
            List<Map<String, Object>> result = new ArrayList<>();

            for (Map.Entry<String, ImageInfo> entry : drillAssetImageMap.entrySet()) {
                String entryKey = entry.getKey();

                // 检查是否属于当前演练：key格式为 rangeId:assetId:imageId
                if (entryKey.startsWith(rangeId + ":")) {
                    ImageInfo image = entry.getValue();

                    // 解析key获取assetId
                    String[] keyParts = entryKey.split(":");
                    if (keyParts.length >= 2) {
                        Long assetId = Long.parseLong(keyParts[1]);

                        // 获取资产信息
                        Optional<Asset> assetOpt = assetService.getAssetById(assetId);
                        if (assetOpt.isPresent()) {
                            Asset asset = assetOpt.get();

                            // 构建镜像部署条目
                            Map<String, Object> imageEntry = buildDrillImageData(rangeId, asset, image, entryKey);
                            result.add(imageEntry);
                        }
                    }
                }
            }

            logger.warn("演练镜像部署条目数量: {}", result.size());
            if (!result.isEmpty()) {
                logger.warn("镜像条目列表:");
                result.forEach(entry ->
                    logger.warn("  - 镜像: {}, 资产: {}", entry.get("dockerImage"), entry.get("name"))
                );
            } else {
                logger.warn("⚠ 该演练尚未添加任何镜像部署条目");
            }

            logger.warn("✓ 成功返回 {} 个镜像部署条目", result.size());
            logger.warn("========================================");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ 获取演练镜像部署条目失败 - rangeId: {}", rangeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 构建演练资产数据结构
     */
    private Map<String, Object> buildDrillAssetData(Long rangeId, Asset asset) {
        Map<String, Object> drillAsset = new HashMap<>();
        
        // 基础资产信息
        drillAsset.put("id", asset.getId());
        drillAsset.put("assetId", asset.getId());
        drillAsset.put("name", asset.getName());
        drillAsset.put("containerName", generateContainerName(rangeId, asset.getId(), asset.getName()));
        drillAsset.put("category", mapAssetTypeToCategory(asset));
        drillAsset.put("dockerImage", getDockerImageForAsset(asset));
        
        // 获取部署状态（从状态映射中获取，如果不存在则为PENDING）
        String assetKey = rangeId + ":" + asset.getId();
        Map<String, Object> deploymentStatus = assetDeploymentStatusMap.getOrDefault(assetKey, new HashMap<>());
        
        drillAsset.put("deploymentStatus", deploymentStatus.getOrDefault("status", "PENDING"));
        drillAsset.put("isActive", true);
        drillAsset.put("networkConfig", deploymentStatus.get("networkConfig"));
        drillAsset.put("ipAddress", deploymentStatus.get("ipAddress"));
        drillAsset.put("containerId", deploymentStatus.get("containerId"));
        drillAsset.put("deployedAt", deploymentStatus.get("deployedAt"));
        drillAsset.put("accessUrl", deploymentStatus.get("accessUrl"));
        drillAsset.put("hostPort", deploymentStatus.get("hostPort"));
        drillAsset.put("deployError", deploymentStatus.get("deployError"));
        
        // 容器配置信息
        drillAsset.put("containerPorts", asset.getContainerPorts());
        drillAsset.put("environments", asset.getEnvironments());
        drillAsset.put("containerCommand", asset.getContainerCommand());
        drillAsset.put("healthCheckUrl", asset.getHealthCheckUrl());
        drillAsset.put("resourceLimitCpu", asset.getResourceLimitCpu());
        drillAsset.put("resourceLimitMemory", asset.getResourceLimitMemory());
        drillAsset.put("assetType", asset.getAssetType());
        
        // 节点部署信息
        drillAsset.put("preferredHostNodeId", asset.getPreferredHostNodeId());
        drillAsset.put("preferredHostNodeName", asset.getPreferredHostNodeName());
        drillAsset.put("targetHostNodeId", deploymentStatus.get("targetHostNodeId"));
        
        // 创建 asset 子对象
        Map<String, Object> assetInfo = new HashMap<>();
        assetInfo.put("id", asset.getId());
        assetInfo.put("name", asset.getName());
        assetInfo.put("type", asset.getVisibility());
        assetInfo.put("assetType", asset.getAssetType());
        assetInfo.put("dockerImage", getDockerImageForAsset(asset));
        assetInfo.put("isContainerAsset", asset.isContainerAsset());
        assetInfo.put("topologyProjectId", asset.getTopologyProjectId());
        drillAsset.put("asset", assetInfo);

        // 🔍 集成容器发现数据
        if (asset.getPreferredHostNodeId() != null) {
            try {
                List<ContainerInfo> discoveredContainers = containerDiscoveryService.getContainersFromNode(asset.getPreferredHostNodeId());
                drillAsset.put("discoveredContainers", discoveredContainers);
                drillAsset.put("discoveredContainerCount", discoveredContainers.size());
            } catch (Exception e) {
                logger.warn("获取资产 {} 的容器发现数据失败: {}", asset.getId(), e.getMessage());
                drillAsset.put("discoveredContainers", new ArrayList<>());
                drillAsset.put("discoveredContainerCount", 0);
            }
        } else {
            drillAsset.put("discoveredContainers", new ArrayList<>());
            drillAsset.put("discoveredContainerCount", 0);
        }

        // 📦 集成镜像发现数据
        if (asset.getPreferredHostNodeId() != null) {
            try {
                List<ImageInfo> discoveredImages = containerDiscoveryService.getImagesFromNode(asset.getPreferredHostNodeId());
                drillAsset.put("discoveredImages", discoveredImages);
                drillAsset.put("discoveredImageCount", discoveredImages.size());
            } catch (Exception e) {
                logger.warn("获取资产 {} 的镜像发现数据失败: {}", asset.getId(), e.getMessage());
                drillAsset.put("discoveredImages", new ArrayList<>());
                drillAsset.put("discoveredImageCount", 0);
            }
        } else {
            drillAsset.put("discoveredImages", new ArrayList<>());
            drillAsset.put("discoveredImageCount", 0);
        }

        return drillAsset;
    }

    /**
     * 🆕 构建镜像部署条目数据结构（镜像模式）
     * 为每个镜像创建独立的部署条目
     */
    private Map<String, Object> buildDrillImageData(Long rangeId, Asset asset, ImageInfo image, String entryKey) {
        Map<String, Object> imageEntry = new HashMap<>();

        // 使用entryKey作为唯一ID (rangeId:assetId:imageId)
        imageEntry.put("id", entryKey);
        imageEntry.put("assetId", asset.getId());
        imageEntry.put("imageId", image.getImageId());

        // 🆕 镜像信息（重点展示）
        imageEntry.put("dockerImage", image.getFullName());  // nginx:alpine
        imageEntry.put("repository", image.getRepository());
        imageEntry.put("tag", image.getTag());
        imageEntry.put("imageSize", image.getSize());
        imageEntry.put("imageCreated", image.getCreated());

        // 资产基础信息
        imageEntry.put("name", asset.getName() + " - " + image.getRepository());  // 显示：docker容器测试 - nginx
        imageEntry.put("assetType", asset.getAssetType());
        imageEntry.put("category", mapAssetTypeToCategory(asset));

        // 生成包含镜像名的容器名称
        String sanitizedImageName = image.getRepository().replaceAll("[^a-zA-Z0-9-]", "-");
        String containerName = String.format("drill-%d-%s-%s-%d",
            rangeId,
            asset.getName().replaceAll("[^a-zA-Z0-9-]", "-"),
            sanitizedImageName,
            System.currentTimeMillis());
        imageEntry.put("containerName", containerName);

        // 获取部署状态（使用entryKey作为状态key）
        Map<String, Object> deploymentStatus = assetDeploymentStatusMap.getOrDefault(entryKey, new HashMap<>());

        imageEntry.put("deploymentStatus", deploymentStatus.getOrDefault("status", "PENDING"));
        imageEntry.put("isActive", true);
        imageEntry.put("networkConfig", deploymentStatus.get("networkConfig"));
        imageEntry.put("ipAddress", deploymentStatus.get("ipAddress"));
        imageEntry.put("containerId", deploymentStatus.get("containerId"));
        imageEntry.put("deployedAt", deploymentStatus.get("deployedAt"));
        imageEntry.put("accessUrl", deploymentStatus.get("accessUrl"));
        imageEntry.put("hostPort", deploymentStatus.get("hostPort"));
        imageEntry.put("deployError", deploymentStatus.get("deployError"));

        // 容器配置信息（从资产继承）
        imageEntry.put("containerPorts", asset.getContainerPorts());
        imageEntry.put("environments", asset.getEnvironments());
        imageEntry.put("containerCommand", asset.getContainerCommand());
        imageEntry.put("healthCheckUrl", asset.getHealthCheckUrl());
        imageEntry.put("resourceLimitCpu", asset.getResourceLimitCpu());
        imageEntry.put("resourceLimitMemory", asset.getResourceLimitMemory());

        // 节点部署信息（从ImageInfo获取）
        imageEntry.put("hostNodeId", image.getHostNodeId());
        imageEntry.put("hostNodeName", image.getHostNodeName());
        imageEntry.put("hostNodeIp", image.getHostNodeIp());
        imageEntry.put("preferredHostNodeId", asset.getPreferredHostNodeId());
        imageEntry.put("preferredHostNodeName", asset.getPreferredHostNodeName());
        imageEntry.put("targetHostNodeId", deploymentStatus.get("targetHostNodeId"));

        // 创建 asset 子对象
        Map<String, Object> assetInfo = new HashMap<>();
        assetInfo.put("id", asset.getId());
        assetInfo.put("name", asset.getName());
        assetInfo.put("type", asset.getVisibility());
        assetInfo.put("assetType", asset.getAssetType());
        assetInfo.put("dockerImage", image.getFullName());  // 使用实际镜像名
        assetInfo.put("isContainerAsset", asset.isContainerAsset());
        assetInfo.put("topologyProjectId", asset.getTopologyProjectId());
        imageEntry.put("asset", assetInfo);

        return imageEntry;
    }

    /**
     * 废弃：现在资产直接来自关联项目，不再需要手动添加
     * 保留此方法以便兼容旧的前端代码
     */
    /**
     * 🆕 添加资产到演练部署列表
     * 从关联项目的资产中选择特定资产，用于本次演练的容器部署
     */
    @PostMapping("/{rangeId}/assets")
    public ResponseEntity<Map<String, Object>> addAssetToDrill(
            @PathVariable Long rangeId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取请求参数
            Long assetId = ((Number) request.get("assetId")).longValue();
            logger.warn("========== 添加资产到演练（镜像扩展模式）==========");
            logger.warn("演练ID: {}, 资产ID: {}", rangeId, assetId);

            // 1. 验证演练是否存在
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练项目不存在");
                logger.error("❌ 演练不存在: rangeId={}", rangeId);
                return ResponseEntity.badRequest().body(response);
            }

            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();

            // 2. 验证资产是否存在
            Optional<Asset> assetOpt = assetService.getAssetById(assetId);
            if (assetOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "资产不存在");
                logger.error("❌ 资产不存在: assetId={}", assetId);
                return ResponseEntity.badRequest().body(response);
            }

            Asset asset = assetOpt.get();

            // 3. 验证资产是否属于演练关联的项目
            String assetProject = asset.getCompany() + "｜" + asset.getProject();
            if (topologyProjectId != null && !topologyProjectId.equals(assetProject)) {
                response.put("success", false);
                response.put("message", "该资产不属于演练关联的项目");
                logger.error("❌ 资产项目不匹配: 资产={}, 演练项目={}", assetProject, topologyProjectId);
                return ResponseEntity.badRequest().body(response);
            }

            // 4. 检查是否已添加（检查镜像map而非资产map）
            String keyPrefix = rangeId + ":" + assetId + ":";
            List<String> existingImageKeys = drillAssetImageMap.keySet().stream()
                .filter(key -> key.startsWith(keyPrefix))
                .collect(Collectors.toList());

            if (!existingImageKeys.isEmpty()) {
                // 收集冲突的镜像信息
                List<Map<String, String>> conflictImages = new ArrayList<>();
                for (String key : existingImageKeys) {
                    ImageInfo imageInfo = drillAssetImageMap.get(key);
                    if (imageInfo != null) {
                        Map<String, String> imageDetail = new HashMap<>();
                        imageDetail.put("imageId", imageInfo.getImageId());
                        imageDetail.put("imageName", imageInfo.getRepository());  // 使用 repository 而不是 imageName
                        imageDetail.put("tag", imageInfo.getTag());
                        conflictImages.add(imageDetail);
                    }
                }

                response.put("success", false);
                response.put("message", "该资产的镜像已添加到演练中，是否删除并重新添加？");
                response.put("assetId", assetId);
                response.put("conflictImages", conflictImages);
                response.put("conflictCount", conflictImages.size());
                logger.warn("⚠ 资产镜像已存在: assetId={}, 冲突镜像数: {}", assetId, conflictImages.size());

                // 返回 409 Conflict 状态码（表示资源冲突）
                return ResponseEntity.status(409).body(response);
            }

            // 5. 🆕 调用镜像发现API，获取该资产的所有Docker镜像
            logger.warn("📦 开始发现资产 {} 的Docker镜像...", asset.getName());
            ResponseEntity<?> imageDiscoveryResponse = containerController.discoverAssetImages(assetId);

            if (!imageDiscoveryResponse.getStatusCode().is2xxSuccessful()) {
                response.put("success", false);
                response.put("message", "获取资产镜像失败");
                logger.error("❌ 镜像发现失败: assetId={}", assetId);
                return ResponseEntity.badRequest().body(response);
            }

            // 6. 解析镜像列表
            Map<String, Object> imageData = (Map<String, Object>) imageDiscoveryResponse.getBody();
            List<ImageInfo> images = (List<ImageInfo>) imageData.get("images");

            if (images == null || images.isEmpty()) {
                response.put("success", false);
                response.put("message", "该资产没有可用的Docker镜像");
                logger.warn("⚠ 资产无镜像: assetId={}", assetId);
                return ResponseEntity.badRequest().body(response);
            }

            // 7. 为每个有效镜像创建部署条目
            int addedCount = 0;
            for (ImageInfo image : images) {
                if (image != null && image.isValid()) {  // 过滤掉 <none>:<none>
                    String entryKey = rangeId + ":" + assetId + ":" + image.getImageId();

                    // 7.1 添加到内存Map
                    drillAssetImageMap.put(entryKey, image);

                    // 7.2 🆕 同步到数据库（持久化）
                    try {
                        // 检查数据库中是否已存在该映射
                        if (!mappingRepository.existsByRangeIdAndAssetIdAndImageId(rangeId, assetId, image.getImageId())) {
                            DrillAssetImageMapping mapping = new DrillAssetImageMapping(
                                rangeId,
                                assetId,
                                image.getImageId(),
                                image.getRepository(),
                                image.getTag(),
                                image.getFullName()
                            );
                            mappingRepository.save(mapping);
                            logger.debug("    ✓ 数据库保存映射: {}", image.getFullName());
                        }
                    } catch (Exception e) {
                        logger.error("保存镜像映射到数据库失败: {}", image.getFullName(), e);
                        // 不影响主流程，继续处理
                    }

                    addedCount++;
                    logger.warn("  ✓ 添加镜像条目: {}", image.getFullName());
                }
            }

            // 8. 同时添加到资产map（保持兼容性）
            Set<Long> assetIds = drillAssetMap.computeIfAbsent(rangeId, k -> new HashSet<>());
            assetIds.add(assetId);

            logger.warn("✓ 成功添加资产 {} (ID: {})，创建了 {} 个镜像部署条目", asset.getName(), assetId, addedCount);

            response.put("success", true);
            response.put("message", "成功添加 " + addedCount + " 个镜像容器");
            response.put("assetId", assetId);
            response.put("assetName", asset.getName());
            response.put("imageCount", addedCount);
            response.put("images", images.stream()
                .filter(img -> img != null && img.isValid())
                .map(ImageInfo::getFullName)
                .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 添加资产失败", e);
            response.put("success", false);
            response.put("message", "添加资产失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 根据Asset属性映射到DrillAsset的category
     */
    private String mapAssetTypeToCategory(Asset asset) {
        // 优先使用资产的 assetType 字段
        if (asset.getAssetType() != null && !asset.getAssetType().trim().isEmpty()) {
            switch (asset.getAssetType().toLowerCase()) {
                case "container": return asset.isTarget() ? "web_app" : "server";
                case "server": return "server";
                case "service": return "web_app";
                case "network": return "network_device";
                default: return "server";
            }
        }
        
        // 回退到基于可见性的映射
        if (asset.getVisibility() != null) {
            switch (asset.getVisibility().toLowerCase()) {
                case "red": return "vulnerability_tool";
                case "blue": return "server";
                default: return "network_device";
            }
        }
        return asset.isTarget() ? "web_app" : "server";
    }
    
    /**
     * 根据资产类型选择合适的Docker镜像
     */
    private String getDockerImageForAsset(Asset asset) {
        // 优先使用资产的 dockerImage 字段
        if (asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()) {
            return asset.getDockerImage();
        }
        
        // 如果没有设置 dockerImage，使用智能推断（保持向后兼容）
        if (asset.getAssetType() != null && "container".equalsIgnoreCase(asset.getAssetType())) {
            // 对于容器类型资产，根据名称推断
            String assetName = asset.getName().toLowerCase();
            if (assetName.contains("nginx")) return "nginx:alpine";
            if (assetName.contains("mysql")) return "mysql:8.0";
            if (assetName.contains("redis")) return "redis:alpine";
            if (assetName.contains("apache")) return "httpd:alpine";
            if (assetName.contains("postgres")) return "postgres:alpine";
        }
        
        // 根据资产可见性和类型选择镜像（向后兼容旧逻辑）
        if (asset.isTarget()) {
            return "vulnerables/web-dvwa"; // 漏洞应用
        }
        if ("red".equalsIgnoreCase(asset.getVisibility())) {
            return "kalilinux/kali-rolling:latest"; // 渗透测试工具
        }
        
        // 默认镜像
        return "ubuntu:20.04";
    }
    
    /**
     * 生成容器名称
     */
    private String generateContainerName(Long rangeId, Long assetId, String assetName) {
        // 将中文和特殊字符替换为英文字符
        String sanitizedName = assetName.replaceAll("[^a-zA-Z0-9]", "-")
                                       .toLowerCase()
                                       .replaceAll("-+", "-")
                                       .replaceAll("^-|-$", "");
        
        // 如果处理后的名称为空或太短，使用默认名称
        if (sanitizedName.isEmpty() || sanitizedName.length() < 2) {
            sanitizedName = "asset";
        }
        
        return String.format("drill-%d-%d-%s-%d", rangeId, assetId, sanitizedName, System.currentTimeMillis());
    }
    
    /**
     * 分配可用端口
     */
    private int allocatePort() {
        // 端口范围：8000-8999
        Random random = new Random();
        return 8000 + random.nextInt(1000);
    }
    
    /**
     * 获取容器默认端口映射
     */
    private String getDefaultPortMapping(String dockerImage, int hostPort) {
        String containerPort = "80"; // 默认端口
        
        if (dockerImage.contains("nginx")) {
            containerPort = "80";
        } else if (dockerImage.contains("mysql")) {
            containerPort = "3306";
        } else if (dockerImage.contains("redis")) {
            containerPort = "6379";
        } else if (dockerImage.contains("apache") || dockerImage.contains("httpd")) {
            containerPort = "80";
        } else if (dockerImage.contains("postgres")) {
            containerPort = "5432";
        }
        
        return hostPort + ":" + containerPort;
    }
    
    /**
     * 从资产配置中获取端口映射
     */
    private String getPortMappingFromAsset(Asset asset) {
        if (asset.getContainerPorts() == null || asset.getContainerPorts().trim().isEmpty()) {
            return null;
        }
        
        try {
            // 资产的 containerPorts 字段存储的是 JSON 格式的端口映射
            // 格式如：{"80/tcp": "8080", "443/tcp": "8443"}
            String portsJson = asset.getContainerPorts();
            
            // 简单的JSON解析，提取第一个端口映射
            if (portsJson.startsWith("{") && portsJson.contains(":")) {
                // 提取第一个端口映射
                String portPair = portsJson.replace("{", "").replace("}", "")
                    .replace("\"", "").split(",")[0];
                
                if (portPair.contains(":")) {
                    String[] parts = portPair.split(":");
                    if (parts.length == 2) {
                        String containerPortWithProtocol = parts[0].trim();
                        String hostPort = parts[1].trim();
                        
                        // 移除协议部分 (如 /tcp)
                        String containerPort = containerPortWithProtocol.split("/")[0];
                        
                        return hostPort + ":" + containerPort;
                    }
                }
            }
        } catch (Exception e) {
            // Debug statement removed
        }
        
        return null;
    }
    @DeleteMapping("/{rangeId}/assets/{assetId}")
    public ResponseEntity<Map<String, Object>> removeAssetFromDrill(
            @PathVariable Long rangeId,
            @PathVariable Long assetId,
            @RequestParam(required = false) String imageId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (imageId != null && !imageId.isEmpty()) {
                logger.warn("========== 移除单个容器（保留镜像面板）==========");
                logger.warn("演练ID: {}, 资产ID: {}, 镜像ID: {}", rangeId, assetId, imageId);
            } else {
                logger.warn("========== 移除资产的所有容器（保留镜像面板）==========");
                logger.warn("演练ID: {}, 资产ID: {}", rangeId, assetId);
            }

            // 1. 根据imageId参数决定删除哪些容器
            List<String> imageEntryKeys;
            if (imageId != null && !imageId.isEmpty()) {
                // 单容器删除模式：只删除指定imageId的容器
                String singleKey = rangeId + ":" + assetId + ":" + imageId;
                imageEntryKeys = drillAssetImageMap.containsKey(singleKey)
                    ? Collections.singletonList(singleKey)
                    : Collections.emptyList();
                logger.warn("单容器删除模式: 目标镜像 {}", imageId);
            } else {
                // 批量删除模式：删除该资产的所有容器
                String keyPrefix = rangeId + ":" + assetId + ":";
                imageEntryKeys = drillAssetImageMap.keySet().stream()
                    .filter(key -> key.startsWith(keyPrefix))
                    .collect(Collectors.toList());
                logger.warn("批量删除模式: 找到 {} 个容器", imageEntryKeys.size());
            }

            if (imageEntryKeys.isEmpty()) {
                response.put("success", false);
                response.put("message", "未找到要删除的容器");
                return ResponseEntity.badRequest().body(response);
            }

            int deletedCount = 0;
            for (String entryKey : imageEntryKeys) {
                // 2. 停止并删除Docker容器实例
                Map<String, Object> deploymentStatus = assetDeploymentStatusMap.get(entryKey);
                if (deploymentStatus != null) {
                    String containerId = (String) deploymentStatus.get("containerId");
                    if (containerId != null && !containerId.isEmpty()) {
                        try {
                            logger.warn("  停止并删除容器: {}", containerId);
                            dockerService.stopContainer(containerId);
                            dockerService.removeContainer(containerId, true);
                            deletedCount++;

                            // 3. 删除DrillContainer数据库记录（容器实例记录）
                            try {
                                Optional<DrillContainer> containerOpt = drillContainerRepository.findByContainerId(containerId);
                                if (containerOpt.isPresent()) {
                                    drillContainerRepository.delete(containerOpt.get());
                                    logger.debug("    ✓ 删除容器记录: {}", containerId);
                                }
                            } catch (Exception e) {
                                logger.warn("    删除容器记录失败: {}", e.getMessage());
                            }
                        } catch (Exception e) {
                            logger.warn("  容器删除失败: {}, 继续处理其他容器", e.getMessage());
                        }
                    }
                    // 4. 清理部署状态（内存）
                    assetDeploymentStatusMap.remove(entryKey);
                    logger.debug("  ✓ 清理部署状态: {}", entryKey);
                }

                // ❌ 不删除 drillAssetImageMap（镜像面板保留）
                // ❌ 不删除 DrillAssetImageMapping（数据库映射保留，刷新后恢复）
            }

            logger.warn("✓ 共删除了 {} 个Docker容器实例", deletedCount);

            // 5. 清理旧格式的部署状态（兼容性）
            String assetKey = rangeId + ":" + assetId;
            Map<String, Object> oldDeploymentStatus = assetDeploymentStatusMap.get(assetKey);
            if (oldDeploymentStatus != null) {
                String containerId = (String) oldDeploymentStatus.get("containerId");
                if (containerId != null && !containerId.isEmpty()) {
                    try {
                        logger.warn("停止并删除旧格式容器: {}", containerId);
                        dockerService.stopContainer(containerId);
                        dockerService.removeContainer(containerId, true);
                    } catch (Exception e) {
                        // 忽略错误
                    }
                }
                assetDeploymentStatusMap.remove(assetKey);
            }

            // ✅ drillAssetImageMap 和 drillAssetMap 保持不变（镜像面板保留）

            response.put("success", true);
            response.put("message", "容器已移除，镜像面板保留，可重新部署");
            response.put("deletedContainerCount", deletedCount);
            logger.warn("✓ 容器移除完成，镜像面板保留");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ 移除资产失败", e);
            response.put("success", false);
            response.put("message", "移除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 完全删除资产及其所有镜像映射（用于替换资产场景）
     * 与普通删除的区别：
     * - 普通删除：只删除容器实例，保留镜像配置和映射，可重新部署
     * - 完全删除：删除容器 + 内存映射 + 数据库记录，为重新选择资产做准备
     *
     * @param rangeId 演练ID
     * @param assetId 资产ID
     * @return 删除结果
     */
    @DeleteMapping("/{rangeId}/assets/{assetId}/complete")
    public ResponseEntity<Map<String, Object>> completelyRemoveAsset(
            @PathVariable Long rangeId,
            @PathVariable Long assetId) {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.warn("========== 完全删除资产（容器+映射+配置）==========");
            logger.warn("演练ID: {}, 资产ID: {}", rangeId, assetId);

            // 1. 查找该资产的所有镜像条目
            String keyPrefix = rangeId + ":" + assetId + ":";
            List<String> imageEntryKeys = drillAssetImageMap.keySet().stream()
                .filter(key -> key.startsWith(keyPrefix))
                .collect(Collectors.toList());

            logger.warn("找到 {} 个镜像条目需要删除", imageEntryKeys.size());

            // 2. 停止并删除所有Docker容器实例
            int deletedContainerCount = 0;
            List<String> failedContainers = new ArrayList<>();

            for (String entryKey : imageEntryKeys) {
                Map<String, Object> deploymentStatus = assetDeploymentStatusMap.get(entryKey);
                if (deploymentStatus != null) {
                    String containerId = (String) deploymentStatus.get("containerId");
                    if (containerId != null && !containerId.isEmpty()) {
                        try {
                            logger.warn("  停止并删除容器: {}", containerId);
                            dockerService.stopContainer(containerId);
                            dockerService.removeContainer(containerId, true);
                            deletedContainerCount++;

                            // 删除DrillContainer数据库记录
                            try {
                                Optional<DrillContainer> containerOpt = drillContainerRepository.findByContainerId(containerId);
                                if (containerOpt.isPresent()) {
                                    drillContainerRepository.delete(containerOpt.get());
                                    logger.debug("    ✓ 删除容器记录: {}", containerId);
                                }
                            } catch (Exception e) {
                                logger.warn("    删除容器记录失败: {}", e.getMessage());
                            }
                        } catch (Exception e) {
                            logger.error("  ❌ 容器删除失败: {}, 错误: {}", containerId, e.getMessage());
                            failedContainers.add(containerId + ": " + e.getMessage());
                            // 完全删除模式下，容器删除失败应抛出异常
                            throw new RuntimeException("容器删除失败: " + containerId + ", " + e.getMessage(), e);
                        }
                    }
                    // 清理部署状态（内存）
                    assetDeploymentStatusMap.remove(entryKey);
                }
            }

            // 3. 删除旧格式的部署状态（兼容性）
            String assetKey = rangeId + ":" + assetId;
            Map<String, Object> oldDeploymentStatus = assetDeploymentStatusMap.get(assetKey);
            if (oldDeploymentStatus != null) {
                String containerId = (String) oldDeploymentStatus.get("containerId");
                if (containerId != null && !containerId.isEmpty()) {
                    try {
                        logger.warn("停止并删除旧格式容器: {}", containerId);
                        dockerService.stopContainer(containerId);
                        dockerService.removeContainer(containerId, true);
                        deletedContainerCount++;
                    } catch (Exception e) {
                        logger.error("❌ 旧格式容器删除失败: {}", e.getMessage());
                        throw new RuntimeException("旧格式容器删除失败: " + e.getMessage(), e);
                    }
                }
                assetDeploymentStatusMap.remove(assetKey);
            }

            // 4. ✅ 删除内存中的镜像映射（drillAssetImageMap）
            int removedMemoryMappings = 0;
            for (String entryKey : imageEntryKeys) {
                drillAssetImageMap.remove(entryKey);
                removedMemoryMappings++;
            }
            logger.warn("✓ 删除内存映射: {} 个", removedMemoryMappings);

            // 5. ✅ 删除数据库中的镜像映射（DrillAssetImageMapping）
            try {
                mappingRepository.deleteByRangeIdAndAssetId(rangeId, assetId);
                logger.warn("✓ 删除数据库映射记录");
            } catch (Exception e) {
                logger.error("❌ 删除数据库映射失败: {}", e.getMessage());
                throw new RuntimeException("删除数据库映射失败: " + e.getMessage(), e);
            }

            // 6. 删除演练-资产关联关系
            Set<Long> assetIds = drillAssetMap.get(rangeId);
            if (assetIds != null) {
                assetIds.remove(assetId);
                logger.warn("✓ 删除演练-资产关联关系");
            }

            logger.warn("========== 完全删除完成 ==========");
            logger.warn("✓ 删除容器: {} 个", deletedContainerCount);
            logger.warn("✓ 删除内存映射: {} 个", removedMemoryMappings);
            logger.warn("✓ 删除数据库映射: 完成");

            response.put("success", true);
            response.put("message", "资产及所有配置已完全删除");
            response.put("deletedContainerCount", deletedContainerCount);
            response.put("deletedMappingCount", removedMemoryMappings);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ 完全删除资产失败", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            // 返回 500 Internal Server Error 而不是 400，因为这是服务器端错误
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 部署单个资产/镜像
     * 支持镜像模式：请求体包含 imageId 时部署指定镜像
     * 向后兼容：请求体为空或无 imageId 时使用旧的资产部署逻辑
     */
    @PostMapping("/{rangeId}/assets/{assetId}/deploy")
    public ResponseEntity<Map<String, Object>> deployAsset(
            @PathVariable Long rangeId,
            @PathVariable Long assetId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 检查是否为镜像模式部署
            String imageId = null;
            if (requestBody != null && requestBody.containsKey("imageId")) {
                imageId = (String) requestBody.get("imageId");
            }

            if (imageId != null && !imageId.isEmpty()) {
                // 🆕 镜像模式部署
                logger.warn("========== 镜像模式部署 ==========");
                logger.warn("演练ID: {}, 资产ID: {}, 镜像ID: {}", rangeId, assetId, imageId);
                return deployImageEntry(rangeId, assetId, imageId);
            }

            // 旧的资产模式部署（向后兼容）
            logger.warn("========== 资产模式部署（兼容模式）==========");
            logger.warn("演练ID: {}, 资产ID: {}", rangeId, assetId);

            // 获取资产信息
            Asset asset = assetService.getAssetById(assetId).orElse(null);
            if (asset == null) {
                response.put("success", false);
                response.put("message", "资产不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证资产是否属于演练关联的项目
            CyberRange cyberRange = cyberRangeRepository.findById(rangeId).orElse(null);
            if (cyberRange == null) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!asset.getTopologyProjectId().equals(cyberRange.getTopologyProjectId())) {
                response.put("success", false);
                response.put("message", "资产不属于演练关联的项目");
                return ResponseEntity.badRequest().body(response);
            }
            
            String assetKey = rangeId + ":" + assetId;
            
            // 🔧 设置部署中状态
            Map<String, Object> deploymentStatus = new HashMap<>();
            deploymentStatus.put("status", "DEPLOYING");
            deploymentStatus.put("deployStartTime", LocalDateTime.now().toString());
            assetDeploymentStatusMap.put(assetKey, deploymentStatus);
            
            // 🔧 异步真实部署
            new Thread(() -> {
                try {
                    // 使用资产的完整配置
                    String dockerImage = getDockerImageForAsset(asset);
                    String assetName = asset.getName();
                    String containerName = generateContainerName(rangeId, assetId, assetName);
                    
                    // 智能节点选择 - 考虑资产的首选节点配置
                    Long targetNodeId = selectOptimalDeploymentNode(asset);
                    
                    // 使用资产配置的端口映射
                    String portMapping = getPortMappingFromAsset(asset);
                    int hostPort = 0;
                    if (portMapping == null) {
                        hostPort = allocatePort();
                        portMapping = getDefaultPortMapping(dockerImage, hostPort);
                    } else {
                        hostPort = extractHostPortFromMapping(portMapping);
                    }
                    
                    // 使用资产配置的环境变量
                    String environmentsStr = asset.getEnvironments();
                    List<String> environments = parseEnvironmentVariables(environmentsStr);
                    
                    // Debug statement removed
                    // Debug statement removed
                    // Debug statement removed
                    // Debug statement removed
                    // Debug statement removed
                    // Debug statement removed
                    // Debug statement removed
                    
                    // 🔧 调用真实Docker服务，使用完整的资产配置
                    String deployResult = dockerService.runContainer(
                        dockerImage, 
                        containerName, 
                        portMapping, 
                        environments
                    );
                    
                    // Debug statement removed
                    
                    // 更新部署状态
                    Map<String, Object> updatedStatus = assetDeploymentStatusMap.get(assetKey);
                    if (updatedStatus == null) updatedStatus = new HashMap<>();
                    
                    if (deployResult.contains("容器创建成功") || deployResult.contains("容器ID")) {
                        // 部署成功
                        String containerId = extractContainerIdFromResult(deployResult);
                        
                        updatedStatus.put("status", "DEPLOYED");
                        updatedStatus.put("containerId", containerId);
                        updatedStatus.put("containerName", containerName);
                        updatedStatus.put("containerFullName", containerName);
                        updatedStatus.put("ipAddress", "172.16.190.130:" + hostPort);
                        updatedStatus.put("hostPort", hostPort);
                        updatedStatus.put("deployedAt", LocalDateTime.now().toString());
                        updatedStatus.put("accessUrl", "http://172.16.190.130:" + hostPort);
                        updatedStatus.put("targetHostNodeId", targetNodeId);
                        updatedStatus.put("deployError", null);
                        
                        // Debug statement removed
                        // Debug statement removed
                        // Debug statement removed
                    } else {
                        // 部署失败
                        updatedStatus.put("status", "FAILED");
                        updatedStatus.put("deployError", deployResult);
                        // Debug statement removed
                    }
                    
                    assetDeploymentStatusMap.put(assetKey, updatedStatus);
                    
                } catch (Exception e) {
                    Map<String, Object> failedStatus = assetDeploymentStatusMap.get(assetKey);
                    if (failedStatus == null) failedStatus = new HashMap<>();
                    failedStatus.put("status", "FAILED");
                    failedStatus.put("deployError", e.getMessage());
                    assetDeploymentStatusMap.put(assetKey, failedStatus);

                    logger.error("异步部署资产失败", e);
                }
            }).start();
            
            response.put("success", true);
            response.put("message", "开始部署资产");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("部署资产失败", e);
            response.put("success", false);
            response.put("message", "部署失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🆕 部署镜像条目（镜像模式）
     * 基于imageId部署单个Docker镜像
     */
    private ResponseEntity<Map<String, Object>> deployImageEntry(Long rangeId, Long assetId, String imageId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 构建entryKey
            String entryKey = rangeId + ":" + assetId + ":" + imageId;
            logger.warn("entryKey: {}", entryKey);

            // 2. 从镜像map获取镜像信息
            ImageInfo image = drillAssetImageMap.get(entryKey);
            if (image == null) {
                logger.error("镜像条目不存在: {}", entryKey);
                response.put("success", false);
                response.put("message", "镜像条目不存在");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 获取资产信息
            Asset asset = assetService.getAssetById(assetId).orElse(null);
            if (asset == null) {
                response.put("success", false);
                response.put("message", "资产不存在");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. 验证资产是否属于演练关联的项目
            CyberRange cyberRange = cyberRangeRepository.findById(rangeId).orElse(null);
            if (cyberRange == null) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }

            if (!asset.getTopologyProjectId().equals(cyberRange.getTopologyProjectId())) {
                response.put("success", false);
                response.put("message", "资产不属于演练关联的项目");
                return ResponseEntity.badRequest().body(response);
            }

            logger.warn("准备部署镜像: {}", image.getFullName());
            logger.warn("资产: {}", asset.getName());
            logger.warn("资产Docker配置: {}", asset.getDockerApiUrl());

            // 5. 设置部署中状态
            Map<String, Object> deploymentStatus = new HashMap<>();
            deploymentStatus.put("status", "DEPLOYING");
            deploymentStatus.put("deployStartTime", LocalDateTime.now().toString());
            assetDeploymentStatusMap.put(entryKey, deploymentStatus);

            // 6. 异步部署容器
            new Thread(() -> {
                try {
                    // 使用镜像的完整名称（如 nginx:alpine）
                    String dockerImage = image.getFullName();

                    // 生成容器名称（包含镜像repository）
                    String sanitizedImageName = image.getRepository().replaceAll("[^a-zA-Z0-9-]", "-");
                    String sanitizedAssetName = asset.getName().replaceAll("[^a-zA-Z0-9-]", "-");
                    String containerName = String.format("drill-%d-%s-%s-%d",
                        rangeId,
                        sanitizedAssetName,
                        sanitizedImageName,
                        System.currentTimeMillis());

                    logger.warn("容器名称: {}", containerName);

                    // 智能节点选择（使用资产的首选节点配置）
                    Long targetNodeId = selectOptimalDeploymentNode(asset);

                    // 端口映射配置
                    String portMapping = getPortMappingFromAsset(asset);
                    int hostPort = 0;
                    if (portMapping == null) {
                        hostPort = allocatePort();
                        portMapping = getDefaultPortMapping(dockerImage, hostPort);
                    } else {
                        hostPort = extractHostPortFromMapping(portMapping);
                    }

                    logger.warn("端口映射: {}", portMapping);

                    // 环境变量配置
                    String environmentsStr = asset.getEnvironments();
                    List<String> environments = parseEnvironmentVariables(environmentsStr);

                    logger.warn("环境变量数量: {}", environments.size());

                    // 🆕 调用Docker服务部署容器 - 使用新的V2方法获取可靠的结果
                    DockerService.ContainerCreationResult result = dockerService.runContainerV2(
                        dockerImage,
                        containerName,
                        portMapping,
                        environments
                    );

                    logger.warn("部署结果: {} - {}", result.isSuccess() ? "成功" : "失败", result.getMessage());

                    // 更新部署状态
                    Map<String, Object> updatedStatus = assetDeploymentStatusMap.get(entryKey);
                    if (updatedStatus == null) updatedStatus = new HashMap<>();

                    if (result.isSuccess()) {
                        // ✓ 部署成功 - 容器真实存在且运行
                        String containerId = result.getContainerId();

                        updatedStatus.put("status", "DEPLOYED");
                        updatedStatus.put("containerId", containerId);
                        updatedStatus.put("containerName", containerName);
                        updatedStatus.put("containerFullName", containerName);
                        updatedStatus.put("ipAddress", "172.16.190.130:" + hostPort);
                        updatedStatus.put("hostPort", hostPort);
                        updatedStatus.put("deployedAt", LocalDateTime.now().toString());
                        updatedStatus.put("accessUrl", "http://172.16.190.130:" + hostPort);
                        updatedStatus.put("targetHostNodeId", targetNodeId);
                        updatedStatus.put("deployError", null);

                        logger.warn("✓ 镜像部署成功: {}", image.getFullName());
                        logger.warn("  容器ID: {}", containerId);
                        logger.warn("  容器名称: {}", containerName);
                        logger.warn("  访问地址: http://172.16.190.130:{}", hostPort);

                        // 💾 保存部署记录到数据库（持久化）
                        try {
                            DrillContainer drillContainer = new DrillContainer();
                            drillContainer.setRangeId(rangeId);
                            drillContainer.setAssetId(assetId);
                            drillContainer.setName(containerName);
                            drillContainer.setContainerFullName(containerName);
                            drillContainer.setImageName(image.getFullName());
                            drillContainer.setContainerId(containerId);
                            drillContainer.setStatus("running");
                            drillContainer.setHostNodeId(targetNodeId);
                            drillContainer.setHostNodeIp("172.16.190.130");
                            drillContainer.setIp("172.16.190.130:" + hostPort);
                            drillContainer.setPort(hostPort);
                            drillContainer.setDeployTime(LocalDateTime.now());
                            drillContainer.setStartTime(LocalDateTime.now());
                            drillContainer.setAutoStart(true);
                            drillContainer.setRestartPolicy("no");

                            DrillContainer savedRecord = drillContainerRepository.save(drillContainer);
                            logger.warn("  ✓ 部署记录已保存到数据库，记录ID: {}", savedRecord.getId());
                        } catch (Exception dbEx) {
                            logger.error("  ⚠ 部署记录保存到数据库失败: {}", dbEx.getMessage());
                            // 不影响部署成功状态，只是记录未持久化
                        }
                    } else {
                        // ✗ 部署失败 - 容器不存在或未运行
                        String errorDetails = result.getErrorDetails();
                        updatedStatus.put("status", "FAILED");
                        updatedStatus.put("deployError", errorDetails != null ? errorDetails : result.getMessage());
                        logger.error("✗ 镜像部署失败: {}", image.getFullName());
                        logger.error("  错误信息: {}", errorDetails != null ? errorDetails : result.getMessage());
                    }

                    assetDeploymentStatusMap.put(entryKey, updatedStatus);

                } catch (Exception e) {
                    Map<String, Object> failedStatus = assetDeploymentStatusMap.get(entryKey);
                    if (failedStatus == null) failedStatus = new HashMap<>();
                    failedStatus.put("status", "FAILED");
                    failedStatus.put("deployError", e.getMessage());
                    assetDeploymentStatusMap.put(entryKey, failedStatus);

                    logger.error("异步部署镜像失败", e);
                }
            }).start();

            response.put("success", true);
            response.put("message", "开始部署镜像: " + image.getFullName());
            response.put("imageId", imageId);
            response.put("imageName", image.getFullName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("部署镜像条目失败", e);
            response.put("success", false);
            response.put("message", "部署失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 智能选择最优部署节点
     */
    private Long selectOptimalDeploymentNode(Asset asset) {
        // 优先使用资产配置的首选节点
        if (asset.getPreferredHostNodeId() != null) {
            // Debug statement removed
            return asset.getPreferredHostNodeId();
        }
        
        // 根据部署策略选择
        String strategy = asset.getDeploymentStrategy();
        if ("fixed".equals(strategy) && asset.getPreferredHostNodeId() != null) {
            return asset.getPreferredHostNodeId();
        } else if ("load_balanced".equals(strategy)) {
            // TODO: 实现负载均衡节点选择逻辑
            // Debug statement removed
        }
        
        // 默认返回null，让Docker服务自动选择
        return null;
    }
    
    /**
     * 从Docker结果中提取容器ID
     */
    private String extractContainerIdFromResult(String result) {
        // Docker运行成功时通常返回 "容器创建成功，容器ID: xxxx" 或直接返回容器ID
        if (result.contains("容器ID:")) {
            String containerId = result.substring(result.indexOf("容器ID:") + 4).trim();
            // 移除可能的前缀符号
            if (containerId.startsWith(": ")) {
                containerId = containerId.substring(2);
            }
            return containerId;
        }
        // 如果直接返回容器ID（通常是最后一行）
        String[] lines = result.split("\n");
        String lastLine = lines[lines.length - 1].trim();
        if (lastLine.matches("^[a-f0-9]{12,64}$")) {
            return lastLine;
        }
        // 如果都不匹配，返回时间戳作为临时ID
        return "container_" + System.currentTimeMillis();
    }
    
    /**
     * 从端口映射字符串中提取主机端口
     */
    private int extractHostPortFromMapping(String portMapping) {
        if (portMapping == null || portMapping.isEmpty()) {
            return 0;
        }
        
        // 解析类似 "8080:80" 的端口映射格式
        if (portMapping.contains(":")) {
            try {
                String hostPortStr = portMapping.split(":")[0];
                return Integer.parseInt(hostPortStr);
            } catch (NumberFormatException e) {
                // Debug statement removed
                return 0;
            }
        }
        
        return 0;
    }
    
    /**
     * 解析环境变量字符串为List
     */
    private List<String> parseEnvironmentVariables(String environmentsStr) {
        List<String> environments = new ArrayList<>();
        
        if (environmentsStr == null || environmentsStr.trim().isEmpty()) {
            return environments;
        }
        
        // 支持多种分隔符：换行符、分号、逗号
        String[] envLines = environmentsStr.split("[\\n;,]");
        
        for (String envLine : envLines) {
            String trimmed = envLine.trim();
            if (!trimmed.isEmpty() && trimmed.contains("=")) {
                environments.add(trimmed);
            }
        }
        
        return environments;
    }
    
    /**
     * 停止资产（只停止容器，不删除）
     * 支持镜像模式（传递imageId参数）和传统模式（不传递imageId）
     */
    @PostMapping("/{rangeId}/assets/{assetId}/stop")
    public ResponseEntity<Map<String, Object>> stopAsset(
            @PathVariable Long rangeId,
            @PathVariable Long assetId,
            @RequestParam(required = false) String imageId) {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.warn("========== 停止资产 ==========");
            logger.warn("演练ID: {}, 资产ID: {}, 镜像ID: {}", rangeId, assetId, imageId);

            // ✅ 修复：支持镜像模式和传统模式
            String assetKey = null;
            Map<String, Object> deploymentStatus = null;

            if (imageId != null && !imageId.isEmpty()) {
                // 镜像模式：使用完整key查找（rangeId:assetId:imageId）
                assetKey = rangeId + ":" + assetId + ":" + imageId;
                deploymentStatus = assetDeploymentStatusMap.get(assetKey);
                logger.warn("镜像模式 - 查找key: {}", assetKey);
            } else {
                // 传统模式：尝试前缀匹配（兼容旧版本）
                String keyPrefix = rangeId + ":" + assetId + ":";
                for (Map.Entry<String, Map<String, Object>> entry : assetDeploymentStatusMap.entrySet()) {
                    if (entry.getKey().startsWith(keyPrefix)) {
                        assetKey = entry.getKey();
                        deploymentStatus = entry.getValue();
                        logger.warn("传统模式 - 前缀匹配到key: {}", assetKey);
                        break;
                    }
                }

                // 如果前缀匹配失败，尝试精确匹配（兼容非镜像部署）
                if (deploymentStatus == null) {
                    assetKey = rangeId + ":" + assetId;
                    deploymentStatus = assetDeploymentStatusMap.get(assetKey);
                    if (deploymentStatus != null) {
                        logger.warn("传统模式 - 精确匹配到key: {}", assetKey);
                    }
                }
            }

            if (deploymentStatus == null) {
                response.put("success", false);
                response.put("message", "资产无部署状态，可能尚未部署或已被清理");
                logger.error("❌ 未找到部署状态 - rangeId: {}, assetId: {}, imageId: {}",
                    rangeId, assetId, imageId);
                logger.error("当前存储的keys: {}", assetDeploymentStatusMap.keySet());
                return ResponseEntity.badRequest().body(response);
            }

            String containerId = (String) deploymentStatus.get("containerId");

            if (containerId == null || containerId.isEmpty()) {
                response.put("success", false);
                response.put("message", "容器ID不存在");
                return ResponseEntity.badRequest().body(response);
            }

            // ✅ 修复：调用 stopContainer 而不是 removeContainer
            logger.warn("正在停止容器: {}", containerId);
            String stopResult = dockerService.stopContainer(containerId);
            logger.warn("停止容器结果: {}", stopResult);

            // ✅ 修复：检查停止是否成功
            if (stopResult == null || (!stopResult.contains("已停止") && !stopResult.contains("stopped"))) {
                // 检查是否是容器不存在或已经停止
                if (stopResult != null && (stopResult.contains("No such container") || stopResult.contains("is not running"))) {
                    logger.warn("容器不存在或已停止，继续更新状态");
                } else {
                    response.put("success", false);
                    response.put("message", "停止容器失败: " + stopResult);
                    logger.error("❌ 停止容器失败: {}", stopResult);
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // ✅ 修复：更新数据库容器状态为 stopped（不删除记录）
            try {
                Optional<DrillContainer> containerOpt = drillContainerRepository.findByContainerId(containerId);
                if (containerOpt.isPresent()) {
                    DrillContainer dbContainer = containerOpt.get();
                    dbContainer.setStatus("stopped");
                    drillContainerRepository.save(dbContainer);
                    logger.warn("✓ 更新数据库容器状态为 stopped: {}", dbContainer.getId());
                }
            } catch (Exception e) {
                logger.warn("更新数据库容器状态失败: {}", e.getMessage());
                // 不影响主流程
            }

            // ✅ 修复：更新内存状态为 STOPPED，保留 containerId
            deploymentStatus.put("status", "STOPPED");
            // containerId 保留，因为容器只是停止，没有删除
            // ipAddress, hostPort, accessUrl 也保留，方便查看
            deploymentStatus.put("stoppedAt", LocalDateTime.now().toString());

            assetDeploymentStatusMap.put(assetKey, deploymentStatus);

            logger.warn("✓ 资产停止成功");

            response.put("success", true);
            response.put("message", "容器已停止");
            response.put("containerId", containerId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ 停止资产失败", e);
            response.put("success", false);
            response.put("message", "停止失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取资产日志
     */
    @GetMapping("/{rangeId}/assets/{assetId}/logs")
    public ResponseEntity<String> getAssetLogs(
            @PathVariable Long rangeId,
            @PathVariable Long assetId) {
        try {
            // 🆕 遍历查找所有以 "rangeId:assetId:" 开头的key（镜像部署）
            String prefix = rangeId + ":" + assetId + ":";
            Map<String, Object> deploymentStatus = null;
            String matchedKey = null;

            for (Map.Entry<String, Map<String, Object>> entry : assetDeploymentStatusMap.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    deploymentStatus = entry.getValue();
                    matchedKey = entry.getKey();
                    logger.warn("找到镜像部署记录: {}", matchedKey);
                    break; // 使用第一个匹配的镜像部署记录
                }
            }

            if (deploymentStatus != null) {
                String containerId = (String) deploymentStatus.get("containerId");
                String status = (String) deploymentStatus.getOrDefault("status", "PENDING");

                if (containerId != null && !containerId.isEmpty() && "DEPLOYED".equals(status)) {
                    // 🔧 获取真实容器日志
                    String logs = dockerService.getContainerLogs(containerId, 50);

                    if (logs != null && !logs.trim().isEmpty()) {
                        return ResponseEntity.ok(logs);
                    } else {
                        return ResponseEntity.ok("容器日志为空或无法获取");
                    }
                } else {
                    // 🔧 如果容器未部署，显示部署错误信息
                    String deployError = (String) deploymentStatus.get("deployError");
                    if (deployError != null) {
                        return ResponseEntity.ok("=== 部署错误日志 ===\n" + deployError);
                    } else {
                        String deployStartTime = (String) deploymentStatus.get("deployStartTime");
                        String stoppedAt = (String) deploymentStatus.get("stoppedAt");

                        StringBuilder statusInfo = new StringBuilder();
                        statusInfo.append("=== 资产部署状态 ===\n");
                        statusInfo.append("当前状态: ").append(status).append("\n");
                        if (deployStartTime != null) {
                            statusInfo.append("开始部署时间: ").append(deployStartTime).append("\n");
                        }
                        if (stoppedAt != null) {
                            statusInfo.append("停止时间: ").append(stoppedAt).append("\n");
                        }
                        statusInfo.append("\n容器尚未成功部署或已停止");

                        return ResponseEntity.ok(statusInfo.toString());
                    }
                }
            }

            // 🆕 Fallback: 从数据库查询
            List<DrillContainer> dbRecords = drillContainerRepository.findByRangeIdAndAssetId(rangeId, assetId);
            if (!dbRecords.isEmpty()) {
                DrillContainer container = dbRecords.get(0);
                String containerId = container.getContainerId();
                if (containerId != null && !containerId.isEmpty()) {
                    logger.warn("从数据库找到部署记录，容器ID: {}", containerId);
                    String logs = dockerService.getContainerLogs(containerId, 50);
                    if (logs != null && !logs.trim().isEmpty()) {
                        return ResponseEntity.ok(logs);
                    } else {
                        return ResponseEntity.ok("容器日志为空或无法获取");
                    }
                }
                return ResponseEntity.ok("数据库中找到部署记录，但容器ID为空");
            }

            return ResponseEntity.ok("资产无部署记录");
        } catch (Exception e) {
            logger.error("获取日志失败", e);
            return ResponseEntity.ok("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据演练获取关联项目的可用容器资产
     */
    @GetMapping("/{rangeId}/available-assets")
    public ResponseEntity<List<Map<String, Object>>> getAvailableAssetsForDrill(@PathVariable Long rangeId) {
        try {
            // 获取演练信息
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                // Debug statement removed
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }
            
            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();
            
            if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
                // Debug statement removed
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            // 获取项目内的容器资产
            List<Asset> projectAssets = assetService.getAssetsByProjectId(topologyProjectId)
                .stream()
                .filter(asset -> "container".equalsIgnoreCase(asset.getAssetType()) || asset.isContainerAsset())
                .collect(Collectors.toList());
            
            List<Map<String, Object>> result = projectAssets.stream()
                .map(asset -> {
                    Map<String, Object> assetInfo = new HashMap<>();
                    assetInfo.put("id", asset.getId());
                    assetInfo.put("name", asset.getName());
                    assetInfo.put("ip", asset.getIp());
                    assetInfo.put("company", asset.getCompany());
                    assetInfo.put("owner", asset.getOwner());
                    assetInfo.put("visibility", asset.getVisibility());
                    assetInfo.put("isTarget", asset.isTarget());
                    assetInfo.put("enabled", asset.isEnabled());
                    assetInfo.put("notes", asset.getNotes());
                    assetInfo.put("project", asset.getProject());
                    assetInfo.put("topologyProjectId", asset.getTopologyProjectId());
                    
                    // 添加容器相关信息
                    assetInfo.put("assetType", asset.getAssetType());
                    assetInfo.put("dockerImage", asset.getDockerImage());
                    assetInfo.put("containerPorts", asset.getContainerPorts());
                    assetInfo.put("environments", asset.getEnvironments());
                    assetInfo.put("containerCommand", asset.getContainerCommand());
                    assetInfo.put("healthCheckUrl", asset.getHealthCheckUrl());
                    assetInfo.put("resourceLimitCpu", asset.getResourceLimitCpu());
                    assetInfo.put("resourceLimitMemory", asset.getResourceLimitMemory());
                    assetInfo.put("isContainerAsset", asset.isContainerAsset());
                    
                    // 节点部署配置
                    assetInfo.put("preferredHostNodeId", asset.getPreferredHostNodeId());
                    assetInfo.put("preferredHostNodeName", asset.getPreferredHostNodeName());
                    assetInfo.put("deploymentStrategy", asset.getDeploymentStrategy());
                    
                    return assetInfo;
                })
                .collect(Collectors.toList());
            
            // Debug statement removed
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取演练可用资产失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 兼容旧接口：根据类型获取可用资产
     */
    @Deprecated
    @GetMapping("/available-assets/{type}")
    public ResponseEntity<List<Map<String, Object>>> getAvailableAssetsByType(@PathVariable String type) {
        try {
            List<Asset> assets;
            
            // 根据类型获取资产
            if ("container".equalsIgnoreCase(type)) {
                // 获取所有容器类型的资产
                assets = assetService.getAllAssets().stream()
                    .filter(asset -> "container".equalsIgnoreCase(asset.getAssetType()) || 
                                   asset.isContainerAsset())
                    .collect(Collectors.toList());
            } else {
                // 使用现有方法获取其他类型
                assets = assetService.getAssetsByCompany(type);
            }
            
            List<Map<String, Object>> result = assets.stream()
                .map(asset -> {
                    Map<String, Object> assetInfo = new HashMap<>();
                    assetInfo.put("id", asset.getId());
                    assetInfo.put("name", asset.getName());
                    assetInfo.put("ip", asset.getIp());
                    assetInfo.put("company", asset.getCompany());
                    assetInfo.put("owner", asset.getOwner());
                    assetInfo.put("visibility", asset.getVisibility());
                    assetInfo.put("isTarget", asset.isTarget());
                    assetInfo.put("enabled", asset.isEnabled());
                    assetInfo.put("notes", asset.getNotes());
                    assetInfo.put("project", asset.getProject());
                    
                    // 添加容器相关信息
                    assetInfo.put("assetType", asset.getAssetType());
                    assetInfo.put("dockerImage", asset.getDockerImage());
                    assetInfo.put("containerPorts", asset.getContainerPorts());
                    assetInfo.put("environments", asset.getEnvironments());
                    assetInfo.put("containerCommand", asset.getContainerCommand());
                    assetInfo.put("healthCheckUrl", asset.getHealthCheckUrl());
                    assetInfo.put("resourceLimitCpu", asset.getResourceLimitCpu());
                    assetInfo.put("resourceLimitMemory", asset.getResourceLimitMemory());
                    assetInfo.put("isContainerAsset", asset.isContainerAsset());
                    
                    return assetInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 验证 Asset 是否为容器类型
     */
    private boolean isContainerAsset(Asset asset) {
        return "container".equalsIgnoreCase(asset.getAssetType()) || 
               (asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty());
    }
    @GetMapping("/{rangeId}/deployment-status")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus(@PathVariable Long rangeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<DrillAsset> drillAssets = drillAssetService.getAllActiveAssets();
            
            Map<String, Object> status = new HashMap<>();
            status.put("totalAssets", drillAssets.size());
            status.put("activeAssets", drillAssets.stream().filter(da -> da.getIsActive()).count());
            status.put("targetAssets", drillAssets.stream().filter(da -> da.getIsTarget()).count());
            status.put("inactiveAssets", drillAssets.stream().filter(da -> !da.getIsActive()).count());
            
            response.put("success", true);
            response.put("status", status);
            response.put("assets", drillAssets);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量启动容器
     */
    @PostMapping("/{rangeId}/containers/batch-start")
    public ResponseEntity<Map<String, Object>> batchStartContainers(
            @PathVariable Long rangeId, 
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> assetIds = (List<Long>) request.get("assetIds");
            
            if (assetIds == null || assetIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "未指定要启动的资产");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            // 获取指定范围的演练资产
            List<DrillAsset> drillAssets = drillAssetService.getAllActiveAssets();
            
            if (drillAssets != null && !drillAssets.isEmpty()) {
                for (Long assetId : assetIds) {
                    Optional<DrillAsset> drillAssetOpt = drillAssets.stream()
                        .filter(da -> assetId.equals(da.getId()))
                        .findFirst();
                    
                    if (drillAssetOpt.isPresent()) {
                        DrillAsset drillAsset = drillAssetOpt.get();
                        
                        // 查找对应的容器
                        List<DrillContainer> containers = drillContainerService.getContainersByAsset(assetId);
                        if (!containers.isEmpty()) {
                            DrillContainer container = containers.get(0); // 取第一个容器
                            String containerId = container.getContainerId();
                        
                            Map<String, Object> result = new HashMap<>();
                            result.put("assetId", assetId);
                            result.put("assetName", drillAsset.getName());
                            
                            if (containerId != null && !containerId.isEmpty()) {
                                try {
                                    String startResult = dockerService.startContainer(containerId);
                                    if (startResult.contains("已启动") || startResult.contains("启动成功")) {
                                        container.setStatus("RUNNING");
                                        drillContainerRepository.save(container);
                                        result.put("success", true);
                                        result.put("message", "启动成功");
                                    } else {
                                        result.put("success", false);
                                        result.put("message", "启动失败: " + startResult);
                                    }
                                } catch (Exception e) {
                                    result.put("success", false);
                                    result.put("message", "启动失败: " + e.getMessage());
                                }
                            } else {
                                result.put("success", false);
                                result.put("message", "容器未部署");
                            }
                            
                            results.add(result);
                        }
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("assetId", assetId);
                        result.put("success", false);
                        result.put("message", "资产不存在");
                        results.add(result);
                    }
                }
            }
            
            long successCount = results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum();
            
            response.put("success", true);
            response.put("message", String.format("批量启动完成，成功 %d 个，失败 %d 个", successCount, results.size() - successCount));
            response.put("results", results);
            response.put("totalCount", results.size());
            response.put("successCount", successCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量启动失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量停止容器
     */
    @PostMapping("/{rangeId}/containers/batch-stop")
    public ResponseEntity<Map<String, Object>> batchStopContainers(
            @PathVariable Long rangeId, 
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> assetIds = (List<Long>) request.get("assetIds");
            
            if (assetIds == null || assetIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "未指定要停止的资产");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            // 获取指定范围的演练资产
            List<DrillAsset> drillAssets = drillAssetService.getAllActiveAssets();
            
            if (drillAssets != null && !drillAssets.isEmpty()) {
                for (Long assetId : assetIds) {
                    Optional<DrillAsset> drillAssetOpt = drillAssets.stream()
                        .filter(da -> assetId.equals(da.getId()))
                        .findFirst();
                    
                    if (drillAssetOpt.isPresent()) {
                        DrillAsset drillAsset = drillAssetOpt.get();
                        
                        // 查找对应的容器
                        List<DrillContainer> containers = drillContainerService.getContainersByAsset(assetId);
                        if (!containers.isEmpty()) {
                            DrillContainer container = containers.get(0); // 取第一个容器
                            String containerId = container.getContainerId();
                        
                            Map<String, Object> result = new HashMap<>();
                            result.put("assetId", assetId);
                            result.put("assetName", drillAsset.getName());
                            
                            if (containerId != null && !containerId.isEmpty()) {
                                try {
                                    String stopResult = dockerService.stopContainer(containerId);
                                    if (stopResult.contains("已停止") || stopResult.contains("停止成功")) {
                                        container.setStatus("STOPPED");
                                        drillContainerRepository.save(container);
                                        result.put("success", true);
                                        result.put("message", "停止成功");
                                    } else {
                                        result.put("success", false);
                                        result.put("message", "停止失败: " + stopResult);
                                    }
                                } catch (Exception e) {
                                    result.put("success", false);
                                    result.put("message", "停止失败: " + e.getMessage());
                                }
                            } else {
                                result.put("success", false);
                                result.put("message", "容器未部署");
                            }
                            
                            results.add(result);
                        }
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("assetId", assetId);
                        result.put("success", false);
                        result.put("message", "资产不存在");
                        results.add(result);
                    }
                }
            }
            
            long successCount = results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum();
            
            response.put("success", true);
            response.put("message", String.format("批量停止完成，成功 %d 个，失败 %d 个", successCount, results.size() - successCount));
            response.put("results", results);
            response.put("totalCount", results.size());
            response.put("successCount", successCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量停止失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量重启容器
     */
    @PostMapping("/{rangeId}/containers/batch-restart")
    public ResponseEntity<Map<String, Object>> batchRestartContainers(
            @PathVariable Long rangeId, 
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> assetIds = (List<Long>) request.get("assetIds");
            
            if (assetIds == null || assetIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "未指定要重启的资产");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            // 获取指定范围的演练资产
            List<DrillAsset> drillAssets = drillAssetService.getAllActiveAssets();
            
            if (drillAssets != null && !drillAssets.isEmpty()) {
                for (Long assetId : assetIds) {
                    Optional<DrillAsset> drillAssetOpt = drillAssets.stream()
                        .filter(da -> assetId.equals(da.getId()))
                        .findFirst();
                    
                    if (drillAssetOpt.isPresent()) {
                        DrillAsset drillAsset = drillAssetOpt.get();
                        
                        // 查找对应的容器
                        List<DrillContainer> containers = drillContainerService.getContainersByAsset(assetId);
                        if (!containers.isEmpty()) {
                            DrillContainer container = containers.get(0); // 取第一个容器
                            String containerId = container.getContainerId();
                        
                            Map<String, Object> result = new HashMap<>();
                            result.put("assetId", assetId);
                            result.put("assetName", drillAsset.getName());
                            
                            if (containerId != null && !containerId.isEmpty()) {
                                try {
                                    String restartResult = dockerService.restartContainer(containerId);
                                    if (restartResult.contains("重启成功") || restartResult.contains("已重启")) {
                                        container.setStatus("RUNNING");
                                        drillContainerRepository.save(container);
                                        result.put("success", true);
                                        result.put("message", "重启成功");
                                    } else {
                                        result.put("success", false);
                                        result.put("message", "重启失败: " + restartResult);
                                    }
                                } catch (Exception e) {
                                    result.put("success", false);
                                    result.put("message", "重启失败: " + e.getMessage());
                                }
                            } else {
                                result.put("success", false);
                                result.put("message", "容器未部署");
                            }
                            
                            results.add(result);
                        }
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("assetId", assetId);
                        result.put("success", false);
                        result.put("message", "资产不存在");
                        results.add(result);
                    }
                }
            }
            
            long successCount = results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum();
            
            response.put("success", true);
            response.put("message", String.format("批量重启完成，成功 %d 个，失败 %d 个", successCount, results.size() - successCount));
            response.put("results", results);
            response.put("totalCount", results.size());
            response.put("successCount", successCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量重启失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 刷新容器状态
     */
    @PostMapping("/{rangeId}/containers/refresh-status")
    public ResponseEntity<Map<String, Object>> refreshContainerStatus(@PathVariable Long rangeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取指定范围的演练资产
            List<DrillAsset> drillAssets = drillAssetService.getAllActiveAssets();
            if (drillAssets == null || drillAssets.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练资产不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Map<String, Object>> statusResults = new ArrayList<>();
            
            for (DrillAsset drillAsset : drillAssets) {
                // 查找对应的容器
                List<DrillContainer> containers = drillContainerService.getContainersByAsset(drillAsset.getId());
                
                Map<String, Object> statusInfo = new HashMap<>();
                statusInfo.put("assetId", drillAsset.getId());
                statusInfo.put("assetName", drillAsset.getName());
                
                if (!containers.isEmpty()) {
                    DrillContainer container = containers.get(0); // 取第一个容器
                    String containerId = container.getContainerId();
                    statusInfo.put("containerId", containerId);
                    
                    if (containerId != null && !containerId.isEmpty()) {
                        try {
                            // 获取容器最新状态
                            ContainerInfo containerInfo = dockerService.getContainerInfo(containerId);
                            if (containerInfo != null) {
                                statusInfo.put("status", containerInfo.getStatus());
                                statusInfo.put("simpleStatus", containerInfo.getSimpleStatus());
                                statusInfo.put("healthStatus", containerInfo.getHealthStatus());
                                statusInfo.put("isHealthy", containerInfo.isHealthy());
                                statusInfo.put("cpuUsage", containerInfo.getCpuUsage());
                                statusInfo.put("memoryUsage", containerInfo.getMemoryUsage());
                                
                                // 更新容器状态
                                if (containerInfo.getSimpleStatus().equals("运行中")) {
                                    container.setStatus("RUNNING");
                                } else {
                                    container.setStatus("STOPPED");
                                }
                                drillContainerRepository.save(container);
                                
                                statusInfo.put("success", true);
                            } else {
                                statusInfo.put("success", false);
                                statusInfo.put("message", "容器不存在或无法访问");
                                container.setStatus("FAILED");
                                drillContainerRepository.save(container);
                            }
                        } catch (Exception e) {
                            statusInfo.put("success", false);
                            statusInfo.put("message", "获取状态失败: " + e.getMessage());
                        }
                    } else {
                        statusInfo.put("success", false);
                        statusInfo.put("message", "容器未部署");
                    }
                } else {
                    statusInfo.put("success", false);
                    statusInfo.put("message", "容器未部署");
                }
                
                statusResults.add(statusInfo);
            }
            
            long healthyCount = statusResults.stream()
                .mapToLong(s -> (Boolean) s.getOrDefault("isHealthy", false) ? 1 : 0)
                .sum();
            
            response.put("success", true);
            response.put("message", "状态刷新完成");
            response.put("statusResults", statusResults);
            response.put("totalAssets", statusResults.size());
            response.put("healthyCount", healthyCount);
            response.put("refreshTime", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "刷新状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ========== 拓扑感知传输优化相关API ==========
    
    /**
     * 智能部署全部资产 - 基于项目拓扑感知
     */
    @PostMapping("/{rangeId}/smart-deploy-all")
    public ResponseEntity<Map<String, Object>> smartDeployAllAssets(@PathVariable Long rangeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取演练信息
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();
            
            if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "演练未关联拓扑项目");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 使用项目感知传输服务进行智能部署
            Map<String, Object> deploymentResult = projectAwareTransferService.deployContainerGroupForProject(
                topologyProjectId, rangeId, "production"
            );
            
            response.put("success", deploymentResult.get("success"));
            response.put("message", "智能部署完成");
            response.put("deploymentResult", deploymentResult);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("智能部署全部资产失败", e);
            response.put("success", false);
            response.put("message", "智能部署失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取项目信息 - 用于DrillContainerStatus组件显示
     */
    @GetMapping("/{rangeId}/project-info")
    public ResponseEntity<Map<String, Object>> getProjectInfo(@PathVariable Long rangeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取演练信息
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();
            
            if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
                response.put("projectId", "未关联");
                response.put("totalAssets", 0);
                response.put("deploymentNodes", 0);
                response.put("balanceScore", 100);
                return ResponseEntity.ok(response);
            }
            
            // 获取项目传输统计信息
            Map<String, Object> projectStats = projectAwareTransferService.getProjectTransferStats(topologyProjectId);
            
            return ResponseEntity.ok(projectStats);
        } catch (Exception e) {
            // Debug statement removed
            response.put("success", false);
            response.put("message", "获取项目信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取负载均衡分析 - 分析当前容器分布
     */
    @GetMapping("/{rangeId}/balance-analysis")
    public ResponseEntity<Map<String, Object>> getBalanceAnalysis(@PathVariable Long rangeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取演练信息
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();
            
            if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "演练未关联拓扑项目");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 分析项目分布均衡度
            Map<String, Object> balanceAnalysis = assetService.analyzeProjectDistributionBalance(topologyProjectId);
            
            return ResponseEntity.ok(balanceAnalysis);
        } catch (Exception e) {
            // Debug statement removed
            response.put("success", false);
            response.put("message", "负载分析失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 执行负载均衡 - 重新分配容器到最优节点
     */
    @PostMapping("/{rangeId}/execute-load-balance")
    public ResponseEntity<Map<String, Object>> executeLoadBalance(@PathVariable Long rangeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取演练信息
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();
            
            if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "演练未关联拓扑项目");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 执行项目级容器负载重平衡
            Map<String, Object> rebalanceResult = projectAwareTransferService.rebalanceProjectContainers(
                topologyProjectId, "production"
            );
            
            return ResponseEntity.ok(rebalanceResult);
        } catch (Exception e) {
            // Debug statement removed
            response.put("success", false);
            response.put("message", "负载均衡执行失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 容器迁移API - 将容器迁移到指定节点
     */
    @PostMapping("/{rangeId}/containers/{containerId}/migrate")
    public ResponseEntity<Map<String, Object>> migrateContainer(
            @PathVariable Long rangeId,
            @PathVariable Long containerId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long targetNodeId = ((Number) request.get("targetNodeId")).longValue();
            Boolean forceRebalance = (Boolean) request.getOrDefault("forceRebalance", false);
            
            // 执行容器迁移
            Map<String, Object> migrationResult = projectAwareTransferService.migrateContainer(
                containerId, targetNodeId, forceRebalance
            );
            
            return ResponseEntity.ok(migrationResult);
        } catch (Exception e) {
            // Debug statement removed
            response.put("success", false);
            response.put("message", "容器迁移失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 推荐最优部署节点
     */
    @GetMapping("/{rangeId}/recommend-node")
    public ResponseEntity<Map<String, Object>> recommendOptimalNode(
            @PathVariable Long rangeId,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String assetType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取演练信息
            Optional<CyberRange> rangeOpt = cyberRangeRepository.findById(rangeId);
            if (rangeOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "演练不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            CyberRange cyberRange = rangeOpt.get();
            String topologyProjectId = cyberRange.getTopologyProjectId();
            
            if (topologyProjectId == null || topologyProjectId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "演练未关联拓扑项目");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 推荐最优节点
            Map<String, Object> recommendation = assetService.recommendOptimalNode(
                topologyProjectId, environment, assetType
            );
            
            return ResponseEntity.ok(recommendation);
        } catch (Exception e) {
            // Debug statement removed
            response.put("success", false);
            response.put("message", "节点推荐失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}