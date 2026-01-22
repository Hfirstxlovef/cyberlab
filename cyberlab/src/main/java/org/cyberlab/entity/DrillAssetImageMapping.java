package org.cyberlab.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

/**
 * 演练资产镜像映射表
 * 用于持久化演练-资产-镜像的关联关系，支持刷新页面后数据恢复
 */
@Entity
@Table(name = "drill_asset_image_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"range_id", "asset_id", "image_id"}),
       indexes = {
           @Index(name = "idx_range_id", columnList = "range_id"),
           @Index(name = "idx_asset_id", columnList = "asset_id")
       })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DrillAssetImageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 演练ID
     */
    @Column(name = "range_id", nullable = false)
    private Long rangeId;

    /**
     * 资产ID
     */
    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    /**
     * 镜像ID (SHA256或其他唯一标识)
     */
    @Column(name = "image_id", nullable = false, length = 255)
    private String imageId;

    /**
     * 镜像名称 (例如: nginx)
     */
    @Column(name = "image_name", nullable = false, length = 255)
    private String imageName;

    /**
     * 镜像标签 (例如: 1.20)
     */
    @Column(name = "image_tag", length = 100)
    private String imageTag;

    /**
     * 镜像完整名称 (例如: nginx:1.20)
     */
    @Column(name = "image_full_name", length = 500)
    private String imageFullName;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造函数
    public DrillAssetImageMapping() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public DrillAssetImageMapping(Long rangeId, Long assetId, String imageId,
                                   String imageName, String imageTag, String imageFullName) {
        this.rangeId = rangeId;
        this.assetId = assetId;
        this.imageId = imageId;
        this.imageName = imageName;
        this.imageTag = imageTag;
        this.imageFullName = imageFullName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 生成复合键 (rangeId:assetId:imageId)
     */
    public String generateKey() {
        return rangeId + ":" + assetId + ":" + imageId;
    }

    // PreUpdate hook
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRangeId() {
        return rangeId;
    }

    public void setRangeId(Long rangeId) {
        this.rangeId = rangeId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getImageFullName() {
        return imageFullName;
    }

    public void setImageFullName(String imageFullName) {
        this.imageFullName = imageFullName;
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

    @Override
    public String toString() {
        return "DrillAssetImageMapping{" +
                "id=" + id +
                ", rangeId=" + rangeId +
                ", assetId=" + assetId +
                ", imageId='" + imageId + '\'' +
                ", imageName='" + imageName + '\'' +
                ", imageTag='" + imageTag + '\'' +
                ", imageFullName='" + imageFullName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
