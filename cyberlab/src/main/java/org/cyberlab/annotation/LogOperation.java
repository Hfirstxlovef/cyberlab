package org.cyberlab.annotation;

import org.cyberlab.enums.BusinessModule;
import org.cyberlab.enums.OperationType;

import java.lang.annotation.*;

/**
 * 日志操作注解（增强版）
 *
 * 使用示例：
 * <pre>
 * {@code
 * @LogOperation(
 *     value = "删除容器",
 *     operationType = OperationType.DELETE,
 *     module = BusinessModule.CONTAINER,
 *     objectType = "Container"
 * )
 * public void deleteContainer(Long id) { }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 操作名称（必填）
     * 示例："删除容器"、"修改用户权限"
     */
    String value();

    /**
     * 操作类型（可选，默认EXECUTE）
     * CREATE：创建操作
     * READ：查询操作
     * UPDATE：更新操作
     * DELETE：删除操作
     * EXECUTE：执行操作（启动、停止、重启）
     * BATCH：批量操作
     * 其他：参见OperationType枚举
     */
    OperationType operationType() default OperationType.EXECUTE;

    /**
     * 业务模块（可选，默认OTHER）
     * CONTAINER：容器管理
     * USER：用户管理
     * ACHIEVEMENT：成果管理
     * 其他：参见BusinessModule枚举
     */
    BusinessModule module() default BusinessModule.OTHER;

    /**
     * 操作对象类型（可选）
     * 示例："Container"、"User"、"Achievement"
     * 留空则自动从方法参数推断
     */
    String objectType() default "";
}