package org.cyberlab.service;

import org.cyberlab.entity.SystemLog;
import org.cyberlab.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 容器操作审计服务
 * 记录和查询容器相关的操作日志
 */
@Service
public class ContainerAuditService {

    @Autowired
    private SystemLogRepository systemLogRepository;

    /**
     * 记录容器操作日志
     */
    public void logContainerOperation(String username, String operation, String containerId, 
                                    String containerName, String hostInfo, String result) {
        try {
            SystemLog log = new SystemLog();
            log.setUsername(username != null ? username : "system");
            log.setOperation("CONTAINER_" + operation.toUpperCase());
            
            String description = String.format(
                "容器操作: %s | 容器ID: %s | 容器名: %s | 主机: %s | 结果: %s",
                operation, containerId, containerName, hostInfo, result
            );
            log.setDescription(description);
            log.setIp(getClientIpAddress());
            log.setTimestamp(LocalDateTime.now());
            
            systemLogRepository.save(log);
        } catch (Exception e) {
            // 记录容器操作日志失败
        }
    }

    /**
     * 记录批量容器操作日志
     */
    public void logBatchContainerOperation(String username, String operation, 
                                         List<String> containerIds, String result) {
        try {
            SystemLog log = new SystemLog();
            log.setUsername(username != null ? username : "system");
            log.setOperation("CONTAINER_BATCH_" + operation.toUpperCase());
            
            String description = String.format(
                "批量容器操作: %s | 容器数量: %d | 容器列表: %s | 结果: %s",
                operation, containerIds.size(), String.join(",", containerIds), result
            );
            log.setDescription(description);
            log.setIp(getClientIpAddress());
            log.setTimestamp(LocalDateTime.now());
            
            systemLogRepository.save(log);
        } catch (Exception e) {
            // 记录批量容器操作日志失败
        }
    }

    /**
     * 记录容器状态同步日志
     */
    public void logContainerSync(String username, String syncType, Map<String, Object> syncResult) {
        try {
            SystemLog log = new SystemLog();
            log.setUsername(username != null ? username : "system");
            log.setOperation("CONTAINER_SYNC_" + syncType.toUpperCase());
            
            String description = String.format(
                "容器同步: %s | 处理数量: %s | 成功数量: %s | 失败数量: %s",
                syncType, 
                syncResult.get("totalProcessed"),
                syncResult.get("totalSynced"),
                syncResult.get("totalFailed")
            );
            log.setDescription(description);
            log.setIp(getClientIpAddress());
            log.setTimestamp(LocalDateTime.now());
            
            systemLogRepository.save(log);
        } catch (Exception e) {
            // 记录容器同步日志失败
        }
    }

    /**
     * 记录资产容器操作日志
     */
    public void logAssetContainerOperation(String username, String operation, Long assetId, 
                                         String assetName, String containerId, String result) {
        try {
            SystemLog log = new SystemLog();
            log.setUsername(username != null ? username : "system");
            log.setOperation("ASSET_CONTAINER_" + operation.toUpperCase());
            
            String description = String.format(
                "资产容器操作: %s | 资产ID: %d | 资产名: %s | 容器ID: %s | 结果: %s",
                operation, assetId, assetName, containerId, result
            );
            log.setDescription(description);
            log.setIp(getClientIpAddress());
            log.setTimestamp(LocalDateTime.now());
            
            systemLogRepository.save(log);
        } catch (Exception e) {
            // 记录资产容器操作日志失败
        }
    }

    /**
     * 记录权限验证日志
     */
    public void logPermissionCheck(String username, String operation, String resource, 
                                 boolean allowed, String reason) {
        try {
            SystemLog log = new SystemLog();
            log.setUsername(username != null ? username : "anonymous");
            log.setOperation("PERMISSION_CHECK");
            
            String description = String.format(
                "权限检查: %s | 资源: %s | 结果: %s | 原因: %s",
                operation, resource, allowed ? "允许" : "拒绝", reason
            );
            log.setDescription(description);
            log.setIp(getClientIpAddress());
            log.setTimestamp(LocalDateTime.now());
            
            systemLogRepository.save(log);
        } catch (Exception e) {
            // 记录权限检查日志失败
        }
    }

    /**
     * 查询容器操作日志
     */
    public Page<SystemLog> getContainerOperationLogs(int page, int size, String username, 
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        if (username != null && !username.trim().isEmpty()) {
            if (startTime != null && endTime != null) {
                return systemLogRepository.findByUsernameAndOperationContainingAndTimestampBetween(
                    username, "CONTAINER", startTime, endTime, pageable);
            } else {
                return systemLogRepository.findByUsernameAndOperationContaining(
                    username, "CONTAINER", pageable);
            }
        } else {
            if (startTime != null && endTime != null) {
                return systemLogRepository.findByOperationContainingAndTimestampBetween(
                    "CONTAINER", startTime, endTime, pageable);
            } else {
                return systemLogRepository.findByOperationContaining("CONTAINER", pageable);
            }
        }
    }

    /**
     * 获取容器操作统计
     */
    public Map<String, Object> getContainerOperationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取时间范围内的所有容器操作日志
            List<SystemLog> logs;
            if (startTime != null && endTime != null) {
                logs = systemLogRepository.findByOperationContainingAndTimestampBetween(
                    "CONTAINER", startTime, endTime);
            } else {
                logs = systemLogRepository.findByOperationContaining("CONTAINER");
            }
            
            // 统计操作类型
            Map<String, Long> operationStats = new HashMap<>();
            Map<String, Long> userStats = new HashMap<>();
            Map<String, Long> hourlyStats = new HashMap<>();
            
            for (SystemLog log : logs) {
                // 操作类型统计
                String operation = log.getOperation();
                operationStats.put(operation, operationStats.getOrDefault(operation, 0L) + 1);
                
                // 用户统计
                String user = log.getUsername();
                userStats.put(user, userStats.getOrDefault(user, 0L) + 1);
                
                // 小时统计
                if (log.getTimestamp() != null) {
                    String hour = log.getTimestamp().getHour() + ":00";
                    hourlyStats.put(hour, hourlyStats.getOrDefault(hour, 0L) + 1);
                }
            }
            
            stats.put("totalOperations", logs.size());
            stats.put("operationTypeStats", operationStats);
            stats.put("userStats", userStats);
            stats.put("hourlyStats", hourlyStats);
            stats.put("timeRange", Map.of(
                "startTime", startTime,
                "endTime", endTime
            ));
            
        } catch (Exception e) {
            // 获取容器操作统计失败
            stats.put("error", "统计数据获取失败: " + e.getMessage());
        }
        
        return stats;
    }

    /**
     * 获取用户的容器操作历史
     */
    public List<SystemLog> getUserContainerOperationHistory(String username, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        return systemLogRepository.findByUsernameAndOperationContaining(username, "CONTAINER", pageable)
            .getContent();
    }

    /**
     * 查找可疑的容器操作
     */
    public List<SystemLog> findSuspiciousContainerOperations(LocalDateTime since) {
        try {
            // 查找频繁操作、批量操作等可疑行为
            LocalDateTime timeThreshold = since != null ? since : LocalDateTime.now().minusHours(24);
            
            return systemLogRepository.findByOperationContainingAndTimestampAfter("CONTAINER", timeThreshold)
                .stream()
                .filter(log -> {
                    // 这里可以添加更复杂的可疑行为检测逻辑
                    String desc = log.getDescription();
                    return desc != null && (
                        desc.contains("失败") || 
                        desc.contains("异常") ||
                        desc.contains("BATCH") && desc.contains("数量: [1-9]\\d+") // 大批量操作
                    );
                })
                .toList();
        } catch (Exception e) {
            // 查找可疑容器操作失败
            return List.of();
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "system";
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 清理过期审计日志
     */
    public int cleanupOldAuditLogs(int daysToKeep) {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
            List<SystemLog> oldLogs = systemLogRepository.findByOperationContainingAndTimestampBefore(
                "CONTAINER", threshold);
            
            systemLogRepository.deleteAll(oldLogs);
            return oldLogs.size();
        } catch (Exception e) {
            // 清理过期审计日志失败
            return 0;
        }
    }
}