package org.cyberlab.dto;

import org.cyberlab.entity.Achievement;
import java.util.List;
import java.util.Map;

public class RefereeDashboardData {
    private int totalSubmissions;
    private int pendingReviews;
    private int approvedCount;
    private int rejectedCount;
    private double successRate;
    private Map<String, Long> teamPerformance;
    private List<Achievement> pendingList;

    // Getters and Setters
    public int getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }

    public int getPendingReviews() { return pendingReviews; }
    public void setPendingReviews(int pendingReviews) { this.pendingReviews = pendingReviews; }

    public int getApprovedCount() { return approvedCount; }
    public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }

    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public Map<String, Long> getTeamPerformance() { return teamPerformance; }
    public void setTeamPerformance(Map<String, Long> teamPerformance) { this.teamPerformance = teamPerformance; }

    public List<Achievement> getPendingList() { return pendingList; }
    public void setPendingList(List<Achievement> pendingList) { this.pendingList = pendingList; }
}