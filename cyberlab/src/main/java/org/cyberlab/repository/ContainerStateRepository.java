package org.cyberlab.repository;

import org.cyberlab.entity.ContainerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerStateRepository extends JpaRepository<ContainerState, Long> {

    /**
     * 根据资产ID查找容器状态
     */
    List<ContainerState> findByAssetId(Long assetId);

    /**
     * 根据容器ID查找容器状态
     */
    Optional<ContainerState> findByContainerId(String containerId);

    /**
     * 根据资产ID和容器ID查找容器状态
     */
    Optional<ContainerState> findByAssetIdAndContainerId(Long assetId, String containerId);

    /**
     * 根据主机节点ID查找容器状态
     */
    List<ContainerState> findByHostNodeId(Long hostNodeId);

    /**
     * 查找需要调和的容器状态
     */
    @Query("SELECT cs FROM ContainerState cs WHERE " +
           "cs.desiredStatus != cs.currentStatus AND " +
           "cs.syncStatus IN ('OUT_OF_SYNC', 'FAILED') AND " +
           "cs.syncAttempts < cs.maxSyncAttempts")
    List<ContainerState> findNeedingReconciliation();

    /**
     * 查找同步状态为指定值的容器状态
     */
    List<ContainerState> findBySyncStatus(String syncStatus);

    /**
     * 查找健康状态为指定值的容器状态
     */
    List<ContainerState> findByHealthStatus(String healthStatus);

    /**
     * 查找长时间未同步的容器状态
     */
    @Query("SELECT cs FROM ContainerState cs WHERE " +
           "cs.lastSyncAt IS NULL OR cs.lastSyncAt < :threshold")
    List<ContainerState> findNotSyncedSince(@Param("threshold") LocalDateTime threshold);

    /**
     * 查找特定主机节点上需要调和的容器状态
     */
    @Query("SELECT cs FROM ContainerState cs WHERE " +
           "cs.hostNodeId = :hostNodeId AND " +
           "cs.desiredStatus != cs.currentStatus AND " +
           "cs.syncStatus IN ('OUT_OF_SYNC', 'FAILED') AND " +
           "cs.syncAttempts < cs.maxSyncAttempts")
    List<ContainerState> findNeedingReconciliationByHostNode(@Param("hostNodeId") Long hostNodeId);

    /**
     * 统计各种同步状态的数量
     */
    @Query("SELECT cs.syncStatus, COUNT(cs) FROM ContainerState cs GROUP BY cs.syncStatus")
    List<Object[]> countBySyncStatus();

    /**
     * 统计指定同步状态的数量
     */
    long countBySyncStatus(String syncStatus);

    /**
     * 统计各种健康状态的数量
     */
    @Query("SELECT cs.healthStatus, COUNT(cs) FROM ContainerState cs GROUP BY cs.healthStatus")
    List<Object[]> countByHealthStatus();

    /**
     * 查找同步失败的容器状态
     */
    @Query("SELECT cs FROM ContainerState cs WHERE " +
           "cs.syncStatus = 'FAILED' OR " +
           "cs.syncAttempts >= cs.maxSyncAttempts")
    List<ContainerState> findFailedSyncs();

    /**
     * 清理超过指定时间的已完成状态记录
     */
    @Query("DELETE FROM ContainerState cs WHERE " +
           "cs.syncStatus = 'SYNCED' AND " +
           "cs.updatedAt < :threshold")
    void deleteOldSyncedStates(@Param("threshold") LocalDateTime threshold);

    /**
     * 根据创建者查找容器状态
     */
    List<ContainerState> findByCreatedBy(String createdBy);

    /**
     * 查找指定时间范围内的容器状态
     */
    @Query("SELECT cs FROM ContainerState cs WHERE " +
           "cs.createdAt BETWEEN :startTime AND :endTime")
    List<ContainerState> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查找当前正在同步的容器状态
     */
    List<ContainerState> findBySyncStatusAndLastSyncAtAfter(String syncStatus, LocalDateTime threshold);
}