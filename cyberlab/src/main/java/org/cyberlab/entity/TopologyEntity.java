package org.cyberlab.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "topology_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = "projectId")
})
@Data
public class TopologyEntity {

    private static final Logger logger = LoggerFactory.getLogger(TopologyEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String nodes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String links;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String customElements;

    // 手动添加getter和setter方法以确保兼容性
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getLinks() {
        return links;
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public String getCustomElements() {
        return customElements;
    }

    public void setCustomElements(String customElements) {
        this.customElements = customElements;
    }

    // ✅ 静态图标映射表
    private static final Map<String, String> ICON_MAP = new HashMap<>() {{
        put("server", "服务器");
        put("firewall", "防火墙");
        put("DNS", "DNS");
        put("pc", "笔记本");
        put("router", "router");
        put("database", "database");
        // 你可以继续添加更多类型
    }};

    /**
     * ✅ 工具方法：返回带 iconName 字段的节点 JSON 字符串
     */
    public String getNodesWithIconName() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> nodeList = mapper.readValue(this.nodes, new TypeReference<>() {
            });

            for (Map<String, Object> node : nodeList) {
                Object typeObj = node.get("type");
                if (typeObj != null) {
                    String type = typeObj.toString();
                    String iconName = ICON_MAP.getOrDefault(type, "default");
                    node.put("iconName", iconName);
                }
            }

            return mapper.writeValueAsString(nodeList);
        } catch (Exception e) {
            logger.error("处理节点图标名称失败", e);
            return this.nodes;
        }
    }
}