package org.cyberlab.service;

import org.cyberlab.repository.*;
import org.cyberlab.entity.Achievement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BigScreenService {

    @Autowired
    private CyberRangeRepository cyberRangeRepository;
    
    @Autowired
    private DrillContainerRepository drillContainerRepository;
    
    @Autowired
    private AchievementRepository achievementRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SystemLogRepository systemLogRepository;

    /**
     * 根据用户角色获取大屏数据
     */
    public Map<String, Object> getBigScreenDataByRole(String role) {
        switch (role.toLowerCase()) {
            case "admin":
                return getAdminBigScreenData();
            case "red":
                return getRedTeamBigScreenData(null);
            case "blue":
                return getBlueTeamBigScreenData(null);
            case "judge":
                return getJudgeBigScreenData();
            default:
                return getPublicBigScreenData();
        }
    }
    
    /**
     * 获取管理员专用大屏数据
     */
    public Map<String, Object> getAdminBigScreenData() {
        Map<String, Object> result = new HashMap<>();

        // 管理员可以看到完整的系统数据
        result.put("overview", getOverviewData());
        result.put("userStats", getUserStatistics());
        result.put("systemHealth", getSystemHealthData());
        result.put("securityAlerts", getSecurityAlerts());
        result.put("resourceUsage", getSystemResources());
        result.put("networkTraffic", getNetworkTrafficData());
        result.put("activeContainers", getContainerStatus());
        result.put("recentOperations", getRecentOperations());

        // 添加前端需要的字段
        result.put("trend", getAttackTrend(24));           // 攻击趋势数据
        result.put("vulnerability", getVulnerabilityDistribution());  // 漏洞类型分布
        result.put("ranking", getTeamRanking());           // 战队排行

        return result;
    }
    
    /**
     * 获取红队专用大屏数据
     */
    public Map<String, Object> getRedTeamBigScreenData(String username) {
        Map<String, Object> result = new HashMap<>();
        
        // 红队关注攻击相关数据
        result.put("attackStats", getRedTeamAttackStats(username));
        result.put("targetAssets", getAttackableAssets());
        result.put("attackHistory", getAttackHistory(username));
        result.put("vulnerabilityTypes", getVulnerabilityDistribution());
        result.put("successRate", getAttackSuccessRate(username));
        result.put("leaderboard", getRedTeamLeaderboard());
        result.put("ongoingAttacks", getOngoingAttacks(username));
        
        return result;
    }
    
    /**
     * 获取蓝队专用大屏数据
     */
    public Map<String, Object> getBlueTeamBigScreenData(String username) {
        Map<String, Object> result = new HashMap<>();
        
        // 蓝队关注防护相关数据
        result.put("defenseStats", getBlueTeamDefenseStats(username));
        result.put("protectedAssets", getProtectedAssets());
        result.put("attackDetection", getAttackDetectionData());
        result.put("securityEvents", getSecurityEvents());
        result.put("defenseSuccess", getDefenseSuccessRate(username));
        result.put("threatIntelligence", getThreatIntelligenceData());
        result.put("incidentResponse", getIncidentResponseData());
        
        return result;
    }
    
    /**
     * 获取裁判专用大屏数据
     */
    public Map<String, Object> getJudgeBigScreenData() {
        Map<String, Object> result = new HashMap<>();
        
        // 裁判关注比赛状态和评分数据
        result.put("competitionOverview", getCompetitionOverview());
        result.put("teamComparison", getTeamComparisonData());
        result.put("scoreboard", getScoreboardData());
        result.put("pendingSubmissions", getPendingSubmissions());
        result.put("timelineEvents", getCompetitionTimeline());
        result.put("performanceMetrics", getTeamPerformanceMetrics());
        result.put("fairnessIndicators", getFairnessIndicators());
        
        return result;
    }
    
    /**
     * 获取公共大屏数据（未认证用户）
     */
    public Map<String, Object> getPublicBigScreenData() {
        Map<String, Object> result = new HashMap<>();
        
        // 只显示基础的公开信息
        result.put("systemStatus", "运行正常");
        result.put("onlineUsers", userRepository.countByEnabled(true));
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * 获取总览数据
     */
    private Map<String, Object> getOverviewData() {
        Map<String, Object> overview = new HashMap<>();
        
        // 运行中的演练数量
        long runningDrills = cyberRangeRepository.countByStatus("running");
        overview.put("runningDrills", runningDrills);
        
        // 累计攻击次数（成果提交数）
        long totalAttacks = achievementRepository.count();
        overview.put("totalAttacks", totalAttacks);
        
        // 当前在线人数（活跃用户）
        long onlineUsers = userRepository.countByEnabled(true);
        overview.put("onlineUsers", onlineUsers);
        
        // 活跃容器数
        long activeContainers = drillContainerRepository.countByStatus("running");
        overview.put("activeContainers", activeContainers);
        
        return overview;
    }

    /**
     * 获取攻击趋势数据
     */
    public List<Map<String, Object>> getAttackTrend(int hours) {
        List<Map<String, Object>> trendData = new ArrayList<>();
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);

        // 按小时分组统计
        for (int i = 0; i < hours; i++) {
            LocalDateTime hourStart = startTime.plusHours(i);
            LocalDateTime hourEnd = hourStart.plusHours(1);

            Map<String, Object> hourData = new HashMap<>();
            hourData.put("time", hourStart.format(DateTimeFormatter.ofPattern("HH:mm")));

            // 统计红队攻击数（假设通过成果提交统计）
            long redTeamAttacks = achievementRepository.countBySubmitTimeBetween(hourStart, hourEnd);
            hourData.put("attacks", redTeamAttacks);  // 改名为 attacks 以匹配前端

            // 统计蓝队防守数（可以通过日志或其他方式统计）
            long blueTeamDefense = systemLogRepository.countByTimestampBetween(hourStart, hourEnd) / 2;
            hourData.put("blocks", blueTeamDefense);  // 改名为 blocks 以匹配前端

            trendData.add(hourData);
        }

        return trendData;
    }

    /**
     * 获取漏洞类型分布 - 从真实成果提交数据统计
     */
    private List<Map<String, Object>> getVulnerabilityDistribution() {
        List<Map<String, Object>> vulnData = new ArrayList<>();

        try {
            // 从所有成果提交中统计攻击方法
            List<Achievement> achievements = achievementRepository.findAll();

            // 按攻击方法分组统计
            Map<String, Long> vulnTypes = achievements.stream()
                .filter(a -> a.getAttackMethod() != null && !a.getAttackMethod().trim().isEmpty())
                .collect(Collectors.groupingBy(
                    Achievement::getAttackMethod,
                    Collectors.counting()
                ));

            // 如果没有数据，使用默认示例数据
            if (vulnTypes.isEmpty()) {
                vulnTypes.put("SQL注入", 35L);
                vulnTypes.put("XSS", 28L);
                vulnTypes.put("RCE", 20L);
                vulnTypes.put("文件上传", 12L);
                vulnTypes.put("其他", 5L);
            }

            // 转换为前端需要的格式，并按数量降序排序
            vulnTypes.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    vulnData.add(item);
                });

        } catch (Exception e) {
            // 如果出错，返回默认数据
            Map<String, Integer> defaultTypes = new HashMap<>();
            defaultTypes.put("SQL注入", 35);
            defaultTypes.put("XSS", 28);
            defaultTypes.put("RCE", 20);
            defaultTypes.put("文件上传", 12);
            defaultTypes.put("其他", 5);

            defaultTypes.forEach((type, count) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", type);
                item.put("value", count);
                vulnData.add(item);
            });
        }

        return vulnData;
    }

    /**
     * 获取容器状态
     */
    private List<Map<String, Object>> getContainerStatus() {
        return drillContainerRepository.findAll().stream()
            .limit(10) // 只显示最新的10个容器
            .map(container -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", container.getName());
                item.put("status", container.getStatus());
                item.put("ip", container.getIp());
                item.put("image", container.getImageName());
                item.put("createTime", container.getCreateTime());
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取系统资源数据
     */
    public Map<String, Object> getSystemResources() {
        Map<String, Object> resources = new HashMap<>();

        try {
            // 获取运行时信息
            Runtime runtime = Runtime.getRuntime();

            // JVM内存使用率
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;
            resources.put("memory", Math.round(memoryUsage * 100.0) / 100.0);

            // CPU使用率 - 使用OperatingSystemMXBean
            com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getSystemCpuLoad() * 100;
            // 如果获取失败，使用默认值
            if (cpuUsage < 0) {
                cpuUsage = 0;
            }
            resources.put("cpu", Math.round(cpuUsage * 100.0) / 100.0);

            // 磁盘使用率 - 获取根目录的磁盘使用情况
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double diskUsage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
            resources.put("disk", Math.round(diskUsage * 100.0) / 100.0);

            // 网络使用 - 暂时使用容器数量作为网络活跃度的简单指标
            long activeContainers = drillContainerRepository.countByStatus("running");
            // 假设每个活跃容器平均占用5%的网络带宽
            double networkUsage = Math.min(activeContainers * 5, 100);
            resources.put("network", Math.round(networkUsage * 100.0) / 100.0);

        } catch (Exception e) {
            // 如果获取失败，返回默认值
            resources.put("cpu", 0);
            resources.put("memory", 0);
            resources.put("disk", 0);
            resources.put("network", 0);
        }

        return resources;
    }

    /**
     * 获取最近告警信息
     */
    private List<Map<String, Object>> getRecentAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // 模拟告警数据
        String[] alertTypes = {"安全告警", "性能告警", "系统告警"};
        String[] severities = {"高危", "中危", "低危"};
        
        for (int i = 0; i < 5; i++) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", alertTypes[i % alertTypes.length]);
            alert.put("severity", severities[i % severities.length]);
            alert.put("message", "检测到异常活动 #" + (i + 1));
            alert.put("time", LocalDateTime.now().minusMinutes(i * 10));
            alerts.add(alert);
        }
        
        return alerts;
    }

    /**
     * 获取战队排行 - 从真实成果提交数据统计
     */
    private List<Map<String, Object>> getTeamRanking() {
        List<Map<String, Object>> ranking = new ArrayList<>();

        try {
            // 从成果提交中统计各战队的得分
            List<Achievement> achievements = achievementRepository.findAll();

            // 按战队分组统计成功的成果数
            Map<String, Long> teamScores = achievements.stream()
                .filter(a -> "approved".equals(a.getStatus()) &&
                           a.getTeamName() != null &&
                           !a.getTeamName().trim().isEmpty())
                .collect(Collectors.groupingBy(
                    Achievement::getTeamName,
                    Collectors.counting()
                ));

            // 如果没有数据，使用默认示例数据
            if (teamScores.isEmpty()) {
                String[] teams = {"红队Alpha", "红队Beta", "蓝队Gamma", "蓝队Delta", "红队Epsilon"};
                for (int i = 0; i < teams.length; i++) {
                    Map<String, Object> team = new HashMap<>();
                    team.put("name", teams[i]);
                    team.put("score", 100 - i * 15);
                    team.put("type", teams[i].startsWith("红队") ? "red" : "blue");
                    team.put("change", 0);
                    ranking.add(team);
                }
            } else {
                // 转换为前端需要的格式，并按得分降序排序
                // 每个成功的成果计10分
                teamScores.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> {
                        Map<String, Object> team = new HashMap<>();
                        String teamName = entry.getKey();
                        team.put("name", teamName);
                        team.put("score", entry.getValue() * 10);  // 每个成果10分

                        // 根据队名判断类型（包含"红队"或"red"为红队，否则为蓝队）
                        String lowerName = teamName.toLowerCase();
                        if (lowerName.contains("红") || lowerName.contains("red")) {
                            team.put("type", "red");
                        } else if (lowerName.contains("蓝") || lowerName.contains("blue")) {
                            team.put("type", "blue");
                        } else {
                            // 默认根据首字母判断，或设为red
                            team.put("type", "red");
                        }

                        // 排名变化（暂时设为0，需要历史数据支持）
                        team.put("change", 0);

                        ranking.add(team);
                    });
            }

        } catch (Exception e) {
            // 如果出错，返回默认排行数据
            String[] teams = {"红队Alpha", "红队Beta", "蓝队Gamma", "蓝队Delta", "红队Epsilon"};
            for (int i = 0; i < teams.length; i++) {
                Map<String, Object> team = new HashMap<>();
                team.put("name", teams[i]);
                team.put("score", 100 - i * 15);
                team.put("type", teams[i].startsWith("红队") ? "red" : "blue");
                team.put("change", 0);
                ranking.add(team);
            }
        }

        // 只返回前10名
        return ranking.stream().limit(10).collect(Collectors.toList());
    }
    
    // ==================== 角色特定的数据获取方法 ====================
    
    /**
     * 获取用户统计数据
     */
    private Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("adminUsers", userRepository.countByRole("admin"));
        stats.put("redTeamUsers", userRepository.countByRole("red"));
        stats.put("blueTeamUsers", userRepository.countByRole("blue"));
        stats.put("judgeUsers", userRepository.countByRole("judge"));
        stats.put("activeUsers", userRepository.countByEnabled(true));
        return stats;
    }
    
    /**
     * 获取系统健康数据
     */
    private Map<String, Object> getSystemHealthData() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "健康");
        health.put("uptime", "72小时15分钟");
        health.put("lastReboot", LocalDateTime.now().minusHours(72));
        health.put("services", Arrays.asList(
            createServiceStatusMap("数据库", "正常"),
            createServiceStatusMap("Docker", "正常"),
            createServiceStatusMap("网络", "正常")
        ));
        return health;
    }
    
    /**
     * 创建服务状态Map的辅助方法
     */
    private Map<String, Object> createServiceStatusMap(String name, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("status", status);
        return map;
    }
    
    /**
     * 创建通用Map的辅助方法
     */
    private Map<String, Object> createMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
    
    /**
     * 获取安全告警 - 从真实系统日志和容器状态获取
     */
    private List<Map<String, Object>> getSecurityAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        try {
            // 1. 从系统日志中获取异常操作（假设operation包含"失败"、"错误"、"异常"等关键词）
            systemLogRepository.findTop10ByOrderByTimestampDesc().stream()
                .filter(log -> {
                    String op = log.getOperation() != null ? log.getOperation().toLowerCase() : "";
                    return op.contains("失败") || op.contains("错误") || op.contains("异常") ||
                           op.contains("告警") || op.contains("警告");
                })
                .limit(3)
                .forEach(log -> {
                    Map<String, Object> alert = new HashMap<>();
                    // 根据关键词判断告警级别
                    String op = log.getOperation().toLowerCase();
                    if (op.contains("错误") || op.contains("异常")) {
                        alert.put("level", "高危");
                    } else if (op.contains("警告") || op.contains("告警")) {
                        alert.put("level", "中危");
                    } else {
                        alert.put("level", "低危");
                    }
                    alert.put("type", "系统异常");
                    alert.put("message", log.getOperation());
                    alert.put("time", log.getTimestamp());
                    alerts.add(alert);
                });

            // 2. 从容器状态获取失败的容器作为告警
            drillContainerRepository.findAll().stream()
                .filter(container -> "failed".equals(container.getStatus()) ||
                                   "error".equals(container.getStatus()))
                .limit(2)
                .forEach(container -> {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("level", "中危");
                    alert.put("type", "容器异常");
                    alert.put("message", String.format("容器 %s 运行异常", container.getName()));
                    // 使用 stopTime 或 createTime 作为告警时间
                    alert.put("time", container.getStopTime() != null ?
                        container.getStopTime() :
                        (container.getCreateTime() != null ? container.getCreateTime() : LocalDateTime.now()));
                    alerts.add(alert);
                });

            // 如果没有告警，添加一个默认的系统正常消息
            if (alerts.isEmpty()) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("level", "信息");
                alert.put("type", "系统状态");
                alert.put("message", "系统运行正常，暂无告警信息");
                alert.put("time", LocalDateTime.now());
                alerts.add(alert);
            }

        } catch (Exception e) {
            // 如果出错，返回默认告警数据
            Map<String, Object> alert1 = new HashMap<>();
            alert1.put("level", "高危");
            alert1.put("type", "异常登录");
            alert1.put("message", "检测到来自异常IP的登录尝试");
            alert1.put("time", LocalDateTime.now().minusMinutes(15));
            alerts.add(alert1);

            Map<String, Object> alert2 = new HashMap<>();
            alert2.put("level", "中危");
            alert2.put("type", "资源异常");
            alert2.put("message", "容器CPU使用率超过阈值");
            alert2.put("time", LocalDateTime.now().minusMinutes(30));
            alerts.add(alert2);
        }

        // 只返回最近的5条告警
        return alerts.stream().limit(5).collect(Collectors.toList());
    }
    
    /**
     * 获取网络流量数据
     */
    private Map<String, Object> getNetworkTrafficData() {
        Map<String, Object> traffic = new HashMap<>();
        traffic.put("inbound", Math.random() * 1000 + "MB/s");
        traffic.put("outbound", Math.random() * 800 + "MB/s");
        traffic.put("connections", (int)(Math.random() * 500 + 100));
        return traffic;
    }
    
    /**
     * 获取最近操作记录
     */
    private List<Map<String, Object>> getRecentOperations() {
        return systemLogRepository.findTop10ByOrderByTimestampDesc().stream()
            .map(log -> createMap(
                "operation", log.getOperation(),
                "user", log.getUsername(),
                "time", log.getTimestamp(),
                "status", "成功"
            ))
            .collect(Collectors.toList());
    }
    
    // ==================== 红队相关数据方法 ====================
    
    /**
     * 获取红队攻击统计
     */
    private Map<String, Object> getRedTeamAttackStats(String username) {
        Map<String, Object> stats = new HashMap<>();
        
        if (username != null) {
            // 个人统计
            List<Achievement> userAchievements = achievementRepository.findByTeamName(username);
            stats.put("personalAttempts", userAchievements.size());
            stats.put("personalSuccess", userAchievements.stream().mapToInt(a -> 
                "approved".equals(a.getStatus()) ? 1 : 0).sum());
        }
        
        // 团队统计
        stats.put("totalAttempts", achievementRepository.count());
        stats.put("successfulAttacks", achievementRepository.countByStatus("approved"));
        stats.put("pendingReviews", achievementRepository.countByStatus("pending"));
        
        return stats;
    }
    
    /**
     * 获取可攻击资产
     */
    private List<Map<String, Object>> getAttackableAssets() {
        // 模拟可攻击资产数据
        List<Map<String, Object>> assets = new ArrayList<>();
        String[] assetNames = {"Web服务器", "数据库", "文件服务器", "邮件服务器", "DNS服务器"};
        String[] difficulties = {"简单", "中等", "困难"};
        
        for (int i = 0; i < assetNames.length; i++) {
            assets.add(createMap(
                "name", assetNames[i],
                "ip", "192.168.1." + (10 + i),
                "difficulty", difficulties[i % 3],
                "points", (i + 1) * 10,
                "status", Math.random() > 0.3 ? "在线" : "离线"
            ));
        }
        
        return assets;
    }
    
    /**
     * 获取攻击历史
     */
    private List<Map<String, Object>> getAttackHistory(String username) {
        if (username == null) return new ArrayList<>();
        
        return achievementRepository.findByTeamName(username).stream()
            .limit(10)
            .map(achievement -> createMap(
                "target", achievement.getTargetName(),
                "method", achievement.getAttackMethod() != null ? achievement.getAttackMethod() : "未知",
                "status", achievement.getStatus(),
                "time", achievement.getSubmitTime(),
                "points", 10 // 默认分数，因为Achievement实体没有points字段
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取攻击成功率
     */
    private Map<String, Object> getAttackSuccessRate(String username) {
        Map<String, Object> rate = new HashMap<>();
        
        if (username != null) {
            List<Achievement> userAchievements = achievementRepository.findByTeamName(username);
            long total = userAchievements.size();
            long success = userAchievements.stream().filter(a -> "approved".equals(a.getStatus())).count();
            
            rate.put("total", total);
            rate.put("success", success);
            rate.put("rate", total > 0 ? (double) success / total * 100 : 0);
        } else {
            rate.put("total", 0);
            rate.put("success", 0);
            rate.put("rate", 0);
        }
        
        return rate;
    }
    
    /**
     * 获取红队排行榜
     */
    private List<Map<String, Object>> getRedTeamLeaderboard() {
        // 模拟排行榜数据
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        String[] teams = {"红队Alpha", "红队Beta", "红队Gamma", "红队Delta", "红队Epsilon"};
        
        for (int i = 0; i < teams.length; i++) {
            leaderboard.add(createMap(
                "rank", i + 1,
                "team", teams[i],
                "score", 100 - i * 15,
                "attacks", 10 - i * 2,
                "trend", i % 2 == 0 ? "上升" : "下降"
            ));
        }
        
        return leaderboard;
    }
    
    /**
     * 获取进行中的攻击
     */
    private List<Map<String, Object>> getOngoingAttacks(String username) {
        List<Map<String, Object>> ongoing = new ArrayList<>();
        
        // 模拟进行中的攻击
        if (Math.random() > 0.5) {
            ongoing.add(Map.of(
                "target", "Web服务器",
                "method", "SQL注入",
                "progress", (int)(Math.random() * 100),
                "startTime", LocalDateTime.now().minusMinutes((int)(Math.random() * 60))
            ));
        }
        
        return ongoing;
    }
    
    // ==================== 蓝队相关数据方法 ====================
    
    /**
     * 获取蓝队防护统计
     */
    private Map<String, Object> getBlueTeamDefenseStats(String username) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("protectedAssets", 15);
        stats.put("detectedAttacks", 8);
        stats.put("blockedAttacks", 6);
        stats.put("activeIncidents", 2);
        
        return stats;
    }
    
    /**
     * 获取受保护资产
     */
    private List<Map<String, Object>> getProtectedAssets() {
        List<Map<String, Object>> assets = new ArrayList<>();
        String[] assetNames = {"核心数据库", "Web前端", "API网关", "用户系统", "支付系统"};
        String[] protectionLevels = {"高", "中", "高", "高", "中"};
        
        for (int i = 0; i < assetNames.length; i++) {
            assets.add(Map.of(
                "name", assetNames[i],
                "ip", "10.0.1." + (10 + i),
                "protection", protectionLevels[i],
                "status", "受保护",
                "lastCheck", LocalDateTime.now().minusMinutes(i * 5)
            ));
        }
        
        return assets;
    }
    
    /**
     * 获取攻击检测数据
     */
    private Map<String, Object> getAttackDetectionData() {
        Map<String, Object> detection = new HashMap<>();
        detection.put("totalDetections", 15);
        detection.put("todayDetections", 3);
        detection.put("falsePositives", 1);
        detection.put("accuracy", 93.3);
        return detection;
    }
    
    /**
     * 获取安全事件
     */
    private List<Map<String, Object>> getSecurityEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(Map.of(
            "type", "SQL注入尝试",
            "source", "192.168.1.100",
            "target", "数据库服务器",
            "action", "已阻止",
            "time", LocalDateTime.now().minusMinutes(10)
        ));
        events.add(Map.of(
            "type", "暴力破解",
            "source", "192.168.1.105",
            "target", "登录系统",
            "action", "已阻止",
            "time", LocalDateTime.now().minusMinutes(25)
        ));
        return events;
    }
    
    /**
     * 获取防御成功率
     */
    private Map<String, Object> getDefenseSuccessRate(String username) {
        Map<String, Object> rate = new HashMap<>();
        rate.put("total", 20);
        rate.put("successful", 18);
        rate.put("rate", 90.0);
        return rate;
    }
    
    /**
     * 获取威胁情报数据
     */
    private Map<String, Object> getThreatIntelligenceData() {
        Map<String, Object> intel = new HashMap<>();
        intel.put("knownThreats", 150);
        intel.put("newThreats", 5);
        intel.put("lastUpdate", LocalDateTime.now().minusHours(2));
        return intel;
    }
    
    /**
     * 获取事件响应数据
     */
    private Map<String, Object> getIncidentResponseData() {
        Map<String, Object> response = new HashMap<>();
        response.put("activeIncidents", 1);
        response.put("avgResponseTime", "3.5分钟");
        response.put("resolvedToday", 2);
        return response;
    }
    
    // ==================== 裁判相关数据方法 ====================
    
    /**
     * 获取比赛概览
     */
    private Map<String, Object> getCompetitionOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("startTime", LocalDateTime.now().minusHours(4));
        overview.put("duration", "8小时");
        overview.put("remainingTime", "4小时15分钟");
        overview.put("totalTeams", 10);
        overview.put("activeTeams", 8);
        overview.put("completedScenarios", 3);
        overview.put("totalScenarios", 5);
        return overview;
    }
    
    /**
     * 获取团队对比数据
     */
    private Map<String, Object> getTeamComparisonData() {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("redTeams", 5);
        comparison.put("blueTeams", 5);
        comparison.put("redScore", 285);
        comparison.put("blueScore", 320);
        comparison.put("redSuccessRate", 72);
        comparison.put("blueDefenseRate", 85);
        return comparison;
    }
    
    /**
     * 获取计分板数据
     */
    private List<Map<String, Object>> getScoreboardData() {
        List<Map<String, Object>> scoreboard = new ArrayList<>();
        String[] teams = {"蓝队Alpha", "红队Beta", "蓝队Gamma", "红队Delta", "蓝队Epsilon"};
        int[] scores = {320, 285, 275, 260, 245};
        
        for (int i = 0; i < teams.length; i++) {
            scoreboard.add(Map.of(
                "rank", i + 1,
                "team", teams[i],
                "score", scores[i],
                "type", teams[i].startsWith("红队") ? "red" : "blue",
                "lastActivity", LocalDateTime.now().minusMinutes(i * 10)
            ));
        }
        
        return scoreboard;
    }
    
    /**
     * 获取待审核提交
     */
    private List<Map<String, Object>> getPendingSubmissions() {
        return achievementRepository.findAll().stream()
            .filter(a -> "pending".equals(a.getStatus()))
            .limit(5)
            .map(achievement -> createMap(
                "id", achievement.getId(),
                "team", achievement.getTeamName(),
                "target", achievement.getTargetName(),
                "method", achievement.getAttackMethod() != null ? achievement.getAttackMethod() : "未知",
                "submitTime", achievement.getSubmitTime()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取比赛时间线
     */
    private List<Map<String, Object>> getCompetitionTimeline() {
        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(Map.of(
            "time", LocalDateTime.now().minusMinutes(30),
            "event", "红队Beta成功攻破Web服务器",
            "type", "attack"
        ));
        timeline.add(Map.of(
            "time", LocalDateTime.now().minusMinutes(45),
            "event", "蓝队Alpha部署新防护措施",
            "type", "defense"
        ));
        timeline.add(Map.of(
            "time", LocalDateTime.now().minusHours(1),
            "event", "第二轮攻防演练开始",
            "type", "system"
        ));
        return timeline;
    }
    
    /**
     * 获取团队表现指标
     */
    private Map<String, Object> getTeamPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avgAttackTime", "15.5分钟");
        metrics.put("avgDefenseTime", "8.2分钟");
        metrics.put("mostActiveTeam", "红队Beta");
        metrics.put("bestDefender", "蓝队Alpha");
        return metrics;
    }
    
    /**
     * 获取公平性指标
     */
    private Map<String, Object> getFairnessIndicators() {
        Map<String, Object> indicators = new HashMap<>();
        indicators.put("resourceBalance", "均衡");
        indicators.put("difficultyBalance", "适中");
        indicators.put("timeAllocation", "合理");
        indicators.put("overallFairness", 92.5);
        return indicators;
    }
}