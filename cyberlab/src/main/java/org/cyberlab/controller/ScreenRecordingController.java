package org.cyberlab.controller;

import org.cyberlab.entity.ScreenRecording;
import org.cyberlab.service.ScreenRecordingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/screen-recording")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class ScreenRecordingController {

    private static final Logger logger = LoggerFactory.getLogger(ScreenRecordingController.class);

    @Autowired
    private ScreenRecordingService screenRecordingService;

    /**
     * 开始录屏会话
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startRecording(
            @RequestParam Long userId,
            @RequestParam String username) {

        try {
            String sessionId = UUID.randomUUID().toString();
            ScreenRecording recording = screenRecordingService.startRecording(userId, username, sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", recording.getSessionId());
            response.put("startTime", recording.getStartTime());
            response.put("message", "录屏会话已开始");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("开始录屏失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "开始录屏失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 上传录屏分片
     */
    @PostMapping("/upload-chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam String sessionId,
            @RequestParam int chunkIndex,
            @RequestParam MultipartFile file,
            @RequestParam long timestamp) {

        try {
            screenRecordingService.uploadChunk(sessionId, chunkIndex, file, timestamp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chunkIndex", chunkIndex);
            response.put("message", "分片上传成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("上传分片失败: session={}, chunk={}, error={}",
                sessionId, chunkIndex, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "上传失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 停止录屏
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopRecording(
            @RequestParam String sessionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            ScreenRecording recording = screenRecordingService.stopRecording(sessionId, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recordingId", recording.getId());
            response.put("filePath", recording.getFilePath());
            response.put("fileSize", recording.getFileSize());
            response.put("duration", recording.getDurationSeconds());
            response.put("message", "录屏已完成");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("停止录屏失败: session={}, error={}", sessionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "停止录屏失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 检查用户录屏状态（准入验证）
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> checkRecordingStatus(@PathVariable Long userId) {
        try {
            if (userId == null || userId <= 0) {
                logger.warn("无效的 userId: {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "isRecording", false,
                    "message", "无效的用户ID"
                ));
            }

            boolean isRecording = screenRecordingService.isUserRecording(userId);
            Optional<ScreenRecording> activeRecording = screenRecordingService.getUserActiveRecording(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("isRecording", isRecording);

            if (activeRecording.isPresent()) {
                ScreenRecording recording = activeRecording.get();
                response.put("sessionId", recording.getSessionId());
                response.put("startTime", recording.getStartTime());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检查录屏状态失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "isRecording", false,
                "message", "检查录屏状态失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 按时间范围搜索录屏（管理员）
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('admin', 'judge')")
    public ResponseEntity<List<ScreenRecording>> searchRecordings(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            List<ScreenRecording> recordings;

            if (username != null && startTime != null && endTime != null) {
                recordings = screenRecordingService.findByTimeRange(username, startTime, endTime);
            } else if (username != null) {
                recordings = screenRecordingService.getUserCompletedRecordings(username);
            } else {
                recordings = screenRecordingService.getAllCompletedRecordings();
            }

            return ResponseEntity.ok(recordings);
        } catch (Exception e) {
            logger.error("搜索录屏失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 视频流式播放（支持Range请求）
     */
    @GetMapping("/{id}/stream")
    @PreAuthorize("hasAnyRole('admin', 'judge', 'red', 'blue')")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        try {
            Optional<ScreenRecording> recordingOpt = screenRecordingService.getRecordingById(id);

            if (recordingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScreenRecording recording = recordingOpt.get();
            File videoFile = new File(recording.getFilePath());

            if (!videoFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(videoFile);
            long fileLength = videoFile.length();

            // 处理Range请求（支持断点续播）
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty() ?
                    Long.parseLong(ranges[1]) : fileLength - 1;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("video/webm"));
                headers.setContentLength(end - start + 1);
                headers.set("Content-Range", String.format("bytes %d-%d/%d", start, end, fileLength));
                headers.set("Accept-Ranges", "bytes");

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(resource);
            } else {
                // 完整响应
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("video/webm"));
                headers.setContentLength(fileLength);
                headers.set("Accept-Ranges", "bytes");

                return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
            }
        } catch (Exception e) {
            logger.error("视频流式播放失败: id={}, error={}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 下载录屏
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('admin', 'judge')")
    public ResponseEntity<Resource> downloadRecording(@PathVariable Long id) {
        try {
            Optional<ScreenRecording> recordingOpt = screenRecordingService.getRecordingById(id);

            if (recordingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScreenRecording recording = recordingOpt.get();
            File videoFile = new File(recording.getFilePath());

            if (!videoFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(videoFile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(recording.getFileName())
                    .build()
            );

            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
        } catch (Exception e) {
            logger.error("下载录屏失败: id={}, error={}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取录屏详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'judge')")
    public ResponseEntity<ScreenRecording> getRecordingDetail(@PathVariable Long id) {
        Optional<ScreenRecording> recording = screenRecordingService.getRecordingById(id);
        return recording.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 清理旧录屏
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Object>> cleanupOldRecordings(
            @RequestParam(defaultValue = "30") int daysToKeep) {

        try {
            int deletedCount = screenRecordingService.cleanOldRecordings(daysToKeep);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deletedCount,
                "message", String.format("已清理 %d 条超过 %d 天的录屏记录", deletedCount, daysToKeep)
            ));
        } catch (Exception e) {
            logger.error("清理录屏失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "清理失败: " + e.getMessage()
            ));
        }
    }
}
