package org.cyberlab.repository;

import org.cyberlab.entity.TeamScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamScoreRepository extends JpaRepository<TeamScore, Long> {

    /**
     * 根据团队名称和演练ID查找得分记录
     */
    Optional<TeamScore> findByTeamNameAndRangeId(String teamName, Long rangeId);

    /**
     * 根据演练ID和队伍类型查找，按总分降序排列（用于排行榜）
     */
    List<TeamScore> findByRangeIdAndTeamTypeOrderByTotalScoreDescApprovedCountDescLastUpdateTimeAsc(
            Long rangeId, String teamType);

    /**
     * 根据演练ID查找所有队伍得分，按总分降序排列
     */
    List<TeamScore> findByRangeIdOrderByTotalScoreDescApprovedCountDescLastUpdateTimeAsc(Long rangeId);

    /**
     * 统计指定演练中指定类型队伍的数量
     */
    long countByRangeIdAndTeamType(Long rangeId, String teamType);

    /**
     * 根据演练ID和队伍类型查询，用于更新排名
     */
    @Query("SELECT ts FROM TeamScore ts WHERE ts.rangeId = :rangeId AND ts.teamType = :teamType " +
           "ORDER BY ts.totalScore DESC, ts.approvedCount DESC, ts.lastUpdateTime ASC")
    List<TeamScore> findForRanking(@Param("rangeId") Long rangeId, @Param("teamType") String teamType);

    /**
     * 根据队伍ID查找所有得分记录
     */
    List<TeamScore> findByTeamId(Long teamId);

    /**
     * 删除指定演练的所有得分记录（演练结束后清理）
     */
    void deleteByRangeId(Long rangeId);
}
