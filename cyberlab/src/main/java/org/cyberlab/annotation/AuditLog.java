package org.cyberlab.annotation;

import org.cyberlab.enums.BusinessModule;
import org.cyberlab.enums.OperationType;

import java.lang.annotation.*;

/**
 * 审计日志注解（增强版）
 *
 * 用于标记需要详细审计追踪的安全敏感操作，如：
 * - 用户权限变更
 * - 容器部署/删除
 * - 敏感数据修改
 * - 成果审核
 *
 * 功能特性：
 * 1. 自动记录操作前后数据（beforeData/afterData）
 * 2. 强制记录到 AUDIT 或 SECURITY 分类
 * 3. 支持指定业务ID字段
 * 4. 支持标签系统
 * 5. 支持操作类型和业务模块分类
 *
 * 使用示例：
 * <pre>
 * {@code
 * @AuditLog(
 *     operation = "修改用户角色",
 *     operationType = OperationType.UPDATE,
 *     module = BusinessModule.USER,
 *     objectType = "User",
 *     category = "SECURITY",
 *     level = "WARN",
 *     captureArgs = true,
 *     businessIdParam = 0,
 *     tags = {"权限变更", "敏感操作"}
 * )
 * public void updateUserRole(Long userId, String newRole) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作名称（必填）
     * 示例："删除容器", "修改用户权限", "审核成果"
     */
    String operation();

    /**
     * 操作类型（可选，默认EXECUTE）
     * CREATE：创建操作
     * UPDATE：更新操作
     * DELETE：删除操作
     * APPROVE：审核通过
     * REJECT：审核驳回
     * 其他：参见OperationType枚举
     */
    OperationType operationType() default OperationType.EXECUTE;

    /**
     * 业务模块（可选，默认OTHER）
     * USER：用户管理
     * PERMISSION：权限管理
     * CONTAINER：容器管理
     * ACHIEVEMENT：成果管理
     * 其他：参见BusinessModule枚举
     */
    BusinessModule module() default BusinessModule.OTHER;

    /**
     * 操作对象类型（可选）
     * 示例："User"、"Role"、"Container"
     * 留空则自动从方法参数推断
     */
    String objectType() default "";

    /**
     * 日志分类（默认AUDIT）
     * 可选值：AUDIT（审计）, SECURITY（安全）, BUSINESS（业务）
     */
    String category() default "AUDIT";

    /**
     * 日志级别（默认WARN）
     * WARN: 重要操作（权限变更、数据修改）
     * ERROR: 异常操作（失败的敏感操作）
     * INFO: 一般审计操作
     */
    String level() default "WARN";

    /**
     * 是否捕获方法参数作为 beforeData（默认true）
     * 会将所有参数序列化为JSON存储
     */
    boolean captureArgs() default true;

    /**
     * 是否捕获方法返回值作为 afterData（默认false）
     * 注意：大对象可能导致性能问题
     */
    boolean captureResult() default false;

    /**
     * 业务ID参数索引（默认-1表示自动推断）
     * 示例：businessIdParam = 0 表示第一个参数是业务ID
     */
    int businessIdParam() default -1;

    /**
     * 业务ID字段名（用于复杂对象）
     * 示例：businessIdField = "containerId" 从参数对象中提取 containerId 字段
     */
    String businessIdField() default "";

    /**
     * 标签数组（用于日志分类和检索）
     * 示例：tags = {"敏感操作", "录屏", "critical"}
     */
    String[] tags() default {};

    /**
     * 操作描述（可选，提供额外的上下文信息）
     */
    String description() default "";
}
