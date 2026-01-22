package org.cyberlab.repository;

import org.cyberlab.entity.HostNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostNodeRepository extends JpaRepository<HostNode, Long> {
    
    /**
     * 根据节点名称查找
     */
    Optional<HostNode> findByName(String name);
    
    /**
     * 根据状态查找节点列表
     */
    List<HostNode> findByStatus(String status);
    
    /**
     * 查找所有活跃的节点
     */
    List<HostNode> findByStatusOrderByPriorityDesc(String status);
    
    /**
     * 根据环境类型查找节点
     */
    List<HostNode> findByEnvironment(String environment);
    
    /**
     * 根据集群名称查找节点
     */
    List<HostNode> findByClusterName(String clusterName);
    
    /**
     * 根据节点类型查找
     */
    List<HostNode> findByNodeType(String nodeType);
    
    /**
     * 查找启用监控的节点
     */
    List<HostNode> findByMonitoringEnabledTrue();
    
    /**
     * 查找优先级最高的活跃节点
     */
    @Query("SELECT h FROM HostNode h WHERE h.status = 'active' ORDER BY h.priority DESC, h.id ASC")
    List<HostNode> findTopPriorityActiveNodes();
    
    /**
     * 统计各状态的节点数量
     */
    @Query("SELECT h.status, COUNT(h) FROM HostNode h GROUP BY h.status")
    List<Object[]> countNodesByStatus();
    
    /**
     * 根据IP地址查找节点
     */
    Optional<HostNode> findByHostIp(String hostIp);
    
    /**
     * 根据IP地址查找所有节点（用于重复检查）
     */
    List<HostNode> findAllByHostIp(String hostIp);
    
    /**
     * 检查节点是否存在（根据名称和IP）
     */
    boolean existsByNameAndHostIp(String name, String hostIp);
    
    /**
     * 查找可用于部署的节点（活跃且未达到最大容器数）
     */
    @Query(value = "SELECT h.* FROM host_nodes h " +
           "LEFT JOIN (SELECT host_node_id, COUNT(*) as container_count " +
           "FROM drill_containers WHERE status IN ('running', 'deploying') " +
           "GROUP BY host_node_id) c ON h.id = c.host_node_id " +
           "WHERE h.status = 'active' " +
           "AND (c.container_count IS NULL OR c.container_count < h.max_containers) " +
           "ORDER BY h.priority DESC, COALESCE(c.container_count, 0) ASC", 
           nativeQuery = true)
    List<HostNode> findAvailableNodesForDeployment();
    
    /**
     * 根据环境和状态查找节点
     */
    List<HostNode> findByEnvironmentAndStatus(String environment, String status);
}