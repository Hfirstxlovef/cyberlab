package org.cyberlab.repository;

import org.cyberlab.entity.ContainerDiscoveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 容器探测记录Repository
 * 支持增量同步操作：查询、新增、更新、批量删除
 */
@Repository
public interface ContainerDiscoveryRecordRepository extends JpaRepository<ContainerDiscoveryRecord, Long> {

    /**
     * 根据项目ID查找所有容器记录
     * @param projectId 项目ID
     * @return 容器记录列表
     */
    List<ContainerDiscoveryRecord> findByProjectId(String projectId);

    /**
     * 根据项目ID和容器ID查找唯一记录
     * @param projectId 项目ID
     * @param containerId 容器ID
     * @return 容器记录（可选）
     */
    Optional<ContainerDiscoveryRecord> findByProjectIdAndContainerId(String projectId, String containerId);

    /**
     * 批量删除指定项目的指定容器记录
     * 用于删除探测时不存在的容器（已销毁的容器）
     * @param projectId 项目ID
     * @param containerIds 要删除的容器ID列表
     */
    @Modifying
    @Query("DELETE FROM ContainerDiscoveryRecord WHERE projectId = :projectId AND containerId IN :containerIds")
    void deleteByProjectIdAndContainerIdIn(@Param("projectId") String projectId, @Param("containerIds") List<String> containerIds);

    /**
     * 根据资产ID查找容器记录
     * @param assetId 资产ID
     * @return 容器记录列表
     */
    List<ContainerDiscoveryRecord> findByAssetId(Long assetId);

    /**
     * 统计项目的容器数量
     * @param projectId 项目ID
     * @return 容器数量
     */
    long countByProjectId(String projectId);

    /**
     * 删除项目的所有容器记录
     * @param projectId 项目ID
     */
    void deleteByProjectId(String projectId);
}
