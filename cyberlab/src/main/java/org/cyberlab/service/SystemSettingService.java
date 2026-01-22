package org.cyberlab.service;

import org.cyberlab.entity.SystemSetting;
import org.cyberlab.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository settingRepository;

    // 获取所有设置
    public Map<String, String> getAllSettings() {
        List<SystemSetting> settings = settingRepository.findAll();
        Map<String, String> settingsMap = new HashMap<>();
        
        for (SystemSetting setting : settings) {
            settingsMap.put(setting.getSettingKey(), setting.getSettingValue());
        }
        
        // 设置默认值
        setDefaultValues(settingsMap);
        return settingsMap;
    }

    // 获取公共设置（所有用户可访问）
    public Map<String, String> getPublicSettings() {
        List<SystemSetting> settings = settingRepository.findAll();
        Map<String, String> settingsMap = new HashMap<>();
        
        // 只包含公共可访问的设置
        String[] publicKeys = {"system_logo", "website_title", "login_page_title", "sidebar_title", "login_title"};
        
        for (SystemSetting setting : settings) {
            String key = setting.getSettingKey();
            if (isPublicSetting(key)) {
                settingsMap.put(key, setting.getSettingValue());
            }
        }
        
        // 设置公共默认值
        for (String key : publicKeys) {
            settingsMap.putIfAbsent(key, getDefaultValue(key));
        }
        
        return settingsMap;
    }

    // 判断是否为公共设置
    private boolean isPublicSetting(String key) {
        String[] publicKeys = {"system_logo", "website_title", "login_page_title", "sidebar_title", "login_title"};
        for (String publicKey : publicKeys) {
            if (publicKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    // 获取单个设置
    public String getSetting(String key) {
        Optional<SystemSetting> setting = settingRepository.findBySettingKey(key);
        if (setting.isPresent() && setting.get().getSettingValue() != null) {
            return setting.get().getSettingValue();
        }
        return getDefaultValue(key);
    }

    // 保存设置
    @Transactional
    public void saveSetting(String key, String value, String type, String description) {
        Optional<SystemSetting> existingSetting = settingRepository.findBySettingKey(key);
        
        if (existingSetting.isPresent()) {
            SystemSetting setting = existingSetting.get();
            setting.setSettingValue(value);
            setting.setUpdatedAt(LocalDateTime.now());
            settingRepository.save(setting);
        } else {
            SystemSetting newSetting = new SystemSetting(key, value, type, description);
            settingRepository.save(newSetting);
        }
    }

    // 批量保存设置
    @Transactional
    public void saveSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String type = getSettingType(key);
            String description = getSettingDescription(key);
            saveSetting(key, value, type, description);
        }
    }

    // 设置默认值（遵循八耻八荣原则：使用正确的格式）
    private void setDefaultValues(Map<String, String> settingsMap) {
        settingsMap.putIfAbsent("system_logo", "/images/default-logo.png");
        settingsMap.putIfAbsent("website_title", "CyberLab网络空间安全攻防实验室");
        settingsMap.putIfAbsent("login_page_title", "红岸网络空间安全对抗平台");
        settingsMap.putIfAbsent("sidebar_title", "CyberLab平台");
        // 八耻八荣：使用完整的序列号格式 CYBERLAB-YYYY-MM-DD-NNNNN
        settingsMap.putIfAbsent("serial_number", "CYBERLAB-2025-12-31-00001");
        settingsMap.putIfAbsent("license_code", "CL-DEMO-TRIAL-INACTIVE-00000000");
        settingsMap.putIfAbsent("license_owner", "未设置");

        // 向后兼容性处理
        settingsMap.putIfAbsent("login_title", "欢迎使用CyberLab网络安全攻防演练平台");
    }

    private String getDefaultValue(String key) {
        switch (key) {
            case "system_logo": return "/images/default-logo.png";
            case "website_title": return "CyberLab网络空间安全攻防实验室";
            case "login_page_title": return "红岸网络空间安全对抗平台";
            case "sidebar_title": return "CyberLab平台";
            // 八耻八荣：使用完整的序列号格式 CYBERLAB-YYYY-MM-DD-NNNNN
            case "serial_number": return "CYBERLAB-2025-12-31-00001";
            case "license_code": return "CL-DEMO-TRIAL-INACTIVE-00000000";
            case "license_owner": return "未设置";
            // 向后兼容性
            case "login_title": return "欢迎使用CyberLab网络安全攻防演练平台";
            default: return "";
        }
    }

    private String getSettingType(String key) {
        switch (key) {
            case "system_logo": return "image";
            case "serial_number": case "license_code": return "code";
            default: return "text";
        }
    }

    private String getSettingDescription(String key) {
        switch (key) {
            case "system_logo": return "系统Logo图片";
            case "website_title": return "网站标题（浏览器标题栏显示）";
            case "login_page_title": return "登录页面显示的标题";
            case "sidebar_title": return "侧边栏标题";
            case "serial_number": return "产品序列号";
            case "license_code": return "产品授权码";
            // 向后兼容性
            case "login_title": return "网站标题（已废弃，请使用website_title）";
            default: return "";
        }
    }
}