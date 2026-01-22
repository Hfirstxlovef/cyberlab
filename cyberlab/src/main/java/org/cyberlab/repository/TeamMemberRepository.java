package org.cyberlab.repository;

import org.cyberlab.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // 按战队ID查询所有成员
    List<TeamMember> findByTeamId(Long teamId);

    // 按用户ID查询（一个用户只能在一个战队）
    Optional<TeamMember> findByUserId(Long userId);

    // 按战队ID和用户ID查询
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    // 按战队ID和成员角色查询
    List<TeamMember> findByTeamIdAndMemberRole(Long teamId, String memberRole);

    // 统计战队成员数量
    long countByTeamId(Long teamId);

    // 删除战队的所有成员
    void deleteByTeamId(Long teamId);

    // 检查用户是否已在某个战队
    boolean existsByUserId(Long userId);

    // 检查用户是否在指定战队
    boolean existsByTeamIdAndUserId(Long teamId, Long userId);
}
