package org.cyberlab.controller;

import org.cyberlab.entity.User;
import org.cyberlab.entity.Asset;
import org.cyberlab.service.UserService;
import org.cyberlab.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/red")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class RedController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AssetService assetService;
    
    @GetMapping("/hello")
    public String helloRed() {
        return "Hello Red Team!";
    }
    
    // 红队专属用户列表 - 只能查看红队成员
    @GetMapping("/users")
    @PreAuthorize("hasRole('red')")
    public ResponseEntity<List<User>> getRedTeamUsers() {
        try {
            List<User> redUsers = userService.getUsersByRole("red");
            return ResponseEntity.ok(redUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 红队可见的攻击目标资产
    @GetMapping("/assets")
    @PreAuthorize("hasRole('red')")
    public ResponseEntity<List<Asset>> getAttackableAssets() {
        try {
            List<Asset> assets = assetService.getVisibleAssets("red");
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 红队统计数据
    @GetMapping("/stats")
    @PreAuthorize("hasRole('red')")
    public ResponseEntity<Map<String, Object>> getRedTeamStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<User> redUsers = userService.getUsersByRole("red");
            List<Asset> attackableAssets = assetService.getVisibleAssets("red");
            
            stats.put("teamMemberCount", redUsers.size());
            stats.put("attackableTargets", attackableAssets.size());
            stats.put("highValueTargets", attackableAssets.stream().mapToInt(asset -> asset.isTarget() ? 1 : 0).sum());
            stats.put("onlineMembers", (int) redUsers.stream().filter(User::isEnabled).count());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
