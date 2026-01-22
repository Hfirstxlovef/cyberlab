package org.cyberlab.config;

import org.cyberlab.service.HostNodeService;
import org.cyberlab.service.ContainerStateInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时的初始化任务
 */
@Component
public class StartupInitializer implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);
    
    @Autowired
    private HostNodeService hostNodeService;
    
    @Autowired
    private ContainerStateInitializationService containerStateInitService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 延迟一点确保所有Bean都已完全初始化
        Thread.sleep(2000);

        try {
            // 初始化容器状态数据（仅数据库操作，无网络探测）
            containerStateInitService.initializeAllContainerStates();

            logger.info("启动初始化完成（节点探测已移至资产管理页面按需触发）");

        } catch (Exception e) {
            logger.error("Startup initialization tasks failed: " + e.getMessage(), e);
        }
    }
}