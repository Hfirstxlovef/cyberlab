package org.cyberlab.enums;

/**
 * 成果类型枚举
 * 区分红队和蓝队的成果类型，定义建议分值范围
 */
public enum AchievementType {

    // ============ 红队成果类型 (攻击方向) ============
    RED_VULNERABILITY_EXPLOIT("red_vulnerability_exploit", "漏洞发现与利用", "🎯", "red", 80, 100),
    RED_PRIVILEGE_ESCALATION("red_privilege_escalation", "权限提升", "🔓", "red", 75, 95),
    RED_LATERAL_MOVEMENT("red_lateral_movement", "横向移动", "🌐", "red", 70, 90),
    RED_DATA_EXFILTRATION("red_data_exfiltration", "数据窃取", "💾", "red", 65, 85),
    RED_SOCIAL_ENGINEERING("red_social_engineering", "社会工程学", "🎭", "red", 60, 80),
    RED_BACKDOOR_IMPLANT("red_backdoor_implant", "后门植入", "🚪", "red", 70, 85),
    RED_RECONNAISSANCE("red_reconnaissance", "信息收集", "🔍", "red", 50, 70),
    RED_ZERO_DAY("red_zero_day", "0day漏洞发现", "🎁", "red", 90, 100),

    // ============ 蓝队成果类型 (防御方向) ============
    BLUE_INTRUSION_DETECTION("blue_intrusion_detection", "入侵检测与响应", "🛡️", "blue", 80, 100),
    BLUE_THREAT_INTELLIGENCE("blue_threat_intelligence", "威胁情报分析", "🔬", "blue", 75, 95),
    BLUE_LOG_ANALYSIS("blue_log_analysis", "日志分析与关联", "📊", "blue", 70, 90),
    BLUE_INCIDENT_RESPONSE("blue_incident_response", "应急响应处置", "🚨", "blue", 75, 90),
    BLUE_VULNERABILITY_REMEDIATION("blue_vulnerability_remediation", "漏洞修复加固", "🔧", "blue", 65, 80),
    BLUE_FORENSICS("blue_forensics", "取证分析", "🔎", "blue", 60, 80),
    BLUE_SECURITY_POLICY("blue_security_policy", "安全策略优化", "📋", "blue", 55, 75),
    BLUE_APT_ATTRIBUTION("blue_apt_attribution", "APT攻击溯源", "🎯", "blue", 85, 100);

    private final String value;
    private final String name;
    private final String icon;
    private final String teamType;  // red/blue
    private final int minScore;     // 建议最低分值
    private final int maxScore;     // 建议最高分值

    AchievementType(String value, String name, String icon, String teamType, int minScore, int maxScore) {
        this.value = value;
        this.name = name;
        this.icon = icon;
        this.teamType = teamType;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getTeamType() {
        return teamType;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    /**
     * 获取建议基础分值（取范围中间值）
     */
    public int getBaseScore() {
        return (minScore + maxScore) / 2;
    }

    /**
     * 根据字符串值获取枚举
     */
    public static AchievementType fromValue(String value) {
        for (AchievementType type : AchievementType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown achievement type: " + value);
    }

    /**
     * 根据字符串值获取基础分值
     */
    public static int getBaseScoreByValue(String value) {
        try {
            return fromValue(value).getBaseScore();
        } catch (IllegalArgumentException e) {
            // 如果找不到对应类型，返回默认分值
            return 70;
        }
    }

    /**
     * 根据队伍类型获取对应的成果类型列表
     */
    public static AchievementType[] getTypesByTeamType(String teamType) {
        if (teamType == null) {
            return values();
        }

        return java.util.Arrays.stream(values())
                .filter(type -> type.teamType.equals(teamType))
                .toArray(AchievementType[]::new);
    }
}
