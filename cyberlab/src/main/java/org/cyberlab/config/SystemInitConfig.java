package org.cyberlab.config;

import org.cyberlab.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统初始化配置
 * 在应用启动时确保必要的系统设置存在
 */
@Configuration
public class SystemInitConfig {

    private static final Logger logger = LoggerFactory.getLogger(SystemInitConfig.class);

    @Autowired
    private SystemSettingService settingService;

    @Bean
    public ApplicationRunner initializeSystemSettings() {
        return args -> {
            try {
                // Debug statement removed
                
                // 确保系统Logo设置存在
                if (settingService.getSetting("system_logo") == null || 
                    settingService.getSetting("system_logo").isEmpty()) {
                    settingService.saveSetting("system_logo", "/images/default-logo.png", "image", "系统Logo图片");
                    // Debug statement removed
                }
                
                // 确保登录标题设置存在
                if (settingService.getSetting("login_title") == null || 
                    settingService.getSetting("login_title").isEmpty()) {
                    settingService.saveSetting("login_title", "欢迎使用CyberLab网络空间安全攻防演练平台", "text", "登录页面标题");
                    // Debug statement removed
                }
                
                // 确保侧边栏标题设置存在
                if (settingService.getSetting("sidebar_title") == null || 
                    settingService.getSetting("sidebar_title").isEmpty()) {
                    settingService.saveSetting("sidebar_title", "CyberLab平台", "text", "侧边栏标题");
                    // Debug statement removed
                }
                
                // 确保序列号设置存在
                if (settingService.getSetting("serial_number") == null || 
                    settingService.getSetting("serial_number").isEmpty()) {
                    settingService.saveSetting("serial_number", "CYBERLAB-2024-001", "code", "产品序列号");
                    // Debug statement removed
                }
                
                // 确保授权码设置存在
                if (settingService.getSetting("license_code") == null || 
                    settingService.getSetting("license_code").isEmpty()) {
                    settingService.saveSetting("license_code", "未设置授权码", "code", "产品授权码");
                    // Debug statement removed
                }
                
                // Debug statement removed

            } catch (Exception e) {
                logger.error("系统设置初始化失败", e);
            }
        };
    }
}