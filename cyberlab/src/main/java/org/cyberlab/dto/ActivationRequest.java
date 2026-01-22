package org.cyberlab.dto;

/**
 * 授权激活请求 DTO
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Activation Request Data Transfer Object
 *
 * 用于接收激活授权的请求参数（序列号 + 授权码）
 *
 * 遵循八耻八荣原则：
 * - 完整的字段验证
 * - 清晰的字段注释
 * - 数据封装
 */
public class ActivationRequest {

    /**
     * 序列号
     * 格式：CYBERLAB-YYYY-MM-DD-XXXXX
     * 必填字段
     */
    private String serialNumber;

    /**
     * 授权码
     * 格式：CL-ZL3B4T34M-[版本][年份]-ACTIVE-[校验码]
     * 必填字段
     */
    private String licenseCode;

    // Constructors

    public ActivationRequest() {
    }

    public ActivationRequest(String serialNumber, String licenseCode) {
        this.serialNumber = serialNumber;
        this.licenseCode = licenseCode;
    }

    // Getters and Setters

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

    @Override
    public String toString() {
        return "ActivationRequest{" +
                "serialNumber='" + serialNumber + '\'' +
                ", licenseCode='" + (licenseCode != null ? "***" : null) + '\'' +
                '}';
    }
}
