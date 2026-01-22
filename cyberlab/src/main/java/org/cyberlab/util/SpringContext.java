package org.cyberlab.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        SpringContext.context = applicationContext;
    }

    /**
     * 根据类型获取Bean
     * @param clazz Bean的类型
     * @param <T> 泛型类型
     * @return Bean实例
     * @throws IllegalStateException 如果ApplicationContext未初始化
     */
    public static <T> T getBean(Class<T> clazz) {
        checkContextInitialized();
        return context.getBean(clazz);
    }

    /**
     * 根据名称获取Bean
     * @param name Bean的名称
     * @return Bean实例
     * @throws IllegalStateException 如果ApplicationContext未初始化
     */
    public static Object getBean(String name) {
        checkContextInitialized();
        return context.getBean(name);
    }

    /**
     * 根据名称和类型获取Bean
     * @param name Bean的名称
     * @param clazz Bean的类型
     * @param <T> 泛型类型
     * @return Bean实例
     * @throws IllegalStateException 如果ApplicationContext未初始化
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        checkContextInitialized();
        return context.getBean(name, clazz);
    }

    /**
     * 检查是否包含指定名称的Bean
     * @param name Bean的名称
     * @return 如果包含返回true，否则返回false
     */
    public static boolean containsBean(String name) {
        checkContextInitialized();
        return context.containsBean(name);
    }

    /**
     * 获取ApplicationContext实例
     * @return ApplicationContext实例
     * @throws IllegalStateException 如果ApplicationContext未初始化
     */
    public static ApplicationContext getApplicationContext() {
        checkContextInitialized();
        return context;
    }

    /**
     * 检查ApplicationContext是否已初始化
     * @throws IllegalStateException 如果ApplicationContext未初始化
     */
    private static void checkContextInitialized() {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext尚未初始化，请确保SpringContext已被Spring容器管理");
        }
    }
}