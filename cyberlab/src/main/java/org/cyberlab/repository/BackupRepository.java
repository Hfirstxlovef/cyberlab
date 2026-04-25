package org.cyberlab.repository;

import org.cyberlab.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统备份Repository
 */
@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {

    /**
     * 按创建时间倒序查询所有备份
     */
    List<Backup> findAllByOrderByCreatedTimeDesc();

    /**
     * 按状态查询备份
     */
    List<Backup> findByStatus(String status);

    /**
     * 按类型查询备份
     */
    List<Backup> findByType(String type);

    /**
     * 查询指定时间之前的备份
     */
    List<Backup> findByCreatedTimeBefore(LocalDateTime dateTime);

    /**
     * 查询已完成的备份数量
     */
    @Query("SELECT COUNT(b) FROM Backup b WHERE b.status = 'completed'")
    long countCompletedBackups();

    /**
     * 查询备份总大小
     */
    @Query("SELECT SUM(b.fileSize) FROM Backup b WHERE b.status = 'completed'")
    Long getTotalBackupSize();

    /**
     * 按创建者查询备份
     */
    List<Backup> findByCreatedBy(String createdBy);
}
