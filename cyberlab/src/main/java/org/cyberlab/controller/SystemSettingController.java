package org.cyberlab.controller;

import org.cyberlab.annotation.LogOperation;
import org.cyberlab.service.LicenseService;
import org.cyberlab.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class SystemSettingController {

    @Autowired
    private SystemSettingService settingService;

    @Autowired
    private LicenseService licenseService;

    @Value("${file.upload.path:/uploads}")
    private String uploadPath;

    // 获取所有系统设置 - 所有用户都可以访问基础设置
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSettings() {
        Map<String, String> settings = settingService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    // 获取公共系统设置（不需要管理员权限的设置）
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> getPublicSettings() {
        Map<String, String> publicSettings = settingService.getPublicSettings();
        return ResponseEntity.ok(publicSettings);
    }

    // 保存系统设置 - 仅管理员可访问
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @LogOperation("保存系统设置")
    public ResponseEntity<Map<String, Object>> saveSettings(@RequestBody Map<String, String> settings) {
        try {
            settingService.saveSettings(settings);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "系统设置保存成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "保存失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 上传Logo - 仅管理员可访问
    @PostMapping("/upload/logo")
    @PreAuthorize("hasRole('admin')")
    @LogOperation("上传系统Logo")
    public ResponseEntity<Map<String, Object>> uploadLogo(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "请上传图片文件");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证文件大小 (限制2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "文件大小不能超过2MB");
                return ResponseEntity.badRequest().body(response);
            }

            // 创建上传目录
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名 - 修复空指针问题
            String originalFilename = file.getOriginalFilename();
            String extension;
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                // 如果文件名为空或没有扩展名，根据Content-Type设置默认扩展名
                switch (contentType) {
                    case "image/jpeg":
                        extension = ".jpg";
                        break;
                    case "image/png":
                        extension = ".png";
                        break;
                    case "image/gif":
                        extension = ".gif";
                        break;
                    case "image/webp":
                        extension = ".webp";
                        break;
                    default:
                        extension = ".jpg"; // 默认扩展名
                }
            }
            String filename = "logo_" + UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, filename);
            Files.copy(file.getInputStream(), filePath);
            
            // 返回文件访问路径
            String fileUrl = "/uploads/" + filename;
            
            // 保存到数据库
            settingService.saveSetting("system_logo", fileUrl, "image", "系统Logo图片");
            
            response.put("success", true);
            response.put("message", "Logo上传成功");
            response.put("url", fileUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 获取单个设置
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        String value = settingService.getSetting(key);
        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        return ResponseEntity.ok(response);
    }

    /**
     * 验证授权信息
     * 蟑螂恶霸团队 - 授权系统
     * 所有用户都可以访问授权验证信息
     *
     * @return 授权验证结果
     */
    @GetMapping("/license/validate")
    public ResponseEntity<Map<String, Object>> validateLicense() {
        Map<String, Object> licenseInfo = licenseService.validateLicense();
        return ResponseEntity.ok(licenseInfo);
    }
}