package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 容器探测记录实体
 * 用于持久化存储容器发现结果
 * 实现增量同步：以探测结果为准，自动增删容器记录
 */
@Entity
@Table(name = "container_discovery_records",
    indexes = {
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_asset_id", columnList = "asset_id"),
        @Index(name = "idx_container_id", columnList = "container_id"),
        @Index(name = "idx_last_seen", columnList = "last_seen_at"),
        @Index(name = "idx_project_container", columnList = "project_id, container_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_container", columnNames = {"project_id", "container_id"})
    }
)
public class ContainerDiscoveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "asset_ip", length = 100)
    private String assetIp;

    @Column(name = "container_id", nullable = false)
    private String containerId;

    @Column(name = "container_name")
    private String containerName;

    @Column(length = 500)
    private String image;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String ports;

    @Column(columnDefinition = "TEXT")
    private String labels;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    // Constructors
    public ContainerDiscoveryRecord() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetIp() {
        return assetIp;
    }

    public void setAssetIp(String assetIp) {
        this.assetIp = assetIp;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
