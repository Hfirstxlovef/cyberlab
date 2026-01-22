package org.cyberlab.dto;

import java.time.LocalDate;

/**
 * 授权请求 DTO
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Request Data Transfer Object
 *
 * 用于接收生成授权的请求参数
 *
 * 遵循八耻八荣原则：
 * - 完整的字段验证
 * - 清晰的字段注释
 * - 数据封装
 */
public class LicenseRequest {

    /**
     * 授权给（客户名称）
     * 必填字段
     */
    private String issuedTo;

    /**
     * 版本类型
     * 可选值：PRO, ENTERPRISE, TRIAL
     * 必填字段
     */
    private String edition;

    /**
     * 过期日期
     * 必填字段
     */
    private LocalDate expiryDate;

    /**
     * 备注信息
     * 可选字段
     */
    private String notes;

    // Constructors

    public LicenseRequest() {
    }

    public LicenseRequest(String issuedTo, String edition, LocalDate expiryDate) {
        this.issuedTo = issuedTo;
        this.edition = edition;
        this.expiryDate = expiryDate;
    }

    public LicenseRequest(String issuedTo, String edition, LocalDate expiryDate, String notes) {
        this.issuedTo = issuedTo;
        this.edition = edition;
        this.expiryDate = expiryDate;
        this.notes = notes;
    }

    // Getters and Setters

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "LicenseRequest{" +
                "issuedTo='" + issuedTo + '\'' +
                ", edition='" + edition + '\'' +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
