package org.cyberlab.repository;

import org.cyberlab.entity.DrillAssetImageMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 演练资产镜像映射Repository
 */
@Repository
public interface DrillAssetImageMappingRepository extends JpaRepository<DrillAssetImageMapping, Long> {

    /**
     * 根据演练ID查找所有映射
     */
    List<DrillAssetImageMapping> findByRangeId(Long rangeId);

    /**
     * 根据演练ID和资产ID查找所有映射
     */
    List<DrillAssetImageMapping> findByRangeIdAndAssetId(Long rangeId, Long assetId);

    /**
     * 精确查找单个映射（演练+资产+镜像）
     */
    Optional<DrillAssetImageMapping> findByRangeIdAndAssetIdAndImageId(
            Long rangeId, Long assetId, String imageId);

    /**
     * 检查映射是否存在
     */
    boolean existsByRangeIdAndAssetIdAndImageId(Long rangeId, Long assetId, String imageId);

    /**
     * 删除演练的所有映射
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM DrillAssetImageMapping m WHERE m.rangeId = :rangeId")
    void deleteByRangeId(@Param("rangeId") Long rangeId);

    /**
     * 删除资产的所有映射
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM DrillAssetImageMapping m WHERE m.rangeId = :rangeId AND m.assetId = :assetId")
    void deleteByRangeIdAndAssetId(@Param("rangeId") Long rangeId, @Param("assetId") Long assetId);

    /**
     * 精确删除单个映射（单镜像删除）
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM DrillAssetImageMapping m WHERE m.rangeId = :rangeId AND m.assetId = :assetId AND m.imageId = :imageId")
    void deleteByRangeIdAndAssetIdAndImageId(
            @Param("rangeId") Long rangeId,
            @Param("assetId") Long assetId,
            @Param("imageId") String imageId);

    /**
     * 统计演练下的映射数量
     */
    long countByRangeId(Long rangeId);

    /**
     * 统计资产下的映射数量
     */
    long countByRangeIdAndAssetId(Long rangeId, Long assetId);
}
