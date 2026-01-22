package org.cyberlab.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardData {
    private int totalUsers;
    private int activeUsers;
    private int redTeamCount;
    private int blueTeamCount;
    private int totalAssets;
    private int activeAssets;
    private int targetAssets;
    private int totalAchievements;
    private int pendingAchievements;
    private int approvedAchievements;
    private String systemStatus;
    private LocalDateTime lastUpdateTime;
    private List<String> recentActivities;

    // Getters and Setters
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }

    public int getRedTeamCount() { return redTeamCount; }
    public void setRedTeamCount(int redTeamCount) { this.redTeamCount = redTeamCount; }

    public int getBlueTeamCount() { return blueTeamCount; }
    public void setBlueTeamCount(int blueTeamCount) { this.blueTeamCount = blueTeamCount; }

    public int getTotalAssets() { return totalAssets; }
    public void setTotalAssets(int totalAssets) { this.totalAssets = totalAssets; }

    public int getActiveAssets() { return activeAssets; }
    public void setActiveAssets(int activeAssets) { this.activeAssets = activeAssets; }

    public int getTargetAssets() { return targetAssets; }
    public void setTargetAssets(int targetAssets) { this.targetAssets = targetAssets; }

    public int getTotalAchievements() { return totalAchievements; }
    public void setTotalAchievements(int totalAchievements) { this.totalAchievements = totalAchievements; }

    public int getPendingAchievements() { return pendingAchievements; }
    public void setPendingAchievements(int pendingAchievements) { this.pendingAchievements = pendingAchievements; }

    public int getApprovedAchievements() { return approvedAchievements; }
    public void setApprovedAchievements(int approvedAchievements) { this.approvedAchievements = approvedAchievements; }

    public String getSystemStatus() { return systemStatus; }
    public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }

    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public List<String> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<String> recentActivities) { this.recentActivities = recentActivities; }
}