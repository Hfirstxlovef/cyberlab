package org.cyberlab.dto;

public class UpdateTeamRequest {
    private String name;
    private Long leaderId;
    private String description;
    private String status; // active, disbanded

    // 构造函数
    public UpdateTeamRequest() {}

    public UpdateTeamRequest(String name, Long leaderId, String description, String status) {
        this.name = name;
        this.leaderId = leaderId;
        this.description = description;
        this.status = status;
    }

    // Getter和Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Long leaderId) {
        this.leaderId = leaderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
