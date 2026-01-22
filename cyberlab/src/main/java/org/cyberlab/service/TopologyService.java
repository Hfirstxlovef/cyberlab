package org.cyberlab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.cyberlab.dto.TopologyData;
import org.cyberlab.entity.TopologyEntity;
import org.cyberlab.repository.TopologyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TopologyService {

    private static final Logger logger = LoggerFactory.getLogger(TopologyService.class);

    @Resource
    private TopologyRepository repository;

    @Resource
    private ObjectMapper objectMapper;

    // ✅ 图标映射（type -> 英文 iconName，与前端 iconMap.js 匹配）
    private static final Map<String, String> ICON_MAP = Map.of(
            "server", "storage_server",
            "firewall", "firewall",
            "dns", "dns",
            "pc", "laptop",
            "router", "main_switch",
            "database", "database",
            "mail", "mail_server",
            "switch_fiber", "fiber_switch",
            "switch_ethernet", "ethernet_switch",
            "web", "webserver"
            // 可继续扩展
    );

    @Transactional(rollbackFor = Exception.class)
    public void save(TopologyData data) throws JsonProcessingException {
        logger.info("开始保存拓扑数据，projectId: {}", data.getProjectId());
        
        // 🔧 添加图标数据调试日志
        if (data.getNodes() != null && !data.getNodes().isEmpty()) {
            logger.info("节点图标数据调试:");
            for (Object nodeObj : data.getNodes()) {
                if (nodeObj instanceof Map) {
                    Map<String, Object> node = (Map<String, Object>) nodeObj;
                    logger.info("节点 {}: name={}, iconName={}, symbol={}", 
                        node.get("id"), 
                        node.get("name"),
                        node.get("iconName"),
                        node.get("symbol")
                    );
                }
            }
        }
        
        try {
            Optional<TopologyEntity> existing = repository.findByProjectId(data.getProjectId());

            TopologyEntity entity = existing.orElse(new TopologyEntity());
            entity.setProjectId(data.getProjectId());
            entity.setNodes(objectMapper.writeValueAsString(data.getNodes()));
            entity.setLinks(objectMapper.writeValueAsString(data.getLinks()));
            entity.setCustomElements(objectMapper.writeValueAsString(data.getCustomElements()));

            repository.save(entity);
            
            logger.info("拓扑数据保存成功，projectId: {}, 节点数: {}, 连线数: {}, 自定义元素数: {}", 
                data.getProjectId(),
                data.getNodes() != null ? data.getNodes().size() : 0,
                data.getLinks() != null ? data.getLinks().size() : 0,
                data.getCustomElements() != null ? data.getCustomElements().size() : 0
            );
        } catch (Exception e) {
            logger.error("保存拓扑数据失败，projectId: {}, 错误: {}", data.getProjectId(), e.getMessage(), e);
            throw e;  // 重新抛出异常，触发事务回滚
        }
    }

    public TopologyData loadByProjectId(String projectId) throws JsonProcessingException {
        logger.info("开始加载拓扑数据，projectId: {}", projectId);
        
        try {
            Optional<TopologyEntity> optional = repository.findByProjectId(projectId);
            if (optional.isEmpty()) {
                logger.warn("未找到拓扑数据，projectId: {}", projectId);
                return null;
            }

            TopologyEntity entity = optional.get();
            TopologyData data = new TopologyData();
            data.setProjectId(projectId);

            // ✅ 优化节点图标处理逻辑：优先保留用户自定义的图标，只为缺失的节点设置默认图标
            List<Map<String, Object>> nodeList = objectMapper.readValue(
                    entity.getNodes(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            logger.info("加载拓扑数据后的节点图标处理:");
            
            for (Map<String, Object> node : nodeList) {
                String nodeId = (String) node.get("id");
                String existingIconName = (String) node.get("iconName");
                String existingSymbol = (String) node.get("symbol");
                
                logger.info("处理节点 {}: 现有iconName={}, 现有symbol={}", nodeId, existingIconName, existingSymbol);
                
                // 如果节点已经有有效的图标数据，保留用户的选择
                if (existingIconName != null && !existingIconName.isEmpty() && 
                    existingSymbol != null && !existingSymbol.isEmpty()) {
                    logger.info("节点 {} 保留现有图标: iconName={}, symbol={}", nodeId, existingIconName, existingSymbol);
                    // 保持现有图标不变
                    continue;
                }
                
                // 只有当图标数据缺失时，才根据type设置默认图标
                Object typeObj = node.get("type");
                if (typeObj != null) {
                    String defaultIconName = ICON_MAP.getOrDefault(typeObj.toString().toLowerCase(), "pc");
                    String defaultSymbol = "image://icons/" + defaultIconName + ".png";
                    
                    node.put("iconName", defaultIconName);
                    node.put("symbol", defaultSymbol);
                    
                    logger.info("节点 {} 设置默认图标: iconName={}, symbol={}", nodeId, defaultIconName, defaultSymbol);
                } else {
                    logger.warn("节点 {} 缺少type信息，无法设置默认图标", nodeId);
                }
            }

            List<Map<String, Object>> linkList = objectMapper.readValue(
                    entity.getLinks(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> customElementList = objectMapper.readValue(
                    entity.getCustomElements(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            data.setNodes(nodeList);
            data.setLinks(linkList);
            data.setCustomElements(customElementList);

            logger.info("拓扑数据加载成功，projectId: {}, 节点数: {}, 连线数: {}, 自定义元素数: {}", 
                projectId, nodeList.size(), linkList.size(), customElementList.size());

            return data;
        } catch (Exception e) {
            logger.error("加载拓扑数据失败，projectId: {}, 错误: {}", projectId, e.getMessage(), e);
            throw e;
        }
    }
}