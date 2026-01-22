package org.cyberlab.controller;

import org.cyberlab.entity.Asset;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.service.ContainerExceptionHandler;
import org.cyberlab.service.ContainerPermissionService;
import org.cyberlab.service.AssetService;
import org.cyberlab.service.ContainerAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 容器保护控制器
 * 提供容器异常检测、保护机制和恢复建议的接口
 */
@RestController
@RequestMapping("/api/container-protection")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class ContainerProtectionController {

    @Autowired
    private ContainerExceptionHandler exceptionHandler;
    
    @Autowired
    private ContainerPermissionService permissionService;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private ContainerAuditService auditService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * 检查主机状态
     */
    @GetMapping("/host-status/{hostId}")
    public ResponseEntity<?> checkHostStatus(@PathVariable Long hostId) {
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证
            if (!permissionService.canAccessHostContainers(currentUser, hostId)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限检查该主机状态")
                );
            }
            
            Map<String, Object> hostStatus = exceptionHandler.checkHostStatus(hostId);
            
            // 记录操作日志
            auditService.logContainerOperation(currentUser, "CHECK_HOST_STATUS", 
                                             "", "", 
                                             "Host:" + hostId, 
                                             (String) hostStatus.get("status"));
            
            return ResponseEntity.ok(hostStatus);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "检查主机状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 检测所有失联的容器
     */
    @GetMapping("/lost-containers")
    public ResponseEntity<?> detectLostContainers() {
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证 - 只有管理员可以执行全局检测
            if (!permissionService.canManageContainerSync(currentUser)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限执行失联容器检测")
                );
            }
            
            List<Map<String, Object>> lostContainers = exceptionHandler.detectLostContainers();
            
            // 记录操作日志
            auditService.logContainerOperation(currentUser, "DETECT_LOST_CONTAINERS", 
                                             "", "", 
                                             "Global", 
                                             String.format("发现%d个失联容器", lostContainers.size()));
            
            return ResponseEntity.ok(Map.of(
                "lostContainers", lostContainers,
                "totalCount", lostContainers.size(),
                "scanTime", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "检测失联容器失败: " + e.getMessage())
            );
        }
    }

    /**
     * 检查指定资产的容器是否失联
     */
    @GetMapping("/check-container-lost/{assetId}")
    public ResponseEntity<?> checkContainerLost(@PathVariable Long assetId) {
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证
            if (!permissionService.canAccessAssetContainers(currentUser, assetId)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限检查该资产的容器状态")
                );
            }
            
            Optional<Asset> assetOpt = assetService.getAssetById(assetId);
            if (assetOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "资产不存在")
                );
            }
            
            Asset asset = assetOpt.get();
            Map<String, Object> lostStatus = exceptionHandler.checkContainerLost(asset);
            
            // 记录操作日志
            auditService.logAssetContainerOperation(currentUser, "CHECK_CONTAINER_LOST", 
                                                  assetId, asset.getName(), "", 
                                                  (Boolean) lostStatus.get("isLost") ? "失联" : "正常");
            
            return ResponseEntity.ok(lostStatus);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "检查容器失联状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 端口冲突预检查
     */
    @PostMapping("/precheck-ports")
    public ResponseEntity<?> precheckPortConflicts(@RequestBody Map<String, Object> request) {
        try {
            String currentUser = getCurrentUsername();
            String portMapping = (String) request.get("portMapping");
            Long hostNodeId = request.get("hostNodeId") != null ? 
                Long.valueOf(request.get("hostNodeId").toString()) : null;
            
            // 权限验证
            if (hostNodeId != null && !permissionService.canAccessHostContainers(currentUser, hostNodeId)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限检查该主机的端口状态")
                );
            }
            
            Map<String, Object> conflictCheck = exceptionHandler.preCheckPortConflicts(portMapping, hostNodeId);
            
            // 记录操作日志
            auditService.logContainerOperation(currentUser, "PRECHECK_PORTS", 
                                             "", "", 
                                             "Host:" + hostNodeId + ",Ports:" + portMapping, 
                                             (Boolean) conflictCheck.get("hasConflict") ? "有冲突" : "无冲突");
            
            return ResponseEntity.ok(conflictCheck);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "端口冲突预检查失败: " + e.getMessage())
            );
        }
    }

    /**
     * 容器重新匹配建议
     */
    @PostMapping("/suggest-rematch")
    public ResponseEntity<?> suggestContainerRematch(@RequestBody Map<String, Object> request) {
        try {
            String currentUser = getCurrentUsername();
            Long assetId = Long.valueOf(request.get("assetId").toString());
            
            // 权限验证
            if (!permissionService.canAccessAssetContainers(currentUser, assetId)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限为该资产建议容器重新匹配")
                );
            }
            
            Optional<Asset> assetOpt = assetService.getAssetById(assetId);
            if (assetOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "资产不存在")
                );
            }
            
            Asset asset = assetOpt.get();
            
            // 获取可用容器列表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> availableContainersData = 
                (List<Map<String, Object>>) request.get("availableContainers");
            
            // 转换为ContainerInfo对象（简化处理）
            List<ContainerInfo> availableContainers = availableContainersData.stream()
                .map(this::mapToContainerInfo)
                .toList();
            
            List<Map<String, Object>> suggestions = exceptionHandler.suggestContainerRematch(asset, availableContainers);
            
            // 记录操作日志
            auditService.logAssetContainerOperation(currentUser, "SUGGEST_REMATCH", 
                                                  assetId, asset.getName(), "", 
                                                  String.format("生成%d个重匹配建议", suggestions.size()));
            
            return ResponseEntity.ok(Map.of(
                "assetId", assetId,
                "assetName", asset.getName(),
                "suggestions", suggestions,
                "suggestionCount", suggestions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "生成容器重新匹配建议失败: " + e.getMessage())
            );
        }
    }

    /**
     * 验证主机操作
     */
    @PostMapping("/validate-host-operation")
    public ResponseEntity<?> validateHostOperation(@RequestBody Map<String, Object> request) {
        try {
            String currentUser = getCurrentUsername();
            Long hostNodeId = Long.valueOf(request.get("hostNodeId").toString());
            String operation = (String) request.get("operation");
            
            // 权限验证
            if (!permissionService.canAccessHostContainers(currentUser, hostNodeId)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限在该主机上执行操作")
                );
            }
            
            Map<String, Object> validation = exceptionHandler.validateHostOperation(hostNodeId, operation);
            
            // 记录操作日志
            auditService.logContainerOperation(currentUser, "VALIDATE_HOST_OPERATION", 
                                             "", "", 
                                             String.format("Host:%d,Operation:%s", hostNodeId, operation), 
                                             (Boolean) validation.get("allowed") ? "允许" : "禁止");
            
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "验证主机操作失败: " + e.getMessage())
            );
        }
    }

    /**
     * 批量健康检查
     */
    @PostMapping("/batch-health-check")
    public ResponseEntity<?> batchHealthCheck(@RequestBody Map<String, Object> request) {
        try {
            String currentUser = getCurrentUsername();
            @SuppressWarnings("unchecked")
            List<Long> assetIds = (List<Long>) request.get("assetIds");
            
            if (assetIds == null || assetIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "未指定要检查的资产")
                );
            }
            
            // 权限验证
            if (!permissionService.canPerformBatchContainerOperations(currentUser, assetIds)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限执行批量健康检查")
                );
            }
            
            List<Map<String, Object>> healthResults = new ArrayList<>();
            
            for (Long assetId : assetIds) {
                try {
                    Optional<Asset> assetOpt = assetService.getAssetById(assetId);
                    if (assetOpt.isPresent()) {
                        Asset asset = assetOpt.get();
                        Map<String, Object> healthStatus = exceptionHandler.checkContainerLost(asset);
                        healthStatus.put("assetId", assetId);
                        healthStatus.put("assetName", asset.getName());
                        healthResults.add(healthStatus);
                    }
                } catch (Exception e) {
                    healthResults.add(Map.of(
                        "assetId", assetId,
                        "error", "检查失败: " + e.getMessage(),
                        "isLost", true
                    ));
                }
            }
            
            long healthyCount = healthResults.stream()
                .mapToLong(result -> !(Boolean) result.getOrDefault("isLost", true) ? 1 : 0)
                .sum();
            
            // 记录操作日志
            auditService.logContainerOperation(currentUser, "BATCH_HEALTH_CHECK", 
                                             "", "", 
                                             String.format("Assets:%s", assetIds.toString()), 
                                             String.format("健康:%d,总数:%d", healthyCount, healthResults.size()));
            
            return ResponseEntity.ok(Map.of(
                "healthResults", healthResults,
                "totalChecked", healthResults.size(),
                "healthyCount", healthyCount,
                "unhealthyCount", healthResults.size() - healthyCount,
                "checkTime", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "批量健康检查失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取系统保护状态概览
     */
    @GetMapping("/protection-overview")
    public ResponseEntity<?> getProtectionOverview() {
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证 - 只有管理员可以查看系统概览
            if (!permissionService.canManageContainerSync(currentUser)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限查看系统保护概览")
                );
            }
            
            // 检测失联容器
            List<Map<String, Object>> lostContainers = exceptionHandler.detectLostContainers();
            
            Map<String, Object> overview = new HashMap<>();
            overview.put("lostContainersCount", lostContainers.size());
            overview.put("protectionEnabled", true);
            overview.put("lastScanTime", System.currentTimeMillis());
            
            // 统计保护状态
            Map<String, Object> protectionStats = new HashMap<>();
            protectionStats.put("totalAssets", assetService.getAllAssets().size());
            protectionStats.put("containerAssets", assetService.getAllContainerAssets().size());
            protectionStats.put("lostContainers", lostContainers.size());
            protectionStats.put("protectionRate", lostContainers.isEmpty() ? 1.0 : 
                (1.0 - (double) lostContainers.size() / assetService.getAllContainerAssets().size()));
            
            overview.put("protectionStatistics", protectionStats);
            overview.put("recentLostContainers", lostContainers);
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取保护概览失败: " + e.getMessage())
            );
        }
    }

    /**
     * 将Map转换为ContainerInfo对象（辅助方法）
     */
    private ContainerInfo mapToContainerInfo(Map<String, Object> data) {
        ContainerInfo container = new ContainerInfo();
        container.setContainerId((String) data.get("containerId"));
        container.setName((String) data.get("name"));
        container.setImage((String) data.get("image"));
        container.setStatus((String) data.get("status"));
        container.setPortMappings((String) data.get("portMappings"));
        return container;
    }
}