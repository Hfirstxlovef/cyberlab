package org.cyberlab.controller;

import org.cyberlab.entity.SystemLog;
import org.cyberlab.service.ContainerAuditService;
import org.cyberlab.service.ContainerPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 容器审计控制器
 * 提供容器操作审计日志的查询接口
 */
@RestController
@RequestMapping("/api/container-audit")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class ContainerAuditController {

    @Autowired
    private ContainerAuditService containerAuditService;
    
    @Autowired
    private ContainerPermissionService containerPermissionService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * 获取容器操作日志
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getContainerOperationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证
            if (!containerPermissionService.canViewContainerAuditLogs(currentUser, username)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限查看审计日志")
                );
            }
            
            // 如果非管理员且未指定用户名，则只查看自己的日志
            String targetUsername = username;
            if (!containerPermissionService.canManageContainerSync(currentUser) && username == null) {
                targetUsername = currentUser;
            }
            
            Page<SystemLog> logs = containerAuditService.getContainerOperationLogs(
                page, size, targetUsername, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "logs", logs.getContent(),
                "totalElements", logs.getTotalElements(),
                "totalPages", logs.getTotalPages(),
                "currentPage", logs.getNumber(),
                "pageSize", logs.getSize(),
                "hasNext", logs.hasNext(),
                "hasPrevious", logs.hasPrevious()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取容器操作日志失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取容器操作统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getContainerOperationStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证 - 只有管理员可以查看统计信息
            if (!containerPermissionService.canManageContainerSync(currentUser)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限查看操作统计")
                );
            }
            
            Map<String, Object> stats = containerAuditService.getContainerOperationStatistics(startTime, endTime);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取容器操作统计失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取用户的容器操作历史
     */
    @GetMapping("/user-history")
    public ResponseEntity<?> getUserContainerOperationHistory(
            @RequestParam(required = false) String targetUsername,
            @RequestParam(defaultValue = "50") int limit) {
        
        try {
            String currentUser = getCurrentUsername();
            
            // 确定查询目标用户
            String queryUsername = targetUsername != null ? targetUsername : currentUser;
            
            // 权限验证
            if (!containerPermissionService.canViewContainerAuditLogs(currentUser, queryUsername)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限查看该用户的操作历史")
                );
            }
            
            List<SystemLog> history = containerAuditService.getUserContainerOperationHistory(queryUsername, limit);
            
            return ResponseEntity.ok(Map.of(
                "username", queryUsername,
                "operationHistory", history,
                "totalCount", history.size(),
                "limit", limit
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取用户操作历史失败: " + e.getMessage())
            );
        }
    }

    /**
     * 查找可疑的容器操作
     */
    @GetMapping("/suspicious-operations")
    public ResponseEntity<?> findSuspiciousContainerOperations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证 - 只有管理员可以查看可疑操作
            if (!containerPermissionService.canManageContainerSync(currentUser)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限查看可疑操作")
                );
            }
            
            List<SystemLog> suspiciousOps = containerAuditService.findSuspiciousContainerOperations(since);
            
            return ResponseEntity.ok(Map.of(
                "suspiciousOperations", suspiciousOps,
                "totalCount", suspiciousOps.size(),
                "sinceTime", since != null ? since : LocalDateTime.now().minusHours(24)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "查找可疑操作失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取用户权限摘要
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> getUserPermissions(@RequestParam(required = false) String targetUsername) {
        try {
            String currentUser = getCurrentUsername();
            String queryUsername = targetUsername != null ? targetUsername : currentUser;
            
            // 权限验证 - 管理员可以查看任何人的权限，普通用户只能查看自己的
            if (!containerPermissionService.canViewContainerAuditLogs(currentUser, queryUsername)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限查看该用户的权限信息")
                );
            }
            
            Map<String, Object> permissions = containerPermissionService.getUserPermissionSummary(queryUsername);
            
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取用户权限信息失败: " + e.getMessage())
            );
        }
    }

    /**
     * 记录手动操作日志
     */
    @PostMapping("/log-operation")
    public ResponseEntity<Map<String, Object>> logOperation(@RequestBody Map<String, Object> request) {
        try {
            String currentUser = getCurrentUsername();
            String operation = (String) request.get("operation");
            String containerId = (String) request.get("containerId");
            String containerName = (String) request.get("containerName");
            String hostInfo = (String) request.get("hostInfo");
            String result = (String) request.get("result");
            
            containerAuditService.logContainerOperation(currentUser, operation, containerId, 
                                                      containerName, hostInfo, result);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "操作日志已记录"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "记录操作日志失败: " + e.getMessage())
            );
        }
    }

    /**
     * 清理过期审计日志
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldAuditLogs(
            @RequestParam(defaultValue = "90") int daysToKeep) {
        
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证 - 只有管理员可以清理日志
            if (!containerPermissionService.canManageContainerSync(currentUser)) {
                return ResponseEntity.status(403).body(
                    Map.of("success", false, "error", "无权限清理审计日志")
                );
            }
            
            int cleaned = containerAuditService.cleanupOldAuditLogs(daysToKeep);
            
            // 记录清理操作
            containerAuditService.logContainerOperation(currentUser, "CLEANUP_AUDIT_LOGS", 
                                                      "", "", "", 
                                                      String.format("清理了%d条%d天前的日志", cleaned, daysToKeep));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审计日志清理完成",
                "cleanedCount", cleaned,
                "daysToKeep", daysToKeep
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "error", "清理审计日志失败: " + e.getMessage())
            );
        }
    }

    /**
     * 导出审计日志
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1000") int maxRecords) {
        
        try {
            String currentUser = getCurrentUsername();
            
            // 权限验证
            if (!containerPermissionService.canViewContainerAuditLogs(currentUser, username)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "无权限导出审计日志")
                );
            }
            
            // 获取日志数据
            Page<SystemLog> logs = containerAuditService.getContainerOperationLogs(
                0, maxRecords, username, startTime, endTime);
            
            // 记录导出操作
            containerAuditService.logContainerOperation(currentUser, "EXPORT_AUDIT_LOGS", 
                                                      "", "", "", 
                                                      String.format("导出了%d条审计日志", logs.getContent().size()));
            
            return ResponseEntity.ok(Map.of(
                "exportData", logs.getContent(),
                "totalRecords", logs.getContent().size(),
                "exportTime", LocalDateTime.now(),
                "exportedBy", currentUser,
                "filters", Map.of(
                    "username", username,
                    "startTime", startTime,
                    "endTime", endTime,
                    "maxRecords", maxRecords
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "导出审计日志失败: " + e.getMessage())
            );
        }
    }
}