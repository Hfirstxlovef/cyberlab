package org.cyberlab.repository;

import org.cyberlab.entity.CyberRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CyberRangeRepository extends JpaRepository<CyberRange, Long> {
    
    /**
     * 根据状态统计演练数量
     */
    long countByStatus(String status);
    
    /**
     * 根据状态查找演练列表
     */
    List<CyberRange> findByStatus(String status);
}