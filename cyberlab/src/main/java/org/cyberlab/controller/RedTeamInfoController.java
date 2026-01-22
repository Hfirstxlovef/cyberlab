package org.cyberlab.controller;

import org.cyberlab.entity.Asset;
import org.cyberlab.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 红队专用信息控制器
 * 仅提供红队需要的演练目标信息，不暴露内部资产细节
 */
@RestController
@RequestMapping("/api/red")
@CrossOrigin(origins = "*")
public class RedTeamInfoController {

    private static final Logger logger = LoggerFactory.getLogger(RedTeamInfoController.class);

    @Autowired
    private AssetService assetService;
    
    /**
     * 获取红队可参与的活跃演练列表
     */
    @GetMapping("/active-drills")
    @PreAuthorize("hasAnyRole('red', 'admin')")
    public ResponseEntity<List<Map<String, Object>>> getActiveDrills() {
        try {
            // 获取苏州科技大学项目的演练信息
            List<Map<String, Object>> activeDrills = new ArrayList<>();
            
            Map<String, Object> drill = new HashMap<>();
            drill.put("id", 1L);
            drill.put("name", "苏州科技大学｜网络安全演练项目");
            drill.put("status", "active");
            drill.put("startTime", "2025-01-01T00:00:00");
            drill.put("endTime", "2025-12-31T23:59:59");
            activeDrills.add(drill);
            
            return ResponseEntity.ok(activeDrills);
        } catch (Exception e) {
            logger.error("获取活跃演练列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定演练的目标信息（仅互联网出口）
     */
    @GetMapping("/drill-targets/{drillId}")
    @PreAuthorize("hasAnyRole('red', 'admin')")
    public ResponseEntity<List<Map<String, Object>>> getDrillTargets(@PathVariable Long drillId) {
        try {
            // 从数据库获取苏州科技大学项目的资产
            List<Asset> allAssets = assetService.getAllAssets();
            
            // 过滤条件：
            // 1. 项目名称包含"苏州科技大学"
            // 2. 是目标资产 (isTarget = true)
            // 3. 对红队可见 (visibility 包含 'red' 或 'both')
            List<Asset> targetAssets = allAssets.stream()
                .filter(asset -> asset.getProject() != null && 
                               asset.getProject().contains("苏州科技大学"))
                .filter(asset -> asset.isTarget())
                .filter(asset -> asset.getVisibility() != null && 
                               (asset.getVisibility().contains("red") || 
                                asset.getVisibility().contains("both")))
                .collect(Collectors.toList());
            
            // 转换为红队需要的信息格式
            List<Map<String, Object>> targets = new ArrayList<>();
            
            for (Asset asset : targetAssets) {
                Map<String, Object> target = new HashMap<>();
                target.put("name", asset.getName());
                target.put("ip", asset.getIp());
                
                // 解析端口信息（从容器端口配置或其他字段）
                List<Integer> ports = extractPorts(asset);
                target.put("ports", ports);
                
                // 简化的描述，不暴露内部细节
                String description = generateSafeDescription(asset);
                target.put("description", description);
                
                targets.add(target);
            }
            
            // 如果没有找到实际数据，返回默认的演练目标
            if (targets.isEmpty()) {
                targets = generateDefaultTargets();
            }
            
            return ResponseEntity.ok(targets);
        } catch (Exception e) {
            logger.error("获取演练目标信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 从资产信息中提取端口
     */
    private List<Integer> extractPorts(Asset asset) {
        List<Integer> ports = new ArrayList<>();
        
        // 根据资产类型推断常见端口
        String assetType = asset.getAssetType();
        String name = asset.getName().toLowerCase();
        
        if (name.contains("web") || assetType != null && assetType.contains("server")) {
            ports.add(80);
            ports.add(443);
        }
        if (name.contains("api")) {
            ports.add(8080);
            ports.add(8443);
        }
        if (name.contains("ftp")) {
            ports.add(21);
            ports.add(22);
        }
        if (name.contains("ssh")) {
            ports.add(22);
        }
        if (name.contains("database") || name.contains("mysql")) {
            ports.add(3306);
        }
        
        // 如果没有识别出端口，返回默认端口
        if (ports.isEmpty()) {
            ports.add(80);
        }
        
        return ports;
    }
    
    /**
     * 生成安全的描述信息
     */
    private String generateSafeDescription(Asset asset) {
        String assetType = asset.getAssetType();
        String name = asset.getName().toLowerCase();
        
        if (name.contains("web")) {
            return "Web应用服务";
        } else if (name.contains("api")) {
            return "API服务接口";
        } else if (name.contains("ftp")) {
            return "文件传输服务";
        } else if (name.contains("database")) {
            return "数据存储服务";
        } else if (assetType != null) {
            switch (assetType) {
                case "server":
                    return "应用服务器";
                case "container":
                    return "容器化服务";
                case "service":
                    return "网络服务";
                case "network":
                    return "网络设备";
                default:
                    return "目标系统";
            }
        }
        
        return "目标系统";
    }
    
    /**
     * 生成默认的目标信息
     */
    private List<Map<String, Object>> generateDefaultTargets() {
        List<Map<String, Object>> targets = new ArrayList<>();
        
        Map<String, Object> target1 = new HashMap<>();
        target1.put("name", "Web服务器");
        target1.put("ip", "192.168.1.100");
        target1.put("ports", Arrays.asList(80, 443));
        target1.put("description", "主要Web应用服务器");
        targets.add(target1);
        
        Map<String, Object> target2 = new HashMap<>();
        target2.put("name", "API网关");
        target2.put("ip", "192.168.1.101");
        target2.put("ports", Arrays.asList(8080, 8443));
        target2.put("description", "API服务网关");
        targets.add(target2);
        
        Map<String, Object> target3 = new HashMap<>();
        target3.put("name", "FTP服务器");
        target3.put("ip", "192.168.1.102");
        target3.put("ports", Arrays.asList(21, 22));
        target3.put("description", "文件传输服务");
        targets.add(target3);
        
        return targets;
    }
}