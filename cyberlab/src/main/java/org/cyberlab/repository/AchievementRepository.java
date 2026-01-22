package org.cyberlab.repository;

import org.cyberlab.entity.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    
    // 根据状态分页查询
    Page<Achievement> findByStatus(String status, Pageable pageable);
    
    // 根据演练ID查询
    List<Achievement> findByRangeId(Long rangeId);
    
    // 在现有的AchievementRepository中添加以下方法
    List<Achievement> findByTeamName(String teamName);

    // 根据队伍名称分页查询
    Page<Achievement> findByTeamName(String teamName, Pageable pageable);

    // 根据队伍名称和状态分页查询
    Page<Achievement> findByTeamNameAndStatus(String teamName, String status, Pageable pageable);
    
    // 管理员审批页面 - 获取所有成果（分页）
    @Query("SELECT a FROM Achievement a ORDER BY a.submitTime DESC")
    Page<Achievement> findAllOrderBySubmitTimeDesc(Pageable pageable);
    
    // 统计查询方法
    long countByStatus(String status);
    
    /**
     * 根据提交时间范围统计成果数量
     */
    long countBySubmitTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取最近审核通过的成果（用于实时展示）
     */
    @Query("SELECT a FROM Achievement a WHERE a.status = 'approved' AND a.reviewTime >= :since ORDER BY a.reviewTime DESC")
    List<Achievement> findRecentApproved(@org.springframework.data.repository.query.Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 获取最新审核通过的成果（简化版）
     * 添加 reviewTime IS NOT NULL 条件，避免排序失败
     */
    @Query("SELECT a FROM Achievement a WHERE a.status = :status AND a.reviewTime IS NOT NULL ORDER BY a.reviewTime DESC")
    List<Achievement> findTop10ByStatusOrderByReviewTimeDesc(@org.springframework.data.repository.query.Param("status") String status, Pageable pageable);
}