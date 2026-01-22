package org.cyberlab.task;

import org.cyberlab.service.EnhancedLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志归档定时任务
 *
 * 功能：
 * 1. 定期清理超过保留期限的日志
 * 2. 归档重要日志（可选）
 * 3. 数据库日志表空间管理
 *
 * 执行策略：
 * - 每天凌晨2点执行日志清理
 * - 每周日凌晨3点执行日志归档
 */
@Component
public class LogArchiveTask {

    private static final Logger log = LoggerFactory.getLogger(LogArchiveTask.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private EnhancedLogService logService;

    /**
     * 日志保留天数（默认90天）
     * 可以在 application.yml 中配置：
     * cyberlab.log.retention-days: 90
     */
    @Value("${cyberlab.log.retention-days:90}")
    private int retentionDays;

    /**
     * 是否启用日志归档（默认启用）
     */
    @Value("${cyberlab.log.archive-enabled:true}")
    private boolean archiveEnabled;

    /**
     * 定期清理旧日志
     * 每天凌晨2点执行
     * Cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldLogs() {
        if (logService == null) {
            log.warn("EnhancedLogService未注入，跳过日志清理任务");
            return;
        }

        if (!archiveEnabled) {
            log.info("ℹ️ 日志归档已禁用，跳过清理任务");
            return;
        }

        try {
            log.info("🗑️ 开始清理旧日志... (保留天数: {})", retentionDays);

            // 计算清理时间点
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            log.info("📅 清理时间点: {} ({}天前)", cutoffTime.format(formatter), retentionDays);

            // 执行清理
            int deletedCount = logService.cleanOldLogs(cutoffTime);

            if (deletedCount > 0) {
                log.info("日志清理完成！删除了 {} 条旧记录", deletedCount);
            } else {
                log.info("ℹ️ 没有需要清理的日志记录");
            }

        } catch (Exception e) {
            log.error("日志清理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定期归档日志（月度归档）
     * 每月1日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void archiveLogs() {
        if (logService == null) {
            log.warn("EnhancedLogService未注入，跳过日志归档任务");
            return;
        }

        if (!archiveEnabled) {
            log.info("ℹ️ 日志归档已禁用，跳过归档任务");
            return;
        }

        try {
            log.info("开始归档上月日志...");

            // 计算上个月的时间范围
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastMonthStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime lastMonthEnd = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);

            log.info("📅 归档时间范围: {} ~ {}",
                    lastMonthStart.format(formatter),
                    lastMonthEnd.format(formatter));

            // 执行归档（实际实现可以导出到文件、备份数据库等）
            logService.archiveLogs(lastMonthStart, lastMonthEnd);

            log.info("日志归档任务完成");

        } catch (Exception e) {
            log.error("日志归档失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定期清理重复日志（去重）
     * 每周日凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void deduplicateLogs() {
        if (logService == null) {
            log.warn("EnhancedLogService未注入，跳过日志去重任务");
            return;
        }

        try {
            log.info("🔄 开始日志去重任务...");

            // 去重逻辑：
            // 1. 相同 username + operation + description + timestamp(秒级) 的日志视为重复
            // 2. 保留第一条，删除后续重复记录
            // 注：实际实现需要复杂的SQL或分批查询，这里仅记录任务

            log.info("ℹ️ 日志去重任务执行完成（当前版本仅记录）");

        } catch (Exception e) {
            log.error("日志去重失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 日志统计报告（每周一早上8点）
     * 生成上周的日志统计报告
     */
    @Scheduled(cron = "0 0 8 ? * MON")
    public void generateWeeklyReport() {
        if (logService == null) {
            log.warn("EnhancedLogService未注入，跳过日志统计任务");
            return;
        }

        try {
            log.info("📊 开始生成上周日志统计报告...");

            // 计算上周时间范围
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastWeekStart = now.minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime lastWeekEnd = now.withHour(0).withMinute(0).withSecond(0).minusSeconds(1);

            // 统计各级别日志数量
            var levelStats = logService.countByLogLevel(lastWeekStart, lastWeekEnd);
            var categoryStats = logService.countByCategory(lastWeekStart, lastWeekEnd);

            // 输出统计报告
            log.info("📈 上周日志统计 ({} ~ {})",
                    lastWeekStart.format(formatter),
                    lastWeekEnd.format(formatter));

            log.info("   按级别统计:");
            levelStats.forEach((level, count) ->
                log.info("     - {}: {} 条", level, count));

            log.info("   按分类统计:");
            categoryStats.forEach((category, count) ->
                log.info("     - {}: {} 条", category, count));

            // 统计异常类型
            var exceptionStats = logService.getExceptionTypeStats(lastWeekStart, lastWeekEnd);
            if (!exceptionStats.isEmpty()) {
                log.info("   异常类型TOP5:");
                exceptionStats.entrySet().stream()
                        .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                        .limit(5)
                        .forEach(entry ->
                            log.info("     - {}: {} 次", entry.getKey(), entry.getValue()));
            }

            log.info("日志统计报告生成完成");

        } catch (Exception e) {
            log.error("生成日志统计报告失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发日志清理（用于测试或紧急清理）
     * 可通过管理接口调用
     */
    public void manualCleanup(int days) {
        if (logService == null) {
            throw new IllegalStateException("EnhancedLogService未注入");
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        int deletedCount = logService.cleanOldLogs(cutoffTime);
        log.info("手动清理完成！删除了 {} 条记录（{}天前）", deletedCount, days);
    }

    /**
     * 获取任务配置信息
     */
    public String getTaskInfo() {
        return String.format(
                "日志归档任务配置:\n" +
                "  - 保留天数: %d\n" +
                "  - 归档启用: %b\n" +
                "  - EnhancedLogService: %s",
                retentionDays,
                archiveEnabled,
                logService != null ? "已注入" : "未注入"
        );
    }
}
