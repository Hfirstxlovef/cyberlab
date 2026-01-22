package org.cyberlab.controller;

import org.cyberlab.annotation.LogOperation;
import org.cyberlab.dto.ActivationRequest;
import org.cyberlab.dto.LicenseRequest;
import org.cyberlab.dto.LicenseResponse;
import org.cyberlab.service.LicenseManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 授权管理控制器
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Management Controller for CyberLab Platform
 *
 * 仅 hongan (license_admin) 用户可访问
 *
 * 遵循八耻八荣原则：
 * - 完整的权限控制
 * - 详细的异常处理
 * - 清晰的日志记录
 * - 友好的错误信息
 */
@RestController
@RequestMapping("/api/licenses/manage")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class LicenseManagementController {

    private static final Logger logger = LoggerFactory.getLogger(LicenseManagementController.class);

    @Autowired
    private LicenseManagementService licenseManagementService;

    /**
     * 生成新授权
     * 仅 license_admin 角色可访问
     *
     * @param request        授权请求
     * @param authentication 认证信息
     * @return 生成的授权信息
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('license_admin')")
    @LogOperation("生成新授权")
    public ResponseEntity<Map<String, Object>> generateLicense(
            @RequestBody LicenseRequest request,
            Authentication authentication) {

        logger.info("收到生成授权请求: {}, 用户: {}", request, authentication.getName());

        Map<String, Object> response = new HashMap<>();

        try {
            // 八耻八荣：参数验证
            if (request == null) {
                response.put("success", false);
                response.put("message", "请求参数不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 创建授权
            String createdBy = authentication.getName();
            LicenseResponse licenseResponse = licenseManagementService.createLicense(request, createdBy);

            response.put("success", true);
            response.put("message", "授权生成成功");
            response.put("data", licenseResponse);

            logger.info("授权生成成功，序列号: {}", licenseResponse.getSerialNumber());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("生成授权失败 - 参数错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("生成授权失败", e);
            response.put("success", false);
            response.put("message", "生成授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取所有授权列表
     *
     * @return 授权列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('license_admin')")
    public ResponseEntity<Map<String, Object>> getLicenseList() {
        logger.info("获取授权列表");

        Map<String, Object> response = new HashMap<>();

        try {
            List<LicenseResponse> licenses = licenseManagementService.getAllLicenses();

            response.put("success", true);
            response.put("data", licenses);
            response.put("total", licenses.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取授权列表失败", e);
            response.put("success", false);
            response.put("message", "获取授权列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 搜索授权
     *
     * @param keyword 搜索关键词
     * @return 匹配的授权列表
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('license_admin')")
    public ResponseEntity<Map<String, Object>> searchLicenses(@RequestParam("keyword") String keyword) {
        logger.info("搜索授权，关键词: {}", keyword);

        Map<String, Object> response = new HashMap<>();

        try {
            List<LicenseResponse> licenses = licenseManagementService.searchLicenses(keyword);

            response.put("success", true);
            response.put("data", licenses);
            response.put("total", licenses.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("搜索授权失败", e);
            response.put("success", false);
            response.put("message", "搜索授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取当前激活的授权
     *
     * @return 当前授权信息
     */
    @GetMapping("/current")
    @PreAuthorize("hasRole('license_admin')")
    public ResponseEntity<Map<String, Object>> getCurrentLicense() {
        logger.info("获取当前授权");

        Map<String, Object> response = new HashMap<>();

        try {
            LicenseResponse currentLicense = licenseManagementService.getCurrentLicense();

            if (currentLicense == null) {
                response.put("success", false);
                response.put("message", "未找到当前激活的授权");
                return ResponseEntity.ok(response);
            }

            response.put("success", true);
            response.put("data", currentLicense);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取当前授权失败", e);
            response.put("success", false);
            response.put("message", "获取当前授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 设置为当前激活授权
     * 关键接口：触发双表同步
     *
     * @param id 授权 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasRole('license_admin')")
    @LogOperation("设置当前授权")
    public ResponseEntity<Map<String, Object>> setCurrentLicense(@PathVariable("id") Long id) {
        logger.info("设置当前授权，ID: {}", id);

        Map<String, Object> response = new HashMap<>();

        try {
            licenseManagementService.setAsCurrentLicense(id);

            response.put("success", true);
            response.put("message", "已设置为当前授权，并同步到系统配置");

            logger.info("设置当前授权成功，ID: {}", id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("设置当前授权失败 - 业务错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("设置当前授权失败，ID: {}", id, e);
            response.put("success", false);
            response.put("message", "设置当前授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 延长授权有效期
     *
     * @param id            授权 ID
     * @param newExpiryDate 新的过期日期
     * @return 操作结果
     */
    @PutMapping("/{id}/extend")
    @PreAuthorize("hasRole('license_admin')")
    @LogOperation("延长授权有效期")
    public ResponseEntity<Map<String, Object>> extendLicense(
            @PathVariable("id") Long id,
            @RequestParam("newExpiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newExpiryDate) {

        logger.info("延长授权有效期，ID: {}, 新过期日期: {}", id, newExpiryDate);

        Map<String, Object> response = new HashMap<>();

        try {
            licenseManagementService.extendLicense(id, newExpiryDate);

            response.put("success", true);
            response.put("message", "授权有效期延长成功");

            logger.info("延长授权成功，ID: {}", id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("延长授权失败 - 参数错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("延长授权失败，ID: {}", id, e);
            response.put("success", false);
            response.put("message", "延长授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 激活授权
     *
     * @param id 授权 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('license_admin')")
    @LogOperation("激活授权")
    public ResponseEntity<Map<String, Object>> activateLicense(@PathVariable("id") Long id) {
        logger.info("激活授权，ID: {}", id);

        Map<String, Object> response = new HashMap<>();

        try {
            licenseManagementService.activateLicense(id);

            response.put("success", true);
            response.put("message", "授权激活成功");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("激活授权失败 - 业务错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("激活授权失败，ID: {}", id, e);
            response.put("success", false);
            response.put("message", "激活授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 停用授权
     *
     * @param id 授权 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('license_admin')")
    @LogOperation("停用授权")
    public ResponseEntity<Map<String, Object>> deactivateLicense(@PathVariable("id") Long id) {
        logger.info("停用授权，ID: {}", id);

        Map<String, Object> response = new HashMap<>();

        try {
            licenseManagementService.deactivateLicense(id);

            response.put("success", true);
            response.put("message", "授权停用成功");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("停用授权失败 - 业务错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("停用授权失败，ID: {}", id, e);
            response.put("success", false);
            response.put("message", "停用授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 删除授权
     *
     * @param id 授权 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('license_admin')")
    @LogOperation("删除授权")
    public ResponseEntity<Map<String, Object>> deleteLicense(@PathVariable("id") Long id) {
        logger.info("删除授权，ID: {}", id);

        Map<String, Object> response = new HashMap<>();

        try {
            licenseManagementService.deleteLicense(id);

            response.put("success", true);
            response.put("message", "授权删除成功");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("删除授权失败 - 业务错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("删除授权失败，ID: {}", id, e);
            response.put("success", false);
            response.put("message", "删除授权失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 通过授权码激活系统（新增接口）
     * 允许 admin 或其他角色通过输入序列号和授权码激活系统
     * 不限于 license_admin，因为这是系统激活流程的关键步骤
     *
     * @param request 激活请求（包含序列号和授权码）
     * @return 操作结果
     */
    @PostMapping("/activate-by-code")
    @PreAuthorize("hasAnyRole('admin', 'license_admin')")
    @LogOperation("通过授权码激活系统")
    public ResponseEntity<Map<String, Object>> activateByCode(@RequestBody ActivationRequest request) {
        logger.info("收到授权码激活请求，序列号: {}", request.getSerialNumber());

        Map<String, Object> response = new HashMap<>();

        try {
            // 八耻八荣：参数验证
            if (request == null) {
                response.put("success", false);
                response.put("message", "请求参数不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 调用服务层进行激活
            licenseManagementService.activateLicenseByCode(
                    request.getSerialNumber(),
                    request.getLicenseCode()
            );

            response.put("success", true);
            response.put("message", "系统激活成功！授权信息已同步到系统配置");

            logger.info("系统激活成功，序列号: {}", request.getSerialNumber());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("系统激活失败 - 参数错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            logger.warn("系统激活失败 - 状态错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("系统激活失败", e);
            response.put("success", false);
            response.put("message", "系统激活失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
