package org.cyberlab.repository;

import org.cyberlab.entity.ScreenRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRecordingRepository extends JpaRepository<ScreenRecording, Long> {

    // 根据sessionId查找
    Optional<ScreenRecording> findBySessionId(String sessionId);

    // 检查用户是否有正在录制的会话
    boolean existsByUserIdAndStatus(Long userId, String status);

    // 查找用户正在录制的会话
    Optional<ScreenRecording> findByUserIdAndStatus(Long userId, String status);

    // 根据用户名和时间范围查找录屏
    @Query("SELECT sr FROM ScreenRecording sr WHERE sr.username = :username " +
           "AND sr.startTime >= :startTime AND sr.startTime <= :endTime " +
           "ORDER BY sr.startTime DESC")
    List<ScreenRecording> findByUsernameAndTimeRange(
        @Param("username") String username,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // 查找用户所有已完成的录屏
    List<ScreenRecording> findByUsernameAndStatusOrderByStartTimeDesc(String username, String status);

    // 查找所有已完成的录屏（分页）
    List<ScreenRecording> findByStatusOrderByStartTimeDesc(String status);

    // 根据用户ID查找所有录屏
    List<ScreenRecording> findByUserIdOrderByStartTimeDesc(Long userId);

    // 查找指定时间之前的录屏（用于清理）
    @Query("SELECT sr FROM ScreenRecording sr WHERE sr.createdAt < :beforeDate")
    List<ScreenRecording> findOldRecordings(@Param("beforeDate") LocalDateTime beforeDate);

    // 统计用户录屏总时长
    @Query("SELECT SUM(sr.durationSeconds) FROM ScreenRecording sr " +
           "WHERE sr.userId = :userId AND sr.status = 'completed'")
    Long sumDurationByUserId(@Param("userId") Long userId);

    // 统计用户录屏总大小
    @Query("SELECT SUM(sr.fileSize) FROM ScreenRecording sr " +
           "WHERE sr.userId = :userId AND sr.status = 'completed'")
    Long sumFileSizeByUserId(@Param("userId") Long userId);
}
