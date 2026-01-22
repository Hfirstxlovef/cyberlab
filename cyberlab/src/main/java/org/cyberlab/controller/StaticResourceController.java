package org.cyberlab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class StaticResourceController {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourceController.class);

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 处理所有图片文件的访问请求，包括logo文件
     */
    @GetMapping(value = {"/logo_*.png", "/logo_*.jpg", "/logo_*.jpeg", "/logo_*.gif", 
                        "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.ico", "/*.svg"})
    public ResponseEntity<Resource> getImageFile(HttpServletRequest request) {
        try {
            String requestUrl = request.getRequestURI();
            String filename = requestUrl.substring(1); // 去掉开头的 "/"
            
            logger.debug("处理图片文件请求: {} -> {}", requestUrl, filename);
            
            // 安全检查：只允许访问图片文件
            if (!isImageFile(filename)) {
                logger.warn("非图片文件请求被拒绝: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
            // 安全检查：防止路径遍历攻击
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                logger.warn("危险路径请求被拒绝: {}", filename);
                return ResponseEntity.badRequest().build();
            }
            
            Path filePath = Paths.get(uploadPath, filename);
            File file = filePath.toFile();
            
            logger.debug("尝试访问文件: {}", filePath.toAbsolutePath());
            
            if (!file.exists() || !file.isFile()) {
                logger.warn("文件不存在: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // 检查文件大小，防止恶意访问
            if (file.length() > 10 * 1024 * 1024) { // 10MB限制
                logger.warn("文件过大: {} bytes", file.length());
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new FileSystemResource(file);
            String ext = getFileExtension(filename);
            String contentType = determineContentType(ext);
            
            logger.debug("成功访问文件: {}, 大小: {} bytes", filename, file.length());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // 1小时缓存
                    .contentLength(file.length())
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("访问文件失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filename) {
        if (filename == null) return false;
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".png") || lowerFilename.endsWith(".jpg") || 
               lowerFilename.endsWith(".jpeg") || lowerFilename.endsWith(".gif") || 
               lowerFilename.endsWith(".bmp") || lowerFilename.endsWith(".webp") ||
               lowerFilename.endsWith(".svg") || lowerFilename.endsWith(".ico");
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * 根据文件扩展名确定Content-Type
     */
    private String determineContentType(String ext) {
        if (ext == null) return "application/octet-stream";
        
        switch (ext.toLowerCase()) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            default:
                return "application/octet-stream";
        }
    }
}