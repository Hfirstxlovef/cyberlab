package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "screen_recordings")
public class ScreenRecording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;  // 前端生成的唯一会话ID

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;  // 文件大小（字节）

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;  // 录制时长（秒）

    @Column(name = "status", nullable = false)
    private String status = "recording";  // recording/uploading/completed/failed

    @Column(name = "project_id")
    private String projectId;  // 关联的攻击项目ID

    @Column(name = "related_achievement_ids", columnDefinition = "TEXT")
    private String relatedAchievementIds;  // JSON格式存储关联的成果ID列表

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;  // 已上传的分片数量

    @Column(name = "total_chunks")
    private Integer totalChunks;  // 总分片数量

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public ScreenRecording() {}

    public ScreenRecording(Long userId, String username, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.startTime = LocalDateTime.now();
        this.status = "recording";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getRelatedAchievementIds() { return relatedAchievementIds; }
    public void setRelatedAchievementIds(String relatedAchievementIds) {
        this.relatedAchievementIds = relatedAchievementIds;
    }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void incrementChunkCount() {
        this.chunkCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return "completed".equals(this.status);
    }

    public boolean isRecording() {
        return "recording".equals(this.status);
    }
}
