package org.cyberlab.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.cyberlab.dto.PageResponse;
import org.cyberlab.dto.TopologyData;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.HostNode;
import org.cyberlab.entity.SystemLog;
import org.cyberlab.repository.AssetRepository;
import org.cyberlab.repository.SystemLogRepository;
import org.cyberlab.service.AssetService;
import org.cyberlab.service.TopologyService;
import org.cyberlab.service.ContainerDiscoveryService;
import org.cyberlab.service.HostNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class AssetController {

    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetService assetService;

    @Autowired
    private TopologyService topologyService;
    
    @Autowired
    private ContainerDiscoveryService containerDiscoveryService;

    @Autowired
    private HostNodeService hostNodeService;

    @Autowired
    private SystemLogRepository systemLogRepository;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAssetById(@PathVariable Long id) {
        return assetRepository.findById(id).map(asset -> {
            Map<String, Object> response = new HashMap<>();
            response.put("id", asset.getId());
            response.put("name", asset.getName());
            response.put("ip", asset.getIp());
            response.put("company", asset.getCompany());
            response.put("owner", asset.getOwner());
            response.put("visibility", asset.getVisibility());
            response.put("isTarget", asset.isTarget());
            response.put("enabled", asset.isEnabled());
            response.put("notes", asset.getNotes());
            response.put("project", asset.getProject());
            response.put("topologyProjectId", asset.getTopologyProjectId());
            
            // 集成拓扑数据
            if (asset.getTopologyProjectId() != null && !asset.getTopologyProjectId().trim().isEmpty()) {
                try {
                    TopologyData topologyData = topologyService.loadByProjectId(asset.getTopologyProjectId());
                    response.put("topologyData", topologyData);
                } catch (Exception e) {
                    response.put("topologyData", null);
                    response.put("topologyError", "拓扑数据加载失败: " + e.getMessage());
                }
            } else {
                response.put("topologyData", null);
            }
            
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Asset>> getAllAssets() {
        return ResponseEntity.ok(assetRepository.findAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<Asset>> getAllAssetsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        return ResponseEntity.ok(PageResponse.of(assetService.getAllAssets(pageable)));
    }

    @GetMapping("/red")
    public ResponseEntity<List<Asset>> getAssetsForRedTeam() {
        return ResponseEntity.ok(assetRepository.findByIsTargetTrue());
    }

    @GetMapping("/blue")
    public ResponseEntity<List<Asset>> getAssetsForBlueTeam() {
        return ResponseEntity.ok(assetRepository.findAll());
    }

    @GetMapping("/visible/{visibility}")
    public ResponseEntity<List<Asset>> getAssetsByVisibility(@PathVariable String visibility) {
        return ResponseEntity.ok(assetRepository.findByVisibility(visibility));
    }

    @PostMapping
    public ResponseEntity<?> createAsset(@RequestBody Asset asset) {
        String currentUser = getCurrentUsername();
        if (asset.getOwner() == null || asset.getOwner().isBlank()) {
            asset.setOwner(currentUser);
        }

        Asset savedAsset = assetRepository.save(asset);

        // 记录资产创建日志
        try {
            SystemLog log = new SystemLog();
            log.setUsername(currentUser != null ? currentUser : "anonymous");
            log.setOperation("ASSET_CREATED");
            log.setDescription(String.format(
                "创建了资产: %s | 企业: %s | 项目: %s | 类型: %s | IP: %s",
                savedAsset.getName(),
                savedAsset.getCompany(),
                savedAsset.getProject(),
                savedAsset.getAssetType(),
                savedAsset.getIp()
            ));
            log.setTimestamp(LocalDateTime.now());
            systemLogRepository.save(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
        }

        return ResponseEntity.ok(savedAsset);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAsset(@PathVariable Long id, @RequestBody Asset asset) {
        String currentUser = getCurrentUsername();

        return assetRepository.findById(id).map(existing -> {
            if (!"admin".equalsIgnoreCase(currentUser) &&
                    !currentUser.equalsIgnoreCase(existing.getOwner())) {
                return ResponseEntity.status(403).body("❌ 无权限修改该资产");
            }

            existing.setName(asset.getName());
            existing.setIp(asset.getIp());
            existing.setCompany(asset.getCompany());
            existing.setOwner(asset.getOwner());
            existing.setVisibility(asset.getVisibility());
            existing.setTarget(asset.isTarget());
            existing.setEnabled(asset.isEnabled());
            existing.setNotes(asset.getNotes());
            existing.setTopologyProjectId(asset.getTopologyProjectId());
            
            // 添加缺失的字段更新 - 修复资产类型等字段无法保存的问题
            existing.setProject(asset.getProject());
            existing.setAssetType(asset.getAssetType());
            existing.setDockerImage(asset.getDockerImage());
            existing.setContainerPorts(asset.getContainerPorts());
            existing.setVolumes(asset.getVolumes());
            existing.setEnvironments(asset.getEnvironments());
            existing.setContainerCommand(asset.getContainerCommand());
            existing.setHealthCheckUrl(asset.getHealthCheckUrl());
            existing.setResourceLimitCpu(asset.getResourceLimitCpu());
            existing.setResourceLimitMemory(asset.getResourceLimitMemory());
            existing.setPreferredHostNodeId(asset.getPreferredHostNodeId());
            existing.setPreferredHostNodeName(asset.getPreferredHostNodeName());
            existing.setDeploymentStrategy(asset.getDeploymentStrategy());

            // 容器探测配置字段
            existing.setAssetPlatform(asset.getAssetPlatform());
            existing.setDockerPort(asset.getDockerPort());
            existing.setDockerApiEnabled(asset.getDockerApiEnabled());
            existing.setK8sApiServer(asset.getK8sApiServer());
            existing.setK8sToken(asset.getK8sToken());

            Asset updatedAsset = assetRepository.save(existing);

            // 记录资产更新日志
            try {
                SystemLog log = new SystemLog();
                log.setUsername(currentUser != null ? currentUser : "anonymous");
                log.setOperation("ASSET_UPDATED");
                log.setDescription(String.format(
                    "更新了资产: %s (ID: %d) | 企业: %s | 项目: %s",
                    updatedAsset.getName(),
                    updatedAsset.getId(),
                    updatedAsset.getCompany(),
                    updatedAsset.getProject()
                ));
                log.setTimestamp(LocalDateTime.now());
                systemLogRepository.save(log);
            } catch (Exception e) {
                // 日志记录失败不影响主流程
            }

            return ResponseEntity.ok(updatedAsset);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        String currentUser = getCurrentUsername();

        return assetRepository.findById(id).map(asset -> {
            if (!"admin".equalsIgnoreCase(currentUser) &&
                    !currentUser.equalsIgnoreCase(asset.getOwner())) {
                return ResponseEntity.status(403).body("❌ 无权限删除该资产");
            }

            // 保存资产信息用于日志
            String assetName = asset.getName();
            String assetCompany = asset.getCompany();
            String assetProject = asset.getProject();

            assetRepository.deleteById(id);

            // 记录资产删除日志
            try {
                SystemLog log = new SystemLog();
                log.setUsername(currentUser != null ? currentUser : "anonymous");
                log.setOperation("ASSET_DELETED");
                log.setDescription(String.format(
                    "删除了资产: %s (ID: %d) | 企业: %s | 项目: %s",
                    assetName,
                    id,
                    assetCompany,
                    assetProject
                ));
                log.setTimestamp(LocalDateTime.now());
                systemLogRepository.save(log);
            } catch (Exception e) {
                // 日志记录失败不影响主流程
            }

            return ResponseEntity.ok("✅ 资产已删除");
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getAssetProjects() {
        // Debug statement removed
        
        List<Asset> allAssets = assetRepository.findAll();
        // Debug statement removed
        
        // 使用Map来去重
        Map<String, String> uniqueProjects = new HashMap<>();
        
        // 收集所有有效的项目及其topologyProjectId
        for (Asset asset : allAssets) {
            String projectName = asset.getProject();
            String topologyId = asset.getTopologyProjectId();
            
            // Debug statement removed
            
            // 更宽松的条件检查
            if (projectName != null && !projectName.trim().isEmpty()) {
                if (topologyId != null && !topologyId.trim().isEmpty()) {
                    uniqueProjects.put(projectName.trim(), topologyId.trim());
                    // Debug statement removed
                } else {
                    // 即使没有拓扑ID也添加，使用项目名作为默认ID
                    uniqueProjects.put(projectName.trim(), projectName.trim());
                    // Debug statement removed
                }
            }
        }
        
        // Debug statement removed
        
        // 构建结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : uniqueProjects.entrySet()) {
            Map<String, Object> project = new HashMap<>();
            project.put("name", entry.getKey());
            project.put("topologyProjectId", entry.getValue());
            result.add(project);
            // Debug statement removed
        }
        
        // Debug statement removed
        // Debug statement removed
        // Debug statement removed
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    public void exportAssets(HttpServletResponse response) {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String filename = URLEncoder.encode("assets.csv", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            // 添加 BOM 以避免 Excel 中文乱码
            response.getOutputStream().write(0xEF);
            response.getOutputStream().write(0xBB);
            response.getOutputStream().write(0xBF);

            PrintWriter writer = new PrintWriter(response.getOutputStream());
            writer.println("ID,名称,IP,企业,负责人,可见性,是否靶场,启用,备注");

            for (Asset asset : assetRepository.findAll()) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        asset.getId(),
                        asset.getName(),
                        asset.getIp(),
                        asset.getCompany(),
                        asset.getOwner(),
                        asset.getVisibility(),
                        asset.isTarget() ? "是" : "否",
                        asset.isEnabled() ? "启用" : "禁用",
                        asset.getNotes().replace(",", "，")); // 避免 CSV 逗号破坏格式
            }

            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("❌ 导出资产失败", e);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Asset>> searchAssets(@RequestParam String keyword) {
        return ResponseEntity.ok(assetService.searchAssetsByKeyword(keyword));
    }

    @GetMapping("/search/paged")
    public ResponseEntity<PageResponse<Asset>> searchAssetsPaged(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.of(assetService.searchAssetsByKeyword(keyword, pageable)));
    }

    @GetMapping("/company/{company}")
    public ResponseEntity<List<Asset>> getAssetsByCompany(@PathVariable String company) {
        return ResponseEntity.ok(assetService.getAssetsByCompany(company));
    }

    @GetMapping("/owner/{owner}")
    public ResponseEntity<List<Asset>> getAssetsByOwner(@PathVariable String owner) {
        return ResponseEntity.ok(assetService.getAssetsByOwner(owner));
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<Asset>> getEnabledAssets() {
        return ResponseEntity.ok(assetService.getEnabledAssets());
    }

    @GetMapping("/target")
    public ResponseEntity<List<Asset>> getTargetAssets() {
        return ResponseEntity.ok(assetService.getTargetAssets());
    }

    @GetMapping("/topology/{topologyProjectId}")
    public ResponseEntity<List<Asset>> getAssetsByTopologyProject(@PathVariable Long topologyProjectId) {
        return ResponseEntity.ok(assetService.getAssetsByTopologyProjectId(topologyProjectId));
    }

    @GetMapping("/without-topology")
    public ResponseEntity<List<Asset>> getAssetsWithoutTopology() {
        return ResponseEntity.ok(assetService.getAssetsWithoutTopologyProject());
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<?> enableAsset(@PathVariable Long id) {
        assetService.enableAsset(id);
        return ResponseEntity.ok("✅ 资产已启用");
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<?> disableAsset(@PathVariable Long id) {
        assetService.disableAsset(id);
        return ResponseEntity.ok("✅ 资产已禁用");
    }

    @GetMapping("/project-stats/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectStats(@PathVariable String projectId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取项目相关资产
            List<Asset> projectAssets = assetRepository.findByTopologyProjectId(projectId);
            
            // 计算统计数据
            long totalAssets = projectAssets.size();
            long enabledAssets = projectAssets.stream().filter(Asset::isEnabled).count();
            long containerAssets = projectAssets.stream().filter(asset -> 
                asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()).count();
            long targetAssets = projectAssets.stream().filter(Asset::isTarget).count();
            
            stats.put("totalAssets", totalAssets);
            stats.put("enabledAssets", enabledAssets);
            stats.put("containerAssets", containerAssets);
            stats.put("targetAssets", targetAssets);
            stats.put("projectId", projectId);
            stats.put("generatedAt", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取项目统计失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取项目统计失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/target")
    public ResponseEntity<?> setAsTarget(@PathVariable Long id, @RequestParam boolean isTarget) {
        assetService.setAsTarget(id, isTarget);
        return ResponseEntity.ok("✅ 资产目标状态已更新");
    }

    @PostMapping("/{id}/assign-topology")
    public ResponseEntity<?> assignToTopology(@PathVariable Long id, @RequestParam Long topologyProjectId) {
        assetService.assignToTopologyProject(id, topologyProjectId);
        return ResponseEntity.ok("✅ 资产已分配到拓扑项目");
    }

    @PostMapping("/{id}/remove-topology")
    public ResponseEntity<?> removeFromTopology(@PathVariable Long id) {
        assetService.removeFromTopologyProject(id);
        return ResponseEntity.ok("✅ 资产已从拓扑项目中移除");
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAssetStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAssets", assetService.getTotalAssetsCount());
        statistics.put("enabledAssets", assetService.getEnabledAssetsCount());
        statistics.put("targetAssets", assetService.getTargetAssetsCount());
        statistics.put("companyStats", assetService.getCompanyStatistics());
        statistics.put("ownerStats", assetService.getOwnerStatistics());
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/validate-ip")
    public ResponseEntity<Boolean> validateIp(@RequestParam String ip) {
        return ResponseEntity.ok(assetService.isIpUnique(ip));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateAsset(@RequestBody Asset asset) {
        Map<String, Object> response = new HashMap<>();
        boolean isValid = assetService.validateAssetConfiguration(asset);
        response.put("valid", isValid);
        
        if (!isValid) {
            List<String> errors = List.of(
                asset.getName() == null || asset.getName().trim().isEmpty() ? "资产名称不能为空" : null,
                asset.getIp() == null || asset.getIp().trim().isEmpty() ? "IP地址不能为空" : null,
                asset.getCompany() == null || asset.getCompany().trim().isEmpty() ? "所属企业不能为空" : null
            ).stream().filter(error -> error != null).collect(Collectors.toList());
            response.put("errors", errors);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ip-range")
    public ResponseEntity<List<Asset>> getAssetsByIpRange(@RequestParam String ipPattern) {
        return ResponseEntity.ok(assetService.getAssetsByIpRange(ipPattern));
    }
    
    /**
     * 从指定节点发现容器
     */
    @GetMapping("/discover-containers/{nodeId}")
    public ResponseEntity<?> discoverContainersFromNode(@PathVariable Long nodeId) {
        try {
            Optional<HostNode> nodeOpt = hostNodeService.getNodeById(nodeId);
            if (nodeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点不存在"));
            }
            
            HostNode node = nodeOpt.get();
            Map<String, Object> discoveryResult = containerDiscoveryService.discoverAllContainers();
            
            // 过滤出指定节点的容器
            @SuppressWarnings("unchecked")
            Map<String, Object> nodesMap = (Map<String, Object>) discoveryResult.get("nodes");
            @SuppressWarnings("unchecked")
            Map<String, Object> nodeContainers = (Map<String, Object>) nodesMap.get("node_" + nodeId);
            
            if (nodeContainers == null) {
                return ResponseEntity.ok(Map.of(
                    "nodeId", nodeId,
                    "nodeName", node.getDisplayName(),
                    "containers", List.of(),
                    "message", "该节点上未发现容器"
                ));
            }
            
            @SuppressWarnings("unchecked")
            List<ContainerInfo> containers = (List<ContainerInfo>) nodeContainers.get("containers");
            List<ContainerInfo> suitableContainers = containers.stream()
                .filter(containerDiscoveryService::isContainerSuitableForAsset)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "nodeId", nodeId,
                "nodeName", node.getDisplayName(),
                "allContainers", containers,
                "suitableContainers", suitableContainers,
                "totalCount", containers.size(),
                "suitableCount", suitableContainers.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "容器发现失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量导入容器为资产
     */
    @PostMapping("/import-containers")
    public ResponseEntity<?> importContainersAsAssets(@RequestBody Map<String, Object> request) {
        try {
            Long nodeId = Long.valueOf(request.get("nodeId").toString());
            String company = (String) request.get("company");
            String project = (String) request.get("project");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> containerData = (List<Map<String, Object>>) request.get("containers");
            
            Optional<HostNode> nodeOpt = hostNodeService.getNodeById(nodeId);
            if (nodeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点不存在"));
            }
            
            HostNode node = nodeOpt.get();
            List<Asset> importedAssets = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (Map<String, Object> containerMap : containerData) {
                try {
                    // 构建 ContainerInfo 对象
                    ContainerInfo container = new ContainerInfo();
                    container.setContainerId((String) containerMap.get("containerId"));
                    container.setName((String) containerMap.get("name"));
                    container.setImage((String) containerMap.get("image"));
                    container.setStatus((String) containerMap.get("status"));
                    
                    // 检查是否适合转换为资产
                    if (!containerDiscoveryService.isContainerSuitableForAsset(container)) {
                        errors.add("容器 " + container.getName() + " 不适合转换为资产");
                        continue;
                    }
                    
                    // 转换为资产并保存
                    Asset asset = containerDiscoveryService.convertContainerToAsset(container, node, company, project);
                    
                    // 检查是否已存在相同名称的资产
                    if (assetRepository.findByNameAndCompany(asset.getName(), asset.getCompany()).isPresent()) {
                        errors.add("资产 " + asset.getName() + " 已存在");
                        continue;
                    }
                    
                    Asset savedAsset = assetRepository.save(asset);
                    importedAssets.add(savedAsset);
                } catch (Exception e) {
                    errors.add("导入容器失败: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "importedCount", importedAssets.size(),
                "totalRequested", containerData.size(),
                "importedAssets", importedAssets,
                "errors", errors
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "批量导入失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有节点的容器概览（用于容器发现界面）
     */
    @GetMapping("/containers-overview")
    public ResponseEntity<?> getContainersOverview() {
        try {
            Map<String, Object> overview = containerDiscoveryService.discoverAllContainers();
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取容器概览失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有容器类型的资产
     */
    @GetMapping("/container-assets")
    public ResponseEntity<List<Asset>> getContainerAssets() {
        try {
            List<Asset> containerAssets = assetService.getAllContainerAssets();
            return ResponseEntity.ok(containerAssets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据资产类型获取资产
     */
    @GetMapping("/by-type/{assetType}")
    public ResponseEntity<List<Asset>> getAssetsByType(@PathVariable String assetType) {
        try {
            List<Asset> assets = assetService.getAssetsByAssetType(assetType);
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取资产类型统计信息
     */
    @GetMapping("/type-statistics")
    public ResponseEntity<Map<String, Object>> getAssetTypeStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAssets", assetService.getTotalAssetsCount());
            stats.put("containerAssets", assetService.getContainerAssetsCount());
            stats.put("typeBreakdown", assetService.getAssetTypeStatistics());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 验证资产名称在企业内的唯一性
     */
    @GetMapping("/validate-name")
    public ResponseEntity<Boolean> validateAssetNameUniqueness(
            @RequestParam String name, 
            @RequestParam String company) {
        try {
            boolean isUnique = assetService.isAssetNameUniqueInCompany(name, company);
            return ResponseEntity.ok(isUnique);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取资产的容器清单
     */
    @GetMapping("/{id}/containers")
    public ResponseEntity<?> getAssetContainers(@PathVariable Long id) {
        try {
            Optional<Asset> assetOpt = assetService.getAssetById(id);
            if (assetOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "资产不存在"));
            }
            
            Asset asset = assetOpt.get();
            
            // 获取资产关联的主机节点容器
            if (asset.getPreferredHostNodeId() != null) {
                Optional<HostNode> nodeOpt = hostNodeService.getNodeById(asset.getPreferredHostNodeId());
                if (nodeOpt.isPresent()) {
                    HostNode node = nodeOpt.get();
                    
                    // 获取该节点上的所有容器
                    List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(node.getId());
                    
                    // 过滤出与该资产相关的容器（通过镜像名称或容器名称匹配）
                    List<ContainerInfo> relevantContainers = containers.stream()
                        .filter(container -> isContainerRelevantToAsset(container, asset))
                        .collect(Collectors.toList());
                    
                    return ResponseEntity.ok(Map.of(
                        "assetId", asset.getId(),
                        "assetName", asset.getName(),
                        "hostNode", Map.of(
                            "id", node.getId(),
                            "name", node.getDisplayName(),
                            "status", node.getStatus()
                        ),
                        "containers", relevantContainers,
                        "totalCount", relevantContainers.size(),
                        "lastRefresh", System.currentTimeMillis()
                    ));
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "关联的主机节点不存在"));
                }
            } else {
                // 如果没有关联主机节点，返回空列表
                return ResponseEntity.ok(Map.of(
                    "assetId", asset.getId(),
                    "assetName", asset.getName(),
                    "hostNode", null,
                    "containers", List.of(),
                    "totalCount", 0,
                    "message", "该资产未关联主机节点"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取容器清单失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新资产的容器清单
     */
    @PostMapping("/{id}/refresh-containers")
    public ResponseEntity<?> refreshAssetContainers(@PathVariable Long id) {
        try {
            Optional<Asset> assetOpt = assetService.getAssetById(id);
            if (assetOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "资产不存在"));
            }
            
            Asset asset = assetOpt.get();
            
            if (asset.getPreferredHostNodeId() != null) {
                Optional<HostNode> nodeOpt = hostNodeService.getNodeById(asset.getPreferredHostNodeId());
                if (nodeOpt.isPresent()) {
                    HostNode node = nodeOpt.get();
                    
                    // 强制刷新该节点的容器信息
                    List<ContainerInfo> containers = containerDiscoveryService.forceRefreshNodeContainers(node.getId());
                    
                    // 过滤出与该资产相关的容器
                    List<ContainerInfo> relevantContainers = containers.stream()
                        .filter(container -> isContainerRelevantToAsset(container, asset))
                        .collect(Collectors.toList());
                    
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "容器清单刷新成功",
                        "assetId", asset.getId(),
                        "containers", relevantContainers,
                        "totalCount", relevantContainers.size(),
                        "refreshTime", System.currentTimeMillis()
                    ));
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "关联的主机节点不存在"));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "该资产未关联主机节点，无法刷新容器"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "刷新容器清单失败: " + e.getMessage()));
        }
    }
    
    /**
     * 测试资产主机连接
     */
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<?> testAssetConnection(@PathVariable Long id) {
        try {
            Optional<Asset> assetOpt = assetService.getAssetById(id);
            if (assetOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "资产不存在"));
            }
            
            Asset asset = assetOpt.get();
            
            if (asset.getPreferredHostNodeId() != null) {
                Optional<HostNode> nodeOpt = hostNodeService.getNodeById(asset.getPreferredHostNodeId());
                if (nodeOpt.isPresent()) {
                    HostNode node = nodeOpt.get();
                    
                    // 测试节点连接
                    boolean isConnected = hostNodeService.testNodeConnection(node.getId());
                    
                    if (isConnected) {
                        // 如果连接成功，尝试获取Docker信息
                        try {
                            List<ContainerInfo> containers = containerDiscoveryService.getContainersFromNode(node.getId());
                            return ResponseEntity.ok(Map.of(
                                "success", true,
                                "connected", true,
                                "message", "连接测试成功",
                                "assetId", asset.getId(),
                                "hostNode", Map.of(
                                    "id", node.getId(),
                                    "name", node.getDisplayName(),
                                    "status", "在线"
                                ),
                                "dockerAvailable", true,
                                "containerCount", containers.size(),
                                "testTime", System.currentTimeMillis()
                            ));
                        } catch (Exception e) {
                            return ResponseEntity.ok(Map.of(
                                "success", true,
                                "connected", true,
                                "message", "主机连接成功，但Docker服务异常",
                                "assetId", asset.getId(),
                                "dockerAvailable", false,
                                "dockerError", e.getMessage(),
                                "testTime", System.currentTimeMillis()
                            ));
                        }
                    } else {
                        return ResponseEntity.ok(Map.of(
                            "success", false,
                            "connected", false,
                            "message", "主机连接失败",
                            "assetId", asset.getId(),
                            "hostNode", Map.of(
                                "id", node.getId(),
                                "name", node.getDisplayName(),
                                "status", "离线"
                            ),
                            "testTime", System.currentTimeMillis()
                        ));
                    }
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "关联的主机节点不存在"));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "该资产未关联主机节点，无法测试连接"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "连接测试失败: " + e.getMessage()));
        }
    }
    
    /**
     * 判断容器是否与资产相关
     */
    private boolean isContainerRelevantToAsset(ContainerInfo container, Asset asset) {
        if (container == null || asset == null) {
            return false;
        }
        
        // 1. 通过Docker镜像匹配
        if (asset.getDockerImage() != null && !asset.getDockerImage().trim().isEmpty()) {
            String assetImage = asset.getDockerImage().trim();
            String containerImage = container.getImage();
            
            // 支持模糊匹配（忽略版本标签）
            if (containerImage != null) {
                String assetImageBase = assetImage.split(":")[0];
                String containerImageBase = containerImage.split(":")[0];
                if (assetImageBase.equals(containerImageBase)) {
                    return true;
                }
            }
        }
        
        // 2. 通过容器名称匹配
        String expectedContainerName = asset.getDefaultContainerName();
        if (container.getName() != null && container.getName().contains(expectedContainerName)) {
            return true;
        }
        
        // 3. 通过资产名称匹配
        if (asset.getName() != null && container.getName() != null) {
            String assetNameLower = asset.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            String containerNameLower = container.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            if (containerNameLower.contains(assetNameLower) || assetNameLower.contains(containerNameLower)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 根据项目ID获取资产列表（新接口）
     */
    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<List<Asset>> getAssetsByProject(@PathVariable String projectId) {
        try {
            // 支持多种项目ID格式的查询
            List<Asset> assets = assetRepository.findAll().stream()
                .filter(asset -> {
                    // 检查项目名称匹配
                    if (projectId.equals(asset.getProject())) {
                        return true;
                    }
                    // 检查拓扑项目ID匹配
                    if (projectId.equals(asset.getTopologyProjectId())) {
                        return true;
                    }
                    // 检查组合项目标识符匹配
                    String combinedId = String.format("%s|%s", 
                        asset.getCompany() != null ? asset.getCompany() : "未知企业",
                        asset.getProject() != null ? asset.getProject() : "未分组"
                    );
                    return projectId.equals(combinedId);
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取节点关联的资产列表
     */
    @GetMapping("/by-node/{nodeId}")
    public ResponseEntity<List<Asset>> getAssetsByNode(@PathVariable Long nodeId) {
        try {
            List<Asset> assets = assetRepository.findAll().stream()
                .filter(asset -> nodeId.equals(asset.getPreferredHostNodeId()))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取资产的容器状态聚合信息
     */
    @GetMapping("/container-status")
    public ResponseEntity<Map<String, Object>> getAssetsWithContainerStatus() {
        try {
            List<Asset> allAssets = assetRepository.findAll();
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> assetsWithStatus = new ArrayList<>();
            
            for (Asset asset : allAssets) {
                Map<String, Object> assetInfo = new HashMap<>();
                assetInfo.put("id", asset.getId());
                assetInfo.put("name", asset.getName());
                assetInfo.put("ip", asset.getIp());
                assetInfo.put("company", asset.getCompany());
                assetInfo.put("project", asset.getProject());
                assetInfo.put("assetType", asset.getAssetType());
                assetInfo.put("dockerImage", asset.getDockerImage());
                assetInfo.put("preferredHostNodeId", asset.getPreferredHostNodeId());
                assetInfo.put("preferredHostNodeName", asset.getPreferredHostNodeName());
                assetInfo.put("deploymentStrategy", asset.getDeploymentStrategy());
                
                // 获取容器运行状态（这里可以集成DrillContainerService获取实际状态）
                if (asset.isContainerAsset()) {
                    // TODO: 集成实际的容器状态查询
                    assetInfo.put("containerStatus", "not_deployed");
                    assetInfo.put("runningContainers", 0);
                } else {
                    assetInfo.put("containerStatus", null);
                    assetInfo.put("runningContainers", null);
                }
                
                assetsWithStatus.add(assetInfo);
            }
            
            result.put("assets", assetsWithStatus);
            result.put("total", allAssets.size());
            result.put("containerAssets", assetsWithStatus.stream()
                .mapToInt(asset -> "container".equals(asset.get("assetType")) ? 1 : 0)
                .sum());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取节点资产分布统计
     */
    @GetMapping("/node-distribution")
    public ResponseEntity<Map<String, Object>> getNodeAssetDistribution() {
        try {
            List<Asset> allAssets = assetRepository.findAll();
            Map<String, Object> distribution = new HashMap<>();
            
            // 按节点分组统计
            Map<String, List<Asset>> nodeAssets = allAssets.stream()
                .filter(asset -> asset.getPreferredHostNodeId() != null)
                .collect(Collectors.groupingBy(asset -> 
                    asset.getPreferredHostNodeName() != null ? 
                    asset.getPreferredHostNodeName() : "Unknown Node"));
            
            // 统计未分配节点的资产
            long unassignedAssets = allAssets.stream()
                .filter(asset -> asset.getPreferredHostNodeId() == null)
                .count();
            
            distribution.put("nodeAssets", nodeAssets.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                )));
            distribution.put("unassignedAssets", unassignedAssets);
            distribution.put("totalAssets", allAssets.size());
            
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取项目统计信息
     */
    @GetMapping("/project-statistics")
    public ResponseEntity<Map<String, Object>> getProjectStatistics(@RequestParam String projectId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 解析项目ID获取企业和项目名称
            String[] parts = projectId.split("｜");
            String company = parts.length > 0 ? parts[0] : "";
            String project = parts.length > 1 ? parts[1] : "";
            
            // 获取项目资产
            List<Asset> projectAssets = assetRepository.findByCompanyAndProject(company, project);
            
            // 统计不同类型的资产
            long totalAssets = projectAssets.size();
            long containerAssets = projectAssets.stream().filter(a -> "container".equals(a.getAssetType())).count();
            long serverAssets = projectAssets.stream().filter(a -> "server".equals(a.getAssetType())).count();
            long serviceAssets = projectAssets.stream().filter(a -> "service".equals(a.getAssetType())).count();
            long networkAssets = projectAssets.stream().filter(a -> "network".equals(a.getAssetType())).count();
            long enabledAssets = projectAssets.stream().filter(Asset::isEnabled).count();
            
            stats.put("totalAssets", totalAssets);
            stats.put("containerAssets", containerAssets);
            stats.put("serverAssets", serverAssets);
            stats.put("serviceAssets", serviceAssets);
            stats.put("networkAssets", networkAssets);
            stats.put("enabledAssets", enabledAssets);
            
            // 获取项目关联的节点和容器统计
            List<HostNode> projectNodes = hostNodeService.getAllNodes().stream()
                .filter(node -> projectAssets.stream()
                    .anyMatch(asset -> node.getId().equals(asset.getPreferredHostNodeId())))
                .collect(Collectors.toList());
            
            // 计算节点统计
            long totalNodes = projectNodes.size();
            long activeNodes = projectNodes.stream().filter(node -> "active".equals(node.getStatus())).count();
            long inactiveNodes = totalNodes - activeNodes;
            long maintenanceNodes = projectNodes.stream().filter(node -> "maintenance".equals(node.getStatus())).count();
            
            stats.put("totalNodes", totalNodes);
            stats.put("activeNodes", activeNodes);
            stats.put("inactiveNodes", inactiveNodes);
            stats.put("maintenanceNodes", maintenanceNodes);
            
            // 计算容器统计 - 使用HostNodeService的实际容器计数
            long totalContainers = 0;
            long runningContainers = 0;
            
            try {
                // 获取全局容器统计信息
                Map<String, Object> clusterStats = hostNodeService.getClusterLoadStatistics();
                totalContainers = ((Number) clusterStats.getOrDefault("totalContainers", 0)).longValue();
                runningContainers = ((Number) clusterStats.getOrDefault("runningContainers", 0)).longValue();
                
                // 如果项目有特定节点，则统计这些节点上的容器
                if (!projectNodes.isEmpty()) {
                    totalContainers = 0;
                    runningContainers = 0;
                    for (HostNode node : projectNodes) {
                        Map<String, Object> nodeLoad = hostNodeService.getNodeLoadInfo(node.getId());
                        totalContainers += ((Number) nodeLoad.getOrDefault("totalContainers", 0)).longValue();
                        runningContainers += ((Number) nodeLoad.getOrDefault("runningContainers", 0)).longValue();
                    }
                }
            } catch (Exception e) {
                // Debug statement removed
                // 降级处理，设置为0
                totalContainers = 0;
                runningContainers = 0;
            }
            
            stats.put("totalContainers", totalContainers);
            stats.put("runningContainers", runningContainers);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取项目统计信息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取项目统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目活动日志（从SystemLog表查询真实数据）
     */
    @GetMapping("/project-activity")
    public ResponseEntity<List<Map<String, Object>>> getProjectActivity(@RequestParam String projectId) {
        try {
            List<Map<String, Object>> activities = new ArrayList<>();

            // 解析项目ID获取企业和项目名称（用于日志描述匹配）
            String[] parts = projectId.split("｜");
            String company = parts.length > 0 ? parts[0] : "";
            String project = parts.length > 1 ? parts[1] : "";

            // 查询最近24小时的系统日志
            LocalDateTime since = LocalDateTime.now().minusDays(1);

            // 查询与资产相关的操作日志
            List<SystemLog> assetLogs = systemLogRepository
                .findByOperationContainingAndTimestampAfter("ASSET", since);

            // 查询与容器相关的操作日志
            List<SystemLog> containerLogs = systemLogRepository
                .findByOperationContainingAndTimestampAfter("CONTAINER", since);

            // 合并所有日志
            List<SystemLog> allLogs = new ArrayList<>();
            allLogs.addAll(assetLogs);
            allLogs.addAll(containerLogs);

            // 按时间倒序排序并取前20条
            allLogs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            List<SystemLog> recentLogs = allLogs.stream()
                .limit(20)
                .collect(Collectors.toList());

            // 转换为前端需要的格式
            for (SystemLog log : recentLogs) {
                Map<String, Object> activity = new HashMap<>();

                // 根据operation类型确定activity type
                String activityType = mapOperationToActivityType(log.getOperation());
                activity.put("type", activityType);

                // 描述信息
                activity.put("description", log.getDescription());

                // 时间戳（转换为毫秒）
                long timestamp = log.getTimestamp() != null ?
                    log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() :
                    System.currentTimeMillis();
                activity.put("timestamp", timestamp);

                // 额外信息
                activity.put("username", log.getUsername());
                activity.put("operation", log.getOperation());

                activities.add(activity);
            }

            // 如果没有日志，返回空列表
            return ResponseEntity.ok(activities);

        } catch (Exception e) {
            // 如果查询失败，返回空列表而不是错误
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * 将操作类型映射为活动类型
     */
    private String mapOperationToActivityType(String operation) {
        if (operation == null) {
            return "unknown";
        }

        if (operation.contains("ASSET_CREATED") || operation.contains("ASSET") && operation.contains("CREATE")) {
            return "asset_created";
        } else if (operation.contains("ASSET_UPDATED") || operation.contains("ASSET") && operation.contains("UPDATE")) {
            return "asset_updated";
        } else if (operation.contains("ASSET_DELETED") || operation.contains("ASSET") && operation.contains("DELETE")) {
            return "asset_deleted";
        } else if (operation.contains("CONTAINER_START") || operation.contains("DEPLOYED")) {
            return "container_deployed";
        } else if (operation.contains("CONTAINER_STOP")) {
            return "container_stopped";
        } else if (operation.contains("NODE") && operation.contains("ADD")) {
            return "node_added";
        } else if (operation.contains("NODE") && operation.contains("REMOVE")) {
            return "node_removed";
        } else {
            return "other";
        }
    }

    /**
     * 导出项目报告
     */
    @GetMapping("/project-report")
    public ResponseEntity<String> exportProjectReport(@RequestParam String projectId) {
        try {
            // 简单的报告生成，实际项目中应该生成PDF或Excel文件
            String report = "项目报告 - " + projectId + "\n生成时间: " + new java.util.Date() + "\n这是一个示例报告。";
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=project-report.txt");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/plain");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 验证项目资产部署策略一致性
     */
    @GetMapping("/validate-deployment/{projectId}")
    public ResponseEntity<Map<String, Object>> validateProjectDeployment(@PathVariable String projectId) {
        try {
            Map<String, Object> validation = assetService.validateProjectAssetDeploymentConsistency(projectId);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 修复项目资产部署策略一致性问题
     */
    @PostMapping("/fix-deployment/{projectId}")
    public ResponseEntity<Map<String, Object>> fixProjectDeployment(@PathVariable String projectId) {
        try {
            Map<String, Object> result = assetService.fixAssetDeploymentInconsistency(projectId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "修复失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // ========== 智能负载均衡相关接口 ==========

    /**
     * 获取项目资产重分配建议
     */
    @GetMapping("/project-redistribution-plan/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectRedistributionPlan(
            @PathVariable String projectId,
            @RequestParam(required = false) String environment) {
        try {
            Map<String, Object> redistributionPlan = assetService.redistributeProjectAssets(projectId, environment);
            return ResponseEntity.ok(redistributionPlan);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "计划生成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 执行项目资产重分配
     */
    @PostMapping("/project-redistribution/{projectId}")
    public ResponseEntity<Map<String, Object>> executeProjectRedistribution(
            @PathVariable String projectId,
            @RequestParam(required = false) String environment,
            @RequestParam(defaultValue = "false") boolean forceRebalance) {
        try {
            Map<String, Object> result = assetService.executeAssetRedistribution(projectId, environment, forceRebalance);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "重分配失败: " + e.getMessage()));
        }
    }
    
    /**
     * 为新资产推荐最优节点
     */
    @GetMapping("/recommend-node/{projectId}")
    public ResponseEntity<Map<String, Object>> recommendOptimalNode(
            @PathVariable String projectId,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String assetType) {
        try {
            Map<String, Object> recommendation = assetService.recommendOptimalNode(projectId, environment, assetType);
            return ResponseEntity.ok(recommendation);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "推荐失败: " + e.getMessage()));
        }
    }
    
    // ========== 增强部署策略和故障转移接口 ==========
    
    /**
     * 为单个资产选择最优部署节点
     */
    @PostMapping("/{id}/select-optimal-node")
    public ResponseEntity<Map<String, Object>> selectOptimalNode(
            @PathVariable Long id,
            @RequestParam(required = false) String environment) {
        try {
            Optional<Asset> assetOpt = assetService.getAssetById(id);
            if (assetOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "资产不存在"));
            }
            
            Map<String, Object> result = assetService.selectOptimalDeploymentNode(assetOpt.get(), environment);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "选择失败: " + e.getMessage()));
        }
    }
    
    /**
     * 执行资产故障转移
     */
    @PostMapping("/{id}/failover")
    public ResponseEntity<Map<String, Object>> performAssetFailover(
            @PathVariable Long id,
            @RequestParam Long failedNodeId) {
        try {
            Map<String, Object> result = assetService.performFailoverIfNeeded(id, failedNodeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "故障转移失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量故障转移检查
     */
    @PostMapping("/batch-failover")
    public ResponseEntity<Map<String, Object>> batchFailoverCheck(@RequestParam Long failedNodeId) {
        try {
            Map<String, Object> result = assetService.batchFailoverCheck(failedNodeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "批量故障转移失败: " + e.getMessage()));
        }
    }
    
    /**
     * 验证和修复项目部署配置
     */
    @PostMapping("/validate-and-fix-deployment/{projectId}")
    public ResponseEntity<Map<String, Object>> validateAndFixDeploymentConfiguration(@PathVariable String projectId) {
        try {
            Map<String, Object> result = assetService.validateAndFixDeploymentConfiguration(projectId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Debug statement removed
            return ResponseEntity.internalServerError().body(Map.of("error", "验证和修复失败: " + e.getMessage()));
        }
    }
}