package org.cyberlab.dto;

import org.cyberlab.entity.Achievement;
import java.util.List;
import java.util.Map;

public class BlueDashboardData {
    private int protectedAssets;
    private int targetAssets;
    private int totalAttacks;
    private int blockedAttacks;
    private int successfulAttacks;
    private double defenseSuccessRate;
    private List<Achievement> recentAttacks;
    private Map<String, String> assetStatus;

    // Getters and Setters
    public int getProtectedAssets() { return protectedAssets; }
    public void setProtectedAssets(int protectedAssets) { this.protectedAssets = protectedAssets; }

    public int getTargetAssets() { return targetAssets; }
    public void setTargetAssets(int targetAssets) { this.targetAssets = targetAssets; }

    public int getTotalAttacks() { return totalAttacks; }
    public void setTotalAttacks(int totalAttacks) { this.totalAttacks = totalAttacks; }

    public int getBlockedAttacks() { return blockedAttacks; }
    public void setBlockedAttacks(int blockedAttacks) { this.blockedAttacks = blockedAttacks; }

    public int getSuccessfulAttacks() { return successfulAttacks; }
    public void setSuccessfulAttacks(int successfulAttacks) { this.successfulAttacks = successfulAttacks; }

    public double getDefenseSuccessRate() { return defenseSuccessRate; }
    public void setDefenseSuccessRate(double defenseSuccessRate) { this.defenseSuccessRate = defenseSuccessRate; }

    public List<Achievement> getRecentAttacks() { return recentAttacks; }
    public void setRecentAttacks(List<Achievement> recentAttacks) { this.recentAttacks = recentAttacks; }

    public Map<String, String> getAssetStatus() { return assetStatus; }
    public void setAssetStatus(Map<String, String> assetStatus) { this.assetStatus = assetStatus; }
}