package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

/**
 * 容器状态实体
 * 用于记录容器的目标状态和当前状态，支持状态调和机制
 */
@Entity
@Table(name = "container_states")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ContainerState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "host_node_id")
    private Long hostNodeId;

    @Column(name = "container_id", length = 255)
    private String containerId;

    @Column(name = "container_name", length = 255)
    private String containerName;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "desired_status", length = 50)
    private String desiredStatus; // RUNNING, STOPPED, PAUSED, RESTARTED

    @Column(name = "current_status", length = 50)
    private String currentStatus; // RUNNING, STOPPED, PAUSED, UNKNOWN

    @Column(name = "health_status", length = 50)
    private String healthStatus; // healthy, unhealthy, unknown

    @Column(name = "sync_status", length = 50)
    private String syncStatus; // SYNCED, OUT_OF_SYNC, SYNCING, FAILED

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "sync_attempts")
    private Integer syncAttempts;

    @Column(name = "max_sync_attempts")
    private Integer maxSyncAttempts;

    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    // 构造函数
    public ContainerState() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.syncAttempts = 0;
        this.maxSyncAttempts = 3;
        this.syncStatus = "OUT_OF_SYNC";
    }

    public ContainerState(Long assetId, Long hostNodeId, String containerId, String desiredStatus) {
        this();
        this.assetId = assetId;
        this.hostNodeId = hostNodeId;
        this.containerId = containerId;
        this.desiredStatus = desiredStatus;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getHostNodeId() {
        return hostNodeId;
    }

    public void setHostNodeId(Long hostNodeId) {
        this.hostNodeId = hostNodeId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getDesiredStatus() {
        return desiredStatus;
    }

    public void setDesiredStatus(String desiredStatus) {
        this.desiredStatus = desiredStatus;
        this.updatedAt = LocalDateTime.now();
        // 当目标状态改变时，重置同步状态
        if (!desiredStatus.equals(this.currentStatus)) {
            this.syncStatus = "OUT_OF_SYNC";
            this.syncAttempts = 0;
        }
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
        this.updatedAt = LocalDateTime.now();
        // 检查是否与目标状态一致
        updateSyncStatus();
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Integer getSyncAttempts() {
        return syncAttempts;
    }

    public void setSyncAttempts(Integer syncAttempts) {
        this.syncAttempts = syncAttempts;
    }

    public Integer getMaxSyncAttempts() {
        return maxSyncAttempts;
    }

    public void setMaxSyncAttempts(Integer maxSyncAttempts) {
        this.maxSyncAttempts = maxSyncAttempts;
    }

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // 业务逻辑方法

    /**
     * 检查是否需要调和（目标状态与当前状态不一致）
     */
    public boolean needsReconciliation() {
        return desiredStatus != null && 
               !desiredStatus.equals(currentStatus) && 
               !"SYNCING".equals(syncStatus) &&
               (syncAttempts == null || syncAttempts < maxSyncAttempts);
    }

    /**
     * 检查是否已达到最大同步尝试次数
     */
    public boolean hasExceededMaxAttempts() {
        return syncAttempts != null && maxSyncAttempts != null && 
               syncAttempts >= maxSyncAttempts;
    }

    /**
     * 检查是否健康
     */
    public boolean isHealthy() {
        return "healthy".equals(healthStatus) && "SYNCED".equals(syncStatus);
    }

    /**
     * 开始同步
     */
    public void startSync() {
        this.syncStatus = "SYNCING";
        this.lastSyncAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.syncAttempts == null) {
            this.syncAttempts = 0;
        }
        this.syncAttempts++;
    }

    /**
     * 同步成功
     */
    public void syncSuccess() {
        this.syncStatus = "SYNCED";
        this.syncError = null;
        this.updatedAt = LocalDateTime.now();
        updateSyncStatus();
    }

    /**
     * 同步失败
     */
    public void syncFailed(String error) {
        this.syncStatus = hasExceededMaxAttempts() ? "FAILED" : "OUT_OF_SYNC";
        this.syncError = error;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 重置同步状态
     */
    public void resetSync() {
        this.syncAttempts = 0;
        this.syncError = null;
        this.syncStatus = "OUT_OF_SYNC";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新同步状态
     */
    private void updateSyncStatus() {
        if (desiredStatus != null && desiredStatus.equals(currentStatus)) {
            this.syncStatus = "SYNCED";
        } else if (!"SYNCING".equals(syncStatus) && !"FAILED".equals(syncStatus)) {
            this.syncStatus = "OUT_OF_SYNC";
        }
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if ("SYNCED".equals(syncStatus)) {
            return "状态一致";
        } else if ("OUT_OF_SYNC".equals(syncStatus)) {
            return "需要调和";
        } else if ("SYNCING".equals(syncStatus)) {
            return "同步中";
        } else if ("FAILED".equals(syncStatus)) {
            return String.format("同步失败 (%d/%d)", syncAttempts, maxSyncAttempts);
        } else {
            return "未知状态";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("ContainerState{id=%d, assetId=%d, containerId='%s', desired='%s', current='%s', sync='%s'}", 
                           id, assetId, containerId, desiredStatus, currentStatus, syncStatus);
    }
}