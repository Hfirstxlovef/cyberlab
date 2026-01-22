package org.cyberlab.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TopologyData {
    private String projectId;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> links;
    private List<Map<String, Object>> customElements;

    // 手动添加getter和setter方法以确保兼容性
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public List<Map<String, Object>> getLinks() {
        return links;
    }

    public void setLinks(List<Map<String, Object>> links) {
        this.links = links;
    }

    public List<Map<String, Object>> getCustomElements() {
        return customElements;
    }

    public void setCustomElements(List<Map<String, Object>> customElements) {
        this.customElements = customElements;
    }
}