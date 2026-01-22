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
@RequestMapping("/api/blue")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class BlueController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AssetService assetService;
    
    @GetMapping("/hello")
    public String helloBlue() {
        return "Hello Blue Team!";
    }
    
    // 蓝队专属用户列表 - 只能查看蓝队成员
    @GetMapping("/users")
    @PreAuthorize("hasRole('blue')")
    public ResponseEntity<List<User>> getBlueTeamUsers() {
        try {
            List<User> blueUsers = userService.getUsersByRole("blue");
            return ResponseEntity.ok(blueUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 蓝队需要防护的资产
    @GetMapping("/assets")
    @PreAuthorize("hasRole('blue')")
    public ResponseEntity<List<Asset>> getDefensiveAssets() {
        try {
            List<Asset> assets = assetService.getVisibleAssets("blue");
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 蓝队统计数据
    @GetMapping("/stats")
    @PreAuthorize("hasRole('blue')")
    public ResponseEntity<Map<String, Object>> getBlueTeamStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<User> blueUsers = userService.getUsersByRole("blue");
            List<Asset> defensiveAssets = assetService.getVisibleAssets("blue");
            
            stats.put("teamMemberCount", blueUsers.size());
            stats.put("protectedAssets", defensiveAssets.size());
            stats.put("criticalAssets", defensiveAssets.stream().mapToInt(asset -> asset.isTarget() ? 1 : 0).sum());
            stats.put("onlineMembers", (int) blueUsers.stream().filter(User::isEnabled).count());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}