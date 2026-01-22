package org.cyberlab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    // CORS配置已移至SecurityConfig中，避免配置冲突
    // @Override
    // public void addCorsMappings(@NonNull CorsRegistry registry) {
    //     // 注释掉：CORS配置由SecurityConfig.corsConfigurationSource()统一管理
    // }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 配置上传文件的访问路径 - 支持相对路径和绝对路径
        if (uploadPath.startsWith("/")) {
            // 绝对路径
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + uploadPath + "/")
                    .setCachePeriod(3600)
                    .resourceChain(false); // 改为false，避免resourceChain处理问题
        } else {
            // 相对路径 - 相对于项目根目录
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:./" + uploadPath + "/")
                    .setCachePeriod(3600)
                    .resourceChain(false); // 改为false，避免resourceChain处理问题
        }
        
        
        // 特别添加对logo等图片文件的直接支持 - 映射根路径下的图片到uploads目录
        registry.addResourceHandler("/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.ico", "/*.svg")
                .addResourceLocations("file:./" + uploadPath + "/")
                .setCachePeriod(3600)
                .resourceChain(false);
        
        // 为了兼容旧版本的直接访问方式，添加logo文件的特殊映射
        registry.addResourceHandler("/logo_*.png", "/logo_*.jpg", "/logo_*.jpeg", "/logo_*.gif")
                .addResourceLocations("file:./" + uploadPath + "/")
                .setCachePeriod(3600)
                .resourceChain(false);
    }
}