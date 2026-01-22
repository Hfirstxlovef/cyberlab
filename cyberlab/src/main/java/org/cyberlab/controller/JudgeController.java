package org.cyberlab.controller;

import org.cyberlab.entity.User;
import org.cyberlab.entity.Asset;
import org.cyberlab.entity.Achievement;
import org.cyberlab.service.UserService;
import org.cyberlab.service.AssetService;
import org.cyberlab.repository.AchievementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/judge")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class JudgeController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private AchievementRepository achievementRepository;
    
    @GetMapping("/hello")
    public String helloJudge() {
        return "Hello Judge!";
    }
    
    // 裁判可以查看所有角色的用户 - 监督权限
    @GetMapping("/users")
    @PreAuthorize("hasRole('judge')")
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        try {
            if (role != null && !role.trim().isEmpty()) {
                List<User> users = userService.getUsersByRole(role);
                return ResponseEntity.ok(users);
            } else {
                List<User> users = userService.getAllUsers();
                return ResponseEntity.ok(users);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 裁判可以查看所有资产 - 监督演练过程
    @GetMapping("/assets")
    @PreAuthorize("hasRole('judge')")
    public ResponseEntity<List<Asset>> getAllAssets() {
        try {
            List<Asset> assets = assetService.getAllAssets();
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 裁判统计数据 - 整体比赛监督视角
    @GetMapping("/stats")
    @PreAuthorize("hasRole('judge')")
    public ResponseEntity<Map<String, Object>> getJudgeStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 用户统计
            List<User> redUsers = userService.getUsersByRole("red");
            List<User> blueUsers = userService.getUsersByRole("blue");
            List<User> allUsers = userService.getAllUsers();
            
            stats.put("redTeamCount", redUsers.size());
            stats.put("blueTeamCount", blueUsers.size());
            stats.put("totalParticipants", redUsers.size() + blueUsers.size());
            stats.put("totalUsers", allUsers.size());
            
            // 资产统计
            List<Asset> allAssets = assetService.getAllAssets();
            List<Asset> redAssets = assetService.getVisibleAssets("red");
            List<Asset> blueAssets = assetService.getVisibleAssets("blue");
            
            stats.put("totalAssets", allAssets.size());
            stats.put("redVisibleAssets", redAssets.size());
            stats.put("blueVisibleAssets", blueAssets.size());
            stats.put("targetAssets", assetService.getTargetAssets().size());
            
            // 成就统计
            List<Achievement> achievements = achievementRepository.findAll();
            stats.put("totalAchievements", achievements.size());
            stats.put("pendingReviews", (int) achievements.stream().filter(a -> "pending".equals(a.getStatus())).count());
            stats.put("approvedAchievements", (int) achievements.stream().filter(a -> "approved".equals(a.getStatus())).count());
            stats.put("rejectedAchievements", (int) achievements.stream().filter(a -> "rejected".equals(a.getStatus())).count());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 获取比赛整体概况
    @GetMapping("/competition-overview")
    @PreAuthorize("hasRole('judge')")
    public ResponseEntity<Map<String, Object>> getCompetitionOverview() {
        Map<String, Object> overview = new HashMap<>();
        try {
            // 参赛队伍状态
            List<User> redTeam = userService.getUsersByRole("red");
            List<User> blueTeam = userService.getUsersByRole("blue");
            
            overview.put("redTeamStatus", Map.of(
                "memberCount", redTeam.size(),
                "onlineCount", (int) redTeam.stream().filter(User::isEnabled).count()
            ));
            
            overview.put("blueTeamStatus", Map.of(
                "memberCount", blueTeam.size(),
                "onlineCount", (int) blueTeam.stream().filter(User::isEnabled).count()
            ));
            
            // 比赛环境状态
            List<Asset> allAssets = assetService.getAllAssets();
            overview.put("environmentStatus", Map.of(
                "totalAssets", allAssets.size(),
                "enabledAssets", (int) allAssets.stream().filter(Asset::isEnabled).count(),
                "targetAssets", (int) allAssets.stream().filter(Asset::isTarget).count()
            ));
            
            // 比赛进度
            List<Achievement> achievements = achievementRepository.findAll();
            double completionRate = achievements.isEmpty() ? 0 : 
                (double) achievements.stream().mapToInt(a -> "approved".equals(a.getStatus()) ? 1 : 0).sum() / achievements.size() * 100;
            
            overview.put("competitionProgress", Map.of(
                "totalSubmissions", achievements.size(),
                "completionRate", Math.round(completionRate * 100.0) / 100.0,
                "pendingReviews", (int) achievements.stream().filter(a -> "pending".equals(a.getStatus())).count()
            ));
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}