package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统备份记录实体
 * 用于记录系统备份的元数据信息
 */
@Entity
@Table(name = "system_backup")
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 备份名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 备份类型：full(完整备份), database(仅数据库), files(仅文件)
     */
    @Column(nullable = false, length = 20)
    private String type;

    /**
     * 备份文件路径（相对于backups目录）
     */
    @Column(nullable = false, length = 500)
    private String filePath;

    /**
     * 备份文件大小（字节）
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * 备份状态：pending(创建中), completed(完成), failed(失败), restoring(恢复中)
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 备份描述
     */
    @Column(length = 1000)
    private String description;

    /**
     * 创建者
     */
    @Column(length = 100)
    private String createdBy;

    /**
     * 错误信息（备份失败时）
     */
    @Column(length = 2000)
    private String errorMessage;

    /**
     * 数据库名称
     */
    @Column(length = 100)
    private String databaseName;

    /**
     * 包含的表数量
     */
    private Integer tableCount;

    /**
     * 备份文件MD5校验值
     */
    @Column(length = 32)
    private String md5Checksum;

    /**
     * 是否加密（密码保护）
     */
    @Column(nullable = false)
    private Boolean encrypted = false;

    /**
     * 加密方法：NONE/AES256
     */
    @Column(length = 20)
    private String encryptionMethod = "NONE";

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public Integer getTableCount() {
        return tableCount;
    }

    public void setTableCount(Integer tableCount) {
        this.tableCount = tableCount;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }

    public void setMd5Checksum(String md5Checksum) {
        this.md5Checksum = md5Checksum;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getEncryptionMethod() {
        return encryptionMethod;
    }

    public void setEncryptionMethod(String encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    /**
     * 获取文件大小的可读格式
     */
    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";

        long size = fileSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * 检查备份是否完成
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * 检查备份是否失败
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }

    /**
     * 检查备份是否正在进行
     */
    public boolean isPending() {
        return "pending".equals(status);
    }

    /**
     * 获取类型显示名称
     */
    public String getTypeDisplayName() {
        if (type == null) return "未知";
        switch (type.toLowerCase()) {
            case "full": return "完整备份";
            case "database": return "仅数据库";
            case "files": return "仅文件";
            default: return type;
        }
    }

    /**
     * 获取状态显示名称
     */
    public String getStatusDisplayName() {
        if (status == null) return "未知";
        switch (status.toLowerCase()) {
            case "pending": return "创建中";
            case "completed": return "已完成";
            case "failed": return "失败";
            case "restoring": return "恢复中";
            default: return status;
        }
    }
}
