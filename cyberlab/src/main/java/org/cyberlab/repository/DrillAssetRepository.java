package org.cyberlab.repository;

import org.cyberlab.entity.DrillAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrillAssetRepository extends JpaRepository<DrillAsset, Long> {
    
    // 根据分类查找资产
    List<DrillAsset> findByCategoryAndIsActiveTrue(String category);
    
    // 根据分类查找资产（分页）
    Page<DrillAsset> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    // 根据难度等级查找资产
    List<DrillAsset> findByDifficultyLevelAndIsActiveTrue(String difficultyLevel);
    
    // 根据团队可见性查找资产
    List<DrillAsset> findByTeamVisibilityAndIsActiveTrue(String teamVisibility);
    
    // 根据团队可见性查找资产（包括both）
    @Query("SELECT a FROM DrillAsset a WHERE a.isActive = true AND (a.teamVisibility = :teamVisibility OR a.teamVisibility = 'both')")
    List<DrillAsset> findByTeamVisibilityOrBoth(@Param("teamVisibility") String teamVisibility);
    
    // 根据漏洞类型查找资产
    List<DrillAsset> findByVulnerabilityTypeAndIsActiveTrue(String vulnerabilityType);
    
    // 根据是否为攻击目标查找资产
    List<DrillAsset> findByIsTargetAndIsActiveTrue(Boolean isTarget);
    
    // 多条件查询
    @Query("SELECT a FROM DrillAsset a WHERE " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:difficultyLevel IS NULL OR a.difficultyLevel = :difficultyLevel) AND " +
           "(:vulnerabilityType IS NULL OR a.vulnerabilityType = :vulnerabilityType) AND " +
           "(:teamVisibility IS NULL OR a.teamVisibility = :teamVisibility OR a.teamVisibility = 'both') AND " +
           "(:isTarget IS NULL OR a.isTarget = :isTarget) AND " +
           "a.isActive = true")
    Page<DrillAsset> findByMultipleConditions(
            @Param("category") String category,
            @Param("difficultyLevel") String difficultyLevel,
            @Param("vulnerabilityType") String vulnerabilityType,
            @Param("teamVisibility") String teamVisibility,
            @Param("isTarget") Boolean isTarget,
            Pageable pageable);
    
    // 按名称搜索资产（模糊查询）
    @Query("SELECT a FROM DrillAsset a WHERE a.isActive = true AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<DrillAsset> searchByKeyword(@Param("keyword") String keyword);
    
    // 按名称搜索资产（模糊查询，分页）
    @Query("SELECT a FROM DrillAsset a WHERE a.isActive = true AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DrillAsset> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 获取所有活跃的资产
    List<DrillAsset> findByIsActiveTrueOrderByCreatedAtDesc();
    
    // 获取所有活跃的资产（分页）
    Page<DrillAsset> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // 根据创建者查找资产
    List<DrillAsset> findByCreatedByAndIsActiveTrue(Long createdBy);
    
    // 获取所有分类
    @Query("SELECT DISTINCT a.category FROM DrillAsset a WHERE a.isActive = true")
    List<String> findAllDistinctCategories();
    
    // 获取所有难度等级
    @Query("SELECT DISTINCT a.difficultyLevel FROM DrillAsset a WHERE a.isActive = true")
    List<String> findAllDistinctDifficultyLevels();
    
    // 获取所有漏洞类型
    @Query("SELECT DISTINCT a.vulnerabilityType FROM DrillAsset a WHERE a.isActive = true AND a.vulnerabilityType IS NOT NULL")
    List<String> findAllDistinctVulnerabilityTypes();
    
    // 统计各分类的资产数量
    @Query("SELECT a.category, COUNT(a) FROM DrillAsset a WHERE a.isActive = true GROUP BY a.category")
    List<Object[]> countByCategory();
    
    // 统计各难度等级的资产数量
    @Query("SELECT a.difficultyLevel, COUNT(a) FROM DrillAsset a WHERE a.isActive = true GROUP BY a.difficultyLevel")
    List<Object[]> countByDifficultyLevel();
    
    // 统计总的活跃资产数量
    long countByIsActiveTrue();
}