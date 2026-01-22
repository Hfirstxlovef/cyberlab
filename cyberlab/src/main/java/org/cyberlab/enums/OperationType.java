package org.cyberlab.enums;

/**
 * 操作类型枚举
 *
 * 用于标准化日志中的操作类型，便于统计分析和查询过滤
 *
 * 使用场景：
 * - 日志分类统计（CREATE操作有多少次）
 * - 权限控制验证（某用户是否有DELETE权限）
 * - 审计报告生成（上周所有UPDATE操作列表）
 */
public enum OperationType {

    /**
     * 创建操作
     * 示例：创建用户、创建容器、创建演练场景
     */
    CREATE("创建"),

    /**
     * 读取/查询操作
     * 示例：查看用户列表、查询容器详情、导出报表
     */
    READ("查询"),

    /**
     * 更新操作
     * 示例：修改用户信息、更新容器配置、编辑场景参数
     */
    UPDATE("更新"),

    /**
     * 删除操作
     * 示例：删除用户、删除容器、清理过期数据
     */
    DELETE("删除"),

    /**
     * 执行操作（启动、停止、重启等状态变更）
     * 示例：启动容器、停止演练、重启服务
     */
    EXECUTE("执行"),

    /**
     * 批量操作
     * 示例：批量删除容器、批量导入用户、批量审核成果
     */
    BATCH("批量操作"),

    /**
     * 导出操作
     * 示例：导出日志、导出用户列表、导出成绩单
     */
    EXPORT("导出"),

    /**
     * 导入操作
     * 示例：导入用户、导入场景配置、导入题目
     */
    IMPORT("导入"),

    /**
     * 审计操作
     * 示例：查看操作日志、审计权限变更、安全审查
     */
    AUDIT("审计"),

    /**
     * 审核通过操作
     * 示例：审核成果通过、审批权限申请
     */
    APPROVE("审核通过"),

    /**
     * 审核驳回操作
     * 示例：驳回成果提交、拒绝权限申请
     */
    REJECT("驳回"),

    /**
     * 配置操作
     * 示例：修改系统配置、调整参数、变更设置
     */
    CONFIGURE("配置"),

    /**
     * 登录操作
     * 示例：用户登录、SSO登录
     */
    LOGIN("登录"),

    /**
     * 登出操作
     * 示例：用户登出、会话超时
     */
    LOGOUT("登出");

    private final String description;

    OperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据描述查找操作类型（容错匹配）
     */
    public static OperationType fromDescription(String desc) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }

        for (OperationType type : values()) {
            if (desc.contains(type.description)) {
                return type;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return name() + "(" + description + ")";
    }
}
