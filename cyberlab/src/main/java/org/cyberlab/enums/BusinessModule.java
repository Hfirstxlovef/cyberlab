package org.cyberlab.enums;

/**
 * 业务模块枚举
 *
 * 用于分类系统中的业务领域，便于：
 * - 按模块统计操作量（容器模块操作1523次）
 * - 模块健康度监控（用户模块成功率98.5%）
 * - 权限管理（仅允许访问特定模块）
 * - 大屏展示（各模块操作热力图）
 */
public enum BusinessModule {

    /**
     * 容器管理模块
     * 功能：容器的创建、启动、停止、删除、配置等
     */
    CONTAINER("容器管理"),

    /**
     * 用户管理模块
     * 功能：用户注册、登录、信息修改、密码重置等
     */
    USER("用户管理"),

    /**
     * 成果管理模块
     * 功能：成果提交、审核、评分、导出等
     */
    ACHIEVEMENT("成果管理"),

    /**
     * 演练管理模块
     * 功能：演练创建、启动、监控、结束等
     */
    DRILL("演练管理"),

    /**
     * 场景管理模块
     * 功能：场景模板的创建、编辑、部署、导入导出等
     */
    SCENARIO("场景管理"),

    /**
     * 权限管理模块
     * 功能：权限分配、角色授权、权限审计等
     */
    PERMISSION("权限管理"),

    /**
     * 角色管理模块
     * 功能：角色创建、编辑、权限配置等
     */
    ROLE("角色管理"),

    /**
     * 队伍管理模块
     * 功能：队伍创建、成员管理、队伍评分等
     */
    TEAM("队伍管理"),

    /**
     * 主机节点管理模块
     * 功能：Docker主机的添加、配置、监控、健康检查等
     */
    HOST_NODE("主机节点管理"),

    /**
     * 资产管理模块
     * 功能：IT资产的登记、管理、探测、监控等
     */
    ASSET("资产管理"),

    /**
     * 系统配置模块
     * 功能：系统参数配置、初始化设置、环境变量管理等
     */
    SYSTEM_CONFIG("系统配置"),

    /**
     * 日志管理模块
     * 功能：日志查询、清理、归档、导出等
     */
    LOG("日志管理"),

    /**
     * 认证授权模块
     * 功能：登录认证、Token管理、SSO集成等
     */
    AUTH("认证授权"),

    /**
     * 监控告警模块
     * 功能：系统监控、性能指标、告警规则、通知推送等
     */
    MONITOR("监控告警"),

    /**
     * 大屏展示模块
     * 功能：大屏配置、数据展示、实时刷新等
     */
    BIG_SCREEN("大屏展示"),

    /**
     * 文件上传模块
     * 功能：文件上传、下载、管理等
     */
    FILE("文件管理"),

    /**
     * 录屏管理模块
     * 功能：屏幕录制、录像查看、录像管理等
     */
    RECORDING("录屏管理"),

    /**
     * 网络拓扑模块
     * 功能：网络拓扑的创建、编辑、可视化等
     */
    TOPOLOGY("网络拓扑"),

    /**
     * 数据备份模块
     * 功能：数据备份、恢复、归档等
     */
    BACKUP("数据备份"),

    /**
     * 通知消息模块
     * 功能：站内信、邮件通知、短信推送等
     */
    NOTIFICATION("通知消息"),

    /**
     * 其他/未分类模块
     * 用于无法明确归类的操作
     */
    OTHER("其他");

    private final String description;

    BusinessModule(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据描述查找业务模块（容错匹配 + 关键词推断）
     */
    public static BusinessModule fromDescription(String desc) {
        if (desc == null || desc.isEmpty()) {
            return OTHER;
        }

        String lowerDesc = desc.toLowerCase();

        // 优先级1: 精确匹配模块描述
        for (BusinessModule module : values()) {
            if (desc.contains(module.description) ||
                lowerDesc.contains(module.name().toLowerCase())) {
                return module;
            }
        }

        // 优先级2: 关键词推断
        // 用户管理模块关键词
        if (lowerDesc.matches(".*(用户|登录|注册|认证|授权|角色|权限).*")) {
            return USER;
        }

        // 容器管理模块关键词
        if (lowerDesc.matches(".*(容器|docker|启动|停止|重启|部署|镜像|container).*")) {
            return CONTAINER;
        }

        // 资产管理模块关键词
        if (lowerDesc.matches(".*(资产|靶机|设备|asset).*")) {
            return ASSET;
        }

        // 成果管理模块关键词
        if (lowerDesc.matches(".*(成果|提交|审核|flag|答案|achievement).*")) {
            return ACHIEVEMENT;
        }

        // 场景管理模块关键词
        if (lowerDesc.matches(".*(场景|演练|训练|scenario|drill|template|模板).*")) {
            return SCENARIO;
        }

        // 主机节点管理模块关键词
        if (lowerDesc.matches(".*(主机|节点|host|node|宿主).*")) {
            return HOST_NODE;
        }

        // 日志管理模块关键词
        if (lowerDesc.matches(".*(日志|审计|监控|log|audit).*")) {
            return LOG;
        }

        // 系统配置模块关键词
        if (lowerDesc.matches(".*(设置|配置|参数|系统|setting|config).*")) {
            return SYSTEM_CONFIG;
        }

        // 认证授权模块关键词
        if (lowerDesc.matches(".*(认证|授权|许可证|license|激活|登录|注册).*")) {
            return AUTH;
        }

        // 队伍管理模块关键词
        if (lowerDesc.matches(".*(队伍|团队|team).*")) {
            return TEAM;
        }

        // 权限管理模块关键词
        if (lowerDesc.matches(".*(权限|permission).*")) {
            return PERMISSION;
        }

        // 角色管理模块关键词
        if (lowerDesc.matches(".*(角色|role).*")) {
            return ROLE;
        }

        // 大屏展示模块关键词
        if (lowerDesc.matches(".*(大屏|展示|可视化|dashboard|bigscreen).*")) {
            return BIG_SCREEN;
        }

        // 文件管理模块关键词
        if (lowerDesc.matches(".*(文件|上传|下载|file|upload).*")) {
            return FILE;
        }

        // 监控告警模块关键词
        if (lowerDesc.matches(".*(监控|告警|报警|monitor|alert).*")) {
            return MONITOR;
        }

        // 演练管理模块关键词
        if (lowerDesc.matches(".*(演练|drill).*")) {
            return DRILL;
        }

        // 录屏管理模块关键词
        if (lowerDesc.matches(".*(录屏|录像|视频|recording|video).*")) {
            return RECORDING;
        }

        // 网络拓扑模块关键词
        if (lowerDesc.matches(".*(拓扑|网络|topology|network).*")) {
            return TOPOLOGY;
        }

        // 数据备份模块关键词
        if (lowerDesc.matches(".*(备份|恢复|归档|backup|restore).*")) {
            return BACKUP;
        }

        // 通知消息模块关键词
        if (lowerDesc.matches(".*(通知|消息|邮件|短信|notification|message).*")) {
            return NOTIFICATION;
        }

        return OTHER;
    }

    @Override
    public String toString() {
        return name() + "(" + description + ")";
    }
}
