package org.cyberlab.repository;

import org.cyberlab.entity.DrillContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrillContainerRepository extends JpaRepository<DrillContainer, Long> {
    
    /**
     * 根据演练ID查找所有容器
     */
    List<DrillContainer> findByRangeId(Long rangeId);
    
    /**
     * 根据容器名称查找
     */
    Optional<DrillContainer> findByRangeIdAndName(Long rangeId, String name);
    
    /**
     * 根据Docker容器ID查找
     */
    Optional<DrillContainer> findByContainerId(String containerId);
    
    /**
     * 根据状态查找容器
     */
    List<DrillContainer> findByStatus(String status);
    
    /**
     * 根据演练ID和状态查找容器
     */
    List<DrillContainer> findByRangeIdAndStatus(Long rangeId, String status);
    
    /**
     * 统计演练下的容器数量
     */
    @Query("SELECT COUNT(dc) FROM DrillContainer dc WHERE dc.rangeId = :rangeId")
    long countByRangeId(@Param("rangeId") Long rangeId);
    
    /**
     * 统计运行中的容器数量
     */
    @Query("SELECT COUNT(dc) FROM DrillContainer dc WHERE dc.rangeId = :rangeId AND dc.status = 'running'")
    long countRunningByRangeId(@Param("rangeId") Long rangeId);
    
    /**
     * 根据状态统计容器数量
     */
    long countByStatus(String status);
    
    /**
     * 根据主机节点ID查找容器
     */
    List<DrillContainer> findByHostNodeId(Long hostNodeId);
    
    /**
     * 根据主机节点ID统计容器数量
     */
    long countByHostNodeId(Long hostNodeId);
    
    /**
     * 根据主机节点ID和状态统计容器数量
     */
    @Query("SELECT COUNT(dc) FROM DrillContainer dc WHERE dc.hostNodeId = :hostNodeId AND dc.status = :status")
    long countByHostNodeIdAndStatus(@Param("hostNodeId") Long hostNodeId, @Param("status") String status);
    
    /**
     * 根据资产ID查找容器
     */
    List<DrillContainer> findByAssetId(Long assetId);
    
    /**
     * 根据资产ID统计容器数量
     */
    long countByAssetId(Long assetId);

    /**
     * 根据演练ID和资产ID查找容器（用于日志查询）
     */
    List<DrillContainer> findByRangeIdAndAssetId(Long rangeId, Long assetId);
}