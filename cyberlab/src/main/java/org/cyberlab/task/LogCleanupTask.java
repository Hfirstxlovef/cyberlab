package org.cyberlab.task;

import org.cyberlab.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LogCleanupTask {

    @Autowired
    private SystemLogRepository logRepository;

    // 每天凌晨3点执行清理，删除 3 个月之前的日志
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldLogs() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        int deleted = logRepository.deleteByTimestampBefore(threeMonthsAgo);
        // Debug statement removed
    }
}
