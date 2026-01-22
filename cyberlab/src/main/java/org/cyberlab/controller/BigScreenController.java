package org.cyberlab.controller;

import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.service.BigScreenService;
import org.cyberlab.service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/bigscreen")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class BigScreenController {

    @Autowired
    private BigScreenService bigScreenService;
    
    @Autowired
    private DockerService dockerService;

    /**
     * 获取大屏展示数据 - 根据用户角色返回不同数据
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getBigScreenData() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userRole = getUserRole(auth);
            
            Map<String, Object> data = bigScreenService.getBigScreenDataByRole(userRole);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取管理员专用大屏数据
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Object>> getAdminBigScreenData() {
        try {
            Map<String, Object> data = bigScreenService.getAdminBigScreenData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取红队专用大屏数据
     */
    @GetMapping("/red")
    @PreAuthorize("hasRole('red')")
    public ResponseEntity<Map<String, Object>> getRedTeamBigScreenData() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Map<String, Object> data = bigScreenService.getRedTeamBigScreenData(username);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取蓝队专用大屏数据
     */
    @GetMapping("/blue")
    @PreAuthorize("hasRole('blue')")
    public ResponseEntity<Map<String, Object>> getBlueTeamBigScreenData() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Map<String, Object> data = bigScreenService.getBlueTeamBigScreenData(username);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取裁判专用大屏数据
     */
    @GetMapping("/judge")
    @PreAuthorize("hasRole('judge')")
    public ResponseEntity<Map<String, Object>> getJudgeBigScreenData() {
        try {
            Map<String, Object> data = bigScreenService.getJudgeBigScreenData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取实时系统资源数据
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> getSystemResources() {
        try {
            Map<String, Object> resources = bigScreenService.getSystemResources();
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取攻击趋势数据
     */
    @GetMapping("/trend")
    public ResponseEntity<Object> getAttackTrend(@RequestParam(defaultValue = "24") int hours) {
        try {
            Object trendData = bigScreenService.getAttackTrend(hours);
            return ResponseEntity.ok(trendData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取容器状态信息 - 用于大屏展示
     */
    @GetMapping("/container-status")
    public ResponseEntity<List<ContainerInfo>> getContainerStatus() {
        try {
            List<ContainerInfo> containers = dockerService.getAllContainers();
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取当前用户信息 - 用于前端大屏页面
     */
    @GetMapping("/user/current")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Map<String, Object> userInfo = new HashMap<>();
            
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                userInfo.put("username", auth.getName());
                userInfo.put("displayName", auth.getName()); // 可以从用户服务获取真实姓名
                userInfo.put("role", getUserRole(auth));
                userInfo.put("authenticated", true);
            } else {
                userInfo.put("username", "访客");
                userInfo.put("displayName", "访客用户");
                userInfo.put("role", "guest");
                userInfo.put("authenticated", false);
            }
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            // 返回默认访客信息
            Map<String, Object> guestInfo = new HashMap<>();
            guestInfo.put("username", "访客");
            guestInfo.put("displayName", "访客用户");
            guestInfo.put("role", "guest");
            guestInfo.put("authenticated", false);
            return ResponseEntity.ok(guestInfo);
        }
    }

    /**
     * 获取用户角色
     */
    private String getUserRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return "guest";
        }
        
        // 从权限中提取角色
        return auth.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(role -> role.startsWith("ROLE_"))
            .map(role -> role.substring(5)) // 移除"ROLE_"前缀
            .findFirst()
            .orElse("guest");
    }
}