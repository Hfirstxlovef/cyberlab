package org.cyberlab.controller;

import org.cyberlab.service.ContainerStateService;
import org.cyberlab.task.ContainerSyncTask;
import org.cyberlab.entity.ContainerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 容器同步管理控制器
 * 提供容器状态同步的管理接口
 */
@RestController
@RequestMapping("/api/container-sync")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class ContainerSyncController {

    @Autowired
    private ContainerStateService containerStateService;
    
    @Autowired
    private ContainerSyncTask containerSyncTask;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    /**
     * 获取容器状态统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = containerStateService.getContainerStateStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取统计信息失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取同步任务状态
     */
    @GetMapping("/task-status")
    public ResponseEntity<Map<String, Object>> getTaskStatus() {
        try {
            Map<String, Object> status = containerSyncTask.getTaskStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取任务状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 手动触发同步
     */
    @PostMapping("/trigger-sync")
    public ResponseEntity<Map<String, Object>> triggerSync() {
        try {
            boolean triggered = containerSyncTask.triggerManualSync();
            
            Map<String, Object> response = new HashMap<>();
            if (triggered) {
                response.put("success", true);
                response.put("message", "同步任务已触发");
            } else {
                response.put("success", false);
                response.put("message", "同步任务正在进行中，请稍后再试");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "触发同步失败: " + e.getMessage())
            );
        }
    }

    /**
     * 重置失败状态
     */
    @PostMapping("/reset-failed")
    public ResponseEntity<Map<String, Object>> resetFailedStates() {
        try {
            List<ContainerState> resetStates = containerStateService.resetFailedStates();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已重置失败状态",
                "resetCount", resetStates.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "重置失败状态异常: " + e.getMessage())
            );
        }
    }

    /**
     * 强制同步指定资产的容器
     */
    @PostMapping("/force-sync-asset/{assetId}")
    public ResponseEntity<Map<String, Object>> forceSyncAsset(@PathVariable Long assetId) {
        try {
            String currentUser = getCurrentUsername();
            Map<String, Object> result = containerStateService.forceSyncAssetContainers(assetId, currentUser);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "强制同步资产失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取指定资产的容器状态
     */
    @GetMapping("/asset/{assetId}/states")
    public ResponseEntity<?> getAssetContainerStates(@PathVariable Long assetId) {
        try {
            List<ContainerState> states = containerStateService.getContainerStatesByAsset(assetId);
            
            return ResponseEntity.ok(Map.of(
                "assetId", assetId,
                "containerStates", states,
                "totalCount", states.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取资产容器状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取指定主机的容器状态
     */
    @GetMapping("/host/{hostId}/states")
    public ResponseEntity<?> getHostContainerStates(@PathVariable Long hostId) {
        try {
            List<ContainerState> states = containerStateService.getContainerStatesByHost(hostId);
            
            return ResponseEntity.ok(Map.of(
                "hostId", hostId,
                "containerStates", states,
                "totalCount", states.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取主机容器状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 创建或更新容器状态
     */
    @PostMapping("/states")
    public ResponseEntity<Map<String, Object>> createOrUpdateState(@RequestBody Map<String, Object> request) {
        try {
            Long assetId = Long.valueOf(request.get("assetId").toString());
            Long hostNodeId = Long.valueOf(request.get("hostNodeId").toString());
            String containerId = (String) request.get("containerId");
            String desiredStatus = (String) request.get("desiredStatus");
            String currentUser = getCurrentUsername();
            
            ContainerState state = containerStateService.createOrUpdateContainerState(
                assetId, hostNodeId, containerId, desiredStatus, currentUser
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "容器状态已创建/更新",
                "containerState", state
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "创建/更新容器状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 清理过期状态
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldStates(
            @RequestParam(defaultValue = "7") int daysToKeep) {
        try {
            int cleaned = containerStateService.cleanupOldStates(daysToKeep);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "清理完成",
                "cleanedCount", cleaned,
                "daysToKeep", daysToKeep
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "清理过期状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 重置任务失败计数
     */
    @PostMapping("/reset-failure-count")
    public ResponseEntity<Map<String, Object>> resetFailureCount() {
        try {
            containerSyncTask.resetFailureCount();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已重置失败计数"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "重置失败计数异常: " + e.getMessage())
            );
        }
    }

    /**
     * 获取需要调和的容器状态
     */
    @GetMapping("/needing-reconciliation")
    public ResponseEntity<?> getNeedingReconciliation() {
        try {
            // 这里我们需要直接访问repository，但为了保持封装性，我们通过service获取统计信息
            Map<String, Object> stats = containerStateService.getContainerStateStatistics();
            
            return ResponseEntity.ok(Map.of(
                "needingReconciliation", stats.get("needingReconciliation"),
                "failedSyncs", stats.get("failedSyncs"),
                "syncStatusStats", stats.get("syncStatusStats")
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取需调和状态失败: " + e.getMessage())
            );
        }
    }
}