package org.cyberlab.service;

import org.cyberlab.entity.TeamScore;
import org.cyberlab.enums.AchievementType;
import org.cyberlab.repository.TeamScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 打分系统核心服务
 * 负责团队得分的计算、更新和排名管理
 */
@Service
public class ScoringService {

    private static final Logger logger = LoggerFactory.getLogger(ScoringService.class);

    @Autowired
    private TeamScoreRepository teamScoreRepo;

    /**
     * 根据成果类型计算基础分值
     */
    public int calculateBaseScore(String achievementType) {
        try {
            return AchievementType.getBaseScoreByValue(achievementType);
        } catch (Exception e) {
            logger.warn("无法识别的成果类型: {}, 使用默认分值60", achievementType);
            return 60;
        }
    }

    /**
     * 更新团队得分
     * @param teamName 团队名称
     * @param rangeId 演练ID
     * @param score 新增分数
     * @param teamType 队伍类型(red/blue)
     */
    @Transactional
    public void updateTeamScore(String teamName, Long rangeId, int score, String teamType) {
        logger.info("📊 开始更新团队得分: team={}, range={}, score=+{}, type={}",
                teamName, rangeId, score, teamType);

        // 查找或创建得分记录
        Optional<TeamScore> optional = teamScoreRepo.findByTeamNameAndRangeId(teamName, rangeId);
        TeamScore teamScore;

        if (optional.isPresent()) {
            teamScore = optional.get();
            teamScore.addScore(score);
            logger.info("更新已有记录: totalScore={}, approvedCount={}",
                    teamScore.getTotalScore(), teamScore.getApprovedCount());
        } else {
            // 创建新记录
            teamScore = new TeamScore();
            teamScore.setTeamName(teamName);
            teamScore.setRangeId(rangeId);
            teamScore.setTeamType(teamType);
            teamScore.setTotalScore(score);
            teamScore.setApprovedCount(1);
            teamScore.setLastUpdateTime(LocalDateTime.now());
            logger.info("创建新得分记录: team={}, initialScore={}", teamName, score);
        }

        teamScoreRepo.save(teamScore);

        // 更新排名
        recalculateRanking(rangeId, teamType);
    }

    /**
     * 重新计算指定演练指定类型队伍的排名
     */
    @Transactional
    public void recalculateRanking(Long rangeId, String teamType) {
        logger.info("🔄 重新计算排名: range={}, type={}", rangeId, teamType);

        // 获取该类型所有队伍，按分数降序
        List<TeamScore> teams = teamScoreRepo.findForRanking(rangeId, teamType);

        // 更新排名
        int rank = 1;
        for (TeamScore team : teams) {
            team.setRanking(rank++);
            teamScoreRepo.save(team);
        }

        logger.info("排名更新完成，共 {} 支队伍", teams.size());
    }

    /**
     * 获取指定演练指定类型的排行榜
     */
    public List<TeamScore> getTeamRanking(Long rangeId, String teamType) {
        return teamScoreRepo.findByRangeIdAndTeamTypeOrderByTotalScoreDescApprovedCountDescLastUpdateTimeAsc(
                rangeId, teamType);
    }

    /**
     * 获取指定演练所有队伍的排行榜
     */
    public List<TeamScore> getAllTeamRanking(Long rangeId) {
        return teamScoreRepo.findByRangeIdOrderByTotalScoreDescApprovedCountDescLastUpdateTimeAsc(rangeId);
    }

    /**
     * 获取指定队伍在指定演练的得分详情
     */
    public Optional<TeamScore> getTeamScore(String teamName, Long rangeId) {
        return teamScoreRepo.findByTeamNameAndRangeId(teamName, rangeId);
    }

    /**
     * 统计指定演练中指定类型队伍的数量
     */
    public long countTeams(Long rangeId, String teamType) {
        return teamScoreRepo.countByRangeIdAndTeamType(rangeId, teamType);
    }
}
