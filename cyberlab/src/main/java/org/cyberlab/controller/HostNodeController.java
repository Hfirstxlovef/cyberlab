package org.cyberlab.controller;

import org.cyberlab.entity.HostNode;
import org.cyberlab.entity.ContainerInfo;
import org.cyberlab.service.HostNodeService;
import org.cyberlab.service.DockerService;
import org.cyberlab.service.AssetService;
import org.cyberlab.util.NetworkTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/host-nodes")
@CrossOrigin(origins = "*")
public class HostNodeController {

    @Autowired
    private HostNodeService hostNodeService;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private AssetService assetService;
    
    /**
     * 获取所有主机节点
     */
    @GetMapping
    public ResponseEntity<List<HostNode>> getAllNodes() {
        try {
            List<HostNode> nodes = hostNodeService.getAllNodes();
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据ID获取节点详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<HostNode> getNodeById(@PathVariable Long id) {
        try {
            Optional<HostNode> node = hostNodeService.getNodeById(id);
            return node.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建新的主机节点
     */
    @PostMapping
    public ResponseEntity<?> createNode(@RequestBody HostNode node) {
        try {
            HostNode createdNode = hostNodeService.createNode(node);
            return ResponseEntity.ok(createdNode);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "创建节点失败"));
        }
    }
    
    /**
     * 更新主机节点信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNode(@PathVariable Long id, @RequestBody HostNode node) {
        try {
            HostNode updatedNode = hostNodeService.updateNode(id, node);
            return ResponseEntity.ok(updatedNode);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "更新节点失败"));
        }
    }
    
    /**
     * 删除主机节点
     * 删除前会自动清空所有关联资产的节点引用
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNode(@PathVariable Long id) {
        try {
            // 检查节点是否存在
            Optional<HostNode> nodeOpt = hostNodeService.getNodeById(id);
            if (nodeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点不存在"));
            }

            HostNode node = nodeOpt.get();

            // 统计关联的资产数量
            long affectedAssetCount = assetService.countAssetsByNodeId(id);

            // 清空关联的资产引用
            int clearedCount = assetService.clearNodeReferences(id);

            // 删除节点
            hostNodeService.deleteNode(id);

            // 返回删除结果和影响的资产数量
            Map<String, Object> response = new HashMap<>();
            response.put("message", "节点删除成功");
            response.put("nodeName", node.getDisplayName());
            response.put("affectedAssets", clearedCount);

            if (clearedCount > 0) {
                response.put("warning", String.format(
                    "已自动清空 %d 个资产的节点引用，这些资产的部署策略已改为'any'",
                    clearedCount
                ));
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "删除节点失败"));
        }
    }
    
    /**
     * 获取项目的主机节点状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getProjectHostNodeStatus(@RequestParam String projectId) {
        try {
            // 获取所有节点
            List<HostNode> allNodes = hostNodeService.getAllNodes();
            
            // 统计节点状态
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("projectId", projectId);
            statusInfo.put("totalNodes", allNodes.size());
            
            // 统计不同状态的节点数量
            long activeCount = allNodes.stream()
                .filter(node -> "active".equalsIgnoreCase(node.getStatus()))
                .count();
            long inactiveCount = allNodes.stream()
                .filter(node -> "inactive".equalsIgnoreCase(node.getStatus()))
                .count();
            long maintenanceCount = allNodes.stream()
                .filter(node -> "maintenance".equalsIgnoreCase(node.getStatus()))
                .count();
            
            statusInfo.put("activeNodes", activeCount);
            statusInfo.put("inactiveNodes", inactiveCount);
            statusInfo.put("maintenanceNodes", maintenanceCount);
            
            // 节点列表（简化信息）
            List<Map<String, Object>> nodeList = new ArrayList<>();
            for (HostNode node : allNodes) {
                Map<String, Object> nodeInfo = new HashMap<>();
                nodeInfo.put("id", node.getId());
                nodeInfo.put("name", node.getName());
                nodeInfo.put("ip", node.getHostIp());
                nodeInfo.put("status", node.getStatus());
                nodeInfo.put("environment", node.getEnvironment());
                nodeList.add(nodeInfo);
            }
            statusInfo.put("nodes", nodeList);
            
            return ResponseEntity.ok(statusInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取主机节点状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新节点状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateNodeStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "状态参数不能为空"));
            }
            
            HostNode updatedNode = hostNodeService.updateNodeStatus(id, status);
            return ResponseEntity.ok(updatedNode);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "更新节点状态失败"));
        }
    }
    
    /**
     * 获取活跃节点列表
     */
    @GetMapping("/active")
    public ResponseEntity<List<HostNode>> getActiveNodes() {
        try {
            List<HostNode> activeNodes = hostNodeService.getActiveNodes();
            return ResponseEntity.ok(activeNodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取可用于部署的节点
     */
    @GetMapping("/available-for-deployment")
    public ResponseEntity<List<HostNode>> getAvailableNodesForDeployment() {
        try {
            List<HostNode> availableNodes = hostNodeService.getAvailableNodesForDeployment();
            return ResponseEntity.ok(availableNodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据环境类型获取节点
     */
    @GetMapping("/environment/{environment}")
    public ResponseEntity<List<HostNode>> getNodesByEnvironment(@PathVariable String environment) {
        try {
            List<HostNode> nodes = hostNodeService.getNodesByEnvironment(environment);
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据集群获取节点
     */
    @GetMapping("/cluster/{clusterName}")
    public ResponseEntity<List<HostNode>> getNodesByCluster(@PathVariable String clusterName) {
        try {
            List<HostNode> nodes = hostNodeService.getNodesByCluster(clusterName);
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 选择最佳部署节点
     */
    @GetMapping("/select-best")
    public ResponseEntity<?> selectBestNodeForDeployment(
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String preferredNodeName) {
        try {
            Optional<HostNode> bestNode = hostNodeService.selectBestNodeForDeployment(environment, preferredNodeName);
            
            if (bestNode.isPresent()) {
                return ResponseEntity.ok(bestNode.get());
            } else {
                return ResponseEntity.ok(Map.of("message", "没有找到可用的部署节点"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "选择部署节点失败"));
        }
    }
    
    /**
     * 获取节点状态统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getNodeStatusStatistics() {
        try {
            Map<String, Long> statistics = hostNodeService.getNodeStatusStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 执行节点健康检查
     */
    @PostMapping("/{id}/health-check")
    public ResponseEntity<?> performHealthCheck(@PathVariable Long id) {
        try {
            boolean isHealthy = hostNodeService.performHealthCheck(id);
            return ResponseEntity.ok(Map.of(
                "nodeId", id,
                "healthy", isHealthy,
                "message", isHealthy ? "节点健康" : "节点不可达"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "健康检查失败"));
        }
    }
    
    /**
     * 批量健康检查
     */
    @PostMapping("/health-check/batch")
    public ResponseEntity<?> batchHealthCheck(@RequestBody List<Long> nodeIds) {
        try {
            Map<Long, Boolean> results = nodeIds.stream()
                .collect(java.util.stream.Collectors.toMap(
                    id -> id,
                    id -> {
                        try {
                            return hostNodeService.performHealthCheck(id);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                ));
            
            return ResponseEntity.ok(Map.of("results", results));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "批量健康检查失败"));
        }
    }
    
    /**
     * 发现指定节点上的容器
     */
    @GetMapping("/{id}/containers")
    public ResponseEntity<?> discoverContainers(@PathVariable Long id) {
        try {
            Optional<HostNode> nodeOpt = hostNodeService.getNodeById(id);
            if (nodeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点不存在"));
            }
            
            HostNode node = nodeOpt.get();
            if (!node.isActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点未激活"));
            }
            
            List<ContainerInfo> containers = dockerService.getContainersOnNode(id);
            return ResponseEntity.ok(Map.of(
                "nodeId", id,
                "nodeName", node.getDisplayName(),
                "containers", containers,
                "totalCount", containers.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "容器发现失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有节点上的容器概览
     */
    @GetMapping("/containers/overview")
    public ResponseEntity<?> getContainersOverview() {
        try {
            List<HostNode> activeNodes = hostNodeService.getActiveNodes();
            Map<String, Object> overview = new java.util.HashMap<>();
            
            int totalContainers = 0;
            for (HostNode node : activeNodes) {
                try {
                    List<ContainerInfo> containers = dockerService.getContainersOnNode(node.getId());
                    overview.put("node_" + node.getId(), Map.of(
                        "nodeName", node.getDisplayName(),
                        "nodeIp", node.getHostIp(),
                        "containerCount", containers.size(),
                        "containers", containers
                    ));
                    totalContainers += containers.size();
                } catch (Exception e) {
                    overview.put("node_" + node.getId(), Map.of(
                        "nodeName", node.getDisplayName(),
                        "nodeIp", node.getHostIp(),
                        "containerCount", 0,
                        "error", "无法连接到节点"
                    ));
                }
            }
            
            overview.put("summary", Map.of(
                "totalNodes", activeNodes.size(),
                "totalContainers", totalContainers
            ));
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取容器概览失败: " + e.getMessage()));
        }
    }
    
    /**
     * 同步节点状态（强制更新数据库中的节点状态）
     */
    @PostMapping("/sync-status")
    public ResponseEntity<?> syncNodeStatus() {
        try {
            // 强制激活172.16.190.130:2375节点（根据日志显示该节点连接正常）
            boolean activated = hostNodeService.forceActivateNodeIfHealthy("172.16.190.130", 2375);
            
            // 执行批量健康检查
            Map<String, Object> result = hostNodeService.performBatchHealthCheckAndAutoActivate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("knownNodeActivated", activated);
            response.put("batchResult", result);
            response.put("message", "节点状态同步完成");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "节点状态同步失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 批量健康检查并自动激活可用节点
     */
    @PostMapping("/batch-health-check-activate")
    public ResponseEntity<?> batchHealthCheckAndActivate() {
        try {
            Map<String, Object> result = hostNodeService.performBatchHealthCheckAndAutoActivate();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "批量健康检查失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 强制激活指定IP和端口的节点（用于修复已知可用但未激活的节点）
     */
    @PostMapping("/force-activate")
    public ResponseEntity<?> forceActivateNode(@RequestBody Map<String, Object> request) {
        try {
            String hostIp = (String) request.get("hostIp");
            Integer dockerPort = (Integer) request.get("dockerPort");
            
            if (hostIp == null || dockerPort == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "请提供hostIp和dockerPort参数"
                ));
            }
            
            boolean success = hostNodeService.forceActivateNodeIfHealthy(hostIp, dockerPort);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "节点强制激活成功",
                    "hostIp", hostIp,
                    "dockerPort", dockerPort
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "节点强制激活失败，可能节点不存在或不健康"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "强制激活节点失败: " + e.getMessage()
            ));
        }
    }
    @GetMapping("/{id}/docker-status")
    public ResponseEntity<?> checkDockerStatus(@PathVariable Long id) {
        try {
            boolean dockerAvailable = dockerService.isDockerAvailableOnNode(id);
            return ResponseEntity.ok(Map.of(
                "nodeId", id,
                "dockerAvailable", dockerAvailable,
                "message", dockerAvailable ? "Docker服务正常" : "Docker服务不可用"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "检查Docker状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目关联的节点列表
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<HostNode>> getProjectNodes(@PathVariable String projectId) {
        try {
            // 这里简化实现，实际项目中应该有项目-节点关联表
            // 目前返回所有节点，后续可以根据项目ID筛选
            List<HostNode> allNodes = hostNodeService.getAllNodes();
            return ResponseEntity.ok(allNodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取项目节点统计
     */
    @GetMapping("/project/{projectId}/statistics")
    public ResponseEntity<Map<String, Object>> getProjectNodeStatistics(@PathVariable String projectId) {
        try {
            // 简化实现，实际项目中应该根据项目ID获取关联节点
            List<HostNode> allNodes = hostNodeService.getAllNodes();
            
            Map<String, Object> stats = new java.util.HashMap<>();
            long active = allNodes.stream().filter(node -> "active".equals(node.getStatus())).count();
            long inactive = allNodes.stream().filter(node -> "inactive".equals(node.getStatus())).count();
            long maintenance = allNodes.stream().filter(node -> "maintenance".equals(node.getStatus())).count();
            
            stats.put("active", active);
            stats.put("inactive", inactive);
            stats.put("maintenance", maintenance);
            stats.put("containers", 0); // 暂时设为0，可以后续从容器服务获取
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 为项目添加节点关联
     */
    @PostMapping("/project/{projectId}/add-nodes")
    public ResponseEntity<?> addNodesToProject(@PathVariable String projectId, @RequestBody List<Long> nodeIds) {
        try {
            // 简化实现，实际项目中应该在项目-节点关联表中添加记录
            // 这里只是验证节点是否存在
            List<HostNode> validNodes = new java.util.ArrayList<>();
            for (Long nodeId : nodeIds) {
                Optional<HostNode> node = hostNodeService.getNodeById(nodeId);
                if (node.isPresent()) {
                    validNodes.add(node.get());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "成功关联 " + validNodes.size() + " 个节点",
                "associatedNodes", validNodes
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "关联节点失败: " + e.getMessage()));
        }
    }

    /**
     * 移除项目节点关联
     */
    @DeleteMapping("/project/{projectId}/remove-node/{nodeId}")
    public ResponseEntity<?> removeNodeFromProject(@PathVariable String projectId, @PathVariable Long nodeId) {
        try {
            // 简化实现，实际项目中应该从项目-节点关联表中删除记录
            Optional<HostNode> node = hostNodeService.getNodeById(nodeId);
            if (node.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点不存在"));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "成功移除节点关联"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "移除关联失败: " + e.getMessage()));
        }
    }

    /**
     * 网络诊断 - 测试指定节点的网络连通性和Docker API可用性
     */
    @PostMapping("/{id}/network-diagnosis")
    public ResponseEntity<?> performNetworkDiagnosis(@PathVariable Long id) {
        try {
            Optional<HostNode> nodeOpt = hostNodeService.getNodeById(id);
            if (nodeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "节点不存在"));
            }
            
            HostNode node = nodeOpt.get();
            
            // 执行网络诊断
            NetworkTestUtil.NetworkDiagnosticResult result = 
                NetworkTestUtil.diagnoseConnection(node.getHostIp(), node.getDockerPort());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("nodeId", id);
            response.put("nodeName", node.getDisplayName());
            response.put("nodeIp", node.getHostIp());
            response.put("dockerPort", node.getDockerPort());
            response.put("timestamp", result.getTimestamp());
            response.put("pingSuccess", result.isPingSuccess());
            response.put("tcpConnectionSuccess", result.isTcpConnectionSuccess());
            response.put("dockerApiSuccess", result.isDockerApiSuccess());
            response.put("overallStatus", result.getOverallStatus());
            response.put("recommendation", result.getRecommendation());
            
            // 根据诊断结果更新节点状态
            String newStatus;
            switch (result.getOverallStatus()) {
                case "SUCCESS":
                    newStatus = "active";
                    break;
                case "PARTIAL":
                    newStatus = "docker_unavailable";
                    break;
                case "NETWORK_ISSUE":
                    newStatus = "ping_ok_docker_fail";
                    break;
                default:
                    newStatus = "offline";
                    break;
            }
            
            hostNodeService.updateNodeStatus(id, newStatus);
            response.put("updatedStatus", newStatus);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "网络诊断失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有节点负载信息
     */
    @GetMapping("/load-info")
    public ResponseEntity<?> getAllNodesLoadInfo() {
        try {
            List<Map<String, Object>> loadInfoList = hostNodeService.getAllNodesLoadInfo();
            return ResponseEntity.ok(loadInfoList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取节点负载信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个节点负载信息
     */
    @GetMapping("/{id}/load-info")
    public ResponseEntity<?> getNodeLoadInfo(@PathVariable Long id) {
        try {
            Map<String, Object> loadInfo = hostNodeService.getNodeLoadInfo(id);
            return ResponseEntity.ok(loadInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取节点负载信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取活跃节点负载信息（按负载排序）
     */
    @GetMapping("/active/load-info")
    public ResponseEntity<?> getActiveNodesLoadInfoSorted() {
        try {
            List<Map<String, Object>> loadInfoList = hostNodeService.getActiveNodesLoadInfoSorted();
            return ResponseEntity.ok(loadInfoList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取活跃节点负载信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 智能推荐部署节点
     */
    @GetMapping("/recommend-deployment")
    public ResponseEntity<?> recommendDeploymentNodes(
            @RequestParam(required = false) String environment,
            @RequestParam(defaultValue = "3") int count) {
        try {
            List<HostNode> recommendedNodes = hostNodeService.recommendDeploymentNodes(environment, count);
            return ResponseEntity.ok(Map.of(
                "recommendedNodes", recommendedNodes,
                "count", recommendedNodes.size(),
                "environment", environment != null ? environment : "any"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取推荐节点失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取负载均衡推荐节点
     */
    @GetMapping("/load-balanced-node")
    public ResponseEntity<?> getLoadBalancedNode(@RequestParam(required = false) String environment) {
        try {
            Optional<HostNode> node = hostNodeService.getLoadBalancedNode(environment);
            if (node.isPresent()) {
                return ResponseEntity.ok(node.get());
            } else {
                return ResponseEntity.ok(Map.of("message", "没有找到合适的负载均衡节点"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取负载均衡节点失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取节点容量预警信息
     */
    @GetMapping("/capacity-alerts")
    public ResponseEntity<?> getNodeCapacityAlerts() {
        try {
            List<Map<String, Object>> alerts = hostNodeService.getNodeCapacityAlerts();
            return ResponseEntity.ok(Map.of(
                "alerts", alerts,
                "alertCount", alerts.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取容量预警失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取集群负载统计
     */
    @GetMapping("/cluster-statistics")
    public ResponseEntity<?> getClusterLoadStatistics() {
        try {
            Map<String, Object> stats = hostNodeService.getClusterLoadStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取集群统计失败: " + e.getMessage()));
        }
    }

    /**
     * 批量网络诊断 - 测试所有节点或指定节点列表的网络状态
     */
    @PostMapping("/network-diagnosis/batch")
    public ResponseEntity<?> performBatchNetworkDiagnosis(@RequestBody(required = false) List<Long> nodeIds) {
        try {
            List<HostNode> nodesToTest;
            
            if (nodeIds != null && !nodeIds.isEmpty()) {
                // 测试指定的节点
                nodesToTest = new java.util.ArrayList<>();
                for (Long nodeId : nodeIds) {
                    hostNodeService.getNodeById(nodeId).ifPresent(nodesToTest::add);
                }
            } else {
                // 测试所有节点
                nodesToTest = hostNodeService.getAllNodes();
            }
            
            Map<String, Object> batchResult = new java.util.HashMap<>();
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            
            int successCount = 0;
            int failedCount = 0;
            
            for (HostNode node : nodesToTest) {
                NetworkTestUtil.NetworkDiagnosticResult result = 
                    NetworkTestUtil.diagnoseConnection(node.getHostIp(), node.getDockerPort());
                
                Map<String, Object> nodeResult = new java.util.HashMap<>();
                nodeResult.put("nodeId", node.getId());
                nodeResult.put("nodeName", node.getDisplayName());
                nodeResult.put("nodeIp", node.getHostIp());
                nodeResult.put("dockerPort", node.getDockerPort());
                nodeResult.put("pingSuccess", result.isPingSuccess());
                nodeResult.put("tcpConnectionSuccess", result.isTcpConnectionSuccess());
                nodeResult.put("dockerApiSuccess", result.isDockerApiSuccess());
                nodeResult.put("overallStatus", result.getOverallStatus());
                nodeResult.put("recommendation", result.getRecommendation());
                
                results.add(nodeResult);
                
                // 统计结果
                if (result.isDockerApiSuccess()) {
                    successCount++;
                } else {
                    failedCount++;
                }
                
                // 更新节点状态
                String newStatus;
                switch (result.getOverallStatus()) {
                    case "SUCCESS":
                        newStatus = "active";
                        break;
                    case "PARTIAL":
                        newStatus = "docker_unavailable";
                        break;
                    case "NETWORK_ISSUE":
                        newStatus = "ping_ok_docker_fail";
                        break;
                    default:
                        newStatus = "offline";
                        break;
                }
                hostNodeService.updateNodeStatus(node.getId(), newStatus);
            }
            
            batchResult.put("totalTested", nodesToTest.size());
            batchResult.put("successCount", successCount);
            batchResult.put("failedCount", failedCount);
            batchResult.put("timestamp", System.currentTimeMillis());
            batchResult.put("results", results);
            
            return ResponseEntity.ok(batchResult);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "批量网络诊断失败: " + e.getMessage()));
        }
    }

    
    private String getNodeStatusDisplay(String status) {
        if (status == null) return "unknown";
        
        switch (status.toLowerCase()) {
            case "active":
            case "online":
                return "active";
            case "offline":
            case "unreachable":
                return "inactive";
            case "maintenance":
                return "maintenance";
            default:
                return "inactive";
        }
    }
    
    private int generateRandomLoad() {
        return (int) (Math.random() * 100);
    }
    
    private int getNodeContainerCount(Long nodeId) {
        try {
            List<ContainerInfo> containers = dockerService.getContainersOnNode(nodeId);
            return containers.size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String formatLastHeartbeat(HostNode node) {
        // 简化实现，实际应该基于最后心跳时间
        if ("active".equalsIgnoreCase(node.getStatus())) {
            return "刚刚";
        } else {
            return "5分钟前";
        }
    }
}