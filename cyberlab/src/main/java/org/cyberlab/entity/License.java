package org.cyberlab.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 授权实体类
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Entity for CyberLab Platform
 *
 * 遵循八耻八荣原则：
 * - 完整的字段验证
 * - 清晰的字段注释
 * - 自动时间戳管理
 */
@Entity
@Table(name = "licenses")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 产品序列号
     * 格式：CYBERLAB-YYYY-MM-DD-NNNNN
     * 示例：CYBERLAB-2025-12-31-00001
     */
    @Column(name = "serial_number", nullable = false, unique = true, length = 50)
    private String serialNumber;

    /**
     * 授权码
     * 格式：CL-TEAM-EDITION-STATUS-HASH
     * 示例：CL-ZL3B4T34M-PRO2025-ACTIVE-A7F3D2E8
     */
    @Column(name = "license_code", nullable = false, unique = true, length = 200)
    private String licenseCode;

    /**
     * 版本类型
     * 可选值：PRO（专业版）, ENTERPRISE（企业版）, TRIAL（试用版）
     */
    @Column(name = "edition", nullable = false, length = 20)
    private String edition;

    /**
     * 授权状态
     * 可选值：ACTIVE（激活）, INACTIVE（停用）, EXPIRED（过期）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 授权给（客户名称）
     */
    @Column(name = "issued_to", length = 100)
    private String issuedTo;

    /**
     * 颁发日期
     */
    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    /**
     * 过期日期
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    /**
     * 是否为当前激活授权
     * true - 当前系统使用的授权
     * false - 历史授权或备用授权
     */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    /**
     * 创建人用户名（通常是 hongan）
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 备注信息
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors

    public License() {
    }

    public License(String serialNumber, String licenseCode, String edition, String status,
                   String issuedTo, LocalDate issuedDate, LocalDate expiryDate) {
        this.serialNumber = serialNumber;
        this.licenseCode = licenseCode;
        this.edition = edition;
        this.status = status;
        this.issuedTo = issuedTo;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
    }

    // JPA 生命周期回调 - 自动设置创建时间和更新时间

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isCurrent == null) {
            isCurrent = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = licenseCode;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "License{" +
                "id=" + id +
                ", serialNumber='" + serialNumber + '\'' +
                ", edition='" + edition + '\'' +
                ", status='" + status + '\'' +
                ", issuedTo='" + issuedTo + '\'' +
                ", expiryDate=" + expiryDate +
                ", isCurrent=" + isCurrent +
                '}';
    }
}
