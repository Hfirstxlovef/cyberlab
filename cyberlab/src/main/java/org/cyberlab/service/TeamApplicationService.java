package org.cyberlab.service;

import org.cyberlab.entity.Team;
import org.cyberlab.entity.TeamApplication;
import org.cyberlab.entity.TeamMember;
import org.cyberlab.entity.User;
import org.cyberlab.repository.TeamApplicationRepository;
import org.cyberlab.repository.TeamMemberRepository;
import org.cyberlab.repository.TeamRepository;
import org.cyberlab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamApplicationService {

    @Autowired
    private TeamApplicationRepository applicationRepo;

    @Autowired
    private TeamRepository teamRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TeamMemberRepository teamMemberRepo;

    /**
     * 提交加入战队申请
     */
    @Transactional
    public TeamApplication submitApplication(Long teamId, Long userId, String message) {
        // 验证战队是否存在且激活
        Team team = teamRepo.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("战队不存在: " + teamId));

        if ("disbanded".equals(team.getStatus())) {
            throw new IllegalArgumentException("该战队已解散，无法申请加入");
        }

        // 验证用户是否存在
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        // 检查用户角色是否匹配战队类型
        if (!user.getRole().equals(team.getTeamType())) {
            throw new IllegalArgumentException("用户角色与战队类型不匹配");
        }

        // 检查用户是否已在其他战队
        if (teamMemberRepo.existsByUserId(userId)) {
            throw new IllegalArgumentException("您已加入其他战队，无法申请");
        }

        // 检查是否已有待处理的申请
        if (applicationRepo.findByTeamIdAndUserIdAndStatus(teamId, userId, "pending").isPresent()) {
            throw new IllegalArgumentException("您已经提交过申请，请等待审批");
        }

        // 创建申请
        TeamApplication application = new TeamApplication();
        application.setTeamId(teamId);
        application.setUserId(userId);
        application.setMessage(message);
        application.setStatus("pending");

        return applicationRepo.save(application);
    }

    /**
     * 批准申请
     */
    @Transactional
    public void approveApplication(Long applicationId, Long reviewerId) {
        TeamApplication application = applicationRepo.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("申请不存在: " + applicationId));

        if (!"pending".equals(application.getStatus())) {
            throw new IllegalArgumentException("申请已处理，无法重复操作");
        }

        // 验证审批人是否为战队队长
        Team team = teamRepo.findById(application.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("战队不存在"));

        if (!team.getLeaderId().equals(reviewerId)) {
            throw new IllegalArgumentException("只有队长可以审批申请");
        }

        // 再次检查用户是否已加入其他战队（防止并发问题）
        if (teamMemberRepo.existsByUserId(application.getUserId())) {
            throw new IllegalArgumentException("该用户已加入其他战队");
        }

        // 批准申请
        application.approve(reviewerId);
        applicationRepo.save(application);

        // 添加用户到战队
        TeamMember member = new TeamMember();
        member.setTeamId(application.getTeamId());
        member.setUserId(application.getUserId());
        member.setMemberRole("member");
        teamMemberRepo.save(member);

        // 更新用户的team_id
        userRepo.findById(application.getUserId()).ifPresent(user -> {
            user.setTeamId(application.getTeamId());
            userRepo.save(user);
        });
    }

    /**
     * 拒绝申请
     */
    @Transactional
    public void rejectApplication(Long applicationId, Long reviewerId, String reason) {
        TeamApplication application = applicationRepo.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("申请不存在: " + applicationId));

        if (!"pending".equals(application.getStatus())) {
            throw new IllegalArgumentException("申请已处理，无法重复操作");
        }

        // 验证审批人是否为战队队长
        Team team = teamRepo.findById(application.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("战队不存在"));

        if (!team.getLeaderId().equals(reviewerId)) {
            throw new IllegalArgumentException("只有队长可以审批申请");
        }

        // 拒绝申请
        application.reject(reviewerId, reason);
        applicationRepo.save(application);
    }

    /**
     * 撤回申请（申请人自己撤回）
     */
    @Transactional
    public void withdrawApplication(Long applicationId, Long userId) {
        TeamApplication application = applicationRepo.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("申请不存在: " + applicationId));

        if (!application.getUserId().equals(userId)) {
            throw new IllegalArgumentException("您无权撤回此申请");
        }

        if (!"pending".equals(application.getStatus())) {
            throw new IllegalArgumentException("只能撤回待处理的申请");
        }

        // 删除申请
        applicationRepo.delete(application);
    }

    /**
     * 获取战队的所有申请
     */
    public List<TeamApplication> getTeamApplications(Long teamId) {
        return applicationRepo.findByTeamId(teamId);
    }

    /**
     * 获取战队的待处理申请
     */
    public List<TeamApplication> getPendingApplicationsByTeam(Long teamId) {
        return applicationRepo.findByTeamIdAndStatus(teamId, "pending");
    }

    /**
     * 获取用户的所有申请
     */
    public List<TeamApplication> getUserApplications(Long userId) {
        return applicationRepo.findByUserId(userId);
    }

    /**
     * 获取用户的待处理申请
     */
    public List<TeamApplication> getPendingApplicationsByUser(Long userId) {
        return applicationRepo.findByUserIdAndStatus(userId, "pending");
    }

    /**
     * 根据ID获取申请
     */
    public TeamApplication getApplicationById(Long id) {
        return applicationRepo.findById(id).orElse(null);
    }

    /**
     * 统计战队的待处理申请数量
     */
    public long countPendingApplications(Long teamId) {
        return applicationRepo.countByTeamIdAndStatus(teamId, "pending");
    }

    /**
     * 检查用户是否有待处理的申请
     */
    public boolean hasPendingApplication(Long userId) {
        return applicationRepo.existsPendingApplicationByUserId(userId);
    }
}
