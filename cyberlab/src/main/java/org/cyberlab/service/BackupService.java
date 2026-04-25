package org.cyberlab.service;

import org.cyberlab.entity.Backup;
import org.cyberlab.repository.BackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 系统备份服务
 * 负责创建、管理和删除系统备份
 */
@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

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

    // 备份文件存储目录
    private static final String BACKUP_DIR = "backups";

    /**
     * 创建系统备份（异步）
     * @param backupId 备份记录ID
     * @param type 备份类型
     * @param password 加密密码（可选，为null则不加密）
     *
     * ✅ 修复：移除@Transactional注解，避免长时间持有数据库连接
     * 数据库操作通过独立的事务方法执行（短事务），mysqldump命令在事务外执行
     */
    @Async
    public void createBackupAsync(Long backupId, String type, String password) {
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));

        try {
            // ✅ 修复：强制验证密码（安全加固）
            if (password == null || password.trim().isEmpty()) {
                throw new RuntimeException("备份密码为必需，无法创建不加密的备份");
            }

            if (password.trim().length() < 8) {
                throw new RuntimeException("备份密码长度至少为8位");
            }

            logger.info("开始创建备份: {}, 类型: {}, 加密: AES-256", backup.getName(), type);

            // 创建备份目录
            Path backupDirPath = Paths.get(System.getProperty("user.dir"), BACKUP_DIR);
            Files.createDirectories(backupDirPath);

            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("backup_%s_%s.zip", type, timestamp);
            Path backupFilePath = backupDirPath.resolve(backupFileName);

            // 创建ZIP备份文件
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFilePath.toFile()))) {

                // 根据类型备份不同内容
                if ("full".equals(type) || "database".equals(type)) {
                    // 备份数据库
                    backupDatabase(zos);
                }

                if ("full".equals(type) || "files".equals(type)) {
                    // 备份上传文件
                    backupFiles(zos);
                }

                // 备份配置文件（仅完整备份）
                if ("full".equals(type)) {
                    backupConfiguration(zos);
                }
            }

            // ✅ 修复：所有备份都必须加密，移除可选逻辑
            logger.info("开始对备份文件进行AES-256加密...");
            encryptBackupFile(backupFilePath, password);
            backup.setEncrypted(true);
            backup.setEncryptionMethod("AES256");
            logger.info("备份文件加密完成");

            // 计算文件大小和MD5
            long fileSize = Files.size(backupFilePath);
            String md5 = calculateMD5(backupFilePath);

            // ✅ 修复：使用独立的事务方法更新备份记录（短事务）
            updateBackupComplete(backupId, backupFileName, fileSize, md5,
                    backup.getEncrypted(), backup.getEncryptionMethod());

            logger.info("备份创建成功: {}, 文件大小: {} bytes, 加密: {}",
                       backupFileName, fileSize, backup.getEncrypted());

        } catch (Exception e) {
            logger.error("备份创建失败: {}", backup.getName(), e);
            // ✅ 修复：使用独立的事务方法更新状态（短事务）
            updateBackupFailed(backupId, e.getMessage());
            throw new RuntimeException("备份创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新备份完成状态（独立事务方法）
     * ✅ 修复：将数据库更新操作独立出来，使用短事务，避免长时间持有连接
     */
    @Transactional
    protected void updateBackupComplete(Long backupId, String filePath, long fileSize,
                                       String md5, Boolean encrypted, String encryptionMethod) {
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));
        backup.setFilePath(filePath);
        backup.setFileSize(fileSize);
        backup.setMd5Checksum(md5);
        backup.setEncrypted(encrypted);
        backup.setEncryptionMethod(encryptionMethod);
        backup.setStatus("completed");
        backup.setCompletedTime(LocalDateTime.now());
        backupRepository.save(backup);
    }

    /**
     * 更新备份失败状态（独立事务方法）
     * ✅ 修复：将数据库更新操作独立出来，使用短事务，避免长时间持有连接
     */
    @Transactional
    protected void updateBackupFailed(Long backupId, String errorMessage) {
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));
        backup.setStatus("failed");
        backup.setErrorMessage(errorMessage);
        backup.setCompletedTime(LocalDateTime.now());
        backupRepository.save(backup);
    }

    /**
     * 备份数据库
     */
    private void backupDatabase(ZipOutputStream zos) throws Exception {
        logger.info("开始备份数据库...");

        // 解析数据库名称
        String dbName = extractDatabaseName(datasourceUrl);

        // 生成SQL备份文件名
        String sqlFileName = String.format("database_%s.sql",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        // 创建临时SQL文件
        Path tempSqlFile = Files.createTempFile("backup_", ".sql");

        try {
            // 使用mysqldump命令导出数据库
            // ✅ 修复：使用MYSQL_PWD环境变量传递密码，避免命令行警告污染SQL文件
            ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-u", datasourceUsername,
                "--single-transaction",
                "--skip-lock-tables",
                "--routines",
                "--triggers",
                dbName
            );

            // 设置MYSQL_PWD环境变量（避免密码警告）
            pb.environment().put("MYSQL_PWD", datasourcePassword);

            // 输出到SQL文件
            pb.redirectOutput(tempSqlFile.toFile());

            // ✅ 修复：将错误输出到专用日志文件，避免污染SQL文件
            Path errorLog = Files.createTempFile("backup_error_", ".log");
            pb.redirectError(errorLog.toFile());

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // 读取错误信息（从错误日志文件，不是SQL文件）
                String error = new String(Files.readAllBytes(errorLog));
                Files.deleteIfExists(errorLog);
                throw new RuntimeException("mysqldump执行失败: " + error);
            }

            // 删除错误日志文件
            Files.deleteIfExists(errorLog);

            // 将SQL文件添加到ZIP
            zos.putNextEntry(new ZipEntry(sqlFileName));
            Files.copy(tempSqlFile, zos);
            zos.closeEntry();

            logger.info("数据库备份完成: {}", sqlFileName);

        } finally {
            // 删除临时文件
            Files.deleteIfExists(tempSqlFile);
        }
    }

    /**
     * 备份上传文件
     */
    private void backupFiles(ZipOutputStream zos) throws Exception {
        logger.info("开始备份上传文件...");

        Path uploadsDir = Paths.get(uploadsPath);
        if (!Files.exists(uploadsDir)) {
            logger.warn("上传目录不存在: {}", uploadsPath);
            return;
        }

        // 递归添加文件到ZIP
        Files.walk(uploadsDir)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    Path relativePath = uploadsDir.relativize(file);
                    String zipEntryName = "uploads/" + relativePath.toString().replace("\\", "/");

                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(file, zos);
                    zos.closeEntry();

                } catch (IOException e) {
                    logger.error("备份文件失败: {}", file, e);
                }
            });

        logger.info("文件备份完成");
    }

    /**
     * 备份配置文件（完善版）
     * 备份所有关键配置文件，包括SSL证书
     */
    private void backupConfiguration(ZipOutputStream zos) throws Exception {
        logger.info("开始备份配置文件...");

        // 需要备份的配置文件列表
        String[] configFiles = {
            "application.yml",              // 主配置文件
            "application.properties",       // 备用配置文件
            "application-container.properties", // 容器配置
            "keystore.p12"                  // SSL证书（关键！）
        };

        int backedUpCount = 0;

        for (String configFile : configFiles) {
            try {
                // 使用ClassPathResource读取jar内或资源目录的文件
                ClassPathResource resource = new ClassPathResource(configFile);

                if (resource.exists()) {
                    zos.putNextEntry(new ZipEntry("config/" + configFile));

                    // 从资源流读取并写入ZIP
                    try (InputStream is = resource.getInputStream()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                    }

                    zos.closeEntry();
                    backedUpCount++;
                    logger.info("配置文件已备份: {}", configFile);
                } else {
                    logger.warn("配置文件不存在，跳过: {}", configFile);
                }
            } catch (Exception e) {
                logger.warn("备份配置文件失败，跳过: {} ({})", configFile, e.getMessage());
            }
        }

        logger.info("配置文件备份完成，成功备份 {} 个文件", backedUpCount);
    }

    /**
     * 对备份文件进行AES-256加密
     * @param backupFilePath 原始未加密的备份文件路径
     * @param password 加密密码
     */
    private void encryptBackupFile(Path backupFilePath, String password) throws Exception {
        // 创建临时加密文件路径
        Path tempEncryptedFile = Paths.get(backupFilePath.toString() + ".encrypted.tmp");

        try {
            // 1. 使用zip4j创建一个新的加密ZIP文件
            try (ZipFile encryptedZip = new ZipFile(tempEncryptedFile.toFile(), password.toCharArray())) {
                // 配置AES-256加密参数
                ZipParameters zipParameters = new ZipParameters();
                zipParameters.setEncryptFiles(true);
                zipParameters.setEncryptionMethod(EncryptionMethod.AES);
                zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

                // 2. 解压原ZIP到临时目录
                Path tempExtractDir = Files.createTempDirectory("backup_encrypt_");

                try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                        Files.newInputStream(backupFilePath))) {

                    java.util.zip.ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        Path entryPath = tempExtractDir.resolve(entry.getName());

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

                // 3. 将临时目录的内容逐个添加到加密ZIP（保持目录结构）
                // ✅ 修复：不使用addFolder()，而是逐文件添加，避免多层目录嵌套
                try (var fileStream = Files.walk(tempExtractDir)) {
                    fileStream.forEach(filePath -> {
                        try {
                            if (Files.isRegularFile(filePath)) {
                                // 计算相对路径（相对于tempExtractDir）
                                Path relativePath = tempExtractDir.relativize(filePath);
                                String zipEntryName = relativePath.toString().replace("\\", "/");

                                // 为每个文件创建独立的ZipParameters
                                ZipParameters fileZipParams = new ZipParameters();
                                fileZipParams.setEncryptFiles(true);
                                fileZipParams.setEncryptionMethod(EncryptionMethod.AES);
                                fileZipParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

                                // 设置ZIP内的文件路径（保持原始目录结构）
                                fileZipParams.setFileNameInZip(zipEntryName);

                                // 添加文件到加密ZIP
                                encryptedZip.addFile(filePath.toFile(), fileZipParams);
                                logger.debug("添加文件到加密ZIP: {}", zipEntryName);
                            }
                        } catch (Exception e) {
                            logger.error("添加文件到加密ZIP失败: {}", filePath, e);
                            throw new RuntimeException("加密文件添加失败", e);
                        }
                    });
                }

                // 4. 清理临时目录
                deleteDirectory(tempExtractDir.toFile());
            }

            // 5. 删除原始未加密文件
            Files.delete(backupFilePath);

            // 6. 重命名加密文件为原文件名
            Files.move(tempEncryptedFile, backupFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            // 清理临时文件
            Files.deleteIfExists(tempEncryptedFile);
            throw new Exception("备份文件加密失败: " + e.getMessage(), e);
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
     * 计算文件MD5校验值
     */
    private String calculateMD5(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        }

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 从JDBC URL中提取数据库名称
     */
    private String extractDatabaseName(String jdbcUrl) {
        // jdbc:mysql://localhost:3306/cyberlab?params...
        int lastSlash = jdbcUrl.lastIndexOf('/');
        int questionMark = jdbcUrl.indexOf('?', lastSlash);

        if (questionMark > 0) {
            return jdbcUrl.substring(lastSlash + 1, questionMark);
        } else {
            return jdbcUrl.substring(lastSlash + 1);
        }
    }

    /**
     * 创建备份记录
     */
    @Transactional
    public Backup createBackupRecord(String name, String type, String description, String createdBy) {
        Backup backup = new Backup();
        backup.setName(name);
        backup.setType(type);
        backup.setDescription(description);
        backup.setCreatedBy(createdBy);
        backup.setStatus("pending");
        backup.setCreatedTime(LocalDateTime.now());
        backup.setDatabaseName(extractDatabaseName(datasourceUrl));
        // 设置临时值，避免NOT NULL约束错误，后续异步任务会更新为实际值
        backup.setFilePath("pending");
        backup.setFileSize(0L);

        return backupRepository.save(backup);
    }

    /**
     * 获取所有备份列表
     */
    public List<Backup> getAllBackups() {
        return backupRepository.findAllByOrderByCreatedTimeDesc();
    }

    /**
     * 获取备份详情
     */
    public Backup getBackupById(Long id) {
        return backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("备份不存在: " + id));
    }

    /**
     * 删除备份
     */
    @Transactional
    public void deleteBackup(Long id) {
        Backup backup = getBackupById(id);

        // 删除备份文件
        try {
            Path backupFile = Paths.get(System.getProperty("user.dir"), BACKUP_DIR, backup.getFilePath());
            Files.deleteIfExists(backupFile);
            logger.info("备份文件已删除: {}", backupFile);
        } catch (IOException e) {
            logger.error("删除备份文件失败: {}", backup.getFilePath(), e);
        }

        // 删除数据库记录
        backupRepository.deleteById(id);
        logger.info("备份记录已删除: {}", id);
    }

    /**
     * 获取备份文件路径
     */
    public Path getBackupFilePath(Long id) {
        Backup backup = getBackupById(id);
        return Paths.get(System.getProperty("user.dir"), BACKUP_DIR, backup.getFilePath());
    }

    /**
     * 获取备份统计信息
     */
    public BackupStatistics getStatistics() {
        BackupStatistics stats = new BackupStatistics();
        stats.setTotalBackups(backupRepository.count());
        stats.setCompletedBackups(backupRepository.countCompletedBackups());
        stats.setTotalSize(backupRepository.getTotalBackupSize());

        return stats;
    }

    /**
     * 扫描物理备份文件并同步到数据库
     * ✅ 修复：用于恢复数据库后，重新导入物理文件的备份记录
     * @return 导入的备份数量
     */
    @Transactional
    public int scanPhysicalBackups() {
        try {
            logger.info("开始扫描物理备份文件...");

            Path backupDir = Paths.get(System.getProperty("user.dir"), BACKUP_DIR);

            if (!Files.exists(backupDir)) {
                logger.warn("备份目录不存在: {}", backupDir);
                return 0;
            }

            // 获取数据库中已有的文件路径
            java.util.Set<String> existingFilePaths = backupRepository.findAll().stream()
                .map(Backup::getFilePath)
                .filter(path -> path != null && !path.isEmpty() && !path.equals("pending"))
                .collect(java.util.stream.Collectors.toSet());

            int importedCount = 0;

            // 扫描目录中的所有 .zip 文件
            try (var stream = Files.list(backupDir)) {
                for (Path filePath : (Iterable<Path>) stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".zip"))::iterator) {

                    String fileName = filePath.getFileName().toString();

                    // 跳过数据库中已存在的文件
                    if (existingFilePaths.contains(fileName)) {
                        logger.debug("备份文件已存在于数据库: {}", fileName);
                        continue;
                    }

                    try {
                        // 读取文件信息
                        long fileSize = Files.size(filePath);
                        java.nio.file.attribute.BasicFileAttributes attrs =
                            Files.readAttributes(filePath, java.nio.file.attribute.BasicFileAttributes.class);

                        // 解析文件名获取备份信息
                        // 格式: backup_<type>_<yyyyMMdd_HHmmss>.zip
                        String nameWithoutExt = fileName.replace(".zip", "");
                        String[] parts = nameWithoutExt.split("_");

                        String type = "full"; // 默认类型
                        String backupName = fileName;

                        if (parts.length >= 2) {
                            type = parts[1]; // full/database/files
                        }
                        if (parts.length >= 4) {
                            // backup_full_20251029_120111
                            backupName = "系统备份_" + parts[2] + "_" + parts[3];
                        }

                        // 创建备份记录
                        Backup backup = new Backup();
                        backup.setName(backupName);
                        backup.setType(type);
                        backup.setDescription("从物理文件扫描导入");
                        backup.setCreatedBy("system");
                        backup.setStatus("completed");
                        backup.setFilePath(fileName);
                        backup.setFileSize(fileSize);
                        backup.setDatabaseName(extractDatabaseName(datasourceUrl));
                        backup.setEncrypted(true); // 假设都是加密的
                        backup.setEncryptionMethod("AES-256");

                        // 使用文件创建时间
                        java.time.Instant instant = attrs.creationTime().toInstant();
                        backup.setCreatedTime(java.time.LocalDateTime.ofInstant(instant,
                            java.time.ZoneId.systemDefault()));
                        backup.setCompletedTime(backup.getCreatedTime());

                        backupRepository.save(backup);
                        importedCount++;

                        logger.info("导入备份文件: {} - {} bytes", fileName, fileSize);

                    } catch (Exception e) {
                        logger.error("导入备份文件失败: {}", fileName, e);
                    }
                }
            }

            logger.info("物理备份文件扫描完成: 新导入 {} 个文件", importedCount);
            return importedCount;

        } catch (Exception e) {
            logger.error("扫描物理备份文件失败", e);
            throw new RuntimeException("扫描备份文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 备份统计信息类
     */
    public static class BackupStatistics {
        private long totalBackups;
        private long completedBackups;
        private Long totalSize;

        public long getTotalBackups() {
            return totalBackups;
        }

        public void setTotalBackups(long totalBackups) {
            this.totalBackups = totalBackups;
        }

        public long getCompletedBackups() {
            return completedBackups;
        }

        public void setCompletedBackups(long completedBackups) {
            this.completedBackups = completedBackups;
        }

        public Long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(Long totalSize) {
            this.totalSize = totalSize;
        }
    }
}
