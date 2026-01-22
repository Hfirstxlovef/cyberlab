package org.cyberlab.repository;

import org.cyberlab.entity.ScenarioTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScenarioTemplateRepository extends JpaRepository<ScenarioTemplate, Long> {
    
    // 根据场景类型查找模板
    List<ScenarioTemplate> findByScenarioTypeAndIsActiveTrue(String scenarioType);
    
    // 根据场景类型查找模板（分页）
    Page<ScenarioTemplate> findByScenarioTypeAndIsActiveTrue(String scenarioType, Pageable pageable);
    
    // 根据难度等级查找模板
    List<ScenarioTemplate> findByDifficultyLevelAndIsActiveTrue(String difficultyLevel);
    
    // 根据是否公开查找模板
    List<ScenarioTemplate> findByIsPublicTrueAndIsActiveTrue();
    
    // 根据是否公开查找模板（分页）
    Page<ScenarioTemplate> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);
    
    // 根据创建者查找模板
    List<ScenarioTemplate> findByCreatedByAndIsActiveTrue(Long createdBy);
    
    // 多条件查询
    @Query("SELECT t FROM ScenarioTemplate t WHERE " +
           "(:scenarioType IS NULL OR t.scenarioType = :scenarioType) AND " +
           "(:difficultyLevel IS NULL OR t.difficultyLevel = :difficultyLevel) AND " +
           "(:maxParticipants IS NULL OR t.maxParticipants <= :maxParticipants) AND " +
           "(:isPublic IS NULL OR t.isPublic = :isPublic) AND " +
           "t.isActive = true")
    Page<ScenarioTemplate> findByMultipleConditions(
            @Param("scenarioType") String scenarioType,
            @Param("difficultyLevel") String difficultyLevel,
            @Param("maxParticipants") Integer maxParticipants,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable);
    
    // 按名称搜索模板（模糊查询）
    @Query("SELECT t FROM ScenarioTemplate t WHERE t.isActive = true AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ScenarioTemplate> searchByKeyword(@Param("keyword") String keyword);
    
    // 按名称搜索模板（模糊查询，分页）
    @Query("SELECT t FROM ScenarioTemplate t WHERE t.isActive = true AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ScenarioTemplate> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 获取所有活跃的模板，按使用次数排序
    List<ScenarioTemplate> findByIsActiveTrueOrderByUsageCountDesc();
    
    // 获取所有活跃的模板，按创建时间排序
    List<ScenarioTemplate> findByIsActiveTrueOrderByCreatedAtDesc();
    
    // 获取所有活跃的模板，按最后使用时间排序
    @Query("SELECT t FROM ScenarioTemplate t WHERE t.isActive = true ORDER BY t.lastUsedAt DESC NULLS LAST")
    List<ScenarioTemplate> findByIsActiveTrueOrderByLastUsedAtDesc();
    
    // 获取热门模板（按使用次数排序，分页）
    Page<ScenarioTemplate> findByIsActiveTrueOrderByUsageCountDesc(Pageable pageable);
    
    // 获取最新模板（按创建时间排序，分页）
    Page<ScenarioTemplate> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // 获取预计演练时间在指定范围内的模板
    @Query("SELECT t FROM ScenarioTemplate t WHERE t.isActive = true AND " +
           "(:minDuration IS NULL OR t.estimatedDuration >= :minDuration) AND " +
           "(:maxDuration IS NULL OR t.estimatedDuration <= :maxDuration)")
    List<ScenarioTemplate> findByEstimatedDurationBetween(
            @Param("minDuration") Integer minDuration,
            @Param("maxDuration") Integer maxDuration);
    
    // 获取所有场景类型
    @Query("SELECT DISTINCT t.scenarioType FROM ScenarioTemplate t WHERE t.isActive = true")
    List<String> findAllDistinctScenarioTypes();
    
    // 获取所有难度等级
    @Query("SELECT DISTINCT t.difficultyLevel FROM ScenarioTemplate t WHERE t.isActive = true")
    List<String> findAllDistinctDifficultyLevels();
    
    // 统计各场景类型的模板数量
    @Query("SELECT t.scenarioType, COUNT(t) FROM ScenarioTemplate t WHERE t.isActive = true GROUP BY t.scenarioType")
    List<Object[]> countByScenarioType();
    
    // 统计各难度等级的模板数量
    @Query("SELECT t.difficultyLevel, COUNT(t) FROM ScenarioTemplate t WHERE t.isActive = true GROUP BY t.difficultyLevel")
    List<Object[]> countByDifficultyLevel();
    
    // 获取最受欢迎的模板（使用次数最多的前N个）
    @Query("SELECT t FROM ScenarioTemplate t WHERE t.isActive = true AND t.isPublic = true ORDER BY t.usageCount DESC")
    List<ScenarioTemplate> findTopUsedTemplates(Pageable pageable);
}