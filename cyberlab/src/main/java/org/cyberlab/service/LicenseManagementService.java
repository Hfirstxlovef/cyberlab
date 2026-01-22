package org.cyberlab.service;

import org.cyberlab.dto.LicenseRequest;
import org.cyberlab.dto.LicenseResponse;
import org.cyberlab.entity.License;
import org.cyberlab.repository.LicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 授权管理服务
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Management Service for CyberLab Platform
 *
 * 负责授权的创建、管理和与 system_settings 表的同步
 *
 * 遵循八耻八荣原则：
 * - 完整的事务管理
 * - 详细的异常处理
 * - 数据一致性保证
 * - 清晰的日志记录
 */
@Service
public class LicenseManagementService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseManagementService.class);

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseGeneratorService generatorService;

    @Autowired
    private SystemSettingService systemSettingService;

    /**
     * 创建新授权
     *
     * @param request   授权请求
     * @param createdBy 创建人用户名
     * @return 授权响应
     */
    @Transactional
    public LicenseResponse createLicense(LicenseRequest request, String createdBy) {
        logger.info("开始创建授权，请求: {}, 创建人: {}", request, createdBy);

        try {
            // 八耻八荣：参数验证
            validateLicenseRequest(request);

            // 生成序列号
            String serialNumber = generatorService.generateSerialNumber(request.getExpiryDate());

            // 生成授权码
            String licenseCode = generatorService.generateLicenseCode(serialNumber, request.getEdition());

            // 创建授权实体
            License license = new License();
            license.setSerialNumber(serialNumber);
            license.setLicenseCode(licenseCode);
            license.setEdition(request.getEdition().toUpperCase());
            license.setStatus("ACTIVE");
            license.setIssuedTo(request.getIssuedTo());
            license.setIssuedDate(LocalDate.now());
            license.setExpiryDate(request.getExpiryDate());
            license.setIsCurrent(false);  // 新创建的授权默认不是当前授权
            license.setCreatedBy(createdBy);
            license.setNotes(request.getNotes());

            // 保存到数据库
            License savedLicense = licenseRepository.save(license);

            logger.info("成功创建授权，ID: {}, 序列号: {}", savedLicense.getId(), savedLicense.getSerialNumber());

            return LicenseResponse.fromEntity(savedLicense);

        } catch (Exception e) {
            logger.error("创建授权失败", e);
            throw new RuntimeException("创建授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置为当前激活授权
     * 关键方法：同步到 system_settings 表
     *
     * @param licenseId 授权 ID
     */
    @Transactional
    public void setAsCurrentLicense(Long licenseId) {
        logger.info("开始设置当前授权，ID: {}", licenseId);

        try {
            // 八耻八荣：参数验证
            if (licenseId == null) {
                throw new IllegalArgumentException("授权 ID 不能为空");
            }

            // 查找授权
            Optional<License> licenseOpt = licenseRepository.findById(licenseId);
            if (!licenseOpt.isPresent()) {
                throw new IllegalArgumentException("授权不存在，ID: " + licenseId);
            }

            License license = licenseOpt.get();

            // 验证授权状态
            if (!"ACTIVE".equals(license.getStatus())) {
                throw new IllegalStateException("只能设置状态为 ACTIVE 的授权为当前授权");
            }

            // 1. 清除所有授权的 is_current 标志
            licenseRepository.clearAllCurrentFlags();
            logger.info("已清除所有授权的 is_current 标志");

            // 2. 设置新的当前授权
            license.setIsCurrent(true);
            licenseRepository.save(license);
            logger.info("已设置授权为当前授权，ID: {}", licenseId);

            // 3. 同步到 system_settings 表（关键步骤）
            syncToSystemSettings(license);
            logger.info("已同步授权信息到 system_settings 表");

            logger.info("成功设置当前授权，序列号: {}", license.getSerialNumber());

        } catch (Exception e) {
            logger.error("设置当前授权失败，ID: {}", licenseId, e);
            throw new RuntimeException("设置当前授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步授权信息到 system_settings 表
     * 保持双表数据一致性
     *
     * @param license 授权实体
     */
    private void syncToSystemSettings(License license) {
        logger.info("开始同步授权到 system_settings 表，序列号: {}", license.getSerialNumber());

        try {
            // 八耻八荣：null 检查
            if (license == null) {
                throw new IllegalArgumentException("授权对象不能为空");
            }

            // 同步序列号
            systemSettingService.saveSetting(
                    "serial_number",
                    license.getSerialNumber(),
                    "code",
                    "产品序列号（有效期至" + license.getExpiryDate() + "）"
            );

            // 同步授权码
            systemSettingService.saveSetting(
                    "license_code",
                    license.getLicenseCode(),
                    "code",
                    "蟑螂恶霸团队授权码（" + getEditionText(license.getEdition()) + "）"
            );

            // 同步授权持有者
            if (license.getIssuedTo() != null && !license.getIssuedTo().trim().isEmpty()) {
                systemSettingService.saveSetting(
                        "license_owner",
                        license.getIssuedTo(),
                        "text",
                        "授权持有者名称"
                );
            }

            // 同步颁发日期
            if (license.getIssuedDate() != null) {
                systemSettingService.saveSetting(
                        "license_issued_date",
                        license.getIssuedDate().toString(),
                        "date",
                        "授权颁发日期"
                );
            }

            logger.info("成功同步授权到 system_settings 表");

        } catch (Exception e) {
            logger.error("同步授权到 system_settings 失败", e);
            throw new RuntimeException("同步授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 延长授权有效期
     *
     * @param licenseId     授权 ID
     * @param newExpiryDate 新的过期日期
     */
    @Transactional
    public void extendLicense(Long licenseId, LocalDate newExpiryDate) {
        logger.info("开始延长授权有效期，ID: {}, 新过期日期: {}", licenseId, newExpiryDate);

        try {
            // 八耻八荣：参数验证
            if (licenseId == null) {
                throw new IllegalArgumentException("授权 ID 不能为空");
            }
            if (newExpiryDate == null) {
                throw new IllegalArgumentException("新过期日期不能为空");
            }

            // 查找授权
            Optional<License> licenseOpt = licenseRepository.findById(licenseId);
            if (!licenseOpt.isPresent()) {
                throw new IllegalArgumentException("授权不存在，ID: " + licenseId);
            }

            License license = licenseOpt.get();

            // 验证新日期必须晚于当前过期日期
            if (newExpiryDate.isBefore(license.getExpiryDate()) || newExpiryDate.isEqual(license.getExpiryDate())) {
                throw new IllegalArgumentException("新过期日期必须晚于当前过期日期");
            }

            // 更新过期日期
            license.setExpiryDate(newExpiryDate);

            // 如果授权已过期，重新激活
            if ("EXPIRED".equals(license.getStatus())) {
                license.setStatus("ACTIVE");
                logger.info("授权已过期，延期后重新激活");
            }

            // 保存
            licenseRepository.save(license);

            // 如果是当前授权，同步到 system_settings
            if (Boolean.TRUE.equals(license.getIsCurrent())) {
                syncToSystemSettings(license);
                logger.info("已同步延期后的授权到 system_settings");
            }

            logger.info("成功延长授权有效期，序列号: {}, 新过期日期: {}", license.getSerialNumber(), newExpiryDate);

        } catch (Exception e) {
            logger.error("延长授权失败，ID: {}", licenseId, e);
            throw new RuntimeException("延长授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停用授权
     *
     * @param licenseId 授权 ID
     */
    @Transactional
    public void deactivateLicense(Long licenseId) {
        logger.info("开始停用授权，ID: {}", licenseId);

        try {
            // 八耻八荣：参数验证
            if (licenseId == null) {
                throw new IllegalArgumentException("授权 ID 不能为空");
            }

            // 查找授权
            Optional<License> licenseOpt = licenseRepository.findById(licenseId);
            if (!licenseOpt.isPresent()) {
                throw new IllegalArgumentException("授权不存在，ID: " + licenseId);
            }

            License license = licenseOpt.get();

            // 如果是当前授权，不允许停用
            if (Boolean.TRUE.equals(license.getIsCurrent())) {
                throw new IllegalStateException("不能停用当前激活的授权，请先设置其他授权为当前授权");
            }

            // 设置状态为 INACTIVE
            license.setStatus("INACTIVE");
            licenseRepository.save(license);

            logger.info("成功停用授权，序列号: {}", license.getSerialNumber());

        } catch (Exception e) {
            logger.error("停用授权失败，ID: {}", licenseId, e);
            throw new RuntimeException("停用授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 激活授权
     *
     * @param licenseId 授权 ID
     */
    @Transactional
    public void activateLicense(Long licenseId) {
        logger.info("开始激活授权，ID: {}", licenseId);

        try {
            // 八耻八荣：参数验证
            if (licenseId == null) {
                throw new IllegalArgumentException("授权 ID 不能为空");
            }

            // 查找授权
            Optional<License> licenseOpt = licenseRepository.findById(licenseId);
            if (!licenseOpt.isPresent()) {
                throw new IllegalArgumentException("授权不存在，ID: " + licenseId);
            }

            License license = licenseOpt.get();

            // 检查是否已过期
            if (LocalDate.now().isAfter(license.getExpiryDate())) {
                throw new IllegalStateException("授权已过期，无法激活，请先延长有效期");
            }

            // 设置状态为 ACTIVE
            license.setStatus("ACTIVE");
            licenseRepository.save(license);

            logger.info("成功激活授权，序列号: {}", license.getSerialNumber());

        } catch (Exception e) {
            logger.error("激活授权失败，ID: {}", licenseId, e);
            throw new RuntimeException("激活授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过序列号和授权码激活授权（新增方法）
     * 用于管理员或客户输入授权码进行系统激活
     *
     * @param serialNumber 序列号
     * @param licenseCode 授权码
     */
    @Transactional
    public void activateLicenseByCode(String serialNumber, String licenseCode) {
        logger.info("开始通过授权码激活系统，序列号: {}", serialNumber);

        try {
            // 八耻八荣：参数验证
            if (serialNumber == null || serialNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("序列号不能为空");
            }
            if (licenseCode == null || licenseCode.trim().isEmpty()) {
                throw new IllegalArgumentException("授权码不能为空");
            }

            // 查找匹配的授权
            Optional<License> licenseOpt = licenseRepository.findBySerialNumberAndLicenseCode(
                    serialNumber.trim(),
                    licenseCode.trim()
            );

            if (!licenseOpt.isPresent()) {
                logger.warn("序列号或授权码无效，序列号: {}", serialNumber);
                throw new IllegalArgumentException("序列号或授权码无效，请检查后重试");
            }

            License license = licenseOpt.get();

            // 检查授权是否已过期
            if (LocalDate.now().isAfter(license.getExpiryDate())) {
                logger.warn("授权已过期，序列号: {}, 过期日期: {}", serialNumber, license.getExpiryDate());
                throw new IllegalStateException("授权已过期（有效期至 " + license.getExpiryDate() + "），无法激活系统");
            }

            // 检查授权状态
            if ("INACTIVE".equals(license.getStatus())) {
                logger.warn("授权已停用，序列号: {}", serialNumber);
                throw new IllegalStateException("该授权已被停用，无法激活系统");
            }

            // 调用现有的设置当前授权方法
            // 会自动清除其他授权的 is_current 标志，并同步到 system_settings
            setAsCurrentLicense(license.getId());

            logger.info("成功通过授权码激活系统，序列号: {}, 授权给: {}", serialNumber, license.getIssuedTo());

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            logger.error("通过授权码激活系统失败，序列号: {}", serialNumber, e);
            throw new RuntimeException("激活系统失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除授权
     *
     * @param licenseId 授权 ID
     */
    @Transactional
    public void deleteLicense(Long licenseId) {
        logger.info("开始删除授权，ID: {}", licenseId);

        try {
            // 八耻八荣：参数验证
            if (licenseId == null) {
                throw new IllegalArgumentException("授权 ID 不能为空");
            }

            // 查找授权
            Optional<License> licenseOpt = licenseRepository.findById(licenseId);
            if (!licenseOpt.isPresent()) {
                throw new IllegalArgumentException("授权不存在，ID: " + licenseId);
            }

            License license = licenseOpt.get();

            // 如果是当前授权，不允许删除
            if (Boolean.TRUE.equals(license.getIsCurrent())) {
                throw new IllegalStateException("不能删除当前激活的授权，请先设置其他授权为当前授权");
            }

            // 删除
            licenseRepository.deleteById(licenseId);

            logger.info("成功删除授权，序列号: {}", license.getSerialNumber());

        } catch (Exception e) {
            logger.error("删除授权失败，ID: {}", licenseId, e);
            throw new RuntimeException("删除授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取当前激活的授权
     *
     * @return 当前授权响应
     */
    public LicenseResponse getCurrentLicense() {
        try {
            Optional<License> licenseOpt = licenseRepository.findByIsCurrentTrue();

            if (!licenseOpt.isPresent()) {
                logger.warn("未找到当前激活的授权");
                return null;
            }

            return LicenseResponse.fromEntity(licenseOpt.get());

        } catch (Exception e) {
            logger.error("获取当前授权失败", e);
            return null;
        }
    }

    /**
     * 获取所有授权列表
     *
     * @return 授权列表
     */
    public List<LicenseResponse> getAllLicenses() {
        try {
            List<License> licenses = licenseRepository.findAllByOrderByCreatedAtDesc();

            return licenses.stream()
                    .map(LicenseResponse::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("获取授权列表失败", e);
            throw new RuntimeException("获取授权列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索授权
     *
     * @param keyword 搜索关键词
     * @return 匹配的授权列表
     */
    public List<LicenseResponse> searchLicenses(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getAllLicenses();
            }

            List<License> licenses = licenseRepository.searchLicenses(keyword.trim());

            return licenses.stream()
                    .map(LicenseResponse::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("搜索授权失败，关键词: {}", keyword, e);
            throw new RuntimeException("搜索授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证授权请求
     *
     * @param request 授权请求
     */
    private void validateLicenseRequest(LicenseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("授权请求不能为空");
        }

        if (request.getIssuedTo() == null || request.getIssuedTo().trim().isEmpty()) {
            throw new IllegalArgumentException("授权持有者不能为空");
        }

        if (request.getEdition() == null || request.getEdition().trim().isEmpty()) {
            throw new IllegalArgumentException("版本不能为空");
        }

        if (request.getExpiryDate() == null) {
            throw new IllegalArgumentException("过期日期不能为空");
        }

        // 验证过期日期必须是未来的日期
        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("过期日期必须是未来的日期");
        }
    }

    /**
     * 获取版本文本（中文）
     *
     * @param edition 版本代码
     * @return 版本文本
     */
    private String getEditionText(String edition) {
        if (edition == null) {
            return "未知版本";
        }

        switch (edition.toUpperCase()) {
            case "PRO":
                return "专业版";
            case "ENTERPRISE":
                return "企业版";
            case "TRIAL":
                return "试用版";
            default:
                return edition;
        }
    }
}
