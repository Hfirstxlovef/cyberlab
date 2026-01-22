package org.cyberlab.service;

import org.cyberlab.entity.Backup;
import org.cyberlab.repository.BackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.lingala.zip4j.ZipFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 系统恢复服务
 * 负责从备份文件恢复系统数据
 */
@Service
public class RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(RestoreService.class);

    @Autowired
    private BackupRepository backupRepository;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${file.upload.path:${user.dir}/uploads}")
    private String uploadsPath;

    private static final String BACKUP_DIR = "backups";
    private static final String RESTORE_TEMP_DIR = "temp_restore";

    /**
     * 恢复备份（异步）
     * @param backupId 备份记录ID
     * @param password 解密密码（如果备份已加密）
     *
     * ✅ 修复：移除@Transactional注解，避免长时间持有数据库连接
     * 数据库操作通过独立的事务方法执行（短事务），mysql命令在事务外执行
     */
    @Async
    public void restoreBackupAsync(Long backupId, String password) {
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));

        // ✅ 修复：使用独立的事务方法更新状态（短事务）
        updateBackupStatus(backupId, "restoring", null);

        try {
            logger.info("开始恢复备份: {}", backup.getName());

            // 获取备份文件路径
            Path backupFilePath = Paths.get(System.getProperty("user.dir"), BACKUP_DIR, backup.getFilePath());

            if (!Files.exists(backupFilePath)) {
                throw new RuntimeException("备份文件不存在: " + backupFilePath);
            }

            // 创建临时恢复目录（先清理旧数据）
            Path tempRestoreDir = Paths.get(System.getProperty("user.dir"), RESTORE_TEMP_DIR);

            // ✅ 修复：如果临时目录已存在，先完全删除以避免旧文件干扰
            if (Files.exists(tempRestoreDir)) {
                logger.info("清理旧的临时恢复目录: {}", tempRestoreDir);
                deleteDirectory(tempRestoreDir.toFile());
            }

            Files.createDirectories(tempRestoreDir);
            logger.info("创建临时恢复目录: {}", tempRestoreDir);

            // 检查备份是否加密
            if (Boolean.TRUE.equals(backup.getEncrypted())) {
                // 加密备份需要密码
                if (password == null || password.trim().isEmpty()) {
                    throw new RuntimeException("该备份已加密，请提供解密密码");
                }
                logger.info("备份已加密，使用密码解压...");
                unzipEncryptedBackup(backupFilePath, tempRestoreDir, password);
            } else {
                // 普通备份
                logger.info("解压备份文件...");
                unzipBackup(backupFilePath, tempRestoreDir);
            }

            // 根据备份类型恢复不同内容
            String type = backup.getType();
            if ("full".equals(type) || "database".equals(type)) {
                // 恢复数据库
                restoreDatabase(tempRestoreDir);
            }

            if ("full".equals(type) || "files".equals(type)) {
                // 恢复文件
                restoreFiles(tempRestoreDir);
            }

            if ("full".equals(type)) {
                // 恢复配置文件（仅完整备份包含配置）
                restoreConfiguration(tempRestoreDir);
            }

            // 清理临时目录
            deleteDirectory(tempRestoreDir.toFile());

            // ✅ 修复：使用独立的事务方法更新状态（短事务）
            updateBackupStatus(backupId, "completed", null);

            // ✅ 修复：恢复完成后，同步所有备份文件的大小
            // 解决数据库恢复导致 fileSize 被还原为旧值（0）的问题
            syncBackupFileSizes();

            logger.info("备份恢复成功: {}", backup.getName());

        } catch (Exception e) {
            logger.error("备份恢复失败: {}", backup.getName(), e);
            // ✅ 修复：使用独立的事务方法更新状态（短事务）
            updateBackupStatus(backupId, "failed", "恢复失败: " + e.getMessage());
            throw new RuntimeException("备份恢复失败: " + e.getMessage());
        }
    }

    /**
     * 更新备份状态（独立事务方法）
     * ✅ 修复：将数据库更新操作独立出来，使用短事务，避免长时间持有连接
     * @param backupId 备份ID
     * @param status 状态
     * @param errorMessage 错误信息（可选）
     */
    @Transactional
    protected void updateBackupStatus(Long backupId, String status, String errorMessage) {
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));
        backup.setStatus(status);
        backup.setErrorMessage(errorMessage);
        backupRepository.save(backup);
    }

    /**
     * 解压加密的备份文件
     * @param zipFilePath 加密ZIP文件路径
     * @param destDir 目标解压目录
     * @param password 解密密码
     */
    private void unzipEncryptedBackup(Path zipFilePath, Path destDir, String password) throws IOException {
        try {
            // 使用zip4j解压加密ZIP
            ZipFile zipFile = new ZipFile(zipFilePath.toFile(), password.toCharArray());

            // 验证密码是否正确
            if (!zipFile.isValidZipFile()) {
                throw new RuntimeException("备份文件已损坏或不是有效的ZIP文件");
            }

            // 解压到目标目录
            zipFile.extractAll(destDir.toString());

            logger.info("加密备份文件解压完成");

        } catch (net.lingala.zip4j.exception.ZipException e) {
            if (e.getMessage().contains("Wrong Password")) {
                throw new RuntimeException("解密密码错误，无法恢复备份");
            }
            throw new RuntimeException("解压加密备份失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("解压加密备份失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解压备份文件（普通未加密）
     */
    private void unzipBackup(Path zipFilePath, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // 创建父目录
                    Files.createDirectories(entryPath.getParent());

                    // 写入文件
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        logger.info("备份文件解压完成");
    }

    /**
     * 恢复数据库
     */
    private void restoreDatabase(Path tempRestoreDir) throws Exception {
        logger.info("开始恢复数据库...");

        // 查找SQL文件
        Path sqlFile = findSqlFile(tempRestoreDir);
        if (sqlFile == null || !Files.exists(sqlFile)) {
            throw new RuntimeException("未找到数据库备份文件");
        }

        // 解析数据库名称
        String dbName = extractDatabaseName(datasourceUrl);

        // 使用mysql命令恢复数据库
        // ✅ 修复：使用MYSQL_PWD环境变量传递密码，避免命令行警告
        ProcessBuilder pb = new ProcessBuilder(
            "mysql",
            "-u", datasourceUsername,
            dbName
        );

        // 设置MYSQL_PWD环境变量（避免密码警告）
        pb.environment().put("MYSQL_PWD", datasourcePassword);

        // 从SQL文件读取输入
        pb.redirectInput(sqlFile.toFile());

        // 捕获错误输出
        Path errorLog = Files.createTempFile("restore_error_", ".log");
        pb.redirectError(errorLog.toFile());

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // 读取错误信息
            String error = new String(Files.readAllBytes(errorLog));
            Files.deleteIfExists(errorLog);
            throw new RuntimeException("mysql执行失败: " + error);
        }

        Files.deleteIfExists(errorLog);
        logger.info("数据库恢复完成");
    }

    /**
     * 恢复文件
     */
    private void restoreFiles(Path tempRestoreDir) throws Exception {
        logger.info("开始恢复文件...");

        Path uploadsBackup = tempRestoreDir.resolve("uploads");
        if (!Files.exists(uploadsBackup)) {
            logger.warn("未找到文件备份");
            return;
        }

        Path uploadsDir = Paths.get(uploadsPath);

        // 创建备份当前文件
        if (Files.exists(uploadsDir)) {
            Path backupCurrent = Paths.get(uploadsPath + "_backup_" +
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            logger.info("备份当前文件到: {}", backupCurrent);
            copyDirectory(uploadsDir.toFile(), backupCurrent.toFile());
        }

        // 删除当前文件
        if (Files.exists(uploadsDir)) {
            deleteDirectory(uploadsDir.toFile());
        }

        // 恢复备份文件
        Files.createDirectories(uploadsDir);
        copyDirectory(uploadsBackup.toFile(), uploadsDir.toFile());

        logger.info("文件恢复完成");
    }

    /**
     * 恢复配置文件（完善版）
     * 恢复所有关键配置文件，包括SSL证书
     */
    private void restoreConfiguration(Path tempRestoreDir) throws Exception {
        logger.info("开始恢复配置文件...");

        Path configBackup = tempRestoreDir.resolve("config");
        if (!Files.exists(configBackup)) {
            logger.warn("未找到配置文件备份目录");
            return;
        }

        // 目标配置目录（项目资源目录）
        Path resourcesDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources");

        // 配置文件列表
        String[] configFiles = {
            "application.yml",
            "application.properties",
            "application-container.properties",
            "keystore.p12"
        };

        int restoredCount = 0;

        for (String configFile : configFiles) {
            Path backupFile = configBackup.resolve(configFile);

            if (Files.exists(backupFile)) {
                // 备份当前配置文件（如果存在）
                Path targetFile = resourcesDir.resolve(configFile);
                if (Files.exists(targetFile)) {
                    Path backupCurrent = resourcesDir.resolve(configFile + ".backup_" +
                        LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
                    Files.copy(targetFile, backupCurrent, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("当前配置已备份: {}", backupCurrent.getFileName());
                }

                // 恢复配置文件
                Files.copy(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                restoredCount++;
                logger.info("配置文件已恢复: {}", configFile);
            } else {
                logger.warn("备份中未找到配置文件: {}", configFile);
            }
        }

        logger.info("配置文件恢复完成，成功恢复 {} 个文件", restoredCount);
        logger.warn("重要提示：配置文件已更新，请重启应用程序使配置生效！");
    }

    /**
     * 查找SQL文件（返回最新修改时间的SQL文件）
     * ✅ 修复：使用递归查找，支持子目录中的SQL文件
     */
    private Path findSqlFile(Path dir) throws IOException {
        Path latestSqlFile = null;
        long latestTime = 0;

        // ✅ 修复：使用 Files.walk() 递归查找所有.sql文件
        try (var stream = Files.walk(dir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".sql"))) {

            for (Path entry : (Iterable<Path>) stream::iterator) {
                long modTime = Files.getLastModifiedTime(entry).toMillis();
                if (latestSqlFile == null || modTime > latestTime) {
                    latestSqlFile = entry;
                    latestTime = modTime;
                }
            }
        }

        if (latestSqlFile != null) {
            logger.info("找到SQL备份文件: {} (完整路径: {})",
                       latestSqlFile.getFileName(), latestSqlFile);
        } else {
            logger.warn("在目录 {} 中未找到SQL文件，目录内容: {}",
                       dir, listDirectoryContents(dir));
        }

        return latestSqlFile;
    }

    /**
     * 列出目录内容（用于调试）
     */
    private String listDirectoryContents(Path dir) {
        try {
            if (!Files.exists(dir)) {
                return "[目录不存在]";
            }
            return Files.list(dir)
                    .map(p -> p.getFileName().toString())
                    .limit(20)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
        } catch (IOException e) {
            return "[无法读取目录: " + e.getMessage() + "]";
        }
    }

    /**
     * 从JDBC URL中提取数据库名称
     */
    private String extractDatabaseName(String jdbcUrl) {
        int lastSlash = jdbcUrl.lastIndexOf('/');
        int questionMark = jdbcUrl.indexOf('?', lastSlash);

        if (questionMark > 0) {
            return jdbcUrl.substring(lastSlash + 1, questionMark);
        } else {
            return jdbcUrl.substring(lastSlash + 1);
        }
    }

    /**
     * 递归复制目录
     */
    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }

    /**
     * 验证备份文件完整性
     */
    public boolean validateBackup(Long backupId) {
        try {
            Backup backup = backupRepository.findById(backupId)
                    .orElseThrow(() -> new RuntimeException("备份记录不存在"));

            Path backupFilePath = Paths.get(System.getProperty("user.dir"), BACKUP_DIR, backup.getFilePath());

            // 检查文件是否存在
            if (!Files.exists(backupFilePath)) {
                return false;
            }

            // 检查文件大小
            long actualSize = Files.size(backupFilePath);
            if (actualSize != backup.getFileSize()) {
                logger.warn("备份文件大小不匹配: 预期={}, 实际={}", backup.getFileSize(), actualSize);
                return false;
            }

            // TODO: 验证MD5校验值（可选）

            return true;

        } catch (Exception e) {
            logger.error("验证备份文件失败", e);
            return false;
        }
    }

    /**
     * 同步备份文件大小
     * ✅ 修复：恢复数据库后，重新扫描物理文件，更新 fileSize 字段
     * 解决数据库恢复导致 fileSize 被还原为旧值（0）的问题
     */
    @Transactional
    public void syncBackupFileSizes() {
        try {
            logger.info("开始同步备份文件大小...");

            // 获取所有备份记录
            java.util.List<Backup> backups = backupRepository.findAll();
            int syncedCount = 0;
            int errorCount = 0;

            Path backupDir = Paths.get(System.getProperty("user.dir"), BACKUP_DIR);

            for (Backup backup : backups) {
                try {
                    Path backupFilePath = backupDir.resolve(backup.getFilePath());

                    if (Files.exists(backupFilePath)) {
                        long actualSize = Files.size(backupFilePath);

                        // 只有当数据库记录与实际大小不一致时才更新
                        if (backup.getFileSize() == null || backup.getFileSize() != actualSize) {
                            logger.info("更新备份文件大小: {} - {} -> {} bytes",
                                       backup.getName(),
                                       backup.getFileSize(),
                                       actualSize);

                            backup.setFileSize(actualSize);
                            backupRepository.save(backup);
                            syncedCount++;
                        }
                    } else {
                        logger.warn("备份文件不存在，跳过同步: {}", backup.getFilePath());
                        errorCount++;
                    }
                } catch (Exception e) {
                    logger.error("同步备份文件大小失败: {}", backup.getName(), e);
                    errorCount++;
                }
            }

            logger.info("备份文件大小同步完成: 更新 {} 个，失败 {} 个", syncedCount, errorCount);

        } catch (Exception e) {
            logger.error("同步备份文件大小失败", e);
        }
    }
}
