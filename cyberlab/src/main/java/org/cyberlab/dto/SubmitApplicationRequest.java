package org.cyberlab.dto;

public class SubmitApplicationRequest {
    private Long teamId;
    private String message;

    // Constructors
    public SubmitApplicationRequest() {
    }

    public SubmitApplicationRequest(Long teamId, String message) {
        this.teamId = teamId;
        this.message = message;
    }

    // Getters and Setters
    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
