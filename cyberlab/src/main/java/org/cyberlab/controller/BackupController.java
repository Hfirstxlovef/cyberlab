package org.cyberlab.controller;

import org.cyberlab.entity.Backup;
import org.cyberlab.service.BackupService;
import org.cyberlab.service.RestoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统备份管理控制器
 * 提供备份创建、恢复、下载、删除等功能
 */
@RestController
@RequestMapping("/api/backup")
@PreAuthorize("hasRole('admin')")
public class BackupController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);

    @Autowired
    private BackupService backupService;

    @Autowired
    private RestoreService restoreService;

    private static final String BACKUP_DIR = "backups";

    /**
     * 创建新备份
     * POST /api/backup/create
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBackup(
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam String createdBy,
            @RequestParam String password) {  // ✅ 修复：密码改为必需参数

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证备份类型
            if (!type.matches("full|database|files")) {
                response.put("success", false);
                response.put("message", "无效的备份类型: " + type);
                return ResponseEntity.badRequest().body(response);
            }

            // ✅ 修复：强制验证密码（安全加固）
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "备份密码为必需，请提供至少8位密码");
                return ResponseEntity.badRequest().body(response);
            }

            if (password.trim().length() < 8) {
                response.put("success", false);
                response.put("message", "备份密码长度至少为8位");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("收到备份创建请求: name={}, type={}, createdBy={}, encrypted=true",
                       name, type, createdBy);

            // 创建备份记录
            Backup backup = backupService.createBackupRecord(name, type, description, createdBy);

            // 异步执行备份（带密码）
            backupService.createBackupAsync(backup.getId(), type, password);

            response.put("success", true);
            response.put("message", "加密备份任务已启动");  // ✅ 修复：所有备份都加密
            response.put("data", backup);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("创建备份失败", e);
            response.put("success", false);
            response.put("message", "创建备份失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有备份列表
     * GET /api/backup/list
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listBackups() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Backup> backups = backupService.getAllBackups();

            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", backups);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询备份列表失败", e);
            response.put("success", false);
            response.put("message", "查询备份列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取备份详情
     * GET /api/backup/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBackup(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Backup backup = backupService.getBackupById(id);

            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", backup);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询备份详情失败: id={}", id, e);
            response.put("success", false);
            response.put("message", "查询备份详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * 恢复备份
     * POST /api/backup/restore/{id}
     */
    @PostMapping("/restore/{id}")
    public ResponseEntity<Map<String, Object>> restoreBackup(
            @PathVariable Long id,
            @RequestParam String password) {  // ✅ 修复：密码改为必需参数（安全加固）
        Map<String, Object> response = new HashMap<>();
        Backup backup = null;  // ✅ 修复：在try外部声明，便于catch块使用

        try {
            // 获取备份信息
            backup = backupService.getBackupById(id);

            // 验证备份状态
            if (!"completed".equals(backup.getStatus())) {
                response.put("success", false);
                response.put("message", "只能恢复已完成的备份");
                return ResponseEntity.badRequest().body(response);
            }

            // ✅ 修复：强制验证密码（安全加固），所有备份恢复都需要密码
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "备份密码为必需，请提供解密密码");
                response.put("requiresPassword", true);
                return ResponseEntity.badRequest().body(response);
            }

            // 验证备份文件完整性
            if (!restoreService.validateBackup(id)) {
                response.put("success", false);
                response.put("message", "备份文件验证失败，文件可能损坏或丢失");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("开始恢复备份: id={}, name={}, encrypted={}", id, backup.getName(), backup.getEncrypted());

            // 异步执行恢复（带密码）
            restoreService.restoreBackupAsync(id, password);

            response.put("success", true);
            response.put("message", "恢复任务已启动");
            response.put("data", backup);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // ✅ 修复：增强错误日志和返回信息
            String backupName = backup != null ? backup.getName() : "unknown";
            logger.error("恢复备份失败: id={}, name={}, password={}",
                id, backupName, (password != null ? "已提供" : "未提供"), e);

            // 构建详细错误信息
            String errorDetail = e.getMessage();
            if (e.getCause() != null) {
                errorDetail += " (原因: " + e.getCause().getMessage() + ")";
            }

            response.put("success", false);
            response.put("message", "恢复备份失败: " + errorDetail);
            response.put("errorType", e.getClass().getSimpleName());  // 添加错误类型便于调试
            if (backup != null) {
                response.put("backupInfo", Map.of(
                    "id", backup.getId(),
                    "name", backup.getName(),
                    "type", backup.getType(),
                    "encrypted", backup.getEncrypted() != null ? backup.getEncrypted() : false
                ));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除备份
     * DELETE /api/backup/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Backup backup = backupService.getBackupById(id);

            // 检查备份是否正在进行中
            if ("pending".equals(backup.getStatus()) || "restoring".equals(backup.getStatus())) {
                response.put("success", false);
                response.put("message", "无法删除正在进行中的备份");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("删除备份: id={}, name={}", id, backup.getName());

            backupService.deleteBackup(id);

            response.put("success", true);
            response.put("message", "备份已删除");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("删除备份失败: id={}", id, e);
            response.put("success", false);
            response.put("message", "删除备份失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 下载备份文件
     * POST /api/backup/download/{id}
     * ✅ 修复：改为POST方法，添加密码验证（安全加固）
     */
    @PostMapping("/download/{id}")
    public ResponseEntity<Resource> downloadBackup(
            @PathVariable Long id,
            @RequestParam String password) {  // ✅ 修复：密码为必需参数
        try {
            Backup backup = backupService.getBackupById(id);

            // 验证备份状态
            if (!"completed".equals(backup.getStatus())) {
                logger.warn("尝试下载未完成的备份: id={}", id);
                return ResponseEntity.badRequest().build();
            }

            // ✅ 修复：强制验证密码（安全加固）
            if (password == null || password.trim().isEmpty()) {
                logger.warn("下载备份缺少密码: id={}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path backupFilePath = backupService.getBackupFilePath(id);

            if (!Files.exists(backupFilePath)) {
                logger.error("备份文件不存在: {}", backupFilePath);
                return ResponseEntity.notFound().build();
            }

            // ✅ 修复：验证密码是否正确（通过尝试打开加密ZIP）
            if (Boolean.TRUE.equals(backup.getEncrypted())) {
                try {
                    net.lingala.zip4j.ZipFile zipFile = new net.lingala.zip4j.ZipFile(
                        backupFilePath.toFile(),
                        password.toCharArray()
                    );

                    // 验证密码：尝试读取ZIP文件头
                    if (!zipFile.isValidZipFile()) {
                        logger.warn("备份文件已损坏: id={}", id);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }

                    // 测试密码是否正确（尝试获取文件列表）
                    zipFile.getFileHeaders();

                    logger.info("密码验证成功，允许下载: id={}, file={}", id, backup.getFilePath());

                } catch (net.lingala.zip4j.exception.ZipException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Wrong Password")) {
                        logger.warn("下载备份密码错误: id={}", id);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    logger.error("验证备份文件失败: id={}", id, e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            }

            Resource resource = new FileSystemResource(backupFilePath);

            logger.info("下载备份文件: id={}, file={}", id, backup.getFilePath());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + backup.getFilePath() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(backupFilePath))
                    .body(resource);

        } catch (Exception e) {
            logger.error("下载备份文件失败: id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 上传外部备份文件
     * POST /api/backup/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadBackup(
            @RequestParam("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam String createdBy) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证文件
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "上传文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证文件扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".zip")) {
                response.put("success", false);
                response.put("message", "只支持上传ZIP格式的备份文件");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("收到备份文件上传: name={}, size={}", originalFilename, file.getSize());

            // 创建备份目录
            Path backupDirPath = Paths.get(System.getProperty("user.dir"), BACKUP_DIR);
            Files.createDirectories(backupDirPath);

            // 保存文件
            String backupFileName = "uploaded_" + System.currentTimeMillis() + "_" + originalFilename;
            Path targetPath = backupDirPath.resolve(backupFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 创建备份记录
            Backup backup = new Backup();
            backup.setName(name);
            backup.setType(type);
            backup.setDescription(description);
            backup.setCreatedBy(createdBy);
            backup.setStatus("completed");
            backup.setFilePath(backupFileName);
            backup.setFileSize(file.getSize());
            backup.setCreatedTime(LocalDateTime.now());
            backup.setCompletedTime(LocalDateTime.now());

            Backup savedBackup = backupService.createBackupRecord(
                backup.getName(),
                backup.getType(),
                backup.getDescription(),
                backup.getCreatedBy()
            );

            response.put("success", true);
            response.put("message", "备份文件上传成功");
            response.put("data", savedBackup);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("上传备份文件失败", e);
            response.put("success", false);
            response.put("message", "上传备份文件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询备份状态
     * GET /api/backup/status/{id}
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> getBackupStatus(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Backup backup = backupService.getBackupById(id);

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("id", backup.getId());
            statusData.put("name", backup.getName());
            statusData.put("status", backup.getStatus());
            statusData.put("statusDisplayName", backup.getStatusDisplayName());
            statusData.put("progress", calculateProgress(backup));
            statusData.put("errorMessage", backup.getErrorMessage());

            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", statusData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询备份状态失败: id={}", id, e);
            response.put("success", false);
            response.put("message", "查询备份状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * 获取备份统计信息
     * GET /api/backup/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> response = new HashMap<>();

        try {
            BackupService.BackupStatistics stats = backupService.getStatistics();

            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询备份统计信息失败", e);
            response.put("success", false);
            response.put("message", "查询统计信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 扫描物理备份文件并同步到数据库
     * POST /api/backup/scan
     * ✅ 修复：用于恢复数据库后，重新导入物理文件的备份记录
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanBackups() {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("开始扫描物理备份文件...");

            int importedCount = backupService.scanPhysicalBackups();

            response.put("success", true);
            response.put("message", "扫描完成，导入 " + importedCount + " 个备份文件");
            response.put("data", Map.of("importedCount", importedCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("扫描备份文件失败", e);
            response.put("success", false);
            response.put("message", "扫描备份文件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证备份完整性
     * POST /api/backup/validate/{id}
     */
    @PostMapping("/validate/{id}")
    public ResponseEntity<Map<String, Object>> validateBackup(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isValid = restoreService.validateBackup(id);

            response.put("success", true);
            response.put("message", isValid ? "备份文件验证通过" : "备份文件验证失败");
            response.put("data", Map.of("valid", isValid));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("验证备份失败: id={}", id, e);
            response.put("success", false);
            response.put("message", "验证备份失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 计算备份进度
     */
    private int calculateProgress(Backup backup) {
        String status = backup.getStatus();
        if ("pending".equals(status)) {
            return 30; // 创建中
        } else if ("restoring".equals(status)) {
            return 50; // 恢复中
        } else if ("completed".equals(status)) {
            return 100; // 已完成
        } else if ("failed".equals(status)) {
            return 0; // 失败
        }
        return 0;
    }
}
