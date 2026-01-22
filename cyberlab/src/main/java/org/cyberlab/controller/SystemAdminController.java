package org.cyberlab.controller;

import org.cyberlab.service.DatabaseValidationService;
import org.cyberlab.service.HostNodeService;
import org.cyberlab.util.NetworkTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 系统管理员控制器
 * 提供系统维护和修复功能
 */
@RestController
@RequestMapping("/api/system-admin")
@CrossOrigin(origins = "*")
public class SystemAdminController {

    @Autowired
    private DatabaseValidationService validationService;
    
    @Autowired
    private HostNodeService hostNodeService;

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        try {
            Map<String, Object> healthReport = validationService.getHealthReport();
            return ResponseEntity.ok(healthReport);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "健康检查失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 验证VM配置
     */
    @GetMapping("/validate-vm")
    public ResponseEntity<?> validateVMConfiguration() {
        try {
            Map<String, Object> validation = validationService.validateVMConfiguration();
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "VM配置验证失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 修复VM配置
     */
    @PostMapping("/fix-vm")
    public ResponseEntity<?> fixVMConfiguration() {
        try {
            Map<String, Object> fixResult = validationService.fixVMConfiguration();
            
            // 修复后进行网络测试
            if ((Boolean) fixResult.get("success")) {
                NetworkTestUtil.NetworkDiagnosticResult networkResult = 
                    NetworkTestUtil.diagnoseConnection("172.16.190.130", 2375);
                
                fixResult.put("networkTest", Map.of(
                    "pingSuccess", networkResult.isPingSuccess(),
                    "dockerApiSuccess", networkResult.isDockerApiSuccess(),
                    "overallStatus", networkResult.getOverallStatus(),
                    "recommendation", networkResult.getRecommendation()
                ));
            }
            
            return ResponseEntity.ok(fixResult);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "VM配置修复失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 完整系统诊断
     */
    @PostMapping("/full-diagnosis")
    public ResponseEntity<?> performFullDiagnosis() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 1. 数据库验证
            Map<String, Object> validation = validationService.validateVMConfiguration();
            result.put("databaseValidation", validation);
            
            // 2. 网络测试
            NetworkTestUtil.NetworkDiagnosticResult networkResult = 
                NetworkTestUtil.diagnoseConnection("172.16.190.130", 2375);
            
            result.put("networkTest", Map.of(
                "host", "172.16.190.130",
                "port", 2375,
                "pingSuccess", networkResult.isPingSuccess(),
                "tcpConnectionSuccess", networkResult.isTcpConnectionSuccess(),
                "dockerApiSuccess", networkResult.isDockerApiSuccess(),
                "overallStatus", networkResult.getOverallStatus(),
                "recommendation", networkResult.getRecommendation()
            ));
            
            // 3. 主机节点状态
            try {
                var nodes = hostNodeService.getAllNodes();
                var vmNodes = nodes.stream()
                    .filter(node -> "172.16.190.130".equals(node.getHostIp()))
                    .map(node -> Map.of(
                        "id", node.getId(),
                        "name", node.getDisplayName(),
                        "ip", node.getHostIp(),
                        "port", node.getDockerPort(),
                        "status", node.getStatus(),
                        "lastHealthCheck", node.getLastHealthCheck() != null ? 
                                         node.getLastHealthCheck().toString() : "未检查"
                    ))
                    .toList();
                
                result.put("hostNodes", vmNodes);
            } catch (Exception e) {
                result.put("hostNodes", "检查失败: " + e.getMessage());
            }
            
            // 4. 综合评估
            boolean databaseHealthy = (Boolean) validation.get("overallHealthy");
            boolean networkHealthy = networkResult.isDockerApiSuccess();
            
            String overallStatus;
            String overallRecommendation;
            
            if (databaseHealthy && networkHealthy) {
                overallStatus = "HEALTHY";
                overallRecommendation = "系统配置正确，容器发现功能应该正常工作";
            } else if (databaseHealthy && !networkHealthy) {
                overallStatus = "NETWORK_ISSUE";
                overallRecommendation = "数据库配置正确，但网络连接有问题，请检查VM Docker daemon配置";
            } else if (!databaseHealthy && networkHealthy) {
                overallStatus = "DATABASE_ISSUE";
                overallRecommendation = "网络连接正常，但数据库配置有问题，建议执行修复";
            } else {
                overallStatus = "CRITICAL";
                overallRecommendation = "数据库和网络都有问题，需要全面修复";
            }
            
            result.put("overallStatus", overallStatus);
            result.put("overallRecommendation", overallRecommendation);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "完整诊断失败: " + e.getMessage()
            ));
        }
    }
}