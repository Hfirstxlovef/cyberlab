package org.cyberlab.dto;

public class CreateTeamRequest {
    private String name;
    private Long leaderId;
    private String description;
    private String teamType; // red, blue

    // 构造函数
    public CreateTeamRequest() {}

    public CreateTeamRequest(String name, Long leaderId, String description, String teamType) {
        this.name = name;
        this.leaderId = leaderId;
        this.description = description;
        this.teamType = teamType;
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

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }
}
