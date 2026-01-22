package org.cyberlab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 授权管理服务
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Management Service for CyberLab Platform
 *
 * 遵循八耻八荣原则：
 * - 完整的 null 检查
 * - 详细的异常处理
 * - 清晰的日志记录
 * - 友好的错误信息
 */
@Service
public class LicenseService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

    @Autowired
    private SystemSettingService systemSettingService;

    /**
     * 序列号格式正则：CYBERLAB-YYYY-MM-DD-NNNNN
     */
    private static final Pattern SERIAL_NUMBER_PATTERN =
            Pattern.compile("^CYBERLAB-(\\d{4})-(\\d{2})-(\\d{2})-(\\d{5})$");

    /**
     * 授权码格式正则：CL-ZL3B4T34M-PRO2025-ACTIVE-ABCD1234
     */
    private static final Pattern LICENSE_CODE_PATTERN =
            Pattern.compile("^CL-([A-Z0-9]+)-([A-Z0-9]+)-(ACTIVE|INACTIVE)-([A-Z0-9]+)$");

    /**
     * 验证授权信息
     *
     * @return 授权验证结果信息
     */
    public Map<String, Object> validateLicense() {
        logger.debug("Starting license validation");
        Map<String, Object> result = new HashMap<>();

        try {
            // 八耻八荣：完整的 null 检查
            if (systemSettingService == null) {
                logger.error("SystemSettingService 未注入");
                return buildErrorResult("系统配置服务未初始化");
            }

            // 获取序列号
            String serialNumber = null;
            try {
                serialNumber = systemSettingService.getSetting("serial_number");
            } catch (Exception e) {
                logger.error("获取序列号失败", e);
                return buildErrorResult("获取序列号失败：" + e.getMessage());
            }

            // 八耻八荣：详细的空值检查
            if (serialNumber == null || serialNumber.trim().isEmpty()) {
                logger.warn("序列号未设置或为空");
                return buildErrorResult("序列号未设置");
            }

            logger.debug("Validating serial number: {}", serialNumber);

            // 解析序列号
            LicenseInfo licenseInfo = parseSerialNumber(serialNumber);
            if (licenseInfo == null) {
                logger.error("序列号格式错误: {}", serialNumber);
                return buildErrorResult("序列号格式错误，正确格式：CYBERLAB-YYYY-MM-DD-NNNNN");
            }

            // 获取授权码
            String licenseCode = null;
            try {
                licenseCode = systemSettingService.getSetting("license_code");
            } catch (Exception e) {
                logger.error("获取授权码失败", e);
                // 授权码获取失败不应导致整个验证失败，使用默认值
                licenseCode = "";
            }

            boolean hasValidLicenseCode = validateLicenseCode(licenseCode);
            if (!hasValidLicenseCode) {
                logger.warn("授权码验证失败: {}", licenseCode);
            }

            // 八耻八荣：安全的日期计算，防止空指针
            LocalDate today = LocalDate.now();
            LocalDate expiryDate = licenseInfo.getExpiryDate();

            if (expiryDate == null) {
                logger.error("过期日期为空");
                return buildErrorResult("序列号中的过期日期无效");
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, expiryDate);
            boolean expired = today.isAfter(expiryDate);
            boolean nearExpiry = !expired && daysRemaining <= 30 && daysRemaining > 0;

            // 构建结果
            result.put("success", true);
            result.put("valid", !expired && hasValidLicenseCode);
            result.put("expired", expired);
            result.put("nearExpiry", nearExpiry);
            result.put("daysRemaining", Math.max(0, daysRemaining));
            result.put("serialNumber", serialNumber);
            result.put("licenseCode", licenseCode);
            result.put("expiryDate", expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            result.put("expiryDateFormatted", formatChineseDate(expiryDate));
            result.put("owner", systemSettingService.getSetting("license_owner"));
            result.put("issuedDate", systemSettingService.getSetting("license_issued_date"));
            result.put("uniqueId", licenseInfo.getUniqueId());

            // 状态信息
            if (expired) {
                result.put("status", "expired");
                result.put("statusText", "授权已过期");
                result.put("statusColor", "danger");
                result.put("message", "授权已于 " + formatChineseDate(expiryDate) + " 过期，请联系蟑螂恶霸团队续期");
            } else if (nearExpiry) {
                result.put("status", "warning");
                result.put("statusText", "即将过期");
                result.put("statusColor", "warning");
                result.put("message", "授权将在 " + daysRemaining + " 天后过期，请及时联系蟑螂恶霸团队续期");
            } else {
                result.put("status", "active");
                result.put("statusText", "正常运行");
                result.put("statusColor", "success");
                result.put("message", "授权有效，剩余 " + daysRemaining + " 天");
            }

            // 联系信息
            result.put("supportContact", "蟑螂恶霸团队");
            result.put("supportEmail", "sun740883686@foxmail.com");

        } catch (Exception e) {
            return buildErrorResult("授权验证失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 解析序列号获取授权信息
     * 遵循八耻八荣原则：完整的异常处理和日志记录
     *
     * @param serialNumber 序列号字符串
     * @return 授权信息对象，解析失败返回 null
     */
    private LicenseInfo parseSerialNumber(String serialNumber) {
        // 八耻八荣：null 检查
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            logger.warn("序列号为空或 null");
            return null;
        }

        String trimmedSerialNumber = serialNumber.trim();
        logger.debug("解析序列号: {}", trimmedSerialNumber);

        Matcher matcher = SERIAL_NUMBER_PATTERN.matcher(trimmedSerialNumber);
        if (!matcher.matches()) {
            logger.warn("序列号格式不匹配: {}，正确格式：CYBERLAB-YYYY-MM-DD-NNNNN", trimmedSerialNumber);
            return null;
        }

        try {
            // 八耻八荣：详细的异常捕获
            String yearStr = matcher.group(1);
            String monthStr = matcher.group(2);
            String dayStr = matcher.group(3);
            String uniqueId = matcher.group(4);

            if (yearStr == null || monthStr == null || dayStr == null || uniqueId == null) {
                logger.error("序列号正则匹配组为 null");
                return null;
            }

            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);
            int day = Integer.parseInt(dayStr);

            // 八耻八荣：验证日期有效性
            if (year < 2020 || year > 2100) {
                logger.warn("序列号年份超出有效范围: {}", year);
                return null;
            }
            if (month < 1 || month > 12) {
                logger.warn("序列号月份超出有效范围: {}", month);
                return null;
            }
            if (day < 1 || day > 31) {
                logger.warn("序列号日期超出有效范围: {}", day);
                return null;
            }

            LocalDate expiryDate = LocalDate.of(year, month, day);
            logger.debug("Successfully parsed serial number, expiry date: {}", expiryDate);

            return new LicenseInfo(expiryDate, uniqueId);

        } catch (NumberFormatException e) {
            logger.error("序列号日期格式转换失败: {}", trimmedSerialNumber, e);
            return null;
        } catch (DateTimeParseException e) {
            logger.error("序列号日期解析失败: {}", trimmedSerialNumber, e);
            return null;
        } catch (Exception e) {
            // 八耻八荣：捕获所有未预期的异常
            logger.error("解析序列号时发生未预期的异常: {}", trimmedSerialNumber, e);
            return null;
        }
    }

    /**
     * 验证授权码格式
     * 遵循八耻八荣原则：完整的 null 检查和日志记录
     *
     * @param licenseCode 授权码
     * @return 是否有效
     */
    private boolean validateLicenseCode(String licenseCode) {
        // 八耻八荣：null 和空值检查
        if (licenseCode == null || licenseCode.trim().isEmpty()) {
            logger.debug("授权码为空或 null");
            return false;
        }

        String trimmedLicenseCode = licenseCode.trim();
        logger.debug("验证授权码: {}", trimmedLicenseCode);

        try {
            Matcher matcher = LICENSE_CODE_PATTERN.matcher(trimmedLicenseCode);
            if (!matcher.matches()) {
                logger.warn("授权码格式不匹配: {}，正确格式：CL-TEAM-EDITION-STATUS-HASH", trimmedLicenseCode);
                return false;
            }

            // 检查状态是否为 ACTIVE
            String status = matcher.group(3);
            if (status == null) {
                logger.error("授权码状态提取失败");
                return false;
            }

            boolean isActive = "ACTIVE".equals(status);
            if (!isActive) {
                logger.warn("授权码状态不是 ACTIVE: {}", status);
            }

            return isActive;

        } catch (Exception e) {
            // 八耻八荣：捕获所有未预期的异常
            logger.error("验证授权码时发生异常: {}", trimmedLicenseCode, e);
            return false;
        }
    }

    /**
     * 构建错误结果
     * 遵循八耻八荣原则：友好的错误信息，不暴露敏感细节
     *
     * @param message 错误信息
     * @return 错误结果 Map
     */
    private Map<String, Object> buildErrorResult(String message) {
        // 八耻八荣：记录详细错误日志
        logger.error("授权验证失败: {}", message);

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("valid", false);
        result.put("expired", true);
        result.put("daysRemaining", 0);
        result.put("status", "error");
        result.put("statusText", "授权异常");
        result.put("statusColor", "danger");

        // 八耻八荣：提供友好的用户错误信息，不暴露技术细节
        String userFriendlyMessage = message != null ? message : "授权验证失败";
        result.put("message", userFriendlyMessage + "，请联系蟑螂恶霸团队");

        result.put("serialNumber", "未知");
        result.put("licenseCode", "未知");
        result.put("expiryDate", null);
        result.put("expiryDateFormatted", "未知");
        result.put("owner", "未知");
        result.put("supportContact", "蟑螂恶霸团队");
        result.put("supportEmail", "sun740883686@foxmail.com");

        return result;
    }

    /**
     * 格式化日期为中文格式
     *
     * @param date 日期
     * @return 中文格式日期字符串（如：2025年12月31日）
     */
    private String formatChineseDate(LocalDate date) {
        return date.getYear() + "年" + date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
    }

    /**
     * 授权信息内部类
     */
    private static class LicenseInfo {
        private final LocalDate expiryDate;
        private final String uniqueId;

        public LicenseInfo(LocalDate expiryDate, String uniqueId) {
            this.expiryDate = expiryDate;
            this.uniqueId = uniqueId;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        public String getUniqueId() {
            return uniqueId;
        }
    }
}
