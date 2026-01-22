package org.cyberlab.enums;

/**
 * 操作状态枚举
 *
 * 用于记录操作的执行结果，便于：
 * - 计算操作成功率（成功率 = SUCCESS / (SUCCESS + FAILED)）
 * - 失败操作告警（FAILED状态触发告警）
 * - 异步操作跟踪（PENDING状态标识未完成）
 * - 批量操作结果分析（PARTIAL状态表示部分成功）
 */
public enum OperationStatus {

    /**
     * 成功
     * 操作完全成功，无任何错误
     */
    SUCCESS("成功"),

    /**
     * 失败
     * 操作完全失败，抛出异常或返回错误
     */
    FAILED("失败"),

    /**
     * 部分成功
     * 批量操作中部分成功、部分失败
     * 示例：批量删除10个容器，成功7个，失败3个
     */
    PARTIAL("部分成功"),

    /**
     * 待处理
     * 异步操作已提交但未完成
     * 示例：后台任务排队中、长时间运行的操作
     */
    PENDING("待处理"),

    /**
     * 超时
     * 操作执行超过预期时间
     */
    TIMEOUT("超时"),

    /**
     * 已取消
     * 用户或系统主动取消的操作
     */
    CANCELLED("已取消"),

    /**
     * 跳过
     * 条件不满足，操作被跳过
     * 示例：容器已存在，跳过创建
     */
    SKIPPED("跳过");

    private final String description;

    OperationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为终态（最终状态）
     * 终态：SUCCESS, FAILED, PARTIAL, TIMEOUT, CANCELLED, SKIPPED
     * 非终态：PENDING
     */
    public boolean isFinalState() {
        return this != PENDING;
    }

    /**
     * 判断是否为成功状态（完全成功或部分成功）
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == PARTIAL;
    }

    /**
     * 判断是否为失败状态（完全失败或超时）
     */
    public boolean isFailure() {
        return this == FAILED || this == TIMEOUT;
    }

    /**
     * 根据异常判断状态
     * 无异常 → SUCCESS
     * 有异常 → FAILED
     */
    public static OperationStatus fromException(Throwable ex) {
        return ex == null ? SUCCESS : FAILED;
    }

    /**
     * 根据描述查找操作状态（容错匹配）
     */
    public static OperationStatus fromDescription(String desc) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }

        for (OperationStatus status : values()) {
            if (desc.contains(status.description)) {
                return status;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return name() + "(" + description + ")";
    }
}
