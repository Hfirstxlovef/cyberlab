package org.cyberlab.dto;

import org.cyberlab.entity.Achievement;
import org.cyberlab.entity.Asset;
import java.util.List;
import java.util.Map;

public class RedDashboardData {
    private int availableTargets;
    private int highValueTargets;
    private int totalAttempts;
    private int successfulAttacks;
    private int pendingReviews;
    private double successRate;
    private Map<String, String> targetReachability;
    private List<Achievement> recentAttacks;
    private List<Asset> recommendedTargets;

    // Getters and Setters
    public int getAvailableTargets() { return availableTargets; }
    public void setAvailableTargets(int availableTargets) { this.availableTargets = availableTargets; }

    public int getHighValueTargets() { return highValueTargets; }
    public void setHighValueTargets(int highValueTargets) { this.highValueTargets = highValueTargets; }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }

    public int getSuccessfulAttacks() { return successfulAttacks; }
    public void setSuccessfulAttacks(int successfulAttacks) { this.successfulAttacks = successfulAttacks; }

    public int getPendingReviews() { return pendingReviews; }
    public void setPendingReviews(int pendingReviews) { this.pendingReviews = pendingReviews; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public Map<String, String> getTargetReachability() { return targetReachability; }
    public void setTargetReachability(Map<String, String> targetReachability) { this.targetReachability = targetReachability; }

    public List<Achievement> getRecentAttacks() { return recentAttacks; }
    public void setRecentAttacks(List<Achievement> recentAttacks) { this.recentAttacks = recentAttacks; }

    public List<Asset> getRecommendedTargets() { return recommendedTargets; }
    public void setRecommendedTargets(List<Asset> recommendedTargets) { this.recommendedTargets = recommendedTargets; }
}