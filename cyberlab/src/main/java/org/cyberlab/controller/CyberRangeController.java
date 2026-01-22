package org.cyberlab.controller;

import org.cyberlab.dto.TopologyData;
import org.cyberlab.entity.CyberRange;
import org.cyberlab.repository.CyberRangeRepository;
import org.cyberlab.service.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/range")
@CrossOrigin(origins = "*") // 允许跨域访问
public class CyberRangeController {

    @Autowired
    private CyberRangeRepository rangeRepo;

    @Autowired
    private TopologyService topologyService;

    // ✅ 获取所有演练
    @GetMapping
    public List<CyberRange> getAllRanges() {
        return rangeRepo.findAll();
    }

    // ✅ 获取正在运行的演练（为成果提交页面提供数据）
    @GetMapping("/active")
    public List<CyberRange> getActiveRanges() {
        return rangeRepo.findByStatus("running");
    }

    // ✅ 获取单个演练详情（为 /api/range/{id} 提供支持）
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        CyberRange range = rangeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到对应的演练 ID: " + id));
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", range.getId());
        response.put("name", range.getName());
        response.put("status", range.getStatus());
        response.put("description", range.getDescription());
        response.put("drillType", range.getDrillType());
        response.put("difficultyLevel", range.getDifficultyLevel());
        response.put("startTime", range.getStartTime());
        response.put("endTime", range.getEndTime());
        response.put("maxParticipants", range.getMaxParticipants());
        response.put("creatorId", range.getCreatorId());
        response.put("createdAt", range.getCreatedAt());
        response.put("updatedAt", range.getUpdatedAt());
        response.put("topologyConfig", range.getTopologyConfig());
        response.put("vulnerabilityConfig", range.getVulnerabilityConfig());
        response.put("topologyProjectId", range.getTopologyProjectId());
        
        // 集成拓扑数据
        if (range.getTopologyProjectId() != null && !range.getTopologyProjectId().trim().isEmpty()) {
            try {
                TopologyData topologyData = topologyService.loadByProjectId(range.getTopologyProjectId());
                if (topologyData != null) {
                    response.put("topologyData", topologyData);
                } else {
                    response.put("topologyData", null);
                    response.put("topologyError", "指定的拓扑项目不存在或为空");
                }
            } catch (Exception e) {
                response.put("topologyData", null);
                response.put("topologyError", "拓扑数据加载失败: " + e.getMessage());
                // 记录错误日志但不中断响应
                // Debug statement removed
            }
        } else {
            response.put("topologyData", null);
            response.put("topologyInfo", "该演练尚未关联拓扑项目");
        }
        
        return ResponseEntity.ok(response);
    }

    // ✅ 创建演练
    @PostMapping("/create")
    public CyberRange create(@RequestBody CyberRange range) {
        range.setStatus("running");
        range.setCreatedAt(LocalDateTime.now());
        range.setUpdatedAt(LocalDateTime.now());
        return rangeRepo.save(range);
    }

    // ✅ 更新演练基本信息
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRange(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Optional<CyberRange> optional = rangeRepo.findById(id);
        if (optional.isPresent()) {
            CyberRange range = optional.get();
            
            // 更新基本信息字段
            if (updates.containsKey("name")) {
                range.setName((String) updates.get("name"));
            }
            if (updates.containsKey("description")) {
                range.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("drillType")) {
                range.setDrillType((String) updates.get("drillType"));
            }
            if (updates.containsKey("difficultyLevel")) {
                range.setDifficultyLevel((String) updates.get("difficultyLevel"));
            }
            if (updates.containsKey("maxParticipants")) {
                range.setMaxParticipants((Integer) updates.get("maxParticipants"));
            }
            if (updates.containsKey("topologyProjectId")) {
                range.setTopologyProjectId((String) updates.get("topologyProjectId"));
            }
            
            range.setUpdatedAt(LocalDateTime.now());
            CyberRange savedRange = rangeRepo.save(range);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "演练信息更新成功");
            response.put("data", savedRange);
            
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "未找到对应的演练 ID: " + id);
        return ResponseEntity.notFound().build();
    }
    
    // ✅ 检查拓扑项目状态
    @GetMapping("/{id}/topology-status")
    public ResponseEntity<Map<String, Object>> checkTopologyStatus(@PathVariable Long id) {
        Optional<CyberRange> optional = rangeRepo.findById(id);
        if (optional.isPresent()) {
            CyberRange range = optional.get();
            Map<String, Object> response = new HashMap<>();
            
            if (range.getTopologyProjectId() != null && !range.getTopologyProjectId().trim().isEmpty()) {
                try {
                    TopologyData topologyData = topologyService.loadByProjectId(range.getTopologyProjectId());
                    if (topologyData != null) {
                        int nodeCount = topologyData.getNodes() != null ? topologyData.getNodes().size() : 0;
                        int linkCount = topologyData.getLinks() != null ? topologyData.getLinks().size() : 0;
                        int elementCount = topologyData.getCustomElements() != null ? topologyData.getCustomElements().size() : 0;
                        
                        response.put("hasTopology", true);
                        response.put("topologyProjectId", range.getTopologyProjectId());
                        response.put("nodeCount", nodeCount);
                        response.put("linkCount", linkCount);
                        response.put("customElementCount", elementCount);
                        response.put("totalElements", nodeCount + linkCount + elementCount);
                        response.put("isEmpty", nodeCount == 0 && linkCount == 0 && elementCount == 0);
                    } else {
                        response.put("hasTopology", false);
                        response.put("topologyProjectId", range.getTopologyProjectId());
                        response.put("message", "拓扑项目存在但数据为空");
                    }
                } catch (Exception e) {
                    response.put("hasTopology", false);
                    response.put("topologyProjectId", range.getTopologyProjectId());
                    response.put("error", "拓扑数据加载失败: " + e.getMessage());
                }
            } else {
                response.put("hasTopology", false);
                response.put("topologyProjectId", null);
                response.put("message", "演练未关联拓扑项目");
            }
            
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.notFound().build();
    }

    // ✅ 暂停演练
    @PutMapping("/pause/{id}")
    public CyberRange pause(@PathVariable Long id) {
        Optional<CyberRange> optional = rangeRepo.findById(id);
        if (optional.isPresent()) {
            CyberRange range = optional.get();
            range.setStatus("paused");
            range.setUpdatedAt(LocalDateTime.now());
            return rangeRepo.save(range);
        }
        throw new RuntimeException("未找到对应的演练 ID: " + id);
    }

    // ✅ 启动/恢复演练
    @PutMapping("/start/{id}")
    public CyberRange start(@PathVariable Long id) {
        Optional<CyberRange> optional = rangeRepo.findById(id);
        if (optional.isPresent()) {
            CyberRange range = optional.get();
            range.setStatus("running");
            range.setUpdatedAt(LocalDateTime.now());
            return rangeRepo.save(range);
        }
        throw new RuntimeException("未找到对应的演练 ID: " + id);
    }

    // ✅ 停止演练
    @PutMapping("/stop/{id}")
    public CyberRange stop(@PathVariable Long id) {
        Optional<CyberRange> optional = rangeRepo.findById(id);
        if (optional.isPresent()) {
            CyberRange range = optional.get();
            range.setStatus("stopped");
            range.setUpdatedAt(LocalDateTime.now());
            return rangeRepo.save(range);
        }
        throw new RuntimeException("未找到对应的演练 ID: " + id);
    }

    // ✅ 删除演练
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        rangeRepo.deleteById(id);
    }
}