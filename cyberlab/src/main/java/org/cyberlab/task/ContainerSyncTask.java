package org.cyberlab.task;

import org.cyberlab.service.ContainerStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 容器状态同步定时任务
 * 负责定期同步容器状态，确保目标状态与实际状态一致
 */
@Component
@ConditionalOnProperty(name = "cyberlab.container.sync.enabled", havingValue = "true", matchIfMissing = true)
public class ContainerSyncTask {

    private static final Logger logger = LoggerFactory.getLogger(ContainerSyncTask.class);

    @Autowired
    private ContainerStateService containerStateService;

    // 防止任务重叠执行
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    
    // 任务执行统计
    private LocalDateTime lastSyncTime;
    private Map<String, Object> lastSyncResult;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    /**
     * 主要同步任务 - 每5分钟执行一次（优化内存使用）
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void syncContainerStates() {
        if (!syncInProgress.compareAndSet(false, true)) {
            // Debug statement removed
            return;
        }

        try {
            // Debug statement removed
            
            // 检查连续失败次数
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                // Debug statement removed
                return;
            }
            
            // 执行同步
            Map<String, Object> result = containerStateService.syncNeedingReconciliation();
            
            lastSyncTime = LocalDateTime.now();
            lastSyncResult = result;
            
            int totalProcessed = (Integer) result.get("totalProcessed");
            int totalSynced = (Integer) result.get("totalSynced");
            int totalFailed = (Integer) result.get("totalFailed");
            
            if (totalFailed == 0) {
                consecutiveFailures = 0;
            } else if (totalSynced == 0 && totalFailed > 0) {
                consecutiveFailures++;
            }
            
            // Debug statement removed            
            // 如果有失败，记录详细信息
            if (totalFailed > 0) {
                // Debug statement removed
            }
            
        } catch (Exception e) {
            consecutiveFailures++;
            logger.error("容器状态同步失败", e);
        } finally {
            syncInProgress.set(false);
        }
    }

    /**
     * 统计任务 - 每30分钟执行一次（减少资源消耗）
     */
    @Scheduled(fixedRate = 1800000) // 30分钟 = 1800000毫秒
    public void logContainerStatistics() {
        try {
            Map<String, Object> stats = containerStateService.getContainerStateStatistics();
            
            // Debug statement removed
            // Debug statement removed
            // Debug statement removed
            // Debug statement removed
            // Debug statement removed
            // Debug statement removed
            
        } catch (Exception e) {
            // Debug statement removed
        }
    }

    /**
     * 清理任务 - 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldStates() {
        try {
            // Debug statement removed
            
            // 清理7天前的已同步状态
            int cleaned = containerStateService.cleanupOldStates(7);
            
            // Debug statement removed
            
        } catch (Exception e) {
            // Debug statement removed
        }
    }

    /**
     * 重置失败状态任务 - 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void resetFailedStates() {
        try {
            // Debug statement removed
            
            var resetStates = containerStateService.resetFailedStates();
            
            if (!resetStates.isEmpty()) {
                // Debug statement removed
            }
            
        } catch (Exception e) {
            // Debug statement removed
        }
    }

    /**
     * 健康检查 - 每5分钟执行一次（优化内存使用）
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void healthCheck() {
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            // Debug statement removed
        }
    }

    /**
     * 获取任务状态信息
     */
    public Map<String, Object> getTaskStatus() {
        return Map.of(
            "syncInProgress", syncInProgress.get(),
            "lastSyncTime", lastSyncTime,
            "lastSyncResult", lastSyncResult,
            "consecutiveFailures", consecutiveFailures,
            "maxConsecutiveFailures", MAX_CONSECUTIVE_FAILURES,
            "isHealthy", consecutiveFailures < MAX_CONSECUTIVE_FAILURES
        );
    }

    /**
     * 手动触发同步
     */
    public boolean triggerManualSync() {
        if (syncInProgress.get()) {
            return false; // 已在进行中
        }
        
        // 在新线程中执行，避免阻塞
        new Thread(() -> {
            try {
                syncContainerStates();
            } catch (Exception e) {
                // Debug statement removed
            }
        }).start();
        
        return true;
    }

    /**
     * 重置连续失败计数
     */
    public void resetFailureCount() {
        consecutiveFailures = 0;
        // Debug statement removed
    }
}