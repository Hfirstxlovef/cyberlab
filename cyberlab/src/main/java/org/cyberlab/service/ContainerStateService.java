package org.cyberlab.service;

import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.entity.ContainerState;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.HostNode;
import org.cyberlab.repository.ContainerStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 容器状态管理服务
 * 负责容器状态的同步、调和和监控
 */
@Service
public class ContainerStateService {

    @Autowired
    private ContainerStateRepository containerStateRepository;
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private HostNodeService hostNodeService;
    
    @Autowired
    private AssetService assetService;

    // 用于限流的计数器：主机ID -> 最后访问时间
    private final Map<Long, LocalDateTime> hostAccessMap = new ConcurrentHashMap<>();
    
    // 最小访问间隔（秒）
    private static final int MIN_ACCESS_INTERVAL = 30;
    
    // 缓存清理相关常量
    private static final int MAX_CACHE_SIZE = 100;
    private static final int CACHE_CLEANUP_HOURS = 24;

    /**
     * 创建或更新容器状态
     */
    @Transactional
    public ContainerState createOrUpdateContainerState(Long assetId, Long hostNodeId, 
                                                     String containerId, String desiredStatus, 
                                                     String createdBy) {
        Optional<ContainerState> existingOpt = containerStateRepository.findByAssetIdAndContainerId(assetId, containerId);
        
        ContainerState containerState;
        if (existingOpt.isPresent()) {
            containerState = existingOpt.get();
            containerState.setDesiredStatus(desiredStatus);
            containerState.setUpdatedAt(LocalDateTime.now());
        } else {
            containerState = new ContainerState(assetId, hostNodeId, containerId, desiredStatus);
            containerState.setCreatedBy(createdBy);
        }
        
        return containerStateRepository.save(containerState);
    }

    /**
     * 同步单个容器状态
     */
    @Transactional
    public boolean syncContainerState(ContainerState containerState) {
        try {
            containerState.startSync();
            containerStateRepository.save(containerState);
            
            // 获取容器实际状态
            ContainerInfo containerInfo = dockerService.getContainerInfo(containerState.getContainerId());
            
            if (containerInfo != null) {
                // 更新当前状态
                String currentStatus = mapContainerStatus(containerInfo.getStatus());
                containerState.setCurrentStatus(currentStatus);
                containerState.setHealthStatus(containerInfo.getHealthStatus());
                containerState.setContainerName(containerInfo.getName());
                containerState.setImageName(containerInfo.getImage());
                
                // 检查是否需要执行调和操作
                if (!containerState.getDesiredStatus().equals(currentStatus)) {
                    boolean reconciled = performReconciliation(containerState, containerInfo);
                    if (reconciled) {
                        containerState.syncSuccess();
                    } else {
                        containerState.syncFailed("状态调和失败");
                    }
                } else {
                    containerState.syncSuccess();
                }
            } else {
                containerState.syncFailed("容器不存在或无法访问");
            }
            
            containerStateRepository.save(containerState);
            return "SYNCED".equals(containerState.getSyncStatus());
            
        } catch (Exception e) {
            containerState.syncFailed("同步异常: " + e.getMessage());
            containerStateRepository.save(containerState);
            return false;
        }
    }

    /**
     * 执行状态调和
     */
    private boolean performReconciliation(ContainerState containerState, ContainerInfo containerInfo) {
        try {
            String desiredStatus = containerState.getDesiredStatus();
            String containerId = containerState.getContainerId();
            
            String result;
            switch (desiredStatus) {
                case "RUNNING":
                    if (containerInfo.getSimpleStatus().equals("已停止")) {
                        result = dockerService.startContainer(containerId);
                        return result.contains("已启动") || result.contains("启动成功");
                    }
                    break;
                    
                case "STOPPED":
                    if (containerInfo.getSimpleStatus().equals("运行中")) {
                        result = dockerService.stopContainer(containerId);
                        return result.contains("已停止") || result.contains("停止成功");
                    }
                    break;
                    
                case "RESTARTED":
                    result = dockerService.restartContainer(containerId);
                    return result.contains("重启成功") || result.contains("已重启");
                    
                default:
                    return false;
            }
            
            return true; // 已经是期望状态
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 批量同步需要调和的容器状态
     */
    @Transactional
    public Map<String, Object> syncNeedingReconciliation() {
        List<ContainerState> needingSync = containerStateRepository.findNeedingReconciliation();
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> syncResults = new ArrayList<>();
        
        // 按主机分组进行限流
        Map<Long, List<ContainerState>> groupedByHost = needingSync.stream()
            .collect(Collectors.groupingBy(ContainerState::getHostNodeId));
        
        int totalSynced = 0;
        int totalFailed = 0;
        
        for (Map.Entry<Long, List<ContainerState>> entry : groupedByHost.entrySet()) {
            Long hostNodeId = entry.getKey();
            List<ContainerState> hostContainers = entry.getValue();
            
            // 检查主机访问限流
            if (!canAccessHost(hostNodeId)) {
                continue;
            }
            
            // 检查主机是否在线
            boolean hostOnline = hostNodeService.testNodeConnection(hostNodeId);
            if (!hostOnline) {
                continue;
            }
            
            // 更新主机访问时间
            hostAccessMap.put(hostNodeId, LocalDateTime.now());
            
            // 同步该主机上的容器
            for (ContainerState containerState : hostContainers) {
                boolean success = syncContainerState(containerState);
                
                Map<String, Object> syncResult = new HashMap<>();
                syncResult.put("assetId", containerState.getAssetId());
                syncResult.put("containerId", containerState.getContainerId());
                syncResult.put("hostNodeId", hostNodeId);
                syncResult.put("success", success);
                syncResult.put("syncStatus", containerState.getSyncStatus());
                syncResult.put("desiredStatus", containerState.getDesiredStatus());
                syncResult.put("currentStatus", containerState.getCurrentStatus());
                
                if (!success) {
                    syncResult.put("error", containerState.getSyncError());
                    totalFailed++;
                } else {
                    totalSynced++;
                }
                
                syncResults.add(syncResult);
                
                // 避免对单个主机造成过大压力
                try {
                    Thread.sleep(500); // 500ms间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        result.put("totalProcessed", needingSync.size());
        result.put("totalSynced", totalSynced);
        result.put("totalFailed", totalFailed);
        result.put("syncResults", syncResults);
        result.put("syncTime", LocalDateTime.now());
        
        return result;
    }

    /**
     * 检查是否可以访问主机（限流）并执行缓存清理
     */
    private boolean canAccessHost(Long hostNodeId) {
        // 定期清理过期缓存
        cleanupExpiredCacheEntries();
        
        LocalDateTime lastAccess = hostAccessMap.get(hostNodeId);
        if (lastAccess == null) {
            return true;
        }
        
        return LocalDateTime.now().isAfter(lastAccess.plusSeconds(MIN_ACCESS_INTERVAL));
    }
    
    /**
     * 清理过期的缓存条目
     */
    private void cleanupExpiredCacheEntries() {
        if (hostAccessMap.size() > MAX_CACHE_SIZE) {
            LocalDateTime threshold = LocalDateTime.now().minusHours(CACHE_CLEANUP_HOURS);
            
            // 移除过期条目
            hostAccessMap.entrySet().removeIf(entry -> 
                entry.getValue().isBefore(threshold)
            );
            
            // 如果还是太大，保留最近的条目
            if (hostAccessMap.size() > MAX_CACHE_SIZE) {
                List<Map.Entry<Long, LocalDateTime>> sortedEntries = hostAccessMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.<Long, LocalDateTime>comparingByValue().reversed())
                    .limit(MAX_CACHE_SIZE / 2) // 保留一半最新的
                    .collect(Collectors.toList());
                
                hostAccessMap.clear();
                sortedEntries.forEach(entry -> 
                    hostAccessMap.put(entry.getKey(), entry.getValue())
                );
                
                // Debug statement removed
            }
        }
    }

    /**
     * 映射容器状态
     */
    private String mapContainerStatus(String dockerStatus) {
        if (dockerStatus == null) return "UNKNOWN";
        
        String lower = dockerStatus.toLowerCase();
        if (lower.contains("running") || lower.contains("up")) {
            return "RUNNING";
        } else if (lower.contains("exited") || lower.contains("stopped")) {
            return "STOPPED";
        } else if (lower.contains("paused")) {
            return "PAUSED";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * 获取容器状态统计
     */
    public Map<String, Object> getContainerStateStatistics() {
        List<Object[]> syncStats = containerStateRepository.countBySyncStatus();
        List<Object[]> healthStats = containerStateRepository.countByHealthStatus();
        
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Long> syncStatusMap = new HashMap<>();
        for (Object[] stat : syncStats) {
            syncStatusMap.put((String) stat[0], (Long) stat[1]);
        }
        
        Map<String, Long> healthStatusMap = new HashMap<>();
        for (Object[] stat : healthStats) {
            healthStatusMap.put((String) stat[0], (Long) stat[1]);
        }
        
        result.put("syncStatusStats", syncStatusMap);
        result.put("healthStatusStats", healthStatusMap);
        result.put("totalStates", containerStateRepository.count());
        result.put("needingReconciliation", containerStateRepository.findNeedingReconciliation().size());
        result.put("failedSyncs", containerStateRepository.findFailedSyncs().size());
        
        return result;
    }

    /**
     * 清理过期的容器状态记录
     */
    @Transactional
    public int cleanupOldStates(int daysToKeep) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
        
        // 删除旧的已同步状态
        List<ContainerState> oldStates = containerStateRepository.findAll().stream()
            .filter(cs -> "SYNCED".equals(cs.getSyncStatus()) && 
                         cs.getUpdatedAt() != null && 
                         cs.getUpdatedAt().isBefore(threshold))
            .collect(Collectors.toList());
        
        containerStateRepository.deleteAll(oldStates);
        return oldStates.size();
    }

    /**
     * 重置失败的容器状态
     */
    @Transactional
    public List<ContainerState> resetFailedStates() {
        List<ContainerState> failedStates = containerStateRepository.findFailedSyncs();
        
        for (ContainerState state : failedStates) {
            state.resetSync();
        }
        
        containerStateRepository.saveAll(failedStates);
        return failedStates;
    }

    /**
     * 根据资产ID获取容器状态
     */
    public List<ContainerState> getContainerStatesByAsset(Long assetId) {
        return containerStateRepository.findByAssetId(assetId);
    }

    /**
     * 获取指定主机的容器状态
     */
    public List<ContainerState> getContainerStatesByHost(Long hostNodeId) {
        return containerStateRepository.findByHostNodeId(hostNodeId);
    }

    /**
     * 强制同步指定资产的所有容器
     */
    @Transactional
    public Map<String, Object> forceSyncAssetContainers(Long assetId, String operatorBy) {
        List<ContainerState> assetStates = containerStateRepository.findByAssetId(assetId);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> syncResults = new ArrayList<>();
        
        int successCount = 0;
        int failureCount = 0;
        
        for (ContainerState state : assetStates) {
            // 重置状态以强制同步
            state.resetSync();
            boolean success = syncContainerState(state);
            
            Map<String, Object> syncResult = new HashMap<>();
            syncResult.put("containerId", state.getContainerId());
            syncResult.put("success", success);
            syncResult.put("syncStatus", state.getSyncStatus());
            
            if (success) {
                successCount++;
            } else {
                failureCount++;
                syncResult.put("error", state.getSyncError());
            }
            
            syncResults.add(syncResult);
        }
        
        result.put("assetId", assetId);
        result.put("totalContainers", assetStates.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("syncResults", syncResults);
        result.put("operatorBy", operatorBy);
        result.put("syncTime", LocalDateTime.now());
        
        return result;
    }
}