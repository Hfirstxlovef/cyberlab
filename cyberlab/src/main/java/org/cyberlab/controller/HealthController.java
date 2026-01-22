package org.cyberlab.controller;

import org.cyberlab.service.HostNodeService;
import org.cyberlab.repository.AssetRepository;
import org.cyberlab.repository.ContainerStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    @Autowired(required = false)
    private HostNodeService hostNodeService;
    
    @Autowired(required = false)
    private AssetRepository assetRepository;
    
    @Autowired(required = false)
    private ContainerStateRepository containerStateRepository;

    /**
     * 基础健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "CyberLab Backend");
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 详细健康检查
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        
        boolean overallHealthy = true;
        
        try {
            // 检查数据库连接
            if (assetRepository != null) {
                Map<String, Object> database = new HashMap<>();
                try {
                    long assetCount = assetRepository.count();
                    database.put("status", "UP");
                    database.put("assetCount", assetCount);
                } catch (Exception e) {
                    database.put("status", "DOWN");
                    database.put("error", e.getMessage());
                    overallHealthy = false;
                }
                components.put("database", database);
            }
            
            // 检查主机节点
            if (hostNodeService != null) {
                Map<String, Object> nodes = new HashMap<>();
                try {
                    var activeNodes = hostNodeService.getActiveNodes();
                    nodes.put("status", activeNodes.isEmpty() ? "WARN" : "UP");
                    nodes.put("activeCount", activeNodes.size());
                    nodes.put("totalCount", hostNodeService.getAllNodes().size());
                    
                    if (activeNodes.isEmpty()) {
                        nodes.put("message", "没有活跃的主机节点");
                    }
                } catch (Exception e) {
                    nodes.put("status", "DOWN");
                    nodes.put("error", e.getMessage());
                    overallHealthy = false;
                }
                components.put("hostNodes", nodes);
            }
            
            // 检查容器状态
            if (containerStateRepository != null) {
                Map<String, Object> containers = new HashMap<>();
                try {
                    long totalContainers = containerStateRepository.count();
                    long syncedContainers = containerStateRepository.countBySyncStatus("SYNCED");
                    
                    containers.put("status", "UP");
                    containers.put("totalStates", totalContainers);
                    containers.put("syncedStates", syncedContainers);
                    containers.put("syncRatio", totalContainers > 0 ? 
                        Math.round((double) syncedContainers / totalContainers * 100) : 0);
                } catch (Exception e) {
                    containers.put("status", "DOWN");
                    containers.put("error", e.getMessage());
                    overallHealthy = false;
                }
                components.put("containerStates", containers);
            }
            
            // 系统信息
            Map<String, Object> system = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            system.put("status", "UP");
            system.put("maxMemoryMB", maxMemory / 1024 / 1024);
            system.put("totalMemoryMB", totalMemory / 1024 / 1024);
            system.put("usedMemoryMB", usedMemory / 1024 / 1024);
            system.put("freeMemoryMB", freeMemory / 1024 / 1024);
            system.put("memoryUsagePercent", Math.round((double) usedMemory / totalMemory * 100));
            system.put("availableProcessors", runtime.availableProcessors());
            
            components.put("system", system);
            
            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("components", components);
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(503).body(health);
        }
    }
}