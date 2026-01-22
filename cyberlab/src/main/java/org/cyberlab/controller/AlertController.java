package org.cyberlab.controller;

import org.cyberlab.entity.Asset;
import org.cyberlab.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @Autowired
    private AssetRepository assetRepository;

    /**
     * 获取项目告警信息
     */
    @GetMapping("/project")
    public ResponseEntity<List<Map<String, Object>>> getProjectAlerts(@RequestParam String projectId) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        try {
            // 获取项目相关资产
            List<Asset> projectAssets = assetRepository.findByTopologyProjectId(projectId);
            
            // 模拟一些告警数据
            if (!projectAssets.isEmpty()) {
                // 高CPU使用率告警
                if (Math.random() > 0.7) { // 30%概率出现CPU告警
                    Map<String, Object> cpuAlert = new HashMap<>();
                    cpuAlert.put("level", "warning");
                    cpuAlert.put("message", "项目 " + projectId + " 中的容器CPU使用率过高 (85%)");
                    cpuAlert.put("timestamp", LocalDateTime.now().minusMinutes(5));
                    alerts.add(cpuAlert);
                }
                
                // 节点连接告警
                if (Math.random() > 0.8) { // 20%概率出现节点连接告警
                    Map<String, Object> nodeAlert = new HashMap<>();
                    nodeAlert.put("level", "error");
                    nodeAlert.put("message", "主机节点连接异常，可能影响 " + projectAssets.size() + " 个资产");
                    nodeAlert.put("timestamp", LocalDateTime.now().minusMinutes(10));
                    alerts.add(nodeAlert);
                }
                
                // 内存使用告警
                if (Math.random() > 0.6) { // 40%概率出现内存告警
                    Map<String, Object> memoryAlert = new HashMap<>();
                    memoryAlert.put("level", "info");
                    memoryAlert.put("message", "项目资产内存使用率达到70%，建议关注");
                    memoryAlert.put("timestamp", LocalDateTime.now().minusMinutes(15));
                    alerts.add(memoryAlert);
                }

                // 网络连通性告警
                if (Math.random() > 0.85) { // 15%概率出现网络告警
                    Map<String, Object> networkAlert = new HashMap<>();
                    networkAlert.put("level", "warning");
                    networkAlert.put("message", "检测到部分资产网络延迟较高，请检查网络配置");
                    networkAlert.put("timestamp", LocalDateTime.now().minusMinutes(20));
                    alerts.add(networkAlert);
                }
            } else {
                // 项目无资产时的提示
                Map<String, Object> noAssetAlert = new HashMap<>();
                noAssetAlert.put("level", "info");
                noAssetAlert.put("message", "项目 " + projectId + " 暂无配置资产");
                noAssetAlert.put("timestamp", LocalDateTime.now());
                alerts.add(noAssetAlert);
            }
            
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            // 返回错误告警
            Map<String, Object> errorAlert = new HashMap<>();
            errorAlert.put("level", "error");
            errorAlert.put("message", "获取项目告警信息失败: " + e.getMessage());
            errorAlert.put("timestamp", LocalDateTime.now());
            alerts.add(errorAlert);
            
            return ResponseEntity.ok(alerts);
        }
    }

    /**
     * 获取系统级告警
     */
    @GetMapping("/system")
    public ResponseEntity<List<Map<String, Object>>> getSystemAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // 模拟系统级告警
        if (Math.random() > 0.9) { // 10%概率出现系统告警
            Map<String, Object> systemAlert = new HashMap<>();
            systemAlert.put("level", "error");
            systemAlert.put("message", "系统磁盘使用率超过90%");
            systemAlert.put("timestamp", LocalDateTime.now().minusMinutes(30));
            alerts.add(systemAlert);
        }

        if (Math.random() > 0.85) { // 15%概率出现Docker告警
            Map<String, Object> dockerAlert = new HashMap<>();
            dockerAlert.put("level", "warning");
            dockerAlert.put("message", "Docker守护进程响应缓慢");
            dockerAlert.put("timestamp", LocalDateTime.now().minusMinutes(45));
            alerts.add(dockerAlert);
        }
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * 标记告警为已处理
     */
    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable String alertId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "告警已标记为已处理");
        response.put("alertId", alertId);
        response.put("resolvedAt", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}