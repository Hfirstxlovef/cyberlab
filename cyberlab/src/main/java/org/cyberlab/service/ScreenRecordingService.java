package org.cyberlab.service;

import org.cyberlab.entity.ScreenRecording;
import org.cyberlab.repository.ScreenRecordingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ScreenRecordingService {

    private static final Logger logger = LoggerFactory.getLogger(ScreenRecordingService.class);

    @Autowired
    private ScreenRecordingRepository screenRecordingRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadBasePath;

    private static final String RECORDING_DIR = "screen-recordings";
    private static final String CHUNKS_DIR = "chunks";

    /**
     * 开始录屏会话
     */
    @Transactional
    public ScreenRecording startRecording(Long userId, String username, String sessionId) {
        // 检查用户是否已有正在录制的会话
        Optional<ScreenRecording> existingRecording =
            screenRecordingRepository.findByUserIdAndStatus(userId, "recording");

        if (existingRecording.isPresent()) {
            logger.warn("用户 {} 已有正在录制的会话: {}", username, existingRecording.get().getSessionId());
            return existingRecording.get();
        }

        ScreenRecording recording = new ScreenRecording(userId, username, sessionId);
        recording.setStartTime(LocalDateTime.now());
        recording.setStatus("recording");

        ScreenRecording saved = screenRecordingRepository.save(recording);
        logger.info("录屏会话已开始: sessionId={}, user={}", sessionId, username);

        return saved;
    }

    /**
     * 验证用户是否正在录屏（准入检查）
     */
    public boolean isUserRecording(Long userId) {
        return screenRecordingRepository.existsByUserIdAndStatus(userId, "recording");
    }

    /**
     * 获取用户正在录制的会话
     */
    public Optional<ScreenRecording> getUserActiveRecording(Long userId) {
        return screenRecordingRepository.findByUserIdAndStatus(userId, "recording");
    }

    /**
     * 上传录屏分片
     */
    @Transactional
    public void uploadChunk(String sessionId, int chunkIndex, MultipartFile file, long timestamp) throws IOException {
        Optional<ScreenRecording> recordingOpt = screenRecordingRepository.findBySessionId(sessionId);

        if (recordingOpt.isEmpty()) {
            throw new IllegalArgumentException("录屏会话不存在: " + sessionId);
        }

        ScreenRecording recording = recordingOpt.get();

        // 创建分片存储目录 - 使用Files.createDirectories确保创建成功
        String chunkDir = getChunkDirectory(sessionId);
        Path chunkDirPath = Paths.get(chunkDir);

        try {
            // 使用Files.createDirectories()会自动创建所有父目录，更可靠
            Files.createDirectories(chunkDirPath);
            logger.debug("分片目录已确保存在: {}", chunkDir);
        } catch (IOException e) {
            logger.error("创建分片目录失败: {}, error: {}", chunkDir, e.getMessage());
            throw new IOException("无法创建分片存储目录: " + chunkDir, e);
        }

        // 保存分片文件
        String chunkFileName = String.format("chunk_%05d.webm", chunkIndex);
        File chunkFile = new File(chunkDir, chunkFileName);

        try {
            file.transferTo(chunkFile);
            logger.debug("分片文件已保存: {}", chunkFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("保存分片文件失败: {}, error: {}", chunkFile.getAbsolutePath(), e.getMessage());
            throw new IOException("无法保存分片文件: " + chunkFileName, e);
        }

        // 更新录屏记录
        recording.incrementChunkCount();
        recording.setStatus("uploading");
        screenRecordingRepository.save(recording);

        logger.info("📤 分片上传成功: session={}, chunk={}/{}, size={}KB",
            sessionId, chunkIndex + 1, recording.getTotalChunks() != null ? recording.getTotalChunks() : "?",
            file.getSize() / 1024);
    }

    /**
     * 停止录屏并合并分片
     */
    @Transactional
    public ScreenRecording stopRecording(String sessionId, LocalDateTime endTime) throws IOException {
        Optional<ScreenRecording> recordingOpt = screenRecordingRepository.findBySessionId(sessionId);

        if (recordingOpt.isEmpty()) {
            throw new IllegalArgumentException("录屏会话不存在: " + sessionId);
        }

        ScreenRecording recording = recordingOpt.get();

        // 验证 endTime 的有效性
        LocalDateTime validEndTime = endTime;
        if (recording.getStartTime() != null && endTime != null) {
            if (endTime.isBefore(recording.getStartTime()) || endTime.isEqual(recording.getStartTime())) {
                logger.warn("endTime ({}) 早于或等于 startTime ({}), 使用当前时间",
                    endTime, recording.getStartTime());
                validEndTime = LocalDateTime.now();
            }
        }

        recording.setEndTime(validEndTime);

        // 计算录制时长（确保非负）
        if (recording.getStartTime() != null && validEndTime != null) {
            long seconds = java.time.Duration.between(recording.getStartTime(), validEndTime).getSeconds();
            // 双重保险：即使计算出负数也设置为0
            recording.setDurationSeconds((int) Math.max(0, seconds));
        }

        // 合并分片文件
        String mergedFilePath = mergeChunks(recording);
        recording.setFilePath(mergedFilePath);
        recording.setFileName(new File(mergedFilePath).getName());

        // 计算文件大小
        File mergedFile = new File(mergedFilePath);
        recording.setFileSize(mergedFile.length());

        recording.setStatus("completed");
        ScreenRecording saved = screenRecordingRepository.save(recording);

        logger.info("录屏已完成: session={}, duration={}s, size={}MB",
            sessionId, recording.getDurationSeconds(), recording.getFileSize() / (1024 * 1024));

        return saved;
    }

    /**
     * 合并分片文件
     */
    private String mergeChunks(ScreenRecording recording) throws IOException {
        String chunkDir = getChunkDirectory(recording.getSessionId());
        File chunkDirectory = new File(chunkDir);

        if (!chunkDirectory.exists() || !chunkDirectory.isDirectory()) {
            throw new IOException("分片目录不存在: " + chunkDir);
        }

        // 创建最终文件路径
        String finalDir = getFinalDirectory(recording.getUsername());
        Path finalDirPath = Paths.get(finalDir);

        try {
            Files.createDirectories(finalDirPath);
            logger.debug("最终文件目录已确保存在: {}", finalDir);
        } catch (IOException e) {
            logger.error("创建最终文件目录失败: {}, error: {}", finalDir, e.getMessage());
            throw new IOException("无法创建最终文件目录: " + finalDir, e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = recording.getStartTime().format(formatter);
        String finalFileName = String.format("%s_%s.webm", recording.getUsername(), timestamp);
        String finalFilePath = finalDir + File.separator + finalFileName;

        // 按顺序读取分片并合并
        File finalFile = new File(finalFilePath);
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            File[] chunks = chunkDirectory.listFiles((dir, name) -> name.startsWith("chunk_"));
            if (chunks != null) {
                java.util.Arrays.sort(chunks, (a, b) -> a.getName().compareTo(b.getName()));

                for (File chunk : chunks) {
                    Files.copy(chunk.toPath(), fos);
                }
            }
        }

        // 删除分片文件
        deleteDirectory(chunkDirectory);

        return finalFilePath;
    }

    /**
     * 根据时间范围查找录屏
     */
    public List<ScreenRecording> findByTimeRange(String username, LocalDateTime startTime, LocalDateTime endTime) {
        return screenRecordingRepository.findByUsernameAndTimeRange(username, startTime, endTime);
    }

    /**
     * 获取用户所有已完成的录屏
     */
    public List<ScreenRecording> getUserCompletedRecordings(String username) {
        return screenRecordingRepository.findByUsernameAndStatusOrderByStartTimeDesc(username, "completed");
    }

    /**
     * 获取所有已完成的录屏
     */
    public List<ScreenRecording> getAllCompletedRecordings() {
        return screenRecordingRepository.findByStatusOrderByStartTimeDesc("completed");
    }

    /**
     * 根据ID获取录屏
     */
    public Optional<ScreenRecording> getRecordingById(Long id) {
        return screenRecordingRepository.findById(id);
    }

    /**
     * 清理旧录屏（超过指定天数）
     */
    @Transactional
    public int cleanOldRecordings(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<ScreenRecording> oldRecordings = screenRecordingRepository.findOldRecordings(cutoffDate);

        int deletedCount = 0;
        for (ScreenRecording recording : oldRecordings) {
            try {
                // 删除文件
                if (recording.getFilePath() != null) {
                    File file = new File(recording.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                // 删除数据库记录
                screenRecordingRepository.delete(recording);
                deletedCount++;
            } catch (Exception e) {
                logger.error("删除录屏失败: id={}, error={}", recording.getId(), e.getMessage());
            }
        }

        logger.info("🗑️  清理了 {} 条超过 {} 天的录屏记录", deletedCount, daysToKeep);
        return deletedCount;
    }

    // ==================== 辅助方法 ====================

    private String getChunkDirectory(String sessionId) {
        return uploadBasePath + File.separator + RECORDING_DIR + File.separator + CHUNKS_DIR + File.separator + sessionId;
    }

    private String getFinalDirectory(String username) {
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return uploadBasePath + File.separator + RECORDING_DIR + File.separator + yearMonth;
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
