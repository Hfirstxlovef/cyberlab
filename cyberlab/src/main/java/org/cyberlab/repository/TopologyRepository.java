package org.cyberlab.repository;

import org.cyberlab.entity.TopologyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopologyRepository extends JpaRepository<TopologyEntity, Long> {

    // 根据 projectId 查询拓扑图
    Optional<TopologyEntity> findByProjectId(String projectId);
}