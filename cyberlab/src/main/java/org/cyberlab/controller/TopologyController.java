package org.cyberlab.controller;

import jakarta.annotation.Resource;
import org.cyberlab.dto.TopologyData;
import org.cyberlab.service.TopologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topology")
public class TopologyController {

    @Resource
    private TopologyService service;

    // ✅ 保存拓扑图：包含 projectId
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody TopologyData data) throws Exception {
        // Debug statement removed
        if (data.getProjectId() == null || data.getProjectId().isEmpty()) {
            // Debug statement removed
            throw new IllegalArgumentException("projectId 不能为空");
        }

        // Debug statement removed
        // Debug statement removed
        // Debug statement removed
        // Debug statement removed

        service.save(data);

        // Debug statement removed
        return ResponseEntity.ok().build();
    }

    // ✅ 加载拓扑图：通过 URL 传 projectId
    @GetMapping("/load")
    public TopologyData load(@RequestParam("projectId") String projectId) throws Exception {
        // Debug statement removed
        TopologyData result = service.loadByProjectId(projectId);
        if (result == null) {
            // Debug statement removed
        } else {
            // Debug statement removed
        }
        return result;
    }
}