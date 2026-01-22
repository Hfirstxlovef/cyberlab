package org.cyberlab.repository;

import org.cyberlab.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // 按战队名称查询
    Optional<Team> findByName(String name);

    // 按战队类型查询
    List<Team> findByTeamType(String teamType);

    // 按状态查询
    List<Team> findByStatus(String status);

    // 按战队类型和状态查询
    List<Team> findByTeamTypeAndStatus(String teamType, String status);

    // 按队长ID查询
    Optional<Team> findByLeaderId(Long leaderId);

    // 统计战队数量（按类型）
    long countByTeamType(String teamType);

    // 统计战队数量（按状态）
    long countByStatus(String status);
}
