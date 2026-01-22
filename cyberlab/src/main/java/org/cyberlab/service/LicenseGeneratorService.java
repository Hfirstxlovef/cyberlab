package org.cyberlab.service;

import org.cyberlab.repository.LicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 授权码生成服务
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Generator Service for CyberLab Platform
 *
 * 负责生成序列号和授权码
 *
 * 遵循八耻八荣原则：
 * - 完整的 null 检查
 * - 详细的异常处理
 * - 清晰的日志记录
 * - 安全的加密算法
 */
@Service
public class LicenseGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseGeneratorService.class);

    @Autowired
    private LicenseRepository licenseRepository;

    // 常量定义
    private static final String SERIAL_PREFIX = "CYBERLAB";
    private static final String LICENSE_PREFIX = "CL";
    private static final String TEAM_ID = "ZL3B4T34M";  // 蟑螂恶霸团队 (Zhanglang Eba Team)
    private static final String DEFAULT_STATUS = "ACTIVE";

    /**
     * 生成产品序列号
     * 格式：CYBERLAB-YYYY-MM-DD-NNNNN
     *
     * @param expiryDate 过期日期
     * @return 序列号
     */
    public String generateSerialNumber(LocalDate expiryDate) {
        logger.info("开始生成序列号，过期日期: {}", expiryDate);

        try {
            // 八耻八荣：null 检查
            if (expiryDate == null) {
                logger.error("过期日期为 null");
                throw new IllegalArgumentException("过期日期不能为空");
            }

            // 格式化日期部分
            String datePart = expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 生成唯一标识符（5位数字）
            String uniqueId = generateUniqueId();

            // 组装序列号
            String serialNumber = String.format("%s-%s-%s", SERIAL_PREFIX, datePart, uniqueId);

            logger.info("成功生成序列号: {}", serialNumber);
            return serialNumber;

        } catch (Exception e) {
            logger.error("生成序列号失败", e);
            throw new RuntimeException("生成序列号失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成授权码
     * 格式：CL-ZL3B4T34M-[EDITION][YEAR]-ACTIVE-[HASH]
     *
     * @param serialNumber 序列号
     * @param edition      版本（PRO, ENTERPRISE, TRIAL）
     * @return 授权码
     */
    public String generateLicenseCode(String serialNumber, String edition) {
        logger.info("开始生成授权码，序列号: {}, 版本: {}", serialNumber, edition);

        try {
            // 八耻八荣：null 检查和参数验证
            if (serialNumber == null || serialNumber.trim().isEmpty()) {
                logger.error("序列号为空");
                throw new IllegalArgumentException("序列号不能为空");
            }

            if (edition == null || edition.trim().isEmpty()) {
                logger.error("版本为空");
                throw new IllegalArgumentException("版本不能为空");
            }

            // 验证版本有效性
            String normalizedEdition = normalizeEdition(edition);

            // 提取年份（从序列号中）
            String year = extractYear(serialNumber);

            // 生成校验哈希
            String hash = calculateHash(serialNumber, normalizedEdition);

            // 组装授权码
            String licenseCode = String.format("%s-%s-%s%s-%s-%s",
                    LICENSE_PREFIX,
                    TEAM_ID,
                    normalizedEdition,
                    year,
                    DEFAULT_STATUS,
                    hash);

            logger.info("成功生成授权码: {}", licenseCode);
            return licenseCode;

        } catch (Exception e) {
            logger.error("生成授权码失败", e);
            throw new RuntimeException("生成授权码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成唯一标识符（5位数字）
     * 基于当前数据库中的授权数量 + 1
     *
     * @return 5位数字字符串
     */
    private String generateUniqueId() {
        try {
            // 获取当前授权总数
            long count = licenseRepository.countTotal();

            // 生成下一个序号（从 00001 开始）
            long nextId = count + 1;

            // 格式化为5位数字
            String uniqueId = String.format("%05d", nextId);

            // 八耻八荣：检查唯一性（防止并发问题）
            // 如果已存在，递增重试（最多10次）
            int retryCount = 0;
            while (retryCount < 10) {
                String tempSerial = SERIAL_PREFIX + "-XXXX-XX-XX-" + uniqueId;
                boolean exists = licenseRepository.existsBySerialNumber(tempSerial);

                if (!exists) {
                    return uniqueId;
                }

                nextId++;
                uniqueId = String.format("%05d", nextId);
                retryCount++;
            }

            // 如果重试失败，使用时间戳作为后备方案
            logger.warn("使用时间戳作为唯一标识符的后备方案");
            return String.format("%05d", System.currentTimeMillis() % 100000);

        } catch (Exception e) {
            logger.error("生成唯一标识符失败", e);
            // 后备方案：使用当前时间戳
            return String.format("%05d", System.currentTimeMillis() % 100000);
        }
    }

    /**
     * 标准化版本名称
     *
     * @param edition 原始版本名称
     * @return 标准化后的版本名称
     */
    private String normalizeEdition(String edition) {
        if (edition == null) {
            return "TRIAL";
        }

        String upper = edition.trim().toUpperCase();

        switch (upper) {
            case "PRO":
            case "PROFESSIONAL":
                return "PRO";
            case "ENTERPRISE":
            case "ENT":
                return "ENTERPRISE";
            case "TRIAL":
            case "TEST":
                return "TRIAL";
            default:
                logger.warn("未识别的版本类型: {}, 使用默认值 TRIAL", edition);
                return "TRIAL";
        }
    }

    /**
     * 从序列号中提取年份
     *
     * @param serialNumber 序列号
     * @return 年份（4位数字）
     */
    private String extractYear(String serialNumber) {
        try {
            // 八耻八荣：null 检查
            if (serialNumber == null || serialNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("序列号为空");
            }

            // 序列号格式：CYBERLAB-YYYY-MM-DD-NNNNN
            String[] parts = serialNumber.split("-");

            if (parts.length < 2) {
                throw new IllegalArgumentException("序列号格式错误");
            }

            // 第二部分是年份
            String year = parts[1];

            // 验证年份格式（4位数字）
            if (!year.matches("\\d{4}")) {
                throw new IllegalArgumentException("年份格式错误: " + year);
            }

            return year;

        } catch (Exception e) {
            logger.error("提取年份失败: {}", serialNumber, e);
            // 后备方案：使用当前年份
            return String.valueOf(LocalDate.now().getYear());
        }
    }

    /**
     * 计算校验哈希（SHA-256）
     * 取前8位作为授权码的一部分
     *
     * @param serialNumber 序列号
     * @param edition      版本
     * @return 8位哈希值（大写）
     */
    private String calculateHash(String serialNumber, String edition) {
        try {
            // 八耻八荣：null 检查
            if (serialNumber == null || edition == null) {
                throw new IllegalArgumentException("参数不能为空");
            }

            // 组合输入字符串（加入时间戳增加唯一性）
            String input = serialNumber + edition + System.currentTimeMillis();

            // 使用 SHA-256 加密
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 取前8位并转换为大写
            String hash = hexString.substring(0, 8).toUpperCase();

            logger.debug("计算哈希值: {}", hash);
            return hash;

        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 算法不可用", e);
            throw new RuntimeException("加密算法不可用", e);
        } catch (Exception e) {
            logger.error("计算哈希失败", e);
            throw new RuntimeException("计算哈希失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证序列号格式
     *
     * @param serialNumber 序列号
     * @return true 如果格式正确，否则 false
     */
    public boolean validateSerialNumberFormat(String serialNumber) {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            return false;
        }

        // 正则：CYBERLAB-YYYY-MM-DD-NNNNN
        String pattern = "^CYBERLAB-\\d{4}-\\d{2}-\\d{2}-\\d{5}$";
        return serialNumber.matches(pattern);
    }

    /**
     * 验证授权码格式
     *
     * @param licenseCode 授权码
     * @return true 如果格式正确，否则 false
     */
    public boolean validateLicenseCodeFormat(String licenseCode) {
        if (licenseCode == null || licenseCode.trim().isEmpty()) {
            return false;
        }

        // 正则：CL-[TEAM]-[EDITION][YEAR]-[STATUS]-[HASH]
        String pattern = "^CL-[A-Z0-9]+-[A-Z0-9]+-[A-Z]+-[A-Z0-9]{8}$";
        return licenseCode.matches(pattern);
    }
}
