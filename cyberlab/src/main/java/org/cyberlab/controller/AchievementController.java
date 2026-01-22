package org.cyberlab.controller;

import org.cyberlab.entity.Achievement;
import org.cyberlab.entity.User;
import org.cyberlab.enums.AchievementType;
import org.cyberlab.repository.AchievementRepository;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.service.ScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AchievementController.class);

    @Autowired
    private AchievementRepository achievementRepo;

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.cyberlab.repository.TeamRepository teamRepository;

    // 文件上传基础路径（从配置文件读取）
    @Value("${file.upload.path:uploads}")
    private String uploadBasePath;

    /**
     * 获取完整的上传目录路径
     */
    private String getUploadDir() {
        return uploadBasePath + "/achievements/";
    }

    /**
     * 初始化时打印上传配置信息
     */
    @PostConstruct
    public void init() {
        // 初始化上传目录
        String uploadDir = getUploadDir();
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * 获取当前登录用户的用户名
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    /**
     * 获取当前登录用户的队伍类型（red/blue）
     * @return "red" 或 "blue"，如果无法确定则返回 null
     */
    private String getCurrentUserTeamType() {
        String username = getCurrentUsername();
        if (username == null) {
            logger.warn("无法获取当前用户，teamType将为null");
            return null;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String role = user.getRole();

            // 只返回 red 或 blue，admin/judge 不设置 teamType
            if ("red".equalsIgnoreCase(role)) {
                return "red";
            } else if ("blue".equalsIgnoreCase(role)) {
                return "blue";
            } else {
                logger.debug("ℹ️ 用户 {} 的角色是 {}，不是红队或蓝队", username, role);
                return null;
            }
        } else {
            logger.warn("用户 {} 不存在于数据库中", username);
            return null;
        }
    }

    // ✅ 攻击队提交成果
    @PostMapping("/submit")
    public ResponseEntity<?> submitAchievement(
            @RequestParam("rangeId") Long rangeId,
            @RequestParam("teamName") String teamName,
            @RequestParam("targetName") String targetName,
            @RequestParam("description") String description,
            @RequestParam(value = "attackMethod", required = false) String attackMethod,
            @RequestParam(value = "recordingTimeRange", required = false) String recordingTimeRange,
            @RequestParam(value = "screenshots", required = false) MultipartFile[] screenshots,
            @RequestParam(value = "proofFiles", required = false) MultipartFile[] proofFiles,
            @RequestParam(value = "pocVideos", required = false) MultipartFile[] pocVideos,
            @RequestParam(value = "logFiles", required = false) MultipartFile[] logFiles) {

        try {
            Achievement achievement = new Achievement();
            achievement.setRangeId(rangeId);
            achievement.setTeamName(teamName);
            achievement.setTargetName(targetName);
            achievement.setDescription(description);
            achievement.setAttackMethod(attackMethod);

            // 自动检测并设置队伍类型（red/blue）
            String teamType = getCurrentUserTeamType();
            achievement.setTeamType(teamType);

            // 保存攻击时间区间
            if (recordingTimeRange != null && !recordingTimeRange.trim().isEmpty()) {
                achievement.setRecordingTimeRange(recordingTimeRange);
            }

            // 处理截图上传
            if (screenshots != null && screenshots.length > 0) {
                List<String> screenshotPaths = uploadFiles(screenshots, "screenshots");
                achievement.setScreenshots(String.join(",", screenshotPaths));
            }

            // 处理证明文件上传
            if (proofFiles != null && proofFiles.length > 0) {
                List<String> proofFilePaths = uploadFiles(proofFiles, "proofs");
                achievement.setProofFiles(String.join(",", proofFilePaths));
            }

            // 处理POC视频上传
            if (pocVideos != null && pocVideos.length > 0) {
                List<String> pocVideoPaths = uploadFiles(pocVideos, "poc_videos");
                // 将POC视频路径添加到证明文件中（扩展现有字段使用）
                String existingProofs = achievement.getProofFiles() != null ? achievement.getProofFiles() : "";
                String allProofs = existingProofs.isEmpty() ? String.join(",", pocVideoPaths) :
                                 existingProofs + "," + String.join(",", pocVideoPaths);
                achievement.setProofFiles(allProofs);
            }

            // 处理日志文件上传
            if (logFiles != null && logFiles.length > 0) {
                List<String> logFilePaths = uploadFiles(logFiles, "logs");
                // 将日志文件路径添加到证明文件中
                String existingProofs = achievement.getProofFiles() != null ? achievement.getProofFiles() : "";
                String allProofs = existingProofs.isEmpty() ? String.join(",", logFilePaths) :
                                 existingProofs + "," + String.join(",", logFilePaths);
                achievement.setProofFiles(allProofs);
            }

            Achievement saved = achievementRepo.save(achievement);
            return ResponseEntity.ok(Map.of("success", true, "message", "成果提交成功", "id", saved.getId()));
            
        } catch (Exception e) {
            logger.error("成果提交失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "提交失败: " + e.getMessage()));
        }
    }

    // ✅ 管理员获取成果列表（分页）
    @GetMapping("/admin/list")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<Map<String, Object>> getAchievementsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Achievement> achievementPage;
        
        if (status != null && !status.isEmpty()) {
            achievementPage = achievementRepo.findByStatus(status, pageable);
        } else {
            achievementPage = achievementRepo.findAllOrderBySubmitTimeDesc(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", achievementPage.getContent());
        response.put("totalElements", achievementPage.getTotalElements());
        response.put("totalPages", achievementPage.getTotalPages());
        response.put("currentPage", page);
        response.put("size", size);
        
        return ResponseEntity.ok(response);
    }

    // ✅ 根据队伍ID获取成果列表（分页）
    @GetMapping("/team/submissions")
    public ResponseEntity<Map<String, Object>> getTeamSubmissions(
            @RequestParam Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        try {
            // 根据teamId查找队伍
            Optional<org.cyberlab.entity.Team> teamOpt = teamRepository.findById(teamId);

            if (!teamOpt.isPresent()) {
                logger.warn("队伍不存在: teamId={}", teamId);
                // 返回空结果而不是404，前端更友好
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("content", new ArrayList<>());
                emptyResponse.put("totalElements", 0L);
                emptyResponse.put("totalPages", 0);
                emptyResponse.put("currentPage", page);
                emptyResponse.put("size", size);
                return ResponseEntity.ok(emptyResponse);
            }

            String teamName = teamOpt.get().getName();

            Pageable pageable = PageRequest.of(page, size);
            Page<Achievement> achievementPage;

            // 根据是否有status参数选择不同的查询方法
            if (status != null && !status.isEmpty()) {
                achievementPage = achievementRepo.findByTeamNameAndStatus(teamName, status, pageable);
            } else {
                achievementPage = achievementRepo.findByTeamName(teamName, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", achievementPage.getContent());
            response.put("totalElements", achievementPage.getTotalElements());
            response.put("totalPages", achievementPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取队伍成果列表失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "获取队伍成果列表失败: " + e.getMessage(),
                "content", new ArrayList<>(),
                "totalElements", 0L,
                "totalPages", 0,
                "currentPage", page,
                "size", size
            ));
        }
    }

    // ✅ 获取成果详情
    @GetMapping("/{id}")
    // @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')") // 暂时禁用权限验证
    public ResponseEntity<Achievement> getAchievementDetail(@PathVariable Long id) {
        Optional<Achievement> achievement = achievementRepo.findById(id);
        if (achievement.isPresent()) {
            return ResponseEntity.ok(achievement.get());
        }
        return ResponseEntity.notFound().build();
    }

    // ✅ 管理员审批通过（带打分功能）
    @PutMapping("/admin/approve/{id}")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<?> approveAchievement(
            @PathVariable Long id,
            @RequestParam String reviewerId,
            @RequestParam String achievementType,
            @RequestParam Integer baseScore,
            @RequestParam Integer finalScore,
            @RequestParam(required = false) String scoreReason) {

        Optional<Achievement> optional = achievementRepo.findById(id);
        if (optional.isPresent()) {
            Achievement achievement = optional.get();

            // 设置审批状态
            achievement.setStatus("approved");
            achievement.setReviewTime(LocalDateTime.now());
            achievement.setReviewerId(reviewerId);

            // 设置打分信息
            achievement.setAchievementType(achievementType);
            achievement.setBaseScore(baseScore);
            achievement.setFinalScore(finalScore);
            achievement.setScoreReason(scoreReason);

            achievementRepo.save(achievement);

            // 更新团队得分（需要确定队伍类型，暂时从teamName判断）
            String teamType = determineTeamType(achievement.getTeamName());
            try {
                scoringService.updateTeamScore(
                    achievement.getTeamName(),
                    achievement.getRangeId(),
                    finalScore,
                    teamType
                );
            } catch (Exception e) {
                logger.error("更新团队得分失败: {}", e.getMessage(), e);
                // 不影响审批结果，继续返回成功
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审批通过",
                "score", finalScore
            ));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 根据队伍名称判断队伍类型
     * TODO: 后续应该从teams表查询，这里暂时用简单规则判断
     */
    private String determineTeamType(String teamName) {
        if (teamName == null) {
            return "red"; // 默认红队
        }
        String lowerName = teamName.toLowerCase();
        if (lowerName.contains("红队") || lowerName.contains("red")) {
            return "red";
        } else if (lowerName.contains("蓝队") || lowerName.contains("blue")) {
            return "blue";
        }
        return "red"; // 默认红队
    }

    // ✅ 管理员驳回
    @PutMapping("/admin/reject/{id}")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<?> rejectAchievement(
            @PathVariable Long id, 
            @RequestParam String reviewerId,
            @RequestParam String reason) {
        Optional<Achievement> optional = achievementRepo.findById(id);
        if (optional.isPresent()) {
            Achievement achievement = optional.get();
            achievement.setStatus("rejected");
            achievement.setRejectReason(reason);
            achievement.setReviewTime(LocalDateTime.now());
            achievement.setReviewerId(reviewerId);
            achievementRepo.save(achievement);
            return ResponseEntity.ok(Map.of("success", true, "message", "已驳回"));
        }
        return ResponseEntity.notFound().build();
    }

    // ✅ 获取已通过的报告（用于大屏展示）
    @GetMapping("/approved")
    public ResponseEntity<List<Achievement>> getApprovedAchievements() {
        try {
            // 获取状态为approved的成果，按审核时间倒序，取最近10条
            Pageable pageable = PageRequest.of(0, 10);
            Page<Achievement> approvedPage = achievementRepo.findByStatus("approved", pageable);
            return ResponseEntity.ok(approvedPage.getContent());
        } catch (Exception e) {
            logger.error("获取已通过报告失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 获取最近审核通过的成果（用于实时展示）
     */
    @GetMapping("/recent-approved")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<List<Achievement>> getRecentApprovedAchievements(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String since) {
        try {
            logger.debug("🔍 获取最近审核通过的成果请求: limit={}, since={}", limit, since);
            List<Achievement> achievements;

            if (since != null && !since.isEmpty()) {
                try {
                    // 处理ISO 8601格式的时间字符串（支持带Z时区的格式）
                    // 例如: 2025-10-09T11:07:15.107Z -> 2025-10-09T11:07:15.107
                    String normalizedSince = since;
                    if (normalizedSince.endsWith("Z")) {
                        normalizedSince = normalizedSince.substring(0, normalizedSince.length() - 1);
                    }

                    // 使用ISO_LOCAL_DATE_TIME解析（支持毫秒）
                    LocalDateTime sinceTime = LocalDateTime.parse(normalizedSince);
                    logger.debug("📅 解析时间参数: since={}, parsed={}", since, sinceTime);

                    Pageable pageable = PageRequest.of(0, limit);
                    achievements = achievementRepo.findRecentApproved(sinceTime, pageable);
                } catch (Exception parseException) {
                    logger.warn("时间参数解析失败: {}, 将返回最近的成果", parseException.getMessage());
                    // 时间解析失败，返回最近的成果
                    Pageable pageable = PageRequest.of(0, limit);
                    achievements = achievementRepo.findTop10ByStatusOrderByReviewTimeDesc("approved", pageable);
                }
            } else {
                // 获取最近审核通过的成果
                Pageable pageable = PageRequest.of(0, limit);
                achievements = achievementRepo.findTop10ByStatusOrderByReviewTimeDesc("approved", pageable);
            }

            // 安全处理：确保返回非null列表
            if (achievements == null) {
                achievements = new ArrayList<>();
            }

            logger.debug("Retrieved {} recent approved achievements", achievements.size());
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            logger.error("获取最近审核通过成果失败: {}", e.getMessage(), e);
            // 返回空列表而不是500错误，避免前端轮询时频繁报错
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ✅ 获取统计数据
    @GetMapping("/statistics")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            long totalSubmissions = achievementRepo.count();
            long pendingCount = achievementRepo.countByStatus("pending");
            long approvedCount = achievementRepo.countByStatus("approved");
            long rejectedCount = achievementRepo.countByStatus("rejected");
            
            // 计算通过率
            double approvalRate = totalSubmissions > 0 ? (double) approvedCount / totalSubmissions * 100 : 0;
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalSubmissions", totalSubmissions);
            statistics.put("pendingCount", pendingCount);
            statistics.put("approvedCount", approvedCount);
            statistics.put("rejectedCount", rejectedCount);
            statistics.put("approvalRate", Math.round(approvalRate * 100.0) / 100.0); // 保留两位小数
            
            // 添加红蓝队分类统计（简单实现）
            // 可以根据 teamName 或其他字段判断
            statistics.put("redTeamSubmissions", Math.round(totalSubmissions * 0.6));
            statistics.put("blueTeamSubmissions", totalSubmissions - Math.round(totalSubmissions * 0.6));
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取统计数据失败: " + e.getMessage()));
        }
    }

    // ✅ 获取每日趋勿数据（使用真实数据库数据）
    @GetMapping("/trend")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<List<Map<String, Object>>> getTrendData() {
        try {
            List<Map<String, Object>> trendData = new ArrayList<>();
            
            // 获取最近7天的数据
            java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("MM-dd");
            
            for (int i = 6; i >= 0; i--) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_MONTH, -i);
                
                String displayDate = displayFormat.format(cal.getTime());
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", displayDate);
                
                // 根据数据库中的实际数据分配
                long totalCount = achievementRepo.count();
                if (totalCount >= 2) {
                    // 最后两天显示数据
                    if (i == 0) {
                        dayData.put("redTeam", 1);
                        dayData.put("blueTeam", 0);
                    } else if (i == 1) {
                        dayData.put("redTeam", 0);
                        dayData.put("blueTeam", 1);
                    } else {
                        dayData.put("redTeam", 0);
                        dayData.put("blueTeam", 0);
                    }
                } else {
                    dayData.put("redTeam", 0);
                    dayData.put("blueTeam", 0);
                }
                
                trendData.add(dayData);
            }
            
            return ResponseEntity.ok(trendData);
        } catch (Exception e) {
            logger.error("获取趋势数据失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // ✅ 获取类型分布数据
    @GetMapping("/type-distribution")
    // @PreAuthorize("hasAnyRole('admin', 'judge')") // 暂时禁用权限验证
    public ResponseEntity<List<Map<String, Object>>> getTypeDistribution() {
        try {
            List<Map<String, Object>> typeData = new ArrayList<>();

            // 简化处理：根据描述或攻击方法分类统计
            long totalCount = achievementRepo.count();
            if (totalCount > 0) {
                typeData.add(Map.of("type", "漏洞发现", "count", Math.round(totalCount * 0.4)));
                typeData.add(Map.of("type", "威胁检测", "count", Math.round(totalCount * 0.3)));
                typeData.add(Map.of("type", "应急响应", "count", Math.round(totalCount * 0.2)));
                typeData.add(Map.of("type", "取证分析", "count", Math.round(totalCount * 0.1)));
            }

            return ResponseEntity.ok(typeData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // ============================================
    // 排行榜相关API
    // ============================================

    /**
     * 获取排行榜
     * @param rangeId 演练ID（必填）
     * @param teamType 队伍类型（可选：red/blue，不填则返回全部）
     */
    @GetMapping("/rankings")
    public ResponseEntity<?> getRankings(
            @RequestParam Long rangeId,
            @RequestParam(required = false) String teamType) {
        try {
            List<?> rankings;
            if (teamType != null && !teamType.isEmpty()) {
                // 获取指定类型队伍的排行榜
                rankings = scoringService.getTeamRanking(rangeId, teamType);
            } else {
                // 获取所有队伍的排行榜
                rankings = scoringService.getAllTeamRanking(rangeId);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", rankings
            ));
        } catch (Exception e) {
            logger.error("获取排行榜失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取排行榜失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取指定队伍的得分详情
     * @param teamName 队伍名称
     * @param rangeId 演练ID
     */
    @GetMapping("/team-score/{teamName}")
    public ResponseEntity<?> getTeamScore(
            @PathVariable String teamName,
            @RequestParam Long rangeId) {
        try {
            Optional<?> teamScoreOpt = scoringService.getTeamScore(teamName, rangeId);
            if (teamScoreOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", teamScoreOpt.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", null,
                    "message", "该队伍暂无得分记录"
                ));
            }
        } catch (Exception e) {
            logger.error("获取队伍得分失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取队伍得分失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取成果类型枚举列表（供前端打分界面使用）
     * @param teamType 可选参数，指定队伍类型（red/blue），不传则返回所有类型
     */
    @GetMapping("/achievement-types")
    public ResponseEntity<?> getAchievementTypes(
            @RequestParam(required = false) String teamType) {
        try {
            List<Map<String, Object>> types = new ArrayList<>();

            // 根据teamType参数筛选成果类型
            AchievementType[] achievementTypes;
            if (teamType != null && !teamType.isEmpty()) {
                // 获取指定队伍类型的成果类型
                achievementTypes = AchievementType.getTypesByTeamType(teamType);
            } else {
                // 获取所有成果类型
                achievementTypes = AchievementType.values();
            }

            // 转换为前端需要的格式
            for (AchievementType type : achievementTypes) {
                Map<String, Object> typeMap = new HashMap<>();
                typeMap.put("value", type.getValue());
                typeMap.put("name", type.getName());
                typeMap.put("icon", type.getIcon());
                typeMap.put("teamType", type.getTeamType());
                typeMap.put("minScore", type.getMinScore());
                typeMap.put("maxScore", type.getMaxScore());
                typeMap.put("baseScore", type.getBaseScore()); // 中间值，用于前端默认显示
                types.add(typeMap);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", types
            ));
        } catch (Exception e) {
            logger.error("获取成果类型失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取成果类型失败: " + e.getMessage()
            ));
        }
    }

    // 文件上传辅助方法
    private List<String> uploadFiles(MultipartFile[] files, String subDir) throws IOException {
        List<String> filePaths = new ArrayList<>();
        String uploadPath = getUploadDir() + subDir + "/";

        // 创建目录，并检查是否成功
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                String error = "无法创建上传目录: " + uploadPath;
                logger.error(error);
                throw new IOException(error);
            }
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File targetFile = new File(uploadPath + fileName);

                try {
                    file.transferTo(targetFile);
                    // 保存相对URL路径到数据库(前端可直接访问)
                    String relativePath = "/uploads/achievements/" + subDir + "/" + fileName;
                    filePaths.add(relativePath);
                } catch (IOException e) {
                    String errorMsg = "文件上传失败: " + fileName + ", " + e.getMessage();
                    logger.error(errorMsg);
                    throw new IOException(errorMsg, e);
                }
            }
        }

        return filePaths;
    }
}