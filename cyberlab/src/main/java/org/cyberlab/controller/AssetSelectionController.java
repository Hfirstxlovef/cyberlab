package org.cyberlab.controller;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.HostNode;
import org.cyberlab.service.AssetService;
import org.cyberlab.service.ContainerDiscoveryService;
import org.cyberlab.service.HostNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产选择控制器
 * 用于项目中选择资产时的三栏布局：主机→容器→已选
 */
@RestController
@RequestMapping("/api/asset-selection")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class AssetSelectionController {

    @Autowired
    private HostNodeService hostNodeService;
    
    @Autowired
    private ContainerDiscoveryService containerDiscoveryService;
    
    @Autowired
    private AssetService assetService;

    /**
     * 获取主机列表（左栏）
     */
    @GetMapping("/hosts")
    public ResponseEntity<?> getHostList(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String project) {
        try {
            List<HostNode> allHosts = hostNodeService.getActiveNodes();
            
            // 如果指定了企业或项目，过滤主机
            List<HostNode> filteredHosts = allHosts;
            if (company != null || project != null) {
                // 获取符合条件的资产的主机节点ID
                List<Asset> assets = assetService.getAllAssets();
                Set<Long> validHostNodeIds = assets.stream()
                    .filter(asset -> (company == null || company.equals(asset.getCompany())) &&
                                   (project == null || project.equals(asset.getProject())))
                    .map(Asset::getPreferredHostNodeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                
                filteredHosts = allHosts.stream()
                    .filter(host -> validHostNodeIds.contains(host.getId()))
                    .collect(Collectors.toList());
            }
            
            // 为每个主机获取容器概览信息
            List<Map<String, Object>> hostList = new ArrayList<>();
            for (HostNode host : filteredHosts) {
                Map<String, Object> hostInfo = new HashMap<>();
                hostInfo.put("id", host.getId());
                hostInfo.put("name", host.getDisplayName());
                hostInfo.put("ip", host.getHostIp());
                hostInfo.put("status", host.getStatus());
                hostInfo.put("isLocal", host.isLocal());
                hostInfo.put("dockerPort", host.getDockerPort());
                
                // 获取容器数量统计
                try {
                    List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(host.getId());
                    long runningCount = containers.stream().filter(c -> c.getSimpleStatus().equals("运行中")).count();
                    long stoppedCount = containers.stream().filter(c -> c.getSimpleStatus().equals("已停止")).count();
                    
                    hostInfo.put("containerStats", Map.of(
                        "total", containers.size(),
                        "running", runningCount,
                        "stopped", stoppedCount
                    ));
                } catch (Exception e) {
                    hostInfo.put("containerStats", Map.of(
                        "total", 0,
                        "running", 0,
                        "stopped", 0,
                        "error", "无法获取容器信息"
                    ));
                }
                
                hostList.add(hostInfo);
            }
            
            return ResponseEntity.ok(Map.of(
                "hosts", hostList,
                "totalCount", hostList.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取主机列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定主机的容器列表（中栏）
     */
    @GetMapping("/containers/{hostId}")
    public ResponseEntity<?> getContainerList(
            @PathVariable Long hostId,
            @RequestParam(required = false) Boolean onlyRunning,
            @RequestParam(required = false) String imageFilter,
            @RequestParam(required = false) String searchKeyword) {
        try {
            Optional<HostNode> hostOpt = hostNodeService.getNodeById(hostId);
            if (hostOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "主机节点不存在"));
            }
            
            HostNode host = hostOpt.get();
            List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(hostId);
            
            // 应用过滤器
            List<ContainerInfo> filteredContainers = containers.stream()
                .filter(container -> {
                    // 仅显示运行中的容器
                    if (onlyRunning != null && onlyRunning && !container.getSimpleStatus().equals("运行中")) {
                        return false;
                    }
                    
                    // 镜像筛选
                    if (imageFilter != null && !imageFilter.trim().isEmpty() && 
                        (container.getImage() == null || !container.getImage().toLowerCase().contains(imageFilter.toLowerCase()))) {
                        return false;
                    }
                    
                    // 关键词搜索（容器名、镜像、状态）
                    if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                        String keyword = searchKeyword.toLowerCase();
                        return (container.getName() != null && container.getName().toLowerCase().contains(keyword)) ||
                               (container.getImage() != null && container.getImage().toLowerCase().contains(keyword)) ||
                               (container.getStatus() != null && container.getStatus().toLowerCase().contains(keyword));
                    }
                    
                    return true;
                })
                .sorted(Comparator.comparing(ContainerInfo::getStatusPriority)
                                  .thenComparing(container -> container.getName() != null ? container.getName() : ""))
                .collect(Collectors.toList());
            
            // 构建返回数据
            List<Map<String, Object>> containerList = filteredContainers.stream()
                .map(container -> {
                    Map<String, Object> containerInfo = new HashMap<>();
                    containerInfo.put("containerId", container.getContainerId());
                    containerInfo.put("name", container.getName());
                    containerInfo.put("image", container.getImage());
                    containerInfo.put("status", container.getStatus());
                    containerInfo.put("simpleStatus", container.getSimpleStatus());
                    containerInfo.put("healthStatus", container.getHealthStatus());
                    containerInfo.put("isHealthy", container.isHealthy());
                    containerInfo.put("cpuUsage", container.getCpuUsage());
                    containerInfo.put("memoryUsage", container.getMemoryUsage());
                    containerInfo.put("networkInfo", container.getNetworkInfo());
                    containerInfo.put("portMappings", container.getPortMappings());
                    containerInfo.put("createdAt", container.getCreatedAt());
                    
                    // 检查是否已有对应的资产
                    List<Asset> relatedAssets = assetService.getAllAssets().stream()
                        .filter(asset -> isAssetRelatedToContainer(asset, container))
                        .collect(Collectors.toList());
                    
                    containerInfo.put("hasRelatedAsset", !relatedAssets.isEmpty());
                    if (!relatedAssets.isEmpty()) {
                        containerInfo.put("relatedAssets", relatedAssets.stream()
                            .map(asset -> Map.of(
                                "id", asset.getId(),
                                "name", asset.getName(),
                                "company", asset.getCompany(),
                                "project", asset.getProject()
                            )).collect(Collectors.toList()));
                    }
                    
                    return containerInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "hostId", hostId,
                "hostName", host.getDisplayName(),
                "hostIp", host.getHostIp(),
                "containers", containerList,
                "totalCount", containerList.size(),
                "originalCount", containers.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取容器列表失败: " + e.getMessage()));
        }
    }

    /**
     * 刷新指定主机的容器信息
     */
    @PostMapping("/refresh-host/{hostId}")
    public ResponseEntity<?> refreshHostContainers(@PathVariable Long hostId) {
        try {
            Optional<HostNode> hostOpt = hostNodeService.getNodeById(hostId);
            if (hostOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "主机节点不存在"));
            }
            
            HostNode host = hostOpt.get();
            
            // 强制刷新容器信息
            List<ContainerInfo> containers = containerDiscoveryService.forceRefreshNodeContainers(hostId);
            
            // 统计信息
            long runningCount = containers.stream().filter(c -> c.getSimpleStatus().equals("运行中")).count();
            long stoppedCount = containers.stream().filter(c -> c.getSimpleStatus().equals("已停止")).count();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "主机容器信息刷新成功",
                "hostId", hostId,
                "hostName", host.getDisplayName(),
                "containerStats", Map.of(
                    "total", containers.size(),
                    "running", runningCount,
                    "stopped", stoppedCount
                ),
                "refreshTime", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "刷新主机容器信息失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取镜像筛选选项
     */
    @GetMapping("/image-filters")
    public ResponseEntity<?> getImageFilters() {
        try {
            // 从所有主机获取容器信息，提取镜像列表
            List<HostNode> hosts = hostNodeService.getActiveNodes();
            Set<String> imageSet = new HashSet<>();
            
            for (HostNode host : hosts) {
                try {
                    List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(host.getId());
                    containers.stream()
                        .map(ContainerInfo::getImage)
                        .filter(Objects::nonNull)
                        .forEach(image -> {
                            // 提取镜像名称（去除标签）
                            String imageName = image.split(":")[0];
                            if (imageName.contains("/")) {
                                imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
                            }
                            imageSet.add(imageName);
                        });
                } catch (Exception e) {
                    // 忽略无法访问的主机
                }
            }
            
            List<String> sortedImages = imageSet.stream()
                .sorted()
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "images", sortedImages,
                "count", sortedImages.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取镜像筛选选项失败: " + e.getMessage()));
        }
    }

    /**
     * 批量选择容器（用于项目中批量添加资产）
     */
    @PostMapping("/select-containers")
    public ResponseEntity<?> selectContainers(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selectedContainers = (List<Map<String, Object>>) request.get("containers");
            String targetCompany = (String) request.get("company");
            String targetProject = (String) request.get("project");
            
            if (selectedContainers == null || selectedContainers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "未选择任何容器"));
            }
            
            if (selectedContainers.size() > 50) {
                return ResponseEntity.badRequest().body(Map.of("error", "一次最多只能选择50个容器"));
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (Map<String, Object> containerData : selectedContainers) {
                try {
                    String containerId = (String) containerData.get("containerId");
                    Long hostId = Long.valueOf(containerData.get("hostId").toString());
                    String role = (String) containerData.getOrDefault("role", "blue");
                    String mode = (String) containerData.getOrDefault("mode", "复用");
                    String notes = (String) containerData.get("notes");
                    
                    result.add(Map.of(
                        "containerId", containerId,
                        "hostId", hostId,
                        "role", role,
                        "mode", mode,
                        "notes", notes != null ? notes : "",
                        "selected", true
                    ));
                } catch (Exception e) {
                    errors.add("处理容器选择失败: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "selectedContainers", result,
                "selectedCount", result.size(),
                "errors", errors,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "批量选择容器失败: " + e.getMessage()));
        }
    }

    /**
     * 判断资产是否与容器相关
     */
    private boolean isAssetRelatedToContainer(Asset asset, ContainerInfo container) {
        if (asset == null || container == null) {
            return false;
        }
        
        // 1. 通过Docker镜像匹配
        if (asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()) {
            String assetImage = asset.getDockerImage().trim();
            String containerImage = container.getImage();
            
            if (containerImage != null) {
                String assetImageBase = assetImage.split(":")[0];
                String containerImageBase = containerImage.split(":")[0];
                if (assetImageBase.equals(containerImageBase)) {
                    return true;
                }
            }
        }
        
        // 2. 通过容器名称匹配
        if (asset.getName() != null && container.getName() != null) {
            String assetNameLower = asset.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            String containerNameLower = container.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            if (containerNameLower.contains(assetNameLower) || assetNameLower.contains(containerNameLower)) {
                return true;
            }
        }
        
        // 3. 通过主机节点匹配
        if (asset.getPreferredHostNodeId() != null && 
            asset.getPreferredHostNodeId().equals(container.getHostNodeId())) {
            return true;
        }
        
        return false;
    }
}