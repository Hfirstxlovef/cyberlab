package org.cyberlab.dto;

import org.cyberlab.entity.License;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 授权响应 DTO
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Response Data Transfer Object
 *
 * 用于向前端返回授权信息，包含格式化和计算字段
 *
 * 遵循八耻八荣原则：
 * - 数据封装和转换
 * - 格式化显示
 * - 状态计算
 */
public class LicenseResponse {

    private Long id;
    private String serialNumber;
    private String licenseCode;
    private String edition;
    private String editionText;  // 版本文本（中文）
    private String status;
    private String statusText;   // 状态文本（中文）
    private String statusColor;  // 状态颜色（用于前端显示）
    private String issuedTo;
    private LocalDate issuedDate;
    private String issuedDateFormatted;
    private LocalDate expiryDate;
    private String expiryDateFormatted;
    private Long daysRemaining;  // 剩余天数
    private Boolean isCurrent;
    private Boolean isExpired;   // 是否已过期
    private Boolean isNearExpiry; // 是否临近过期
    private String createdBy;
    private LocalDateTime createdAt;
    private String createdAtFormatted;
    private LocalDateTime updatedAt;
    private String notes;

    // Constructors

    public LicenseResponse() {
    }

    /**
     * 从 License 实体创建 LicenseResponse
     *
     * @param license License 实体
     * @return LicenseResponse 对象
     */
    public static LicenseResponse fromEntity(License license) {
        if (license == null) {
            return null;
        }

        LicenseResponse response = new LicenseResponse();
        response.setId(license.getId());
        response.setSerialNumber(license.getSerialNumber());
        response.setLicenseCode(license.getLicenseCode());
        response.setEdition(license.getEdition());
        response.setStatus(license.getStatus());
        response.setIssuedTo(license.getIssuedTo());
        response.setIssuedDate(license.getIssuedDate());
        response.setExpiryDate(license.getExpiryDate());
        response.setIsCurrent(license.getIsCurrent());
        response.setCreatedBy(license.getCreatedBy());
        response.setCreatedAt(license.getCreatedAt());
        response.setUpdatedAt(license.getUpdatedAt());
        response.setNotes(license.getNotes());

        // 计算和格式化字段
        response.calculateDerivedFields();

        return response;
    }

    /**
     * 计算派生字段（剩余天数、过期状态等）
     */
    private void calculateDerivedFields() {
        // 版本文本
        if (edition != null) {
            switch (edition.toUpperCase()) {
                case "PRO":
                    editionText = "专业版";
                    break;
                case "ENTERPRISE":
                    editionText = "企业版";
                    break;
                case "TRIAL":
                    editionText = "试用版";
                    break;
                default:
                    editionText = edition;
            }
        }

        // 状态文本和颜色
        if (status != null) {
            switch (status.toUpperCase()) {
                case "ACTIVE":
                    statusText = "激活中";
                    statusColor = "success";
                    break;
                case "INACTIVE":
                    statusText = "已停用";
                    statusColor = "info";
                    break;
                case "EXPIRED":
                    statusText = "已过期";
                    statusColor = "danger";
                    break;
                default:
                    statusText = status;
                    statusColor = "info";
            }
        }

        // 日期格式化
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (issuedDate != null) {
            issuedDateFormatted = issuedDate.format(dateFormatter);
        }

        if (expiryDate != null) {
            expiryDateFormatted = expiryDate.format(dateFormatter);

            // 计算剩余天数
            LocalDate today = LocalDate.now();
            daysRemaining = ChronoUnit.DAYS.between(today, expiryDate);

            // 是否已过期
            isExpired = today.isAfter(expiryDate);

            // 是否临近过期（30天内）
            isNearExpiry = !isExpired && daysRemaining <= 30 && daysRemaining > 0;

            // 根据过期状态调整颜色
            if (isExpired && "ACTIVE".equals(status)) {
                statusColor = "danger";
            } else if (isNearExpiry && "ACTIVE".equals(status)) {
                statusColor = "warning";
            }
        }

        if (createdAt != null) {
            createdAtFormatted = createdAt.format(datetimeFormatter);
        }
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

    public String getEditionText() {
        return editionText;
    }

    public void setEditionText(String editionText) {
        this.editionText = editionText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getStatusColor() {
        return statusColor;
    }

    public void setStatusColor(String statusColor) {
        this.statusColor = statusColor;
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

    public String getIssuedDateFormatted() {
        return issuedDateFormatted;
    }

    public void setIssuedDateFormatted(String issuedDateFormatted) {
        this.issuedDateFormatted = issuedDateFormatted;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getExpiryDateFormatted() {
        return expiryDateFormatted;
    }

    public void setExpiryDateFormatted(String expiryDateFormatted) {
        this.expiryDateFormatted = expiryDateFormatted;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public Boolean getIsNearExpiry() {
        return isNearExpiry;
    }

    public void setIsNearExpiry(Boolean isNearExpiry) {
        this.isNearExpiry = isNearExpiry;
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

    public String getCreatedAtFormatted() {
        return createdAtFormatted;
    }

    public void setCreatedAtFormatted(String createdAtFormatted) {
        this.createdAtFormatted = createdAtFormatted;
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
}
