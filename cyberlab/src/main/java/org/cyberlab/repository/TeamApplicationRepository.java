package org.cyberlab.repository;

import org.cyberlab.entity.TeamApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {

    // 按战队ID查询所有申请
    List<TeamApplication> findByTeamId(Long teamId);

    // 按用户ID查询所有申请
    List<TeamApplication> findByUserId(Long userId);

    // 按状态查询
    List<TeamApplication> findByStatus(String status);

    // 按战队ID和状态查询
    List<TeamApplication> findByTeamIdAndStatus(Long teamId, String status);

    // 按用户ID和状态查询
    List<TeamApplication> findByUserIdAndStatus(Long userId, String status);

    // 查询用户对特定战队的待处理申请
    Optional<TeamApplication> findByTeamIdAndUserIdAndStatus(Long teamId, Long userId, String status);

    // 查询用户是否有任何待处理的申请
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM TeamApplication a WHERE a.userId = :userId AND a.status = 'pending'")
    boolean existsPendingApplicationByUserId(@Param("userId") Long userId);

    // 统计战队的待处理申请数量
    long countByTeamIdAndStatus(Long teamId, String status);

    // 统计用户的申请数量（按状态）
    long countByUserIdAndStatus(Long userId, String status);
}
