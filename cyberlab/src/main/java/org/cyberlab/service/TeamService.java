package org.cyberlab.service;

import org.cyberlab.entity.Team;
import org.cyberlab.entity.TeamMember;
import org.cyberlab.entity.User;
import org.cyberlab.repository.TeamRepository;
import org.cyberlab.repository.TeamMemberRepository;
import org.cyberlab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepo;

    @Autowired
    private TeamMemberRepository teamMemberRepo;

    @Autowired
    private UserRepository userRepo;

    // ==================== Team CRUD ====================

    /**
     * 获取所有战队
     */
    public List<Team> getAllTeams() {
        return teamRepo.findAll();
    }

    /**
     * 按类型获取战队
     */
    public List<Team> getTeamsByType(String teamType) {
        if (!"red".equals(teamType) && !"blue".equals(teamType)) {
            throw new IllegalArgumentException("无效的战队类型: " + teamType);
        }
        return teamRepo.findByTeamType(teamType);
    }

    /**
     * 按状态获取战队
     */
    public List<Team> getTeamsByStatus(String status) {
        return teamRepo.findByStatus(status);
    }

    /**
     * 获取活跃的战队（按类型）
     */
    public List<Team> getActiveTeamsByType(String teamType) {
        if (!"red".equals(teamType) && !"blue".equals(teamType)) {
            throw new IllegalArgumentException("无效的战队类型: " + teamType);
        }
        return teamRepo.findByTeamTypeAndStatus(teamType, "active");
    }

    /**
     * 根据ID获取战队
     */
    public Team getTeamById(Long id) {
        return teamRepo.findById(id).orElse(null);
    }

    /**
     * 创建战队
     */
    @Transactional
    public Team createTeam(String name, Long leaderId, String description, String teamType) {
        // 验证战队类型
        if (!"red".equals(teamType) && !"blue".equals(teamType)) {
            throw new IllegalArgumentException("无效的战队类型: " + teamType);
        }

        // 检查战队名称是否已存在
        if (teamRepo.findByName(name).isPresent()) {
            throw new IllegalArgumentException("战队名称已存在: " + name);
        }

        // 验证队长是否存在
        User leader = userRepo.findById(leaderId)
            .orElseThrow(() -> new IllegalArgumentException("队长用户不存在: " + leaderId));

        // 检查队长是否已在其他战队
        if (teamMemberRepo.existsByUserId(leaderId)) {
            throw new IllegalArgumentException("该用户已加入其他战队");
        }

        // 创建战队
        Team team = new Team();
        team.setName(name);
        team.setLeaderId(leaderId);
        team.setDescription(description);
        team.setTeamType(teamType);
        team.setStatus("active");
        Team savedTeam = teamRepo.save(team);

        // 自动将队长加入战队
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeamId(savedTeam.getId());
        leaderMember.setUserId(leaderId);
        leaderMember.setMemberRole("leader");
        teamMemberRepo.save(leaderMember);

        // 更新用户的team_id
        leader.setTeamId(savedTeam.getId());
        userRepo.save(leader);

        return savedTeam;
    }

    /**
     * 更新战队信息
     */
    @Transactional
    public Team updateTeam(Long id, String name, Long leaderId, String description, String status) {
        Team team = teamRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("战队不存在: " + id));

        // 更新战队名称
        if (name != null && !name.trim().isEmpty()) {
            // 检查名称是否被其他战队使用
            teamRepo.findByName(name).ifPresent(existingTeam -> {
                if (!existingTeam.getId().equals(id)) {
                    throw new IllegalArgumentException("战队名称已存在: " + name);
                }
            });
            team.setName(name);
        }

        // 更新队长
        if (leaderId != null && !leaderId.equals(team.getLeaderId())) {
            // 验证新队长是否存在且在本战队
            User newLeader = userRepo.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("新队长用户不存在: " + leaderId));

            TeamMember newLeaderMember = teamMemberRepo.findByTeamIdAndUserId(id, leaderId)
                .orElseThrow(() -> new IllegalArgumentException("新队长不在本战队"));

            // 更新旧队长角色为member
            Long oldLeaderId = team.getLeaderId();
            teamMemberRepo.findByTeamIdAndUserId(id, oldLeaderId).ifPresent(oldLeaderMember -> {
                oldLeaderMember.setMemberRole("member");
                teamMemberRepo.save(oldLeaderMember);
            });

            // 更新新队长角色为leader
            newLeaderMember.setMemberRole("leader");
            teamMemberRepo.save(newLeaderMember);

            team.setLeaderId(leaderId);
        }

        // 更新描述
        if (description != null) {
            team.setDescription(description);
        }

        // 更新状态
        if (status != null) {
            if (!"active".equals(status) && !"disbanded".equals(status)) {
                throw new IllegalArgumentException("无效的状态: " + status);
            }
            team.setStatus(status);
        }

        return teamRepo.save(team);
    }

    /**
     * 删除战队
     */
    @Transactional
    public boolean deleteTeam(Long id) {
        if (!teamRepo.existsById(id)) {
            return false;
        }

        // 清除所有成员的team_id
        List<TeamMember> members = teamMemberRepo.findByTeamId(id);
        members.forEach(member -> {
            userRepo.findById(member.getUserId()).ifPresent(user -> {
                user.setTeamId(null);
                userRepo.save(user);
            });
        });

        // 删除所有战队成员记录
        teamMemberRepo.deleteByTeamId(id);

        // 删除战队
        teamRepo.deleteById(id);
        return true;
    }

    /**
     * 切换战队状态（active ↔ disbanded）
     */
    public boolean toggleTeamStatus(Long id) {
        return teamRepo.findById(id).map(team -> {
            String newStatus = "active".equals(team.getStatus()) ? "disbanded" : "active";
            team.setStatus(newStatus);
            teamRepo.save(team);
            return true;
        }).orElse(false);
    }

    // ==================== Team Member Management ====================

    /**
     * 添加战队成员
     */
    @Transactional
    public TeamMember addMemberToTeam(Long teamId, Long userId) {
        // 验证战队是否存在且激活
        Team team = teamRepo.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("战队不存在: " + teamId));

        if ("disbanded".equals(team.getStatus())) {
            throw new IllegalArgumentException("无法向已解散的战队添加成员");
        }

        // 验证用户是否存在
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        // 检查用户是否已在其他战队
        if (teamMemberRepo.existsByUserId(userId)) {
            throw new IllegalArgumentException("该用户已加入其他战队");
        }

        // 添加成员
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setMemberRole("member");
        TeamMember savedMember = teamMemberRepo.save(member);

        // 更新用户的team_id
        user.setTeamId(teamId);
        userRepo.save(user);

        return savedMember;
    }

    /**
     * 移除战队成员
     */
    @Transactional
    public boolean removeMemberFromTeam(Long teamId, Long userId, Long currentUserId, boolean isAdmin) {
        // 检查战队是否存在
        Team team = teamRepo.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("战队不存在: " + teamId));

        // 权限验证：非管理员需要验证队长权限
        if (!isAdmin) {
            if (!team.getLeaderId().equals(currentUserId)) {
                throw new IllegalArgumentException("只有队长或管理员可以移除成员");
            }
        }

        // 检查是否为队长（队长不能被移除）
        if (team.getLeaderId().equals(userId)) {
            throw new IllegalArgumentException("无法移除队长，请先更换队长或解散战队");
        }

        // 查找并删除成员记录
        Optional<TeamMember> memberOpt = teamMemberRepo.findByTeamIdAndUserId(teamId, userId);
        if (memberOpt.isEmpty()) {
            return false;
        }

        // 清除用户的team_id
        userRepo.findById(userId).ifPresent(user -> {
            user.setTeamId(null);
            userRepo.save(user);
        });

        // 删除成员记录
        teamMemberRepo.delete(memberOpt.get());
        return true;
    }

    /**
     * 获取战队所有成员
     */
    public List<TeamMember> getTeamMembers(Long teamId) {
        return teamMemberRepo.findByTeamId(teamId);
    }

    /**
     * 获取战队成员数量
     */
    public long getTeamMemberCount(Long teamId) {
        return teamMemberRepo.countByTeamId(teamId);
    }

    /**
     * 获取用户所在的战队
     */
    public Team getTeamByUserId(Long userId) {
        Optional<TeamMember> memberOpt = teamMemberRepo.findByUserId(userId);
        if (memberOpt.isEmpty()) {
            return null;
        }
        return teamRepo.findById(memberOpt.get().getTeamId()).orElse(null);
    }

    // ==================== Statistics ====================

    /**
     * 统计战队数量（按类型）
     */
    public long countTeamsByType(String teamType) {
        return teamRepo.countByTeamType(teamType);
    }

    /**
     * 统计活跃战队数量
     */
    public long countActiveTeams() {
        return teamRepo.countByStatus("active");
    }
}
